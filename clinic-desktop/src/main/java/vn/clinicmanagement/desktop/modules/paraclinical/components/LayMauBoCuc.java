package vn.clinicmanagement.desktop.modules.paraclinical.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Coordinates measured from the 1586 x 846 specimen workspace reference. */
public final class LayMauBoCuc extends JPanel {
    private final int referenceWidth;
    private final int referenceHeight;
    private final Map<Component, Rectangle> positions = new LinkedHashMap<>();

    public LayMauBoCuc(int width, int height) {
        super(null);
        referenceWidth = width;
        referenceHeight = height;
        setOpaque(false);
    }

    public void place(Component component, int x, int y, int width, int height) {
        positions.put(component, new Rectangle(x, y, width, height));
        add(component);
    }

    @Override public void doLayout() {
        double sx = getWidth() / (double) referenceWidth;
        double sy = getHeight() / (double) referenceHeight;
        positions.forEach((component, rect) -> component.setBounds(
                (int) Math.round(rect.x * sx), (int) Math.round(rect.y * sy),
                (int) Math.round((rect.x + rect.width) * sx) - (int) Math.round(rect.x * sx),
                (int) Math.round((rect.y + rect.height) * sy) - (int) Math.round(rect.y * sy)));
    }

    /** Scale typography from a single reference, without accumulating rounding errors. */
    public static void scale(Component component, double factor) {
        if (component instanceof JComponent jc) {
            Font original = (Font) jc.getClientProperty("layMau.referenceFont");
            if (original == null && jc.getFont() != null) {
                original = jc.getFont();
                jc.putClientProperty("layMau.referenceFont", original);
            }
            if (original != null) jc.setFont(original.deriveFont((float) (original.getSize2D() * factor)));
            if (jc.getBorder() instanceof EmptyBorder border) {
                Insets insets = (Insets) jc.getClientProperty("layMau.referenceInsets");
                if (insets == null) {
                    insets = border.getBorderInsets();
                    jc.putClientProperty("layMau.referenceInsets", insets);
                }
                jc.setBorder(new EmptyBorder((int)(insets.top * factor), (int)(insets.left * factor),
                        (int)(insets.bottom * factor), (int)(insets.right * factor)));
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) scale(child, factor);
        }
    }
}
