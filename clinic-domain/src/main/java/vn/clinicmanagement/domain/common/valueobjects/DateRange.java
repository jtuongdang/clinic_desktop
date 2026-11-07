package vn.clinicmanagement.domain.common.valueobjects;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public final class DateRange implements Serializable {
    private final LocalDate startDate;
    private final LocalDate endDate;

    public DateRange(LocalDate startDate, LocalDate endDate) {
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
     * Returns whether the ranges overlap or could overlap if an endpoint is unknown.
     * A missing start is conservative here, while {@link #contains(LocalDate)} does not
     * claim a date is inside a range whose start is unknown.
     */
    public boolean overlaps(DateRange other) {
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
        DateRange dateRange = (DateRange) o;
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
