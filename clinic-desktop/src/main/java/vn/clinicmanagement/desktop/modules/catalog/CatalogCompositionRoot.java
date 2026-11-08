package vn.clinicmanagement.desktop.modules.catalog;

import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.application.facility.CreateRoomHandler;
import vn.clinicmanagement.application.inventory.CreateItemHandler;
import vn.clinicmanagement.application.inventory.GetItemCatalogHandler;
import vn.clinicmanagement.desktop.modules.paraclinical.ParaclinicalCompositionRoot;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.common.enums.LoaiItem;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.inventory.DonDatHang;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuNhapKho;
import vn.clinicmanagement.domain.inventory.PhieuXuatKho;
import vn.clinicmanagement.domain.inventory.Stocktake;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.repositories.IKhoRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Composition root cho phân hệ Quản Trị Danh Mục Dùng Chung (Catalog Module - HD-INTEG-04).
 * Lắp ráp và cấu hình:
 * - CreateItemHandler: Tạo mới mặt hàng, thuốc, vật tư y tế
 * - GetItemCatalogHandler: Tra cứu danh mục dược & vật tư
 * - CreateRoomHandler: Tạo mới phòng khám / buồng kỹ thuật
 * - IKhoRepository: Kho lưu trữ hàng hóa, dược phẩm
 * - IPhongKhamRepository: Kho lưu trữ phòng khám & cơ sở vật chất
 * - IChiDinhDichVuRepository: Kho danh mục dịch vụ kỹ thuật, viện phí
 */
public final class CatalogCompositionRoot {

    private static final AuditWritePort FALLBACK_AUDIT = new AuditWritePort() {
        @Override public void appendInCurrentTransaction(SystemAuditEntry entry) {}
        @Override public void appendIndependently(SystemAuditEntry entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}
        @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
    };

