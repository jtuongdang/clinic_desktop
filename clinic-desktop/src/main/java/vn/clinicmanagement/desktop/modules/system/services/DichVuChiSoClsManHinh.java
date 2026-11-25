package vn.clinicmanagement.desktop.modules.system.services;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Giao diện cấu hình "Chỉ Số CLS" (đồng bộ mẫu EHC - Cập Nhật Dịch Vụ).
 * Dùng chung cho tab Chỉ Số Xét Nghiệm trong Cấu Hình Dịch Vụ và mục Quản Lý Doanh Thu.
 */
public final class DichVuChiSoClsManHinh extends JPanel {
    private static final Color BG = new Color(249, 249, 249);
    private static final Color BORDER = new Color(185, 188, 194);
    private static final Color GRID = new Color(207, 210, 215);

    private static final String[] RANGE_COLUMNS = {
            "Khoảng Giá Trị", "Tuổi", "Giới", "Mô Tả", "Bất Thường",
            "Thấp", "Cao", "Quá Thấp", "Quá Cao", "In", "In Thay KQ", "Ghi Chú"
    };

    final JTextField labUnitField = new JTextField("ng/mL");
    final JTextField geneTypeField = new JTextField();
    final JComboBox<String> specimenCombo = new JComboBox<>(new String[]{
            "(Chưa xác định)", "Máu toàn phần", "Huyết thanh", "Huyết tương", "Nước tiểu", "Phân", "Dịch não tủy", "Đờm"
    });
    final JComboBox<String> machineGroupCombo = new JComboBox<>(new String[]{
            "", "Sinh hóa", "Huyết học", "Miễn dịch", "Vi sinh", "Nước tiểu"
    });
    final JTextField manualResultField = new JTextField();

    final JTextField decimalDigitsField = new JTextField("0");
    final JTextField rightIndentField = new JTextField("0");
    final JComboBox<String> methodCombo = new JComboBox<>(new String[]{
            "(Không xác định)", "Đo quang", "ELISA", "Điện hóa phát quang", "PCR", "Đếm tế bào"
    });
    final JTextField henNoteField = new JTextField();
    final JTextField barcodePrefixField = new JTextField();
    final JTextField testMethodField = new JTextField();

    final JCheckBox chkBoldName = new JCheckBox("In đậm tên XN");
    final JCheckBox chkSkipResultCheck = new JCheckBox("Bỏ kiểm tra trạng thái có kết quả");
    final JCheckBox chkSkipEmptyPrint = new JCheckBox("Bỏ in khi không có kết quả");
    final JCheckBox chkIso = new JCheckBox("ISO");
    final JCheckBox chkClearOnCancel = new JCheckBox("Xóa kết quả khi \"Hủy Kết Quả\"");
    final JCheckBox chkPrintHenSeparate = new JCheckBox("In riêng HEN");
    final JCheckBox chkPrintHenTube = new JCheckBox("In ống HEN");
    final JCheckBox chkNoPrintHenTube = new JCheckBox("Không in ống HEN");
    final JCheckBox chkItalicName = new JCheckBox("In nghiêng tên xét nghiệm trên phiếu trả kết quả");

    final JTextArea normalRangeArea = new JTextArea("(<250.0)");
    final JTextArea autoFormulaArea = new JTextArea();
    final JTextArea printResultArea = new JTextArea();

    final DefaultTableModel rangeModel = new DefaultTableModel(RANGE_COLUMNS, 0);
    final JTable rangeTable = new JTable(rangeModel);

    public DichVuChiSoClsManHinh() {
        this(true);
    }

    public DichVuChiSoClsManHinh(boolean showSaveFooter) {
        super(new BorderLayout());
        setBackground(BG);

        JPanel top = buildTopForm();
        JTabbedPane subTabs = buildSubTabs();

        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(6, 10, 4, 10));
        content.add(top, BorderLayout.NORTH);
        content.add(subTabs, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        if (showSaveFooter) {
            add(buildFooter(), BorderLayout.SOUTH);
        }
    }

    private JPanel buildTopForm() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        // Row 1
        JPanel r1 = new JPanel(new GridLayout(1, 5, 8, 0));
        r1.setOpaque(false);
        r1.add(labeled("Đơn vị xét nghiệm", labUnitField));
        r1.add(labeledWithPicker("Loại gen sinh học phân tử", geneTypeField));
        r1.add(labeled("Loại bệnh phẩm xét nghiệm", specimenCombo));
        r1.add(labeled("Nhóm máy thực hiện xét nghiệm", machineGroupCombo));
        r1.add(labeled("Kết quả thủ công (ngăn cách bởi '/')", manualResultField));
        top.add(r1);
        top.add(Box.createVerticalStrut(4));

