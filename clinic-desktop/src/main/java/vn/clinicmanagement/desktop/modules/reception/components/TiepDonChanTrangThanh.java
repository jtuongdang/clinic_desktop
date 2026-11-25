package vn.clinicmanagement.desktop.modules.reception.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

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
public class TiepDonChanTrangThanh extends JPanel {

    private JButton btnNew;
    private JButton btnPrint;
    private Runnable onPrint;
    private boolean saving;
    private JButton btnSave;
    private Runnable onSave;
    private Runnable onNew;

    public TiepDonChanTrangThanh() {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 6, 3));
        setBackground(new Color(250, 250, 250));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(205, 215, 226)));
        setPreferredSize(new Dimension(1000, 36));

        btnNew = createFooterButton("Bệnh Nhân Mới\n(F1)", TiepDonBieuTuong.createNewPatientIcon());
        btnSave = createFooterButton("Lưu Lại\n(F2)", TiepDonBieuTuong.createSaveDiskIcon());
        btnPrint = createFooterButton("In Phiếu Khám\n(F3)", TiepDonBieuTuong.createPrintIcon());
        JButton btnCard = createFooterButton("Thẻ KCB\n(F4)", TiepDonBieuTuong.createPrintIcon());
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

        btnPrint.addActionListener(e -> { if (!saving && onPrint != null) onPrint.run(); });

        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("F3"), "receptionPrint");
        actionMap.put("receptionPrint", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (btnPrint.isEnabled()) btnPrint.doClick(); }
        });
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

    /** Gắn cùng thao tác cho nút in và F3. */
    public void setOnPrint(Runnable listener) { this.onPrint = listener; }

    public void setSaving(boolean saving) {
        this.saving = saving;
        btnSave.setEnabled(!saving);
        btnPrint.setEnabled(!saving);
    }

    private JButton createFooterButton(String text, Icon icon) {
        String[] parts = text.split("\n");
        String line1 = parts.length > 0 ? parts[0] : text;
        String line2 = parts.length > 1 ? parts[1] : "";
        String html = "<html><center><nobr>" + line1 + "</nobr><br><nobr>" + line2 + "</nobr></center></html>";
        JButton btn = new JButton(html, TiepDonKieuMau.icon(icon, 26, 25));
        btn.setFont(UngDungPhongChu.SMALL);
        btn.setForeground(new Color(25, 35, 45));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(6);
        btn.setPreferredSize(new Dimension(112, 28));
        btn.setMargin(new Insets(1, 0, 1, 0));

        TiepDonKieuMau.apply(btn);
        return btn;
    }
}
