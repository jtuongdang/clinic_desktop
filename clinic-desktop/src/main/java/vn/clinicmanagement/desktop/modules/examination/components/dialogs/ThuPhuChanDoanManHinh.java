package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.desktop.modules.examination.components.tabs.DieuTriXuTriTheManHinh;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Giao diện màn hình: "ĐH1 - Chẩn Đoán Bệnh" (Bệnh kèm theo).
 * Mở khi bấm vào nút "Bệnh kèm Theo" bên phải danh sách ICD-10 trong tab Xử Trí.
 * Khớp 100% ảnh chụp thực tế media_1793374319612_82cb38e3.jpg:
 * - Tiêu đề cửa sổ: ĐH1 - Chẩn Đoán Bệnh --- Người đăng nhập: admin -- --- Version: 2022.7.15.3 ---
 * - Hàng 1:
 *   + [Mã bệnh - ICD10]: Ô nhập mã bệnh kèm nút [...]
 *   + [Tên bệnh - ICD10]: Ô nhập tên bệnh kèm nút [...]
 * - Hàng 2:
 *   + [Tên bệnh]: Ô nhập chi tiết tên bệnh trải rộng toàn chiều ngang
 * - Bảng danh mục bệnh kèm theo:
 *   + Cột: [STT] | [Mã bệnh - ICD10] | [Tên bệnh - ICD10]
 *   + Các dòng kẻ ô lưới chuẩn EHC
 * - Góc phải dưới cùng: Nút [💾 Lưu]
 */
public class ThuPhuChanDoanManHinh extends JPanel {

    /** Trường `onSaveCallback`: tải lưu `Callback`. */
    private final Consumer<List<String>> onSaveCallback;
    /** Trường `onCloseCallback`: tải đóng `Callback`. */
    private final Runnable onCloseCallback;

    /** Trường `txtMaIcd`: `txt` mã icd. */
    private JTextField txtMaIcd;
    /** Trường `txtTenIcd`: `txt` `Ten` icd. */
    private JTextField txtTenIcd;
    private JTextField txtTenBenhChiTiet;
    /** Trường `diagTable`: `diag` bảng. */
    private JTable diagTable;
    /** Trường `tableModel`: bảng mô hình. */
    private DefaultTableModel tableModel;

    public ThuPhuChanDoanManHinh(List<String> initialDiagnoses, Consumer<List<String>> onSaveCallback, Runnable onCloseCallback) {
        this.onSaveCallback = onSaveCallback;
        this.onCloseCallback = onCloseCallback;

        initUI(initialDiagnoses);
    }