        // Row 2 + 3 (compact GridBag)
        JPanel r2 = new JPanel(new GridBagLayout());
        r2.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 0, 2, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;

        gc.gridy = 0; gc.gridx = 0; gc.weightx = 0.16;
        r2.add(labeled("Số ký số sau dấu , ở kết quả khi in", decimalDigitsField), gc);
        gc.gridx = 1; gc.weightx = 0.08;
        r2.add(labeled("Số tab lùi phải", rightIndentField), gc);
        gc.gridx = 2; gc.weightx = 0.12;
        r2.add(labeled("Phương pháp XN", methodCombo), gc);
        gc.gridx = 3; gc.weightx = 0.14;
        r2.add(labeled("Ghi chú HEN", henNoteField), gc);

        JPanel checkGrid = new JPanel(new GridLayout(2, 3, 8, 2));
        checkGrid.setOpaque(false);
        for (JCheckBox cb : List.of(
                chkBoldName, chkSkipResultCheck, chkSkipEmptyPrint,
                chkIso, chkClearOnCancel, chkPrintHenSeparate)) {
            cb.setFont(UngDungPhongChu.SMALL);
            cb.setOpaque(false);
            checkGrid.add(cb);
        }
        gc.gridx = 4; gc.weightx = 0.50;
        gc.insets = new Insets(10, 4, 2, 0);
        r2.add(checkGrid, gc);

        // Row 3
        gc.gridy = 1; gc.insets = new Insets(2, 0, 2, 8);
        JPanel henChecks = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        henChecks.setOpaque(false);
        chkPrintHenTube.setFont(UngDungPhongChu.SMALL);
        chkPrintHenTube.setOpaque(false);
        chkNoPrintHenTube.setFont(UngDungPhongChu.SMALL);
        chkNoPrintHenTube.setOpaque(false);
        henChecks.add(chkPrintHenTube);
        henChecks.add(chkNoPrintHenTube);
        gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 0.24;
        r2.add(henChecks, gc);

        gc.gridwidth = 1;
        gc.gridx = 2; gc.weightx = 0.12;
        r2.add(inlineLabeled("Tiền tố barcode", barcodePrefixField), gc);
        gc.gridx = 3; gc.weightx = 0.14;
        r2.add(inlineLabeled("Phương pháp thử", testMethodField), gc);
        gc.gridx = 4; gc.weightx = 0.50;
        chkItalicName.setFont(UngDungPhongChu.SMALL);
        chkItalicName.setOpaque(false);
        r2.add(chkItalicName, gc);

        top.add(r2);
        top.add(Box.createVerticalStrut(4));

        // Row 4: 3 TextAreas
        JPanel r4 = new JPanel(new GridBagLayout());
        r4.setOpaque(false);
        GridBagConstraints tc = new GridBagConstraints();
        tc.insets = new Insets(2, 0, 2, 8);
        tc.fill = GridBagConstraints.BOTH;
        tc.gridy = 0;

        tc.gridx = 0; tc.weightx = 0.30;
        r4.add(areaBox("Chỉ số xét nghiệm bình thường", normalRangeArea, false), tc);
        tc.gridx = 1; tc.weightx = 0.48;
        r4.add(areaBox("Công thức tính tự động", autoFormulaArea, true), tc);
        tc.gridx = 2; tc.weightx = 0.22; tc.insets = new Insets(2, 0, 2, 0);
        r4.add(areaBox("In kết quả xét nghiệm", printResultArea, false), tc);

