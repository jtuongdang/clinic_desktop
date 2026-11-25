package vn.clinicmanagement.desktop.modules.pharmacy;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.inventory.receive.NhapNhaCungCap;
import vn.clinicmanagement.application.inventory.retail.XuatLe;
import vn.clinicmanagement.application.prescription.approve.ApproveDonThuocHandler;
import vn.clinicmanagement.infrastructure.persistence.examination.LuotKhamRepository;
import vn.clinicmanagement.infrastructure.persistence.inventory.NhapNhaCungCapRepository;
import vn.clinicmanagement.infrastructure.persistence.inventory.XuatLeRepository;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.application.inventory.approve.ApproveKiemKeHandler;
import vn.clinicmanagement.application.inventory.dispense.DispenseDonThuocHandler;
import vn.clinicmanagement.application.inventory.get.GetTonKhoKhaDungHandler;
import vn.clinicmanagement.application.inventory.receive.ReceiveKhoTonKhoHandler;
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
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    };

    private static final GiaoDichRanhGioi DIRECT_TRANSACTIONS = new GiaoDichRanhGioi() {
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
        @Override public int saveChanges() { return 1; }
        @Override public boolean saveEntities() { return true; }
    };

    public record Services(
            DispenseDonThuocHandler dispensePrescription,
            ReceiveKhoTonKhoHandler receiveStock,
            GetTonKhoKhaDungHandler checkStock,
            ApproveKiemKeHandler approveStocktake,
            IKhoRepository khoRepository,
            IDonThuocRepository prescriptionRepository,
            vn.clinicmanagement.application.inventory.create.CreateMatHangHandler createItem,
            vn.clinicmanagement.application.inventory.create.CreateThuocDayDuHandler saveDrug, XuatLe retail,
            NhapNhaCungCap supplierReceipt, ApproveDonThuocHandler approvePrescription) {
        public Services(DispenseDonThuocHandler dispensePrescription,ReceiveKhoTonKhoHandler receiveStock,
                GetTonKhoKhaDungHandler checkStock,ApproveKiemKeHandler approveStocktake,IKhoRepository khoRepository,
                IDonThuocRepository prescriptionRepository,vn.clinicmanagement.application.inventory.create.CreateMatHangHandler createItem){
            this(dispensePrescription,receiveStock,checkStock,approveStocktake,khoRepository,prescriptionRepository,createItem,null,null,null,null);
        }
        public Services(DispenseDonThuocHandler dispensePrescription,ReceiveKhoTonKhoHandler receiveStock,
                GetTonKhoKhaDungHandler checkStock,ApproveKiemKeHandler approveStocktake,IKhoRepository khoRepository,
                IDonThuocRepository prescriptionRepository,vn.clinicmanagement.application.inventory.create.CreateMatHangHandler createItem,
                vn.clinicmanagement.application.inventory.create.CreateThuocDayDuHandler saveDrug){
            this(dispensePrescription,receiveStock,checkStock,approveStocktake,khoRepository,prescriptionRepository,createItem,saveDrug,null,null,null);
        }
        public Services(DispenseDonThuocHandler dispensePrescription,ReceiveKhoTonKhoHandler receiveStock,
                GetTonKhoKhaDungHandler checkStock,ApproveKiemKeHandler approveStocktake,IKhoRepository khoRepository,
                IDonThuocRepository prescriptionRepository,vn.clinicmanagement.application.inventory.create.CreateMatHangHandler createItem,
                vn.clinicmanagement.application.inventory.create.CreateThuocDayDuHandler saveDrug, XuatLe retail){
            this(dispensePrescription,receiveStock,checkStock,approveStocktake,khoRepository,prescriptionRepository,createItem,saveDrug,retail,null,null);
        }
        public Services withDrugForm(vn.clinicmanagement.application.inventory.create.CreateThuocDayDuHandler handler){
            return withPharmacyForms(handler,retail,supplierReceipt,approvePrescription);
        }
        private Services withPharmacyForms(vn.clinicmanagement.application.inventory.create.CreateThuocDayDuHandler handler,
                XuatLe retail, NhapNhaCungCap supplierReceipt, ApproveDonThuocHandler approvePrescription){
            return new Services(dispensePrescription,receiveStock,checkStock,approveStocktake,khoRepository,prescriptionRepository,createItem,handler,retail,supplierReceipt,approvePrescription);
        }
        public Services(DispenseDonThuocHandler dispensePrescription,ReceiveKhoTonKhoHandler receiveStock,
                GetTonKhoKhaDungHandler checkStock,ApproveKiemKeHandler approveStocktake,
                IKhoRepository khoRepository,IDonThuocRepository prescriptionRepository){
            this(dispensePrescription,receiveStock,checkStock,approveStocktake,khoRepository,prescriptionRepository,null);
        }
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
        // Lớp tiện ích/khởi tạo đối tượng.
    }

    public static Services createProduction(){
        var uow=new vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork();
        var tx=new vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy(uow);
        var provider=new vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap(tx);
        var inventory=new vn.clinicmanagement.infrastructure.persistence.inventory.KhoRepository(provider,uow);
        var audit=new vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository(provider,uow,vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi::getConnection);
        var prescriptions = new vn.clinicmanagement.infrastructure.persistence.prescription.DonThuocRepository(provider, uow);
        var visits = new LuotKhamRepository(provider, uow);
        var invoices = new vn.clinicmanagement.infrastructure.persistence.billing.HoaDonRepository(provider, uow);
        var rxBilling = new vn.clinicmanagement.application.billing.DonThuocVienPhiDongBo(invoices);
        return create(inventory, prescriptions, new FEFOPhanBoDichVu(), tx, audit, rxBilling)
            .withPharmacyForms(
                new vn.clinicmanagement.application.inventory.create.CreateThuocDayDuHandler(inventory,new vn.clinicmanagement.infrastructure.persistence.inventory.NhaThuocDanhSachRepository(provider),tx,audit),
                new XuatLeRepository(provider,tx),
                new NhapNhaCungCapRepository(provider, tx, audit),
                new ApproveDonThuocHandler(prescriptions, visits, tx, audit, rxBilling));
    }

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

    public static Services create(
            IKhoRepository khoRepository,
            IDonThuocRepository prescriptionRepository,
            FEFOPhanBoDichVu fefoService,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit) {
        return create(khoRepository, prescriptionRepository, fefoService, transactions, audit, null);
    }

    public static Services create(
            IKhoRepository khoRepository,
            IDonThuocRepository prescriptionRepository,
            FEFOPhanBoDichVu fefoService,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit,
            vn.clinicmanagement.application.billing.DonThuocVienPhiDongBo rxBilling) {
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
                audit,
                rxBilling
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
                prescriptionRepository,
                new vn.clinicmanagement.application.inventory.create.CreateMatHangHandler(khoRepository,transactions,audit)
        );
    }

    public static final class InMemoryInventoryRepository implements IKhoRepository {
        private final Map<Integer, MatHang> items = new ConcurrentHashMap<>();
        private final Map<Integer, TonKho> batches = new ConcurrentHashMap<>();
        private final Map<Integer, Kho> warehouses = new ConcurrentHashMap<>();
        private final Map<Integer, PhieuNhapKho> nhapKhos = new ConcurrentHashMap<>();
        private final Map<Integer, PhieuXuatKho> xuatKhos = new ConcurrentHashMap<>();
        private final Map<Integer, DonDatHang> purchaseOrders = new ConcurrentHashMap<>();
        private final Map<Integer, KiemKe> stocktakes = new ConcurrentHashMap<>();

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
        public Optional<MatHang> getItemById(int id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public Optional<MatHang> getItemByCode(String code) {
            return items.values().stream().filter(i -> Objects.equals(i.getMaItem(), code)).findFirst();
        }

        @Override
        public synchronized MatHang saveItem(MatHang item) {
            if (item.getId() == null || item.getId() == 0) {
                item.setId(itemSeq++);
            }
            items.put(item.getId(), item);
            return item;
        }

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
        public synchronized KiemKe saveStocktake(KiemKe stocktake) {
            if (stocktake.getId() == null || stocktake.getId() == 0) {
                stocktake.setId(stocktakeSeq++);
            }
            stocktakes.put(stocktake.getId(), stocktake);
            return stocktake;
        }

        @Override
        public Optional<KiemKe> getStocktakeById(int id) {
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
        public PhanTrangKetQua<DonThuoc> getPaged(int pageIndex, int pageSize, ITieuChuan<DonThuoc> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<DonThuoc> find(ITieuChuan<DonThuoc> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<DonThuoc> specification) {
            return false;
        }

        @Override
        public int count(ITieuChuan<DonThuoc> specification) {
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
