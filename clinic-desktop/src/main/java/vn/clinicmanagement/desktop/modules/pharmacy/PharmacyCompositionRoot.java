package vn.clinicmanagement.desktop.modules.pharmacy;

import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.application.inventory.ApproveStocktakeHandler;
import vn.clinicmanagement.application.inventory.DispensePrescriptionHandler;
import vn.clinicmanagement.application.inventory.GetStockAvailabilityHandler;
import vn.clinicmanagement.application.inventory.ReceiveWarehouseStockHandler;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.inventory.DonDatHang;
import vn.clinicmanagement.domain.inventory.FefoAllocationService;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuNhapKho;
import vn.clinicmanagement.domain.inventory.PhieuXuatKho;
import vn.clinicmanagement.domain.inventory.Stocktake;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.IKhoRepository;
import vn.clinicmanagement.domain.repositories.ISpecification;
import vn.clinicmanagement.domain.repositories.PagedResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Composition root cho phân hệ Quản Lý Dược & Kho Tủ Trực (Pharmacy & Inventory).
 * Khởi tạo và lắp ráp 4 Clean Architecture Handlers:
 * - DispensePrescriptionHandler: Cấp phát đơn thuốc
 * - ReceiveWarehouseStockHandler: Nhập kho dược / vật tư
 * - GetStockAvailabilityHandler: Kiểm tra tồn kho khả dụng
 * - ApproveStocktakeHandler: Phê duyệt biên bản kiểm kê
 * Cùng với 2 Repositories:
 * - IKhoRepository: Quản lý kho, lô hàng, tồn kho, phiếu nhập/xuất, kiểm kê
 * - IDonThuocRepository: Quản lý đơn thuốc
 */
