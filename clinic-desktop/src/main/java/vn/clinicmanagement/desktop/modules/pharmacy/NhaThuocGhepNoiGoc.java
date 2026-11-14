package vn.clinicmanagement.desktop.modules.pharmacy;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.application.inventory.ApproveKiemKeHandler;
import vn.clinicmanagement.application.inventory.DispenseDonThuocHandler;
import vn.clinicmanagement.application.inventory.GetTonKhoKhaDungHandler;
import vn.clinicmanagement.application.inventory.ReceiveKhoTonKhoHandler;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.inventory.DonDatHang;
import vn.clinicmanagement.domain.inventory.FEFOPhanBoDichVu;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuNhapKho;
import vn.clinicmanagement.domain.inventory.PhieuXuatKho;
import vn.clinicmanagement.domain.inventory.KiemKe;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.IKhoRepository;
import vn.clinicmanagement.domain.repositories.ITieuChuan;
import vn.clinicmanagement.domain.repositories.PhanTrangKetQua;

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
public final class NhaThuocGhepNoiGoc {

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
            /** Trường `dispensePrescription`: cấp phát đơn thuốc. */
            DispenseDonThuocHandler dispensePrescription,
            /** Trường `receiveStock`: tiếp nhận tồn kho. */
            ReceiveKhoTonKhoHandler receiveStock,
            /** Trường `checkStock`: kiểm tra tồn kho. */
            GetTonKhoKhaDungHandler checkStock,
            /** Trường `approveStocktake`: duyệt kiemke. */
            ApproveKiemKeHandler approveStocktake,
            /** Trường `khoRepository`: `kho` kho dữ liệu. */
            IKhoRepository khoRepository,
            /** Trường `prescriptionRepository`: đơn thuốc kho dữ liệu. */
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

    private NhaThuocGhepNoiGoc() {
        // Utility / Factory class
    }

