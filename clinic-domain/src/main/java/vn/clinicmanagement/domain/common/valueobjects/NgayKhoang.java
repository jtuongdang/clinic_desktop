package vn.clinicmanagement.domain.common.valueobjects;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public final class NgayKhoang implements Serializable {
    private final LocalDate startDate;
    private final LocalDate endDate;

    public NgayKhoang(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Ngày kết thúc không thể trước ngày bắt đầu.");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean contains(LocalDate date) {
        if (date == null || startDate == null) return false;
        if (date.isBefore(startDate)) return false;
        return endDate == null || !date.isAfter(endDate);
    }

    /**
     * Cho biết hai khoảng có giao nhau hay có thể giao nhau khi chưa biết một đầu mút. Ở đây, thiếu ngày bắt đầu được xử lý theo hướng thận trọng; còn {@link #contains(LocalDate)} không khẳng định ngày nằm trong khoảng nếu chưa biết ngày bắt đầu.
     */
    public boolean overlaps(NgayKhoang other) {
        if (other == null) return false;
        if (this.endDate != null && other.startDate != null && other.startDate.isAfter(this.endDate)) return false;
        if (other.endDate != null && this.startDate != null && this.startDate.isAfter(other.endDate)) return false;
        return true;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NgayKhoang dateRange = (NgayKhoang) o;
        return Objects.equals(startDate, dateRange.startDate) && Objects.equals(endDate, dateRange.endDate);
    }

    @Override
    public int hashCode() { return Objects.hash(startDate, endDate); }

    @Override
    public String toString() {
        String start = startDate != null ? startDate.toString() : "Chưa rõ";
        String end = endDate != null ? endDate.toString() : "Hiện tại";
        return start + " -> " + end;
    }
}
