package vn.clinicmanagement.application.reports;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * DTO chi tiết từng dòng mặt hàng / lô tồn kho dược phẩm (RPT-01).
 * Hỗ trợ hiển thị trên bảng phân tích và ánh xạ 7 cột chuẩn của ClinicReportPanel.
 */
public class KhoBaoCaoMatHangDuLieu {

    private final int stt;
    /** Trường `itemId`: mathang mã. */
    private final int itemId;
    /** Trường `itemCode`: mathang mã. */
    private final String itemCode;
    /** Trường `itemName`: mathang tên. */
    private final String itemName;
    /** Trường `unit`: đơn vị. */
    private final String unit;
    /** Trường `warehouseId`: kho mã. */
    private final int warehouseId;
    /** Trường `warehouseName`: kho tên. */
    private final String warehouseName;
    /** Trường `batchNumber`: lo so. */
    private final String batchNumber;
    /** Trường `expiryDate`: `expiry` ngày. */
    private final LocalDate expiryDate;
    /** Trường `openingStock`: `opening` tồn kho. */
    private final BigDecimal openingStock;
    /** Trường `inflowStock`: `inflow` tồn kho. */
    private final BigDecimal inflowStock;
    /** Trường `outflowStock`: `outflow` tồn kho. */
    private final BigDecimal outflowStock;
    /** Trường `closingStock`: `closing` tồn kho. */
    private final BigDecimal closingStock;
    /** Trường `unitPrice`: đơn vị `Price`. */
    private final BigDecimal unitPrice;
    private final BigDecimal retailPrice;
    /** Trường `totalValue`: `total` giá trị. */
    private final BigDecimal totalValue;
    /** Trường `daysToExpiry`: `days` den `Expiry`. */
    private final Long daysToExpiry;
    /** Trường `isExpired`: là `Expired`. */
    private final boolean isExpired;
    /** Trường `isNearExpiry`: là `Near` `Expiry`. */
    private final boolean isNearExpiry;
    /** Trường `isLowStock`: là thap tồn kho. */
    private final boolean isLowStock;
    /** Trường `status`: trạng thái. */
    private final String status;
    private final String note;

    public KhoBaoCaoMatHangDuLieu(int stt, int itemId, String itemCode, String itemName,
                                  String unit, int warehouseId, String warehouseName,
                                  String batchNumber, LocalDate expiryDate,
                                  BigDecimal openingStock, BigDecimal inflowStock, BigDecimal outflowStock,
                                  BigDecimal closingStock, BigDecimal unitPrice, BigDecimal retailPrice,
                                  BigDecimal totalValue, Long daysToExpiry,
                                  boolean isExpired, boolean isNearExpiry, boolean isLowStock,
                                  String status, String note) {
        this.stt = stt;
        this.itemId = itemId;
        this.itemCode = itemCode != null ? itemCode : "";
        this.itemName = itemName != null ? itemName : "";
        this.unit = unit != null ? unit : "Hộp";
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName != null ? warehouseName : "";
        this.batchNumber = batchNumber != null ? batchNumber : "";
        this.expiryDate = expiryDate;
        this.openingStock = openingStock != null ? openingStock : BigDecimal.ZERO;
        this.inflowStock = inflowStock != null ? inflowStock : BigDecimal.ZERO;
        this.outflowStock = outflowStock != null ? outflowStock : BigDecimal.ZERO;
        this.closingStock = closingStock != null ? closingStock : BigDecimal.ZERO;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.retailPrice = retailPrice != null ? retailPrice : BigDecimal.ZERO;
        this.totalValue = totalValue != null ? totalValue : BigDecimal.ZERO;
        this.daysToExpiry = daysToExpiry;
        this.isExpired = isExpired;
        this.isNearExpiry = isNearExpiry;
        this.isLowStock = isLowStock;
        this.status = status != null ? status : "BÌNH THƯỜNG";
        this.note = note != null ? note : "";
    }

    /** Hàm `getStt`: lấy `Stt`. */
    public int getStt() { return stt; }
    public int stt() { return stt; }

    /** Hàm `getItemId`: lấy mathang mã. */
    public int getItemId() { return itemId; }
    /** Hàm `itemId`: mathang mã. */
    public int itemId() { return itemId; }

    /** Hàm `getItemCode`: lấy mathang mã. */
    public String getItemCode() { return itemCode; }
    /** Hàm `itemCode`: mathang mã. */
    public String itemCode() { return itemCode; }

    /** Hàm `getItemName`: lấy mathang tên. */
    public String getItemName() { return itemName; }
    /** Hàm `itemName`: mathang tên. */
    public String itemName() { return itemName; }

    /** Hàm `getUnit`: lấy đơn vị. */
    public String getUnit() { return unit; }
    /** Hàm `unit`: đơn vị. */
    public String unit() { return unit; }

    /** Hàm `getWarehouseId`: lấy kho mã. */
    public int getWarehouseId() { return warehouseId; }
    /** Hàm `warehouseId`: kho mã. */
    public int warehouseId() { return warehouseId; }

    /** Hàm `getWarehouseName`: lấy kho tên. */
    public String getWarehouseName() { return warehouseName; }
    /** Hàm `warehouseName`: kho tên. */
    public String warehouseName() { return warehouseName; }

