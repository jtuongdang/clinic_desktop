package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordThanhToanCommand(
        /** Trường `invoiceId`: hóa đơn mã. */
        int invoiceId,
        BigDecimal amount,
        /** Trường `paymentMethod`: thanhtoan `Method`. */
        PhuongThucThanhToan paymentMethod,
        /** Trường `debtDueDate`: congno `Due` ngày. */
        LocalDate debtDueDate,
        String notes,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<RecordThanhToanBienNhan> {}
