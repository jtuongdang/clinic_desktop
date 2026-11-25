package vn.clinicmanagement.domain.common.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NgayKhoangTest {

    @Test
    void rejectsEndDateBeforeKnownStartDate() {
        LocalDate start = LocalDate.of(2025, 1, 10);

        assertThrows(IllegalArgumentException.class,
                () -> new NgayKhoang(start, LocalDate.of(2025, 1, 9)));
    }

    @Test
    void acceptsUnknownStartDateWithoutClaimingDateMembership() {
        LocalDate end = LocalDate.of(2025, 1, 12);
        NgayKhoang range = new NgayKhoang(null, end);

        assertFalse(range.contains(LocalDate.of(2025, 1, 11)));
        assertFalse(range.contains(null));
        assertTrue(range.toString().startsWith("Chưa rõ -> "));
    }

    @Test
    void containsBothInclusiveEndpointsAndRejectsDatesOutsideRange() {
        LocalDate start = LocalDate.of(2025, 1, 10);
        LocalDate end = LocalDate.of(2025, 1, 12);
        NgayKhoang range = new NgayKhoang(start, end);

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
        NgayKhoang range = new NgayKhoang(start, null);

        assertTrue(range.contains(start));
        assertTrue(range.contains(LocalDate.of(2025, 1, 11)));
        assertFalse(range.contains(LocalDate.of(2025, 1, 9)));
    }

    @Test
    void overlapIncludesSharedBoundaryAndRejectsDisjointRanges() {
        NgayKhoang first = new NgayKhoang(
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 12));
        NgayKhoang touching = new NgayKhoang(
                LocalDate.of(2025, 1, 12), LocalDate.of(2025, 1, 15));
        NgayKhoang disjoint = new NgayKhoang(
                LocalDate.of(2025, 1, 13), LocalDate.of(2025, 1, 15));

        assertTrue(first.overlaps(touching));
        assertTrue(touching.overlaps(first));
        assertFalse(first.overlaps(disjoint));
        assertFalse(disjoint.overlaps(first));
        assertFalse(first.overlaps(null));
    }

    @Test
    void openEndedRangesOverlapLaterRangesButNotEarlierDisjointRanges() {
        NgayKhoang openEnded = new NgayKhoang(
                LocalDate.of(2025, 1, 10), null);
        NgayKhoang later = new NgayKhoang(
                LocalDate.of(2025, 1, 20), LocalDate.of(2025, 1, 25));
        NgayKhoang earlier = new NgayKhoang(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 9));

        assertTrue(openEnded.overlaps(later));
        assertTrue(later.overlaps(openEnded));
        assertFalse(openEnded.overlaps(earlier));
        assertFalse(earlier.overlaps(openEnded));
    }

    @Test
    void unknownStartRangeConservativelyOverlapsEarlierAndTouchingRanges() {
        NgayKhoang unknownStart = new NgayKhoang(null, LocalDate.of(2025, 1, 12));
        NgayKhoang earlier = new NgayKhoang(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2));
        NgayKhoang touching = new NgayKhoang(LocalDate.of(2025, 1, 12), LocalDate.of(2025, 1, 15));
        NgayKhoang later = new NgayKhoang(LocalDate.of(2025, 1, 13), LocalDate.of(2025, 1, 15));

        assertTrue(unknownStart.overlaps(earlier));
        assertTrue(earlier.overlaps(unknownStart));
        assertTrue(unknownStart.overlaps(touching));
        assertTrue(touching.overlaps(unknownStart));
        assertFalse(unknownStart.overlaps(later));
        assertFalse(later.overlaps(unknownStart));
    }
}