    /** Hàm `getBatchNumber`: lấy lo so. */
    public String getBatchNumber() { return batchNumber; }
    /** Hàm `batchNumber`: lo so. */
    public String batchNumber() { return batchNumber; }
    /** Hàm `getSoLo`: lấy so lo. */
    public String getSoLo() { return batchNumber; }
    public String soLo() { return batchNumber; }

    /** Hàm `getExpiryDate`: lấy `Expiry` ngày. */
    public LocalDate getExpiryDate() { return expiryDate; }
    /** Hàm `expiryDate`: `expiry` ngày. */
    public LocalDate expiryDate() { return expiryDate; }
    /** Hàm `getHanSuDung`: lấy han su `Dung`. */
    public LocalDate getHanSuDung() { return expiryDate; }
    public LocalDate hanSuDung() { return expiryDate; }

    /** Hàm `getOpeningStock`: lấy `Opening` tồn kho. */
    public BigDecimal getOpeningStock() { return openingStock; }
    /** Hàm `openingStock`: `opening` tồn kho. */
    public BigDecimal openingStock() { return openingStock; }

    /** Hàm `getInflowStock`: lấy `Inflow` tồn kho. */
    public BigDecimal getInflowStock() { return inflowStock; }
    /** Hàm `inflowStock`: `inflow` tồn kho. */
    public BigDecimal inflowStock() { return inflowStock; }

    /** Hàm `getOutflowStock`: lấy `Outflow` tồn kho. */
    public BigDecimal getOutflowStock() { return outflowStock; }
    /** Hàm `outflowStock`: `outflow` tồn kho. */
    public BigDecimal outflowStock() { return outflowStock; }

    /** Hàm `getClosingStock`: lấy `Closing` tồn kho. */
    public BigDecimal getClosingStock() { return closingStock; }
    /** Hàm `closingStock`: `closing` tồn kho. */
    public BigDecimal closingStock() { return closingStock; }
    /** Hàm `getQuantity`: lấy `Quantity`. */
    public BigDecimal getQuantity() { return closingStock; }
    public BigDecimal quantity() { return closingStock; }
    /** Hàm `getSoLuongTon`: lấy so luong ton. */
    public BigDecimal getSoLuongTon() { return closingStock; }
    public BigDecimal soLuongTon() { return closingStock; }

    /** Hàm `getUnitPrice`: lấy đơn vị `Price`. */
    public BigDecimal getUnitPrice() { return unitPrice; }
    /** Hàm `unitPrice`: đơn vị `Price`. */
    public BigDecimal unitPrice() { return unitPrice; }
    /** Hàm `getGiaNhap`: lấy `Gia` nhap. */
    public BigDecimal getGiaNhap() { return unitPrice; }
    public BigDecimal giaNhap() { return unitPrice; }

    /** Hàm `getRetailPrice`: lấy `Retail` `Price`. */
    public BigDecimal getRetailPrice() { return retailPrice; }
    public BigDecimal retailPrice() { return retailPrice; }
    /** Hàm `getGiaBan`: lấy `Gia` `Ban`. */
    public BigDecimal getGiaBan() { return retailPrice; }
    public BigDecimal giaBan() { return retailPrice; }

    /** Hàm `getTotalValue`: lấy `Total` giá trị. */
    public BigDecimal getTotalValue() { return totalValue; }
    /** Hàm `totalValue`: `total` giá trị. */
    public BigDecimal totalValue() { return totalValue; }
    /** Hàm `getThanhTien`: lấy thanh tien. */
    public BigDecimal getThanhTien() { return totalValue; }
    public BigDecimal thanhTien() { return totalValue; }

    /** Hàm `getDaysToExpiry`: lấy `Days` den `Expiry`. */
    public Long getDaysToExpiry() { return daysToExpiry; }
    /** Hàm `daysToExpiry`: `days` den `Expiry`. */
    public Long daysToExpiry() { return daysToExpiry; }

    /** Hàm `isExpired`: là `Expired`. */
    public boolean isExpired() { return isExpired; }
    /** Hàm `isNearExpiry`: là `Near` `Expiry`. */
    public boolean isNearExpiry() { return isNearExpiry; }
    /** Hàm `isLowStock`: là thap tồn kho. */
    public boolean isLowStock() { return isLowStock; }

    /** Hàm `getStatus`: lấy trạng thái. */
    public String getStatus() { return status; }
    /** Hàm `status`: trạng thái. */
    public String status() { return status; }

    /** Hàm `getNote`: lấy `Note`. */
    public String getNote() { return note; }
    public String note() { return note; }

    /**
     * Chuyển đổi thành hàng 7 cột tương thích với ClinicReportPanel:
     * {"STT", "Chỉ Tiêu / Hạng Mục", "Đơn Vị Tính", "Kỳ Trước", "Kỳ Này", "Tăng/Giảm (%)", "Ghi Chú"}
     */
    public Object[] toTableRow(int rowIndex) {
        NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        String targetDesc = itemName;
        if (batchNumber != null && !batchNumber.isBlank()) {
            targetDesc += " (Lô: " + batchNumber + ")";
        }

        String noteAlert = status;
        if (expiryDate != null) {
            noteAlert += " | HSD: " + expiryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        if (note != null && !note.isBlank()) {
            noteAlert += " - " + note;
        }

        return new Object[] {
                String.valueOf(rowIndex),
                targetDesc,
                unit,
                openingStock.compareTo(BigDecimal.ZERO) > 0 ? fmt.format(openingStock) : "-",
                fmt.format(closingStock),
                "-",
                noteAlert
        };
    }
}
