package vn.clinicmanagement.desktop.modules.catalog;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.application.facility.CreatePhongHandler;
import vn.clinicmanagement.application.inventory.CreateMatHangHandler;
import vn.clinicmanagement.application.inventory.GetMatHangDanhMucHandler;
import vn.clinicmanagement.desktop.modules.paraclinical.CanLamSangGhepNoiGoc;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.inventory.DonDatHang;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuNhapKho;
import vn.clinicmanagement.domain.inventory.PhieuXuatKho;
import vn.clinicmanagement.domain.inventory.KiemKe;
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
public final class DanhMucGhepNoiGoc {

    private static final KiemToanWriteCongKetNoi FALLBACK_AUDIT = new KiemToanWriteCongKetNoi() {
        /** Hàm `appendInCurrentTransaction`: thêm vào trong hientai giao dịch. */
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        /** Hàm `appendIndependently`: thêm vào `Independently`. */
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        /** Hàm `appendMedicalRecordInCurrentTransaction`: thêm vào yte ghi nhận trong hientai giao dịch. */
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        /** Hàm `appendMedicalRecordIndependently`: thêm vào yte ghi nhận `Independently`. */
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    };

    private static final GiaoDichRanhGioi DIRECT_TRANSACTIONS = new GiaoDichRanhGioi() {
        /** Hàm `inTransaction`: trong giao dịch. */
        @Override
        public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation) {
            try {
                return operation.get();
            } catch (Exception ex) {
                return KetQua.failure("TX_ERROR", ex.getMessage());
            }
        }
    };

    private static final IUnitOfWork NOOP_UOW = new IUnitOfWork() {
        /** Hàm `saveChanges`: lưu thaydoi. */
        @Override public int saveChanges() { return 1; }
        /** Hàm `saveEntities`: lưu thực thể. */
        @Override public boolean saveEntities() { return true; }
    };

    public record Services(
            /** Trường `createItem`: tạo mathang. */
            CreateMatHangHandler createItem,
            /** Trường `getItemCatalog`: lấy mathang danhmuc. */
            GetMatHangDanhMucHandler getItemCatalog,
            /** Trường `createRoom`: tạo phòng. */
            CreatePhongHandler createRoom,
            /** Trường `inventoryRepository`: kho kho dữ liệu. */
            IKhoRepository inventoryRepository,
            /** Trường `roomRepository`: phòng kho dữ liệu. */
            IPhongKhamRepository roomRepository,
            /** Trường `serviceRepository`: dịch vụ kho dữ liệu. */
            IChiDinhDichVuRepository serviceRepository) {
        public Services {
            Objects.requireNonNull(createItem, "createItem");
            Objects.requireNonNull(getItemCatalog, "getItemCatalog");
            Objects.requireNonNull(createRoom, "createRoom");
            Objects.requireNonNull(inventoryRepository, "inventoryRepository");
            Objects.requireNonNull(roomRepository, "roomRepository");
            Objects.requireNonNull(serviceRepository, "serviceRepository");
        }

        /** Hàm `getCreateItem`: lấy tạo mathang. */
        public CreateMatHangHandler getCreateItem() { return createItem; }
        /** Hàm `getGetItemCatalog`: lấy lấy mathang danhmuc. */
        public GetMatHangDanhMucHandler getGetItemCatalog() { return getItemCatalog; }
        /** Hàm `getCreateRoom`: lấy tạo phòng. */
        public CreatePhongHandler getCreateRoom() { return createRoom; }
        /** Hàm `getInventoryRepository`: lấy kho kho dữ liệu. */
        public IKhoRepository getInventoryRepository() { return inventoryRepository; }
        /** Hàm `getRoomRepository`: lấy phòng kho dữ liệu. */
        public IPhongKhamRepository getRoomRepository() { return roomRepository; }
        /** Hàm `getServiceRepository`: lấy dịch vụ kho dữ liệu. */
        public IChiDinhDichVuRepository getServiceRepository() { return serviceRepository; }
    }

    private static volatile Services instance;

    private DanhMucGhepNoiGoc() {}

    /** Hàm `getInstance`: lấy `Instance`. */
    public static synchronized Services getInstance() {
        if (instance == null) {
            instance = createDefault();
        }
        return instance;
    }

    public static synchronized void configure(Services services) {
        instance = Objects.requireNonNull(services, "services");
    }

    /** Hàm `reset`: đặt lại. */
    public static synchronized void reset() {
        instance = null;
    }

    /** Hàm `createDefault`: tạo macdinh. */
    public static Services createDefault() {
        InMemoryKhoRepository khoRepo = new InMemoryKhoRepository();
        seedDefaultItems(khoRepo);

        InMemoryPhongKhamRepository roomRepo = new InMemoryPhongKhamRepository();
        seedDefaultRooms(roomRepo);

        CanLamSangGhepNoiGoc.InMemoryChiDinhDichVuRepository serviceRepo =
                new CanLamSangGhepNoiGoc.InMemoryChiDinhDichVuRepository();
        seedDefaultServices(serviceRepo);

        return create(khoRepo, roomRepo, serviceRepo, DIRECT_TRANSACTIONS, FALLBACK_AUDIT);
    }

    /** Hàm `create`: tạo. */
    public static Services create(
            IKhoRepository inventoryRepository,
            IPhongKhamRepository roomRepository,
            IChiDinhDichVuRepository serviceRepository) {
        return create(inventoryRepository, roomRepository, serviceRepository, DIRECT_TRANSACTIONS, FALLBACK_AUDIT);
    }

    /** Hàm `create`: tạo. */
    public static Services create(
            IKhoRepository inventoryRepository,
            IPhongKhamRepository roomRepository,
            IChiDinhDichVuRepository serviceRepository,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit) {
        Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        Objects.requireNonNull(roomRepository, "roomRepository");
        Objects.requireNonNull(serviceRepository, "serviceRepository");
        Objects.requireNonNull(transactions, "transactions");
        Objects.requireNonNull(audit, "audit");

        CreateMatHangHandler createItem = new CreateMatHangHandler(inventoryRepository, transactions, audit);
        GetMatHangDanhMucHandler getItemCatalog = new GetMatHangDanhMucHandler(inventoryRepository, audit);
        CreatePhongHandler createRoom = new CreatePhongHandler(roomRepository, transactions);

        return new Services(createItem, getItemCatalog, createRoom, inventoryRepository, roomRepository, serviceRepository);
    }

    /** Hàm `seedDefaultItems`: `seed` macdinh `Items`. */
    private static void seedDefaultItems(InMemoryKhoRepository repo) {
        MatHang item1 = new MatHang();
        item1.setMaItem("MED-PARA-500");
        item1.setTenItem("Paracetamol 500mg");
        item1.setLoaiItem(LoaiMatHang.THUOC);
        item1.setHoatChat("Paracetamol");
        item1.setHamLuong("500mg");
        item1.setDonViCoBan("Viên");
        item1.setGiaBanMacDinh(new BigDecimal("1500"));
        item1.setHoatDong(true);
        repo.saveItem(item1);

        MatHang item2 = new MatHang();
        item2.setMaItem("MED-AMOX-500");
        item2.setTenItem("Amoxicillin 500mg");
        item2.setLoaiItem(LoaiMatHang.THUOC);
        item2.setHoatChat("Amoxicillin trihydrat");
        item2.setHamLuong("500mg");
        item2.setDonViCoBan("Viên");
        item2.setGiaBanMacDinh(new BigDecimal("2500"));
        item2.setHoatDong(true);
        repo.saveItem(item2);

        MatHang item3 = new MatHang();
        item3.setMaItem("MED-IBU-400");
        item3.setTenItem("Ibuprofen 400mg");
        item3.setLoaiItem(LoaiMatHang.THUOC);
        item3.setHoatChat("Ibuprofen");
        item3.setHamLuong("400mg");
        item3.setDonViCoBan("Viên");
        item3.setGiaBanMacDinh(new BigDecimal("3000"));
        item3.setHoatDong(true);
        repo.saveItem(item3);
    }

    /** Hàm `seedDefaultRooms`: `seed` macdinh phòng. */
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

    /** Hàm `seedDefaultServices`: `seed` macdinh dịch vụ. */
    private static void seedDefaultServices(CanLamSangGhepNoiGoc.InMemoryChiDinhDichVuRepository repo) {
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
        private final Map<Integer, MatHang> items = new ConcurrentHashMap<>();
        private final Map<Integer, Kho> warehouses = new ConcurrentHashMap<>();
        private final Map<Integer, TonKho> batches = new ConcurrentHashMap<>();
        /** Trường `itemSeq`: mathang `Seq`. */
        private int itemSeq = 1;

        /** Hàm `getItemById`: lấy mathang theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getItemById`. */
        @Override
        public Optional<MatHang> getItemById(int id) {
            return Optional.ofNullable(items.get(id));
        }

        /** Hàm `getItemByCode`: lấy mathang theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getItemByCode`. */
        @Override
        public Optional<MatHang> getItemByCode(String code) {
            if (code == null) return Optional.empty();
            return items.values().stream()
                    .filter(i -> Objects.equals(i.getMaItem(), code))
                    .findFirst();
        }

        /** Hàm `saveItem`: lưu mathang. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#saveItem`. */
        @Override
        public synchronized MatHang saveItem(MatHang item) {
            if (item == null) return null;
            if (item.getId() == null || item.getId() == 0) {
                item.setId(itemSeq++);
            }
            items.put(item.getId(), item);
            return item;
        }

        /** Hàm `searchItems`: tìm kiếm `Items`. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#searchItems`. */
        @Override
        public List<MatHang> searchItems(String keyword, Boolean hoatDong) {
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

        /** Hàm `getKhoById`: lấy kho theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getKhoById`. */
        @Override
        public Optional<Kho> getKhoById(int id) {
            return Optional.ofNullable(warehouses.get(id));
        }

        /** Hàm `getAvailableBatches`: lấy khadung `Batches`. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getAvailableBatches`. */
        @Override
        public List<TonKho> getAvailableBatches(int maKho, int maItem) {
            return batches.values().stream()
                    .filter(b -> b.getMaKho() == maKho && b.getMaItem() == maItem)
                    .toList();
        }

        /** Hàm `getBatchesByKho`: lấy `Batches` theo kho. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getBatchesByKho`. */
        @Override
        public List<TonKho> getBatchesByKho(int maKho) {
            return batches.values().stream().filter(b -> b.getMaKho() == maKho).toList();
        }

        /** Hàm `getBatchById`: lấy lo theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getBatchById`. */
        @Override
        public Optional<TonKho> getBatchById(int id) {
            return Optional.ofNullable(batches.get(id));
        }

        /** Hàm `saveTonKho`: lưu ton kho. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#saveTonKho`. */
        @Override
        public synchronized TonKho saveTonKho(TonKho tonKho) {
            if (tonKho != null && tonKho.getId() != null) {
                batches.put(tonKho.getId(), tonKho);
            }
            return tonKho;
        }

        /** Hàm `savePhieuNhapKho`: lưu phiếu nhap kho. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#savePhieuNhapKho`. */
        @Override
        public PhieuNhapKho savePhieuNhapKho(PhieuNhapKho phieuNhapKho) {
            return phieuNhapKho;
        }

        /** Hàm `getPhieuNhapKhoById`: lấy phiếu nhap kho theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getPhieuNhapKhoById`. */
        @Override
        public Optional<PhieuNhapKho> getPhieuNhapKhoById(int id) {
            return Optional.empty();
        }

        /** Hàm `savePhieuXuatKho`: lưu phiếu xuất kho. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#savePhieuXuatKho`. */
        @Override
        public PhieuXuatKho savePhieuXuatKho(PhieuXuatKho phieuXuatKho) {
            return phieuXuatKho;
        }

        /** Hàm `getPhieuXuatKhoById`: lấy phiếu xuất kho theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getPhieuXuatKhoById`. */
        @Override
        public Optional<PhieuXuatKho> getPhieuXuatKhoById(int id) {
            return Optional.empty();
        }

        /** Hàm `savePurchaseOrder`: lưu muahang yêu cầu. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#savePurchaseOrder`. */
        @Override
        public DonDatHang savePurchaseOrder(DonDatHang donDatHang) {
            return donDatHang;
        }

        /** Hàm `getPurchaseOrderById`: lấy muahang yêu cầu theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getPurchaseOrderById`. */
        @Override
        public Optional<DonDatHang> getPurchaseOrderById(int id) {
            return Optional.empty();
        }

        /** Hàm `saveStocktake`: lưu kiemke. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#saveStocktake`. */
        @Override
        public KiemKe saveStocktake(KiemKe stocktake) {
            return stocktake;
        }

        /** Hàm `getStocktakeById`: lấy kiemke theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getStocktakeById`. */
        @Override
        public Optional<KiemKe> getStocktakeById(int id) {
            return Optional.empty();
        }

        /** Hàm `saveChanges`: lưu thaydoi. Gốc khai báo tại `IKhoRepository#saveChanges`. */
        @Override
        public int saveChanges() {
            return 1;
        }

        /** Hàm `saveEntities`: lưu thực thể. Gốc khai báo tại `IKhoRepository#saveEntities`. */
        @Override
        public boolean saveEntities() {
            return true;
        }

        /** Hàm `clear`: xóa. */
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
        /** Trường `store`: luutru. */
        private final Map<Integer, PhongKham> store = new ConcurrentHashMap<>();
        /** Trường `codeToRoom`: mã den phòng. */
        private final Map<String, PhongKham> codeToRoom = new ConcurrentHashMap<>();
        /** Trường `idSeq`: mã `Seq`. */
        private int idSeq = 1;

        /** Hàm `getById`: lấy theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#getById`. */
        @Override
        public Optional<PhongKham> getById(int id) {
            return Optional.ofNullable(store.get(id));
        }

        /** Hàm `getByMaPhong`: lấy theo mã phòng. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#getByMaPhong`. */
        @Override
        public Optional<PhongKham> getByMaPhong(String maPhong) {
            if (maPhong == null) return Optional.empty();
            return Optional.ofNullable(codeToRoom.get(maPhong.trim().toUpperCase()));
        }

        /** Hàm `existsByMaPhong`: `exists` theo mã phòng. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#existsByMaPhong`. */
        @Override
        public boolean existsByMaPhong(String maPhong) {
            if (maPhong == null) return false;
            return codeToRoom.containsKey(maPhong.trim().toUpperCase());
        }

        /** Hàm `getAll`: lấy `All`. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#getAll`. */
        @Override
        public List<PhongKham> getAll() {
            return new ArrayList<>(store.values());
        }

        /** Hàm `getActiveRooms`: lấy hoạt động phòng. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#getActiveRooms`. */
        @Override
        public List<PhongKham> getActiveRooms() {
            return store.values().stream().filter(PhongKham::isHoatDong).toList();
        }

        /** Hàm `getActiveRoomsInScope`: lấy hoạt động phòng trong phamvi. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#getActiveRoomsInScope`. */
        @Override
        public List<PhongKham> getActiveRoomsInScope(int facilityId, int departmentId) {
            return store.values().stream()
                    .filter(PhongKham::isHoatDong)
                    .filter(p -> departmentId <= 0 || p.getMaKhoa() == departmentId)
                    .toList();
        }

        /** Hàm `getByMaKhoa`: lấy theo mã khoa. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#getByMaKhoa`. */
        @Override
        public List<PhongKham> getByMaKhoa(int maKhoa) {
            return store.values().stream().filter(p -> p.getMaKhoa() == maKhoa).toList();
        }

        /** Hàm `getByMaCoSo`: lấy theo mã có so. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#getByMaCoSo`. */
        @Override
        public List<PhongKham> getByMaCoSo(int maCoSo) {
            return new ArrayList<>(store.values());
        }

        /** Hàm `add`: thêm. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#add`. */
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

        /** Hàm `update`: cập nhật. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#update`. */
        @Override
        public synchronized void update(PhongKham phongKham) {
            if (phongKham != null && phongKham.getId() != null) {
                store.put(phongKham.getId(), phongKham);
                if (phongKham.getMaPhong() != null) {
                    codeToRoom.put(phongKham.getMaPhong().trim().toUpperCase(), phongKham);
                }
            }
        }

        /** Hàm `delete`: xóa. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#delete`. */
        @Override
        public synchronized void delete(PhongKham phongKham) {
            if (phongKham != null && phongKham.getId() != null) {
                store.remove(phongKham.getId());
                if (phongKham.getMaPhong() != null) {
                    codeToRoom.remove(phongKham.getMaPhong().trim().toUpperCase());
                }
            }
        }

        /** Hàm `clear`: xóa. */
        public void clear() {
            store.clear();
            codeToRoom.clear();
        }
    }
}
