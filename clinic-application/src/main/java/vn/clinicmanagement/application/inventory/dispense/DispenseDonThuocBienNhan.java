package vn.clinicmanagement.application.inventory.dispense;

import vn.clinicmanagement.application.inventory.DaCapPhatMatHangPhanBoDuLieu;

import java.math.BigDecimal;
import java.util.List;

public record DispenseDonThuocBienNhan(
        int prescriptionId,
        int issueReceiptId,
        String soPhieuXuat,
        BigDecimal tongTien,
        List<DaCapPhatMatHangPhanBoDuLieu> allocations) {
}