    /** Hàm `createDefault`: tạo macdinh. */
    public static Services createDefault() {
        InMemoryInventoryRepository khoRepo = new InMemoryInventoryRepository();
        IDonThuocRepository prescriptionRepo = new InMemoryPrescriptionRepository();
        FEFOPhanBoDichVu fefoService = new FEFOPhanBoDichVu();

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

    /** Hàm `create`: tạo. */
    public static Services create(
            IKhoRepository khoRepository,
            IDonThuocRepository prescriptionRepository,
            FEFOPhanBoDichVu fefoService,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit) {
        Objects.requireNonNull(khoRepository, "khoRepository");
        Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        Objects.requireNonNull(fefoService, "fefoService");
        Objects.requireNonNull(transactions, "transactions");
        Objects.requireNonNull(audit, "audit");

        DispenseDonThuocHandler dispensePrescription = new DispenseDonThuocHandler(
                prescriptionRepository,
                khoRepository,
                fefoService,
                transactions,
                audit
        );
        ReceiveKhoTonKhoHandler receiveStock = new ReceiveKhoTonKhoHandler(
                khoRepository,
                transactions,
                audit
        );
        GetTonKhoKhaDungHandler checkStock = new GetTonKhoKhaDungHandler(
                khoRepository,
                audit
        );
        ApproveKiemKeHandler approveStocktake = new ApproveKiemKeHandler(
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
        private final Map<Integer, MatHang> items = new ConcurrentHashMap<>();
        private final Map<Integer, TonKho> batches = new ConcurrentHashMap<>();
        private final Map<Integer, Kho> warehouses = new ConcurrentHashMap<>();
        private final Map<Integer, PhieuNhapKho> nhapKhos = new ConcurrentHashMap<>();
        private final Map<Integer, PhieuXuatKho> xuatKhos = new ConcurrentHashMap<>();
        /** Trường `purchaseOrders`: muahang `Orders`. */
        private final Map<Integer, DonDatHang> purchaseOrders = new ConcurrentHashMap<>();
        private final Map<Integer, KiemKe> stocktakes = new ConcurrentHashMap<>();

        /** Trường `itemSeq`: mathang `Seq`. */
        private int itemSeq = 1;
        /** Trường `batchSeq`: lo `Seq`. */
        private int batchSeq = 1;
        private int nhapKhoSeq = 1;
        private int xuatKhoSeq = 1;
        private int poSeq = 1;
        /** Trường `stocktakeSeq`: kiemke `Seq`. */
        private int stocktakeSeq = 1;

        /** Hàm `addKho`: thêm kho. */
        public void addKho(Kho kho) {
            if (kho != null && kho.getId() != null) {
                warehouses.put(kho.getId(), kho);
            }
        }

        /** Hàm `getAllKho`: lấy `All` kho. */
        public List<Kho> getAllKho() {
            return new ArrayList<>(warehouses.values());
        }

        /** Hàm `getItemById`: lấy mathang theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getItemById`. */
        @Override
        public Optional<MatHang> getItemById(int id) {
            return Optional.ofNullable(items.get(id));
        }

        /** Hàm `getItemByCode`: lấy mathang theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getItemByCode`. */
        @Override
        public Optional<MatHang> getItemByCode(String code) {
            return items.values().stream().filter(i -> Objects.equals(i.getMaItem(), code)).findFirst();
        }

        /** Hàm `saveItem`: lưu mathang. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#saveItem`. */
        @Override
        public synchronized MatHang saveItem(MatHang item) {
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
                                || (i.getTenItem() != null && i.getTenItem().toLowerCase().contains(kw));
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
            return batches.values().stream()
                    .filter(b -> b.getMaKho() == maKho)
                    .toList();
        }

        /** Hàm `getBatchById`: lấy lo theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getBatchById`. */
        @Override
        public Optional<TonKho> getBatchById(int id) {
            return Optional.ofNullable(batches.get(id));
        }

        /** Hàm `saveTonKho`: lưu ton kho. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#saveTonKho`. */
        @Override
        public synchronized TonKho saveTonKho(TonKho tonKho) {
            if (tonKho.getId() == null || tonKho.getId() == 0) {
                tonKho.setId(batchSeq++);
            }
            batches.put(tonKho.getId(), tonKho);
            return tonKho;
        }

        /** Hàm `savePhieuNhapKho`: lưu phiếu nhap kho. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#savePhieuNhapKho`. */
        @Override
        public synchronized PhieuNhapKho savePhieuNhapKho(PhieuNhapKho phieu) {
            if (phieu.getId() == null || phieu.getId() == 0) {
                phieu.setId(nhapKhoSeq++);
            }
            nhapKhos.put(phieu.getId(), phieu);
            return phieu;
        }

        /** Hàm `getPhieuNhapKhoById`: lấy phiếu nhap kho theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getPhieuNhapKhoById`. */
        @Override
        public Optional<PhieuNhapKho> getPhieuNhapKhoById(int id) {
            return Optional.ofNullable(nhapKhos.get(id));
        }

        /** Hàm `savePhieuXuatKho`: lưu phiếu xuất kho. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#savePhieuXuatKho`. */
        @Override
        public synchronized PhieuXuatKho savePhieuXuatKho(PhieuXuatKho phieu) {
            if (phieu.getId() == null || phieu.getId() == 0) {
                phieu.setId(xuatKhoSeq++);
            }
            xuatKhos.put(phieu.getId(), phieu);
            return phieu;
        }

        /** Hàm `getPhieuXuatKhoById`: lấy phiếu xuất kho theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getPhieuXuatKhoById`. */
        @Override
        public Optional<PhieuXuatKho> getPhieuXuatKhoById(int id) {
            return Optional.ofNullable(xuatKhos.get(id));
        }

        /** Hàm `savePurchaseOrder`: lưu muahang yêu cầu. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#savePurchaseOrder`. */
        @Override
        public synchronized DonDatHang savePurchaseOrder(DonDatHang don) {
            if (don.getId() == null || don.getId() == 0) {
                don.setId(poSeq++);
            }
            purchaseOrders.put(don.getId(), don);
            return don;
        }

        /** Hàm `getPurchaseOrderById`: lấy muahang yêu cầu theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getPurchaseOrderById`. */
        @Override
        public Optional<DonDatHang> getPurchaseOrderById(int id) {
            return Optional.ofNullable(purchaseOrders.get(id));
        }

        /** Hàm `saveStocktake`: lưu kiemke. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#saveStocktake`. */
        @Override
        public synchronized KiemKe saveStocktake(KiemKe stocktake) {
            if (stocktake.getId() == null || stocktake.getId() == 0) {
                stocktake.setId(stocktakeSeq++);
            }
            stocktakes.put(stocktake.getId(), stocktake);
            return stocktake;
        }

        /** Hàm `getStocktakeById`: lấy kiemke theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getStocktakeById`. */
        @Override
        public Optional<KiemKe> getStocktakeById(int id) {
            return Optional.ofNullable(stocktakes.get(id));
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
    }

    public static final class InMemoryPrescriptionRepository implements IDonThuocRepository {
        private final Map<Integer, DonThuoc> prescriptions = new ConcurrentHashMap<>();
        /** Trường `idSeq`: mã `Seq`. */
        private int idSeq = 1;

        /** Hàm `getByLuotKhamId`: lấy theo luot kham mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IDonThuocRepository#getByLuotKhamId`. */
        @Override
        public synchronized List<DonThuoc> getByLuotKhamId(int maLuotKham) {
            return prescriptions.values().stream()
                    .filter(d -> d.getMaLuotKham() == maLuotKham)
                    .toList();
        }

        /** Hàm `getById`: lấy theo mã. Gốc khai báo tại `IDonThuocRepository#getById`. */
        @Override
        public synchronized Optional<DonThuoc> getById(int id) {
            return Optional.ofNullable(prescriptions.get(id));
        }

        /** Hàm `getAll`: lấy `All`. Gốc khai báo tại `IDonThuocRepository#getAll`. */
        @Override
        public synchronized List<DonThuoc> getAll() {
            return new ArrayList<>(prescriptions.values());
        }

        /** Hàm `getPaged`: lấy phân trang. Gốc khai báo tại `IDonThuocRepository#getPaged`. */
        @Override
        public PhanTrangKetQua<DonThuoc> getPaged(int pageIndex, int pageSize, ITieuChuan<DonThuoc> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        /** Hàm `find`: tìm. Gốc khai báo tại `IDonThuocRepository#find`. */
        @Override
        public List<DonThuoc> find(ITieuChuan<DonThuoc> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<DonThuoc> specification) {
            return false;
        }

        /** Hàm `count`: dem. Gốc khai báo tại `IDonThuocRepository#count`. */
        @Override
        public int count(ITieuChuan<DonThuoc> specification) {
            return prescriptions.size();
        }

        /** Hàm `add`: thêm. Gốc khai báo tại `IDonThuocRepository#add`. */
        @Override
        public synchronized void add(DonThuoc entity) {
            if (entity.getId() == null || entity.getId() == 0) {
                entity.setId(idSeq++);
            }
            prescriptions.put(entity.getId(), entity);
        }

        /** Hàm `update`: cập nhật. Gốc khai báo tại `IDonThuocRepository#update`. */
        @Override
        public synchronized void update(DonThuoc entity) {
            if (entity.getId() != null) {
                prescriptions.put(entity.getId(), entity);
            }
        }

        /** Hàm `delete`: xóa. Gốc khai báo tại `IDonThuocRepository#delete`. */
        @Override
        public synchronized void delete(DonThuoc entity) {
            if (entity != null && entity.getId() != null) {
                prescriptions.remove(entity.getId());
            }
        }

        /** Hàm `getUnitOfWork`: lấy đơn vị của làm việc. Gốc khai báo tại `IDonThuocRepository#getUnitOfWork`. */
        @Override
        public IUnitOfWork getUnitOfWork() {
            return NOOP_UOW;
        }
    }
}
