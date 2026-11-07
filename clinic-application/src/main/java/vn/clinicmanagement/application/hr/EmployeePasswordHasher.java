package vn.clinicmanagement.application.hr;

@FunctionalInterface
public interface EmployeePasswordHasher {
    String hash(char[] password);
}
