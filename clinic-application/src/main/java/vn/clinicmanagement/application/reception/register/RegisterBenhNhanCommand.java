package vn.clinicmanagement.application.reception.register;

import vn.clinicmanagement.application.reception.DangKyBienNhan;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;

import java.util.Objects;

/** Nghiệp vụ tiếp đón: tạo/cập nhật thông tin nhân khẩu học, lưu thẻ bảo hiểm và mở lượt khám hôm nay. */
public record RegisterBenhNhanCommand(
        BenhNhan patient, BaoHiem insurance, int roomId,
        Integer serviceId,
        String registrationType, String reason,
        DoiTuongKham payerType, boolean priority, int createdBy,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<DangKyBienNhan> {
    public RegisterBenhNhanCommand(BenhNhan patient, BaoHiem insurance, int roomId,
                                   String registrationType, String reason,
                                   DoiTuongKham payerType, boolean priority, int createdBy,
                                   PhanQuyenNguCanh authorizationContext,
                                   KiemToanTacNhanNguCanh auditActorContext) {
        this(patient, insurance, roomId, null, registrationType, reason, payerType, priority,
                createdBy, authorizationContext, auditActorContext);
    }

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
