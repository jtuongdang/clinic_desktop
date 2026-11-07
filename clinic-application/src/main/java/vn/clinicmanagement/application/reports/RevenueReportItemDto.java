package vn.clinicmanagement.application.reports;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * DTO chi tiết từng hạng mục doanh thu (RPT-01).
 * Hỗ trợ hiển thị bảng phân tích và ánh xạ 7 cột chuẩn của ClinicReportPanel.
 */
public class RevenueReportItemDto {

    private final int stt;
    private final String category;
    private final String unit;
    private final long itemCount;
    private final BigDecimal amount;
    private final BigDecimal previousAmount;
    private final Double growthRate;
    private final BigDecimal insuranceAmount;
    private final BigDecimal patientAmount;
    private final Double percentage;
    private final String note;

    public RevenueReportItemDto(int stt, String category, String unit, long itemCount,
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

    public RevenueReportItemDto(int stt, String category, long itemCount,
                                BigDecimal amount, BigDecimal insuranceAmount, BigDecimal patientAmount,
                                Double percentage, String note) {
        this(stt, category, "VNĐ", itemCount, amount, BigDecimal.ZERO, null, insuranceAmount, patientAmount, percentage, note);
    }

    public RevenueReportItemDto(String category, long itemCount,
                                BigDecimal amount, BigDecimal insuranceAmount, BigDecimal patientAmount,
                                Double percentage, String note) {
        this(0, category, "VNĐ", itemCount, amount, BigDecimal.ZERO, null, insuranceAmount, patientAmount, percentage, note);
    }

    public int getStt() { return stt; }
    public int stt() { return stt; }

    public String getCategory() { return category; }
    public String category() { return category; }
    public String getGroupName() { return category; }
    public String groupName() { return category; }

    public String getUnit() { return unit; }
    public String unit() { return unit; }

    public long getItemCount() { return itemCount; }
    public long itemCount() { return itemCount; }

    public BigDecimal getAmount() { return amount; }
    public BigDecimal amount() { return amount; }
    public BigDecimal getRevenue() { return amount; }
    public BigDecimal revenue() { return amount; }

    public BigDecimal getPreviousAmount() { return previousAmount; }
    public BigDecimal previousAmount() { return previousAmount; }

    public Double getGrowthRate() { return growthRate; }
    public Double growthRate() { return growthRate; }

    public BigDecimal getInsuranceAmount() { return insuranceAmount; }
    public BigDecimal insuranceAmount() { return insuranceAmount; }

    public BigDecimal getPatientAmount() { return patientAmount; }
    public BigDecimal patientAmount() { return patientAmount; }

    public Double getPercentage() { return percentage; }
    public Double percentage() { return percentage; }

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
