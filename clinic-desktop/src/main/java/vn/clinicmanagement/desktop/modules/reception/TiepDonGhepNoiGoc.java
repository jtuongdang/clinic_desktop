package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.application.reception.get.GetPhieuDangKyKhamHandler;

import vn.clinicmanagement.application.authorization.HoatDongPhienCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.reception.get.GetHoatDongTiepDonPhongHandler;
import vn.clinicmanagement.application.reception.get.GetTiepDonHangDoiHandler;
import vn.clinicmanagement.application.reception.register.RegisterBenhNhanHandler;
import vn.clinicmanagement.application.reception.search.SearchBenhNhanHandler;
import vn.clinicmanagement.application.reception.TiepDonHangDoiCongKetNoi;
import vn.clinicmanagement.desktop.modules.billing.VienPhiGhepNoiGoc;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;
import vn.clinicmanagement.infrastructure.persistence.billing.HoaDonRepository;
import vn.clinicmanagement.infrastructure.persistence.examination.LuotKhamRepository;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.facility.PhongKhamRepository;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepository;
import vn.clinicmanagement.infrastructure.persistence.patient.BaoHiemRepository;
import vn.clinicmanagement.infrastructure.persistence.patient.BenhNhanCoSoRepository;
import vn.clinicmanagement.infrastructure.persistence.patient.BenhNhanRepository;
import vn.clinicmanagement.infrastructure.persistence.reception.TiepDonHangDoiRepository;
import vn.clinicmanagement.infrastructure.persistence.service.ChiDinhDichVuRepository;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository;
import vn.clinicmanagement.infrastructure.persistence.system.HeThongRepository;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Lắp ráp các nghiệp vụ tiếp đón cùng adapter JDBC dùng chung, ranh giới giao dịch, bộ ghi kiểm toán và dịch vụ phân quyền.
 */
public final class TiepDonGhepNoiGoc {

    private final JdbcUnitOfWork unitOfWork;
    private final JdbcGiaoDichQuanLy transactionManager;
    private final JdbcKetNoiNhaCungCap connectionProvider;
    private final JdbcKiemToanRepository auditRepository;
    private final PhanQuyenDichVu authorizationService;

    private final RegisterBenhNhanHandler registerPatientHandler;
    private final SearchBenhNhanHandler searchPatientsHandler;
    private final GetTiepDonHangDoiHandler getReceptionQueueHandler;
    private final GetHoatDongTiepDonPhongHandler getActiveReceptionRoomsHandler;
    private final IChiDinhDichVuRepository serviceRepository;
    private final TiepDonHangDoiCongKetNoi queueRepository;
    private final IHoaDonRepository invoiceRepository;
    private final VienPhiGhepNoiGoc.Services billingServices;

    /** Lắp ráp truy vấn phiếu với cùng quyền và bộ ghi kiểm toán của tiếp đón. */
    public vn.clinicmanagement.application.reception.get.GetPhieuDangKyKhamHandler getPrintSlipHandler() {
        return new vn.clinicmanagement.application.reception.get.GetPhieuDangKyKhamHandler(
                new vn.clinicmanagement.infrastructure.persistence.reception.PhieuDangKyKhamRepository(connectionProvider),
                authorizationService, auditRepository);
    }

    public TiepDonGhepNoiGoc(
            JdbcUnitOfWork unitOfWork,
            JdbcGiaoDichQuanLy transactionManager,
            JdbcKetNoiNhaCungCap connectionProvider,
            JdbcKiemToanRepository auditRepository,
            PhanQuyenDichVu authorizationService) {
        this(unitOfWork, transactionManager, connectionProvider, auditRepository, authorizationService, null);
    }

