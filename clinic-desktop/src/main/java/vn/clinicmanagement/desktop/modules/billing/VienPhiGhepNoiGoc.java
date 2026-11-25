package vn.clinicmanagement.desktop.modules.billing;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.billing.create.CreateHoaDonHandler;
import vn.clinicmanagement.application.billing.get.GetHoaDonTheoLuotKhamHandler;
import vn.clinicmanagement.application.billing.get.GetBenhNhanCongNoHandler;
import vn.clinicmanagement.application.billing.record.RecordThanhToanHandler;
import vn.clinicmanagement.application.billing.refund.RecordHoanTraHandler;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuHoanTra;
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
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    };

    private final CreateHoaDonHandler createInvoiceHandler;
    private final GetHoaDonTheoLuotKhamHandler getInvoiceByVisitHandler;
    private final RecordThanhToanHandler recordPaymentHandler;
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

    public VienPhiGhepNoiGoc(IHoaDonRepository invoiceRepository, ILuotKhamRepository visitRepository,
                             KiemToanWriteCongKetNoi auditWritePort, GiaoDichRanhGioi transactions) {
        this(
                new CreateHoaDonHandler(invoiceRepository, visitRepository, auditWritePort, transactions),
                new GetHoaDonTheoLuotKhamHandler(invoiceRepository, auditWritePort),
                new RecordThanhToanHandler(invoiceRepository, auditWritePort, transactions),
                new GetBenhNhanCongNoHandler(invoiceRepository, auditWritePort)
        );
    }

    public static Services create(
            CreateHoaDonHandler createInvoiceHandler,
            GetHoaDonTheoLuotKhamHandler getInvoiceByVisitHandler,
            RecordThanhToanHandler recordPaymentHandler,
            GetBenhNhanCongNoHandler getPatientDebtHandler) {
        return new Services(createInvoiceHandler, getInvoiceByVisitHandler, recordPaymentHandler, getPatientDebtHandler);
    }

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
                new GetBenhNhanCongNoHandler(invoiceRepository, auditWritePort),
                null,
                new RecordHoanTraHandler(invoiceRepository, auditWritePort)
        );
    }

    public static Services create(IHoaDonRepository invoiceRepository, ILuotKhamRepository visitRepository,
                                  KiemToanWriteCongKetNoi auditWritePort, GiaoDichRanhGioi transactions) {
        Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        Objects.requireNonNull(visitRepository, "visitRepository");
        Objects.requireNonNull(auditWritePort, "auditWritePort");
        Objects.requireNonNull(transactions, "transactions");
        return new Services(
                new CreateHoaDonHandler(invoiceRepository, visitRepository, auditWritePort, transactions),
                new GetHoaDonTheoLuotKhamHandler(invoiceRepository, auditWritePort),
                new RecordThanhToanHandler(invoiceRepository, auditWritePort, transactions),
                new GetBenhNhanCongNoHandler(invoiceRepository, auditWritePort),
                null,
                new RecordHoanTraHandler(invoiceRepository, auditWritePort, transactions)
        );
    }

    public static Services create(vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu sessionLifecycle) {
        return createDefault();
    }

    public static Services createDefault() {
        InMemoryInvoiceRepository invoiceRepo = new InMemoryInvoiceRepository();
        InMemoryVisitRepository visitRepo = new InMemoryVisitRepository();
        return create(invoiceRepo, visitRepo, FALLBACK_AUDIT);
    }

    public CreateHoaDonHandler getCreateInvoiceHandler() {
        return createInvoiceHandler;
    }

    public CreateHoaDonHandler createInvoiceHandler() {
        return createInvoiceHandler;
    }

    public GetHoaDonTheoLuotKhamHandler getGetInvoiceByVisitHandler() {
        return getInvoiceByVisitHandler;
    }

    public GetHoaDonTheoLuotKhamHandler getInvoiceByVisitHandler() {
        return getInvoiceByVisitHandler;
    }

    public RecordThanhToanHandler getRecordPaymentHandler() {
        return recordPaymentHandler;
    }

    public RecordThanhToanHandler recordPaymentHandler() {
        return recordPaymentHandler;
    }

    public GetBenhNhanCongNoHandler getGetPatientDebtHandler() {
        return getPatientDebtHandler;
    }

    public GetBenhNhanCongNoHandler getPatientDebtHandler() {
        return getPatientDebtHandler;
    }

    public GetBenhNhanCongNoHandler patientDebtHandler() {
        return getPatientDebtHandler;
    }

    public Services getServices() {
        return new Services(createInvoiceHandler, getInvoiceByVisitHandler, recordPaymentHandler, getPatientDebtHandler);
    }

    public Services services() {
        return getServices();
    }

    public record Services(
            CreateHoaDonHandler createInvoice,
            GetHoaDonTheoLuotKhamHandler getInvoiceByVisit,
            RecordThanhToanHandler recordPayment,
            GetBenhNhanCongNoHandler patientDebt,
            vn.clinicmanagement.application.billing.VienPhiManHinhReadCongKetNoi screenRead,
            RecordHoanTraHandler recordRefund) {
        public Services(CreateHoaDonHandler createInvoice, GetHoaDonTheoLuotKhamHandler getInvoiceByVisit,
                RecordThanhToanHandler recordPayment, GetBenhNhanCongNoHandler patientDebt) {
            this(createInvoice, getInvoiceByVisit, recordPayment, patientDebt, null, null);
        }
        public Services(CreateHoaDonHandler createInvoice, GetHoaDonTheoLuotKhamHandler getInvoiceByVisit,
                RecordThanhToanHandler recordPayment, GetBenhNhanCongNoHandler patientDebt,
                vn.clinicmanagement.application.billing.VienPhiManHinhReadCongKetNoi screenRead) {
            this(createInvoice, getInvoiceByVisit, recordPayment, patientDebt, screenRead, null);
        }
        public Services withScreenRead(vn.clinicmanagement.application.billing.VienPhiManHinhReadCongKetNoi read) {
            return new Services(createInvoice, getInvoiceByVisit, recordPayment, patientDebt, read, recordRefund);
        }
        public Services withRecordRefund(RecordHoanTraHandler refundHandler) {
            return new Services(createInvoice, getInvoiceByVisit, recordPayment, patientDebt, screenRead, refundHandler);
        }
        public Services {
            Objects.requireNonNull(createInvoice, "createInvoice");
            Objects.requireNonNull(getInvoiceByVisit, "getInvoiceByVisit");
            Objects.requireNonNull(recordPayment, "recordPayment");
            Objects.requireNonNull(patientDebt, "patientDebt");
        }

        public CreateHoaDonHandler getCreateInvoice() {
            return createInvoice;
        }

        public GetHoaDonTheoLuotKhamHandler getGetInvoiceByVisit() {
            return getInvoiceByVisit;
        }

        public RecordThanhToanHandler getRecordPayment() {
            return recordPayment;
        }

        public GetBenhNhanCongNoHandler getPatientDebt() {
            return patientDebt;
        }

        public RecordHoanTraHandler getRecordRefund() {
            return recordRefund;
        }
    }

    /** Cài đặt `IHoaDonRepository` trong bộ nhớ để ghép nối desktop độc lập. */
    public static final class InMemoryInvoiceRepository implements IHoaDonRepository {
        private final Map<Integer, HoaDon> invoices = new ConcurrentHashMap<>();
        private final List<PhieuThu> receipts = new ArrayList<>();
        private final List<PhieuHoanTra> refunds = new ArrayList<>();
        private final List<CongNoBenhNhan> debts = new ArrayList<>();
        private int idSeq = 1;
        private int receiptSeq = 1;
        private int refundSeq = 1;
        private int debtSeq = 1;

        @Override
        public synchronized Optional<HoaDon> getBySoHoaDon(String soHoaDon) {
            if (soHoaDon == null) return Optional.empty();
            return invoices.values().stream().filter(h -> soHoaDon.equals(h.getSoHoaDon())).findFirst();
        }

        @Override
        public synchronized Optional<HoaDon> getByLuotKham(int maLuotKham) {
            return invoices.values().stream().filter(h -> h.getMaLuotKham() == maLuotKham).findFirst();
        }

        @Override
        public synchronized List<HoaDon> getByBenhNhan(int maBenhNhan) {
            return invoices.values().stream().filter(h -> h.getMaBenhNhan() == maBenhNhan).toList();
        }

        @Override
        public synchronized HoaDon save(HoaDon hoaDon) {
            if (hoaDon.getId() == null || hoaDon.getId() == 0) {
                hoaDon.setId(idSeq++);
            }
            invoices.put(hoaDon.getId(), hoaDon);
            return hoaDon;
        }

        @Override
        public synchronized void savePhieuThu(PhieuThu phieuThu) {
            if (phieuThu.getId() == null || phieuThu.getId() == 0) {
                phieuThu.setId(receiptSeq++);
            }
            receipts.add(phieuThu);
        }

        @Override
        public synchronized List<PhieuThu> getPhieuThuByHoaDon(int maHoaDon) {
            return receipts.stream().filter(r -> r.getMaHoaDon() == maHoaDon).toList();
        }

        @Override
        public synchronized void savePhieuHoanTra(PhieuHoanTra phieuHoanTra) {
            if (phieuHoanTra.getId() == null || phieuHoanTra.getId() == 0) {
                phieuHoanTra.setId(refundSeq++);
            }
            refunds.add(phieuHoanTra);
        }

        @Override
        public synchronized List<PhieuHoanTra> getPhieuHoanTraByHoaDon(int maHoaDon) {
            return refunds.stream().filter(r -> r.getMaHoaDon() == maHoaDon).toList();
        }

        @Override
        public synchronized void saveCongNo(CongNoBenhNhan congNo) {
            if (congNo.getId() == null || congNo.getId() == 0) {
                congNo.setId(debtSeq++);
            }
            debts.removeIf(d -> Objects.equals(d.getId(), congNo.getId()));
            debts.add(congNo);
        }

        @Override
        public synchronized Optional<CongNoBenhNhan> getCongNoByHoaDon(int maHoaDon) {
            return debts.stream().filter(d -> d.getMaHoaDon() == maHoaDon).findFirst();
        }

        @Override
        public synchronized List<CongNoBenhNhan> getCongNoByBenhNhan(int maBenhNhan) {
            return debts.stream().filter(d -> d.getMaBenhNhan() == maBenhNhan).toList();
        }

        @Override
        public synchronized Optional<HoaDon> getById(int id) {
            return Optional.ofNullable(invoices.get(id));
        }

        @Override
        public synchronized List<HoaDon> getAll() {
            return new ArrayList<>(invoices.values());
        }

        @Override
        public PhanTrangKetQua<HoaDon> getPaged(int pageIndex, int pageSize, ITieuChuan<HoaDon> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<HoaDon> find(ITieuChuan<HoaDon> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<HoaDon> specification) {
            return false;
        }

        @Override
        public int count(ITieuChuan<HoaDon> specification) {
            return invoices.size();
        }

        @Override
        public void add(HoaDon entity) {
            save(entity);
        }

        @Override
        public void update(HoaDon entity) {
            save(entity);
        }

        @Override
        public void delete(HoaDon entity) {
            if (entity != null && entity.getId() != null) {
                invoices.remove(entity.getId());
            }
        }

        @Override
        public IUnitOfWork getUnitOfWork() {
            return NOOP_UOW;
        }
    }

    private static final IUnitOfWork NOOP_UOW = new IUnitOfWork() {
        @Override public int saveChanges() { return 1; }
        @Override public boolean saveEntities() { return true; }
    };

    /** Cài đặt `ILuotKhamRepository` trong bộ nhớ để ghép nối desktop độc lập. */
    public static final class InMemoryVisitRepository implements ILuotKhamRepository {
        private final Map<Integer, LuotKham> visits = new ConcurrentHashMap<>();
        private int idSeq = 1;

        @Override
        public void add(LuotKham entity) {
            save(entity);
        }

        @Override
        public void update(LuotKham entity) {
            save(entity);
        }

        @Override
        public void delete(LuotKham entity) {
            if (entity != null && entity.getId() != null) {
                visits.remove(entity.getId());
            }
        }

        @Override
        public IUnitOfWork getUnitOfWork() {
            return NOOP_UOW;
        }

        public synchronized LuotKham save(LuotKham visit) {
            if (visit.getId() == null || visit.getId() == 0) {
                visit.setId(idSeq++);
            }
            visits.put(visit.getId(), visit);
            return visit;
        }

        @Override
        public List<LuotKham> getVisitsByDoctor(int maBacSi, LocalDate date) {
            return List.of();
        }

        @Override
        public int getNextQueueNumber(int maCoSo, int maKhoa, LocalDate date) {
            return 1;
        }

        @Override
        public Optional<LuotKham> getById(int id) {
            return Optional.ofNullable(visits.get(id));
        }

        @Override
        public List<LuotKham> getAll() {
            return new ArrayList<>(visits.values());
        }

        @Override
        public PhanTrangKetQua<LuotKham> getPaged(int pageIndex, int pageSize, ITieuChuan<LuotKham> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<LuotKham> find(ITieuChuan<LuotKham> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<LuotKham> specification) {
            return false;
        }

        @Override
        public int count(ITieuChuan<LuotKham> specification) {
            return visits.size();
        }
    }
}
