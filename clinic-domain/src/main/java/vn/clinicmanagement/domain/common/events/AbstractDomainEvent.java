package vn.clinicmanagement.domain.common.events;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractDomainEvent implements IDomainEvent {
    private final UUID eventId = UUID.randomUUID();
    private final LocalDateTime occurredOn;

    protected AbstractDomainEvent() {
        this(Clock.systemUTC());
    }

    protected AbstractDomainEvent(Clock clock) {
        Clock eventClock = Objects.requireNonNull(clock, "clock must not be null");
        this.occurredOn = LocalDateTime.ofInstant(eventClock.instant(), ZoneOffset.UTC);
    }

    @Override
    public UUID getEventId() { return eventId; }
    @Override
    public LocalDateTime getOccurredOn() { return occurredOn; }
}