    /** Hàm `initUI`: `init` giao diện. */
    private void initUI(List<String> initialDiagnoses) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new LineBorder(new Color(175, 185, 200), 1));
        setPreferredSize(new Dimension(640, 520));

        // 1. THANH TIÊU ĐỀ CỬA SỔ CHUẨN WINDOWS EHC
        JPanel windowTitleBar = new JPanel(new BorderLayout());
        windowTitleBar.setBackground(new Color(240, 243, 248));
        windowTitleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));
        windowTitleBar.setPreferredSize(new Dimension(640, 26));

        JPanel titleLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        titleLeft.setOpaque(false);

        JLabel lblRedCross = new JLabel("✚");
        lblRedCross.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRedCross.setForeground(new Color(180, 30, 30));

        JLabel lblTitle = new JLabel("ĐH1 - Chẩn Đoán Bệnh --- Người đăng nhập: admin -- --- Version: 2022.7.15.3 ---");
        lblTitle.setFont(UngDungPhongChu.SMALL_BOLD);
        lblTitle.setForeground(new Color(30, 40, 55));

        titleLeft.add(lblRedCross);
        titleLeft.add(lblTitle);
        windowTitleBar.add(titleLeft, BorderLayout.WEST);

        JPanel winBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        winBtns.setOpaque(false);

        JLabel btnClose = new JLabel("✕ ");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setForeground(new Color(100, 110, 125));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onCloseCallback != null) onCloseCallback.run();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                btnClose.setForeground(Color.RED);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnClose.setForeground(new Color(100, 110, 125));
            }
        });
        winBtns.add(btnClose);
        windowTitleBar.add(winBtns, BorderLayout.EAST);

        add(windowTitleBar, BorderLayout.NORTH);

        // 2. KHUNG NỘI DUNG CHÍNH (FORM NHẬP + BẢNG)
        JPanel contentPanel = new JPanel(new BorderLayout(0, 6));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(8, 10, 8, 10));

        // Form nhập thông tin chẩn đoán bên trên
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);

        // Hàng 1: Mã bệnh - ICD10 (kèm nút ...) & Tên bệnh - ICD10 (kèm nút ...)
        JPanel row1 = new JPanel(new GridBagLayout());
        row1.setBackground(Color.WHITE);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 4, 6);

        // Cột 1: Mã bệnh - ICD10
        JPanel pnlMa = new JPanel(new BorderLayout(0, 2));
        pnlMa.setBackground(Color.WHITE);
        JLabel lblMa = new JLabel("Mã bệnh - ICD10");
        lblMa.setFont(UngDungPhongChu.SMALL);
        lblMa.setForeground(new Color(30, 35, 45));

        JPanel codeBox = new JPanel(new BorderLayout());
        codeBox.setBackground(Color.WHITE);
        codeBox.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        codeBox.setPreferredSize(new Dimension(100, 24));

        txtMaIcd = new JTextField();
        txtMaIcd.setFont(UngDungPhongChu.SMALL_BOLD);
        txtMaIcd.setBorder(new EmptyBorder(2, 4, 2, 2));
        txtMaIcd.addActionListener(e -> openIcdLookup());

        JButton btnDotsMa = new JButton("...");
        btnDotsMa.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnDotsMa.setPreferredSize(new Dimension(20, 22));
        btnDotsMa.setBackground(new Color(245, 247, 250));
        btnDotsMa.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));
        btnDotsMa.setFocusPainted(false);
        btnDotsMa.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDotsMa.addActionListener(e -> openIcdLookup());

        codeBox.add(txtMaIcd, BorderLayout.CENTER);
        codeBox.add(btnDotsMa, BorderLayout.EAST);

        pnlMa.add(lblMa, BorderLayout.NORTH);
        pnlMa.add(codeBox, BorderLayout.CENTER);

        c.gridx = 0; c.gridy = 0; c.weightx = 0.22;
        row1.add(pnlMa, c);

        // Cột 2: Tên bệnh - ICD10
        JPanel pnlTen = new JPanel(new BorderLayout(0, 2));
        pnlTen.setBackground(Color.WHITE);
        JLabel lblTen = new JLabel("Tên bệnh - ICD10");
        lblTen.setFont(UngDungPhongChu.SMALL);
        lblTen.setForeground(new Color(30, 35, 45));

        JPanel nameBox = new JPanel(new BorderLayout());
        nameBox.setBackground(Color.WHITE);
        nameBox.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        nameBox.setPreferredSize(new Dimension(480, 24));

        txtTenIcd = new JTextField();
        txtTenIcd.setFont(UngDungPhongChu.SMALL);
        txtTenIcd.setBorder(new EmptyBorder(2, 4, 2, 2));
        txtTenIcd.addActionListener(e -> addDiagnosisToTable());

        JButton btnDotsTen = new JButton("...");
        btnDotsTen.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnDotsTen.setPreferredSize(new Dimension(20, 22));
        btnDotsTen.setBackground(new Color(245, 247, 250));
        btnDotsTen.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));
        btnDotsTen.setFocusPainted(false);
        btnDotsTen.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDotsTen.addActionListener(e -> openIcdLookup());

        nameBox.add(txtTenIcd, BorderLayout.CENTER);
        nameBox.add(btnDotsTen, BorderLayout.EAST);

        pnlTen.add(lblTen, BorderLayout.NORTH);
        pnlTen.add(nameBox, BorderLayout.CENTER);

        c.gridx = 1; c.gridy = 0; c.weightx = 0.78; c.insets = new Insets(0, 0, 4, 0);
        row1.add(pnlTen, c);

        formPanel.add(row1);

        // Hàng 2: Tên bệnh (trải rộng)
        JPanel row2 = new JPanel(new BorderLayout(0, 2));
        row2.setBackground(Color.WHITE);
        row2.setBorder(new EmptyBorder(0, 0, 6, 0));

        JLabel lblTenChiTiet = new JLabel("Tên bệnh");
        lblTenChiTiet.setFont(UngDungPhongChu.SMALL);
        lblTenChiTiet.setForeground(new Color(30, 35, 45));

        txtTenBenhChiTiet = new JTextField();
        txtTenBenhChiTiet.setFont(UngDungPhongChu.SMALL);
        txtTenBenhChiTiet.setPreferredSize(new Dimension(600, 24));
        txtTenBenhChiTiet.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        txtTenBenhChiTiet.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addDiagnosisToTable();
                }
            }
        });

        row2.add(lblTenChiTiet, BorderLayout.NORTH);
        row2.add(txtTenBenhChiTiet, BorderLayout.CENTER);

        formPanel.add(row2);
        contentPanel.add(formPanel, BorderLayout.NORTH);

        // Bảng danh mục bệnh kèm theo khớp ảnh mẫu
        String[] cols = {"STT", "Mã bệnh - ICD10", "Tên bệnh - ICD10"};
        tableModel = new DefaultTableModel(cols, 0) {
            /** Hàm `isCellEditable`: là `Cell` `Editable`. */
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        // Nạp dữ liệu ban đầu nếu có (ví dụ A02 - Nhiễm salmonella khác)
        if (initialDiagnoses != null && !initialDiagnoses.isEmpty()) {
            int stt = 1;
            for (String diag : initialDiagnoses) {
                String code = "";
                String name = diag;
                if (diag.contains(" - ")) {
                    String[] parts = diag.split(" - ", 2);
                    code = parts[0].trim();
                    name = parts[1].trim();
                }
                tableModel.addRow(new Object[]{String.valueOf(stt++), code, name});
            }
        }

        diagTable = new JTable(tableModel);
        diagTable.setFont(UngDungPhongChu.SMALL);
        diagTable.setRowHeight(21);
        diagTable.setShowGrid(true);
        diagTable.setGridColor(new Color(230, 235, 243));
        diagTable.setSelectionBackground(new Color(214, 232, 255));
        diagTable.setSelectionForeground(new Color(15, 25, 40));

        JTableHeader header = diagTable.getTableHeader();
        header.setFont(UngDungPhongChu.SMALL_BOLD);
        header.setBackground(new Color(242, 246, 252));
        header.setForeground(new Color(30, 35, 45));
        header.setPreferredSize(new Dimension(600, 24));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));

        diagTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        diagTable.getColumnModel().getColumn(0).setMaxWidth(60);
        diagTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        diagTable.getColumnModel().getColumn(1).setMaxWidth(150);
        diagTable.getColumnModel().getColumn(2).setPreferredWidth(440);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        diagTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        diagTable.getColumnModel().getColumn(1).setCellRenderer(centerRender);

        JScrollPane scrollPane = new JScrollPane(diagTable);
        scrollPane.setBorder(new LineBorder(new Color(195, 205, 220), 1));
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);

        // 3. CHÂN TRANG: NÚT [💾 Lưu]
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        footer.setBackground(new Color(248, 250, 253));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 218, 228)));

        JButton btnSave = new JButton("Lưu", new DieuTriXuTriTheManHinh.SaveDiskIcon());
        btnSave.setFont(UngDungPhongChu.SMALL_BOLD);
        btnSave.setForeground(new Color(25, 30, 40));
        btnSave.setBackground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(3, 10, 3, 10)
        ));
        btnSave.addActionListener(e -> handleSave());

        footer.add(btnSave);
        add(footer, BorderLayout.SOUTH);
    }

    /** Hàm `openIcdLookup`: mở icd `Lookup`. */
    private void openIcdLookup() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        String search = txtMaIcd.getText().trim();
        if (search.isEmpty()) {
            search = txtTenIcd.getText().trim();
        }

        ICDDanhMucLuaChonHopThoai dlg = new ICDDanhMucLuaChonHopThoai(owner, search, (code, name) -> {
            txtMaIcd.setText(code);
            txtTenIcd.setText(name);
            txtTenBenhChiTiet.setText(name);
            addDiagnosisToTable();
        });
        dlg.setVisible(true);
    }

    /** Hàm `addDiagnosisToTable`: thêm chandoan den bảng. */
    private void addDiagnosisToTable() {
        String code = txtMaIcd.getText().trim();
        String name = txtTenBenhChiTiet.getText().trim();
        if (name.isEmpty()) {
            name = txtTenIcd.getText().trim();
        }

        if (code.isEmpty() && name.isEmpty()) {
            return;
        }

        // Kiểm tra xem đã tồn tại chưa
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            Object c = tableModel.getValueAt(r, 1);
            if (code.equalsIgnoreCase(c != null ? c.toString() : "")) {
                diagTable.setRowSelectionInterval(r, r);
                return;
            }
        }

        int nextStt = tableModel.getRowCount() + 1;
        tableModel.addRow(new Object[]{String.valueOf(nextStt), code, name});

        // Xóa các ô nhập để sẵn sàng nhập mã tiếp theo
        txtMaIcd.setText("");
        txtTenIcd.setText("");
        txtTenBenhChiTiet.setText("");
        txtMaIcd.requestFocusInWindow();
    }

    /** Hàm `handleSave`: `handle` lưu. */
    private void handleSave() {
        List<String> list = new ArrayList<>();
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            String code = tableModel.getValueAt(r, 1).toString();
            String name = tableModel.getValueAt(r, 2).toString();
            if (!code.isEmpty()) {
                list.add(code + " - " + name);
            } else {
                list.add(name);
            }
        }

        if (onSaveCallback != null) {
            onSaveCallback.accept(list);
        }

        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    /** Hàm `getDiagTable`: lấy `Diag` bảng. */
    public JTable getDiagTable() {
        return diagTable;
    }
}
