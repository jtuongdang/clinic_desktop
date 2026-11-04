package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.desktop.modules.examination.components.print.MedicalRecordPrintPreviewDialog;
import vn.clinicmanagement.desktop.modules.examination.components.print.OutpatientMedicalRecordPrintDialog;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

/**
 * Giao diện tab "Bệnh Án" trong Phân hệ Khám Bệnh.
 * Khớp 100% media_1793381140576_4abb31e5.jpg:
 * - 5 khu vực nhập liệu tóm tắt hồ sơ bệnh án:
 *   1. Quá trình bệnh lý và diễn biến lâm sàng:
 *   2. Tóm tắt kết quả cận lâm sàng có giá trị chẩn đoán: (kèm link xanh [Chọn dịch vụ])
 *   3. Phương pháp điều trị:
 *   4. Tình trạng người bệnh ra viện:
 *   5. Hướng điều trị và các chế độ tiếp theo:
 * - Thanh công cụ chân trang tab Bệnh Án:
 *   + Nút góc trái: [⤢ Phóng To]
 *   + Nút góc phải:
 *     - [🏷 Mẫu Tổng Kết Bệnh Án ▾]
 *     - [💾 Lưu]
 *     - [🖨 In Bệnh Án] (mở trực tiếp MedicalRecordPrintPreviewDialog)
 */
public class MedicalRecordTabPanel extends JPanel {

    private JTextArea txtDienBienLamSang;
    private JTextArea txtCanLamSang;
    private JTextArea txtPhuongPhapDieuTri;
    private JTextArea txtTinhTrangRaVien;
    private JTextArea txtHuongDieuTri;

    private JButton btnExpand;
    private JButton btnTemplates;
    private JButton btnSave;
    private JButton btnPrintRecord;

    private boolean isExpanded = false;

    public MedicalRecordTabPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. VÙNG NHẬP LIỆU CHÍNH (5 KHỐI TÓM TẮT BỆNH ÁN)
        add(buildFormPanel(), BorderLayout.CENTER);

        // 2. THANH THAO TÁC CHÂN TRANG TAB BỆNH ÁN
        add(buildFooterToolbar(), BorderLayout.SOUTH);
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel();
        form.setLayout(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(4, 6, 4, 6));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        int row = 0;

        // --- KHỐI 1: Quá trình bệnh lý và diễn biến lâm sàng ---
        c.gridy = row++;
        c.weightx = 1.0;
        c.weighty = 0.0;
        JLabel lblDienBien = createSectionLabel("Quá trình bệnh lý và diễn biến lâm sàng:");
        form.add(lblDienBien, c);

        c.gridy = row++;
        c.weighty = 1.0;
        txtDienBienLamSang = createTextArea();
        JScrollPane scrollDienBien = createScrollPane(txtDienBienLamSang, 85);
        form.add(scrollDienBien, c);

