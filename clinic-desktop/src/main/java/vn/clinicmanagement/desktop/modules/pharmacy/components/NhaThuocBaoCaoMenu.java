package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.LoaiBaoCao;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.function.Consumer;

/** Menu Báo Cáo của phân hệ Nhà Thuốc theo bố cục tham chiếu. */
public final class NhaThuocBaoCaoMenu extends JPopupMenu {
    private static final int CHIEU_RONG = 278;
    private static final int CHIEU_CAO_DONG = 22;
    private static final Font CHU_MUC = new Font("Arial", Font.PLAIN, 12);

    public NhaThuocBaoCaoMenu(Consumer<LoaiBaoCao> onSelect) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(new LineBorder(new Color(163, 163, 155)));
        setFocusable(true);
        getAccessibleContext().setAccessibleName("Báo cáo nhà thuốc");

        addGroup("Phiếu");
        addGroup("Báo Cáo");
        for (LoaiBaoCao type : LoaiBaoCao.values()) {
            addItem(type, onSelect);
        }
    }

    private void addGroup(String title) {
        JLabel heading = new JLabel(title) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, new Color(250, 250, 246),
                        0, getHeight(), new Color(234, 234, 228)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(224, 224, 216));
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        heading.setFont(CHU_MUC.deriveFont(Font.BOLD));
        heading.setForeground(Color.BLACK);
        heading.setBorder(new EmptyBorder(0, 8, 0, 4));
        sizeRow(heading);
        add(heading);
    }

    private void addItem(LoaiBaoCao type, Consumer<LoaiBaoCao> callback) {
        JMenuItem item = new JMenuItem(type.title());
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
        item.setBorder(new EmptyBorder(0, 20, 0, 8));
        item.setIconTextGap(0);
        item.addActionListener(e -> {
            if (callback != null) {
                callback.accept(type);
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
}
