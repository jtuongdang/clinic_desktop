package vn.clinicmanagement.desktop.modules.auth;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.authorization.DieuHuongQuyenHandler;
import vn.clinicmanagement.application.auth.authenticate.AuthenticateNguoiDungHandler;
import vn.clinicmanagement.application.auth.complete.CompleteDangNhapPhienHandler;
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

/**
 * Lắp ráp nghiệp vụ xác thực và các adapter JDBC cho ứng dụng desktop.
 */
public final class XacThucGhepNoiGoc {
    private XacThucGhepNoiGoc() {}

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

    public static CompleteDangNhapPhienHandler createCompleteLoginSessionHandler() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = new JdbcGiaoDichQuanLy(unitOfWork);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        NhanVienRepository employees = new NhanVienRepository(connections, unitOfWork);
        JdbcKiemToanRepository auditWritePort = new JdbcKiemToanRepository(
                connections, unitOfWork, CoSoDuLieuKetNoi::getConnection);
        return new CompleteDangNhapPhienHandler(employees, transactions, auditWritePort, Clock.systemUTC());
    }

    public static DieuHuongQuyenHandler createNavigationPermissionsHandler() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = new JdbcGiaoDichQuanLy(unitOfWork);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        return new DieuHuongQuyenHandler(new HeThongRepository(connections, unitOfWork));
    }

    public static KiemToanWriteCongKetNoi createAuditWritePort() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = new JdbcGiaoDichQuanLy(unitOfWork);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        return new JdbcKiemToanRepository(connections, unitOfWork, CoSoDuLieuKetNoi::getConnection);
    }

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

    public static PhienVongDoiDichVu createSessionLifecycleService(NguoiDungPhien session) {
        return createSessionLifecycleService(session, createSessionTimeoutPolicy());
    }

    public static PhienVongDoiDichVu createSessionLifecycleService(
            NguoiDungPhien session,
            PhienHetGioChinhSach policy) {
        return new PhienVongDoiDichVu(session, policy, Clock.systemUTC(), createAuditWritePort());
    }
}
