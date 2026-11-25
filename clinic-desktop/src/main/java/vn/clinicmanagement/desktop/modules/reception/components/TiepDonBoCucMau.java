package vn.clinicmanagement.desktop.modules.reception.components;

import java.awt.*;

/** Căn các trường theo tọa độ ngang của tk.png, co giãn theo chiều rộng thực tế. */
final class TiepDonBoCucMau implements LayoutManager {
    private final int referenceWidth;
    private final int height;
    private final int[][] columns;

    TiepDonBoCucMau(int referenceWidth, int height, int[]... columns) {
        this.referenceWidth = referenceWidth;
        this.height = height;
        this.columns = columns;
    }

    @Override public void addLayoutComponent(String name, Component component) { }
    @Override public void removeLayoutComponent(Component component) { }
    @Override public Dimension preferredLayoutSize(Container parent) { return new Dimension(referenceWidth, height); }
    @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(0, height); }
    @Override public void layoutContainer(Container parent) {
        double scale = parent.getWidth() / (double) referenceWidth;
        Component[] children = parent.getComponents();
        for (int i = 0; i < children.length && i < columns.length; i++) {
            int[] column = columns[i];
            children[i].setBounds((int) Math.round(column[0] * scale), (height - 20) / 2,
                    Math.max(0, (int) Math.round(column[1] * scale)), 20);
            if (children[i] instanceof javax.swing.JLabel label) TiepDonKieuMau.fitLabel(label);
        }
    }
}
