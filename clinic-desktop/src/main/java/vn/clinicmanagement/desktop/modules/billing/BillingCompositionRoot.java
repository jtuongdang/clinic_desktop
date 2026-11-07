package vn.clinicmanagement.desktop.modules.billing;

import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.billing.CreateInvoiceHandler;
import vn.clinicmanagement.application.billing.GetInvoiceByVisitHandler;
import vn.clinicmanagement.application.billing.GetPatientDebtHandler;
import vn.clinicmanagement.application.billing.RecordPaymentHandler;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.repositories.ISpecification;
import vn.clinicmanagement.domain.repositories.PagedResult;

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
public final class BillingCompositionRoot {

    private static final AuditWritePort FALLBACK_AUDIT = new AuditWritePort() {
        @Override public void appendInCurrentTransaction(SystemAuditEntry entry) {}
        @Override public void appendIndependently(SystemAuditEntry entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}
        @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
    };

    private final CreateInvoiceHandler createInvoiceHandler;
    private final GetInvoiceByVisitHandler getInvoiceByVisitHandler;
    private final RecordPaymentHandler recordPaymentHandler;
    private final GetPatientDebtHandler getPatientDebtHandler;

    public BillingCompositionRoot(
            CreateInvoiceHandler createInvoiceHandler,
            GetInvoiceByVisitHandler getInvoiceByVisitHandler,
            RecordPaymentHandler recordPaymentHandler,
            GetPatientDebtHandler getPatientDebtHandler) {
        this.createInvoiceHandler = Objects.requireNonNull(createInvoiceHandler, "createInvoiceHandler");
        this.getInvoiceByVisitHandler = Objects.requireNonNull(getInvoiceByVisitHandler, "getInvoiceByVisitHandler");
        this.recordPaymentHandler = Objects.requireNonNull(recordPaymentHandler, "recordPaymentHandler");
        this.getPatientDebtHandler = Objects.requireNonNull(getPatientDebtHandler, "getPatientDebtHandler");
    }

    public BillingCompositionRoot(
            IHoaDonRepository invoiceRepository,
            ILuotKhamRepository visitRepository,
            AuditWritePort auditWritePort) {
        this(
                new CreateInvoiceHandler(invoiceRepository, visitRepository, auditWritePort),
                new GetInvoiceByVisitHandler(invoiceRepository, auditWritePort),
                new RecordPaymentHandler(invoiceRepository, auditWritePort),
                new GetPatientDebtHandler(invoiceRepository, auditWritePort)
        );
    }

    public static Services create(
            CreateInvoiceHandler createInvoiceHandler,
            GetInvoiceByVisitHandler getInvoiceByVisitHandler,
            RecordPaymentHandler recordPaymentHandler,
            GetPatientDebtHandler getPatientDebtHandler) {
        return new Services(createInvoiceHandler, getInvoiceByVisitHandler, recordPaymentHandler, getPatientDebtHandler);
    }

    public static Services create(
            IHoaDonRepository invoiceRepository,
            ILuotKhamRepository visitRepository,
            AuditWritePort auditWritePort) {
        Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        Objects.requireNonNull(visitRepository, "visitRepository");
        Objects.requireNonNull(auditWritePort, "auditWritePort");
        return new Services(
                new CreateInvoiceHandler(invoiceRepository, visitRepository, auditWritePort),
                new GetInvoiceByVisitHandler(invoiceRepository, auditWritePort),
                new RecordPaymentHandler(invoiceRepository, auditWritePort),
                new GetPatientDebtHandler(invoiceRepository, auditWritePort)
        );
    }

    public static Services create(vn.clinicmanagement.desktop.core.session.SessionLifecycleService sessionLifecycle) {
        return createDefault();
    }

    public static Services createDefault() {
        InMemoryInvoiceRepository invoiceRepo = new InMemoryInvoiceRepository();
        InMemoryVisitRepository visitRepo = new InMemoryVisitRepository();
        return create(invoiceRepo, visitRepo, FALLBACK_AUDIT);
    }

    public CreateInvoiceHandler getCreateInvoiceHandler() {
        return createInvoiceHandler;
    }

    public CreateInvoiceHandler createInvoiceHandler() {
        return createInvoiceHandler;
    }

    public GetInvoiceByVisitHandler getGetInvoiceByVisitHandler() {
        return getInvoiceByVisitHandler;
    }

    public GetInvoiceByVisitHandler getInvoiceByVisitHandler() {
        return getInvoiceByVisitHandler;
    }

    public RecordPaymentHandler getRecordPaymentHandler() {
        return recordPaymentHandler;
    }

    public RecordPaymentHandler recordPaymentHandler() {
        return recordPaymentHandler;
    }

    public GetPatientDebtHandler getGetPatientDebtHandler() {
        return getPatientDebtHandler;
    }

    public GetPatientDebtHandler getPatientDebtHandler() {
        return getPatientDebtHandler;
    }

    public GetPatientDebtHandler patientDebtHandler() {
        return getPatientDebtHandler;
    }

    public Services getServices() {
        return new Services(createInvoiceHandler, getInvoiceByVisitHandler, recordPaymentHandler, getPatientDebtHandler);
    }

    public Services services() {
        return getServices();
    }

    public record Services(
            CreateInvoiceHandler createInvoice,
            GetInvoiceByVisitHandler getInvoiceByVisit,
            RecordPaymentHandler recordPayment,
            GetPatientDebtHandler patientDebt) {
        public Services {
            Objects.requireNonNull(createInvoice, "createInvoice");
            Objects.requireNonNull(getInvoiceByVisit, "getInvoiceByVisit");
            Objects.requireNonNull(recordPayment, "recordPayment");
            Objects.requireNonNull(patientDebt, "patientDebt");
        }

        public CreateInvoiceHandler getCreateInvoice() {
            return createInvoice;
        }

        public GetInvoiceByVisitHandler getGetInvoiceByVisit() {
            return getInvoiceByVisit;
        }

        public RecordPaymentHandler getRecordPayment() {
            return recordPayment;
        }

        public GetPatientDebtHandler getPatientDebt() {
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
        private int idSeq = 1;
        private int receiptSeq = 1;
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
        public PagedResult<HoaDon> getPaged(int pageIndex, int pageSize, ISpecification<HoaDon> specification) {
            return new PagedResult<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<HoaDon> find(ISpecification<HoaDon> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ISpecification<HoaDon> specification) {
            return false;
        }

        @Override
        public int count(ISpecification<HoaDon> specification) {
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

    /**
     * In-memory implementation of ILuotKhamRepository for isolated desktop wiring.
     */
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
        public PagedResult<LuotKham> getPaged(int pageIndex, int pageSize, ISpecification<LuotKham> specification) {
            return new PagedResult<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<LuotKham> find(ISpecification<LuotKham> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ISpecification<LuotKham> specification) {
            return false;
        }

        @Override
        public int count(ISpecification<LuotKham> specification) {
            return visits.size();
        }
    }
}
