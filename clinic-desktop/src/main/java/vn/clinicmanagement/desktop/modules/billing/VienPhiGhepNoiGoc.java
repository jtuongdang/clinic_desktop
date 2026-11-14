package vn.clinicmanagement.desktop.modules.billing;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.billing.CreateHoaDonHandler;
import vn.clinicmanagement.application.billing.GetHoaDonTheoLuotKhamHandler;
import vn.clinicmanagement.application.billing.GetBenhNhanCongNoHandler;
import vn.clinicmanagement.application.billing.RecordThanhToanHandler;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.repositories.ITieuChuan;
import vn.clinicmanagement.domain.repositories.PhanTrangKetQua;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Composition root cho phân hệ Thu ngân Viện phí (Billing & Invoicing).
 * Khởi tạo và lắp ráp sạch sẽ CreateInvoiceHandler, GetInvoiceByVisitHandler,
 * RecordPaymentHandler, GetPatientDebtHandler.
 * Tuân thủ nghiêm ngặt Hợp đồng Cách ly 18847587: 100% in-memory, không chạm clinicDb.
 */
public final class VienPhiGhepNoiGoc {

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

    /** Trường `createInvoiceHandler`: tạo hóa đơn xử lý. */
    private final CreateHoaDonHandler createInvoiceHandler;
    /** Trường `getInvoiceByVisitHandler`: lấy hóa đơn theo luotkham xử lý. */
    private final GetHoaDonTheoLuotKhamHandler getInvoiceByVisitHandler;
    /** Trường `recordPaymentHandler`: ghi nhận thanhtoan xử lý. */
    private final RecordThanhToanHandler recordPaymentHandler;
    /** Trường `getPatientDebtHandler`: lấy bệnh nhân congno xử lý. */
    private final GetBenhNhanCongNoHandler getPatientDebtHandler;

    public VienPhiGhepNoiGoc(
            CreateHoaDonHandler createInvoiceHandler,
            GetHoaDonTheoLuotKhamHandler getInvoiceByVisitHandler,
            RecordThanhToanHandler recordPaymentHandler,
            GetBenhNhanCongNoHandler getPatientDebtHandler) {
        this.createInvoiceHandler = Objects.requireNonNull(createInvoiceHandler, "createInvoiceHandler");
        this.getInvoiceByVisitHandler = Objects.requireNonNull(getInvoiceByVisitHandler, "getInvoiceByVisitHandler");
        this.recordPaymentHandler = Objects.requireNonNull(recordPaymentHandler, "recordPaymentHandler");
        this.getPatientDebtHandler = Objects.requireNonNull(getPatientDebtHandler, "getPatientDebtHandler");
    }

    public VienPhiGhepNoiGoc(
            IHoaDonRepository invoiceRepository,
            ILuotKhamRepository visitRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        this(
                new CreateHoaDonHandler(invoiceRepository, visitRepository, auditWritePort),
                new GetHoaDonTheoLuotKhamHandler(invoiceRepository, auditWritePort),
                new RecordThanhToanHandler(invoiceRepository, auditWritePort),
                new GetBenhNhanCongNoHandler(invoiceRepository, auditWritePort)
        );
    }

    /** Hàm `create`: tạo. */
    public static Services create(
            CreateHoaDonHandler createInvoiceHandler,
            GetHoaDonTheoLuotKhamHandler getInvoiceByVisitHandler,
            RecordThanhToanHandler recordPaymentHandler,
            GetBenhNhanCongNoHandler getPatientDebtHandler) {
        return new Services(createInvoiceHandler, getInvoiceByVisitHandler, recordPaymentHandler, getPatientDebtHandler);
    }

    /** Hàm `create`: tạo. */
    public static Services create(
            IHoaDonRepository invoiceRepository,
            ILuotKhamRepository visitRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        Objects.requireNonNull(visitRepository, "visitRepository");
        Objects.requireNonNull(auditWritePort, "auditWritePort");
        return new Services(
                new CreateHoaDonHandler(invoiceRepository, visitRepository, auditWritePort),
                new GetHoaDonTheoLuotKhamHandler(invoiceRepository, auditWritePort),
                new RecordThanhToanHandler(invoiceRepository, auditWritePort),
                new GetBenhNhanCongNoHandler(invoiceRepository, auditWritePort)
        );
    }

    /** Hàm `create`: tạo. */
    public static Services create(vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu sessionLifecycle) {
        return createDefault();
    }

