package vn.clinicmanagement.application.auth;

public interface PasswordHasher {
    boolean verify(char[] password, String encodedHash);
}
