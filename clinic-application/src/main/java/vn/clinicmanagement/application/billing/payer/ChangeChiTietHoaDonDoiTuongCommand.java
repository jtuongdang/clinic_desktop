package vn.clinicmanagement.application.billing.payer;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;

public record ChangeChiTietHoaDonDoiTuongCommand(
        int invoiceId,
        int lineId,
        String targetPayerType,
        BigDecimal fallbackServicePrice,
        BigDecimal fallbackInsurancePrice,
        Integer overrideCoverageRate,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<ChangeChiTietHoaDonDoiTuongBienNhan> {
    public ChangeChiTietHoaDonDoiTuongCommand(
            int invoiceId,
            int lineId,
            String targetPayerType,
            PhanQuyenNguCanh authorizationContext,
            KiemToanTacNhanNguCanh auditActorContext
    ) {
        this(invoiceId, lineId, targetPayerType, null, null, null, authorizationContext, auditActorContext);
    }
}