    /** Hàm `createDefault`: tạo macdinh. */
    public static Services createDefault() {
        InMemoryInvoiceRepository invoiceRepo = new InMemoryInvoiceRepository();
        InMemoryVisitRepository visitRepo = new InMemoryVisitRepository();
        return create(invoiceRepo, visitRepo, FALLBACK_AUDIT);
    }

    /** Hàm `getCreateInvoiceHandler`: lấy tạo hóa đơn xử lý. */
    public CreateHoaDonHandler getCreateInvoiceHandler() {
        return createInvoiceHandler;
    }

    /** Hàm `createInvoiceHandler`: tạo hóa đơn xử lý. */
    public CreateHoaDonHandler createInvoiceHandler() {
        return createInvoiceHandler;
    }

    /** Hàm `getGetInvoiceByVisitHandler`: lấy lấy hóa đơn theo luotkham xử lý. */
    public GetHoaDonTheoLuotKhamHandler getGetInvoiceByVisitHandler() {
        return getInvoiceByVisitHandler;
    }

    /** Hàm `getInvoiceByVisitHandler`: lấy hóa đơn theo luotkham xử lý. */
    public GetHoaDonTheoLuotKhamHandler getInvoiceByVisitHandler() {
        return getInvoiceByVisitHandler;
    }

    /** Hàm `getRecordPaymentHandler`: lấy ghi nhận thanhtoan xử lý. */
    public RecordThanhToanHandler getRecordPaymentHandler() {
        return recordPaymentHandler;
    }

    /** Hàm `recordPaymentHandler`: ghi nhận thanhtoan xử lý. */
    public RecordThanhToanHandler recordPaymentHandler() {
        return recordPaymentHandler;
    }

    /** Hàm `getGetPatientDebtHandler`: lấy lấy bệnh nhân congno xử lý. */
    public GetBenhNhanCongNoHandler getGetPatientDebtHandler() {
        return getPatientDebtHandler;
    }

    /** Hàm `getPatientDebtHandler`: lấy bệnh nhân congno xử lý. */
    public GetBenhNhanCongNoHandler getPatientDebtHandler() {
        return getPatientDebtHandler;
    }

    /** Hàm `patientDebtHandler`: bệnh nhân congno xử lý. */
    public GetBenhNhanCongNoHandler patientDebtHandler() {
        return getPatientDebtHandler;
    }

    /** Hàm `getServices`: lấy dịch vụ. */
    public Services getServices() {
        return new Services(createInvoiceHandler, getInvoiceByVisitHandler, recordPaymentHandler, getPatientDebtHandler);
    }

    /** Hàm `services`: dịch vụ. */
    public Services services() {
        return getServices();
    }

    public record Services(
            /** Trường `createInvoice`: tạo hóa đơn. */
            CreateHoaDonHandler createInvoice,
            /** Trường `getInvoiceByVisit`: lấy hóa đơn theo luotkham. */
            GetHoaDonTheoLuotKhamHandler getInvoiceByVisit,
            /** Trường `recordPayment`: ghi nhận thanhtoan. */
            RecordThanhToanHandler recordPayment,
            /** Trường `patientDebt`: bệnh nhân congno. */
            GetBenhNhanCongNoHandler patientDebt) {
        public Services {
            Objects.requireNonNull(createInvoice, "createInvoice");
            Objects.requireNonNull(getInvoiceByVisit, "getInvoiceByVisit");
            Objects.requireNonNull(recordPayment, "recordPayment");
            Objects.requireNonNull(patientDebt, "patientDebt");
        }

        /** Hàm `getCreateInvoice`: lấy tạo hóa đơn. */
        public CreateHoaDonHandler getCreateInvoice() {
            return createInvoice;
        }

        /** Hàm `getGetInvoiceByVisit`: lấy lấy hóa đơn theo luotkham. */
        public GetHoaDonTheoLuotKhamHandler getGetInvoiceByVisit() {
            return getInvoiceByVisit;
        }

        /** Hàm `getRecordPayment`: lấy ghi nhận thanhtoan. */
        public RecordThanhToanHandler getRecordPayment() {
            return recordPayment;
        }

        /** Hàm `getPatientDebt`: lấy bệnh nhân congno. */
        public GetBenhNhanCongNoHandler getPatientDebt() {
            return patientDebt;
        }
    }

    /**
     * In-memory implementation of IHoaDonRepository for isolated desktop wiring.
     */
    public static final class InMemoryInvoiceRepository implements IHoaDonRepository {
        private final Map<Integer, HoaDon> invoices = new ConcurrentHashMap<>();
        private final List<PhieuThu> receipts = new ArrayList<>();
        private final List<CongNoBenhNhan> debts = new ArrayList<>();
        /** Trường `idSeq`: mã `Seq`. */
        private int idSeq = 1;
        /** Trường `receiptSeq`: biên nhận `Seq`. */
        private int receiptSeq = 1;
        /** Trường `debtSeq`: congno `Seq`. */
        private int debtSeq = 1;

