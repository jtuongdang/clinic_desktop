package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocHandler;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocBienNhan;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocCommand;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.examination.components.common.DraftOrderTabPanel;
import vn.clinicmanagement.desktop.modules.examination.components.common.ExaminationOrderIcons;
import vn.clinicmanagement.desktop.modules.examination.components.common.ExaminationUiFactory;
import vn.clinicmanagement.desktop.modules.examination.components.common.IcdDiagnosisSelectorSupport;
import vn.clinicmanagement.desktop.modules.examination.components.common.PatientOrderBannerPanel;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.examination.ICD;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.TonKho;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Màn hình "ĐHC - Chỉ Định Vật Tư Y Tế": nạp danh mục vật tư thật từ `clinicDb` (`Item` + `TonKho`),
 * kê phiếu nháp qua `VatTuDraftModel` và lưu thật xuống CSDL qua `PrescribeThuocHandler`.
 */
public class YTeVatTuYeuCauManHinh extends JPanel {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public record SupplyCatalogItem(
            int itemId,
            String code,
            String name,
            String unit,
            String category,
            int warehouseId,
            String warehouseName,
            BigDecimal stockQty,
            BigDecimal price) {
    }

    public record SupplyDraftItem(
            int itemId,
            String code,
            String name,
            String unit,
            int warehouseId,
            String warehouseName,
            BigDecimal quantity,
            BigDecimal unitPrice) {
    }

    private final Window ownerWindow;
    private final Runnable onCloseCallback;
    private KhamBenhGhepNoiGoc.Services services;

    private final List<SupplyCatalogItem> allSupplies = new ArrayList<>();
    private final List<SupplyCatalogItem> filteredSupplies = new ArrayList<>();
    private final VatTuDraftModel draftModel = new VatTuDraftModel();
    private SupplyCatalogItem selectedSupplyItem;

    private JTable suppliesTable;
    private DefaultTableModel tableModel;
    private DraftOrderTabPanel draftOrderTabPanel;
    private JTabbedPane tabPane;
    private JTree categoryTree;
    private DefaultTreeModel categoryTreeModel;

    private PatientOrderBannerPanel patientBanner;
    private JLabel lblOrderText;
    private JLabel lblTotalCostVal;
    private JLabel lblBhytCostVal;
    private JLabel lblPatientCostVal;
    private JLabel lblAdvanceVal;
    private JLabel lblRemainingVal;

    private AdminPatientItem patient;
    private int currentVisitId = 0;
    private int currentDoctorId = 0;
    private int currentFacilityId = 1;
    private String selectedCategoryFilter = null;

    private Consumer<List<Object[]>> onSuppliesSavedCallback;
    private boolean suppressDialogs = false;

    private JComboBox<String> cbOrderDate;
    private JComboBox<String> cbIcd;
    private JComboBox<String> cbDiagnosisDesc;
    private IcdDiagnosisSelectorSupport icdSelector;
    private JComboBox<String> cbItemName;
    private JTextField txtUnit;
    private JTextField txtStore;
    private JTextField txtQty;
    private JButton btnSave;
    private JButton btnSaveAndClose;
    private JButton btnClose;

    public YTeVatTuYeuCauManHinh() {
        this((Runnable) null, null);
    }

    public YTeVatTuYeuCauManHinh(Runnable onCloseCallback) {
        this(onCloseCallback, null);
    }

    public YTeVatTuYeuCauManHinh(Runnable onCloseCallback, KhamBenhGhepNoiGoc.Services services) {
        this.ownerWindow = null;
        this.onCloseCallback = onCloseCallback;
        this.services = services;
        initPanel();
        loadSuppliesFromDatabase();
        loadIcdCatalog();
    }

    public YTeVatTuYeuCauManHinh(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
        this.onCloseCallback = () -> {
            if (ownerWindow != null) {
                ownerWindow.dispose();
            }
        };
        initPanel();
        loadSuppliesFromDatabase();
        loadIcdCatalog();
    }

    public void setServices(KhamBenhGhepNoiGoc.Services services) {
        this.services = services;
        loadSuppliesFromDatabase();
        loadIcdCatalog();
    }

