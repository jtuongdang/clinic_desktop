package vn.clinicmanagement.desktop.modules.examination.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Locale;

/**
 * Các icon vector và cell renderer tùy chỉnh dùng cho màn hình Khám Bệnh và các bảng con.
 */
public final class KhamBenhManHinhIconVaRenderer {

    private KhamBenhManHinhIconVaRenderer() {}

    /** Tạo JTabbedPane theo chuẩn giao diện Khám Bệnh EHC. */
    public static JTabbedPane createStyledTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UngDungPhongChu.SMALL);
        tabs.setBackground(new Color(230, 238, 248));
        tabs.setBorder(new EmptyBorder(0, 0, 0, 0));
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabInsets = new Insets(4, 9, 4, 9);
                selectedTabPadInsets = new Insets(2, 2, 2, 2);
                tabAreaInsets = new Insets(3, 4, 0, 4);
                contentBorderInsets = new Insets(1, 0, 0, 0);
            }

            @Override
            protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
                g.setColor(new Color(224, 235, 248));
                g.fillRect(0, 0, tabPane.getWidth(), calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight));
                super.paintTabArea(g, tabPlacement, selectedIndex);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                if (isSelected) {
                    g2.setColor(new Color(254, 240, 200));
                    g2.fillRect(x, y, w, h);
                } else {
                    g2.setColor(new Color(236, 242, 250));
                    g2.fillRect(x, y + 2, w, h - 2);
                }
                g2.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                g.setColor(isSelected ? new Color(210, 175, 110) : new Color(195, 206, 222));
                g.drawRect(x, isSelected ? y : y + 2, w - 1, isSelected ? h - 1 : h - 3);
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                g.setColor(new Color(185, 198, 218));
                int topY = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                g.drawLine(0, topY, tabPane.getWidth(), topY);
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
            }
        });
        return tabs;
    }

    /** Cột 0 của bảng hàng đợi: vẽ chấm cam / tick xanh + số thứ tự căn giữa. */
    public static final class SttCellRenderer extends JComponent implements TableCellRenderer {
        private AdminPatientItem item;
        private boolean selected;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.item = value instanceof AdminPatientItem ? (AdminPatientItem) value : null;
            this.selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(selected ? new Color(0, 114, 198) : Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (item != null) {
                g2.setColor(selected ? new Color(0, 114, 198) : Color.WHITE);
                g2.fillRect(0, 0, 18, getHeight());

                int dy = (getHeight() - 13) / 2;
                boolean isWaitingCls = "CHO_CLS".equalsIgnoreCase(item.visitStatus);
                if (isWaitingCls) {
                    g2.setColor(new Color(111, 66, 193));
                    g2.fillOval(2, dy, 13, 13);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(4, dy + 5, 2, 2);
                    g2.fillOval(7, dy + 5, 2, 2);
                    g2.fillOval(10, dy + 5, 2, 2);
                } else if (item.hasOrangeDot) {
                    g2.setColor(new Color(245, 130, 32));
                    g2.fillOval(2, dy, 13, 13);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawLine(5, dy + 6, 11, dy + 6);
                    g2.drawLine(8, dy + 3, 8, dy + 9);
                } else {
                    g2.setColor(new Color(36, 160, 175));
                    g2.fillOval(2, dy, 13, 13);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawLine(4, dy + 6, 7, dy + 9);
                    g2.drawLine(7, dy + 9, 11, dy + 4);
                }

                g2.setFont(UngDungPhongChu.SMALL);
                g2.setColor(selected ? Color.WHITE : new Color(25, 25, 25));
                FontMetrics fm = g2.getFontMetrics();
                int tx = 18 + (getWidth() - 18 - fm.stringWidth(item.stt)) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(item.stt, tx, ty);
            }
            g2.dispose();
        }
    }

    /** Cột 1 của bảng hàng đợi: vẽ 2 dòng (Tên in đậm + Mã BN - Đối tượng). */
    public static final class PatientNameCellRenderer extends JComponent implements TableCellRenderer {
        private AdminPatientItem item;
        private boolean selected;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.item = value instanceof AdminPatientItem ? (AdminPatientItem) value : null;
            this.selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(selected ? new Color(0, 114, 198) : Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (item != null) {
                g2.setFont(UngDungPhongChu.SMALL_BOLD);
                g2.setColor(selected ? Color.WHITE : new Color(15, 15, 15));
                g2.drawString(item.name, 4, 18);

                g2.setFont(UngDungPhongChu.SMALL);
                String line2Base = item.code + " - " + item.requestType;
                g2.setColor(selected ? new Color(235, 245, 255) : new Color(45, 45, 45));
                g2.drawString(line2Base, 4, 34);

                if ("CHO_CLS".equalsIgnoreCase(item.visitStatus)) {
                    FontMetrics fm = g2.getFontMetrics();
                    int badgeX = 4 + fm.stringWidth(line2Base);
                    g2.setFont(UngDungPhongChu.SMALL_BOLD);
                    g2.setColor(selected ? new Color(255, 235, 150) : new Color(111, 66, 193));
                    g2.drawString(" • Chờ CLS", badgeX, 34);
                }
            }
            g2.dispose();
        }
    }

    /** Chấm trạng thái ở cột 0 của bảng Lịch sử khám. */
    public static final class HistoryStatusDotComponent extends JComponent {
        private final boolean selected;
        private final String status;

        public HistoryStatusDotComponent(boolean selected, String status) {
            this.selected = selected;
            this.status = status != null ? status.trim().toUpperCase(Locale.ROOT) : "";
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(selected ? new Color(215, 232, 250) : Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            int cx = (getWidth() - 12) / 2;
            int cy = (getHeight() - 12) / 2;
            if ("DA_HUY".equals(status)) {
                g2.setColor(new Color(150, 156, 166));
                g2.fillOval(cx, cy, 12, 12);
            } else if ("CHO_CLS".equals(status)) {
                g2.setColor(new Color(111, 66, 193));
                g2.fillOval(cx, cy, 12, 12);
            } else if (KhamBenhManHinh.isCompletedOrCancelledVisitStatus(status)) {
                g2.setColor(new Color(36, 160, 175));
                g2.fillOval(cx, cy, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.3f));
                g2.drawLine(cx + 3, cy + 6, cx + 5, cy + 9);
                g2.drawLine(cx + 5, cy + 9, cx + 9, cy + 4);
            } else {
                g2.setColor(new Color(235, 135, 20));
                g2.fillOval(cx, cy, 12, 12);
            }
            g2.dispose();
        }
    }

    /** Hộp tích vuông ở cột Khoa của bảng Lịch sử khám. */
    public static final class CheckBoxMiniIcon implements Icon {
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 12; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(140, 145, 155));
            g2.drawRect(x, y + 1, 10, 10);
            g2.setColor(new Color(70, 80, 90));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawLine(x + 2, y + 6, x + 4, y + 9);
            g2.drawLine(x + 4, y + 9, x + 9, y + 3);
            g2.dispose();
        }
    }

    public static final class LeftTriangleIcon implements Icon {
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 14; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(55, 110, 170));
            g2.fillPolygon(new int[]{x + 10, x + 2, x + 10}, new int[]{y + 1, y + 7, y + 13}, 3);
            g2.dispose();
        }
    }

    public static final class MiniMagnifierIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(55, 115, 175));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x + 1, y + 1, 8, 8);
            g2.drawLine(x + 8, y + 8, x + 12, y + 12);
            g2.dispose();
        }
    }

    public static final class SmallFormIcon implements Icon {
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 12; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(new Color(80, 120, 170));
            g.drawRect(x + 1, y + 1, 9, 9);
            g.drawLine(x + 3, y + 4, x + 8, y + 4);
            g.drawLine(x + 3, y + 7, x + 8, y + 7);
        }
    }

    /** Bộ icon vector 24x24 cho Sub-Toolbar. */
    public static final class SubToolbarVectorIcon implements Icon {
        private final String type;

        public SubToolbarVectorIcon(String type) {
            this.type = type;
        }

        @Override public int getIconWidth() { return 26; }
        @Override public int getIconHeight() { return 24; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);

            switch (type) {
                case "menu":
                    g2.setColor(new Color(55, 110, 165));
                    g2.fillRect(3, 4, 20, 3);
                    g2.fillRect(3, 10, 20, 3);
                    g2.fillRect(3, 16, 20, 3);
                    break;
                case "print":
                    g2.setColor(new Color(120, 135, 155));
                    g2.fillRect(5, 9, 16, 9);
                    g2.setColor(Color.WHITE);
                    g2.fillRect(7, 3, 12, 6);
                    g2.fillRect(7, 14, 12, 7);
                    g2.setColor(new Color(75, 115, 165));
                    g2.drawRect(7, 3, 12, 6);
                    g2.drawRect(7, 14, 12, 7);
                    break;
                case "refresh":
                    g2.setColor(new Color(75, 130, 185));
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.drawArc(4, 3, 17, 17, 35, 280);
                    g2.fillPolygon(new int[]{18, 23, 17}, new int[]{3, 8, 9}, 3);
                    break;
                case "search":
                    g2.setColor(new Color(180, 215, 245));
                    g2.fillOval(4, 2, 13, 13);
                    g2.setColor(new Color(70, 120, 175));
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawOval(4, 2, 13, 13);
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawLine(15, 13, 21, 20);
                    break;
                case "service":
                    g2.setColor(new Color(140, 150, 165));
                    g2.fillRect(4, 18, 18, 3);
                    g2.drawLine(6, 10, 20, 10);
                    g2.setColor(new Color(220, 80, 70));
                    g2.fillRect(8, 5, 3, 12);
                    g2.setColor(new Color(65, 145, 215));
                    g2.fillRect(15, 5, 3, 12);
                    break;
                case "medicine":
                    g2.setColor(new Color(230, 165, 95));
                    g2.fillRoundRect(4, 7, 8, 13, 2, 2);
                    g2.fillRoundRect(14, 9, 8, 11, 2, 2);
                    g2.setColor(Color.WHITE);
                    g2.fillRect(5, 11, 6, 5);
                    g2.fillRect(15, 13, 6, 4);
                    g2.setColor(new Color(165, 105, 45));
                    g2.drawRoundRect(4, 7, 8, 13, 2, 2);
                    g2.drawRoundRect(14, 9, 8, 11, 2, 2);
                    break;
                case "payment":
                    g2.setColor(Color.WHITE);
                    g2.fillRect(5, 3, 15, 18);
                    g2.setColor(new Color(110, 115, 125));
                    g2.drawRect(5, 3, 15, 18);
                    g2.fillRect(9, 1, 7, 3);
                    g2.setColor(new Color(210, 55, 55));
                    g2.fillRect(11, 7, 3, 7);
                    g2.fillRect(9, 9, 7, 3);
                    break;
                default:
                    g2.setColor(new Color(75, 115, 160));
                    for (int i = 0; i < 3; i++) {
                        int ry = 5 + i * 5;
                        g2.fillOval(5, ry, 3, 3);
                        g2.fillRect(10, ry, 11, 2);
                    }
                    break;
            }
            g2.dispose();
        }
    }
}