        /** Hàm `getBySoHoaDon`: lấy theo so hoa don. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#getBySoHoaDon`. */
        @Override
        public synchronized Optional<HoaDon> getBySoHoaDon(String soHoaDon) {
            if (soHoaDon == null) return Optional.empty();
            return invoices.values().stream().filter(h -> soHoaDon.equals(h.getSoHoaDon())).findFirst();
        }

        /** Hàm `getByLuotKham`: lấy theo luot kham. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#getByLuotKham`. */
        @Override
        public synchronized Optional<HoaDon> getByLuotKham(int maLuotKham) {
            return invoices.values().stream().filter(h -> h.getMaLuotKham() == maLuotKham).findFirst();
        }

        /** Hàm `getByBenhNhan`: lấy theo benh nhãn. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#getByBenhNhan`. */
        @Override
        public synchronized List<HoaDon> getByBenhNhan(int maBenhNhan) {
            return invoices.values().stream().filter(h -> h.getMaBenhNhan() == maBenhNhan).toList();
        }

        /** Hàm `save`: lưu. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#save`. */
        @Override
        public synchronized HoaDon save(HoaDon hoaDon) {
            if (hoaDon.getId() == null || hoaDon.getId() == 0) {
                hoaDon.setId(idSeq++);
            }
            invoices.put(hoaDon.getId(), hoaDon);
            return hoaDon;
        }

        /** Hàm `savePhieuThu`: lưu phiếu thu. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#savePhieuThu`. */
        @Override
        public synchronized void savePhieuThu(PhieuThu phieuThu) {
            if (phieuThu.getId() == null || phieuThu.getId() == 0) {
                phieuThu.setId(receiptSeq++);
            }
            receipts.add(phieuThu);
        }

        /** Hàm `getPhieuThuByHoaDon`: lấy phiếu thu theo hoa don. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#getPhieuThuByHoaDon`. */
        @Override
        public synchronized List<PhieuThu> getPhieuThuByHoaDon(int maHoaDon) {
            return receipts.stream().filter(r -> r.getMaHoaDon() == maHoaDon).toList();
        }

        /** Hàm `saveCongNo`: lưu cong no. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#saveCongNo`. */
        @Override
        public synchronized void saveCongNo(CongNoBenhNhan congNo) {
            if (congNo.getId() == null || congNo.getId() == 0) {
                congNo.setId(debtSeq++);
            }
            debts.removeIf(d -> Objects.equals(d.getId(), congNo.getId()));
            debts.add(congNo);
        }

        /** Hàm `getCongNoByHoaDon`: lấy cong no theo hoa don. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#getCongNoByHoaDon`. */
        @Override
        public synchronized Optional<CongNoBenhNhan> getCongNoByHoaDon(int maHoaDon) {
            return debts.stream().filter(d -> d.getMaHoaDon() == maHoaDon).findFirst();
        }

        /** Hàm `getCongNoByBenhNhan`: lấy cong no theo benh nhãn. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHoaDonRepository#getCongNoByBenhNhan`. */
        @Override
        public synchronized List<CongNoBenhNhan> getCongNoByBenhNhan(int maBenhNhan) {
            return debts.stream().filter(d -> d.getMaBenhNhan() == maBenhNhan).toList();
        }

        /** Hàm `getById`: lấy theo mã. Gốc khai báo tại `IHoaDonRepository#getById`. */
        @Override
        public synchronized Optional<HoaDon> getById(int id) {
            return Optional.ofNullable(invoices.get(id));
        }

        /** Hàm `getAll`: lấy `All`. Gốc khai báo tại `IHoaDonRepository#getAll`. */
        @Override
        public synchronized List<HoaDon> getAll() {
            return new ArrayList<>(invoices.values());
        }

