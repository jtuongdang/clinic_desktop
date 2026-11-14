package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record CancelDonThuocCommand(
        /** Trường `prescriptionId`: đơn thuốc mã. */
        int prescriptionId,
        /** Trường `doctorId`: bacsi mã. */
        int doctorId,
        String lyDoHuy,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Command<CancelDonThuocBienNhan> {

    public CancelDonThuocCommand(
            int prescriptionId,
            int doctorId,
            String lyDoHuy,
            PhanQuyenNguCanh authorizationContext) {
        this(prescriptionId, doctorId, lyDoHuy, authorizationContext, createAuditActor(authorizationContext));
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
