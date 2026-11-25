package vn.clinicmanagement.desktop;

import vn.clinicmanagement.desktop.modules.auth.DangNhapBieuMau;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

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
        UngDungPhongChu.installGlobally();

        // Khởi chạy cửa sổ đăng nhập trên Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            DangNhapBieuMau loginForm = new DangNhapBieuMau();
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
