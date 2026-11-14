package vn.clinicmanagement.domain.common.events;

public class ThapTonKhoCanhBaoSuKien extends TruuTuongMienNghiepVuSuKien {
    /** Trường `itemId`: mathang mã. */
    private final int itemId;
    /** Trường `itemName`: mathang tên. */
    private final String itemName;
    /** Trường `currentStock`: hientai tồn kho. */
    private final java.math.BigDecimal currentStock;
    private final java.math.BigDecimal threshold;

    public ThapTonKhoCanhBaoSuKien(int itemId, String itemName, java.math.BigDecimal currentStock, java.math.BigDecimal threshold) {
        super();
        this.itemId = itemId;
        this.itemName = itemName;
        this.currentStock = currentStock;
        this.threshold = threshold;
    }

    /** Hàm `getItemId`: lấy mathang mã. */
    public int getItemId() { return itemId; }
    /** Hàm `getItemName`: lấy mathang tên. */
    public String getItemName() { return itemName; }
    /** Hàm `getCurrentStock`: lấy hientai tồn kho. */
    public java.math.BigDecimal getCurrentStock() { return currentStock; }
    /** Hàm `getThreshold`: lấy `Threshold`. */
    public java.math.BigDecimal getThreshold() { return threshold; }
}
