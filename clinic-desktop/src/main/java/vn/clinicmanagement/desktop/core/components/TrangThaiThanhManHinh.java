package vn.clinicmanagement.desktop.core.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thanh trạng thái chân màn hình chuẩn EHC (khớp 100% ảnh mẫu):
 * [Đã đăng ký] | [Icon EHC] [EHC] - Phần mềm... | PHÒNG KHÁM TAI MŨI HỌNG SHIHAN >>Ho... |
 * [Số thu: duongng test] | [DB: SHIHh VN] | [VN] | [Thứ ..., dd/MM/yyyy HH:mm:ss] |
 * [Nền vàng: Icon + Thông báo (0)] | [Icon mic + Trợ giúp]
 */
public class TrangThaiThanhManHinh extends JPanel {

    private static final Color BG_BAR = new Color(240, 242, 246);
    private static final Color BG_NOTIFY = new Color(255, 251, 214); // Nền vàng nhạt của ô Thông báo (0)
    private static final Color BORDER_TOP = new Color(185, 195, 210);
    private static final Color SEPARATOR = new Color(188, 196, 208);

    private static final Color COLOR_BLUE_TEXT = new Color(22, 55, 145);
    private static final Color COLOR_DARK_TEXT = new Color(25, 25, 25);

    private static final int BAR_HEIGHT = 25;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final JLabel lblClock = new JLabel("", SwingConstants.CENTER);

