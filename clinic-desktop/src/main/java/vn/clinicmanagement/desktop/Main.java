package vn.clinicmanagement.desktop;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.modules.auth.LoginForm;

import javax.swing.*;

/**
 * Điểm khởi chạy ứng dụng (Bootstrap Main Entry Point)
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        // Thiết lập giao diện hiện đại (Tự nhận FlatLaf nếu có, nếu chưa thì fallback sang System Look & Feel)
        try {
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            flatLafClass.getMethod("setup").invoke(null);
        } catch (Throwable t) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // Áp font chuẩn lên mọi thành phần Swing SAU khi đã chọn Look and Feel
        AppFonts.installGlobally();

        // Khởi chạy cửa sổ đăng nhập trên Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginForm loginForm = new LoginForm();
            loginForm.setVisible(true);
        });
    }
}
