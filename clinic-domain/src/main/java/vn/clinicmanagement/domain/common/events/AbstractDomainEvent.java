package vn.clinicmanagement.domain.common.events;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class AbstractDomainEvent implements IDomainEvent {
    private final UUID eventId = UUID.randomUUID();
    private final LocalDateTime occurredOn = LocalDateTime.now();

    @Override
    public UUID getEventId() { return eventId; }
    @Override
    public LocalDateTime getOccurredOn() { return occurredOn; }
}
