package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.application.authorization.HoatDongPhienCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.reception.GetHoatDongTiepDonPhongHandler;
import vn.clinicmanagement.application.reception.GetTiepDonHangDoiHandler;
import vn.clinicmanagement.application.reception.RegisterBenhNhanHandler;
import vn.clinicmanagement.application.reception.SearchBenhNhanHandler;
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
import vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository;
import vn.clinicmanagement.infrastructure.persistence.system.HeThongRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Assembles reception use cases and their shared JDBC persistence adapters,
 * transaction boundaries, audit writer, and authorization service.
 */
public final class TiepDonGhepNoiGoc {

    /** Trường `unitOfWork`: đơn vị của làm việc. */
    private final JdbcUnitOfWork unitOfWork;
    /** Trường `transactionManager`: giao dịch quanly. */
    private final JdbcGiaoDichQuanLy transactionManager;
    /** Trường `connectionProvider`: kết nối nhacungcap. */
    private final JdbcKetNoiNhaCungCap connectionProvider;
    /** Trường `auditRepository`: kiểm toán kho dữ liệu. */
    private final JdbcKiemToanRepository auditRepository;
    /** Trường `authorizationService`: phanquyen dịch vụ. */
    private final PhanQuyenDichVu authorizationService;

    /** Trường `registerPatientHandler`: đăng ký bệnh nhân xử lý. */
    private final RegisterBenhNhanHandler registerPatientHandler;
    /** Trường `searchPatientsHandler`: tìm kiếm bệnh nhân xử lý. */
    private final SearchBenhNhanHandler searchPatientsHandler;
    /** Trường `getReceptionQueueHandler`: lấy tiếp đón hangdoi xử lý. */
    private final GetTiepDonHangDoiHandler getReceptionQueueHandler;
    /** Trường `getActiveReceptionRoomsHandler`: lấy hoạt động tiếp đón phòng xử lý. */
    private final GetHoatDongTiepDonPhongHandler getActiveReceptionRoomsHandler;

    public TiepDonGhepNoiGoc(
            JdbcUnitOfWork unitOfWork,
            JdbcGiaoDichQuanLy transactionManager,
            JdbcKetNoiNhaCungCap connectionProvider,
            JdbcKiemToanRepository auditRepository,
            PhanQuyenDichVu authorizationService) {
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork");
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager");
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");

        BenhNhanRepository patientRepo = new BenhNhanRepository(connectionProvider, unitOfWork);
        BenhNhanCoSoRepository patientFacilityRepo = new BenhNhanCoSoRepository(connectionProvider, unitOfWork);
        BaoHiemRepository insuranceRepo = new BaoHiemRepository(connectionProvider, unitOfWork);
        PhongKhamRepository roomRepo = new PhongKhamRepository(connectionProvider);
        TiepDonHangDoiRepository queueRepo = new TiepDonHangDoiRepository(connectionProvider, unitOfWork);

        this.registerPatientHandler = new RegisterBenhNhanHandler(
                patientRepo, patientFacilityRepo, insuranceRepo, roomRepo, queueRepo,
                transactionManager, auditRepository, authorizationService);
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
                          /** Trường `audit`: kiểm toán. */
                          JdbcKiemToanRepository audit, PhanQuyenDichVu authz) {}

    /** Hàm `createBundleWithAuthz`: tạo `Bundle` voi `Authz`. */
    private static Bundle createBundleWithAuthz(PhanQuyenDichVu authorizationService) {
        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = new JdbcGiaoDichQuanLy(uow);
        JdbcKetNoiNhaCungCap conn = new JdbcKetNoiNhaCungCap(tx);
        JdbcKiemToanRepository audit = new JdbcKiemToanRepository(conn, uow, CoSoDuLieuKetNoi::getConnection);
        return new Bundle(uow, tx, conn, audit,
                authorizationService != null ? authorizationService : createDefaultAuthz(null, conn, uow));
    }

    /** Hàm `createBundleWithSession`: tạo `Bundle` voi phien. */
    private static Bundle createBundleWithSession(HoatDongPhienCongKetNoi sessionPort) {
        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = new JdbcGiaoDichQuanLy(uow);
        JdbcKetNoiNhaCungCap conn = new JdbcKetNoiNhaCungCap(tx);
        JdbcKiemToanRepository audit = new JdbcKiemToanRepository(conn, uow, CoSoDuLieuKetNoi::getConnection);
        PhanQuyenDichVu authz = createDefaultAuthz(sessionPort, conn, uow);
        return new Bundle(uow, tx, conn, audit, authz);
    }

