package vn.clinicmanagement.domain.common;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.events.IDomainEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaseEntityDomainEventsTest {

    @Test
    void registersDomainEvent() {
        TestEntity entity = new TestEntity();
        IDomainEvent event = syntheticEvent();

        entity.registerDomainEvent(event);

        assertEquals(List.of(event), entity.getDomainEvents());
    }

    @Test
    void rejectsNullEventWithoutChangingRegisteredEvents() {
        TestEntity entity = new TestEntity();
        IDomainEvent event = syntheticEvent();
        entity.registerDomainEvent(event);

        assertThrows(NullPointerException.class, () -> entity.registerDomainEvent(null));

        assertEquals(List.of(event), entity.getDomainEvents());
    }

    @Test
    void getDomainEventsReturnsImmutableSnapshot() {
        TestEntity entity = new TestEntity();
        IDomainEvent firstEvent = syntheticEvent();
        IDomainEvent secondEvent = syntheticEvent();
        entity.registerDomainEvent(firstEvent);

        List<IDomainEvent> snapshot = entity.getDomainEvents();
        entity.registerDomainEvent(secondEvent);

        assertEquals(List.of(firstEvent), snapshot);
        assertEquals(List.of(firstEvent, secondEvent), entity.getDomainEvents());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(syntheticEvent()));
    }

    @Test
    void pullDomainEventsReturnsSnapshotAndClearsEntityEvents() {
        TestEntity entity = new TestEntity();
        IDomainEvent event = syntheticEvent();
        entity.registerDomainEvent(event);

        List<IDomainEvent> pulledEvents = entity.pullDomainEvents();

        assertEquals(List.of(event), pulledEvents);
        assertEquals(List.of(), entity.getDomainEvents());
        assertThrows(UnsupportedOperationException.class, () -> pulledEvents.add(syntheticEvent()));
    }

    @Test
    void repeatedPullReturnsEmptyImmutableSnapshot() {
        TestEntity entity = new TestEntity();
        entity.registerDomainEvent(syntheticEvent());

        entity.pullDomainEvents();
        List<IDomainEvent> secondPull = entity.pullDomainEvents();

        assertEquals(List.of(), secondPull);
        assertThrows(UnsupportedOperationException.class, () -> secondPull.add(syntheticEvent()));
    }

    private static IDomainEvent syntheticEvent() {
        return new TestDomainEvent(UUID.randomUUID(), LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    private static final class TestEntity extends BaseEntity<Integer> {
    }

    private record TestDomainEvent(UUID eventId, LocalDateTime occurredOn) implements IDomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }
}
