package vn.clinicmanagement.domain.common.events;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

public abstract class TruuTuongMienNghiepVuSuKien implements IMienNghiepVuSuKien {
    /** Trường `eventId`: sukien mã. */
    private final UUID eventId = UUID.randomUUID();
    /** Trường `occurredOn`: `occurred` tải. */
    private final LocalDateTime occurredOn;

    protected TruuTuongMienNghiepVuSuKien() {
        this(Clock.systemUTC());
    }

    protected TruuTuongMienNghiepVuSuKien(Clock clock) {
        Clock eventClock = Objects.requireNonNull(clock, "clock must not be null");
        this.occurredOn = LocalDateTime.ofInstant(eventClock.instant(), ZoneOffset.UTC);
    }

    /** Hàm `getEventId`: lấy sukien mã. Gốc khai báo tại `vn.clinicmanagement.domain.common.events.IMienNghiepVuSuKien#getEventId`. */
    @Override
    public UUID getEventId() { return eventId; }
    /** Hàm `getOccurredOn`: lấy `Occurred` tải. Gốc khai báo tại `vn.clinicmanagement.domain.common.events.IMienNghiepVuSuKien#getOccurredOn`. */
    @Override
    public LocalDateTime getOccurredOn() { return occurredOn; }
}
