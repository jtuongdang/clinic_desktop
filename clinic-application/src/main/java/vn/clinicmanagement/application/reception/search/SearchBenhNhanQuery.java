package vn.clinicmanagement.application.reception.search;

import vn.clinicmanagement.application.reception.BenhNhanSearchMatHang;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.util.List;

/**
 * Yêu cầu tìm bệnh nhân được bảo vệ. Các ngữ cảnh được chụp lại cho thao tác này.
 */
public record SearchBenhNhanQuery(String keyword, PhanQuyenNguCanh authorizationContext,
                                  KiemToanTacNhanNguCanh auditActorContext) implements Query<List<BenhNhanSearchMatHang>> {
    /** Các điểm gọi cũ bị từ chối an toàn vì không có ngữ cảnh thao tác đã xác thực. */
    @Deprecated
    public SearchBenhNhanQuery(String keyword) { this(keyword, null, null); }
}
