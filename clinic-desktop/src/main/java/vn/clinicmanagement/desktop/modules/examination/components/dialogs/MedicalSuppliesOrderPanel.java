package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.modules.examination.ExaminationPanel.AdminPatientItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Giao diện màn hình: "ĐHC - Chỉ Định Vật Tư"
 * Màn hình sau khi nhập số lượng và nhấn [✓ OK (Ctrl+A)].
 * Khớp 100% chi tiết trong ảnh chụp mẫu thực tế:
 * - Hàng chẩn đoán kèm các nút duyệt [...]
 * - Khung nhập liệu nhanh: "Tên vật tư/Mã:" kèm nút [...]
 * - Bảng dữ liệu vật tư 7 cột: dòng 1 (22VTNT023 / Kho: 0) được chọn sáng toàn bộ trên nền xanh đậm
 * - Thanh trạng thái dưới bảng: "* Bơm cho ăn 5ml (1)"
 * - Thanh tài chính chân trang cập nhật chính xác: Tổng chi phí: 1,023,160 | BHYT: 44,160 | Bệnh nhân: 979,000 | Còn dư: 9,021,000
 */
public class MedicalSuppliesOrderPanel extends JPanel {

    private final Window ownerWindow;
    private final Runnable onCloseCallback;
    private JTable suppliesTable;
    private DefaultTableModel tableModel;
    private JPanel orderStatusBar;
    private JLabel lblTotalCostVal;
    private JLabel lblBhytCostVal;

    private AdminPatientItem patient;
    private Consumer<List<Object[]>> onSuppliesSavedCallback;
    private boolean suppressDialogs = false;

    private JLabel lblPatientLine1;
    private JLabel lblPatientLine2;
    private JLabel lblPatientLine3;
    private JLabel lblBhyt;
    private JComboBox<String> cbOrderDate;
    private JComboBox<String> cbIcd;
    private JComboBox<String> cbDiagnosisDesc;
    private JTextField txtQty;
    private JButton btnSave;
    private JButton btnSaveAndClose;

    public MedicalSuppliesOrderPanel() {
        this((Runnable) null);
    }

    public MedicalSuppliesOrderPanel(Runnable onCloseCallback) {
        this.ownerWindow = null;
        this.onCloseCallback = onCloseCallback;
        initPanel();
    }

    public MedicalSuppliesOrderPanel(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
        this.onCloseCallback = () -> {
            if (ownerWindow != null) {
                ownerWindow.dispose();
            }
        };
        initPanel();
    }

    private void initPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. THANH TIÊU ĐỀ PHỤ + BANNER THÔNG TIN BỆNH NHÂN GÓC PHẢI
        add(buildHeaderSection(), BorderLayout.NORTH);

        // 2. PHẦN THÂN CHÍNH: DẢI TAB + SPLIT PANE (CÂY DANH MỤC & BẢNG VẬT TƯ)
        add(buildBodySection(), BorderLayout.CENTER);

