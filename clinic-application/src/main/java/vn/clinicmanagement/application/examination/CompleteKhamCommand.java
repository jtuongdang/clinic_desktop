package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.KetQuaDieuTri;

import java.time.LocalDate;
import java.util.UUID;

public record CompleteKhamCommand(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `doctorId`: bacsi mã. */
        int doctorId,
        KetQuaDieuTri ketQuaDieuTri,
        HuongXuTri huongXuTri,
        String loiDanBacSi,
        /** Trường `ngayHenTaiKham`: `ngay` hen `Tai` kham. */
        LocalDate ngayHenTaiKham,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Command<CompleteKhamBienNhan> {

    public CompleteKhamCommand(int visitId, int doctorId, PhanQuyenNguCanh authorizationContext) {
        this(visitId, doctorId, null, null, null, null, authorizationContext);
    }

    public CompleteKhamCommand(int visitId, int doctorId, KetQuaDieuTri ketQuaDieuTri,
                               HuongXuTri huongXuTri, String loiDanBacSi, LocalDate ngayHenTaiKham,
                               PhanQuyenNguCanh authorizationContext) {
        this(visitId, doctorId, ketQuaDieuTri, huongXuTri, loiDanBacSi, ngayHenTaiKham,
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