    public TrangThaiThanhManHinh() {
        setLayout(new GridBagLayout());
        setBackground(BG_BAR);
        setPreferredSize(new Dimension(100, BAR_HEIGHT));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_TOP));

        NguoiDungPhien s = NguoiDungPhien.getInstance();
        String facilityText = s.getFacilityName() != null && !s.getFacilityName().isEmpty()
                ? s.getFacilityName() + " >>Ho..."
                : "PHÒNG KHÁM TAI MŨI HỌNG SHIHAN >>Ho...";
        String stationText = s.getStationId() != null && !s.getStationId().isEmpty()
                ? s.getStationId()
                : "duongng test";

        int x = 0;

        // 1. Đã đăng ký (Chữ xanh đậm căn giữa)
        addCell(x++, createFixedTextCell("Đã đăng ký", COLOR_BLUE_TEXT, 96, null), 0);

        // 2. [EHC] - Phần mềm... (Có icon EHC vuông xanh-cam bên trái)
        addCell(x++, createFixedIconCell(new EhcMiniIcon(), "[EHC] - Phần mềm...", COLOR_DARK_TEXT, 148, null, true), 0);

        // 3. Tên phòng khám / cơ sở (Ô duy nhất co giãn linh hoạt theo chiều ngang màn hình)
        JLabel lblFacility = new JLabel(facilityText, SwingConstants.CENTER);
        lblFacility.setFont(UngDungPhongChu.SMALL);
        lblFacility.setForeground(COLOR_DARK_TEXT);
        lblFacility.setToolTipText(facilityText);
        JComponent facilityCell = wrapCell(lblFacility, 220, 80, null, true);
        addCell(x++, facilityCell, 1.0);

        // 4. Số thu: duongng test (Chữ xanh đậm)
        addCell(x++, createFixedTextCell("Số thu: " + stationText, COLOR_BLUE_TEXT, 142, null), 0);

        // 5. DB: SHIHh VN (Chữ xanh đậm)
        addCell(x++, createFixedTextCell("DB: SHIHh VN", COLOR_BLUE_TEXT, 108, null), 0);

        // 6. VN (Chữ xanh đậm)
        addCell(x++, createFixedTextCell("VN", COLOR_BLUE_TEXT, 32, null), 0);

        // 7. Đồng hồ thời gian thực (Chữ đen)
        lblClock.setFont(UngDungPhongChu.SMALL);
        lblClock.setForeground(COLOR_DARK_TEXT);
        updateClock();
        addCell(x++, wrapCell(lblClock, 162, 162, null, true), 0);

        // 8. Thông báo (0) (Nền vàng nhạt, icon xanh, chữ xanh)
        addCell(x++, createFixedIconCell(new NotifyMiniIcon(), "Thông báo (0)", COLOR_BLUE_TEXT, 118, BG_NOTIFY, true), 0);

        // 9. Trợ giúp (Icon mic xám + chữ đen, không có vạch ngăn phải cùng)
        addCell(x, createFixedIconCell(new MicMiniIcon(), "Trợ giúp", COLOR_DARK_TEXT, 80, null, false), 0);

        new Timer(1000, e -> updateClock()).start();
    }

    /** Hàm `addCell`: thêm `Cell`. */
    private void addCell(int x, JComponent cell, double weightX) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = 0;
        c.weightx = weightX;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        add(cell, c);
    }

    /** Hàm `createFixedTextCell`: tạo `Fixed` văn bản `Cell`. */
    private JComponent createFixedTextCell(String text, Color textColor, int width, Color bgColor) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(UngDungPhongChu.SMALL);
        lbl.setForeground(textColor);
        lbl.setToolTipText(text);
        return wrapCell(lbl, width, width, bgColor, true);
    }

    /** Hàm `createFixedIconCell`: tạo `Fixed` `Icon` `Cell`. */
    private JComponent createFixedIconCell(Icon icon, String text, Color textColor, int width, Color bgColor, boolean rightBorder) {
        JLabel lbl = new JLabel(text, icon, SwingConstants.CENTER);
        lbl.setIconTextGap(5);
        lbl.setFont(UngDungPhongChu.SMALL);
        lbl.setForeground(textColor);
        lbl.setToolTipText(text);
        return wrapCell(lbl, width, width, bgColor, rightBorder);
    }

    private JComponent wrapCell(JComponent content, int prefWidth, int minWidth, Color bgColor, boolean rightBorder) {
        JPanel p = new JPanel(new BorderLayout());
        if (bgColor != null) {
            p.setOpaque(true);
            p.setBackground(bgColor);
        } else {
            p.setOpaque(false);
        }

        if (rightBorder) {
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, SEPARATOR),
                    BorderFactory.createEmptyBorder(0, 5, 0, 5)
            ));
        } else {
            p.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        }

        p.add(content, BorderLayout.CENTER);
        int naturalW = content.getPreferredSize().width + 16;
        int w = prefWidth == minWidth ? Math.max(prefWidth, naturalW) : prefWidth;
        int mw = prefWidth == minWidth ? w : minWidth;
        p.setPreferredSize(new Dimension(w, BAR_HEIGHT));
        p.setMinimumSize(new Dimension(mw, BAR_HEIGHT));
        return p;
    }

    /** Hàm `updateClock`: cập nhật `Clock`. */
    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        String thu = now.getDayOfWeek() == DayOfWeek.SUNDAY
                ? "CN"
                : "Thứ " + (now.getDayOfWeek().getValue() + 1);
        lblClock.setText(thu + ", " + now.format(DATE_TIME));
    }

    /** Icon vuông xanh-cam của EHC ở ô thứ 2 */
    private static class EhcMiniIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(45, 135, 210));
            g2.fillRoundRect(x, y, 13, 13, 3, 3);
            g2.setColor(new Color(245, 145, 30));
            g2.fillOval(x + 2, y + 5, 7, 7);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(x, y, 12, 12, 3, 3);
            g2.dispose();
        }
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
    }

    /** Icon hộp thư/tin nhắn màu xanh ở ô Thông báo (0) */
    private static class NotifyMiniIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(55, 125, 200));
            g2.fillRoundRect(x, y + 1, 14, 10, 2, 2);
            g2.setColor(Color.WHITE);
            g2.fillOval(x + 2, y + 5, 2, 2);
            g2.fillOval(x + 6, y + 5, 2, 2);
            g2.fillOval(x + 10, y + 5, 2, 2);
            g2.dispose();
        }
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 15; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 13; }
    }

    /** Icon micro xám ở ô Trợ giúp */
    private static class MicMiniIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(120, 125, 135));
            g2.fillRoundRect(x + 4, y + 1, 5, 8, 4, 4);
            g2.drawArc(x + 2, y + 3, 9, 8, 180, 180);
            g2.drawLine(x + 6, y + 11, x + 6, y + 13);
            g2.drawLine(x + 4, y + 13, x + 8, y + 13);
            g2.dispose();
        }
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 13; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
    }
}