    /** Hàm `createDefaultAuthz`: tạo macdinh `Authz`. */
    private static PhanQuyenDichVu createDefaultAuthz(
            HoatDongPhienCongKetNoi sessionPort,
            JdbcKetNoiNhaCungCap conn,
            JdbcUnitOfWork uow) {
        NhanVienRepository employees = new NhanVienRepository(conn, uow);
        HeThongRepository system = new HeThongRepository(conn, uow);
        HoatDongPhienCongKetNoi port = sessionPort != null ? sessionPort : (sessionId -> Optional.empty());
        return new PhanQuyenDichVu(port, employees, system);
    }

    /** Hàm `create`: tạo. */
    public static TiepDonGhepNoiGoc create(
            JdbcUnitOfWork unitOfWork,
            JdbcGiaoDichQuanLy transactionManager,
            JdbcKetNoiNhaCungCap connectionProvider,
            JdbcKiemToanRepository auditRepository,
            PhanQuyenDichVu authorizationService) {
        return new TiepDonGhepNoiGoc(unitOfWork, transactionManager, connectionProvider, auditRepository, authorizationService);
    }

    /** Hàm `create`: tạo. */
    public static TiepDonGhepNoiGoc create(PhanQuyenDichVu authorizationService) {
        return new TiepDonGhepNoiGoc(authorizationService);
    }

    /** Hàm `getRegisterPatientHandler`: lấy đăng ký bệnh nhân xử lý. */
    public RegisterBenhNhanHandler getRegisterPatientHandler() {
        return registerPatientHandler;
    }

    /** Hàm `registerPatientHandler`: đăng ký bệnh nhân xử lý. */
    public RegisterBenhNhanHandler registerPatientHandler() {
        return registerPatientHandler;
    }

    /** Hàm `getSearchPatientsHandler`: lấy tìm kiếm bệnh nhân xử lý. */
    public SearchBenhNhanHandler getSearchPatientsHandler() {
        return searchPatientsHandler;
    }

    /** Hàm `searchPatientsHandler`: tìm kiếm bệnh nhân xử lý. */
    public SearchBenhNhanHandler searchPatientsHandler() {
        return searchPatientsHandler;
    }

    /** Hàm `getGetReceptionQueueHandler`: lấy lấy tiếp đón hangdoi xử lý. */
    public GetTiepDonHangDoiHandler getGetReceptionQueueHandler() {
        return getReceptionQueueHandler;
    }

    /** Hàm `getReceptionQueueHandler`: lấy tiếp đón hangdoi xử lý. */
    public GetTiepDonHangDoiHandler getReceptionQueueHandler() {
        return getReceptionQueueHandler;
    }

    /** Hàm `getGetActiveReceptionRoomsHandler`: lấy lấy hoạt động tiếp đón phòng xử lý. */
    public GetHoatDongTiepDonPhongHandler getGetActiveReceptionRoomsHandler() {
        return getActiveReceptionRoomsHandler;
    }

    /** Hàm `getActiveReceptionRoomsHandler`: lấy hoạt động tiếp đón phòng xử lý. */
    public GetHoatDongTiepDonPhongHandler getActiveReceptionRoomsHandler() {
        return getActiveReceptionRoomsHandler;
    }

    /** Hàm `getUnitOfWork`: lấy đơn vị của làm việc. */
    public JdbcUnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    /** Hàm `unitOfWork`: đơn vị của làm việc. */
    public JdbcUnitOfWork unitOfWork() {
        return unitOfWork;
    }

    /** Hàm `getTransactionManager`: lấy giao dịch quanly. */
    public JdbcGiaoDichQuanLy getTransactionManager() {
        return transactionManager;
    }

    /** Hàm `transactionManager`: giao dịch quanly. */
    public JdbcGiaoDichQuanLy transactionManager() {
        return transactionManager;
    }

    /** Hàm `getConnectionProvider`: lấy kết nối nhacungcap. */
    public JdbcKetNoiNhaCungCap getConnectionProvider() {
        return connectionProvider;
    }

    /** Hàm `connectionProvider`: kết nối nhacungcap. */
    public JdbcKetNoiNhaCungCap connectionProvider() {
        return connectionProvider;
    }

    /** Hàm `getAuditRepository`: lấy kiểm toán kho dữ liệu. */
    public JdbcKiemToanRepository getAuditRepository() {
        return auditRepository;
    }

    /** Hàm `auditRepository`: kiểm toán kho dữ liệu. */
    public JdbcKiemToanRepository auditRepository() {
        return auditRepository;
    }

    /** Hàm `getAuthorizationService`: lấy phanquyen dịch vụ. */
    public PhanQuyenDichVu getAuthorizationService() {
        return authorizationService;
    }

    /** Hàm `authorizationService`: phanquyen dịch vụ. */
    public PhanQuyenDichVu authorizationService() {
        return authorizationService;
    }
}
