package vn.clinicmanagement.desktop.core.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Thẻ tab phía dưới (MDI Tab) chuẩn EHC:
 * - Tab đang chọn: nền xanh đậm (#1C66B4), chữ trắng, có mũi tên tam giác trắng nhỏ ở cạnh trên
 * - Tab không chọn: nền xám xanh nhạt (#E9EFF7), chữ đen, nút đóng '×'
 */
public class BottomTab extends JPanel {
    private final String viewKey;
    private boolean isSelected;
    private boolean isHovered = false;

    private final JLabel lblText;
    private final JLabel lblClose;

    public BottomTab(String title, String viewKey, boolean selected, Consumer<String> onSelect, Consumer<String> onClose) {
        this.viewKey = viewKey;
        this.isSelected = selected;

        setLayout(new FlowLayout(FlowLayout.CENTER, 8, 4));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(false);

        lblText = new JLabel(title);
        lblText.setFont(AppFonts.SMALL);

        lblClose = new JLabel("×");
        lblClose.setFont(AppFonts.BODY_BOLD);

        add(lblText);
        add(lblClose);

        updateStyles();

        MouseAdapter ma = new MouseAdapter() {
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
                if (e.getSource() == lblClose) {
                    if (onClose != null) onClose.accept(viewKey);
                } else {
                    if (onSelect != null) onSelect.accept(viewKey);
                }
            }
        };

        addMouseListener(ma);
        lblText.addMouseListener(ma);
        lblClose.addMouseListener(ma);
    }

    public String getViewKey() { return viewKey; }

    public void setSelected(boolean b) {
        this.isSelected = b;
        updateStyles();
        repaint();
    }

    private void updateStyles() {
        lblText.setFont(AppFonts.SMALL);
        if (isSelected) {
            lblText.setForeground(Color.WHITE);
            lblClose.setForeground(new Color(235, 242, 255));
        } else {
            lblText.setForeground(new Color(25, 25, 25));
            lblClose.setForeground(new Color(70, 70, 70));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (isSelected) {
            // Nền xanh đậm chuẩn EHC
            g2.setColor(new Color(28, 102, 180));
            g2.fillRect(0, 0, w, h);

            // Mũi tên tam giác trắng nhỏ ở chính giữa cạnh trên của tab đang active
            int midX = w / 2 - 4;
            int[] xs = {midX - 5, midX + 5, midX};
            int[] ys = {0, 0, 5};
            g2.setColor(Color.WHITE);
            g2.fillPolygon(xs, ys, 3);
        } else {
            g2.setColor(isHovered ? new Color(242, 246, 252) : new Color(233, 239, 247));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(195, 206, 220));
            g2.drawLine(w - 1, 0, w - 1, h);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
