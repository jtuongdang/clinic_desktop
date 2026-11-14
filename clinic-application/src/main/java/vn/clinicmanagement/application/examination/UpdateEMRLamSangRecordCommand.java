package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record UpdateEMRLamSangRecordCommand(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `doctorId`: bacsi mã. */
        int doctorId,
        String quaTrinhBenhLy,
        String tienSuBanThan,
        String tienSuGiaDinh,
        String khamToanThan,
        String khamBoPhan,
        String tomTatBenhAn,
        String huongDieuTri,
        String loiDanBacSi,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<UpdateEMRLamSangRecordBienNhan> {

    public UpdateEMRLamSangRecordCommand(
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
            PhanQuyenNguCanh authorizationContext
    ) {
        this(visitId, doctorId, quaTrinhBenhLy, tienSuBanThan, tienSuGiaDinh,
                khamToanThan, khamBoPhan, tomTatBenhAn, huongDieuTri, loiDanBacSi,
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
