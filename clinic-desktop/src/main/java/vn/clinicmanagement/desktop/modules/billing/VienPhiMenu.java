package vn.clinicmanagement.desktop.modules.billing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.function.Consumer;

/** Menu thả xuống của nút Menu trên màn hình Viện Phí (khớp giao diện EHC HIS). */
public final class VienPhiMenu extends JPopupMenu {

    public enum MucMenu {
        QUAN_LY_VIEN_PHI("Quản Lý Viện Phí"),
        CHOT_SO_LIEU_PHIEU_THU("Chốt Số Liệu Phiếu Thu"),
        TUY_CHON("Tùy Chọn"),
        DANH_SACH_PHIEU_THU("Danh Sách Phiếu Thu"),
        DANH_SACH_SO_THU_TIEN("Danh Sách Sổ Thu Tiền"),
        CHON_SO_THU_TIEN("Chọn Sổ Thu Tiền"),
        LOC_KHOA_PHONG("Lọc Khoa Phòng"),
        MAN_HINH_CHO("Màn Hình Chờ"),
        CAU_HINH_PHAT_LOA("Cấu Hình Phát Loa"),
        HOA_DON_DIEN_TU("Hóa Đơn Điện Tử");

        private final String label;

        MucMenu(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final int CHIEU_RONG = 216;
    private static final int CHIEU_CAO_DONG = 24;
    private static final Font CHU_MUC = new Font("Arial", Font.PLAIN, 12);

    public VienPhiMenu(Consumer<MucMenu> onSelect) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(new LineBorder(new Color(163, 163, 155)));
        setFocusable(true);
        getAccessibleContext().setAccessibleName("Menu Viện Phí");

        addItem(MucMenu.QUAN_LY_VIEN_PHI, null, onSelect);
        addItem(MucMenu.CHOT_SO_LIEU_PHIEU_THU, null, onSelect);

        addGroup("Cấu Hình");
        addItem(MucMenu.TUY_CHON, createGearIcon(), onSelect);

        addGroup("Quản Lý");
        addItem(MucMenu.DANH_SACH_PHIEU_THU, null, onSelect);
        addItem(MucMenu.DANH_SACH_SO_THU_TIEN, null, onSelect);
        addItem(MucMenu.CHON_SO_THU_TIEN, null, onSelect);
        addItem(MucMenu.LOC_KHOA_PHONG, null, onSelect);
        addItem(MucMenu.MAN_HINH_CHO, null, onSelect);
        addItem(MucMenu.CAU_HINH_PHAT_LOA, null, onSelect);

        addFooterItem(MucMenu.HOA_DON_DIEN_TU, onSelect);
    }

    private void addGroup(String title) {
        JLabel heading = new JLabel(title) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, new Color(248, 248, 244),
                        0, getHeight(), new Color(230, 230, 224)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(216, 216, 208));
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        heading.setFont(CHU_MUC.deriveFont(Font.BOLD));
        heading.setForeground(Color.BLACK);
        heading.setBorder(new EmptyBorder(0, 10, 0, 6));
        sizeRow(heading);
        add(heading);
    }

    private void addItem(MucMenu itemType, Icon icon, Consumer<MucMenu> callback) {
        JMenuItem item = new JMenuItem(itemType.label(), icon);
        item.setUI(new javax.swing.plaf.basic.BasicMenuItemUI() {
            {
                selectionBackground = new Color(153, 209, 255);
                selectionForeground = Color.BLACK;
            }
        });
        item.setFont(CHU_MUC);
        item.setForeground(Color.BLACK);
        item.setBackground(Color.WHITE);
        item.setOpaque(true);
        item.setBorder(new EmptyBorder(0, icon != null ? 8 : 32, 0, 8));
        item.setIconTextGap(8);
        item.addActionListener(e -> {
            if (callback != null) {
                callback.accept(itemType);
            }
        });
        sizeRow(item);
        add(item);
    }

    private void addFooterItem(MucMenu itemType, Consumer<MucMenu> callback) {
        JMenuItem item = new JMenuItem(itemType.label()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                if (!isArmed()) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setPaint(new GradientPaint(0, 0, new Color(248, 248, 244),
                            0, getHeight(), new Color(230, 230, 224)));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(new Color(216, 216, 208));
                    g.drawLine(0, 0, getWidth(), 0);
                    g.dispose();
                }
                super.paintComponent(graphics);
            }
        };
        item.setUI(new javax.swing.plaf.basic.BasicMenuItemUI() {
            {
                selectionBackground = new Color(153, 209, 255);
                selectionForeground = Color.BLACK;
            }
        });
        item.setFont(CHU_MUC.deriveFont(Font.BOLD));
        item.setForeground(Color.BLACK);
        item.setOpaque(false);
        item.setBorder(new EmptyBorder(0, 10, 0, 8));
        item.setIconTextGap(0);
        item.addActionListener(e -> {
            if (callback != null) {
                callback.accept(itemType);
            }
        });
        sizeRow(item);
        add(item);
    }

    private static void sizeRow(JComponent row) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(CHIEU_RONG - 2, CHIEU_CAO_DONG));
        row.setMinimumSize(new Dimension(CHIEU_RONG - 2, CHIEU_CAO_DONG));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, CHIEU_CAO_DONG));
    }

    private static Icon createGearIcon() {
        return new Icon() {
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = x + 8, cy = y + 8;
                g2.setColor(new Color(38, 128, 198));
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4.0;
                    int x1 = (int) Math.round(cx + Math.cos(a) * 5.0);
                    int y1 = (int) Math.round(cy + Math.sin(a) * 5.0);
                    int x2 = (int) Math.round(cx + Math.cos(a) * 7.0);
                    int y2 = (int) Math.round(cy + Math.sin(a) * 7.0);
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.drawLine(x1, y1, x2, y2);
                }
                g2.fillOval(cx - 5, cy - 5, 10, 10);
                g2.setColor(Color.WHITE);
                g2.fillOval(cx - 2, cy - 2, 4, 4);
                g2.dispose();
            }
        };
    }
}
