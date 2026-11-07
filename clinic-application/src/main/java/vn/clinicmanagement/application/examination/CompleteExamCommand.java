package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.KetQuaDieuTri;

import java.time.LocalDate;
import java.util.UUID;

public record CompleteExamCommand(
        int visitId,
        int doctorId,
        KetQuaDieuTri ketQuaDieuTri,
        HuongXuTri huongXuTri,
        String loiDanBacSi,
        LocalDate ngayHenTaiKham,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<CompleteExamReceipt> {

    public CompleteExamCommand(int visitId, int doctorId, AuthorizationContext authorizationContext) {
        this(visitId, doctorId, null, null, null, null, authorizationContext);
    }

    public CompleteExamCommand(int visitId, int doctorId, KetQuaDieuTri ketQuaDieuTri,
                               HuongXuTri huongXuTri, String loiDanBacSi, LocalDate ngayHenTaiKham,
                               AuthorizationContext authorizationContext) {
        this(visitId, doctorId, ketQuaDieuTri, huongXuTri, loiDanBacSi, ngayHenTaiKham,
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
