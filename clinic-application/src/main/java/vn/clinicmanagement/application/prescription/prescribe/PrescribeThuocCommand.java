package vn.clinicmanagement.application.prescription.prescribe;

import vn.clinicmanagement.application.prescription.DonThuocMatHangDauVao;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.List;
import java.util.UUID;

public record PrescribeThuocCommand(
        int visitId,
        int doctorId,
        int facilityId,
        Integer warehouseId,
        String ghiChu,
        String loiDanDonThuoc,
        boolean overrideAllergyWarning,
        List<DonThuocMatHangDauVao> items,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<PrescribeThuocBienNhan> {

    public PrescribeThuocCommand(
            int visitId,
            int doctorId,
            int facilityId,
            Integer warehouseId,
            String ghiChu,
            String loiDanDonThuoc,
            boolean overrideAllergyWarning,
            List<DonThuocMatHangDauVao> items,
            PhanQuyenNguCanh authorizationContext) {
        this(visitId, doctorId, facilityId, warehouseId, ghiChu, loiDanDonThuoc, overrideAllergyWarning, items,
                authorizationContext, createAuditActor(authorizationContext));
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