        /** Hàm `getPaged`: lấy phân trang. Gốc khai báo tại `IHoaDonRepository#getPaged`. */
        @Override
        public PhanTrangKetQua<HoaDon> getPaged(int pageIndex, int pageSize, ITieuChuan<HoaDon> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        /** Hàm `find`: tìm. Gốc khai báo tại `IHoaDonRepository#find`. */
        @Override
        public List<HoaDon> find(ITieuChuan<HoaDon> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<HoaDon> specification) {
            return false;
        }

        /** Hàm `count`: dem. Gốc khai báo tại `IHoaDonRepository#count`. */
        @Override
        public int count(ITieuChuan<HoaDon> specification) {
            return invoices.size();
        }

        /** Hàm `add`: thêm. Gốc khai báo tại `IHoaDonRepository#add`. */
        @Override
        public void add(HoaDon entity) {
            save(entity);
        }

        /** Hàm `update`: cập nhật. Gốc khai báo tại `IHoaDonRepository#update`. */
        @Override
        public void update(HoaDon entity) {
            save(entity);
        }

        /** Hàm `delete`: xóa. Gốc khai báo tại `IHoaDonRepository#delete`. */
        @Override
        public void delete(HoaDon entity) {
            if (entity != null && entity.getId() != null) {
                invoices.remove(entity.getId());
            }
        }

        /** Hàm `getUnitOfWork`: lấy đơn vị của làm việc. Gốc khai báo tại `IHoaDonRepository#getUnitOfWork`. */
        @Override
        public IUnitOfWork getUnitOfWork() {
            return NOOP_UOW;
        }
    }

    private static final IUnitOfWork NOOP_UOW = new IUnitOfWork() {
        /** Hàm `saveChanges`: lưu thaydoi. */
        @Override public int saveChanges() { return 1; }
        /** Hàm `saveEntities`: lưu thực thể. */
        @Override public boolean saveEntities() { return true; }
    };

    /**
     * In-memory implementation of ILuotKhamRepository for isolated desktop wiring.
     */
    public static final class InMemoryVisitRepository implements ILuotKhamRepository {
        private final Map<Integer, LuotKham> visits = new ConcurrentHashMap<>();
        /** Trường `idSeq`: mã `Seq`. */
        private int idSeq = 1;

        /** Hàm `add`: thêm. Gốc khai báo tại `ILuotKhamRepository#add`. */
        @Override
        public void add(LuotKham entity) {
            save(entity);
        }

        /** Hàm `update`: cập nhật. Gốc khai báo tại `ILuotKhamRepository#update`. */
        @Override
        public void update(LuotKham entity) {
            save(entity);
        }

        /** Hàm `delete`: xóa. Gốc khai báo tại `ILuotKhamRepository#delete`. */
        @Override
        public void delete(LuotKham entity) {
            if (entity != null && entity.getId() != null) {
                visits.remove(entity.getId());
            }
        }

        /** Hàm `getUnitOfWork`: lấy đơn vị của làm việc. Gốc khai báo tại `ILuotKhamRepository#getUnitOfWork`. */
        @Override
        public IUnitOfWork getUnitOfWork() {
            return NOOP_UOW;
        }

        /** Hàm `save`: lưu. */
        public synchronized LuotKham save(LuotKham visit) {
            if (visit.getId() == null || visit.getId() == 0) {
                visit.setId(idSeq++);
            }
            visits.put(visit.getId(), visit);
            return visit;
        }

        /** Hàm `getVisitsByDoctor`: lấy `Visits` theo bacsi. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.ILuotKhamRepository#getVisitsByDoctor`. */
        @Override
        public List<LuotKham> getVisitsByDoctor(int maBacSi, LocalDate date) {
            return List.of();
        }

        /** Hàm `getNextQueueNumber`: lấy `Next` hangdoi so. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.ILuotKhamRepository#getNextQueueNumber`. */
        @Override
        public int getNextQueueNumber(int maCoSo, int maKhoa, LocalDate date) {
            return 1;
        }

        /** Hàm `getById`: lấy theo mã. Gốc khai báo tại `ILuotKhamRepository#getById`. */
        @Override
        public Optional<LuotKham> getById(int id) {
            return Optional.ofNullable(visits.get(id));
        }

        /** Hàm `getAll`: lấy `All`. Gốc khai báo tại `ILuotKhamRepository#getAll`. */
        @Override
        public List<LuotKham> getAll() {
            return new ArrayList<>(visits.values());
        }

        /** Hàm `getPaged`: lấy phân trang. Gốc khai báo tại `ILuotKhamRepository#getPaged`. */
        @Override
        public PhanTrangKetQua<LuotKham> getPaged(int pageIndex, int pageSize, ITieuChuan<LuotKham> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        /** Hàm `find`: tìm. Gốc khai báo tại `ILuotKhamRepository#find`. */
        @Override
        public List<LuotKham> find(ITieuChuan<LuotKham> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<LuotKham> specification) {
            return false;
        }

        /** Hàm `count`: dem. Gốc khai báo tại `ILuotKhamRepository#count`. */
        @Override
        public int count(ITieuChuan<LuotKham> specification) {
            return visits.size();
        }
    }
}
