package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;
import java.util.List;

public record CreateHoaDonCommand(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `patientId`: bệnh nhân mã. */
        int patientId,
        /** Trường `facilityId`: cơ sở mã. */
        int facilityId,
        BigDecimal insurancePaidAmount,
        BigDecimal advanceAmount,
        BigDecimal discountAmount,
        BigDecimal discountPercentage,
        /** Trường `discountReason`: `discount` lý do. */
        String discountReason,
        String notes,
        List<HoaDonMatHangDauVao> items,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<CreateHoaDonBienNhan> {}