    public TiepDonGhepNoiGoc(
            JdbcUnitOfWork unitOfWork,
            JdbcGiaoDichQuanLy transactionManager,
            JdbcKetNoiNhaCungCap connectionProvider,
            JdbcKiemToanRepository auditRepository,
            PhanQuyenDichVu authorizationService,
            IChiDinhDichVuRepository serviceRepository) {
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork");
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager");
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
        this.serviceRepository = serviceRepository != null
                ? serviceRepository
                : new ChiDinhDichVuRepository(connectionProvider, unitOfWork);

        BenhNhanRepository patientRepo = new BenhNhanRepository(connectionProvider, unitOfWork);
        BenhNhanCoSoRepository patientFacilityRepo = new BenhNhanCoSoRepository(connectionProvider, unitOfWork);
        BaoHiemRepository insuranceRepo = new BaoHiemRepository(connectionProvider, unitOfWork);
        PhongKhamRepository roomRepo = new PhongKhamRepository(connectionProvider);
        TiepDonHangDoiRepository queueRepo = new TiepDonHangDoiRepository(connectionProvider, unitOfWork);
        this.queueRepository = queueRepo;
        this.invoiceRepository = new HoaDonRepository(connectionProvider, unitOfWork);
        this.billingServices = VienPhiGhepNoiGoc.create(invoiceRepository,
                new LuotKhamRepository(connectionProvider, unitOfWork), auditRepository, transactionManager)
                .withScreenRead(new vn.clinicmanagement.infrastructure.persistence.billing.VienPhiManHinhRepository(connectionProvider));

        this.registerPatientHandler = new RegisterBenhNhanHandler(
                patientRepo, patientFacilityRepo, insuranceRepo, roomRepo, queueRepo,
                transactionManager, auditRepository, authorizationService, this.serviceRepository, invoiceRepository);
        this.searchPatientsHandler = new SearchBenhNhanHandler(
                patientRepo, insuranceRepo, authorizationService, auditRepository);
        this.getReceptionQueueHandler = new GetTiepDonHangDoiHandler(
                queueRepo, authorizationService, auditRepository);
        this.getActiveReceptionRoomsHandler = new GetHoatDongTiepDonPhongHandler(
                roomRepo, authorizationService, auditRepository);
    }

    public TiepDonGhepNoiGoc(PhanQuyenDichVu authorizationService) {
        this(createBundleWithAuthz(authorizationService));
    }

    public TiepDonGhepNoiGoc(HoatDongPhienCongKetNoi sessionPort) {
        this(createBundleWithSession(sessionPort));
    }

    public TiepDonGhepNoiGoc() {
        this((HoatDongPhienCongKetNoi) null);
    }

    private TiepDonGhepNoiGoc(Bundle bundle) {
        this(bundle.uow, bundle.tx, bundle.conn, bundle.audit, bundle.authz);
    }

    private record Bundle(JdbcUnitOfWork uow, JdbcGiaoDichQuanLy tx, JdbcKetNoiNhaCungCap conn,
                          JdbcKiemToanRepository audit, PhanQuyenDichVu authz) {}

    private static Bundle createBundleWithAuthz(PhanQuyenDichVu authorizationService) {
        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = new JdbcGiaoDichQuanLy(uow);
        JdbcKetNoiNhaCungCap conn = new JdbcKetNoiNhaCungCap(tx);
        JdbcKiemToanRepository audit = new JdbcKiemToanRepository(conn, uow, CoSoDuLieuKetNoi::getConnection);
        return new Bundle(uow, tx, conn, audit,
                authorizationService != null ? authorizationService : createDefaultAuthz(null, conn, uow));
    }

    private static Bundle createBundleWithSession(HoatDongPhienCongKetNoi sessionPort) {
        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = new JdbcGiaoDichQuanLy(uow);
        JdbcKetNoiNhaCungCap conn = new JdbcKetNoiNhaCungCap(tx);
        JdbcKiemToanRepository audit = new JdbcKiemToanRepository(conn, uow, CoSoDuLieuKetNoi::getConnection);
        PhanQuyenDichVu authz = createDefaultAuthz(sessionPort, conn, uow);
        return new Bundle(uow, tx, conn, audit, authz);
    }