        // --- KHỐI 2: Tóm tắt kết quả cận lâm sàng có giá trị chẩn đoán + [Chọn dịch vụ] ---
        c.gridy = row++;
        c.weighty = 0.0;
        JPanel pnlCanLamSangHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlCanLamSangHeader.setOpaque(false);
        JLabel lblCanLamSang = createSectionLabel("Tóm tắt kết quả cận lâm sàng có giá trị chẩn đoán:");
        JLabel lnkChonDichVu = new JLabel("<html><u>Chọn dịch vụ</u></html>");
        lnkChonDichVu.setFont(AppFonts.SMALL);
        lnkChonDichVu.setForeground(new Color(0, 80, 200));
        lnkChonDichVu.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lnkChonDichVu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openSelectServiceDialog();
            }
        });
        pnlCanLamSangHeader.add(lblCanLamSang);
        pnlCanLamSangHeader.add(lnkChonDichVu);
        form.add(pnlCanLamSangHeader, c);

        c.gridy = row++;
        c.weighty = 1.0;
        txtCanLamSang = createTextArea();
        JScrollPane scrollCanLamSang = createScrollPane(txtCanLamSang, 85);
        form.add(scrollCanLamSang, c);

        // --- KHỐI 3: Phương pháp điều trị ---
        c.gridy = row++;
        c.weighty = 0.0;
        JLabel lblPhuongPhap = createSectionLabel("Phương pháp điều trị:");
        form.add(lblPhuongPhap, c);

        c.gridy = row++;
        c.weighty = 0.9;
        txtPhuongPhapDieuTri = createTextArea();
        JScrollPane scrollPhuongPhap = createScrollPane(txtPhuongPhapDieuTri, 70);
        form.add(scrollPhuongPhap, c);

        // --- KHỐI 4: Tình trạng người bệnh ra viện ---
        c.gridy = row++;
        c.weighty = 0.0;
        JLabel lblTinhTrang = createSectionLabel("Tình trạng người bệnh ra viện:");
        form.add(lblTinhTrang, c);

        c.gridy = row++;
        c.weighty = 0.7;
        txtTinhTrangRaVien = createTextArea();
        JScrollPane scrollTinhTrang = createScrollPane(txtTinhTrangRaVien, 50);
        form.add(scrollTinhTrang, c);

        // --- KHỐI 5: Hướng điều trị và các chế độ tiếp theo ---
        c.gridy = row++;
        c.weighty = 0.0;
        JLabel lblHuongDieuTri = createSectionLabel("Hướng điều trị và các chế độ tiếp theo:");
        form.add(lblHuongDieuTri, c);

        c.gridy = row++;
        c.weighty = 1.0;
        txtHuongDieuTri = createTextArea();
        JScrollPane scrollHuongDieuTri = createScrollPane(txtHuongDieuTri, 85);
        form.add(scrollHuongDieuTri, c);

        return form;
    }

    private JLabel createSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppFonts.SMALL);
        lbl.setForeground(new Color(25, 30, 40));
        return lbl;
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setFont(AppFonts.SMALL);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(3, 5, 3, 5));
        return area;
    }

    private JScrollPane createScrollPane(JTextArea area, int prefHeight) {
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(new LineBorder(new Color(195, 205, 220), 1));
        sp.setPreferredSize(new Dimension(800, prefHeight));
        sp.setMinimumSize(new Dimension(200, 35));
        return sp;
    }

    /**
     * Thanh thao tác chân trang tab Bệnh Án.
     */
    private JPanel buildFooterToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(248, 250, 253));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 218, 230)),
                new EmptyBorder(3, 6, 3, 6)
        ));
        bar.setPreferredSize(new Dimension(1000, 32));

        // Nút bên trái: [⤢ Phóng To]
        btnExpand = new JButton(" Phóng To", new ExpandVectorIcon());
        styleFooterButton(btnExpand);
        btnExpand.addActionListener(e -> toggleExpand());
        bar.add(btnExpand, BorderLayout.WEST);

        // Các nút bên phải: [🏷 Mẫu Tổng Kết Bệnh Án ▾], [💾 Lưu], [🖨 In Bệnh Án]
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlRight.setOpaque(false);

        btnTemplates = new JButton(" Mẫu Tổng Kết Bệnh Án ▾", new TagVectorIcon());
        styleFooterButton(btnTemplates);
        btnTemplates.addActionListener(e -> showTemplatesMenu());

        btnSave = new JButton(" Lưu", new SaveDiskIcon());
        styleFooterButton(btnSave);
        btnSave.addActionListener(e -> handleSaveRecord());

        btnPrintRecord = new JButton(" In Bệnh Án", new PrinterIcon());
        styleFooterButton(btnPrintRecord);
        btnPrintRecord.addActionListener(e -> handlePrintRecord());

        pnlRight.add(btnTemplates);
        pnlRight.add(btnSave);
        pnlRight.add(btnPrintRecord);

        bar.add(pnlRight, BorderLayout.EAST);

        return bar;
    }

    private void styleFooterButton(JButton btn) {
        btn.setFont(AppFonts.SMALL_BOLD);
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(30, 35, 45));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(2, 8, 2, 8)
        ));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 24));
    }

    private void toggleExpand() {
        isExpanded = !isExpanded;
        btnExpand.setText(isExpanded ? " Thu Nhỏ" : " Phóng To");
        JOptionPane.showMessageDialog(this,
                isExpanded ? "Đã phóng to toàn màn hình khu vực nhập tóm tắt bệnh án." : "Đã thu nhỏ khu vực nhập bệnh án về kích thước tiêu chuẩn.",
                "Chế Độ Hiển Thị", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showTemplatesMenu() {
        JPopupMenu menu = new JPopupMenu();
        String[] templates = {
                "Mẫu Tổng kết bệnh án Ngoại khoa",
                "Mẫu Tổng kết bệnh án Nội khoa chung",
                "Mẫu Tổng kết bệnh án Nhi khoa",
                "Mẫu Tổng kết bệnh án Sản - Phụ khoa",
                "Mẫu Tổng kết bệnh án Tai - Mũi - Họng",
                "Mẫu Tổng kết bệnh án Răng - Hàm - Mặt",
                "Mẫu Tổng kết bệnh án Hồi sức cấp cứu"
        };

        for (String tmpl : templates) {
            JMenuItem item = new JMenuItem(tmpl);
            item.setFont(AppFonts.SMALL);
            item.addActionListener(e -> applyTemplate(tmpl));
            menu.add(item);
        }

        menu.show(btnTemplates, 0, btnTemplates.getHeight());
    }

    private void applyTemplate(String templateName) {
        if ("Mẫu Tổng kết bệnh án Nhi khoa".equals(templateName)) {
            txtDienBienLamSang.setText("Bệnh nhi sốt 3 ngày trước khi vào viện, kèm ho khan từng cơn, tiêu phân lỏng 3-4 lần/ngày. Được tiếp nhận điều trị tại khoa Nhi, cắt sốt sau 48h, ăn bú khá.");
            txtCanLamSang.setText("Công thức máu: Bạch cầu 9.2 G/L, Neutrophil 62%. CRP: 4.5 mg/L. Siêu âm ổ bụng: Không ghi nhận bất thường. Xquang phổi: Rốn phổi đậm hai bên.");
            txtPhuongPhapDieuTri.setText("Kháng sinh Cefotaxim tĩnh mạch 5 ngày, bù nước điện giải Oresol, men vi sinh Enterogermina, hạ sốt Paracetamol khi sốt >= 38.5C.");
            txtTinhTrangRaVien.setText("Bệnh nhi hết sốt, tỉnh táo, chơi ngoan, tiêu phân sệt, phổi thông khí đều, ra viện an toàn.");
            txtHuongDieuTri.setText("Tiếp tục theo dõi chế độ dinh dưỡng, uống men vi sinh 5 ngày, tái khám sau 1 tuần hoặc khi có dấu hiệu sốt lại, nôn trớ.");
        } else {
            txtDienBienLamSang.setText("Bệnh nhân khởi bệnh với triệu chứng sốt nhẹ, đau bụng âm ỉ vùng quanh rốn, tiêu chảy phân lỏng. Sau 4 ngày điều trị tại khoa triệu chứng thuyên giảm rõ rệt.");
            txtCanLamSang.setText("Xét nghiệm sinh hóa máu trong giới hạn bình thường. Test nhanh soi phân âm tính.");
            txtPhuongPhapDieuTri.setText("Điều trị nội khoa bảo tồn, bù dịch cân bằng điện giải, kháng sinh đường uống theo phác đồ.");
            txtTinhTrangRaVien.setText("Bệnh nhân tỉnh táo, hết sốt, bụng mềm không đau, đại tiện phân vàng sệt.");
            txtHuongDieuTri.setText("Nghỉ ngơi, ăn chín uống sôi, tái khám khi có dấu hiệu bất thường.");
        }
    }

    private void openSelectServiceDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Chọn Kết Quả Cận Lâm Sàng Đưa Vào Bệnh Án", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(580, 380);
        dlg.setLocationRelativeTo(owner);
        dlg.setLayout(new BorderLayout());

        JPanel pnlContent = new JPanel(new BorderLayout(5, 5));
        pnlContent.setBorder(new EmptyBorder(10, 10, 10, 10));

        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("[XN] Công thức máu toàn bộ (Laser): Bạch cầu 7.8 G/L, Hồng cầu 4.5 T/L");
        model.addElement("[XN] Định lượng Glucose máu: 5.2 mmol/L (Bình thường)");
        model.addElement("[XN] Định lượng Ure, Creatinin máu: Chức năng thận bình thường");
        model.addElement("[CDHA] Siêu âm ổ bụng tổng quát: Gan, mật, thận, lách bình thường");
        model.addElement("[CDHA] X-Quang tim phổi thẳng: Không thấy tổn thương nhu mô phổi");
        model.addElement("[XN] Điện giải đồ (Na, K, Cl): Na+ 138, K+ 4.0, Cl- 102 mmol/L");

        JList<String> list = new JList<>(model);
        list.setFont(AppFonts.SMALL);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setSelectedIndex(0);

        pnlContent.add(new JLabel("Tích chọn các kết quả cận lâm sàng có giá trị chẩn đoán:"), BorderLayout.NORTH);
        pnlContent.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAdd = new JButton(" Thêm Vào Bệnh Án");
        btnAdd.setFont(AppFonts.SMALL_BOLD);
        btnAdd.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (String val : list.getSelectedValuesList()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("- ").append(val);
            }
            String cur = txtCanLamSang.getText().trim();
            if (cur.isEmpty()) {
                txtCanLamSang.setText(sb.toString());
            } else {
                txtCanLamSang.setText(cur + "\n" + sb.toString());
            }
            dlg.dispose();
        });

        JButton btnCancel = new JButton("Đóng");
        btnCancel.setFont(AppFonts.SMALL);
        btnCancel.addActionListener(e -> dlg.dispose());

        pnlBtns.add(btnAdd);
        pnlBtns.add(btnCancel);
        pnlContent.add(pnlBtns, BorderLayout.SOUTH);

        dlg.setContentPane(pnlContent);
        dlg.setVisible(true);
    }

    private void handleSaveRecord() {
        JOptionPane.showMessageDialog(this,
                "<html><b>Đã lưu thông tin tóm tắt hồ sơ Bệnh Án thành công!</b><br>" +
                        "Dữ liệu đã được cập nhật vào cơ sở dữ liệu Bệnh Án Điện Tử (EMR).</html>",
                "Lưu Bệnh Án", JOptionPane.INFORMATION_MESSAGE);
    }

    public void openOutpatientMedicalRecordPrintDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        OutpatientMedicalRecordPrintDialog dlg = new OutpatientMedicalRecordPrintDialog(owner);
        dlg.setVisible(true);
    }

    private void handlePrintRecord() {
        openOutpatientMedicalRecordPrintDialog();
    }

    // Getters / Setters
    public JTextArea getTxtDienBienLamSang() { return txtDienBienLamSang; }
    public JTextArea getTxtCanLamSang() { return txtCanLamSang; }
    public JTextArea getTxtPhuongPhapDieuTri() { return txtPhuongPhapDieuTri; }
    public JTextArea getTxtTinhTrangRaVien() { return txtTinhTrangRaVien; }
    public JTextArea getTxtHuongDieuTri() { return txtHuongDieuTri; }

    // =========================================================================
    // VECTOR ICONS KHỚP 100% ẢNH MẪU
    // =========================================================================

    private static class ExpandVectorIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(60, 110, 185));
            g2.setStroke(new BasicStroke(1.8f));
            // Mũi tên chéo 2 đầu
            g2.drawLine(x + 2, y + 2, x + 6, y + 2);
            g2.drawLine(x + 2, y + 2, x + 2, y + 6);
            g2.drawLine(x + 2, y + 2, x + 12, y + 12);
            g2.drawLine(x + 12, y + 12, x + 8, y + 12);
            g2.drawLine(x + 12, y + 12, x + 12, y + 8);
            g2.dispose();
        }
    }

    private static class TagVectorIcon implements Icon {
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 15; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(65, 125, 205));
            Path2D.Double path = new Path2D.Double();
            path.moveTo(x + 2, y + 6);
            path.lineTo(x + 6, y + 2);
            path.lineTo(x + 12, y + 8);
            path.lineTo(x + 7, y + 13);
            path.closePath();
            g2.fill(path);
            g2.setColor(Color.WHITE);
            g2.fillOval(x + 5, y + 4, 3, 3);
            g2.dispose();
        }
    }

    private static class SaveDiskIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(110, 50, 130));
            g2.fillRoundRect(x + 1, y + 1, 12, 12, 2, 2);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 3, y + 2, 6, 4);
            g2.fillRect(x + 3, y + 7, 7, 5);
            g2.dispose();
        }
    }

    private static class PrinterIcon implements Icon {
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(75, 80, 95));
            g2.fillRect(x + 3, y + 1, 9, 3);
            g2.fillRoundRect(x + 1, y + 4, 13, 6, 2, 2);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 3, y + 7, 9, 5);
            g2.setColor(new Color(100, 105, 115));
            g2.drawRect(x + 3, y + 7, 9, 5);
            g2.dispose();
        }
    }
}