        // 3. THANH TÀI CHÍNH & NÚT LƯU CHÂN TRANG
        add(buildFooterSection(), BorderLayout.SOUTH);
    }

    private JPanel buildHeaderSection() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);

        // Hàng 1: Sub-Toolbar các thao tác + Banner bệnh nhân góc phải
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(new Color(246, 248, 252));
        topRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));
        topRow.setPreferredSize(new Dimension(1000, 52));

        // Nút công cụ bên trái
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        leftActions.setOpaque(false);

        leftActions.add(createSubToolbarButton("Menu", "menu", false));
        leftActions.add(createSubToolbarButton("Đơn Vật Tư Mẫu", "template", true));
        leftActions.add(createSubToolbarButton("Ký Vật Tư", "sign", false));
        leftActions.add(createSubToolbarButton("Đơn Vật Tư Cũ", "history", false));

        topRow.add(leftActions, BorderLayout.WEST);

        // Banner bệnh nhân góc phải (màu đỏ sẫm)
        JPanel rightBanner = new JPanel(new BorderLayout(8, 0));
        rightBanner.setOpaque(false);
        rightBanner.setBorder(new EmptyBorder(2, 6, 2, 12));

        JPanel textPanel = new JPanel(new GridLayout(3, 1, 0, 0));
        textPanel.setOpaque(false);

        lblPatientLine1 = new JLabel("NGUYỄN THỊ BÌNH AN | 22000308 | 1995 (23 tuổi) | Nữ", SwingConstants.RIGHT);
        lblPatientLine1.setFont(AppFonts.SMALL_BOLD);
        lblPatientLine1.setForeground(new Color(125, 20, 20));

        lblPatientLine2 = new JLabel("Mã VP : 214332 | DN-4-01-8337377888 | Còn 867 ngày", SwingConstants.RIGHT);
        lblPatientLine2.setFont(AppFonts.SMALL);
        lblPatientLine2.setForeground(new Color(125, 20, 20));

        lblPatientLine3 = new JLabel("Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh", SwingConstants.RIGHT);
        lblPatientLine3.setFont(AppFonts.SMALL);
        lblPatientLine3.setForeground(new Color(125, 20, 20));

        textPanel.add(lblPatientLine1);
        textPanel.add(lblPatientLine2);
        textPanel.add(lblPatientLine3);

        lblBhyt = new JLabel("<html><center><font color='#800000'><b>BHYT</b></font><br><font color='#0044CC'><b>80%</b></font></center></html>", SwingConstants.CENTER);
        lblBhyt.setFont(AppFonts.SMALL_BOLD);
        lblBhyt.setBorder(new EmptyBorder(0, 8, 0, 4));

        rightBanner.add(textPanel, BorderLayout.CENTER);
        rightBanner.add(lblBhyt, BorderLayout.EAST);

        topRow.add(rightBanner, BorderLayout.EAST);
        headerPanel.add(topRow);

        // Hàng 2: Ngày y lệnh & Chẩn đoán bệnh kèm các nút duyệt [...]
        JPanel contextRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        contextRow.setBackground(new Color(248, 250, 253));
        contextRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 222, 232)));

        JLabel lblOrderDate = new JLabel("Ngày y lệnh:");
        lblOrderDate.setFont(AppFonts.BODY);

        cbOrderDate = new JComboBox<>(new String[]{"15:15 19/08/2022"});
        cbOrderDate.setFont(AppFonts.BODY);
        cbOrderDate.setPreferredSize(new Dimension(150, 24));
        cbOrderDate.setBackground(Color.WHITE);

        JLabel lblDiagnosis = new JLabel("Chẩn đoán:");
        lblDiagnosis.setFont(AppFonts.BODY);

        cbIcd = new JComboBox<>(new String[]{"A02.0"});
        cbIcd.setFont(AppFonts.BODY);
        cbIcd.setPreferredSize(new Dimension(85, 24));
        cbIcd.setBackground(Color.WHITE);

        JButton btnBrowseIcd = createMiniBrowseButton();

        cbDiagnosisDesc = new JComboBox<>(new String[]{"Viêm ruột do Salmonella"});
        cbDiagnosisDesc.setFont(AppFonts.BODY);
        cbDiagnosisDesc.setPreferredSize(new Dimension(420, 24));
        cbDiagnosisDesc.setBackground(Color.WHITE);

        JButton btnBrowseDesc = createMiniBrowseButton();

        contextRow.add(lblOrderDate);
        contextRow.add(cbOrderDate);
        contextRow.add(Box.createHorizontalStrut(15));
        contextRow.add(lblDiagnosis);
        contextRow.add(cbIcd);
        contextRow.add(btnBrowseIcd);
        contextRow.add(cbDiagnosisDesc);
        contextRow.add(btnBrowseDesc);

        headerPanel.add(contextRow);
        return headerPanel;
    }

    private JPanel buildBodySection() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);

        // Dải Tab: Danh Sách Vật Tư | Phiếu Chỉ Định
        JTabbedPane tabPane = createStyledTabbedPane();
        tabPane.addTab("Danh Sách Vật Tư", buildSuppliesSplitView());
        tabPane.addTab("Phiếu Chỉ Định", createPlaceholderTab("Danh sách các phiếu chỉ định vật tư đã kê"));

        body.add(tabPane, BorderLayout.CENTER);
        return body;
    }

    private JComponent buildSuppliesSplitView() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(185);
        splitPane.setDividerSize(3);
        splitPane.setBorder(null);

        // --- CỘT TRÁI: CÂY DANH MỤC VẬT TƯ (TTVT Phòng khám) ---
        JPanel treeContainer = new JPanel(new BorderLayout());
        treeContainer.setBackground(Color.WHITE);
        treeContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(205, 214, 226)));

        JLabel lblTreeHeader = new JLabel("TTVT Phòng khám");
        lblTreeHeader.setFont(AppFonts.BODY_BOLD);
        lblTreeHeader.setForeground(new Color(138, 21, 21));
        lblTreeHeader.setBorder(new EmptyBorder(4, 6, 4, 4));
        treeContainer.add(lblTreeHeader, BorderLayout.NORTH);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Tất cả vật tư (2)");
        DefaultMutableTreeNode vatTuNode = new DefaultMutableTreeNode("Vật tư (2)");
        vatTuNode.add(new DefaultMutableTreeNode("Vật tư y tế (2)"));
        root.add(vatTuNode);

        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setFont(AppFonts.BODY);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        expandAllNodes(tree, 0, tree.getRowCount());

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                JLabel l = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                l.setFont(AppFonts.BODY);
                if (sel || row == 0) {
                    l.setOpaque(true);
                    l.setBackground(new Color(0, 120, 215)); // Màu xanh dương đậm như trong ảnh
                    l.setForeground(Color.WHITE);
                } else {
                    l.setOpaque(false);
                    l.setForeground(Color.BLACK);
                }
                setIcon(null);
                return l;
            }
        });

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setBorder(BorderFactory.createEmptyBorder());
        treeScroll.getViewport().setBackground(Color.WHITE);
        treeContainer.add(treeScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(treeContainer);

        // --- CỘT PHẢI: KHUNG NHẬP LIỆU NHANH + BẢNG DỮ LIỆU VẬT TƯ ---
        JPanel rightContent = new JPanel(new BorderLayout());
        rightContent.setBackground(Color.WHITE);

        // Khung nhập liệu nhanh
        JPanel inputBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
        inputBar.setBackground(new Color(248, 250, 253));
        inputBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 218, 228)));

        JLabel lblName = new JLabel("Tên vật tư/Mã:");
        lblName.setFont(AppFonts.BODY);

        JComboBox<String> cbItemName = new JComboBox<>(new String[]{"Bơm cho ăn 5ml"});
        cbItemName.setFont(AppFonts.BODY);
        cbItemName.setPreferredSize(new Dimension(240, 24));
        cbItemName.setBackground(Color.WHITE);
        cbItemName.setEditable(true);

        JButton btnBrowseItem = createMiniBrowseButton();

        JLabel lblUnit = new JLabel("Đơn vị:");
        lblUnit.setFont(AppFonts.BODY);

        JTextField txtUnit = new JTextField("Cái");
        txtUnit.setFont(AppFonts.BODY);
        txtUnit.setPreferredSize(new Dimension(50, 24));
        txtUnit.setEditable(false);
        txtUnit.setBackground(Color.WHITE);
        txtUnit.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblStore = new JLabel("Tên kho:");
        lblStore.setFont(AppFonts.BODY);

        JTextField txtStore = new JTextField("Cái");
        txtStore.setFont(AppFonts.BODY);
        txtStore.setPreferredSize(new Dimension(50, 24));
        txtStore.setEditable(false);
        txtStore.setBackground(Color.WHITE);
        txtStore.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblQty = new JLabel("Số lượng:");
        lblQty.setFont(AppFonts.BODY);

        txtQty = new JTextField("1");
        txtQty.setFont(AppFonts.BODY_BOLD);
        txtQty.setPreferredSize(new Dimension(50, 24));
        txtQty.setHorizontalAlignment(SwingConstants.CENTER);

        JButton btnOk = new JButton(" OK (Ctrl+A)", new CheckmarkBlueIcon());
        btnOk.setFont(AppFonts.BODY_BOLD);
        btnOk.setBackground(Color.WHITE);
        btnOk.setFocusPainted(false);
        btnOk.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOk.setPreferredSize(new Dimension(125, 24));
        btnOk.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(180, 190, 205), 1),
                new EmptyBorder(1, 4, 1, 4)
        ));

        // Xử lý sự kiện khi nhấn [✓ OK (Ctrl+A)]
        btnOk.addActionListener(e -> applyOrderSelection());

        inputBar.add(lblName);
        inputBar.add(cbItemName);
        inputBar.add(btnBrowseItem);
        inputBar.add(lblUnit);
        inputBar.add(txtUnit);
        inputBar.add(lblStore);
        inputBar.add(txtStore);
        inputBar.add(lblQty);
        inputBar.add(txtQty);
        inputBar.add(btnOk);

        rightContent.add(inputBar, BorderLayout.NORTH);

        // Bảng dữ liệu vật tư 7 cột
        String[] cols = {
                "Mã Vật Tư",
                "Tên Vật Tư",
                "Đơn Vị",
                "Tên Kho",
                "Giá BHYT",
                "Giá viện phí",
                "Giá dịch vụ"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        // 3 dòng vật tư y tế khớp 100% ảnh chụp sau khi nhấn OK
        tableModel.addRow(new Object[]{
                "VT026",
                "Bơm cho ăn 5ml (BT5-KT-VN / Bơm tiêm sử dụng một lần VINAHAMKOOK loại 5ml)",
                "Cái",
                "0",
                "735",
                "735",
                "735"
        });

        tableModel.addRow(new Object[]{
                "22VTNT023",
                "Bơm cho ăn 5ml (BT5-KT-VN / Bơm tiêm sử dụng một lần VINAHAMKOOK loại 5ml)",
                "Cái",
                "0",
                "735",
                "735",
                "735"
        });

        tableModel.addRow(new Object[]{
                "22VTNT023",
                "Bơm cho ăn 5ml (BT5-KT-VN / Bơm tiêm sử dụng một lần VINAHAMKOOK loại 5ml)",
                "Cái",
                "7",
                "735",
                "735",
                "735"
        });

        // Thêm các dòng trống phía dưới để tạo lưới chuẩn HIS
        for (int i = 0; i < 15; i++) {
            tableModel.addRow(new Object[]{"", "", "", "", "", "", ""});
        }

        suppliesTable = new JTable(tableModel);
        suppliesTable.setRowHeight(21);
        suppliesTable.setFont(AppFonts.BODY);
        suppliesTable.setShowGrid(true);
        suppliesTable.setGridColor(new Color(225, 230, 238));
        suppliesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Sau khi nhấn OK: dòng 1 (22VTNT023 / Kho: 0) được chọn sẵn
        suppliesTable.setRowSelectionInterval(1, 1);

        JTableHeader th = suppliesTable.getTableHeader();
        th.setFont(AppFonts.SMALL_BOLD);
        th.setPreferredSize(new Dimension(800, 24));
        th.setBackground(new Color(242, 246, 252));
        th.setForeground(new Color(25, 30, 40));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 210, 222)));

        suppliesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        suppliesTable.getColumnModel().getColumn(0).setPreferredWidth(85);
        suppliesTable.getColumnModel().getColumn(1).setPreferredWidth(520);
        suppliesTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        suppliesTable.getColumnModel().getColumn(3).setPreferredWidth(65);
        suppliesTable.getColumnModel().getColumn(4).setPreferredWidth(75);
        suppliesTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        suppliesTable.getColumnModel().getColumn(6).setPreferredWidth(80);

        // Highlight dòng 1 màu xanh dương đậm chuẩn ảnh chụp sau khi bấm OK
        suppliesTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setOpaque(true);
                if (row == 1) {
                    l.setBackground(new Color(0, 120, 215)); // Màu xanh dương đậm chuẩn EHC
                    l.setForeground(Color.WHITE);
                } else {
                    l.setBackground(Color.WHITE);
                    l.setForeground(Color.BLACK);
                }
                if (column == 2 || column == 3) {
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (column >= 4) {
                    l.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return l;
            }
        });

        JScrollPane tableScroll = new JScrollPane(suppliesTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);

        rightContent.add(tableScroll, BorderLayout.CENTER);

        // --- THANH TRẠNG THÁI CHỈ ĐỊNH DƯỚI BẢNG: "* Bơm cho ăn 5ml (1)" ---
        orderStatusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        orderStatusBar.setBackground(Color.WHITE);
        orderStatusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(225, 230, 238)));

        JLabel lblOrderAsterisk = new JLabel("*");
        lblOrderAsterisk.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblOrderAsterisk.setForeground(new Color(0, 102, 204)); // Dấu hoa thị xanh dương

        JLabel lblOrderText = new JLabel("Bơm cho ăn 5ml (1)");
        lblOrderText.setFont(AppFonts.BODY_BOLD);
        lblOrderText.setForeground(new Color(25, 25, 25));

        orderStatusBar.add(lblOrderAsterisk);
        orderStatusBar.add(lblOrderText);

        rightContent.add(orderStatusBar, BorderLayout.SOUTH);

        splitPane.setRightComponent(rightContent);
        return splitPane;
    }

    private JPanel buildFooterSection() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(245, 248, 252));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(205, 214, 226)),
                new EmptyBorder(4, 10, 4, 10)
        ));
        footer.setPreferredSize(new Dimension(1000, 48));

        // Bảng số liệu tài chính bên trái (2 hàng)
        JPanel statsPanel = new JPanel(new GridLayout(2, 4, 15, 2));
        statsPanel.setOpaque(false);

        // Hàng 1 (Đỏ đậm): Tổng chi phí: 1,023,160 | BHYT: 44,160 | Nguồn khác: 0 | Bệnh nhân: 979,000
        lblTotalCostVal = new JLabel("1,023,160");
        lblTotalCostVal.setFont(AppFonts.BODY_BOLD);
        lblTotalCostVal.setForeground(new Color(185, 25, 25));

        lblBhytCostVal = new JLabel("44,160");
        lblBhytCostVal.setFont(AppFonts.BODY_BOLD);
        lblBhytCostVal.setForeground(new Color(185, 25, 25));

        statsPanel.add(wrapMetric("Tổng chi phí:", lblTotalCostVal));
        statsPanel.add(wrapMetric("BHYT:", lblBhytCostVal));
        statsPanel.add(createMetricLabel("Nguồn khác:", "0", new Color(185, 25, 25)));
        statsPanel.add(createMetricLabel("Bệnh nhân:", "979,000", new Color(185, 25, 25)));

        // Hàng 2 (Xanh tím than): Tạm ứng: 10,000,000 | Đã thu: 0 | Miễn giảm: 0 | Còn dư: 9,021,000
        statsPanel.add(createMetricLabel("Tạm ứng:", "10,000,000", new Color(0, 45, 140)));
        statsPanel.add(createMetricLabel("Đã thu:", "0", new Color(0, 45, 140)));
        statsPanel.add(createMetricLabel("Miễn giảm:", "0", new Color(0, 45, 140)));
        statsPanel.add(createMetricLabel("Còn dư:", "9,021,000", new Color(0, 45, 140)));

        footer.add(statsPanel, BorderLayout.CENTER);

        // Cụm nút Lưu bên phải
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        rightButtons.setOpaque(false);

        btnSave = new JButton(" Lưu (Ctrl + S)", new FloppyDiskIcon(new Color(125, 45, 135)));
        btnSave.setFont(AppFonts.BODY_BOLD);
        btnSave.setBackground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.setPreferredSize(new Dimension(140, 28));
        btnSave.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 208), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        btnSave.addActionListener(e -> executeSave());

        btnSaveAndClose = new JButton(" Lưu + Đóng", new FloppyDiskIcon(new Color(40, 145, 75)));
        btnSaveAndClose.setFont(AppFonts.BODY_BOLD);
        btnSaveAndClose.setBackground(Color.WHITE);
        btnSaveAndClose.setFocusPainted(false);
        btnSaveAndClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSaveAndClose.setPreferredSize(new Dimension(115, 28));
        btnSaveAndClose.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 208), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        btnSaveAndClose.addActionListener(e -> {
            executeSave();
            if (onCloseCallback != null) {
                onCloseCallback.run();
            } else if (ownerWindow != null) {
                ownerWindow.dispose();
            }
        });

        rightButtons.add(btnSave);
        rightButtons.add(btnSaveAndClose);

        footer.add(rightButtons, BorderLayout.EAST);
        return footer;
    }

    public void setPatient(AdminPatientItem patient) {
        this.patient = patient;
        if (patient == null) return;
        if (lblPatientLine1 != null) {
            lblPatientLine1.setText(patient.name + " | " + patient.code + " | " + patient.birthYear + " (" + patient.bannerAge + ") | " + patient.gender);
        }
        if (lblPatientLine2 != null) {
            lblPatientLine2.setText("Mã VP : " + (patient.invoiceCode != null ? patient.invoiceCode : "") + " | " + (patient.requestType != null ? patient.requestType : ""));
        }
        if (lblPatientLine3 != null) {
            lblPatientLine3.setText(patient.bannerAddress != null ? patient.bannerAddress : (patient.address != null ? patient.address : ""));
        }
        boolean isBhyt = patient.requestType != null && (patient.requestType.contains("BHYT") || patient.requestType.contains("hiểm"));
        if (lblBhyt != null) {
            lblBhyt.setText(isBhyt
                    ? "<html><center><font color='#800000'><b>BHYT</b></font><br><font color='#0044CC'><b>80%</b></font></center></html>"
                    : "<html><center><font color='#800000'><b>Viện<br>phí</b></font></center></html>");
        }
        if (cbDiagnosisDesc != null && patient.diagnosis != null && !patient.diagnosis.isBlank()) {
            cbDiagnosisDesc.removeAllItems();
            cbDiagnosisDesc.addItem(patient.diagnosis);
        }
    }

    public void setOnSuppliesSavedCallback(Consumer<List<Object[]>> callback) {
        this.onSuppliesSavedCallback = callback;
    }

    public void setSuppressDialogs(boolean suppressDialogs) {
        this.suppressDialogs = suppressDialogs;
    }

    public boolean isSuppressDialogs() {
        return suppressDialogs;
    }

    private boolean isModalDialogAllowed() {
        if (suppressDialogs || GraphicsEnvironment.isHeadless()) return false;
        String surefire = System.getProperty("surefire.test.class.path");
        return surefire == null || surefire.isBlank();
    }

    public void executeSave() {
        String name = patient != null && patient.name != null ? patient.name : "NGUYỄN THỊ BÌNH AN";
        String code = patient != null && patient.code != null ? patient.code : "22000308";

        List<Object[]> savedItems = new ArrayList<>();
        int selectedRow = suppliesTable != null ? suppliesTable.getSelectedRow() : -1;
        String firstItemDesc = "Bơm cho ăn 5ml (SL: 1) - Kho: 0";

        if (tableModel != null) {
            int stt = 1;
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                Object codeVal = tableModel.getValueAt(r, 0);
                Object nameVal = tableModel.getValueAt(r, 1);
                Object unitVal = tableModel.getValueAt(r, 2);
                if (codeVal != null && !codeVal.toString().trim().isEmpty() &&
                    nameVal != null && !nameVal.toString().trim().isEmpty()) {
                    String qty = (r == selectedRow && txtQty != null && !txtQty.getText().isBlank()) ? txtQty.getText().trim() : "1";
                    savedItems.add(new Object[]{String.valueOf(stt++), codeVal.toString().trim(), nameVal.toString().trim(), unitVal != null ? unitVal.toString().trim() : "Cái", qty});
                }
            }
        }

        if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
            Object nameVal = tableModel.getValueAt(selectedRow, 1);
            Object storeVal = tableModel.getValueAt(selectedRow, 3);
            if (nameVal != null && !nameVal.toString().isBlank()) {
                String shortName = nameVal.toString();
                if (shortName.contains("(")) {
                    shortName = shortName.substring(0, shortName.indexOf('(')).trim();
                }
                String qty = txtQty != null ? txtQty.getText().trim() : "1";
                firstItemDesc = shortName + " (SL: " + qty + ") - Kho: " + (storeVal != null ? storeVal : "0");
            }
        }

        String totalCost = (lblTotalCostVal != null && lblTotalCostVal.getText() != null) ? lblTotalCostVal.getText() : "1,023,160";

        if (onSuppliesSavedCallback != null) {
            onSuppliesSavedCallback.accept(savedItems);
        }

        if (isModalDialogAllowed()) {
            JOptionPane.showMessageDialog(this,
                    "Đã lưu thành công đơn chỉ định vật tư y tế!\n" +
                    "▶ Bệnh nhân: " + name + " | " + code + "\n" +
                    "▶ Vật tư đã kê: " + firstItemDesc + "\n" +
                    "▶ Tổng chi phí: " + totalCost + " đ",
                    "Lưu Chỉ Định Vật Tư", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public JButton getBtnSave() { return btnSave; }
    public JButton getBtnSaveAndClose() { return btnSaveAndClose; }
    public JTable getSuppliesTable() { return suppliesTable; }
    public DefaultTableModel getTableModel() { return tableModel; }
    public AdminPatientItem getPatient() { return patient; }
    public JLabel getLblTotalCostVal() { return lblTotalCostVal; }
    public JTextField getTxtQty() { return txtQty; }

    private void applyOrderSelection() {
        if (suppliesTable != null && suppliesTable.getRowCount() > 1) {
            suppliesTable.setRowSelectionInterval(1, 1);
        }
        if (orderStatusBar != null) {
            orderStatusBar.setVisible(true);
        }
        if (lblTotalCostVal != null) {
            lblTotalCostVal.setText("1,023,160");
        }
        if (lblBhytCostVal != null) {
            lblBhytCostVal.setText("44,160");
        }
    }

    private JPanel wrapMetric(String title, JLabel valLabel) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(AppFonts.SMALL);
        lblTitle.setForeground(new Color(45, 50, 60));
        p.add(lblTitle);
        p.add(valLabel);
        return p;
    }

    private JPanel createMetricLabel(String title, String val, Color valColor) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(AppFonts.SMALL);
        lblTitle.setForeground(new Color(45, 50, 60));

        JLabel lblVal = new JLabel(val);
        lblVal.setFont(AppFonts.BODY_BOLD);
        lblVal.setForeground(valColor);

        p.add(lblTitle);
        p.add(lblVal);
        return p;
    }

    private JButton createMiniBrowseButton() {
        JButton btn = new JButton("...");
        btn.setFont(AppFonts.SMALL_BOLD);
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setBackground(new Color(242, 244, 248));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new LineBorder(new Color(185, 195, 208), 1));
        return btn;
    }

    private JButton createSubToolbarButton(String text, String iconType, boolean hasDropdown) {
        String label = hasDropdown ? text + " ▾" : text;
        JButton btn = new JButton(label, new SuppliesToolbarIcon(iconType));
        btn.setFont(AppFonts.BODY);
        btn.setForeground(new Color(20, 25, 35));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(4, 10, 4, 10));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(230, 238, 250));
                btn.setOpaque(true);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setOpaque(false);
            }
        });

        return btn;
    }

    private JTabbedPane createStyledTabbedPane() {
        JTabbedPane tp = new JTabbedPane();
        tp.setFont(AppFonts.BODY_BOLD);
        tp.setBackground(new Color(214, 228, 245));
        tp.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(2, 4, 0, 4);
                contentBorderInsets = new Insets(1, 0, 0, 0);
                tabInsets = new Insets(4, 12, 4, 12);
            }

            @Override
            protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
                g.setColor(new Color(214, 228, 245));
                g.fillRect(0, 0, tabPane.getWidth(), calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight));
                super.paintTabArea(g, tabPlacement, selectedIndex);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                if (isSelected) {
                    g2.setColor(new Color(245, 248, 253));
                } else {
                    g2.setColor(new Color(204, 220, 240));
                }
                g2.fillRoundRect(x + 1, y + 1, w - 2, h + 2, 4, 4);
                g2.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                g.setColor(isSelected ? new Color(150, 175, 205) : new Color(175, 195, 220));
                g.drawRoundRect(x + 1, y + 1, w - 3, h + 2, 4, 4);
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                int th = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                g.setColor(new Color(165, 185, 212));
                g.drawLine(0, th, tabPane.getWidth(), th);
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {}
        });
        return tp;
    }

    private JPanel createPlaceholderTab(String desc) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        JLabel lbl = new JLabel(desc, SwingConstants.CENTER);
        lbl.setFont(AppFonts.BODY);
        lbl.setForeground(Color.GRAY);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    private void expandAllNodes(JTree tree, int startingIndex, int rowCount) {
        for (int i = startingIndex; i < rowCount; ++i) {
            tree.expandRow(i);
        }
        if (tree.getRowCount() != rowCount) {
            expandAllNodes(tree, rowCount, tree.getRowCount());
        }
    }

    // =========================================================================
    // VECTOR ICONS (KHÔNG DÙNG ẢNH BITMAP HAY EMOJI)
    // =========================================================================

    private static class SuppliesToolbarIcon implements Icon {
        private final String type;
        SuppliesToolbarIcon(String type) { this.type = type; }
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if ("menu".equalsIgnoreCase(type)) {
                g2.setColor(new Color(0, 95, 185));
                g2.fillRect(x + 2, y + 4, 14, 2);
                g2.fillRect(x + 2, y + 8, 14, 2);
                g2.fillRect(x + 2, y + 12, 14, 2);
            } else if ("template".equalsIgnoreCase(type)) {
                g2.setColor(new Color(45, 125, 210));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x + 3, y + 4, 12, 10, 3, 3);
                g2.fillOval(x + 5, y + 7, 3, 3);
            } else if ("sign".equalsIgnoreCase(type)) {
                g2.setColor(new Color(110, 125, 145));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawLine(x + 4, y + 14, x + 12, y + 6);
                g2.drawLine(x + 10, y + 4, x + 14, y + 8);
                g2.drawLine(x + 2, y + 16, x + 5, y + 13);
            } else if ("history".equalsIgnoreCase(type)) {
                g2.setColor(new Color(0, 115, 205));
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawOval(x + 3, y + 3, 12, 12);
                g2.drawLine(x + 9, y + 5, x + 9, y + 9);
                g2.drawLine(x + 9, y + 9, x + 12, y + 9);
            }
            g2.dispose();
        }
    }

    private static class CheckmarkBlueIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 115, 220));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 7, x + 5, y + 11);
            g2.drawLine(x + 5, y + 11, x + 12, y + 2);
            g2.dispose();
        }
    }

    private static class FloppyDiskIcon implements Icon {
        private final Color color;
        FloppyDiskIcon(Color color) { this.color = color; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(x + 1, y + 1, 14, 14, 2, 2);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 4, y + 2, 8, 5);
            g2.fillRect(x + 3, y + 9, 10, 5);
            g2.setColor(color);
            g2.fillRect(x + 9, y + 3, 2, 3);
            g2.dispose();
        }
    }
}
