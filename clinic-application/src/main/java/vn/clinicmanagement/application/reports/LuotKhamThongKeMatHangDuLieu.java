package vn.clinicmanagement.application.reports;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * DTO chi tiết thống kê lượt khám theo khoa/phòng hoặc chỉ tiêu (RPT-01).
 * Hỗ trợ hiển thị trên bảng phân tích và ánh xạ 7 cột chuẩn của ClinicReportPanel.
 */
public class LuotKhamThongKeMatHangDuLieu {

    private final int stt;
    /** Trường `departmentId`: khoa mã. */
    private final int departmentId;
    /** Trường `departmentName`: khoa tên. */
    private final String departmentName;
    /** Trường `roomId`: phòng mã. */
    private final Integer roomId;
    /** Trường `roomName`: phòng tên. */
    private final String roomName;
    private final long totalVisits;
    /** Trường `completedVisits`: dahoantat `Visits`. */
    private final long completedVisits;
    private final long waitingVisits;
    private final long inProgressVisits;
    /** Trường `cancelledVisits`: dahuy `Visits`. */
    private final long cancelledVisits;
    private final double completionRate;
    private final long previousVisits;
    private final Double growthRate;
    private final String note;

    public LuotKhamThongKeMatHangDuLieu(int stt, int departmentId, String departmentName,
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

    public LuotKhamThongKeMatHangDuLieu(int stt, int departmentId, String departmentName,
                                  long totalVisits, long completedVisits, long waitingVisits,
                                  long inProgressVisits, long cancelledVisits,
                                  double completionRate, String note) {
        this(stt, departmentId, departmentName, null, null, totalVisits, completedVisits,
                waitingVisits, inProgressVisits, cancelledVisits, completionRate, 0L, null, note);
    }

    /** Hàm `getStt`: lấy `Stt`. */
    public int getStt() { return stt; }
    public int stt() { return stt; }

    /** Hàm `getDepartmentId`: lấy khoa mã. */
    public int getDepartmentId() { return departmentId; }
    /** Hàm `departmentId`: khoa mã. */
    public int departmentId() { return departmentId; }

    /** Hàm `getDepartmentName`: lấy khoa tên. */
    public String getDepartmentName() { return departmentName; }
    /** Hàm `departmentName`: khoa tên. */
    public String departmentName() { return departmentName; }

    /** Hàm `getRoomId`: lấy phòng mã. */
    public Integer getRoomId() { return roomId; }
    /** Hàm `roomId`: phòng mã. */
    public Integer roomId() { return roomId; }

    /** Hàm `getRoomName`: lấy phòng tên. */
    public String getRoomName() { return roomName; }
    /** Hàm `roomName`: phòng tên. */
    public String roomName() { return roomName; }

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

    /** Hàm `getPreviousVisits`: lấy `Previous` `Visits`. */
    public long getPreviousVisits() { return previousVisits; }
    public long previousVisits() { return previousVisits; }

    /** Hàm `getGrowthRate`: lấy `Growth` `Rate`. */
    public Double getGrowthRate() { return growthRate; }
    public Double growthRate() { return growthRate; }

    /** Hàm `getNote`: lấy `Note`. */
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
