package vn.clinicmanagement.desktop.modules.examination.components.menus;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Menu Popup xổ xuống khi bấm vào nút "Menu" trên thanh công cụ khám bệnh.
 * Khớp 100% giao diện trong ảnh chụp thực tế:
 * 1. Thống Kê:
 *    - Thống Kê Bệnh Nhân
 * 2. Màn Hình Chờ:
 *    - [ℹ] Cập Nhật Thông Báo
 *    - Màn Hình Chờ Khám
 *    - Cấu Hình Phát Loa Từ Nhiều Phòng Khám
 *    - Cấu Hình Không Phát Loa Từ Màn Hình
 * 3. Bệnh Án (chữ màu đỏ):
 *    - Chuyển Loại Bệnh Án
 * 4. Hướng Dẫn:
 *    - Danh Sách Hướng Dẫn Sử Dụng
 *    - Danh Sách Lời Dặn Bác Sĩ
 * 5. Giấy Báo Tử:
 *    - Danh Sách Số Giấy Báo Tử
 *    - Chọn Số Báo Tử
 * 6. Cấu Hình:
 *    - [👥] Chọn Ê-Kíp Khám Bệnh
 *    - [❌] Dừng Đăng Ký BN
 *    - [⌧] Đóng
 */
public class ExaminationMenuPopupMenu extends JPopupMenu {

    private final Consumer<String> onSelectAction;

    public ExaminationMenuPopupMenu(Component parent, Consumer<String> onSelectAction) {
        this.onSelectAction = onSelectAction;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(135, 148, 165), 1));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // --- 1. THỐNG KÊ ---
        addSectionHeader("Thống Kê", false);
        addMenuItem("Thống Kê Bệnh Nhân", null);

        // --- 2. MÀN HÌNH CHỜ ---
        addSectionHeader("Màn Hình Chờ", false);
        addMenuItem("Cập Nhật Thông Báo", new InfoCircleIcon());
        addMenuItem("Màn Hình Chờ Khám", null);
        addMenuItem("Cấu Hình Phát Loa Từ Nhiều Phòng Khám", null);
        addMenuItem("Cấu Hình Không Phát Loa Từ Màn Hình", null);

        // --- 3. BỆNH ÁN (TIÊU ĐỀ MÀU ĐỎ ĐẬM) ---
        addSectionHeader("Bệnh Án", true);
        addMenuItem("Chuyển Loại Bệnh Án", null);

        // --- 4. HƯỚNG DẪN ---
        addSectionHeader("Hướng Dẫn", false);
        addMenuItem("Danh Sách Hướng Dẫn Sử Dụng", null);
        addMenuItem("Danh Sách Lời Dặn Bác Sĩ", null);

        // --- 5. GIẤY BÁO TỬ ---
        addSectionHeader("Giấy Báo Tử", false);
        addMenuItem("Danh Sách Số Giấy Báo Tử", null);
        addMenuItem("Chọn Số Báo Tử", null);

        // --- 6. CẤU HÌNH ---
        addSectionHeader("Cấu Hình", false);
        addMenuItem("Chọn Ê-Kíp Khám Bệnh", new TeamVectorIcon());
        addMenuItem("Dừng Đăng Ký BN", new RedCrossIcon());
        addMenuItem("Đóng", new CloseBoxIcon());
    }

    private void addSectionHeader(String title, boolean isRedTitle) {
        JLabel lblHeader = new JLabel(title);
        lblHeader.setFont(AppFonts.SMALL_BOLD);
        lblHeader.setForeground(isRedTitle ? new Color(185, 30, 30) : new Color(45, 55, 72));
        lblHeader.setHorizontalAlignment(SwingConstants.LEFT);
        lblHeader.setOpaque(true);
        lblHeader.setBackground(new Color(222, 228, 237)); // Nền xám nhạt phân nhóm chuẩn EHC
        lblHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(205, 214, 226)),
                new EmptyBorder(2, 10, 2, 8)
        ));
        lblHeader.setPreferredSize(new Dimension(260, 21));
        lblHeader.setMaximumSize(new Dimension(Short.MAX_VALUE, 21));
        lblHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(lblHeader);
    }

    private void addMenuItem(String title, Icon icon) {
        JMenuItem mi = new JMenuItem();
        mi.setLayout(new BorderLayout());
        mi.setBackground(Color.WHITE);
        mi.setPreferredSize(new Dimension(260, 22));
        mi.setMaximumSize(new Dimension(Short.MAX_VALUE, 22));
        mi.setBorder(new EmptyBorder(1, 8, 1, 8));
        mi.setCursor(new Cursor(Cursor.HAND_CURSOR));
        mi.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Khung bên trái chứa Icon (nếu có) và Tiêu đề
        JPanel leftContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 1));
        leftContainer.setOpaque(false);

        if (icon != null) {
            JLabel lblIcon = new JLabel(icon);
            leftContainer.add(lblIcon);
        } else {
            // Khoảng trống căn thẳng hàng lề với các mục có icon
            JLabel spacer = new JLabel();
            spacer.setPreferredSize(new Dimension(14, 14));
            leftContainer.add(spacer);
        }

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(AppFonts.SMALL);
        lblTitle.setForeground(new Color(25, 30, 40));
        leftContainer.add(lblTitle);

        mi.add(leftContainer, BorderLayout.WEST);

        mi.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mi.setBackground(new Color(225, 238, 252)); // Nền xanh nhạt hover chuẩn EHC
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mi.setBackground(Color.WHITE);
            }
        });

        mi.addActionListener(e -> {
            if (onSelectAction != null) {
                onSelectAction.accept(title);
            }
        });

        add(mi);
    }

    // =========================================================================
    // VECTOR ICONS KHỚP 100% ẢNH MẪU
    // =========================================================================

    /** Biểu tượng [ℹ] tròn xanh chữ i trắng */
    private static class InfoCircleIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(26, 115, 232));
            g2.fillOval(x + 1, y + 1, 12, 12);

            g2.setColor(Color.WHITE);
            g2.fillOval(x + 6, y + 3, 2, 2); // Chấm trên chữ i
            g2.fillRect(x + 6, y + 6, 2, 5); // Thân chữ i
            g2.dispose();
        }
    }

    /** Biểu tượng [👥] 2 người bác sĩ / ê-kíp khám bệnh */
    private static class TeamVectorIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(70, 90, 115));

            // Người 1 (trước)
            g2.fillOval(x + 2, y + 1, 5, 5);
            g2.fillArc(x, y + 6, 9, 7, 0, 180);

            // Người 2 (sau)
            g2.setColor(new Color(110, 130, 155));
            g2.fillOval(x + 7, y + 2, 5, 5);
            g2.fillArc(x + 5, y + 7, 9, 6, 0, 180);

            g2.dispose();
        }
    }

    /** Biểu tượng [❌] dấu X đỏ chéo */
    private static class RedCrossIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(215, 40, 40));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 2, x + 11, y + 11);
            g2.drawLine(x + 11, y + 2, x + 2, y + 11);
            g2.dispose();
        }
    }

    /** Biểu tượng [⌧] hộp vuông viền đỏ có dấu X */
    private static class CloseBoxIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(215, 40, 40));
            g2.drawRect(x + 1, y + 1, 11, 11);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 3, y + 3, x + 10, y + 10);
            g2.drawLine(x + 10, y + 3, x + 3, y + 10);
            g2.dispose();
        }
    }
}
