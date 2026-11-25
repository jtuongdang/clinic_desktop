package vn.clinicmanagement.desktop.modules.examination;

import java.util.UUID;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;

/** Chỉ dùng danh tính của phiên đăng nhập hợp lệ, không tự thay bằng ID mặc định. */
public final class KhamBenhPhienNguCanh {
    private KhamBenhPhienNguCanh() {}
    public static PhanQuyenNguCanh current() {
        NguoiDungPhien session = NguoiDungPhien.getInstance();
        if (!session.isLoggedOn() || session.isLocked() || session.getSessionId() == null) return null;
        try {
            var context = new PhanQuyenNguCanh(session.getUserId(), session.getRoleId(),
                    session.getRoleCode(), session.getFacilityId(), session.getDepartmentId(),
                    UUID.fromString(session.getSessionId()));
            return context.isValid() ? context : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
