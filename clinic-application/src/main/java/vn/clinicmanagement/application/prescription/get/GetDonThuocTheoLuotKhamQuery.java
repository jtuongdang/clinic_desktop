package vn.clinicmanagement.application.prescription.get;

import vn.clinicmanagement.application.prescription.DonThuocDuLieu;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.util.List;
import java.util.UUID;

public record GetDonThuocTheoLuotKhamQuery(
        int visitId,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Query<List<DonThuocDuLieu>> {

    public GetDonThuocTheoLuotKhamQuery(int visitId, PhanQuyenNguCanh authorizationContext) {
        this(visitId, authorizationContext, createAuditActor(authorizationContext));
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
