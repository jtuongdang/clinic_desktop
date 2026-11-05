package vn.clinicmanagement.domain.system;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.BaseEntity;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditEntityImmutabilityTest {

    @Test
    void systemAuditRejectsLifecycleMutation() {
        assertLifecycleMutationRejected(NhatKyHeThong::new);
    }

    @Test
    void medicalRecordAuditRejectsLifecycleMutation() {
        assertLifecycleMutationRejected(NhatKyHoSoBenhAn::new);
    }

    private static void assertLifecycleMutationRejected(Supplier<? extends BaseEntity<Integer>> factory) {
        BaseEntity<Integer> entity = factory.get();

        assertThrows(UnsupportedOperationException.class, () -> entity.markAsUpdated(1));
        assertThrows(UnsupportedOperationException.class, () -> entity.softDelete(1));
        assertThrows(UnsupportedOperationException.class, entity::restore);
        assertThrows(UnsupportedOperationException.class, () -> entity.setDeleted(true));
        assertFalse(entity.isDeleted());
    }
}
