package vn.clinicmanagement.application.attendance.get;

import vn.clinicmanagement.application.attendance.ChamCongTongHopDuLieu;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;

/**
 * Truy vấn chấm công của nhân viên theo kỳ hoặc khoảng thời gian.
 */
public record GetChamCongTheoKyQuery(
        int employeeId,
        LocalDate fromDate,
        LocalDate toDate,
        Integer kyChamCongId,
        PhanQuyenNguCanh authorizationContext,
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
