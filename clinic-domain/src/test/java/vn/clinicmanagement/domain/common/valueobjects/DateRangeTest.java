package vn.clinicmanagement.domain.common.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRangeTest {

    @Test
    void rejectsNullStartDateAndEndDateBeforeStartDate() {
        LocalDate start = LocalDate.of(2025, 1, 10);

        assertThrows(IllegalArgumentException.class, () -> new DateRange(null, start));
        assertThrows(IllegalArgumentException.class,
                () -> new DateRange(start, LocalDate.of(2025, 1, 9)));
    }

    @Test
    void containsBothInclusiveEndpointsAndRejectsDatesOutsideRange() {
        LocalDate start = LocalDate.of(2025, 1, 10);
        LocalDate end = LocalDate.of(2025, 1, 12);
        DateRange range = new DateRange(start, end);

        assertTrue(range.contains(start));
        assertTrue(range.contains(end));
        assertTrue(range.contains(LocalDate.of(2025, 1, 11)));
        assertFalse(range.contains(LocalDate.of(2025, 1, 9)));
        assertFalse(range.contains(LocalDate.of(2025, 1, 13)));
        assertFalse(range.contains(null));
    }

    @Test
    void openEndedRangeContainsDatesFromStartOnward() {
        LocalDate start = LocalDate.of(2025, 1, 10);
        DateRange range = new DateRange(start, null);

        assertTrue(range.contains(start));
        assertTrue(range.contains(LocalDate.of(2025, 1, 11)));
        assertFalse(range.contains(LocalDate.of(2025, 1, 9)));
    }

    @Test
    void overlapIncludesSharedBoundaryAndRejectsDisjointRanges() {
        DateRange first = new DateRange(
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 12));
        DateRange touching = new DateRange(
                LocalDate.of(2025, 1, 12), LocalDate.of(2025, 1, 15));
        DateRange disjoint = new DateRange(
                LocalDate.of(2025, 1, 13), LocalDate.of(2025, 1, 15));

        assertTrue(first.overlaps(touching));
        assertTrue(touching.overlaps(first));
        assertFalse(first.overlaps(disjoint));
        assertFalse(disjoint.overlaps(first));
        assertFalse(first.overlaps(null));
    }

    @Test
    void openEndedRangesOverlapLaterRangesButNotEarlierDisjointRanges() {
        DateRange openEnded = new DateRange(
                LocalDate.of(2025, 1, 10), null);
        DateRange later = new DateRange(
                LocalDate.of(2025, 1, 20), LocalDate.of(2025, 1, 25));
        DateRange earlier = new DateRange(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 9));

        assertTrue(openEnded.overlaps(later));
        assertTrue(later.overlaps(openEnded));
        assertFalse(openEnded.overlaps(earlier));
        assertFalse(earlier.overlaps(openEnded));
    }
}
