package vn.clinicmanagement.application.reports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * DTO tổng hợp báo cáo xuất nhập tồn kho & cảnh báo hạn dùng (RPT-01).
 * Cung cấp số lượng tồn, giá trị tồn (nhập/bán), cảnh báo hết hạn và cảnh báo tồn thấp.
 */
public class KhoBaoCaoDuLieu {

    /** Trường `asOfDate`: `as` của ngày. */
    private final LocalDate asOfDate;
    /** Trường `warehouseId`: kho mã. */
    private final Integer warehouseId;
    private final int totalItems;
    /** Trường `totalStockQuantity`: `total` tồn kho `Quantity`. */
    private final BigDecimal totalStockQuantity;
    /** Trường `totalStockValue`: `total` tồn kho giá trị. */
    private final BigDecimal totalStockValue;
    /** Trường `totalRetailValue`: `total` `Retail` giá trị. */
    private final BigDecimal totalRetailValue;
    /** Trường `expiredItemCount`: `expired` mathang dem. */
    private final int expiredItemCount;
    /** Trường `nearExpiryItemCount`: `near` `Expiry` mathang dem. */
    private final int nearExpiryItemCount;
    /** Trường `lowStockItemCount`: thap tồn kho mathang dem. */
    private final int lowStockItemCount;
    private final List<KhoBaoCaoMatHangDuLieu> items;
    private final List<KhoBaoCaoMatHangDuLieu> expiryAlerts;
    /** Trường `lowStockAlerts`: thap tồn kho `Alerts`. */
    private final List<KhoBaoCaoMatHangDuLieu> lowStockAlerts;

    public KhoBaoCaoDuLieu(LocalDate asOfDate, Integer warehouseId,
                              int totalItems,
                              BigDecimal totalStockQuantity,
                              BigDecimal totalStockValue,
                              BigDecimal totalRetailValue,
                              int expiredItemCount,
                              int nearExpiryItemCount,
                              int lowStockItemCount,
                              List<KhoBaoCaoMatHangDuLieu> items,
                              List<KhoBaoCaoMatHangDuLieu> expiryAlerts,
                              List<KhoBaoCaoMatHangDuLieu> lowStockAlerts) {
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

    /** Hàm `getAsOfDate`: lấy `As` của ngày. */
    public LocalDate getAsOfDate() { return asOfDate; }
    /** Hàm `asOfDate`: `as` của ngày. */
    public LocalDate asOfDate() { return asOfDate; }

    /** Hàm `getWarehouseId`: lấy kho mã. */
    public Integer getWarehouseId() { return warehouseId; }
    /** Hàm `warehouseId`: kho mã. */
    public Integer warehouseId() { return warehouseId; }

    /** Hàm `getTotalItems`: lấy `Total` `Items`. */
    public int getTotalItems() { return totalItems; }
    public int totalItems() { return totalItems; }

    /** Hàm `getTotalStockQuantity`: lấy `Total` tồn kho `Quantity`. */
    public BigDecimal getTotalStockQuantity() { return totalStockQuantity; }
    /** Hàm `totalStockQuantity`: `total` tồn kho `Quantity`. */
    public BigDecimal totalStockQuantity() { return totalStockQuantity; }
    /** Hàm `getTotalQuantity`: lấy `Total` `Quantity`. */
    public BigDecimal getTotalQuantity() { return totalStockQuantity; }
    public BigDecimal totalQuantity() { return totalStockQuantity; }

    /** Hàm `getTotalStockValue`: lấy `Total` tồn kho giá trị. */
    public BigDecimal getTotalStockValue() { return totalStockValue; }
    /** Hàm `totalStockValue`: `total` tồn kho giá trị. */
    public BigDecimal totalStockValue() { return totalStockValue; }
    /** Hàm `getTotalValue`: lấy `Total` giá trị. */
    public BigDecimal getTotalValue() { return totalStockValue; }
    /** Hàm `totalValue`: `total` giá trị. */
    public BigDecimal totalValue() { return totalStockValue; }

    /** Hàm `getTotalRetailValue`: lấy `Total` `Retail` giá trị. */
    public BigDecimal getTotalRetailValue() { return totalRetailValue; }
    /** Hàm `totalRetailValue`: `total` `Retail` giá trị. */
    public BigDecimal totalRetailValue() { return totalRetailValue; }

    /** Hàm `getExpiredItemCount`: lấy `Expired` mathang dem. */
    public int getExpiredItemCount() { return expiredItemCount; }
    /** Hàm `expiredItemCount`: `expired` mathang dem. */
    public int expiredItemCount() { return expiredItemCount; }

    /** Hàm `getNearExpiryItemCount`: lấy `Near` `Expiry` mathang dem. */
    public int getNearExpiryItemCount() { return nearExpiryItemCount; }
    /** Hàm `nearExpiryItemCount`: `near` `Expiry` mathang dem. */
    public int nearExpiryItemCount() { return nearExpiryItemCount; }

    /** Hàm `getLowStockItemCount`: lấy thap tồn kho mathang dem. */
    public int getLowStockItemCount() { return lowStockItemCount; }
    /** Hàm `lowStockItemCount`: thap tồn kho mathang dem. */
    public int lowStockItemCount() { return lowStockItemCount; }

    /** Hàm `getItems`: lấy `Items`. */
    public List<KhoBaoCaoMatHangDuLieu> getItems() { return items; }
    public List<KhoBaoCaoMatHangDuLieu> items() { return items; }

    /** Hàm `getExpiryAlerts`: lấy `Expiry` `Alerts`. */
    public List<KhoBaoCaoMatHangDuLieu> getExpiryAlerts() { return expiryAlerts; }
    public List<KhoBaoCaoMatHangDuLieu> expiryAlerts() { return expiryAlerts; }

    /** Hàm `getLowStockAlerts`: lấy thap tồn kho `Alerts`. */
    public List<KhoBaoCaoMatHangDuLieu> getLowStockAlerts() { return lowStockAlerts; }
    /** Hàm `lowStockAlerts`: thap tồn kho `Alerts`. */
    public List<KhoBaoCaoMatHangDuLieu> lowStockAlerts() { return lowStockAlerts; }
}
