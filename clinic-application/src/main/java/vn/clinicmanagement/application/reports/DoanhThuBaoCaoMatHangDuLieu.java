package vn.clinicmanagement.application.reports;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * DTO chi tiết từng hạng mục doanh thu (RPT-01).
 * Hỗ trợ hiển thị bảng phân tích và ánh xạ 7 cột chuẩn của ClinicReportPanel.
 */
public class DoanhThuBaoCaoMatHangDuLieu {

    private final int stt;
    /** Trường `category`: danhmuc. */
    private final String category;
    /** Trường `unit`: đơn vị. */
    private final String unit;
    /** Trường `itemCount`: mathang dem. */
    private final long itemCount;
    private final BigDecimal amount;
    private final BigDecimal previousAmount;
    private final Double growthRate;
    private final BigDecimal insuranceAmount;
    /** Trường `patientAmount`: bệnh nhân `Amount`. */
    private final BigDecimal patientAmount;
    private final Double percentage;
    private final String note;

    public DoanhThuBaoCaoMatHangDuLieu(int stt, String category, String unit, long itemCount,
                                BigDecimal amount, BigDecimal previousAmount, Double growthRate,
                                BigDecimal insuranceAmount, BigDecimal patientAmount,
                                Double percentage, String note) {
        this.stt = stt;
        this.category = category != null ? category : "";
        this.unit = unit != null ? unit : "VNĐ";
        this.itemCount = itemCount;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.previousAmount = previousAmount != null ? previousAmount : BigDecimal.ZERO;
        this.growthRate = growthRate;
        this.insuranceAmount = insuranceAmount != null ? insuranceAmount : BigDecimal.ZERO;
        this.patientAmount = patientAmount != null ? patientAmount : BigDecimal.ZERO;
        this.percentage = percentage != null ? percentage : 0.0;
        this.note = note != null ? note : "";
    }

    public DoanhThuBaoCaoMatHangDuLieu(int stt, String category, long itemCount,
                                BigDecimal amount, BigDecimal insuranceAmount, BigDecimal patientAmount,
                                Double percentage, String note) {
        this(stt, category, "VNĐ", itemCount, amount, BigDecimal.ZERO, null, insuranceAmount, patientAmount, percentage, note);
    }

    public DoanhThuBaoCaoMatHangDuLieu(String category, long itemCount,
                                BigDecimal amount, BigDecimal insuranceAmount, BigDecimal patientAmount,
                                Double percentage, String note) {
        this(0, category, "VNĐ", itemCount, amount, BigDecimal.ZERO, null, insuranceAmount, patientAmount, percentage, note);
    }

    /** Hàm `getStt`: lấy `Stt`. */
    public int getStt() { return stt; }
    public int stt() { return stt; }

    /** Hàm `getCategory`: lấy danhmuc. */
    public String getCategory() { return category; }
    /** Hàm `category`: danhmuc. */
    public String category() { return category; }
    /** Hàm `getGroupName`: lấy `Group` tên. */
    public String getGroupName() { return category; }
    /** Hàm `groupName`: `group` tên. */
    public String groupName() { return category; }

    /** Hàm `getUnit`: lấy đơn vị. */
    public String getUnit() { return unit; }
    /** Hàm `unit`: đơn vị. */
    public String unit() { return unit; }

    /** Hàm `getItemCount`: lấy mathang dem. */
    public long getItemCount() { return itemCount; }
    /** Hàm `itemCount`: mathang dem. */
    public long itemCount() { return itemCount; }

    /** Hàm `getAmount`: lấy `Amount`. */
    public BigDecimal getAmount() { return amount; }
    public BigDecimal amount() { return amount; }
    /** Hàm `getRevenue`: lấy doanhthu. */
    public BigDecimal getRevenue() { return amount; }
    /** Hàm `revenue`: doanhthu. */
    public BigDecimal revenue() { return amount; }

    /** Hàm `getPreviousAmount`: lấy `Previous` `Amount`. */
    public BigDecimal getPreviousAmount() { return previousAmount; }
    public BigDecimal previousAmount() { return previousAmount; }

    /** Hàm `getGrowthRate`: lấy `Growth` `Rate`. */
    public Double getGrowthRate() { return growthRate; }
    public Double growthRate() { return growthRate; }

    /** Hàm `getInsuranceAmount`: lấy `Insurance` `Amount`. */
    public BigDecimal getInsuranceAmount() { return insuranceAmount; }
    public BigDecimal insuranceAmount() { return insuranceAmount; }

    /** Hàm `getPatientAmount`: lấy bệnh nhân `Amount`. */
    public BigDecimal getPatientAmount() { return patientAmount; }
    /** Hàm `patientAmount`: bệnh nhân `Amount`. */
    public BigDecimal patientAmount() { return patientAmount; }

    /** Hàm `getPercentage`: lấy `Percentage`. */
    public Double getPercentage() { return percentage; }
    public Double percentage() { return percentage; }

    /** Hàm `getNote`: lấy `Note`. */
    public String getNote() { return note; }
    public String note() { return note; }

    /**
     * Chuyển đổi thành hàng 7 cột tương thích với bảng ClinicReportPanel:
     * {"STT", "Chỉ Tiêu / Hạng Mục", "Đơn Vị Tính", "Kỳ Trước", "Kỳ Này", "Tăng/Giảm (%)", "Ghi Chú"}
     */
    public Object[] toTableRow(int rowIndex) {
        NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        String growthStr = growthRate != null ? String.format(Locale.ROOT, "%+.1f%%", growthRate) : "-";
        return new Object[] {
                String.valueOf(rowIndex),
                category,
                unit,
                previousAmount.compareTo(BigDecimal.ZERO) > 0 ? fmt.format(previousAmount) : "-",
                fmt.format(amount),
                growthStr,
                note
        };
    }
}
