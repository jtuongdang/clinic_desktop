package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;

public record PurchaseOrderItemInput(int itemId, BigDecimal soLuong, BigDecimal donGia) {
}
