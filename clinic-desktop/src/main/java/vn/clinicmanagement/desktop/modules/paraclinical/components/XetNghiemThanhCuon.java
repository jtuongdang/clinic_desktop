package vn.clinicmanagement.desktop.modules.paraclinical.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/** Square scrollbar artwork matching the laboratory reference on every OS. */
final class XetNghiemThanhCuon extends BasicScrollBarUI {
    static void install(JScrollPane pane) {
        pane.getHorizontalScrollBar().setUI(new XetNghiemThanhCuon());
        pane.getVerticalScrollBar().setUI(new XetNghiemThanhCuon());
    }
    @Override protected void configureScrollBarColors() {
        trackColor = new Color(239, 239, 239); thumbColor = new Color(190, 190, 190);
    }
    @Override protected JButton createDecreaseButton(int orientation) { return arrow(orientation); }
    @Override protected JButton createIncreaseButton(int orientation) { return arrow(orientation); }
    private JButton arrow(int direction) {
        BasicArrowButton button = new BasicArrowButton(direction, new Color(244,244,244), Color.LIGHT_GRAY, Color.DARK_GRAY, Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder()); return button;
    }
    @Override protected void paintThumb(Graphics graphics, JComponent c, Rectangle r) {
        if (r.isEmpty()) return;
        Graphics2D g = (Graphics2D)graphics.create();
        g.setPaint(new GradientPaint(r.x,r.y,new Color(198,198,198),r.x,r.y+r.height,new Color(178,178,178)));
        g.fillRect(r.x,r.y,r.width,r.height); g.dispose();
    }
}
