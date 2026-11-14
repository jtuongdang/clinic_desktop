package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;

/**
 * Truy vấn chấm công của nhân viên theo kỳ hoặc khoảng thời gian.
 */
public record GetChamCongTheoKyQuery(
        /** Trường `employeeId`: nhân viên mã. */
        int employeeId,
        /** Trường `fromDate`: tu ngày. */
        LocalDate fromDate,
        /** Trường `toDate`: den ngày. */
        LocalDate toDate,
        /** Trường `kyChamCongId`: `ky` cham cong mã. */
        Integer kyChamCongId,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext
) implements Query<ChamCongTongHopDuLieu> {

    public GetChamCongTheoKyQuery(int employeeId, LocalDate fromDate, LocalDate toDate) {
        this(employeeId, fromDate, toDate, null, null, null);
    }

    public GetChamCongTheoKyQuery(int employeeId, LocalDate fromDate, LocalDate toDate,
                                     PhanQuyenNguCanh authorizationContext) {
        this(employeeId, fromDate, toDate, null, authorizationContext, null);
    }
}
