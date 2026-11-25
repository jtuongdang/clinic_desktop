package vn.clinicmanagement.desktop.modules.reception.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import javax.swing.*;
import java.awt.*;

/** Kiểu chữ, viền vuông và biểu tượng dùng riêng cho màn hình theo tk.png. */
final class TiepDonKieuMau {
    private TiepDonKieuMau() { }
    static void apply(Component component) {
        if (component.getFont() != null) component.setFont(component.getFont().isBold()
                ? UngDungPhongChu.TIEP_DON_BOLD : UngDungPhongChu.TIEP_DON);
        if (component instanceof JComponent control) {
            control.putClientProperty("JComponent.roundRect", false);
            control.putClientProperty("JComponent.minimumWidth", 0);
            if (control instanceof JComboBox<?>) control.putClientProperty("FlatLaf.style", "arc: 0; minimumWidth: 0");
            if (control instanceof JButton) control.putClientProperty("FlatLaf.style", "arc: 0; minimumWidth: 0");
        }
        if (component instanceof JCheckBox checkBox) {
            checkBox.setIcon(new Icon() {
                @Override public int getIconWidth() { return 11; }
                @Override public int getIconHeight() { return 11; }
                @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                    g.setColor(Color.WHITE); g.fillRect(x, y, 10, 10);
                    g.setColor(c.isEnabled() ? Color.GRAY : Color.LIGHT_GRAY); g.drawRect(x, y, 10, 10);
                    if (((AbstractButton)c).isSelected()) {
                        g.drawLine(x + 2, y + 5, x + 4, y + 8);
                        g.drawLine(x + 4, y + 8, x + 9, y + 2);
                    }
                }
            });
            checkBox.setIconTextGap(3);
            checkBox.setMargin(new Insets(0, 0, 0, 0));
        }
        if (component instanceof Container container)
            for (Component child : container.getComponents()) apply(child);
    }
    /** Giữ nguyên cỡ chữ dọc, thu nhẹ chiều ngang khi nhãn dài vượt ô ở màn hình nhỏ. */
    static void fitLabel(JLabel label) {
        Font original = (Font) label.getClientProperty("reception.originalFont");
        if (original == null) { original = label.getFont(); label.putClientProperty("reception.originalFont", original); }
        int available = label.getWidth() - label.getInsets().left - label.getInsets().right;
        int needed = label.getFontMetrics(original).stringWidth(label.getText() == null ? "" : label.getText());
        if (available > 6 && needed > available - 4) {
            java.awt.geom.AffineTransform transform = original.getTransform();
            transform.scale((available - 6.0) / needed, 1);
            label.setFont(original.deriveFont(transform));
        } else label.setFont(original);
    }
    static Icon icon(Icon source, int width, int height) {
        return new Icon() {
            @Override public int getIconWidth() { return width; }
            @Override public int getIconHeight() { return height; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D copy = (Graphics2D) g.create();
                copy.translate(x, y);
                copy.scale(width / (double) source.getIconWidth(), height / (double) source.getIconHeight());
                source.paintIcon(c, copy, 0, 0);
                copy.dispose();
            }
        };
    }
}
