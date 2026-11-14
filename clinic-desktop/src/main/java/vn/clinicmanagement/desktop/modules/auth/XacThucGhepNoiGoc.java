package vn.clinicmanagement.desktop.modules.auth;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.authorization.DieuHuongQuyenHandler;
import vn.clinicmanagement.application.auth.AuthenticateNguoiDungHandler;
import vn.clinicmanagement.application.auth.CompleteDangNhapPhienHandler;
import vn.clinicmanagement.application.auth.PBKDF2MatKhauBoBam;
import vn.clinicmanagement.application.session.PhienHetGioChinhSach;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepository;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository;
import vn.clinicmanagement.infrastructure.persistence.system.HeThongRepository;

import java.time.Clock;

/** Builds the authentication use case and its JDBC adapters for the desktop application. */
public final class XacThucGhepNoiGoc {
    private XacThucGhepNoiGoc() {}

    /** Hàm `createHandler`: tạo xử lý. */
    public static AuthenticateNguoiDungHandler createHandler() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = new JdbcGiaoDichQuanLy(unitOfWork);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        NhanVienRepository employees = new NhanVienRepository(connections, unitOfWork);
        JdbcKiemToanRepository auditWritePort = new JdbcKiemToanRepository(
                connections, unitOfWork, CoSoDuLieuKetNoi::getConnection);
        return new AuthenticateNguoiDungHandler(
                employees, employees, new PBKDF2MatKhauBoBam(), transactions, auditWritePort, Clock.systemUTC());
    }

    /** Hàm `createCompleteLoginSessionHandler`: tạo hoàn tất dangnhap phien xử lý. */
    public static CompleteDangNhapPhienHandler createCompleteLoginSessionHandler() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = new JdbcGiaoDichQuanLy(unitOfWork);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        NhanVienRepository employees = new NhanVienRepository(connections, unitOfWork);
        JdbcKiemToanRepository auditWritePort = new JdbcKiemToanRepository(
                connections, unitOfWork, CoSoDuLieuKetNoi::getConnection);
        return new CompleteDangNhapPhienHandler(employees, transactions, auditWritePort, Clock.systemUTC());
    }

    /** Hàm `createNavigationPermissionsHandler`: tạo dieuhuong quyền xử lý. */
    public static DieuHuongQuyenHandler createNavigationPermissionsHandler() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = new JdbcGiaoDichQuanLy(unitOfWork);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        return new DieuHuongQuyenHandler(new HeThongRepository(connections, unitOfWork));
    }

    /** Hàm `createAuditWritePort`: tạo kiểm toán ghi congketnoi. */
    public static KiemToanWriteCongKetNoi createAuditWritePort() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = new JdbcGiaoDichQuanLy(unitOfWork);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        return new JdbcKiemToanRepository(connections, unitOfWork, CoSoDuLieuKetNoi::getConnection);
    }

    /** Hàm `createSessionTimeoutPolicy`: tạo phien hetgio chinhsach. */
    public static PhienHetGioChinhSach createSessionTimeoutPolicy() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = new JdbcGiaoDichQuanLy(unitOfWork);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        PhienHetGioChinhSach configured = PhienHetGioChinhSach.from(new HeThongRepository(connections, unitOfWork));
        return new PhienHetGioChinhSach(
                configured.idleTimeout().compareTo(java.time.Duration.ofMinutes(15)) > 0
                        ? java.time.Duration.ofMinutes(15) : configured.idleTimeout(),
                configured.absoluteTimeout().compareTo(java.time.Duration.ofHours(12)) > 0
                        ? java.time.Duration.ofHours(12) : configured.absoluteTimeout());
    }

    /** Hàm `createSessionLifecycleService`: tạo phien vongdoi dịch vụ. */
    public static PhienVongDoiDichVu createSessionLifecycleService(NguoiDungPhien session) {
        return createSessionLifecycleService(session, createSessionTimeoutPolicy());
    }

    /** Hàm `createSessionLifecycleService`: tạo phien vongdoi dịch vụ. */
    public static PhienVongDoiDichVu createSessionLifecycleService(
            NguoiDungPhien session,
            PhienHetGioChinhSach policy) {
        return new PhienVongDoiDichVu(session, policy, Clock.systemUTC(), createAuditWritePort());
    }
}
