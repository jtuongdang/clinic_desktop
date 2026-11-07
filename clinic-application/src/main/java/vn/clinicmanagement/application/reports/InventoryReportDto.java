package vn.clinicmanagement.application.reports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * DTO tổng hợp báo cáo xuất nhập tồn kho & cảnh báo hạn dùng (RPT-01).
 * Cung cấp số lượng tồn, giá trị tồn (nhập/bán), cảnh báo hết hạn và cảnh báo tồn thấp.
 */
public class InventoryReportDto {

    private final LocalDate asOfDate;
    private final Integer warehouseId;
    private final int totalItems;
    private final BigDecimal totalStockQuantity;
    private final BigDecimal totalStockValue;
    private final BigDecimal totalRetailValue;
    private final int expiredItemCount;
    private final int nearExpiryItemCount;
    private final int lowStockItemCount;
    private final List<InventoryReportItemDto> items;
    private final List<InventoryReportItemDto> expiryAlerts;
    private final List<InventoryReportItemDto> lowStockAlerts;

    public InventoryReportDto(LocalDate asOfDate, Integer warehouseId,
                              int totalItems,
                              BigDecimal totalStockQuantity,
                              BigDecimal totalStockValue,
                              BigDecimal totalRetailValue,
                              int expiredItemCount,
                              int nearExpiryItemCount,
                              int lowStockItemCount,
                              List<InventoryReportItemDto> items,
                              List<InventoryReportItemDto> expiryAlerts,
                              List<InventoryReportItemDto> lowStockAlerts) {
        this.asOfDate = asOfDate;
        this.warehouseId = warehouseId;
        this.totalItems = totalItems;
        this.totalStockQuantity = totalStockQuantity != null ? totalStockQuantity : BigDecimal.ZERO;
        this.totalStockValue = totalStockValue != null ? totalStockValue : BigDecimal.ZERO;
        this.totalRetailValue = totalRetailValue != null ? totalRetailValue : BigDecimal.ZERO;
        this.expiredItemCount = expiredItemCount;
        this.nearExpiryItemCount = nearExpiryItemCount;
        this.lowStockItemCount = lowStockItemCount;
        this.items = items != null ? List.copyOf(items) : List.of();
        this.expiryAlerts = expiryAlerts != null ? List.copyOf(expiryAlerts) : List.of();
        this.lowStockAlerts = lowStockAlerts != null ? List.copyOf(lowStockAlerts) : List.of();
    }

    public LocalDate getAsOfDate() { return asOfDate; }
    public LocalDate asOfDate() { return asOfDate; }

    public Integer getWarehouseId() { return warehouseId; }
    public Integer warehouseId() { return warehouseId; }

    public int getTotalItems() { return totalItems; }
    public int totalItems() { return totalItems; }

    public BigDecimal getTotalStockQuantity() { return totalStockQuantity; }
    public BigDecimal totalStockQuantity() { return totalStockQuantity; }
    public BigDecimal getTotalQuantity() { return totalStockQuantity; }
    public BigDecimal totalQuantity() { return totalStockQuantity; }

    public BigDecimal getTotalStockValue() { return totalStockValue; }
    public BigDecimal totalStockValue() { return totalStockValue; }
    public BigDecimal getTotalValue() { return totalStockValue; }
    public BigDecimal totalValue() { return totalStockValue; }

    public BigDecimal getTotalRetailValue() { return totalRetailValue; }
    public BigDecimal totalRetailValue() { return totalRetailValue; }

    public int getExpiredItemCount() { return expiredItemCount; }
    public int expiredItemCount() { return expiredItemCount; }

    public int getNearExpiryItemCount() { return nearExpiryItemCount; }
    public int nearExpiryItemCount() { return nearExpiryItemCount; }

    public int getLowStockItemCount() { return lowStockItemCount; }
    public int lowStockItemCount() { return lowStockItemCount; }

    public List<InventoryReportItemDto> getItems() { return items; }
    public List<InventoryReportItemDto> items() { return items; }

    public List<InventoryReportItemDto> getExpiryAlerts() { return expiryAlerts; }
    public List<InventoryReportItemDto> expiryAlerts() { return expiryAlerts; }

    public List<InventoryReportItemDto> getLowStockAlerts() { return lowStockAlerts; }
    public List<InventoryReportItemDto> lowStockAlerts() { return lowStockAlerts; }
}
