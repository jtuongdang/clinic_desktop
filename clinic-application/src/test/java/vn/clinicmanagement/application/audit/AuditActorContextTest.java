package vn.clinicmanagement.application.audit;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.reception.RegisterPatientCommand;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditActorContextTest {
    private static final UUID CORRELATION_ID =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void acceptsOnlyTypedNormalizedActorAndScopeMetadata() {
        AuditActorContext context = new AuditActorContext(
                7, 31, "RECEPTION", 11, 21, "station-01", CORRELATION_ID);

        assertEquals(7, context.employeeId());
        assertEquals(31, context.roleId());
        assertEquals("RECEPTION", context.roleCode());
        assertEquals(11, context.facilityId());
        assertEquals(21, context.departmentId());
        assertEquals("station-01", context.workstationId());
        assertEquals(CORRELATION_ID, context.correlationId());
    }

    @Test
    void permitsAnAbsentOrBlankOptionalWorkstation() {
        assertNull(new AuditActorContext(7, 31, "RECEPTION", 11, 21, null, CORRELATION_ID).workstationId());
        assertNull(new AuditActorContext(7, 31, "RECEPTION", 11, 21, " ", CORRELATION_ID).workstationId());
    }

    @Test
    void rejectsInvalidIdentifiersRoleCodeWorkstationAndCorrelation() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuditActorContext(0, 31, "RECEPTION", 11, 21, null, CORRELATION_ID));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditActorContext(7, 0, "RECEPTION", 11, 21, null, CORRELATION_ID));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditActorContext(7, 31, "Reception", 11, 21, null, CORRELATION_ID));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditActorContext(7, 31, "RECEPTION", 0, 21, null, CORRELATION_ID));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditActorContext(7, 31, "RECEPTION", 11, 0, null, CORRELATION_ID));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditActorContext(7, 31, "RECEPTION", 11, 21, "station 01", CORRELATION_ID));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditActorContext(7, 31, "RECEPTION", 11, 21, null, null));
    }

    @Test
    void registrationCommandRequiresItsCreatorToMatchTheAuditActor() {
        AuditActorContext context = new AuditActorContext(7, 31, "RECEPTION", 11, 21, null, CORRELATION_ID);

        assertThrows(IllegalArgumentException.class, () -> new RegisterPatientCommand(
                null, null, 3, "synthetic", "synthetic", DoiTuongKham.THU_PHI, false, 9, context));
    }
}
