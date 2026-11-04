package vn.clinicmanagement.domain.common.exceptions;

public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(String rule) { super("Vi phạm quy tắc miền: " + rule); }
}
