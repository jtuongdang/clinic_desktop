package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;
import java.util.List;

public record DispensePrescriptionReceipt(
        int prescriptionId,
        int issueReceiptId,
        String soPhieuXuat,
        BigDecimal tongTien,
        List<DispensedItemAllocationDto> allocations) {
}
