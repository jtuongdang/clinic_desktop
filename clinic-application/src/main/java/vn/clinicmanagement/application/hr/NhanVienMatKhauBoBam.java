package vn.clinicmanagement.application.hr;

@FunctionalInterface
public interface NhanVienMatKhauBoBam {
    String hash(char[] password);
}
