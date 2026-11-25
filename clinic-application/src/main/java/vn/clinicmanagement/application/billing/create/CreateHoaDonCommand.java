package vn.clinicmanagement.application.billing.create;

import vn.clinicmanagement.application.billing.HoaDonMatHangDauVao;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;
import java.util.List;

public record CreateHoaDonCommand(
        int visitId,
        int patientId,
        int facilityId,
        BigDecimal insurancePaidAmount,
        BigDecimal advanceAmount,
        BigDecimal discountAmount,
        BigDecimal discountPercentage,
        String discountReason,
        String notes,
        List<HoaDonMatHangDauVao> items,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<CreateHoaDonBienNhan> {}
