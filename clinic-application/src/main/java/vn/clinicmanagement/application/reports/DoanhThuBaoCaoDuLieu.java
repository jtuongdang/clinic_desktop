package vn.clinicmanagement.application.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * DTO tổng hợp báo cáo doanh thu tài chính phòng khám (RPT-01).
 * Cung cấp đầy đủ số liệu đa chiều: nhóm dịch vụ, đối soát BHYT, hình thức thanh toán.
 */
public class DoanhThuBaoCaoDuLieu {

    /** Trường `fromDate`: tu ngày. */
    private final LocalDate fromDate;
    /** Trường `toDate`: den ngày. */
    private final LocalDate toDate;
    /** Trường `totalRevenue`: `total` doanhthu. */
    private final BigDecimal totalRevenue;
    /** Trường `consultationRevenue`: `consultation` doanhthu. */
    private final BigDecimal consultationRevenue;
    /** Trường `paraclinicalRevenue`: canlamsang doanhthu. */
    private final BigDecimal paraclinicalRevenue;
    /** Trường `medicationRevenue`: thuốc doanhthu. */
    private final BigDecimal medicationRevenue;
    /** Trường `otherRevenue`: khac doanhthu. */
    private final BigDecimal otherRevenue;
    private final BigDecimal insurancePaidAmount;
    /** Trường `patientPaidAmount`: bệnh nhân `Paid` `Amount`. */
    private final BigDecimal patientPaidAmount;
    private final BigDecimal cashAmount;
    /** Trường `transferAmount`: chuyển `Amount`. */
    private final BigDecimal transferAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal advanceAmount;
    private final long totalInvoices;
    private final long paidInvoices;
    private final List<DoanhThuBaoCaoMatHangDuLieu> items;

