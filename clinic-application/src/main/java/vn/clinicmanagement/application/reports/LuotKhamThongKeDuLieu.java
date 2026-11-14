package vn.clinicmanagement.application.reports;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DTO tổng hợp thống kê lượt khám bệnh và phân bố khoa/phòng (RPT-01).
 * Cung cấp tỷ lệ hoàn tất, phân bố theo khoa phòng và theo trạng thái.
 */
public class LuotKhamThongKeDuLieu {

    /** Trường `fromDate`: tu ngày. */
    private final LocalDate fromDate;
    /** Trường `toDate`: den ngày. */
    private final LocalDate toDate;
    private final long totalVisits;
    /** Trường `completedVisits`: dahoantat `Visits`. */
    private final long completedVisits;
    private final long waitingVisits;
    private final long inProgressVisits;
    /** Trường `cancelledVisits`: dahuy `Visits`. */
    private final long cancelledVisits;
    private final double completionRate;
    /** Trường `departmentBreakdown`: khoa `Breakdown`. */
    private final Map<String, Long> departmentBreakdown;
    /** Trường `statusBreakdown`: trạng thái `Breakdown`. */
    private final Map<String, Long> statusBreakdown;
    private final List<LuotKhamThongKeMatHangDuLieu> items;

    public LuotKhamThongKeDuLieu(LocalDate fromDate, LocalDate toDate,
                              long totalVisits, long completedVisits,
                              long waitingVisits, long inProgressVisits,
                              long cancelledVisits, double completionRate,
                              Map<String, Long> departmentBreakdown,
                              Map<String, Long> statusBreakdown,
                              List<LuotKhamThongKeMatHangDuLieu> items) {
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

    /** Hàm `getFromDate`: lấy tu ngày. */
    public LocalDate getFromDate() { return fromDate; }
    /** Hàm `fromDate`: tu ngày. */
    public LocalDate fromDate() { return fromDate; }

    /** Hàm `getToDate`: lấy den ngày. */
    public LocalDate getToDate() { return toDate; }
    /** Hàm `toDate`: den ngày. */
    public LocalDate toDate() { return toDate; }

    /** Hàm `getTotalVisits`: lấy `Total` `Visits`. */
    public long getTotalVisits() { return totalVisits; }
    public long totalVisits() { return totalVisits; }

    /** Hàm `getCompletedVisits`: lấy dahoantat `Visits`. */
    public long getCompletedVisits() { return completedVisits; }
    /** Hàm `completedVisits`: dahoantat `Visits`. */
    public long completedVisits() { return completedVisits; }

    /** Hàm `getWaitingVisits`: lấy `Waiting` `Visits`. */
    public long getWaitingVisits() { return waitingVisits; }
    public long waitingVisits() { return waitingVisits; }

    /** Hàm `getInProgressVisits`: lấy trong `Progress` `Visits`. */
    public long getInProgressVisits() { return inProgressVisits; }
    public long inProgressVisits() { return inProgressVisits; }

    /** Hàm `getCancelledVisits`: lấy dahuy `Visits`. */
    public long getCancelledVisits() { return cancelledVisits; }
    /** Hàm `cancelledVisits`: dahuy `Visits`. */
    public long cancelledVisits() { return cancelledVisits; }

    /** Hàm `getCompletionRate`: lấy `Completion` `Rate`. */
    public double getCompletionRate() { return completionRate; }
    public double completionRate() { return completionRate; }

    /** Hàm `getDepartmentBreakdown`: lấy khoa `Breakdown`. */
    public Map<String, Long> getDepartmentBreakdown() { return departmentBreakdown; }
    /** Hàm `departmentBreakdown`: khoa `Breakdown`. */
    public Map<String, Long> departmentBreakdown() { return departmentBreakdown; }

    /** Hàm `getStatusBreakdown`: lấy trạng thái `Breakdown`. */
    public Map<String, Long> getStatusBreakdown() { return statusBreakdown; }
    /** Hàm `statusBreakdown`: trạng thái `Breakdown`. */
    public Map<String, Long> statusBreakdown() { return statusBreakdown; }

    /** Hàm `getItems`: lấy `Items`. */
    public List<LuotKhamThongKeMatHangDuLieu> getItems() { return items; }
    public List<LuotKhamThongKeMatHangDuLieu> items() { return items; }
}
