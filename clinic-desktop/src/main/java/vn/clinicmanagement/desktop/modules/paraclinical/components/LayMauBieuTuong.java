package vn.clinicmanagement.desktop.modules.paraclinical.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;

/** Platform independent toolbar artwork; avoids emoji font substitutions. */
public record LayMauBieuTuong(Kind kind, int size) implements Icon {
    public enum Kind { MENU, PRINT, REFRESH, SEARCH, LIST, COLLECT, SAVE, EXPAND, HISTORY, CHART, CLEAR, FORMULA, CANCEL_EXEC, APPROVE }
    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
    @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.scale(size / 40.0, size / 40.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(20, 151, 202));
        switch (kind) {
            case MENU -> { for (int i = 5; i < 35; i += 12) g.fillRect(2, i, 36, 5); }
            case SEARCH -> {
                g.setColor(new Color(205, 235, 248)); g.fillOval(12, 1, 25, 25);
                g.setColor(new Color(125, 155, 171)); g.drawOval(12, 1, 25, 25);
                g.setStroke(new BasicStroke(5)); g.drawLine(14, 24, 3, 37);
            }
            case REFRESH -> {
                g.setStroke(new BasicStroke(3));
                g.draw(new Arc2D.Double(5, 5, 29, 29, 35, 145, Arc2D.OPEN));
                g.draw(new Arc2D.Double(5, 5, 29, 29, 215, 145, Arc2D.OPEN));
                g.fillPolygon(new int[]{29, 37, 37}, new int[]{12, 12, 3}, 3);
                g.fillPolygon(new int[]{11, 3, 3}, new int[]{28, 28, 37}, 3);
            }
            case LIST -> {
                for (int i = 8; i < 36; i += 12) {
                    g.fillOval(1, i - 3, 5, 5); g.setColor(Color.GRAY);
                    g.fillRect(11, i - 2, 26, 3); g.setColor(new Color(20, 151, 202));
                }
            }
            case PRINT -> {
                g.setColor(new Color(128, 135, 140)); g.fillRoundRect(1, 14, 38, 18, 3, 3);
                g.setColor(Color.WHITE); g.fillRect(10, 1, 21, 17); g.fillRect(10, 26, 21, 13);
                g.setColor(new Color(114, 132, 140)); g.drawRect(10, 1, 21, 17); g.drawRect(10, 26, 21, 13);
                g.setColor(new Color(20, 151, 202)); g.fillRect(13, 29, 15, 2); g.fillRect(13, 33, 11, 2);
            }
            case CLEAR -> {
                g.setColor(new Color(224, 54, 16)); g.setStroke(new BasicStroke(4));
                g.drawLine(8, 8, 32, 32); g.drawLine(8, 32, 32, 8);
            }
            case SAVE -> {
                g.setColor(new Color(155, 155, 155)); g.fillRoundRect(3, 2, 34, 36, 3, 3);
                g.setColor(Color.WHITE); g.fillRect(10, 3, 19, 12); g.fillRect(10, 23, 22, 12);
                g.setColor(new Color(155, 155, 155)); g.fillRect(23, 4, 4, 9);
            }
            case EXPAND -> {
                g.setColor(new Color(22, 134, 201)); g.setStroke(new BasicStroke(3));
                for (int i = 0; i < 4; i++) {
                    g.drawLine(4, 15, 4, 4); g.drawLine(4, 4, 15, 4); g.drawLine(4, 4, 16, 16);
                    g.rotate(Math.PI / 2, 20, 20);
                }
            }
            case HISTORY -> {
                g.setColor(new Color(23, 131, 204)); g.setStroke(new BasicStroke(4));
                g.draw(new Arc2D.Double(5, 5, 30, 30, 155, 305, Arc2D.OPEN));
                g.fillPolygon(new int[]{0, 12, 12}, new int[]{10, 4, 18}, 3);
                g.drawLine(20, 11, 20, 22); g.drawLine(20, 22, 28, 22);
            }
            case CHART -> {
                g.setColor(new Color(38, 142, 198)); g.fillRect(0, 24, 8, 15);
                g.setColor(new Color(222, 166, 46)); g.fillRect(10, 16, 8, 23);
                g.setColor(new Color(69, 162, 116)); g.fillRect(20, 3, 8, 36);
                g.setColor(new Color(216, 85, 28)); g.fillRect(30, 11, 8, 28);
            }
            case COLLECT -> {
                g.setPaint(new GradientPaint(6, 3, new Color(97, 181, 137), 32, 36, new Color(36, 145, 109)));
                g.fillPolygon(new int[]{7, 7, 34}, new int[]{3, 37, 20}, 3);
            }
            case FORMULA -> {
                g.setColor(new Color(145, 148, 152)); g.setStroke(new BasicStroke(2.2f));
                g.drawRoundRect(5, 3, 30, 30, 5, 5);
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
                g.drawString("{}", 11, 23);
                g.setColor(new Color(24, 126, 202)); g.setStroke(new BasicStroke(4));
                g.drawLine(4, 36, 17, 23);
            }
            case CANCEL_EXEC -> {
                g.setColor(new Color(115, 118, 122)); g.setStroke(new BasicStroke(2.6f));
                g.drawLine(8, 3, 8, 36);
                g.setColor(new Color(74, 168, 108));
                g.fillPolygon(new int[]{9, 31, 9}, new int[]{4, 13, 22}, 3);
                g.setColor(new Color(218, 83, 34)); g.setStroke(new BasicStroke(3.6f));
                g.drawLine(18, 23, 34, 37); g.drawLine(18, 37, 34, 23);
            }
            case APPROVE -> {
                g.setColor(new Color(45, 24, 168)); g.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(5, 21, 15, 32);
                g.drawLine(15, 32, 35, 8);
            }
        }
        g.dispose();
    }
}
