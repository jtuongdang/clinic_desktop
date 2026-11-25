package vn.clinicmanagement.application.auth;

public interface MatKhauBoBam {
    boolean verify(char[] password, String encodedHash);
}
