package vn.clinicmanagement.domain.common.exceptions;

public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String entityName, Object entityId) {
        super("Không tìm thấy thực thể " + entityName + " với ID: " + entityId);
    }
}
