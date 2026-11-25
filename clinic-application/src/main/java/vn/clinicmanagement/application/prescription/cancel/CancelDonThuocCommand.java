package vn.clinicmanagement.application.prescription.cancel;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record CancelDonThuocCommand(
        int prescriptionId,
        int doctorId,
        String lyDoHuy,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<CancelDonThuocBienNhan> {

    public CancelDonThuocCommand(
            int prescriptionId,
            int doctorId,
            String lyDoHuy,
            PhanQuyenNguCanh authorizationContext) {
        this(prescriptionId, doctorId, lyDoHuy, authorizationContext, createAuditActor(authorizationContext));
    }

    private static KiemToanTacNhanNguCanh createAuditActor(PhanQuyenNguCanh context) {
        return context != null && context.isValid()
                ? new KiemToanTacNhanNguCanh(
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
