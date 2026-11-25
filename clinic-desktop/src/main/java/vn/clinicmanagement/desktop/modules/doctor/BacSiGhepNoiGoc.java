package vn.clinicmanagement.desktop.modules.doctor;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.patienthistory.get.GetBenhNhanTienSuHandler;
import vn.clinicmanagement.application.reception.get.GetBacSiHangDoiHandler;
import vn.clinicmanagement.application.session.PhienHetGioChinhSach;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepository;
import vn.clinicmanagement.infrastructure.persistence.patient.TienSuBenhRepository;
import vn.clinicmanagement.infrastructure.persistence.reception.TiepDonHangDoiRepository;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository;
import vn.clinicmanagement.infrastructure.persistence.system.HeThongRepository;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * Lắp ráp các thao tác đọc JDBC có kiểm tra quyền và ghi kiểm toán cho khu vực bác sĩ.
 */
public final class BacSiGhepNoiGoc {
    private BacSiGhepNoiGoc() {}

    private static final KiemToanWriteCongKetNoi FALLBACK_AUDIT = new KiemToanWriteCongKetNoi() {
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    };

    public static Services createDefault() {
        PhienVongDoiDichVu lifecycle = new PhienVongDoiDichVu(
                NguoiDungPhien.getInstance(),
                new PhienHetGioChinhSach(Duration.ofMinutes(15), Duration.ofHours(12)),
                Clock.systemUTC(),
                FALLBACK_AUDIT);
        return create(lifecycle);
    }

    public static Services create(PhienVongDoiDichVu sessionLifecycle) {
        Objects.requireNonNull(sessionLifecycle, "sessionLifecycle");
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = new JdbcGiaoDichQuanLy(unitOfWork);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        JdbcKiemToanRepository audit = new JdbcKiemToanRepository(
                connections, unitOfWork, CoSoDuLieuKetNoi::getConnection);
        PhanQuyenDichVu authorization = new PhanQuyenDichVu(
                sessionLifecycle,
                new NhanVienRepository(connections, unitOfWork),
                new HeThongRepository(connections, unitOfWork));
        return new Services(
                new GetBacSiHangDoiHandler(
                        new TiepDonHangDoiRepository(connections, unitOfWork), authorization, audit),
                new GetBenhNhanTienSuHandler(
                        new TienSuBenhRepository(connections), authorization, audit));
    }

    public record Services(
            GetBacSiHangDoiHandler doctorQueue,
            GetBenhNhanTienSuHandler patientHistory) {
        public Services {
            Objects.requireNonNull(doctorQueue, "doctorQueue");
            Objects.requireNonNull(patientHistory, "patientHistory");
        }
    }
}
