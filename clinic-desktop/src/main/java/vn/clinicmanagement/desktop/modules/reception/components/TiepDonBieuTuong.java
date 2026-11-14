package vn.clinicmanagement.desktop.modules.reception.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Vector icons chuẩn cho phân hệ Tiếp Đón & Đăng Ký Khám Bệnh.
 * Vẽ trực tiếp bằng Graphics2D, không dùng emoji hệ thống.
 */
public final class TiepDonBieuTuong {
    private TiepDonBieuTuong() {}

    /** Icon Hamburger Menu 3 vạch */
    public static Icon createMenuIcon() {
        return new VectorIcon(20, 18, (g2, w, h) -> {
            g2.setColor(new Color(45, 105, 170));
            g2.fillRect(1, 2, 18, 3);
            g2.fillRect(1, 8, 18, 3);
            g2.fillRect(1, 14, 18, 3);
        });
    }

    /** Icon Máy In */
    public static Icon createPrintIcon() {
        return new VectorIcon(20, 18, (g2, w, h) -> {
            g2.setColor(new Color(110, 125, 145));
            g2.fillRect(3, 7, 14, 8);
            g2.setColor(Color.WHITE);
            g2.fillRect(5, 2, 10, 5);
            g2.fillRect(5, 11, 10, 6);
            g2.setColor(new Color(70, 105, 150));
            g2.drawRect(5, 2, 10, 5);
            g2.drawRect(5, 11, 10, 6);
        });
    }

    /** Icon Làm Mới (Vòng cung xoay) */
    public static Icon createRefreshIcon() {
        return new VectorIcon(20, 18, (g2, w, h) -> {
            g2.setColor(new Color(45, 125, 205));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawArc(3, 2, 14, 14, 40, 275);
            g2.fillPolygon(new int[]{14, 18, 13}, new int[]{2, 6, 7}, 3);
        });
    }

    /** Icon Dấu cộng Xanh (+) - Bệnh Nhân Mới */
    public static Icon createNewPatientIcon() {
        return new VectorIcon(20, 18, (g2, w, h) -> {
            g2.setColor(new Color(25, 120, 215));
            g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(10, 2, 10, 16);
            g2.drawLine(3, 9, 17, 9);
        });
    }

    /** Icon Dấu cộng Đỏ (+) - Nhập BH */
    public static Icon createAddInsuranceIcon() {
        return new VectorIcon(20, 18, (g2, w, h) -> {
            g2.setColor(new Color(225, 45, 45));
            g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(10, 2, 10, 16);
            g2.drawLine(3, 9, 17, 9);
        });
    }

    /** Icon Tích xanh Kiểm tra Thẻ BH */
    public static Icon createVerifyCardIcon() {
        return new VectorIcon(20, 18, (g2, w, h) -> {
            g2.setColor(new Color(40, 165, 80));
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D path = new Path2D.Float();
            path.moveTo(3, 9);
            path.lineTo(8, 14);
            path.lineTo(17, 3);
            g2.draw(path);
        });
    }

    /** Icon Loa phát thanh Gọi Số Tiếp Theo */
    public static Icon createMegaphoneIcon() {
        return new VectorIcon(24, 20, (g2, w, h) -> {
            g2.setColor(new Color(155, 165, 175));
            // Thân loa
            Path2D cone = new Path2D.Float();
            cone.moveTo(4, 8);
            cone.lineTo(12, 4);
            cone.lineTo(12, 16);
            cone.lineTo(4, 12);
            cone.closePath();
            g2.fill(cone);

            // Vành miệng loa
            g2.fillRoundRect(12, 3, 3, 14, 2, 2);

            // Tay cầm
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(6, 12, 4, 17);

            // Sóng âm thanh
            g2.drawArc(15, 6, 5, 8, -45, 90);
            g2.drawArc(18, 4, 5, 12, -45, 90);
        });
    }

    /** Icon Đĩa mềm Lưu Lại (Tím chuẩn EHC) */
    public static Icon createSaveDiskIcon() {
        return new VectorIcon(16, 16, (g2, w, h) -> {
            g2.setColor(new Color(125, 75, 160));
            g2.fillRect(1, 1, 14, 14);
            g2.setColor(Color.WHITE);
            g2.fillRect(4, 1, 8, 5);
            g2.fillRect(3, 8, 10, 6);
            g2.setColor(new Color(100, 55, 135));
            g2.fillRect(6, 2, 2, 3);
        });
    }

    /** Icon Thẻ KCB */
    public static Icon createMedicalCardIcon() {
        return new VectorIcon(16, 16, (g2, w, h) -> {
            g2.setColor(new Color(75, 130, 185));
            g2.fillRoundRect(1, 2, 14, 11, 2, 2);
            g2.setColor(new Color(245, 205, 105));
            g2.fillRect(3, 5, 4, 3);
            g2.setColor(Color.WHITE);
            g2.drawLine(3, 10, 11, 10);
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

        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return width; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
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
