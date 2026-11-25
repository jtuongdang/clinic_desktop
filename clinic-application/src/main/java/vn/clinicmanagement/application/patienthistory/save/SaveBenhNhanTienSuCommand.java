package vn.clinicmanagement.application.patienthistory.save;

import vn.clinicmanagement.application.patienthistory.BenhNhanTienSuBienNhan;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;

/** Tạo mới khi `historyId` là `null`; nếu không, cập nhật bằng phiên bản lạc quan được cung cấp. */
public record SaveBenhNhanTienSuCommand(
        int patientId,
        Integer historyId,
        Long expectedVersion,
        LoaiTienSu category,
        String narrative,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<BenhNhanTienSuBienNhan> {
}
