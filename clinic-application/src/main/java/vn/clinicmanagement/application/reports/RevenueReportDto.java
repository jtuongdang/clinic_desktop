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
public class RevenueReportDto {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final BigDecimal totalRevenue;
    private final BigDecimal consultationRevenue;
    private final BigDecimal paraclinicalRevenue;
    private final BigDecimal medicationRevenue;
    private final BigDecimal otherRevenue;
    private final BigDecimal insurancePaidAmount;
    private final BigDecimal patientPaidAmount;
    private final BigDecimal cashAmount;
    private final BigDecimal transferAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal advanceAmount;
    private final long totalInvoices;
    private final long paidInvoices;
    private final List<RevenueReportItemDto> items;

    public RevenueReportDto(LocalDate fromDate, LocalDate toDate,
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
                            List<RevenueReportItemDto> items) {
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

    public LocalDate getFromDate() { return fromDate; }
    public LocalDate fromDate() { return fromDate; }

    public LocalDate getToDate() { return toDate; }
    public LocalDate toDate() { return toDate; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public BigDecimal totalRevenue() { return totalRevenue; }

    public BigDecimal getConsultationRevenue() { return consultationRevenue; }
    public BigDecimal consultationRevenue() { return consultationRevenue; }

    public BigDecimal getParaclinicalRevenue() { return paraclinicalRevenue; }
    public BigDecimal paraclinicalRevenue() { return paraclinicalRevenue; }

    public BigDecimal getMedicationRevenue() { return medicationRevenue; }
    public BigDecimal medicationRevenue() { return medicationRevenue; }

    public BigDecimal getOtherRevenue() { return otherRevenue; }
    public BigDecimal otherRevenue() { return otherRevenue; }

    public BigDecimal getInsurancePaidAmount() { return insurancePaidAmount; }
    public BigDecimal insurancePaidAmount() { return insurancePaidAmount; }

    public BigDecimal getPatientPaidAmount() { return patientPaidAmount; }
    public BigDecimal patientPaidAmount() { return patientPaidAmount; }

    public BigDecimal getCashAmount() { return cashAmount; }
    public BigDecimal cashAmount() { return cashAmount; }

    public BigDecimal getTransferAmount() { return transferAmount; }
    public BigDecimal transferAmount() { return transferAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal discountAmount() { return discountAmount; }

    public BigDecimal getAdvanceAmount() { return advanceAmount; }
    public BigDecimal advanceAmount() { return advanceAmount; }

    public long getTotalInvoices() { return totalInvoices; }
    public long totalInvoices() { return totalInvoices; }

    public long getPaidInvoices() { return paidInvoices; }
    public long paidInvoices() { return paidInvoices; }

    public List<RevenueReportItemDto> getItems() { return items; }
    public List<RevenueReportItemDto> items() { return items; }

    public double getConsultationPercentage() {
        return calculatePercentage(consultationRevenue);
    }

    public double getParaclinicalPercentage() {
        return calculatePercentage(paraclinicalRevenue);
    }

    public double getMedicationPercentage() {
        return calculatePercentage(medicationRevenue);
    }

    private double calculatePercentage(BigDecimal part) {
        if (totalRevenue.compareTo(BigDecimal.ZERO) <= 0 || part == null || part.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return part.multiply(new BigDecimal("100"))
                .divide(totalRevenue, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