    private static PhanQuyenDichVu createDefaultAuthz(
            HoatDongPhienCongKetNoi sessionPort,
            JdbcKetNoiNhaCungCap conn,
            JdbcUnitOfWork uow) {
        NhanVienRepository employees = new NhanVienRepository(conn, uow);
        HeThongRepository system = new HeThongRepository(conn, uow);
        HoatDongPhienCongKetNoi port = sessionPort != null ? sessionPort : (sessionId -> Optional.empty());
        return new PhanQuyenDichVu(port, employees, system);
    }

    public static TiepDonGhepNoiGoc create(
            JdbcUnitOfWork unitOfWork,
            JdbcGiaoDichQuanLy transactionManager,
            JdbcKetNoiNhaCungCap connectionProvider,
            JdbcKiemToanRepository auditRepository,
            PhanQuyenDichVu authorizationService) {
        return new TiepDonGhepNoiGoc(unitOfWork, transactionManager, connectionProvider, auditRepository, authorizationService);
    }

    public static TiepDonGhepNoiGoc create(PhanQuyenDichVu authorizationService) {
        return new TiepDonGhepNoiGoc(authorizationService);
    }

    public RegisterBenhNhanHandler getRegisterPatientHandler() {
        return registerPatientHandler;
    }

    public RegisterBenhNhanHandler registerPatientHandler() {
        return registerPatientHandler;
    }

    public SearchBenhNhanHandler getSearchPatientsHandler() {
        return searchPatientsHandler;
    }

    public SearchBenhNhanHandler searchPatientsHandler() {
        return searchPatientsHandler;
    }

    public GetTiepDonHangDoiHandler getGetReceptionQueueHandler() {
        return getReceptionQueueHandler;
    }

    public GetTiepDonHangDoiHandler getReceptionQueueHandler() {
        return getReceptionQueueHandler;
    }

    public GetHoatDongTiepDonPhongHandler getGetActiveReceptionRoomsHandler() {
        return getActiveReceptionRoomsHandler;
    }

    public GetHoatDongTiepDonPhongHandler getActiveReceptionRoomsHandler() {
        return getActiveReceptionRoomsHandler;
    }

    public IChiDinhDichVuRepository getServiceRepository() {
        return serviceRepository;
    }

    public IChiDinhDichVuRepository serviceRepository() {
        return serviceRepository;
    }

    public TiepDonHangDoiCongKetNoi getQueueRepository() {
        return queueRepository;
    }

    /** Dịch vụ viện phí dùng chung kho SQL và ranh giới giao dịch với đăng ký tiếp đón. */
    public VienPhiGhepNoiGoc.Services getBillingServices() {
        return billingServices;
    }

    /** Kho lượt khám trong ngày dùng để đưa hóa đơn mới vào danh sách thu ngân. */
    public TiepDonHangDoiCongKetNoi getBillingQueueRepository() {
        return queueRepository;
    }

    public JdbcUnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    public JdbcUnitOfWork unitOfWork() {
        return unitOfWork;
    }

    public JdbcGiaoDichQuanLy getTransactionManager() {
        return transactionManager;
    }

    public JdbcGiaoDichQuanLy transactionManager() {
        return transactionManager;
    }

    public JdbcKetNoiNhaCungCap getConnectionProvider() {
        return connectionProvider;
    }

    public JdbcKetNoiNhaCungCap connectionProvider() {
        return connectionProvider;
    }

    public JdbcKiemToanRepository getAuditRepository() {
        return auditRepository;
    }

    public JdbcKiemToanRepository auditRepository() {
        return auditRepository;
    }

    public PhanQuyenDichVu getAuthorizationService() {
        return authorizationService;
    }

    public PhanQuyenDichVu authorizationService() {
        return authorizationService;
    }
}
