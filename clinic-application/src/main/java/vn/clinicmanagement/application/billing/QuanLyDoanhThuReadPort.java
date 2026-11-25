package vn.clinicmanagement.application.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cổng đọc dữ liệu cho phân hệ Quản Lý Doanh Thu và 5 báo cáo doanh thu.
 */
public interface QuanLyDoanhThuReadPort {

    record RevenueVisitRow(
            int visitId, LocalDateTime admittedAt, LocalDateTime dischargedAt,
            String patientCode, String patientName, LocalDate birthDate,
            String gender, String department, String room,
            String subjectType, String treatmentType, String status,
            BigDecimal totalCost, BigDecimal patientPaid
    ) {}

    record ReceiptReportRow(
            int receiptId, int invoiceId, String receiptCode,
            LocalDateTime receiptTime, LocalDateTime visitDate,
            String patientName, LocalDate birthDate, String gender, String address,
            String cashierName, String paymentMethod,
            String subjectType, String treatmentType, String examType,
            String department, String room,
            int bhytCount, int serviceCount, int feeCount, int lineCount,
            BigDecimal amount, BigDecimal totalRefund, BigDecimal totalAdvance,
            BigDecimal refundAmount, BigDecimal advanceAmount, BigDecimal discountAmount,
            String discountReason, String note, String serviceSummary,
            String receiptBook, boolean shiftBook, String receiptType, boolean closed,
            String invoiceStatus
    ) {}

    record ServiceTypeReportRow(
            String dateStr, String roomName, String groupType, String serviceName,
            String patientName, int qty, BigDecimal unitPrice,
            BigDecimal amount, BigDecimal discount, BigDecimal patientPay, BigDecimal paid
    ) {}

    record RoomOption(Integer id, String name) {
        @Override public String toString() { return name; }
    }

    record RoomServiceLine(String name, int qty, BigDecimal amount, BigDecimal discount, BigDecimal patientPay) {}

    record RoomVisitDetail(
            int visitId, String dateStr, String roomName, String patientName,
            String birthMale, String birthFemale, String address,
            BigDecimal amount, BigDecimal discount, BigDecimal patientPay, BigDecimal paid,
            List<RoomServiceLine> services
    ) {}

    record Service019RawRow(
            String dateStr, String roomName, String groupType, String serviceName,
            String patientCode, String patientName, String treatmentType, String examType,
            String patientSubject, String serviceSubject, String receiptBook,
            String cashierName, String performer, int qty, BigDecimal unitPrice,
            BigDecimal amount, BigDecimal discount, BigDecimal patientPay, BigDecimal paid
    ) {}

    record ClinicSummaryStats(
            int visitCount,
            BigDecimal totalRevenue,
            BigDecimal examRevenue,
            BigDecimal labRevenue,
            BigDecimal imagingRevenue,
            BigDecimal retailDrugRevenue,
            BigDecimal totalCollected,
            BigDecimal serviceDiscount,
            BigDecimal invoiceDiscount
    ) {}

    List<RevenueVisitRow> listVisits(int facilityId, boolean filterByDischarge, LocalDateTime from, LocalDateTime to);

    List<ReceiptReportRow> listReceiptReport(int facilityId, boolean filterByDate, int timeModeIndex,
                                             LocalDateTime from, LocalDateTime to, int sortModeIndex);

    List<String> listReceiptFilterValues(int facilityId, String filterKey);

    List<ServiceTypeReportRow> listServiceTypeReport(int facilityId, LocalDateTime from, LocalDateTime to,
                                                     Integer roomId, String groupType, String serviceName,
                                                     boolean groupByDate, boolean groupByRoom);

    List<String> listServiceNames();

    List<RoomOption> listClinicRooms(int facilityId);

    List<RoomVisitDetail> listRoomReport(int facilityId, LocalDateTime from, LocalDateTime to,
                                         Integer roomId, boolean groupByDate, boolean withService, boolean paidOnly);

    List<String> listService019FilterValues(int facilityId, String filterKey);

    List<Service019RawRow> listService019Report(int facilityId, int searchByIndex, LocalDateTime from, LocalDateTime to);

    ClinicSummaryStats getClinicSummary(int facilityId, LocalDateTime from, LocalDateTime to);
}
