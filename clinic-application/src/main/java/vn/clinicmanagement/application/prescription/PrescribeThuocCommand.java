package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.List;
import java.util.UUID;

public record PrescribeThuocCommand(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `doctorId`: bacsi mã. */
        int doctorId,
        /** Trường `facilityId`: cơ sở mã. */
        int facilityId,
        /** Trường `warehouseId`: kho mã. */
        Integer warehouseId,
        String ghiChu,
        String loiDanDonThuoc,
        /** Trường `overrideAllergyWarning`: `override` `Allergy` canhbao. */
        boolean overrideAllergyWarning,
        List<DonThuocMatHangDauVao> items,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
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
