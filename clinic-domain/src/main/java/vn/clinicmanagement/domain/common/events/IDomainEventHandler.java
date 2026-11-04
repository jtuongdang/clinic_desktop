package vn.clinicmanagement.domain.common.events;

public interface IDomainEventHandler<T extends IDomainEvent> {
    void handle(T event);
}
