package vn.clinicmanagement.application.reports;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DTO tổng hợp thống kê lượt khám bệnh và phân bố khoa/phòng (RPT-01).
 * Cung cấp tỷ lệ hoàn tất, phân bố theo khoa phòng và theo trạng thái.
 */
public class VisitStatisticsDto {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final long totalVisits;
    private final long completedVisits;
    private final long waitingVisits;
    private final long inProgressVisits;
    private final long cancelledVisits;
    private final double completionRate;
    private final Map<String, Long> departmentBreakdown;
    private final Map<String, Long> statusBreakdown;
    private final List<VisitStatisticsItemDto> items;

    public VisitStatisticsDto(LocalDate fromDate, LocalDate toDate,
                              long totalVisits, long completedVisits,
                              long waitingVisits, long inProgressVisits,
                              long cancelledVisits, double completionRate,
                              Map<String, Long> departmentBreakdown,
                              Map<String, Long> statusBreakdown,
                              List<VisitStatisticsItemDto> items) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.totalVisits = totalVisits;
        this.completedVisits = completedVisits;
        this.waitingVisits = waitingVisits;
        this.inProgressVisits = inProgressVisits;
        this.cancelledVisits = cancelledVisits;
        this.completionRate = completionRate;
        this.departmentBreakdown = departmentBreakdown != null ? Map.copyOf(departmentBreakdown) : Map.of();
        this.statusBreakdown = statusBreakdown != null ? Map.copyOf(statusBreakdown) : Map.of();
        this.items = items != null ? List.copyOf(items) : List.of();
    }

    public LocalDate getFromDate() { return fromDate; }
    public LocalDate fromDate() { return fromDate; }

    public LocalDate getToDate() { return toDate; }
    public LocalDate toDate() { return toDate; }

    public long getTotalVisits() { return totalVisits; }
    public long totalVisits() { return totalVisits; }

    public long getCompletedVisits() { return completedVisits; }
    public long completedVisits() { return completedVisits; }

    public long getWaitingVisits() { return waitingVisits; }
    public long waitingVisits() { return waitingVisits; }

    public long getInProgressVisits() { return inProgressVisits; }
    public long inProgressVisits() { return inProgressVisits; }

    public long getCancelledVisits() { return cancelledVisits; }
    public long cancelledVisits() { return cancelledVisits; }

    public double getCompletionRate() { return completionRate; }
    public double completionRate() { return completionRate; }

    public Map<String, Long> getDepartmentBreakdown() { return departmentBreakdown; }
    public Map<String, Long> departmentBreakdown() { return departmentBreakdown; }

    public Map<String, Long> getStatusBreakdown() { return statusBreakdown; }
    public Map<String, Long> statusBreakdown() { return statusBreakdown; }

    public List<VisitStatisticsItemDto> getItems() { return items; }
    public List<VisitStatisticsItemDto> items() { return items; }
}
