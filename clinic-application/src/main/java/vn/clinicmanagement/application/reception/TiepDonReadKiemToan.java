package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenQuyetDinh;
import vn.clinicmanagement.application.authorization.PhanQuyenYeuCau;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;

import java.time.Instant;
import java.util.Objects;

/** Dùng chung cơ chế mặc định từ chối, kiểm tra tác nhân cho từng thao tác và ghi kiểm toán độc lập khi đọc. */
public final class TiepDonReadKiemToan {
    private TiepDonReadKiemToan() {}

    public static PhanQuyenQuyetDinh evaluate(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor,
                                          PhanQuyenDichVu authorization, String module) {
        if (authorization == null || !sameActor(context, actor)) {
            return PhanQuyenQuyetDinh.deny(PhanQuyenQuyetDinh.ReasonCode.INVALID_CONTEXT);
        }
        return authorization.evaluate(new PhanQuyenYeuCau(context, module, HanhDongQuyen.XEM,
                context.facilityId(), context.departmentId()));
    }

    public static boolean sameActor(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        return context != null && context.isValid() && actor != null
                && context.employeeId() == actor.employeeId() && context.roleId() == actor.roleId()
                && Objects.equals(context.roleCode(), actor.roleCode())
                && context.facilityId() == actor.facilityId()
                && context.departmentId() == actor.departmentId();
    }

    public static void success(KiemToanWriteCongKetNoi audit, KiemToanTacNhanNguCanh actor, KiemToanHanhDongMa action) {
        audit.appendIndependently(entry(actor, action, KiemToanKetQua.SUCCESS, null));
    }

    public static void denied(KiemToanWriteCongKetNoi audit, KiemToanTacNhanNguCanh actor, PhanQuyenQuyetDinh decision) {
        if (audit == null || actor == null) return;
        KiemToanLyDoMa reason = decision != null && (decision.reasonCode() == PhanQuyenQuyetDinh.ReasonCode.SCOPE_INVALID
                || decision.reasonCode() == PhanQuyenQuyetDinh.ReasonCode.TARGET_MISMATCH)
                ? KiemToanLyDoMa.SCOPE_MISMATCH : KiemToanLyDoMa.AUTH_FORBIDDEN;
        try {
            audit.appendIndependently(entry(actor, KiemToanHanhDongMa.AUTH_ACCESS_DENIED, KiemToanKetQua.DENIED, reason));
        } catch (RuntimeException ignored) {
            // Khi kho kiểm toán độc lập không khả dụng, yêu cầu bị từ chối an toàn.
        }
    }

    private static HeThongKiemToanNhapLieu entry(KiemToanTacNhanNguCanh actor, KiemToanHanhDongMa action,
                                          KiemToanKetQua outcome, KiemToanLyDoMa reason) {
        return new HeThongKiemToanNhapLieu(actor.employeeId(), actor.roleId(), actor.roleCode(), actor.facilityId(),
                actor.departmentId(), action, null, null, outcome, reason, Instant.now(),
                actor.workstationId(), actor.correlationId());
    }
}
