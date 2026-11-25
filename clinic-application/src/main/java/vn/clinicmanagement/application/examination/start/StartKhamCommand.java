package vn.clinicmanagement.application.examination.start;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record StartKhamCommand(
        int visitId,
        int doctorId,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<StartKhamBienNhan> {

    public StartKhamCommand(int visitId, int doctorId, PhanQuyenNguCanh authorizationContext) {
        this(visitId, doctorId, authorizationContext,
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
