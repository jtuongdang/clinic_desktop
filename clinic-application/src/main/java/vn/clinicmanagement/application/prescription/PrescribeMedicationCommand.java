package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.List;
import java.util.UUID;

public record PrescribeMedicationCommand(
        int visitId,
        int doctorId,
        int facilityId,
        Integer warehouseId,
        String ghiChu,
        String loiDanDonThuoc,
        boolean overrideAllergyWarning,
        List<PrescriptionItemInput> items,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<PrescribeMedicationReceipt> {

    public PrescribeMedicationCommand(
            int visitId,
            int doctorId,
            int facilityId,
            Integer warehouseId,
            String ghiChu,
            String loiDanDonThuoc,
            boolean overrideAllergyWarning,
            List<PrescriptionItemInput> items,
            AuthorizationContext authorizationContext) {
        this(visitId, doctorId, facilityId, warehouseId, ghiChu, loiDanDonThuoc, overrideAllergyWarning, items,
                authorizationContext, createAuditActor(authorizationContext));
    }

    private static AuditActorContext createAuditActor(AuthorizationContext context) {
        return context != null && context.isValid()
                ? new AuditActorContext(
                        context.employeeId(),
                        context.roleId(),
                        context.roleCode(),
                        context.facilityId(),
                        context.departmentId(),
                        null,
                        UUID.randomUUID())
                : null;
    }
}