public final class PharmacyCompositionRoot {

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
            DispensePrescriptionHandler dispensePrescription,
            ReceiveWarehouseStockHandler receiveStock,
            GetStockAvailabilityHandler checkStock,
            ApproveStocktakeHandler approveStocktake,
            IKhoRepository khoRepository,
            IDonThuocRepository prescriptionRepository) {
        public Services {
            Objects.requireNonNull(dispensePrescription, "dispensePrescription");
            Objects.requireNonNull(receiveStock, "receiveStock");
            Objects.requireNonNull(checkStock, "checkStock");
            Objects.requireNonNull(approveStocktake, "approveStocktake");
            Objects.requireNonNull(khoRepository, "khoRepository");
            Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        }
    }

    private PharmacyCompositionRoot() {
        // Utility / Factory class
    }

    public static Services createDefault() {
        InMemoryInventoryRepository khoRepo = new InMemoryInventoryRepository();
        IDonThuocRepository prescriptionRepo = new InMemoryPrescriptionRepository();
        FefoAllocationService fefoService = new FefoAllocationService();

        Kho defaultKho = new Kho();
        defaultKho.setId(1);
        defaultKho.setTenKho("Kho Tổng");
        defaultKho.setLoaiKho(vn.clinicmanagement.domain.common.enums.LoaiKho.THUOC);
        defaultKho.setTrangThai(vn.clinicmanagement.domain.common.enums.TrangThaiKho.HOAT_DONG);
        khoRepo.addKho(defaultKho);

        return create(
                khoRepo,
                prescriptionRepo,
                fefoService,
                DIRECT_TRANSACTIONS,
                FALLBACK_AUDIT
        );
    }

    public static Services create(
            IKhoRepository khoRepository,
            IDonThuocRepository prescriptionRepository,
            FefoAllocationService fefoService,
            TransactionBoundary transactions,
            AuditWritePort audit) {
        Objects.requireNonNull(khoRepository, "khoRepository");
        Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        Objects.requireNonNull(fefoService, "fefoService");
        Objects.requireNonNull(transactions, "transactions");
        Objects.requireNonNull(audit, "audit");

        DispensePrescriptionHandler dispensePrescription = new DispensePrescriptionHandler(
                prescriptionRepository,
                khoRepository,
                fefoService,
                transactions,
                audit
        );
        ReceiveWarehouseStockHandler receiveStock = new ReceiveWarehouseStockHandler(
                khoRepository,
                transactions,
                audit
        );
        GetStockAvailabilityHandler checkStock = new GetStockAvailabilityHandler(
                khoRepository,
                audit
        );
        ApproveStocktakeHandler approveStocktake = new ApproveStocktakeHandler(
                khoRepository,
                transactions,
                audit
        );

        return new Services(
                dispensePrescription,
                receiveStock,
                checkStock,
                approveStocktake,
                khoRepository,
                prescriptionRepository
        );
    }

    public static final class InMemoryInventoryRepository implements IKhoRepository {
        private final Map<Integer, Item> items = new ConcurrentHashMap<>();
        private final Map<Integer, TonKho> batches = new ConcurrentHashMap<>();
        private final Map<Integer, Kho> warehouses = new ConcurrentHashMap<>();
        private final Map<Integer, PhieuNhapKho> nhapKhos = new ConcurrentHashMap<>();
        private final Map<Integer, PhieuXuatKho> xuatKhos = new ConcurrentHashMap<>();
        private final Map<Integer, DonDatHang> purchaseOrders = new ConcurrentHashMap<>();
        private final Map<Integer, Stocktake> stocktakes = new ConcurrentHashMap<>();

        private int itemSeq = 1;
        private int batchSeq = 1;
        private int nhapKhoSeq = 1;
        private int xuatKhoSeq = 1;
        private int poSeq = 1;
        private int stocktakeSeq = 1;

        public void addKho(Kho kho) {
            if (kho != null && kho.getId() != null) {
                warehouses.put(kho.getId(), kho);
            }
        }

        public List<Kho> getAllKho() {
            return new ArrayList<>(warehouses.values());
        }

        @Override
        public Optional<Item> getItemById(int id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public Optional<Item> getItemByCode(String code) {
            return items.values().stream().filter(i -> Objects.equals(i.getMaItem(), code)).findFirst();
        }

        @Override
        public synchronized Item saveItem(Item item) {
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
                                || (i.getTenItem() != null && i.getTenItem().toLowerCase().contains(kw));
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
            return batches.values().stream()
                    .filter(b -> b.getMaKho() == maKho)
                    .toList();
        }

        @Override
        public Optional<TonKho> getBatchById(int id) {
            return Optional.ofNullable(batches.get(id));
        }

        @Override
        public synchronized TonKho saveTonKho(TonKho tonKho) {
            if (tonKho.getId() == null || tonKho.getId() == 0) {
                tonKho.setId(batchSeq++);
            }
            batches.put(tonKho.getId(), tonKho);
            return tonKho;
        }

        @Override
        public synchronized PhieuNhapKho savePhieuNhapKho(PhieuNhapKho phieu) {
            if (phieu.getId() == null || phieu.getId() == 0) {
                phieu.setId(nhapKhoSeq++);
            }
            nhapKhos.put(phieu.getId(), phieu);
            return phieu;
        }

        @Override
        public Optional<PhieuNhapKho> getPhieuNhapKhoById(int id) {
            return Optional.ofNullable(nhapKhos.get(id));
        }

        @Override
        public synchronized PhieuXuatKho savePhieuXuatKho(PhieuXuatKho phieu) {
            if (phieu.getId() == null || phieu.getId() == 0) {
                phieu.setId(xuatKhoSeq++);
            }
            xuatKhos.put(phieu.getId(), phieu);
            return phieu;
        }

        @Override
        public Optional<PhieuXuatKho> getPhieuXuatKhoById(int id) {
            return Optional.ofNullable(xuatKhos.get(id));
        }

        @Override
        public synchronized DonDatHang savePurchaseOrder(DonDatHang don) {
            if (don.getId() == null || don.getId() == 0) {
                don.setId(poSeq++);
            }
            purchaseOrders.put(don.getId(), don);
            return don;
        }

        @Override
        public Optional<DonDatHang> getPurchaseOrderById(int id) {
            return Optional.ofNullable(purchaseOrders.get(id));
        }

        @Override
        public synchronized Stocktake saveStocktake(Stocktake stocktake) {
            if (stocktake.getId() == null || stocktake.getId() == 0) {
                stocktake.setId(stocktakeSeq++);
            }
            stocktakes.put(stocktake.getId(), stocktake);
            return stocktake;
        }

        @Override
        public Optional<Stocktake> getStocktakeById(int id) {
            return Optional.ofNullable(stocktakes.get(id));
        }

        @Override
        public int saveChanges() {
            return 1;
        }

        @Override
        public boolean saveEntities() {
            return true;
        }
    }

    public static final class InMemoryPrescriptionRepository implements IDonThuocRepository {
        private final Map<Integer, DonThuoc> prescriptions = new ConcurrentHashMap<>();
        private int idSeq = 1;

        @Override
        public synchronized List<DonThuoc> getByLuotKhamId(int maLuotKham) {
            return prescriptions.values().stream()
                    .filter(d -> d.getMaLuotKham() == maLuotKham)
                    .toList();
        }

        @Override
        public synchronized Optional<DonThuoc> getById(int id) {
            return Optional.ofNullable(prescriptions.get(id));
        }

        @Override
        public synchronized List<DonThuoc> getAll() {
            return new ArrayList<>(prescriptions.values());
        }

        @Override
        public PagedResult<DonThuoc> getPaged(int pageIndex, int pageSize, ISpecification<DonThuoc> specification) {
            return new PagedResult<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<DonThuoc> find(ISpecification<DonThuoc> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ISpecification<DonThuoc> specification) {
            return false;
        }

        @Override
        public int count(ISpecification<DonThuoc> specification) {
            return prescriptions.size();
        }

        @Override
        public synchronized void add(DonThuoc entity) {
            if (entity.getId() == null || entity.getId() == 0) {
                entity.setId(idSeq++);
            }
            prescriptions.put(entity.getId(), entity);
        }

        @Override
        public synchronized void update(DonThuoc entity) {
            if (entity.getId() != null) {
                prescriptions.put(entity.getId(), entity);
            }
        }

        @Override
        public synchronized void delete(DonThuoc entity) {
            if (entity != null && entity.getId() != null) {
                prescriptions.remove(entity.getId());
            }
        }

        @Override
        public IUnitOfWork getUnitOfWork() {
            return NOOP_UOW;
        }
    }
}
