package vn.clinicmanagement.desktop.modules.pharmacy.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Bộ Icon Vector Graphics2D chuẩn y tế cho phân hệ Nhà Thuốc / Tủ Trực Vật Tư.
 * Không dùng emoji hệ thống, sắc nét trên mọi độ phân giải màn hình.
 */
public final class PharmacyIcons {
    private PharmacyIcons() {}

    /** Icon Hamburger Menu (3 gạch ngang xanh) */
    public static Icon createMenuIcon() {
        return new VectorIcon(18, 16, (g2, w, h) -> {
            g2.setColor(new Color(40, 95, 155));
            g2.fillRect(1, 2, 16, 2);
            g2.fillRect(1, 7, 16, 2);
            g2.fillRect(1, 12, 16, 2);
        });
    }

    /** Icon Báo Cáo (Tờ giấy gập góc có dòng kẻ) */
    public static Icon createReportIcon() {
        return new VectorIcon(16, 16, (g2, w, h) -> {
            g2.setColor(new Color(110, 125, 145));
            g2.fillRect(2, 1, 12, 14);
            g2.setColor(Color.WHITE);
            g2.fillRect(3, 2, 10, 12);
            g2.setColor(new Color(85, 115, 155));
            g2.drawLine(5, 5, 11, 5);
            g2.drawLine(5, 8, 11, 8);
            g2.drawLine(5, 11, 9, 11);
        });
    }

    /** Icon Danh Sách (3 chấm tròn + 3 dòng kẻ) */
    public static Icon createListIcon() {
        return new VectorIcon(16, 16, (g2, w, h) -> {
            g2.setColor(new Color(45, 105, 165));
            for (int i = 0; i < 3; i++) {
                int y = 3 + i * 4;
                g2.fillOval(1, y, 2, 2);
                g2.fillRect(5, y, 10, 2);
            }
        });
    }

    /** Icon Dấu cộng Tạo Phiếu (+) */
    public static Icon createPlusIcon() {
        return new VectorIcon(16, 16, (g2, w, h) -> {
            g2.setColor(new Color(25, 115, 205));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(8, 2, 8, 14);
            g2.drawLine(2, 8, 14, 8);
        });
    }

    /** Icon Kính lúp Tìm Kiếm (Xanh dương) */
    public static Icon createSearchIcon() {
        return new VectorIcon(16, 16, (g2, w, h) -> {
            g2.setColor(new Color(195, 225, 250));
            g2.fillOval(2, 1, 10, 10);
            g2.setColor(new Color(55, 120, 185));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawOval(2, 1, 10, 10);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(10, 9, 15, 14);
        });
    }

    /** Icon Tam giác trỏ xuống nhỏ (▾) */
    public static Icon createSmallArrowDownIcon() {
        return new VectorIcon(10, 8, (g2, w, h) -> {
            g2.setColor(new Color(90, 105, 120));
            int[] xs = {1, 9, 5};
            int[] ys = {2, 2, 6};
            g2.fillPolygon(xs, ys, 3);
        });
    }

    /** Icon Hộp dấu trừ [-] của cây thư mục */
    public static Icon createMinusBoxIcon() {
        return new VectorIcon(11, 11, (g2, w, h) -> {
            g2.setColor(new Color(130, 140, 150));
            g2.drawRect(1, 1, 9, 9);
            g2.setColor(Color.WHITE);
            g2.fillRect(2, 2, 8, 8);
            g2.setColor(new Color(60, 70, 80));
            g2.drawLine(3, 5, 8, 5);
        });
    }

    /** Icon Chevron Lên (Trắng nét đậm cho nút cuộn nổi) */
    public static Icon createChevronUpIcon() {
        return new VectorIcon(24, 18, (g2, w, h) -> {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D path = new Path2D.Float();
            path.moveTo(4, 13);
            path.lineTo(12, 5);
            path.lineTo(20, 13);
            g2.draw(path);
        });
    }

    /** Icon Chevron Xuống (Trắng nét đậm cho nút cuộn nổi) */
    public static Icon createChevronDownIcon() {
        return new VectorIcon(24, 18, (g2, w, h) -> {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D path = new Path2D.Float();
            path.moveTo(4, 5);
            path.lineTo(12, 13);
            path.lineTo(20, 5);
            g2.draw(path);
        });
    }

    @FunctionalInterface
    public interface IconPainter {
        void paint(Graphics2D g2, int width, int height);
    }

    private static class VectorIcon implements Icon {
        private final int width;
        private final int height;
        private final IconPainter painter;

        VectorIcon(int width, int height, IconPainter painter) {
            this.width = width;
            this.height = height;
            this.painter = painter;
        }

        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            painter.paint(g2, width, height);
            g2.dispose();
        }
    }
}
