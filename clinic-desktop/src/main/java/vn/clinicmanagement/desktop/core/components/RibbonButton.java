package vn.clinicmanagement.desktop.core.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * Nút điều hướng trên thanh Menu Ribbon ngang chuẩn EHC:
 * - Hiển thị trên 1 dòng ngang gọn gàng: [Icon 16x16] Tên phân hệ
 * - Khi được chọn: đổ nền vàng cam nhạt (#FDE8B8 -> #F9D18B) với viền cam (#D89B48)
 */
public class RibbonButton extends JButton {
    private final String viewKey;
    private boolean isSelected = false;
    private boolean isHovered = false;
    private JPopupMenu popupMenu = null;

    public RibbonButton(String text, String iconEmoji, String viewKey, Consumer<String> onClickAction) {
        super(text, new RibbonVectorIcon(viewKey));
        this.viewKey = viewKey;
        setFont(AppFonts.SMALL);
        setForeground(new Color(20, 20, 20));
        setHorizontalAlignment(SwingConstants.CENTER);
        setIconTextGap(4);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(2, 4, 2, 4));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (popupMenu != null) {
                    popupMenu.show(RibbonButton.this, 0, getHeight());
                } else if (onClickAction != null) {
                    onClickAction.accept(viewKey);
                }
            }
        });
    }

    public void setPopupMenu(JPopupMenu menu) {
        this.popupMenu = menu;
        if (menu != null) {
            menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                    isSelected = true;
                    repaint();
                }

                @Override
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                    isSelected = false;
                    repaint();
                }

                @Override
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                    isSelected = false;
                    repaint();
                }
            });
        }
    }

    public String getViewKey() { return viewKey; }

    @Override
    public void setSelected(boolean b) {
        this.isSelected = b;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width + 4, 26);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (isSelected) {
            GradientPaint gp = new GradientPaint(
                    0, 1, new Color(255, 242, 204),
                    0, h - 2, new Color(248, 210, 138)
            );
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 4, 4));
            g2.setColor(new Color(215, 160, 75));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 4, 4));
        } else if (isHovered) {
            g2.setColor(new Color(225, 234, 246));
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 4, 4));
            g2.setColor(new Color(180, 198, 220));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 4, 4));
        }

        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * Bộ icon vector 16x16 cho thanh Ribbon (không phụ thuộc emoji hệ điều hành)
     */
    private static class RibbonVectorIcon implements Icon {
        private final String key;
        RibbonVectorIcon(String key) { this.key = key != null ? key.toLowerCase() : ""; }

        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);

            switch (key) {
                case "home":
                    // Ngôi nhà cam-vàng
                    g2.setColor(new Color(220, 120, 40));
                    g2.fillPolygon(new int[]{8, 1, 15}, new int[]{1, 8, 8}, 3);
                    g2.setColor(new Color(245, 205, 110));
                    g2.fillRect(3, 8, 10, 7);
                    g2.setColor(new Color(160, 90, 30));
                    g2.drawRect(3, 8, 10, 7);
                    break;
                case "admin":
                    // Biểu tượng Quản lý (định vị đỏ + tòa nhà)
                    g2.setColor(new Color(210, 65, 55));
                    g2.fillOval(4, 1, 8, 8);
                    g2.fillPolygon(new int[]{5, 11, 8}, new int[]{7, 7, 14}, 3);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(6, 3, 4, 4);
                    break;
                case "reception":
                    // Quầy tiếp đón
                    g2.setColor(new Color(215, 155, 70));
                    g2.fillRect(2, 8, 12, 6);
                    g2.setColor(new Color(80, 130, 190));
                    g2.fillOval(6, 2, 4, 4);
                    break;
                case "examination":
                case "treatment":
                    // Ống nghe y tế xanh-vàng
                    g2.setColor(new Color(45, 115, 175));
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawArc(3, 1, 8, 8, 180, 180);
                    g2.drawLine(7, 9, 7, 13);
                    g2.drawLine(7, 13, 12, 13);
                    g2.setColor(new Color(235, 165, 45));
                    g2.fillOval(10, 10, 5, 5);
                    break;
                case "billing":
                    // Hóa đơn viện phí
                    g2.setColor(Color.WHITE);
                    g2.fillRect(3, 1, 10, 14);
                    g2.setColor(new Color(90, 110, 135));
                    g2.drawRect(3, 1, 10, 14);
                    g2.setColor(new Color(40, 150, 80));
                    g2.drawLine(6, 5, 10, 5);
                    g2.drawLine(6, 8, 10, 8);
                    g2.drawLine(6, 11, 10, 11);
                    break;
                case "paraclinical":
                    // Phim X-Quang / CĐHA
                    g2.setColor(new Color(45, 65, 95));
                    g2.fillRoundRect(2, 1, 12, 14, 2, 2);
                    g2.setColor(new Color(135, 200, 245));
                    g2.drawLine(8, 3, 8, 13);
                    g2.drawArc(4, 4, 8, 4, 0, 180);
                    g2.drawArc(4, 8, 8, 4, 0, 180);
                    break;
                case "pharmacy":
                    // Hộp/Tủ thuốc
                    g2.setColor(new Color(230, 175, 105));
                    g2.fillRect(2, 4, 12, 10);
                    g2.setColor(new Color(165, 110, 55));
                    g2.drawRect(2, 4, 12, 10);
                    g2.setColor(new Color(210, 45, 45));
                    g2.fillRect(7, 6, 2, 6);
                    g2.fillRect(5, 8, 6, 2);
                    break;
                case "reports":
                    // Biểu đồ tròn xanh
                    g2.setColor(new Color(45, 130, 205));
                    g2.fillOval(2, 2, 12, 12);
                    g2.setColor(new Color(120, 195, 245));
                    g2.fillArc(2, 2, 12, 12, 0, 110);
                    break;
                case "system":
                    // Bánh răng cài đặt xanh
                    g2.setColor(new Color(65, 115, 165));
                    g2.fillOval(3, 3, 10, 10);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(8, 1, 8, 15);
                    g2.drawLine(1, 8, 15, 8);
                    g2.drawLine(3, 3, 13, 13);
                    g2.drawLine(13, 3, 3, 13);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(6, 6, 4, 4);
                    break;
                case "catalog":
                    // Danh mục 3 dòng
                    g2.setColor(new Color(55, 120, 180));
                    for (int i = 0; i < 3; i++) {
                        int ry = 3 + i * 4;
                        g2.fillOval(2, ry, 2, 2);
                        g2.fillRect(6, ry, 8, 2);
                    }
                    break;
                case "user":
                    // Người dùng
                    g2.setColor(new Color(70, 125, 180));
                    g2.fillOval(5, 2, 6, 6);
                    g2.fillArc(2, 8, 12, 10, 0, 180);
                    break;
                default:
                    // Nút Kết thúc (Đỏ chữ X)
                    g2.setColor(new Color(205, 65, 50));
                    g2.fillRoundRect(2, 2, 12, 12, 3, 3);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawLine(5, 5, 11, 11);
                    g2.drawLine(11, 5, 5, 11);
                    break;
            }
            g2.dispose();
        }
    }
}
