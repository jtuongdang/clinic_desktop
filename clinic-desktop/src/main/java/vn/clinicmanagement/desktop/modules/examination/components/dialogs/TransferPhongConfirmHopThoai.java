package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Hộp thoại thông báo xác nhận chuyển phòng khám:
 * "EHC - Thông Báo"
 * Khớp 100% media_1793381484099_6087d085.jpg:
 * - Icon hỏi chấm (?) tròn màu xanh dương
 * - Nội dung:
 *   "Thao tác này sẽ xóa bệnh nhân khỏi phòng khám hiện tại và chuyển bệnh nhân sang phòng khám mới.
 *    Bạn có chắc chắn muốn chuyển phòng khám không ?"
 * - 2 nút hành động:
 *   + "Có" (icon check xanh lá)
 *   + "Không" (icon chữ X đỏ)
 */
public class TransferPhongConfirmHopThoai extends JDialog {

    private boolean confirmed = false;

    public TransferPhongConfirmHopThoai(Window owner) {
        super(owner, "EHC - Thông Báo", ModalityType.APPLICATION_MODAL);
        initComponents();
    }

    /** Hàm `showConfirm`: `show` xác nhận. */
    public static boolean showConfirm(Component parent) {
        Window win = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        TransferPhongConfirmHopThoai dlg = new TransferPhongConfirmHopThoai(win);
        dlg.setVisible(true);
        return dlg.isConfirmed();
    }

    /** Hàm `isConfirmed`: là `Confirmed`. */
    public boolean isConfirmed() {
        return confirmed;
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = createDialogPanel(() -> {
            confirmed = true;
            dispose();
        }, () -> {
            confirmed = false;
            dispose();
        });

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(getOwner());

        // Phím Esc để đóng (Không), phím Enter để chọn Có
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    confirmed = false;
                    dispose();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmed = true;
                    dispose();
                }
            }
        });
    }

    /**
     * Tạo panel nội dung của dialog (dùng chung cho cả Dialog thực tế và Offscreen Preview)
     */
    public static JPanel createDialogPanel(Runnable onConfirm, Runnable onCancel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(480, 145));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(160, 160, 160), 1),
                new EmptyBorder(16, 20, 14, 20)
        ));

        // Center: Icon + Message
        JPanel centerPanel = new JPanel(new BorderLayout(16, 0));
        centerPanel.setOpaque(false);

        // Icon tròn hỏi chấm (?) màu xanh dương
        JLabel lblIcon = new JLabel(new QuestionCircleIcon());
        lblIcon.setVerticalAlignment(SwingConstants.TOP);
        centerPanel.add(lblIcon, BorderLayout.WEST);

        // Nội dung thông báo 2 dòng
        String htmlMsg = "<html><body style='font-family: Segoe UI, Tahoma, sans-serif; font-size: 11px; color: #1E2024; line-height: 1.4;'>"
                + "Thao tác này sẽ xóa bệnh nhân khỏi phòng khám hiện tại và chuyển bệnh nhân sang phòng khám mới.<br>"
                + "Bạn có chắc chắn muốn chuyển phòng khám không ?"
                + "</body></html>";
        JLabel lblMessage = new JLabel(htmlMsg);
        lblMessage.setVerticalAlignment(SwingConstants.CENTER);
        centerPanel.add(lblMessage, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Button panel bên phải
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Nút "Có" (Check xanh)
        JButton btnYes = new JButton("Có", new GreenCheckIcon());
        btnYes.setFont(UngDungPhongChu.SMALL);
        btnYes.setPreferredSize(new Dimension(84, 26));
        btnYes.setBackground(Color.WHITE);
        btnYes.setForeground(new Color(25, 25, 25));
        btnYes.setFocusPainted(false);
        btnYes.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(175, 185, 200), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        btnYes.addActionListener(e -> {
            if (onConfirm != null) onConfirm.run();
        });

        // Nút "Không" (X đỏ)
        JButton btnNo = new JButton("Không", new RedCrossMiniIcon());
        btnNo.setFont(UngDungPhongChu.SMALL);
        btnNo.setPreferredSize(new Dimension(84, 26));
        btnNo.setBackground(Color.WHITE);
        btnNo.setForeground(new Color(25, 25, 25));
        btnNo.setFocusPainted(false);
        btnNo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(175, 185, 200), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        btnNo.addActionListener(e -> {
            if (onCancel != null) onCancel.run();
        });

        bottomPanel.add(btnYes);
        bottomPanel.add(btnNo);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // VECTOR ICONS
    // =========================================================================

    /**
     * Icon vòng tròn màu xanh dương chứa dấu hỏi trắng (?)
     */
    public static class QuestionCircleIcon implements Icon {
        private final int size = 32;

        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return size; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Nền vòng tròn màu xanh dương gradient nhẹ
            GradientPaint gp = new GradientPaint(
                    x, y, new Color(28, 92, 175),
                    x + size, y + size, new Color(14, 60, 130)
            );
            g2.setPaint(gp);
            g2.fillOval(x, y, size, size);

            // Viền ngoài nhẹ
            g2.setColor(new Color(60, 130, 215, 120));
            g2.drawOval(x, y, size - 1, size - 1);

            // Dấu hỏi (?) màu trắng
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (size - fm.stringWidth("?")) / 2;
            int ty = y + ((size - fm.getHeight()) / 2) + fm.getAscent() - 1;
            g2.drawString("?", tx, ty);

            g2.dispose();
        }
    }

    /**
     * Icon dấu tick xanh lá ✔
     */
    public static class GreenCheckIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(35, 155, 75));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 7, x + 5, y + 11);
            g2.drawLine(x + 5, y + 11, x + 12, y + 3);
            g2.dispose();
        }
    }

    /**
     * Icon dấu X đỏ ❌
     */
    public static class RedCrossMiniIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(205, 45, 45));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 3, y + 3, x + 11, y + 11);
            g2.drawLine(x + 11, y + 3, x + 3, y + 11);
            g2.dispose();
        }
    }
}
