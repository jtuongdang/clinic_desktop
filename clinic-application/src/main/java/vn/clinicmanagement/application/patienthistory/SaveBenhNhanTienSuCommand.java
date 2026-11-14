package vn.clinicmanagement.application.patienthistory;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;

/** Create when historyId is null; otherwise update using the supplied optimistic version. */
public record SaveBenhNhanTienSuCommand(
        /** Trường `patientId`: bệnh nhân mã. */
        int patientId,
        /** Trường `historyId`: tiensu mã. */
        Integer historyId,
        Long expectedVersion,
        /** Trường `category`: danhmuc. */
        LoaiTienSu category,
        String narrative,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Command<BenhNhanTienSuBienNhan> {
}