        top.add(r4);
        return top;
    }

    private JTabbedPane buildSubTabs() {
        JTabbedPane subTabs = new JTabbedPane();
        subTabs.setFont(UngDungPhongChu.BODY);

        // Tab 1: Chỉ Số Bình Thường/Bất Thường
        JPanel rangePanel = new JPanel(new BorderLayout(0, 4));
        rangePanel.setBackground(Color.WHITE);

        rangeTable.setRowHeight(24);
        rangeTable.setFont(UngDungPhongChu.BODY);
        rangeTable.setGridColor(GRID);
        rangeTable.setShowGrid(true);
        rangeTable.setFillsViewportHeight(true);
        rangeTable.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        rangeTable.getTableHeader().setBackground(new Color(250, 250, 250));
        JScrollPane scroll = new JScrollPane(rangeTable);
        scroll.setBorder(BorderFactory.createLineBorder(GRID));
        rangePanel.add(scroll, BorderLayout.CENTER);

        JPanel rangeActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        rangeActions.setBackground(BG);
        JButton addRange = new JButton("＋ Thêm Khoảng");
        addRange.setFont(UngDungPhongChu.BODY);
        addRange.setForeground(new Color(0, 95, 190));
        addRange.setContentAreaFilled(false);
        addRange.setBorder(new EmptyBorder(2, 4, 2, 4));
        addRange.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addRange.addActionListener(e -> rangeModel.addRow(new Object[]{"", "", "", "", "", "", "", "", "", "", "", ""}));

        JButton removeRange = new JButton("✕ Xóa Khoảng");
        removeRange.setFont(UngDungPhongChu.BODY);
        removeRange.setForeground(new Color(200, 60, 20));
        removeRange.setContentAreaFilled(false);
        removeRange.setBorder(new EmptyBorder(2, 4, 2, 4));
        removeRange.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeRange.addActionListener(e -> {
            int row = rangeTable.getSelectedRow();
            if (row >= 0) rangeModel.removeRow(row);
        });

        rangeActions.add(addRange);
        rangeActions.add(removeRange);
        rangePanel.add(rangeActions, BorderLayout.SOUTH);

        subTabs.addTab("Chỉ Số Bình Thường/Bất Thường", rangePanel);
        subTabs.addTab("Quy Trình Xét Nghiệm", emptySubTab("Quy Trình Xét Nghiệm"));
        subTabs.addTab("Công Thức Tính Tự Động", emptySubTab("Công Thức Tính Tự Động"));
        subTabs.addTab("Thay Thế Kết Quả", emptySubTab("Thay Thế Kết Quả"));

        return subTabs;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, new Color(160, 160, 160)));
        footer.setPreferredSize(new Dimension(100, 44));

        JButton saveBtn = new JButton("Lưu", DichVuCauHinhManHinh.saveIcon());
        saveBtn.setFont(UngDungPhongChu.BODY_BOLD);
        saveBtn.setContentAreaFilled(false);
        saveBtn.setBorder(new EmptyBorder(4, 12, 4, 18));
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> JOptionPane.showMessageDialog(
                this, "Đã lưu cấu hình Chỉ Số CLS.", "Lưu", JOptionPane.INFORMATION_MESSAGE));
        footer.add(saveBtn, BorderLayout.EAST);
        return footer;
    }

    private static JPanel emptySubTab(String name) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        JLabel lbl = new JLabel("Chưa có dữ liệu " + name + ".");
        lbl.setFont(UngDungPhongChu.BODY);
        lbl.setForeground(Color.GRAY);
        p.add(lbl);
        return p;
    }

    private static JPanel labeled(String title, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(UngDungPhongChu.SMALL);
        field.setFont(UngDungPhongChu.BODY);
        field.setPreferredSize(new Dimension(100, 24));
        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static JPanel inlineLabeled(String title, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(UngDungPhongChu.SMALL);
        field.setFont(UngDungPhongChu.BODY);
        field.setPreferredSize(new Dimension(80, 24));
        p.add(lbl, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel labeledWithPicker(String title, JTextField field) {
        JPanel box = new JPanel(new BorderLayout(2, 0));
        box.setOpaque(false);
        field.setFont(UngDungPhongChu.BODY);
        field.setPreferredSize(new Dimension(100, 24));
        JButton btn = new JButton("…");
        btn.setFont(UngDungPhongChu.SMALL_BOLD);
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.addActionListener(e -> {
            String val = JOptionPane.showInputDialog(this, title + ":", field.getText());
            if (val != null) field.setText(val);
        });
        box.add(field, BorderLayout.CENTER);
        box.add(btn, BorderLayout.EAST);
        return labeled(title, box);
    }

    private JPanel areaBox(String title, JTextArea area, boolean withPicker) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(UngDungPhongChu.SMALL);
        area.setFont(UngDungPhongChu.BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(new LineBorder(BORDER));
        sp.setPreferredSize(new Dimension(100, 56));
        p.add(lbl, BorderLayout.NORTH);
        if (withPicker) {
            JPanel wrap = new JPanel(new BorderLayout(2, 0));
            wrap.setOpaque(false);
            JButton btn = new JButton("…");
            btn.setFont(UngDungPhongChu.SMALL_BOLD);
            btn.setPreferredSize(new Dimension(24, 56));
            btn.setMargin(new Insets(0, 0, 0, 0));
            btn.addActionListener(e -> {
                String val = JOptionPane.showInputDialog(this, title + ":", area.getText());
                if (val != null) area.setText(val);
            });
            wrap.add(sp, BorderLayout.CENTER);
            wrap.add(btn, BorderLayout.EAST);
            p.add(wrap, BorderLayout.CENTER);
        } else {
            p.add(sp, BorderLayout.CENTER);
        }
        return p;
    }
}
