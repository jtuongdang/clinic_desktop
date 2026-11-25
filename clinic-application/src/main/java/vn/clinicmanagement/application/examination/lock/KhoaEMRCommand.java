package vn.clinicmanagement.application.examination.lock;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record KhoaEMRCommand(
        int visitId,
        int doctorId,
        String signature,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<KhoaEMRBienNhan> {

    public KhoaEMRCommand(int visitId, int doctorId, String signature,
                          PhanQuyenNguCanh authorizationContext) {
        this(visitId, doctorId, signature, authorizationContext,
                authorizationContext != null && authorizationContext.isValid()
                        ? new KiemToanTacNhanNguCanh(
                                authorizationContext.employeeId(),
                                authorizationContext.roleId(),
                                authorizationContext.roleCode(),
                                authorizationContext.facilityId(),
                                authorizationContext.departmentId(),
                                null,
                                UUID.randomUUID())
                        : null);
    }
}
