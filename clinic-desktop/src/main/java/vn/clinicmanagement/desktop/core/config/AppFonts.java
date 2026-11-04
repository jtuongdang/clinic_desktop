package vn.clinicmanagement.desktop.core.config;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Hệ thống chữ DUY NHẤT của toàn ứng dụng.
 *
 * Quy chuẩn rút ra từ ảnh phần mềm EHC mẫu:
 * - Nhãn (label), nội dung bảng, menu   : 13 thường
 * - Giá trị dữ liệu, tên bệnh nhân, tab : 13 ĐẬM
 * - Tiêu đề cột bảng, tiêu đề nhóm      : 13 ĐẬM
 * - Tiêu đề phân hệ / banner bệnh nhân  : 15 ĐẬM
 * - Tiêu đề lớn (đăng nhập, trang chủ)  : 18 ĐẬM
 * - Chữ phụ (mã BN dưới tên, thanh trạng thái): 12 thường
 *
 * Mọi nơi trong code CHỈ được dùng các hằng số bên dưới, không tự tạo new Font(...).
 * Muốn đổi cỡ chữ toàn hệ thống chỉ cần sửa các hằng số / FONT_SCALE tại đây.
 */
public final class AppFonts {
    private AppFonts() {}

    /** Hệ số phóng chữ toàn hệ thống (1.0 = chuẩn). Ví dụ 1.15 cho màn hình độ phân giải cao. */
    private static final float FONT_SCALE = 1.0f;

    /** Ưu tiên Segoe UI (Windows); nếu máy không có thì dùng Tahoma (giống EHC) rồi tới Arial. */
    public static final String FAMILY = resolveFamily("Segoe UI", "Arial", "Tahoma", Font.SANS_SERIF);

    public static final Font BODY          = make(Font.PLAIN, 13);
    public static final Font BODY_BOLD     = make(Font.BOLD, 13);
    public static final Font SMALL         = make(Font.PLAIN, 12);
    public static final Font SMALL_BOLD    = make(Font.BOLD, 12);
    public static final Font SECTION       = make(Font.BOLD, 15);
    public static final Font HEADER        = make(Font.BOLD, 18);
    public static final Font ICON          = make(Font.PLAIN, 14);

    private static Font make(int style, int size) {
        return new Font(FAMILY, style, Math.round(size * FONT_SCALE));
    }

    private static String resolveFamily(String... candidates) {
        Set<String> installed = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String c : candidates) {
            if (installed.contains(c)) return c;
        }
        return Font.SANS_SERIF;
    }

    /**
     * Áp font chuẩn lên MỌI thành phần Swing (JLabel, JButton, JTable, JTabbedPane, JComboBox...).
     * Phải gọi một lần trước khi tạo bất kỳ cửa sổ nào.
     */
    public static void installGlobally() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof String && ((String) key).endsWith(".font")) {
                String k = (String) key;
                Font target = BODY;
                if (k.startsWith("TableHeader") || k.startsWith("TitledBorder")) target = BODY_BOLD;
                UIManager.put(key, new FontUIResource(target));
            }
        }
        UIManager.put("defaultFont", new FontUIResource(BODY));
    }
}
