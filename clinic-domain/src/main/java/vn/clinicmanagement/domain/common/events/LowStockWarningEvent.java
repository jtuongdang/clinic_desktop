package vn.clinicmanagement.domain.common.events;

public class LowStockWarningEvent extends AbstractDomainEvent {
    private final int itemId;
    private final String itemName;
    private final java.math.BigDecimal currentStock;
    private final java.math.BigDecimal threshold;

    public LowStockWarningEvent(int itemId, String itemName, java.math.BigDecimal currentStock, java.math.BigDecimal threshold) {
        super();
        this.itemId = itemId;
        this.itemName = itemName;
        this.currentStock = currentStock;
        this.threshold = threshold;
    }

    public int getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public java.math.BigDecimal getCurrentStock() { return currentStock; }
    public java.math.BigDecimal getThreshold() { return threshold; }
}
