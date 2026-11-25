package vn.clinicmanagement.application.examination.complete;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.HinhThucDieuTri;
import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.KetQuaDieuTri;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CompleteKhamCommand(
        int visitId,
        int doctorId,
        KetQuaDieuTri ketQuaDieuTri,
        HuongXuTri huongXuTri,
        String loiDanBacSi,
        LocalDate ngayHenTaiKham,
        /** Thời điểm xử trí / kết thúc khám do bác sĩ xác nhận (null = giờ hệ thống hiện tại). */
        LocalDateTime thoiGianXuTri,
        /** Hình thức điều trị cập nhật (VD: `NOI_TRU` khi nhập viện). */
        HinhThucDieuTri hinhThucDieuTri,
        /** Hướng điều trị / khoa điều trị (VD: khoa ngoại trú hoặc khoa nội trú). */
        String huongDieuTri,
        /** Số ngày nghỉ hưởng BHXH (khi xử trí Cấp giấy tờ). */
        Integer soNgayNghiHuongBHXH,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<CompleteKhamBienNhan> {

    public CompleteKhamCommand(int visitId, int doctorId, PhanQuyenNguCanh authorizationContext) {
        this(visitId, doctorId, null, null, null, null, null, null, null, null, authorizationContext);
    }

    public CompleteKhamCommand(int visitId, int doctorId, KetQuaDieuTri ketQuaDieuTri,
                               HuongXuTri huongXuTri, String loiDanBacSi, LocalDate ngayHenTaiKham,
                               PhanQuyenNguCanh authorizationContext) {
        this(visitId, doctorId, ketQuaDieuTri, huongXuTri, loiDanBacSi, ngayHenTaiKham,
                null, null, null, null, authorizationContext, createAuditActor(authorizationContext));
    }

    public CompleteKhamCommand(int visitId, int doctorId, KetQuaDieuTri ketQuaDieuTri,
                               HuongXuTri huongXuTri, String loiDanBacSi, LocalDate ngayHenTaiKham,
                               PhanQuyenNguCanh authorizationContext, KiemToanTacNhanNguCanh auditActorContext) {
        this(visitId, doctorId, ketQuaDieuTri, huongXuTri, loiDanBacSi, ngayHenTaiKham,
                null, null, null, null, authorizationContext, auditActorContext);
    }

    public CompleteKhamCommand(int visitId, int doctorId, KetQuaDieuTri ketQuaDieuTri,
                               HuongXuTri huongXuTri, String loiDanBacSi, LocalDate ngayHenTaiKham,
                               LocalDateTime thoiGianXuTri, HinhThucDieuTri hinhThucDieuTri,
                               String huongDieuTri, Integer soNgayNghiHuongBHXH,
                               PhanQuyenNguCanh authorizationContext) {
        this(visitId, doctorId, ketQuaDieuTri, huongXuTri, loiDanBacSi, ngayHenTaiKham,
                thoiGianXuTri, hinhThucDieuTri, huongDieuTri, soNgayNghiHuongBHXH,
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
