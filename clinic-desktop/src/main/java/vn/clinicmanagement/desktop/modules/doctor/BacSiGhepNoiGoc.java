package vn.clinicmanagement.desktop.modules.doctor;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.patienthistory.GetBenhNhanTienSuHandler;
import vn.clinicmanagement.application.reception.GetBacSiHangDoiHandler;
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

/** Builds the guarded, audited JDBC reads used by the Doctor station. */
public final class BacSiGhepNoiGoc {
    private BacSiGhepNoiGoc() {}

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

    /** Hàm `createDefault`: tạo macdinh. */
    public static Services createDefault() {
        PhienVongDoiDichVu lifecycle = new PhienVongDoiDichVu(
                NguoiDungPhien.getInstance(),
                new PhienHetGioChinhSach(Duration.ofMinutes(15), Duration.ofHours(12)),
                Clock.systemUTC(),
                FALLBACK_AUDIT);
        return create(lifecycle);
    }

    /** Hàm `create`: tạo. */
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
            /** Trường `doctorQueue`: bacsi hangdoi. */
            GetBacSiHangDoiHandler doctorQueue,
            /** Trường `patientHistory`: bệnh nhân tiensu. */
            GetBenhNhanTienSuHandler patientHistory) {
        public Services {
            Objects.requireNonNull(doctorQueue, "doctorQueue");
            Objects.requireNonNull(patientHistory, "patientHistory");
        }
    }
}
