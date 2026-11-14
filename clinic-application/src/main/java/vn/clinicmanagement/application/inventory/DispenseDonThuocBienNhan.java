package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;
import java.util.List;

public record DispenseDonThuocBienNhan(
        /** Trường `prescriptionId`: đơn thuốc mã. */
        int prescriptionId,
        /** Trường `issueReceiptId`: `issue` biên nhận mã. */
        int issueReceiptId,
        String soPhieuXuat,
        BigDecimal tongTien,
        List<DaCapPhatMatHangPhanBoDuLieu> allocations) {
}