    private static final TransactionBoundary DIRECT_TRANSACTIONS = new TransactionBoundary() {
        @Override
        public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
            try {
                return operation.get();
            } catch (Exception ex) {
                return Result.failure("TX_ERROR", ex.getMessage());
            }
        }
    };

    private static final IUnitOfWork NOOP_UOW = new IUnitOfWork() {
        @Override public int saveChanges() { return 1; }
        @Override public boolean saveEntities() { return true; }
    };

    public record Services(
            CreateItemHandler createItem,
            GetItemCatalogHandler getItemCatalog,
            CreateRoomHandler createRoom,
            IKhoRepository inventoryRepository,
            IPhongKhamRepository roomRepository,
            IChiDinhDichVuRepository serviceRepository) {
        public Services {
            Objects.requireNonNull(createItem, "createItem");
            Objects.requireNonNull(getItemCatalog, "getItemCatalog");
            Objects.requireNonNull(createRoom, "createRoom");
            Objects.requireNonNull(inventoryRepository, "inventoryRepository");
            Objects.requireNonNull(roomRepository, "roomRepository");
            Objects.requireNonNull(serviceRepository, "serviceRepository");
        }

        public CreateItemHandler getCreateItem() { return createItem; }
        public GetItemCatalogHandler getGetItemCatalog() { return getItemCatalog; }
        public CreateRoomHandler getCreateRoom() { return createRoom; }
        public IKhoRepository getInventoryRepository() { return inventoryRepository; }
        public IPhongKhamRepository getRoomRepository() { return roomRepository; }
        public IChiDinhDichVuRepository getServiceRepository() { return serviceRepository; }
    }

    private static volatile Services instance;

    private CatalogCompositionRoot() {}

    public static synchronized Services getInstance() {
        if (instance == null) {
            instance = createDefault();
        }
        return instance;
    }

    public static synchronized void configure(Services services) {
        instance = Objects.requireNonNull(services, "services");
    }

    public static synchronized void reset() {
        instance = null;
    }

    public static Services createDefault() {
        InMemoryKhoRepository khoRepo = new InMemoryKhoRepository();
        seedDefaultItems(khoRepo);

        InMemoryPhongKhamRepository roomRepo = new InMemoryPhongKhamRepository();
        seedDefaultRooms(roomRepo);

        ParaclinicalCompositionRoot.InMemoryChiDinhDichVuRepository serviceRepo =
                new ParaclinicalCompositionRoot.InMemoryChiDinhDichVuRepository();
        seedDefaultServices(serviceRepo);

        return create(khoRepo, roomRepo, serviceRepo, DIRECT_TRANSACTIONS, FALLBACK_AUDIT);
    }

    public static Services create(
            IKhoRepository inventoryRepository,
            IPhongKhamRepository roomRepository,
            IChiDinhDichVuRepository serviceRepository) {
        return create(inventoryRepository, roomRepository, serviceRepository, DIRECT_TRANSACTIONS, FALLBACK_AUDIT);
    }

    public static Services create(
            IKhoRepository inventoryRepository,
            IPhongKhamRepository roomRepository,
            IChiDinhDichVuRepository serviceRepository,
            TransactionBoundary transactions,
            AuditWritePort audit) {
        Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        Objects.requireNonNull(roomRepository, "roomRepository");
        Objects.requireNonNull(serviceRepository, "serviceRepository");
        Objects.requireNonNull(transactions, "transactions");
        Objects.requireNonNull(audit, "audit");

        CreateItemHandler createItem = new CreateItemHandler(inventoryRepository, transactions, audit);
        GetItemCatalogHandler getItemCatalog = new GetItemCatalogHandler(inventoryRepository, audit);
        CreateRoomHandler createRoom = new CreateRoomHandler(roomRepository, transactions);

        return new Services(createItem, getItemCatalog, createRoom, inventoryRepository, roomRepository, serviceRepository);
    }

    private static void seedDefaultItems(InMemoryKhoRepository repo) {
        Item item1 = new Item();
        item1.setMaItem("MED-PARA-500");
        item1.setTenItem("Paracetamol 500mg");
        item1.setLoaiItem(LoaiItem.THUOC);
        item1.setHoatChat("Paracetamol");
        item1.setHamLuong("500mg");
        item1.setDonViCoBan("Viên");
        item1.setGiaBanMacDinh(new BigDecimal("1500"));
        item1.setHoatDong(true);
        repo.saveItem(item1);

        Item item2 = new Item();
        item2.setMaItem("MED-AMOX-500");
        item2.setTenItem("Amoxicillin 500mg");
        item2.setLoaiItem(LoaiItem.THUOC);
        item2.setHoatChat("Amoxicillin trihydrat");
        item2.setHamLuong("500mg");
        item2.setDonViCoBan("Viên");
        item2.setGiaBanMacDinh(new BigDecimal("2500"));
        item2.setHoatDong(true);
        repo.saveItem(item2);

        Item item3 = new Item();
        item3.setMaItem("MED-IBU-400");
        item3.setTenItem("Ibuprofen 400mg");
        item3.setLoaiItem(LoaiItem.THUOC);
        item3.setHoatChat("Ibuprofen");
        item3.setHamLuong("400mg");
        item3.setDonViCoBan("Viên");
        item3.setGiaBanMacDinh(new BigDecimal("3000"));
        item3.setHoatDong(true);
        repo.saveItem(item3);
    }

    private static void seedDefaultRooms(InMemoryPhongKhamRepository repo) {
        PhongKham r1 = new PhongKham();
        r1.setMaPhong("PK-101");
        r1.setTenPhong("Phòng Khám Nội 01");
        r1.setMaKhoa(1);
        r1.setViTri("Tầng 1 - Khu A");
        r1.setHoatDong(true);
        repo.add(r1);

        PhongKham r2 = new PhongKham();
        r2.setMaPhong("PK-102");
        r2.setTenPhong("Phòng Khám Ngoại 01");
        r2.setMaKhoa(2);
        r2.setViTri("Tầng 1 - Khu B");
        r2.setHoatDong(true);
        repo.add(r2);

        PhongKham r3 = new PhongKham();
        r3.setMaPhong("PK-201");
        r3.setTenPhong("Phòng Siêu Âm Màu");
        r3.setMaKhoa(3);
        r3.setViTri("Tầng 2 - Khu CĐHA");
        r3.setHoatDong(true);
        repo.add(r3);
    }

    private static void seedDefaultServices(ParaclinicalCompositionRoot.InMemoryChiDinhDichVuRepository repo) {
        DanhMucDichVu dv1 = new DanhMucDichVu();
        dv1.setId(1);
        dv1.setMaDichVu("DV-XN-01");
        dv1.setTenDichVu("Tổng phân tích tế bào máu ngoại vi");
        dv1.setLoaiDichVu(LoaiDichVu.XET_NGHIEM);
        dv1.setDonGia(new BigDecimal("80000"));
        dv1.setGiaBHYT(new BigDecimal("50000"));
        dv1.setHoatDong(true);
        repo.addDichVu(dv1);

        DanhMucDichVu dv2 = new DanhMucDichVu();
        dv2.setId(2);
        dv2.setMaDichVu("DV-XN-02");
        dv2.setTenDichVu("Sinh hóa máu - Glucose lúc đói");
        dv2.setLoaiDichVu(LoaiDichVu.XET_NGHIEM);
        dv2.setDonGia(new BigDecimal("45000"));
        dv2.setGiaBHYT(new BigDecimal("30000"));
        dv2.setHoatDong(true);
        repo.addDichVu(dv2);

        DanhMucDichVu dv3 = new DanhMucDichVu();
        dv3.setId(3);
        dv3.setMaDichVu("DV-CDHA-01");
        dv3.setTenDichVu("Chụp X-Quang ngực thẳng");
        dv3.setLoaiDichVu(LoaiDichVu.CHAN_DOAN_HINH_ANH);
        dv3.setDonGia(new BigDecimal("120000"));
        dv3.setGiaBHYT(new BigDecimal("85000"));
        dv3.setHoatDong(true);
        repo.addDichVu(dv3);

        DanhMucDichVu dv4 = new DanhMucDichVu();
        dv4.setId(4);
        dv4.setMaDichVu("DV-CDHA-02");
        dv4.setTenDichVu("Siêu âm ổ bụng tổng quát");
        dv4.setLoaiDichVu(LoaiDichVu.CHAN_DOAN_HINH_ANH);
        dv4.setDonGia(new BigDecimal("150000"));
        dv4.setGiaBHYT(new BigDecimal("100000"));
        dv4.setHoatDong(true);
        repo.addDichVu(dv4);
    }

    /**
     * In-memory implementation của {@link IKhoRepository} cho phân hệ Catalog.
     */
    public static final class InMemoryKhoRepository implements IKhoRepository {
        private final Map<Integer, Item> items = new ConcurrentHashMap<>();
        private final Map<Integer, Kho> warehouses = new ConcurrentHashMap<>();
        private final Map<Integer, TonKho> batches = new ConcurrentHashMap<>();
        private int itemSeq = 1;

        @Override
        public Optional<Item> getItemById(int id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public Optional<Item> getItemByCode(String code) {
            if (code == null) return Optional.empty();
            return items.values().stream()
                    .filter(i -> Objects.equals(i.getMaItem(), code))
                    .findFirst();
        }

        @Override
        public synchronized Item saveItem(Item item) {
            if (item == null) return null;
            if (item.getId() == null || item.getId() == 0) {
                item.setId(itemSeq++);
            }
            items.put(item.getId(), item);
            return item;
        }

        @Override
        public List<Item> searchItems(String keyword, Boolean hoatDong) {
            return items.values().stream()
                    .filter(i -> {
                        if (hoatDong != null && i.isHoatDong() != hoatDong) return false;
                        if (keyword == null || keyword.isBlank()) return true;
                        String kw = keyword.toLowerCase();
                        return (i.getMaItem() != null && i.getMaItem().toLowerCase().contains(kw))
                                || (i.getTenItem() != null && i.getTenItem().toLowerCase().contains(kw))
                                || (i.getHoatChat() != null && i.getHoatChat().toLowerCase().contains(kw));
                    })
                    .toList();
        }

        @Override
        public Optional<Kho> getKhoById(int id) {
            return Optional.ofNullable(warehouses.get(id));
        }

        @Override
        public List<TonKho> getAvailableBatches(int maKho, int maItem) {
            return batches.values().stream()
                    .filter(b -> b.getMaKho() == maKho && b.getMaItem() == maItem)
                    .toList();
        }

        @Override
        public List<TonKho> getBatchesByKho(int maKho) {
            return batches.values().stream().filter(b -> b.getMaKho() == maKho).toList();
        }

        @Override
        public Optional<TonKho> getBatchById(int id) {
            return Optional.ofNullable(batches.get(id));
        }

        @Override
        public synchronized TonKho saveTonKho(TonKho tonKho) {
            if (tonKho != null && tonKho.getId() != null) {
                batches.put(tonKho.getId(), tonKho);
            }
            return tonKho;
        }

        @Override
        public PhieuNhapKho savePhieuNhapKho(PhieuNhapKho phieuNhapKho) {
            return phieuNhapKho;
        }

        @Override
        public Optional<PhieuNhapKho> getPhieuNhapKhoById(int id) {
            return Optional.empty();
        }

        @Override
        public PhieuXuatKho savePhieuXuatKho(PhieuXuatKho phieuXuatKho) {
            return phieuXuatKho;
        }

        @Override
        public Optional<PhieuXuatKho> getPhieuXuatKhoById(int id) {
            return Optional.empty();
        }

        @Override
        public DonDatHang savePurchaseOrder(DonDatHang donDatHang) {
            return donDatHang;
        }

        @Override
        public Optional<DonDatHang> getPurchaseOrderById(int id) {
            return Optional.empty();
        }

        @Override
        public Stocktake saveStocktake(Stocktake stocktake) {
            return stocktake;
        }

        @Override
        public Optional<Stocktake> getStocktakeById(int id) {
            return Optional.empty();
        }

        @Override
        public int saveChanges() {
            return 1;
        }

        @Override
        public boolean saveEntities() {
            return true;
        }

        public void clear() {
            items.clear();
            warehouses.clear();
            batches.clear();
        }
    }

    /**
     * In-memory implementation của {@link IPhongKhamRepository} cho phân hệ Catalog.
     */
    public static final class InMemoryPhongKhamRepository implements IPhongKhamRepository {
        private final Map<Integer, PhongKham> store = new ConcurrentHashMap<>();
        private final Map<String, PhongKham> codeToRoom = new ConcurrentHashMap<>();
        private int idSeq = 1;

        @Override
        public Optional<PhongKham> getById(int id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<PhongKham> getByMaPhong(String maPhong) {
            if (maPhong == null) return Optional.empty();
            return Optional.ofNullable(codeToRoom.get(maPhong.trim().toUpperCase()));
        }

        @Override
        public boolean existsByMaPhong(String maPhong) {
            if (maPhong == null) return false;
            return codeToRoom.containsKey(maPhong.trim().toUpperCase());
        }

        @Override
        public List<PhongKham> getAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<PhongKham> getActiveRooms() {
            return store.values().stream().filter(PhongKham::isHoatDong).toList();
        }

        @Override
        public List<PhongKham> getActiveRoomsInScope(int facilityId, int departmentId) {
            return store.values().stream()
                    .filter(PhongKham::isHoatDong)
                    .filter(p -> departmentId <= 0 || p.getMaKhoa() == departmentId)
                    .toList();
        }

        @Override
        public List<PhongKham> getByMaKhoa(int maKhoa) {
            return store.values().stream().filter(p -> p.getMaKhoa() == maKhoa).toList();
        }

        @Override
        public List<PhongKham> getByMaCoSo(int maCoSo) {
            return new ArrayList<>(store.values());
        }

        @Override
        public synchronized void add(PhongKham phongKham) {
            if (phongKham == null) return;
            if (phongKham.getId() == null || phongKham.getId() == 0) {
                phongKham.setId(idSeq++);
            }
            store.put(phongKham.getId(), phongKham);
            if (phongKham.getMaPhong() != null) {
                codeToRoom.put(phongKham.getMaPhong().trim().toUpperCase(), phongKham);
            }
            if (phongKham.getTenPhong() != null) {
                codeToRoom.put(phongKham.getTenPhong().trim().toUpperCase(), phongKham);
            }
        }

        @Override
        public synchronized void update(PhongKham phongKham) {
            if (phongKham != null && phongKham.getId() != null) {
                store.put(phongKham.getId(), phongKham);
                if (phongKham.getMaPhong() != null) {
                    codeToRoom.put(phongKham.getMaPhong().trim().toUpperCase(), phongKham);
                }
            }
        }

        @Override
        public synchronized void delete(PhongKham phongKham) {
            if (phongKham != null && phongKham.getId() != null) {
                store.remove(phongKham.getId());
                if (phongKham.getMaPhong() != null) {
                    codeToRoom.remove(phongKham.getMaPhong().trim().toUpperCase());
                }
            }
        }

        public void clear() {
            store.clear();
            codeToRoom.clear();
        }
    }
}
