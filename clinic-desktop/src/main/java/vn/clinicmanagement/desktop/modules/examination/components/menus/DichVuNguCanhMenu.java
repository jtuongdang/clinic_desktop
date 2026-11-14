package vn.clinicmanagement.desktop.modules.examination.components.menus;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Menu ngữ cảnh khi bấm chuột phải vào dịch vụ trong màn hình "Chỉ Định Dịch Vụ".
 * Khớp 100% chi tiết trong ảnh chụp mẫu thực tế:
 * - Tiêu đề: "Dịch Vụ" (chữ màu xanh dương)
 * - Vị Trí Răng ▸
 * - Vị Trí Huyệt Đạo ▸
 * - Chuyển Loại PTTT ▸
 * - (Đường kẻ ngang)
 * - Sửa Đối Tượng ▸
 * - Sửa Số Lượng
 * - Sửa Giá
 * - Số Tiền Cần Nộp
 * - Ghi Chú
 * - (Đường kẻ ngang)
 * - Khóa Đối Tượng Dịch Vụ
 * - Xóa Dịch Vụ
 */
public class DichVuNguCanhMenu extends JPopupMenu {

    public DichVuNguCanhMenu(String serviceCode, String serviceName, Runnable onDeleteAction) {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(130, 145, 165), 1));

        // Tiêu đề: "Dịch Vụ" màu xanh dương
        JLabel lblHeader = new JLabel("Dịch Vụ");
        lblHeader.setFont(UngDungPhongChu.SMALL_BOLD);
        lblHeader.setForeground(new Color(0, 70, 180));
        lblHeader.setBorder(new EmptyBorder(4, 10, 3, 10));
        lblHeader.setOpaque(true);
        lblHeader.setBackground(new Color(242, 246, 252));
        add(lblHeader);

        add(createMenuItem("Vị Trí Răng", new ToothIcon(), true, null));
        add(createMenuItem("Vị Trí Huyệt Đạo", new AcupunctureIcon(), true, null));
        add(createMenuItem("Chuyển Loại PTTT", new PyramidIcon(), true, null));

        addSeparator();

        add(createMenuItem("Sửa Đối Tượng", new UserDocIcon(), true, null));
        add(createMenuItem("Sửa Số Lượng", new EditNumberIcon(), false, () -> {
            JOptionPane.showInputDialog(null, "Nhập số lượng mới cho dịch vụ " + serviceCode + ":", "1");
        }));
        add(createMenuItem("Sửa Giá", new PriceKeyIcon(), false, null));
        add(createMenuItem("Số Tiền Cần Nộp", new CalculatorIcon(), false, null));
        add(createMenuItem("Ghi Chú", new NoteChatIcon(), false, () -> {
            JOptionPane.showInputDialog(null, "Ghi chú cho dịch vụ " + serviceCode + ":", "");
        }));

        addSeparator();

        add(createMenuItem("Khóa Đối Tượng Dịch Vụ", new LockIcon(), false, null));

        JMenuItem miDelete = createMenuItem("Xóa Dịch Vụ", new RedCrossMiniIcon(), false, onDeleteAction);
        add(miDelete);
    }

    /** Hàm `createMenuItem`: tạo menu mathang. */
    private JMenuItem createMenuItem(String text, Icon icon, boolean hasSubmenu, Runnable action) {
        JMenuItem item = new JMenuItem(text, icon);
        item.setFont(UngDungPhongChu.SMALL);
        item.setForeground(new Color(25, 30, 40));
        item.setBackground(Color.WHITE);
        item.setPreferredSize(new Dimension(175, 22));
        item.setBorder(new EmptyBorder(2, 6, 2, 6));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (hasSubmenu) {
            item.setText(text + "       ▸");
        }

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if ("Xóa Dịch Vụ".equals(text)) {
                    item.setBackground(new Color(255, 240, 240)); // Nền hồng nhạt khi hover xóa
                    item.setForeground(new Color(200, 20, 20));
                } else {
                    item.setBackground(new Color(225, 238, 252)); // Nền xanh nhạt chuẩn EHC
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(Color.WHITE);
                item.setForeground(new Color(25, 30, 40));
            }
        });

        if (action != null) {
            item.addActionListener(e -> action.run());
        }

        return item;
    }

    // =========================================================================
    // VECTOR ICONS
    // =========================================================================

    public static class ToothIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(90, 105, 125));
            g2.drawRoundRect(x + 2, y + 2, 10, 8, 4, 4);
            g2.drawLine(x + 5, y + 10, x + 4, y + 13);
            g2.drawLine(x + 9, y + 10, x + 10, y + 13);
            g2.dispose();
        }
    }

    public static class AcupunctureIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(120, 70, 160));
            g2.drawLine(x + 2, y + 12, x + 12, y + 2);
            g2.fillOval(x + 10, y + 1, 3, 3);
            g2.dispose();
        }
    }

    public static class PyramidIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 130, 200));
            int[] px = {x + 7, x + 2, x + 12};
            int[] py = {y + 2, y + 12, y + 12};
            g2.drawPolygon(px, py, 3);
            g2.dispose();
        }
    }

    public static class UserDocIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(70, 120, 190));
            g2.drawOval(x + 4, y + 1, 6, 6);
            g2.drawArc(x + 2, y + 7, 10, 8, 0, 180);
            g2.dispose();
        }
    }

    public static class EditNumberIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(230, 140, 30));
            g2.drawRect(x + 2, y + 2, 10, 10);
            g2.drawString("#", x + 4, y + 11);
            g2.dispose();
        }
    }

    public static class PriceKeyIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(200, 160, 30));
            g2.drawOval(x + 2, y + 2, 5, 5);
            g2.drawLine(x + 7, y + 5, x + 12, y + 10);
            g2.drawLine(x + 10, y + 8, x + 12, y + 6);
            g2.dispose();
        }
    }

    public static class CalculatorIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(60, 140, 100));
            g2.drawRect(x + 2, y + 1, 10, 12);
            g2.drawLine(x + 4, y + 4, x + 10, y + 4);
            g2.drawLine(x + 4, y + 7, x + 6, y + 7);
            g2.drawLine(x + 8, y + 7, x + 10, y + 7);
            g2.drawLine(x + 4, y + 10, x + 6, y + 10);
            g2.drawLine(x + 8, y + 10, x + 10, y + 10);
            g2.dispose();
        }
    }

    public static class NoteChatIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 120, 215));
            g2.drawRoundRect(x + 1, y + 2, 12, 8, 3, 3);
            g2.drawLine(x + 4, y + 10, x + 4, y + 13);
            g2.drawLine(x + 4, y + 13, x + 7, y + 10);
            g2.dispose();
        }
    }

    public static class LockIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(100, 105, 115));
            g2.drawRoundRect(x + 3, y + 6, 8, 7, 2, 2);
            g2.drawArc(x + 4, y + 2, 6, 6, 0, 180);
            g2.dispose();
        }
    }

    public static class RedCrossMiniIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(205, 30, 30));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 2, x + 11, y + 11);
            g2.drawLine(x + 11, y + 2, x + 2, y + 11);
            g2.dispose();
        }
    }
}
