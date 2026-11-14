package vn.clinicmanagement.desktop.modules.doctor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.RoleChoice;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.WorkScopeChoice;
import vn.clinicmanagement.application.auth.DaHoanTatDangNhapPhien;
import vn.clinicmanagement.application.session.PhienHetGioChinhSach;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BacSiGhepNoiGocTest {
    private static final KiemToanWriteCongKetNoi NOOP_AUDIT = new KiemToanWriteCongKetNoi() {
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    };

    private PhienVongDoiDichVu lifecycle;

    @AfterEach
    void cleanUp() {
        NguoiDungPhien.getInstance().clearSession();
    }

    @Test
    void composesGuardedQueueAndHistoryHandlersWithoutOpeningDatabaseConnections() {
        lifecycle = new PhienVongDoiDichVu(
                NguoiDungPhien.getInstance(),
                new PhienHetGioChinhSach(Duration.ofMinutes(15), Duration.ofHours(12)),
                Clock.systemUTC(),
                NOOP_AUDIT);
        lifecycle.startSession(new DaHoanTatDangNhapPhien(
                701, "synthetic-doctor", "Synthetic Doctor",
                new RoleChoice(42, "DOCTOR", "Synthetic Doctor"),
                new WorkScopeChoice(11, "Synthetic Facility", 21, "Synthetic Department"),
                UUID.randomUUID(),
                Instant.now()));

        BacSiGhepNoiGoc.Services services = BacSiGhepNoiGoc.create(lifecycle);

        assertNotNull(services.doctorQueue());
        assertNotNull(services.patientHistory());
    }

    @Test
    void rejectsNullDependencies() {
        lifecycle = new PhienVongDoiDichVu(
                NguoiDungPhien.getInstance(),
                new PhienHetGioChinhSach(Duration.ofMinutes(15), Duration.ofHours(12)),
                Clock.systemUTC(),
                NOOP_AUDIT);
        BacSiGhepNoiGoc.Services services = BacSiGhepNoiGoc.create(lifecycle);

        assertThrows(NullPointerException.class, () -> BacSiGhepNoiGoc.create(null));
        assertThrows(NullPointerException.class,
                () -> new BacSiGhepNoiGoc.Services(null, services.patientHistory()));
        assertThrows(NullPointerException.class,
                () -> new BacSiGhepNoiGoc.Services(services.doctorQueue(), null));
    }
}
