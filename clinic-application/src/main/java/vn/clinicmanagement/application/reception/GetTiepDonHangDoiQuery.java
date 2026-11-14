package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;

/** Protected queue and room-summary request. */
public record GetTiepDonHangDoiQuery(LocalDate date, PhanQuyenNguCanh authorizationContext,
                                     /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
                                     KiemToanTacNhanNguCanh auditActorContext) implements Query<TiepDonHangDoiAnhChup> {
    /** Legacy call sites fail closed because they carry no authenticated operation contexts. */
    @Deprecated
    public GetTiepDonHangDoiQuery(LocalDate date) { this(date, null, null); }
}