    public void setVisitContext(int visitId, int doctorId, int facilityId) {
        this.currentVisitId = Math.max(visitId, 0);
        this.currentDoctorId = Math.max(doctorId, 0);
        this.currentFacilityId = facilityId > 0 ? facilityId : 1;
        clearDraftSupplies();
        if (cbOrderDate != null) {
            cbOrderDate.removeAllItems();
            cbOrderDate.addItem(LocalDateTime.now().format(DATE_TIME_FMT));
        }
    }

    private void initPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        add(buildHeaderSection(), BorderLayout.NORTH);
        add(buildBodySection(), BorderLayout.CENTER);
        add(buildFooterSection(), BorderLayout.SOUTH);
    }

    private JPanel buildHeaderSection() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(new Color(246, 248, 252));
        topRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));
        topRow.setPreferredSize(new Dimension(1000, 52));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        leftActions.setOpaque(false);
        leftActions.add(ExaminationUiFactory.createToolbarButton("Menu", "menu", false, false));
        leftActions.add(ExaminationUiFactory.createToolbarButton("Đơn Vật Tư Mẫu", "template", true, false));
        leftActions.add(ExaminationUiFactory.createToolbarButton("Ký Vật Tư", "sign", false, false));
        JButton btnRefresh = ExaminationUiFactory.createToolbarButton("Làm Mới Danh Mục", "history", false, false);
        btnRefresh.addActionListener(e -> loadSuppliesFromDatabase());
        leftActions.add(btnRefresh);
        if (onCloseCallback != null) {
            JButton btnBack = ExaminationUiFactory.createToolbarButton("Quay lại", "menu", false, false);
            btnBack.addActionListener(e -> closeWithoutSaving());
            leftActions.add(btnBack);
        }
        topRow.add(leftActions, BorderLayout.WEST);

        patientBanner = new PatientOrderBannerPanel("Chưa chọn bệnh nhân", "", "");
        topRow.add(patientBanner, BorderLayout.EAST);
        headerPanel.add(topRow);

        JPanel contextRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        contextRow.setBackground(new Color(248, 250, 253));
        contextRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 222, 232)));

        JLabel lblOrderDate = new JLabel("Ngày y lệnh:");
        lblOrderDate.setFont(UngDungPhongChu.BODY);
        cbOrderDate = new JComboBox<>(new String[]{LocalDateTime.now().format(DATE_TIME_FMT)});
        cbOrderDate.setFont(UngDungPhongChu.BODY);
        cbOrderDate.setPreferredSize(new Dimension(150, 24));
        cbOrderDate.setBackground(Color.WHITE);

        JLabel lblDiagnosis = new JLabel("Chẩn đoán:");
        lblDiagnosis.setFont(UngDungPhongChu.BODY);
        cbIcd = new JComboBox<>(new String[]{""});
        cbIcd.setEditable(true);
        cbIcd.setFont(UngDungPhongChu.BODY);
        cbIcd.setPreferredSize(new Dimension(85, 24));
        cbIcd.setBackground(Color.WHITE);

        cbDiagnosisDesc = new JComboBox<>(new String[]{""});
        cbDiagnosisDesc.setEditable(true);
        cbDiagnosisDesc.setFont(UngDungPhongChu.BODY);
        cbDiagnosisDesc.setPreferredSize(new Dimension(420, 24));
        cbDiagnosisDesc.setBackground(Color.WHITE);

        contextRow.add(lblOrderDate);
        contextRow.add(cbOrderDate);
        contextRow.add(Box.createHorizontalStrut(15));
        contextRow.add(lblDiagnosis);
        contextRow.add(cbIcd);
        JButton btnBrowseIcdCode = ExaminationUiFactory.createMiniBrowseButton(24, 24);
        contextRow.add(btnBrowseIcdCode);
        contextRow.add(cbDiagnosisDesc);
        JButton btnBrowseIcdDesc = ExaminationUiFactory.createMiniBrowseButton(24, 24);
        contextRow.add(btnBrowseIcdDesc);
        icdSelector = new IcdDiagnosisSelectorSupport(
                cbIcd, btnBrowseIcdCode, cbDiagnosisDesc, btnBrowseIcdDesc,
                this::fetchIcdCatalogFromServices, this);

        headerPanel.add(contextRow);
        return headerPanel;
    }

    private JPanel buildBodySection() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);

        tabPane = ExaminationUiFactory.createStyledOrderTabbedPane(true);
        tabPane.addTab("Danh Sách Vật Tư", buildSuppliesSplitView());
        draftOrderTabPanel = new DraftOrderTabPanel(
                "Xóa vật tư đang chọn",
                new String[]{"STT", "Mã Vật Tư", "Tên Vật Tư", "Đơn Vị", "Tên Kho", "Số Lượng", "Đơn Giá", "Thành Tiền"},
                row -> {
                    draftModel.removeAt(row);
                    refreshDraftTable();
                    updateOrderSummaryAndFinancials();
                });
        tabPane.addTab("Phiếu Đang Kê (0)", draftOrderTabPanel);

        body.add(tabPane, BorderLayout.CENTER);
        return body;
    }

    private JComponent buildSuppliesSplitView() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(210);
        splitPane.setDividerSize(3);
        splitPane.setBorder(null);

        splitPane.setLeftComponent(buildCategoryTreePanel());
        splitPane.setRightComponent(buildCatalogRightPanel());
        return splitPane;
    }

    private JPanel buildCategoryTreePanel() {
        JPanel treeContainer = new JPanel(new BorderLayout());
        treeContainer.setBackground(Color.WHITE);
        treeContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(205, 214, 226)));

        JLabel lblTreeHeader = new JLabel("Kho & Nhóm Vật Tư");
        lblTreeHeader.setFont(UngDungPhongChu.BODY_BOLD);
        lblTreeHeader.setForeground(new Color(138, 21, 21));
        lblTreeHeader.setBorder(new EmptyBorder(4, 6, 4, 4));
        treeContainer.add(lblTreeHeader, BorderLayout.NORTH);

        categoryTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Tất cả vật tư (0)"));
        categoryTree = new JTree(categoryTreeModel);
        categoryTree.setFont(UngDungPhongChu.BODY);
        categoryTree.setRootVisible(true);
        categoryTree.setShowsRootHandles(true);
        categoryTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                JLabel l = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                l.setFont(UngDungPhongChu.BODY);
                l.setOpaque(sel);
                if (sel) {
                    l.setBackground(new Color(0, 120, 215));
                    l.setForeground(Color.WHITE);
                } else {
                    l.setForeground(Color.BLACK);
                }
                setIcon(null);
                return l;
            }
        });
        categoryTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) categoryTree.getLastSelectedPathComponent();
            if (node == null) return;
            String label = String.valueOf(node.getUserObject());
            if (node.isRoot()) {
                selectedCategoryFilter = null;
            } else {
                int paren = label.lastIndexOf(" (");
                selectedCategoryFilter = paren > 0 ? label.substring(0, paren) : label;
            }
            refreshCatalogTable();
        });

        JScrollPane treeScroll = new JScrollPane(categoryTree);
        treeScroll.setBorder(BorderFactory.createEmptyBorder());
        treeScroll.getViewport().setBackground(Color.WHITE);
        treeContainer.add(treeScroll, BorderLayout.CENTER);
        return treeContainer;
    }

    private JPanel buildCatalogRightPanel() {
        JPanel rightContent = new JPanel(new BorderLayout());
        rightContent.setBackground(Color.WHITE);
        rightContent.add(buildInputBar(), BorderLayout.NORTH);

        String[] cols = {"Mã Vật Tư", "Tên Vật Tư", "Đơn Vị", "Tên Kho", "Tồn Kho", "Giá viện phí", "Giá dịch vụ"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        suppliesTable = new JTable(tableModel);
        suppliesTable.setRowHeight(22);
        suppliesTable.setFont(UngDungPhongChu.BODY);
        suppliesTable.setShowGrid(true);
        suppliesTable.setGridColor(new Color(225, 230, 238));
        suppliesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suppliesTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = suppliesTable.getSelectedRow();
            if (row >= 0 && row < filteredSupplies.size()) {
                selectCatalogItem(filteredSupplies.get(row));
            }
        });

        JTableHeader th = suppliesTable.getTableHeader();
        th.setFont(UngDungPhongChu.SMALL_BOLD);
        th.setPreferredSize(new Dimension(800, 24));
        th.setBackground(new Color(242, 246, 252));
        th.setForeground(new Color(25, 30, 40));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 210, 222)));

        suppliesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {85, 390, 65, 145, 75, 90, 90};
        for (int i = 0; i < widths.length; i++) {
            suppliesTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        suppliesTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setOpaque(true);
                l.setBackground(isSelected ? new Color(0, 120, 215) : (row % 2 == 0 ? Color.WHITE : new Color(249, 251, 254)));
                l.setForeground(isSelected ? Color.WHITE : Color.BLACK);
                l.setHorizontalAlignment(column == 2 ? SwingConstants.CENTER : (column >= 4 ? SwingConstants.RIGHT : SwingConstants.LEFT));
                return l;
            }
        });

        JScrollPane tableScroll = new JScrollPane(suppliesTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);
        rightContent.add(tableScroll, BorderLayout.CENTER);

        JPanel orderStatusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        orderStatusBar.setBackground(Color.WHITE);
        orderStatusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(225, 230, 238)));
        JLabel lblOrderAsterisk = new JLabel("*");
        lblOrderAsterisk.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblOrderAsterisk.setForeground(new Color(0, 102, 204));
        lblOrderText = new JLabel("Chọn vật tư trong bảng và bấm OK (Ctrl+A) để thêm vào phiếu chỉ định.");
        lblOrderText.setFont(UngDungPhongChu.BODY_BOLD);
        lblOrderText.setForeground(new Color(45, 50, 60));
        orderStatusBar.add(lblOrderAsterisk);
        orderStatusBar.add(lblOrderText);
        rightContent.add(orderStatusBar, BorderLayout.SOUTH);

        return rightContent;
    }

    private JPanel buildInputBar() {
        JPanel inputBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
        inputBar.setBackground(new Color(248, 250, 253));
        inputBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 218, 228)));

        cbItemName = new JComboBox<>();
        cbItemName.setFont(UngDungPhongChu.BODY);
        cbItemName.setPreferredSize(new Dimension(260, 24));
        cbItemName.setBackground(Color.WHITE);
        cbItemName.setEditable(true);
        cbItemName.addActionListener(e -> {
            if ("comboBoxChanged".equals(e.getActionCommand())) {
                String text = cbItemName.getSelectedItem() == null ? "" : cbItemName.getSelectedItem().toString().trim();
                SupplyCatalogItem match = findCatalogItemByLabelOrCode(text);
                if (match != null) selectCatalogItem(match);
            }
        });

        JButton btnFilter = ExaminationUiFactory.createMiniBrowseButton(24, 24);
        btnFilter.setToolTipText("Lọc danh mục theo tên/mã vật tư đang nhập");
        btnFilter.addActionListener(e -> refreshCatalogTable());

        txtUnit = createReadOnlyField(60, SwingConstants.CENTER);
        txtStore = createReadOnlyField(135, SwingConstants.LEFT);
        txtQty = new JTextField("1");
        txtQty.setFont(UngDungPhongChu.BODY_BOLD);
        txtQty.setPreferredSize(new Dimension(50, 24));
        txtQty.setHorizontalAlignment(SwingConstants.CENTER);

        JButton btnOk = new JButton(" OK (Ctrl+A)", new ExaminationOrderIcons.CheckmarkBlueIcon());
        btnOk.setFont(UngDungPhongChu.BODY_BOLD);
        btnOk.setBackground(Color.WHITE);
        btnOk.setFocusPainted(false);
        btnOk.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOk.setPreferredSize(new Dimension(125, 24));
        btnOk.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(180, 190, 205), 1), new EmptyBorder(1, 4, 1, 4)));
        btnOk.addActionListener(e -> applyOrderSelection());

        inputBar.add(bodyLabel("Tên vật tư/Mã:"));
        inputBar.add(cbItemName);
        inputBar.add(btnFilter);
        inputBar.add(bodyLabel("Đơn vị:"));
        inputBar.add(txtUnit);
        inputBar.add(bodyLabel("Tên kho:"));
        inputBar.add(txtStore);
        inputBar.add(bodyLabel("Số lượng:"));
        inputBar.add(txtQty);
        inputBar.add(btnOk);
        return inputBar;
    }

    private JPanel buildFooterSection() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(245, 248, 252));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(205, 214, 226)),
                new EmptyBorder(4, 10, 4, 10)));
        footer.setPreferredSize(new Dimension(1000, 48));

        JPanel statsPanel = new JPanel(new GridLayout(2, 4, 15, 2));
        statsPanel.setOpaque(false);
        Color red = new Color(185, 25, 25);
        Color blue = new Color(0, 45, 140);
        lblTotalCostVal = metricValueLabel(red);
        lblBhytCostVal = metricValueLabel(red);
        lblPatientCostVal = metricValueLabel(red);
        lblAdvanceVal = metricValueLabel(blue);
        lblRemainingVal = metricValueLabel(blue);

        statsPanel.add(ExaminationUiFactory.createMetricBox("Tổng chi phí:", lblTotalCostVal));
        statsPanel.add(ExaminationUiFactory.createMetricBox("BHYT:", lblBhytCostVal));
        statsPanel.add(ExaminationUiFactory.createMetricLabel("Nguồn khác:", "0", red));
        statsPanel.add(ExaminationUiFactory.createMetricBox("Bệnh nhân:", lblPatientCostVal));
        statsPanel.add(ExaminationUiFactory.createMetricBox("Tạm ứng:", lblAdvanceVal));
        statsPanel.add(ExaminationUiFactory.createMetricLabel("Đã thu:", "0", blue));
        statsPanel.add(ExaminationUiFactory.createMetricLabel("Miễn giảm:", "0", blue));
        statsPanel.add(ExaminationUiFactory.createMetricBox("Còn dư:", lblRemainingVal));
        footer.add(statsPanel, BorderLayout.CENTER);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        rightButtons.setOpaque(false);
        btnSave = ExaminationUiFactory.createFooterButton(" Lưu (Ctrl + S)",
                new ExaminationOrderIcons.FloppyDiskIcon(new Color(125, 45, 135)), 140, null);
        btnSave.addActionListener(e -> executeSave(false));
        btnSaveAndClose = ExaminationUiFactory.createFooterButton(" Lưu + Đóng",
                new ExaminationOrderIcons.FloppyDiskIcon(new Color(40, 145, 75)), 115, null);
        btnSaveAndClose.addActionListener(e -> executeSave(true));
        btnClose = ExaminationUiFactory.createFooterButton(" Đóng",
                new ExaminationOrderIcons.CloseCrossIcon(), 86, new Color(175, 30, 30));
        btnClose.addActionListener(e -> closeWithoutSaving());

        rightButtons.add(btnSave);
        rightButtons.add(btnSaveAndClose);
        rightButtons.add(btnClose);
        footer.add(rightButtons, BorderLayout.EAST);
        return footer;
    }

    public void loadSuppliesFromDatabase() {
        List<SupplyCatalogItem> loaded = new ArrayList<>();
        if (services != null && services.inventoryRepository() != null) {
            try {
                Map<Integer, String> khoNames = new LinkedHashMap<>();
                for (MatHang item : services.inventoryRepository().searchItems("", true)) {
                    if (item == null || item.getLoaiItem() != LoaiMatHang.VAT_TU) continue;
                    int warehouseId = 4;
                    BigDecimal stockTotal = BigDecimal.ZERO;
                    if (item.getTonKhos() != null && !item.getTonKhos().isEmpty()) {
                        warehouseId = item.getTonKhos().get(0).getMaKho();
                        for (TonKho tk : item.getTonKhos()) {
                            if (tk != null && tk.getSoLuongTon() != null) stockTotal = stockTotal.add(tk.getSoLuongTon());
                        }
                    }
                    String warehouseName = khoNames.computeIfAbsent(warehouseId, id ->
                            services.inventoryRepository().getKhoById(id).map(Kho::getTenKho).orElse("Kho Vật Tư Y Tế"));
                    String category = ExaminationUiFactory.safeText(item.getHoatChat(), "Vật tư y tế");
                    BigDecimal price = item.getGiaBanMacDinh() != null ? item.getGiaBanMacDinh() : BigDecimal.ZERO;
                    loaded.add(new SupplyCatalogItem(
                            item.getId() != null ? item.getId() : 0,
                            safeText(item.getMaItem()), safeText(item.getTenItem()), safeText(item.getDonViCoBan()),
                            category, warehouseId, warehouseName, stockTotal, price));
                }
            } catch (Exception ignored) {
            }
        }
        loadSupplyCatalogItems(loaded);
    }

    public void loadSupplyCatalogItems(List<SupplyCatalogItem> items) {
        allSupplies.clear();
        if (items != null) allSupplies.addAll(items);
        selectedCategoryFilter = null;
        rebuildCategoryTree();
        if (cbItemName != null) {
            cbItemName.removeAllItems();
            for (SupplyCatalogItem item : allSupplies) cbItemName.addItem(item.code() + " - " + item.name());
        }
        refreshCatalogTable();
        if (!filteredSupplies.isEmpty()) {
            suppliesTable.setRowSelectionInterval(0, 0);
            selectCatalogItem(filteredSupplies.get(0));
        } else {
            selectedSupplyItem = null;
            if (txtUnit != null) txtUnit.setText("");
            if (txtStore != null) txtStore.setText("");
        }
    }

    private void rebuildCategoryTree() {
        if (categoryTreeModel == null || categoryTree == null) return;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Tất cả vật tư (" + allSupplies.size() + ")");
        Map<String, Integer> groupCounts = new LinkedHashMap<>();
        for (SupplyCatalogItem item : allSupplies) {
            groupCounts.merge(ExaminationUiFactory.safeText(item.category(), "Vật tư y tế"), 1, Integer::sum);
        }
        groupCounts.forEach((k, v) -> root.add(new DefaultMutableTreeNode(k + " (" + v + ")")));
        categoryTreeModel.setRoot(root);
        ExaminationUiFactory.expandAllNodes(categoryTree, 0, categoryTree.getRowCount());
    }

    private void refreshCatalogTable() {
        if (tableModel == null) return;
        filteredSupplies.clear();
        tableModel.setRowCount(0);
        String keyword = cbItemName != null && cbItemName.getEditor() != null && cbItemName.getEditor().getItem() != null
                ? cbItemName.getEditor().getItem().toString().trim().toLowerCase(Locale.ROOT) : "";
        boolean applyKeyword = !keyword.isEmpty() && findCatalogItemByLabelOrCode(keyword) == null;

        for (SupplyCatalogItem item : allSupplies) {
            if (selectedCategoryFilter != null && !selectedCategoryFilter.equalsIgnoreCase(item.category())) continue;
            if (applyKeyword && !item.code().toLowerCase(Locale.ROOT).contains(keyword)
                    && !item.name().toLowerCase(Locale.ROOT).contains(keyword)) continue;
            filteredSupplies.add(item);
            String priceText = ExaminationUiFactory.formatMoney(item.price());
            tableModel.addRow(new Object[]{
                    item.code(), item.name(), item.unit(), item.warehouseName(),
                    item.stockQty().stripTrailingZeros().toPlainString(), priceText, priceText});
        }
        for (int i = tableModel.getRowCount(); i < 15; i++) {
            tableModel.addRow(new Object[]{"", "", "", "", "", "", ""});
        }
    }

    private SupplyCatalogItem findCatalogItemByLabelOrCode(String query) {
        if (query == null || query.isBlank()) return null;
        String q = query.trim();
        for (SupplyCatalogItem item : allSupplies) {
            if (item.code().equalsIgnoreCase(q) || (item.code() + " - " + item.name()).equalsIgnoreCase(q) || item.name().equalsIgnoreCase(q)) {
                return item;
            }
        }
        return null;
    }

    public void selectCatalogItemByCode(String code) {
        SupplyCatalogItem item = findCatalogItemByLabelOrCode(code);
        if (item != null) selectCatalogItem(item);
    }

    private void selectCatalogItem(SupplyCatalogItem item) {
        this.selectedSupplyItem = item;
        if (item == null) return;
        if (cbItemName != null) cbItemName.setSelectedItem(item.code() + " - " + item.name());
        if (txtUnit != null) txtUnit.setText(item.unit());
        if (txtStore != null) txtStore.setText(item.warehouseName());
    }

    public void setPatient(AdminPatientItem patient) {
        this.patient = patient;
        if (patient == null) return;
        if (patient.visitId > 0) this.currentVisitId = patient.visitId;
        clearDraftSupplies();
        if (patientBanner != null) patientBanner.bindPatient(patient);
        if (icdSelector != null) {
            icdSelector.setDiagnosis(patient.diagnosis);
        }
        if (cbOrderDate != null) {
            cbOrderDate.removeAllItems();
            cbOrderDate.addItem(LocalDateTime.now().format(DATE_TIME_FMT));
        }
        updateOrderSummaryAndFinancials();
    }

    public void setOnSuppliesSavedCallback(Consumer<List<Object[]>> callback) { this.onSuppliesSavedCallback = callback; }
    public void setSuppressDialogs(boolean suppressDialogs) { this.suppressDialogs = suppressDialogs; }
    public boolean isSuppressDialogs() { return suppressDialogs; }

    private boolean isModalDialogAllowed() {
        if (suppressDialogs || GraphicsEnvironment.isHeadless()) return false;
        String surefire = System.getProperty("surefire.test.class.path");
        return surefire == null || surefire.isBlank();
    }

    public void executeSave() { executeSave(false); }

    public void executeSave(boolean andClose) {
        if (draftModel.isEmpty() && selectedSupplyItem != null) applyOrderSelection();
        if (draftModel.isEmpty()) {
            if (isModalDialogAllowed()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng chọn vật tư và nhấn OK (Ctrl+A) để thêm vào phiếu trước khi lưu.",
                        "Phiếu Vật Tư Trống", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        List<Object[]> savedRows = draftModel.toCallbackRows();
        if (services == null || services.prescribeMedication() == null) {
            if (onSuppliesSavedCallback != null) onSuppliesSavedCallback.accept(savedRows);
            if (andClose) navigateBack();
            return;
        }

        PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();
        if (currentVisitId <= 0 || auth == null || !auth.isValid()) {
            if (isModalDialogAllowed()) {
                JOptionPane.showMessageDialog(this,
                        "Chưa có lượt khám hoặc phiên đăng nhập bác sĩ hợp lệ để lưu phiếu vật tư.",
                        "Không thể lưu phiếu vật tư", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        PrescribeThuocCommand cmd = buildPrescribeCommand(auth);
        Integer selectedIcdId = icdSelector != null ? icdSelector.getSelectedIcdId() : null;
        String diagnosisText = icdSelector != null ? icdSelector.getDiagnosisTextOnly() : "";
        String diagnosisDisplay = icdSelector != null ? icdSelector.getDiagnosisDisplay() : "";
        String previousDiagnosis = patient != null && patient.diagnosis != null ? patient.diagnosis : "";
        new SwingWorker<KetQua<PrescribeThuocBienNhan>, Void>() {
            @Override
            protected KetQua<PrescribeThuocBienNhan> doInBackground() {
                return KhamBenhGhepNoiGoc.prescribeWithAutoStart(
                        services, cmd, selectedIcdId, diagnosisText, diagnosisDisplay, previousDiagnosis);
            }

            @Override
            protected void done() {
                try {
                    KetQua<PrescribeThuocBienNhan> res = get();
                    if (res != null && res.isSuccess()) {
                        PrescribeThuocBienNhan receipt = res.getValue().orElse(null);
                        String ticketCode = receipt != null ? "VT-" + receipt.prescriptionId() : "VT-SUCCESS";
                        if (onSuppliesSavedCallback != null) onSuppliesSavedCallback.accept(savedRows);
                        clearDraftSupplies();
                        if (isModalDialogAllowed()) {
                            JOptionPane.showMessageDialog(YTeVatTuYeuCauManHinh.this,
                                    "Đã lưu phiếu chỉ định vật tư vào CSDL thành công!\n▶ Mã phiếu: " + ticketCode,
                                    "Lưu Chỉ Định Vật Tư", JOptionPane.INFORMATION_MESSAGE);
                        }
                        if (andClose) navigateBack();
                    } else if (isModalDialogAllowed()) {
                        String err = res == null ? "Không nhận được kết quả từ hệ thống."
                                : res.getError().map(e -> e.message()).orElse("Không thể lưu phiếu vật tư.");
                        JOptionPane.showMessageDialog(YTeVatTuYeuCauManHinh.this, err, "Lỗi Nghiệp Vụ", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    if (isModalDialogAllowed()) {
                        JOptionPane.showMessageDialog(YTeVatTuYeuCauManHinh.this,
                                "Lỗi khi lưu phiếu vật tư: " + ex.getMessage(), "Lỗi Hệ Thống", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    public PrescribeThuocCommand buildPrescribeCommand(PhanQuyenNguCanh auth) {
        return draftModel.buildPrescribeCommand(currentVisitId, currentDoctorId, currentFacilityId, auth);
    }

    private void applyOrderSelection() {
        SupplyCatalogItem target = selectedSupplyItem;
        if (target == null && suppliesTable != null && suppliesTable.getSelectedRow() >= 0 && suppliesTable.getSelectedRow() < filteredSupplies.size()) {
            target = filteredSupplies.get(suppliesTable.getSelectedRow());
        }
        if (target == null && !filteredSupplies.isEmpty()) target = filteredSupplies.get(0);
        if (target == null) {
            if (isModalDialogAllowed()) {
                JOptionPane.showMessageDialog(this, "Chưa có vật tư nào được chọn.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        confirmSupplyDraft(target, parsePositiveQty(txtQty != null ? txtQty.getText() : "1"));
    }

    public void confirmSupplyDraft(SupplyCatalogItem item, BigDecimal qty) {
        draftModel.addOrMerge(item, qty);
        if (txtQty != null) txtQty.setText("1");
        refreshDraftTable();
        updateOrderSummaryAndFinancials();
    }

    public void clearDraftSupplies() {
        draftModel.clear();
        refreshDraftTable();
        updateOrderSummaryAndFinancials();
    }

    private void refreshDraftTable() {
        if (draftOrderTabPanel != null) draftOrderTabPanel.setRows(draftModel.toDraftTableRows());
        if (tabPane != null && tabPane.getTabCount() > 1) {
            tabPane.setTitleAt(1, "Phiếu Đang Kê (" + draftModel.size() + ")");
        }
    }

    private void updateOrderSummaryAndFinancials() {
        if (lblOrderText != null) lblOrderText.setText(draftModel.buildStatusSummaryText());
        VatTuDraftModel.FinancialSnapshot snap = draftModel.calculateFinancials(patient);
        if (lblTotalCostVal != null) lblTotalCostVal.setText(ExaminationUiFactory.formatMoney(snap.total()));
        if (lblBhytCostVal != null) lblBhytCostVal.setText(ExaminationUiFactory.formatMoney(snap.bhyt()));
        if (lblPatientCostVal != null) lblPatientCostVal.setText(ExaminationUiFactory.formatMoney(snap.patient()));
        if (lblAdvanceVal != null) lblAdvanceVal.setText("0");
        if (lblRemainingVal != null) lblRemainingVal.setText("0");
    }

    public void closeWithoutSaving() {
        if (!draftModel.isEmpty() && isModalDialogAllowed()) {
            int ret = JOptionPane.showConfirmDialog(this,
                    "Phiếu vật tư đang kê chưa được lưu. Bạn có chắc muốn đóng và quay lại màn hình Khám Bệnh?",
                    "Xác Nhận Đóng", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ret != JOptionPane.YES_OPTION) return;
        }
        clearDraftSupplies();
        navigateBack();
    }

    private void navigateBack() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        } else if (ownerWindow != null) {
            ownerWindow.dispose();
        }
    }

    private static BigDecimal parsePositiveQty(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ONE;
        BigDecimal val = ExaminationUiFactory.parseDecimalSafe(raw.replace(",", "."));
        return val.compareTo(BigDecimal.ZERO) > 0 ? val : BigDecimal.ONE;
    }

    private static JLabel bodyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.BODY);
        return l;
    }

    private static JTextField createReadOnlyField(int width, int align) {
        JTextField tf = new JTextField("");
        tf.setFont(UngDungPhongChu.BODY);
        tf.setPreferredSize(new Dimension(width, 24));
        tf.setEditable(false);
        tf.setBackground(Color.WHITE);
        tf.setHorizontalAlignment(align);
        return tf;
    }

    private static JLabel metricValueLabel(Color color) {
        JLabel l = new JLabel("0");
        l.setFont(UngDungPhongChu.BODY_BOLD);
        l.setForeground(color);
        return l;
    }

    private static String safeText(String val) { return val == null ? "" : val; }

    private List<ICD> fetchIcdCatalogFromServices() {
        if (services == null || services.examQueue() == null) return List.of();
        try {
            return services.examQueue().searchIcdCatalog("");
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void loadIcdCatalog() {
        if (icdSelector != null) {
            icdSelector.loadCatalog(fetchIcdCatalogFromServices());
        }
    }

    public List<SupplyCatalogItem> getAllSupplies() { return List.copyOf(allSupplies); }
    public List<SupplyDraftItem> getDraftSupplyItems() { return draftModel.snapshot(); }
    public JButton getBtnSave() { return btnSave; }
    public JButton getBtnSaveAndClose() { return btnSaveAndClose; }
    public JButton getBtnClose() { return btnClose; }
    public JTable getSuppliesTable() { return suppliesTable; }
    public DefaultTableModel getTableModel() { return tableModel; }
    public AdminPatientItem getPatient() { return patient; }
    public JLabel getLblTotalCostVal() { return lblTotalCostVal; }
    public JTextField getTxtQty() { return txtQty; }
    public JTextField getTxtStore() { return txtStore; }
    public IcdDiagnosisSelectorSupport getIcdSelector() { return icdSelector; }
}
