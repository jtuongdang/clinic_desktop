package vn.clinicmanagement.desktop;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.modules.auth.LoginForm;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Điểm khởi chạy ứng dụng (Bootstrap Main Entry Point)
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        configureLookAndFeel("com.formdev.flatlaf.FlatLightLaf");

        // Áp font chuẩn lên mọi thành phần Swing SAU khi đã chọn Look and Feel
        AppFonts.installGlobally();

        // Khởi chạy cửa sổ đăng nhập trên Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginForm loginForm = new LoginForm();
            loginForm.setVisible(true);
        });
    }

    static void configureLookAndFeel(String flatLafClassName) {
        try {
            Class<?> flatLafClass = Class.forName(flatLafClassName);
            flatLafClass.getMethod("setup").invoke(null);
        } catch (InvocationTargetException setupFailure) {
            if (setupFailure.getCause() instanceof Error fatalFailure) {
                throw fatalFailure;
            }
            installSystemLookAndFeel();
        } catch (ReflectiveOperationException | NoClassDefFoundError unavailableFlatLaf) {
            installSystemLookAndFeel();
        }
    }

    private static void installSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }
}
