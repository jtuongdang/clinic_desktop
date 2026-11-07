package vn.clinicmanagement.application.reports;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * DTO chi tiết thống kê lượt khám theo khoa/phòng hoặc chỉ tiêu (RPT-01).
 * Hỗ trợ hiển thị trên bảng phân tích và ánh xạ 7 cột chuẩn của ClinicReportPanel.
 */
public class VisitStatisticsItemDto {

    private final int stt;
    private final int departmentId;
    private final String departmentName;
    private final Integer roomId;
    private final String roomName;
    private final long totalVisits;
    private final long completedVisits;
    private final long waitingVisits;
    private final long inProgressVisits;
    private final long cancelledVisits;
    private final double completionRate;
    private final long previousVisits;
    private final Double growthRate;
    private final String note;

    public VisitStatisticsItemDto(int stt, int departmentId, String departmentName,
                                  Integer roomId, String roomName,
                                  long totalVisits, long completedVisits, long waitingVisits,
                                  long inProgressVisits, long cancelledVisits,
                                  double completionRate, long previousVisits,
                                  Double growthRate, String note) {
        this.stt = stt;
        this.departmentId = departmentId;
        this.departmentName = departmentName != null ? departmentName : "";
        this.roomId = roomId;
        this.roomName = roomName != null ? roomName : "";
        this.totalVisits = totalVisits;
        this.completedVisits = completedVisits;
        this.waitingVisits = waitingVisits;
        this.inProgressVisits = inProgressVisits;
        this.cancelledVisits = cancelledVisits;
        this.completionRate = completionRate;
        this.previousVisits = previousVisits;
        this.growthRate = growthRate;
        this.note = note != null ? note : "";
    }

    public VisitStatisticsItemDto(int stt, int departmentId, String departmentName,
                                  long totalVisits, long completedVisits, long waitingVisits,
                                  long inProgressVisits, long cancelledVisits,
                                  double completionRate, String note) {
        this(stt, departmentId, departmentName, null, null, totalVisits, completedVisits,
                waitingVisits, inProgressVisits, cancelledVisits, completionRate, 0L, null, note);
    }

    public int getStt() { return stt; }
    public int stt() { return stt; }

    public int getDepartmentId() { return departmentId; }
    public int departmentId() { return departmentId; }

    public String getDepartmentName() { return departmentName; }
    public String departmentName() { return departmentName; }

    public Integer getRoomId() { return roomId; }
    public Integer roomId() { return roomId; }

    public String getRoomName() { return roomName; }
    public String roomName() { return roomName; }

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

    public long getPreviousVisits() { return previousVisits; }
    public long previousVisits() { return previousVisits; }

    public Double getGrowthRate() { return growthRate; }
    public Double growthRate() { return growthRate; }

    public String getNote() { return note; }
    public String note() { return note; }

    /**
     * Chuyển đổi thành hàng 7 cột tương thích với ClinicReportPanel:
     * {"STT", "Chỉ Tiêu / Hạng Mục", "Đơn Vị Tính", "Kỳ Trước", "Kỳ Này", "Tăng/Giảm (%)", "Ghi Chú"}
     */
    public Object[] toTableRow(int rowIndex) {
        NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        String targetName = departmentName;
        if (roomName != null && !roomName.isBlank()) {
            targetName += " - " + roomName;
        }
        String growthStr = growthRate != null ? String.format(Locale.ROOT, "%+.1f%%", growthRate) : "-";
        String noteStr = String.format(Locale.ROOT, "Hoàn tất: %d (%.1f%%) | Chờ: %d | Hủy: %d",
                completedVisits, completionRate, waitingVisits, cancelledVisits);
        if (note != null && !note.isBlank()) {
            noteStr = note + " (" + noteStr + ")";
        }

        return new Object[] {
                String.valueOf(rowIndex),
                targetName,
                "Lượt",
                previousVisits > 0 ? fmt.format(previousVisits) : "-",
                fmt.format(totalVisits),
                growthStr,
                noteStr
        };
    }
}
