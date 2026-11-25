package vn.clinicmanagement.desktop.modules.inventory;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicMenuItemUI;
import java.awt.*;
import java.util.function.Consumer;

/** Chỉ hiển thị loại chứng từ đang có luồng lưu và đọc lại hoàn chỉnh. */
public final class TuTrucTaoPhieuMenu extends JPopupMenu {
    private static final int WIDTH = 246;
    private static final Color BORDER = new Color(160, 160, 155);
    private static final Color GUTTER = new Color(226, 226, 226);

    public TuTrucTaoPhieuMenu(Consumer<String> selected) {
        setName("tuTrucTaoPhieuMenu");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(new LineBorder(BORDER));
        getAccessibleContext().setAccessibleName("Tạo phiếu tủ trực vật tư");

        group("Cấp cơ số tủ trực");
        item("Nhập Bù Cơ Số Tủ Trực", false, selected);
    }

    public void setActionsEnabled(boolean enabled) {
        for (Component child : getComponents()) {
            if (child instanceof JMenuItem item) item.setEnabled(enabled);
        }
    }

    private void group(String text) {
        JLabel heading = new JLabel(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, new Color(244, 244, 241),
                        0, getHeight(), new Color(218, 218, 216)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        heading.setFont(UngDungPhongChu.TU_TRUC_MENU_NHOM);
        heading.setForeground(Color.BLACK);
        heading.setBorder(new EmptyBorder(0, 10, 0, 4));
        size(heading, 21);
        add(heading);
    }

    private void item(String text, boolean separatorAbove, Consumer<String> selected) {
        JMenuItem item = new JMenuItem(text) {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                graphics.setColor(GUTTER);
                graphics.drawLine(24, 0, 24, getHeight() - 1);
                if (separatorAbove) graphics.drawLine(32, 0, getWidth() - 3, 0);
            }
        };
        item.setUI(new BasicMenuItemUI() {
            @Override protected void installDefaults() {
                super.installDefaults();
                selectionBackground = new Color(216, 234, 251);
                selectionForeground = Color.BLACK;
            }
        });
        item.setFont(UngDungPhongChu.TU_TRUC_MENU);
        item.setForeground(Color.BLACK);
        item.setBackground(Color.WHITE);
        item.setOpaque(true);
        item.setBorder(new EmptyBorder(0, 32, 0, 3));
        item.setIconTextGap(0);
        item.setActionCommand(text);
        item.addActionListener(e -> { if (selected != null) selected.accept(e.getActionCommand()); });
        size(item, 22);
        add(item);
    }

    private static void size(JComponent row, int height) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(WIDTH - 2, height));
        row.setMinimumSize(new Dimension(WIDTH - 2, height));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }
}