    public DoanhThuBaoCaoDuLieu(LocalDate fromDate, LocalDate toDate,
                            BigDecimal totalRevenue,
                            BigDecimal consultationRevenue,
                            BigDecimal paraclinicalRevenue,
                            BigDecimal medicationRevenue,
                            BigDecimal otherRevenue,
                            BigDecimal insurancePaidAmount,
                            BigDecimal patientPaidAmount,
                            BigDecimal cashAmount,
                            BigDecimal transferAmount,
                            BigDecimal discountAmount,
                            BigDecimal advanceAmount,
                            long totalInvoices,
                            long paidInvoices,
                            List<DoanhThuBaoCaoMatHangDuLieu> items) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        this.consultationRevenue = consultationRevenue != null ? consultationRevenue : BigDecimal.ZERO;
        this.paraclinicalRevenue = paraclinicalRevenue != null ? paraclinicalRevenue : BigDecimal.ZERO;
        this.medicationRevenue = medicationRevenue != null ? medicationRevenue : BigDecimal.ZERO;
        this.otherRevenue = otherRevenue != null ? otherRevenue : BigDecimal.ZERO;
        this.insurancePaidAmount = insurancePaidAmount != null ? insurancePaidAmount : BigDecimal.ZERO;
        this.patientPaidAmount = patientPaidAmount != null ? patientPaidAmount : BigDecimal.ZERO;
        this.cashAmount = cashAmount != null ? cashAmount : BigDecimal.ZERO;
        this.transferAmount = transferAmount != null ? transferAmount : BigDecimal.ZERO;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.advanceAmount = advanceAmount != null ? advanceAmount : BigDecimal.ZERO;
        this.totalInvoices = totalInvoices;
        this.paidInvoices = paidInvoices;
        this.items = items != null ? List.copyOf(items) : List.of();
    }

    /** Hàm `getFromDate`: lấy tu ngày. */
    public LocalDate getFromDate() { return fromDate; }
    /** Hàm `fromDate`: tu ngày. */
    public LocalDate fromDate() { return fromDate; }

    /** Hàm `getToDate`: lấy den ngày. */
    public LocalDate getToDate() { return toDate; }
    /** Hàm `toDate`: den ngày. */
    public LocalDate toDate() { return toDate; }

    /** Hàm `getTotalRevenue`: lấy `Total` doanhthu. */
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    /** Hàm `totalRevenue`: `total` doanhthu. */
    public BigDecimal totalRevenue() { return totalRevenue; }

    /** Hàm `getConsultationRevenue`: lấy `Consultation` doanhthu. */
    public BigDecimal getConsultationRevenue() { return consultationRevenue; }
    /** Hàm `consultationRevenue`: `consultation` doanhthu. */
    public BigDecimal consultationRevenue() { return consultationRevenue; }

    /** Hàm `getParaclinicalRevenue`: lấy canlamsang doanhthu. */
    public BigDecimal getParaclinicalRevenue() { return paraclinicalRevenue; }
    /** Hàm `paraclinicalRevenue`: canlamsang doanhthu. */
    public BigDecimal paraclinicalRevenue() { return paraclinicalRevenue; }

    /** Hàm `getMedicationRevenue`: lấy thuốc doanhthu. */
    public BigDecimal getMedicationRevenue() { return medicationRevenue; }
    /** Hàm `medicationRevenue`: thuốc doanhthu. */
    public BigDecimal medicationRevenue() { return medicationRevenue; }

    /** Hàm `getOtherRevenue`: lấy khac doanhthu. */
    public BigDecimal getOtherRevenue() { return otherRevenue; }
    /** Hàm `otherRevenue`: khac doanhthu. */
    public BigDecimal otherRevenue() { return otherRevenue; }

    /** Hàm `getInsurancePaidAmount`: lấy `Insurance` `Paid` `Amount`. */
    public BigDecimal getInsurancePaidAmount() { return insurancePaidAmount; }
    public BigDecimal insurancePaidAmount() { return insurancePaidAmount; }

    /** Hàm `getPatientPaidAmount`: lấy bệnh nhân `Paid` `Amount`. */
    public BigDecimal getPatientPaidAmount() { return patientPaidAmount; }
    /** Hàm `patientPaidAmount`: bệnh nhân `Paid` `Amount`. */
    public BigDecimal patientPaidAmount() { return patientPaidAmount; }

    /** Hàm `getCashAmount`: lấy `Cash` `Amount`. */
    public BigDecimal getCashAmount() { return cashAmount; }
    public BigDecimal cashAmount() { return cashAmount; }

    /** Hàm `getTransferAmount`: lấy chuyển `Amount`. */
    public BigDecimal getTransferAmount() { return transferAmount; }
    /** Hàm `transferAmount`: chuyển `Amount`. */
    public BigDecimal transferAmount() { return transferAmount; }

    /** Hàm `getDiscountAmount`: lấy `Discount` `Amount`. */
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal discountAmount() { return discountAmount; }

    /** Hàm `getAdvanceAmount`: lấy `Advance` `Amount`. */
    public BigDecimal getAdvanceAmount() { return advanceAmount; }
    public BigDecimal advanceAmount() { return advanceAmount; }

    /** Hàm `getTotalInvoices`: lấy `Total` `Invoices`. */
    public long getTotalInvoices() { return totalInvoices; }
    public long totalInvoices() { return totalInvoices; }

    /** Hàm `getPaidInvoices`: lấy `Paid` `Invoices`. */
    public long getPaidInvoices() { return paidInvoices; }
    public long paidInvoices() { return paidInvoices; }

    /** Hàm `getItems`: lấy `Items`. */
    public List<DoanhThuBaoCaoMatHangDuLieu> getItems() { return items; }
    public List<DoanhThuBaoCaoMatHangDuLieu> items() { return items; }

    /** Hàm `getConsultationPercentage`: lấy `Consultation` `Percentage`. */
    public double getConsultationPercentage() {
        return calculatePercentage(consultationRevenue);
    }

    /** Hàm `getParaclinicalPercentage`: lấy canlamsang `Percentage`. */
    public double getParaclinicalPercentage() {
        return calculatePercentage(paraclinicalRevenue);
    }

    /** Hàm `getMedicationPercentage`: lấy thuốc `Percentage`. */
    public double getMedicationPercentage() {
        return calculatePercentage(medicationRevenue);
    }

    /** Hàm `calculatePercentage`: tính `Percentage`. */
    private double calculatePercentage(BigDecimal part) {
        if (totalRevenue.compareTo(BigDecimal.ZERO) <= 0 || part == null || part.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return part.multiply(new BigDecimal("100"))
                .divide(totalRevenue, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
