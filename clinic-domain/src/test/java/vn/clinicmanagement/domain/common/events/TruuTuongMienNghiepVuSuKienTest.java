package vn.clinicmanagement.domain.common.events;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TruuTuongMienNghiepVuSuKienTest {

    @Test
    void convertsFixedClockInstantToUtcLocalDateTimeRegardlessOfClockZone() {
        Instant instant = Instant.parse("2026-02-03T04:05:06Z");
        ZoneId nonUtcZone = ZoneId.of("Asia/Tokyo");
        Clock clock = Clock.fixed(instant, nonUtcZone);

        TestDomainEvent event = new TestDomainEvent(clock);

        assertEquals(LocalDateTime.of(2026, 2, 3, 4, 5, 6), event.getOccurredOn());
        assertEquals(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), event.getOccurredOn());
    }

    @Test
    void rejectsNullClockWithClearMessage() {
        NullPointerException exception = assertThrows(
                NullPointerException.class, () -> new TestDomainEvent(null));

        assertTrue(exception.getMessage().contains("clock"));
    }

    private static final class TestDomainEvent extends TruuTuongMienNghiepVuSuKien {
        private TestDomainEvent(Clock clock) {
            super(clock);
        }
    }
}
