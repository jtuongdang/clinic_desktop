package vn.clinicmanagement.domain.common.events;

public interface IDomainEventPublisher {
    void publish(IDomainEvent event);
}
