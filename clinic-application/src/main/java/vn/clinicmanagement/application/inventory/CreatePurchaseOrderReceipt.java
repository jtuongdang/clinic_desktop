package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;

public record CreatePurchaseOrderReceipt(int purchaseOrderId, String soDonDatHang, BigDecimal tongTien) {
}
