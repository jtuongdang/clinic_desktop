package vn.clinicmanagement.application.session;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.repositories.IHeThongRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PhienHetGioChinhSachTest {
    @Test
    void readsApprovedSettingsFromSystemRepository() {
        PhienHetGioChinhSach policy = PhienHetGioChinhSach.from(repository("15", "12"));
        assertEquals(15, policy.idleTimeout().toMinutes());
        assertEquals(12, policy.absoluteTimeout().toHours());
    }

    @Test
    void missingInvalidAndUnboundedValuesUseFiniteApprovedDefaults() {
        PhienHetGioChinhSach missing = PhienHetGioChinhSach.from(repository(null, null));
        PhienHetGioChinhSach invalid = PhienHetGioChinhSach.from(repository("0", "999999999999999999999"));
        assertEquals(15, missing.idleTimeout().toMinutes());
        assertEquals(12, missing.absoluteTimeout().toHours());
        assertEquals(missing, invalid);
        assertThrows(IllegalArgumentException.class,
                () -> new PhienHetGioChinhSach(java.time.Duration.ZERO, java.time.Duration.ofHours(1)));
    }

    private IHeThongRepository repository(String idle, String absolute) {
        return new IHeThongRepository() {
            @Override public List<vn.clinicmanagement.domain.system.MaTranPhanQuyen> getMatrixByRoleId(int roleId) { return List.of(); }
            @Override public Optional<String> getSystemParameterValue(String key) {
                return switch (key) {
                    case PhienHetGioChinhSach.IDLE_TIMEOUT_KEY -> Optional.ofNullable(idle);
                    case PhienHetGioChinhSach.ABSOLUTE_TIMEOUT_KEY -> Optional.ofNullable(absolute);
                    default -> Optional.empty();
                };
            }
        };
    }
}
