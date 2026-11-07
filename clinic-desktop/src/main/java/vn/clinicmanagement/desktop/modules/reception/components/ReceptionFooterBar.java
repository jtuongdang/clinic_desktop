package vn.clinicmanagement.desktop.modules.reception.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Thanh phím tắt chân trang phân hệ Đăng Ký Khám Bệnh:
 * - Bệnh Nhân Mới (F1)
 * - Lưu Lại (F2)
 * - In Phiếu Khám (F3)
 * - Thẻ KCB (F4)
 */
public class ReceptionFooterBar extends JPanel {

    private JButton btnNew;
    private JButton btnSave;
    private Runnable onSave;
    private Runnable onNew;

    public ReceptionFooterBar() {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 6, 3));
        setBackground(new Color(238, 242, 247));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(205, 215, 226)));
        setPreferredSize(new Dimension(1000, 40));

        btnNew = createFooterButton("Bệnh Nhân Mới\n(F1)", ReceptionIcons.createNewPatientIcon());
        btnSave = createFooterButton("Lưu Lại\n(F2)", ReceptionIcons.createSaveDiskIcon());
        JButton btnPrint = createFooterButton("In Phiếu Khám\n(F3)", ReceptionIcons.createPrintIcon());
        JButton btnCard = createFooterButton("Thẻ KCB\n(F4)", ReceptionIcons.createMedicalCardIcon());
        btnNew.setPreferredSize(new Dimension(120, 28));
        btnSave.setPreferredSize(new Dimension(102, 28));
        btnPrint.setPreferredSize(new Dimension(122, 28));
        btnCard.setPreferredSize(new Dimension(98, 28));

        add(btnNew);
        add(btnSave);
        add(btnPrint);
        add(btnCard);

        btnSave.addActionListener(e -> {
            if (onSave != null) onSave.run();
        });
        btnNew.addActionListener(e -> {
            if (onNew != null) onNew.run();
        });

        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("F1"), "receptionNew");
        actionMap.put("receptionNew", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnNew.isEnabled()) {
                    btnNew.doClick();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("F2"), "receptionSave");
        actionMap.put("receptionSave", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnSave.isEnabled()) {
                    btnSave.doClick();
                }
            }
        });
    }

    public void setOnSave(Runnable listener) {
        this.onSave = listener;
    }

    public void setOnNew(Runnable listener) {
        this.onNew = listener;
    }

    public void setSaving(boolean saving) {
        btnSave.setEnabled(!saving);
    }

    private JButton createFooterButton(String text, Icon icon) {
        String[] parts = text.split("\n");
        String line1 = parts.length > 0 ? parts[0] : text;
        String line2 = parts.length > 1 ? parts[1] : "";
        String html = "<html><center><nobr>" + line1 + "</nobr><br><nobr>" + line2 + "</nobr></center></html>";
        JButton btn = new JButton(html, icon) {
            private boolean isHovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                GradientPaint gp = new GradientPaint(0, 0,
                        isHovered ? new Color(245, 250, 255) : new Color(255, 255, 255),
                        0, h,
                        isHovered ? new Color(215, 228, 242) : new Color(232, 238, 246));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 4, 4));

                g2.setColor(new Color(175, 190, 208));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 4, 4));
                g2.dispose();

                super.paintComponent(g);
            }
        };

        btn.setFont(AppFonts.SMALL);
        btn.setForeground(new Color(25, 35, 45));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(6);
        btn.setPreferredSize(new Dimension(112, 28));
        btn.setMargin(new Insets(1, 0, 1, 0));

        return btn;
    }
}
