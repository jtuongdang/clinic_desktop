package vn.clinicmanagement.application.inventory.dispense;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record DispenseDonThuocCommand(
        int prescriptionId,
        int pharmacistId,
        int warehouseId,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<DispenseDonThuocBienNhan> {

    public DispenseDonThuocCommand(
            int prescriptionId,
            int pharmacistId,
            int warehouseId,
            PhanQuyenNguCanh authorizationContext) {
        this(prescriptionId, pharmacistId, warehouseId, authorizationContext, createAuditActor(authorizationContext));
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
