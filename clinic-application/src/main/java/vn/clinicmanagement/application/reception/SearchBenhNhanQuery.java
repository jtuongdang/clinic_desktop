package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.util.List;

/** Protected patient search request. Contexts are captured for this operation. */
public record SearchBenhNhanQuery(String keyword, PhanQuyenNguCanh authorizationContext,
                                  /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
                                  KiemToanTacNhanNguCanh auditActorContext) implements Query<List<BenhNhanSearchMatHang>> {
    /** Legacy call sites fail closed because they carry no authenticated operation contexts. */
    @Deprecated
    public SearchBenhNhanQuery(String keyword) { this(keyword, null, null); }
}
