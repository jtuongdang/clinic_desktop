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
public class InventoryReportItemDto {

    private final int stt;
    private final int itemId;
    private final String itemCode;
    private final String itemName;
    private final String unit;
    private final int warehouseId;
    private final String warehouseName;
    private final String batchNumber;
    private final LocalDate expiryDate;
    private final BigDecimal openingStock;
    private final BigDecimal inflowStock;
    private final BigDecimal outflowStock;
    private final BigDecimal closingStock;
    private final BigDecimal unitPrice;
    private final BigDecimal retailPrice;
    private final BigDecimal totalValue;
    private final Long daysToExpiry;
    private final boolean isExpired;
    private final boolean isNearExpiry;
    private final boolean isLowStock;
    private final String status;
    private final String note;

    public InventoryReportItemDto(int stt, int itemId, String itemCode, String itemName,
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

    public int getStt() { return stt; }
    public int stt() { return stt; }

    public int getItemId() { return itemId; }
    public int itemId() { return itemId; }

    public String getItemCode() { return itemCode; }
    public String itemCode() { return itemCode; }

    public String getItemName() { return itemName; }
    public String itemName() { return itemName; }

    public String getUnit() { return unit; }
    public String unit() { return unit; }

    public int getWarehouseId() { return warehouseId; }
    public int warehouseId() { return warehouseId; }

    public String getWarehouseName() { return warehouseName; }
    public String warehouseName() { return warehouseName; }

    public String getBatchNumber() { return batchNumber; }
    public String batchNumber() { return batchNumber; }
    public String getSoLo() { return batchNumber; }
    public String soLo() { return batchNumber; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public LocalDate expiryDate() { return expiryDate; }
    public LocalDate getHanSuDung() { return expiryDate; }
    public LocalDate hanSuDung() { return expiryDate; }

    public BigDecimal getOpeningStock() { return openingStock; }
    public BigDecimal openingStock() { return openingStock; }

    public BigDecimal getInflowStock() { return inflowStock; }
    public BigDecimal inflowStock() { return inflowStock; }

    public BigDecimal getOutflowStock() { return outflowStock; }
    public BigDecimal outflowStock() { return outflowStock; }

    public BigDecimal getClosingStock() { return closingStock; }
    public BigDecimal closingStock() { return closingStock; }
    public BigDecimal getQuantity() { return closingStock; }
    public BigDecimal quantity() { return closingStock; }
    public BigDecimal getSoLuongTon() { return closingStock; }
    public BigDecimal soLuongTon() { return closingStock; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal unitPrice() { return unitPrice; }
    public BigDecimal getGiaNhap() { return unitPrice; }
    public BigDecimal giaNhap() { return unitPrice; }

    public BigDecimal getRetailPrice() { return retailPrice; }
    public BigDecimal retailPrice() { return retailPrice; }
    public BigDecimal getGiaBan() { return retailPrice; }
    public BigDecimal giaBan() { return retailPrice; }

    public BigDecimal getTotalValue() { return totalValue; }
    public BigDecimal totalValue() { return totalValue; }
    public BigDecimal getThanhTien() { return totalValue; }
    public BigDecimal thanhTien() { return totalValue; }

    public Long getDaysToExpiry() { return daysToExpiry; }
    public Long daysToExpiry() { return daysToExpiry; }

    public boolean isExpired() { return isExpired; }
    public boolean isNearExpiry() { return isNearExpiry; }
    public boolean isLowStock() { return isLowStock; }

    public String getStatus() { return status; }
    public String status() { return status; }

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
