package vn.clinicmanagement.desktop.modules.examination.components.common;

import javax.swing.*;
import java.awt.*;

/**
 * Tập hợp Vector Icons dùng chung cho các màn hình chỉ định (Thuốc, Vật Tư).
 */
public final class ExaminationOrderIcons {

    private ExaminationOrderIcons() {}

    public static final class SuppliesToolbarIcon implements Icon {
        private final String type;

        public SuppliesToolbarIcon(String type) {
            this.type = type;
        }

        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if ("menu".equalsIgnoreCase(type)) {
                g2.setColor(new Color(0, 95, 185));
                g2.fillRect(x + 2, y + 4, 14, 2);
                g2.fillRect(x + 2, y + 8, 14, 2);
                g2.fillRect(x + 2, y + 12, 14, 2);
            } else if ("template".equalsIgnoreCase(type)) {
                g2.setColor(new Color(45, 125, 210));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x + 3, y + 4, 12, 10, 3, 3);
                g2.fillOval(x + 5, y + 7, 3, 3);
            } else if ("sign".equalsIgnoreCase(type)) {
                g2.setColor(new Color(110, 125, 145));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawLine(x + 4, y + 14, x + 12, y + 6);
                g2.drawLine(x + 10, y + 4, x + 14, y + 8);
                g2.drawLine(x + 2, y + 16, x + 5, y + 13);
            } else if ("history".equalsIgnoreCase(type)) {
                g2.setColor(new Color(0, 115, 205));
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawOval(x + 3, y + 3, 12, 12);
                g2.drawLine(x + 9, y + 5, x + 9, y + 9);
                g2.drawLine(x + 9, y + 9, x + 12, y + 9);
            } else if ("print".equalsIgnoreCase(type)) {
                g2.setColor(new Color(70, 80, 95));
                g2.fillRect(x + 4, y + 2, 10, 4);
                g2.fillRoundRect(x + 2, y + 5, 14, 7, 2, 2);
                g2.setColor(Color.WHITE);
                g2.fillRect(x + 5, y + 9, 8, 5);
                g2.setColor(new Color(70, 80, 95));
                g2.drawRect(x + 5, y + 9, 8, 5);
            }
            g2.dispose();
        }
    }

    public static final class CheckmarkBlueIcon implements Icon {
        private final Color color;

        public CheckmarkBlueIcon() {
            this(new Color(0, 115, 220));
        }

        public CheckmarkBlueIcon(Color color) {
            this.color = color;
        }

        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 7, x + 5, y + 11);
            g2.drawLine(x + 5, y + 11, x + 12, y + 2);
            g2.dispose();
        }
    }

    public static final class FloppyDiskIcon implements Icon {
        private final Color color;

        public FloppyDiskIcon(Color color) {
            this.color = color;
        }

        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(x + 1, y + 1, 14, 14, 2, 2);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 4, y + 2, 8, 5);
            g2.fillRect(x + 3, y + 9, 10, 5);
            g2.setColor(color);
            g2.fillRect(x + 9, y + 3, 2, 3);
            g2.dispose();
        }
    }

    public static final class CloseCrossIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(185, 30, 30));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 3, y + 3, x + 11, y + 11);
            g2.drawLine(x + 11, y + 3, x + 3, y + 11);
            g2.dispose();
        }
    }

    public static final class EnvelopeIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 11; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(0, 95, 195));
            g2.drawRect(x + 1, y + 1, 12, 9);
            g2.drawLine(x + 1, y + 2, x + 7, y + 6);
            g2.drawLine(x + 7, y + 6, x + 13, y + 2);
            g2.dispose();
        }
    }

    public static final class StarVectorIcon implements Icon {
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 12; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            int midX = x + 6;
            int midY = y + 6;
            int radius = 5;
            int innerRadius = 2;
            int[] xPoints = new int[10];
            int[] yPoints = new int[10];
            for (int i = 0; i < 10; i++) {
                double angle = i * Math.PI / 5 - Math.PI / 2;
                int r = (i % 2 == 0) ? radius : innerRadius;
                xPoints[i] = (int) (midX + r * Math.cos(angle));
                yPoints[i] = (int) (midY + r * Math.sin(angle));
            }
            g2.fillPolygon(xPoints, yPoints, 10);
            g2.dispose();
        }
    }

    public static final class PauseBarsIcon implements Icon {
        @Override public int getIconWidth() { return 10; }
        @Override public int getIconHeight() { return 12; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(30, 30, 30));
            g2.fillRect(x + 1, y + 2, 2, 8);
            g2.fillRect(x + 5, y + 2, 2, 8);
            g2.dispose();
        }
    }
}
