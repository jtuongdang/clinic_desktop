package vn.clinicmanagement.domain.common.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A domain event with a unique identifier and an occurrence timestamp.
 */
public interface IDomainEvent {
    UUID getEventId();

    /**
     * Returns the event occurrence time as a timezone-free {@link LocalDateTime}
     * whose fields represent UTC.
     *
     * @return the event occurrence time represented in UTC
     */
    LocalDateTime getOccurredOn();
}
