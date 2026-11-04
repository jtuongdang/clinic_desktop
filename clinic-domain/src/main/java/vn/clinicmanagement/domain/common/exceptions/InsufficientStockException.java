package vn.clinicmanagement.domain.common.exceptions;

import java.math.BigDecimal;

public class InsufficientStockException extends DomainException {
    public InsufficientStockException(String itemName, String batchNumber, BigDecimal available, BigDecimal requested) {
        super("Tồn kho không đủ cho mặt hàng '" + itemName + "' (Lô: " + batchNumber + "). Khả dụng: " + available + ", Yêu cầu: " + requested);
    }
}
