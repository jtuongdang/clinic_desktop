package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record DispenseDonThuocCommand(
        /** Trường `prescriptionId`: đơn thuốc mã. */
        int prescriptionId,
        /** Trường `pharmacistId`: `pharmacist` mã. */
        int pharmacistId,
        /** Trường `warehouseId`: kho mã. */
        int warehouseId,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Command<DispenseDonThuocBienNhan> {

    public DispenseDonThuocCommand(
            int prescriptionId,
            int pharmacistId,
            int warehouseId,
            PhanQuyenNguCanh authorizationContext) {
        this(prescriptionId, pharmacistId, warehouseId, authorizationContext, createAuditActor(authorizationContext));
    }

    /** Hàm `createAuditActor`: tạo kiểm toán tacnhan. */
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
