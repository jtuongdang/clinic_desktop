package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record UpdateEmrClinicalRecordCommand(
        int visitId,
        int doctorId,
        String quaTrinhBenhLy,
        String tienSuBanThan,
        String tienSuGiaDinh,
        String khamToanThan,
        String khamBoPhan,
        String tomTatBenhAn,
        String huongDieuTri,
        String loiDanBacSi,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext
) implements Command<UpdateEmrClinicalRecordReceipt> {

    public UpdateEmrClinicalRecordCommand(
            int visitId,
            int doctorId,
            String quaTrinhBenhLy,
            String tienSuBanThan,
            String tienSuGiaDinh,
            String khamToanThan,
            String khamBoPhan,
            String tomTatBenhAn,
            String huongDieuTri,
            String loiDanBacSi,
            AuthorizationContext authorizationContext
    ) {
        this(visitId, doctorId, quaTrinhBenhLy, tienSuBanThan, tienSuGiaDinh,
                khamToanThan, khamBoPhan, tomTatBenhAn, huongDieuTri, loiDanBacSi,
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
