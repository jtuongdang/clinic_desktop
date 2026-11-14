package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;

import java.util.Objects;

/** A reception registration: create/update demographics, persist insurance, and open today's visit. */
public record RegisterBenhNhanCommand(BenhNhan patient, BaoHiem insurance, int roomId,
                                    /** Trường `registrationType`: đăng ký loai. */
                                    /** Trường `reason`: lý do. */
                                    String registrationType, String reason,
                                    /** Trường `payerType`: `payer` loai. */
                                    /** Trường `createdBy`: datao theo. */
                                    DoiTuongKham payerType, boolean priority, int createdBy,
                                    /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
                                    PhanQuyenNguCanh authorizationContext,
                                    /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
                                    KiemToanTacNhanNguCanh auditActorContext) implements Command<DangKyBienNhan> {
    public RegisterBenhNhanCommand {
        Objects.requireNonNull(authorizationContext, "authorizationContext");
        Objects.requireNonNull(auditActorContext, "auditActorContext");
        if (!authorizationContext.isValid()) {
            throw new IllegalArgumentException("A valid authorization context is required.");
        }
        if (createdBy != authorizationContext.employeeId()) {
            throw new IllegalArgumentException("createdBy must match the authenticated actor.");
        }
        if (auditActorContext.employeeId() != authorizationContext.employeeId()
                || auditActorContext.roleId() != authorizationContext.roleId()
                || !auditActorContext.roleCode().equals(authorizationContext.roleCode())
                || auditActorContext.facilityId() != authorizationContext.facilityId()
                || auditActorContext.departmentId() != authorizationContext.departmentId()) {
            throw new IllegalArgumentException("Audit actor must match the authorization context.");
        }
    }
}
