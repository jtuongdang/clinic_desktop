package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocBienNhan;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocCommand;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.examination.components.common.DraftOrderTabPanel;
import vn.clinicmanagement.desktop.modules.examination.components.common.ExaminationOrderIcons;
import vn.clinicmanagement.desktop.modules.examination.components.common.ExaminationUiFactory;
import vn.clinicmanagement.desktop.modules.examination.components.common.IcdDiagnosisSelectorSupport;
import vn.clinicmanagement.desktop.modules.examination.components.common.PatientOrderBannerPanel;
import vn.clinicmanagement.desktop.modules.examination.components.print.DonThuocPrintPreviewHopThoai;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Giao diện màn hình: "ĐHC - Chỉ Định Thuốc"
 * Tải danh mục thuốc & tồn kho thật từ CSDL, hỗ trợ autocomplete (`DonThuocAutocompleteSupport`)
 * và lưu đơn thuốc nháp qua `DonThuocDraftModel`.
 */
public class DonThuocYeuCauManHinh extends JPanel {

    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public static class MedicineSearchItem {
        public final int itemId;
        public final String itemType;
        public final String code;
        public final String name;
        public final String displayString;
        public final String unit;
        public final String pack;
        public final String stock;
        public final BigDecimal stockValue;
        public final String price;
        public final BigDecimal priceValue;
        public final String route;
        public final String hdsd;

        public MedicineSearchItem(String code, String name, String unit, String pack, String stock, String price, String route, String hdsd) {
            this(0, "THUOC", code, name, unit, pack,
                    ExaminationUiFactory.parseDecimalSafe(stock), stock,
                    ExaminationUiFactory.parseDecimalSafe(price), price, route, hdsd);
        }

        public MedicineSearchItem(
                int itemId, String itemType, String code, String name, String unit, String pack,
                BigDecimal stockValue, String stock, BigDecimal priceValue, String price,
                String route, String hdsd) {
            this.itemId = itemId;
            this.itemType = itemType != null && !itemType.isBlank() ? itemType : "THUOC";
            this.code = code != null ? code : "";
            this.name = name != null ? name : "";
            this.displayString = this.code + " - " + this.name;
            this.unit = unit != null ? unit : "";
            this.pack = pack != null ? pack : "";
            this.stockValue = stockValue != null ? stockValue : BigDecimal.ZERO;
            this.stock = stock != null ? stock : ExaminationUiFactory.formatMoney(this.stockValue);
            this.priceValue = priceValue != null ? priceValue : BigDecimal.ZERO;
            this.price = price != null ? price : ExaminationUiFactory.formatMoney(this.priceValue);
            this.route = route != null ? route : "Uống";
            this.hdsd = hdsd != null ? hdsd : "";
        }

        @Override public String toString() { return displayString; }
    }

    public static final class PrescribedDraftItem {
        public final int itemId;
        public final String code;
        public final String name;
        public final String unit;
        public final BigDecimal quantity;
        public final BigDecimal unitPrice;
        public final String route;
        public final String dosageSummary;
        public final String frequency;
        public final Integer days;
        public final BigDecimal morning;
        public final BigDecimal noon;
        public final BigDecimal afternoon;
        public final BigDecimal night;
        public final String hdsd;

        public PrescribedDraftItem(int itemId, String code, String name, String unit,
                                   BigDecimal quantity, BigDecimal unitPrice, String route,
                                   String dosageSummary, String frequency, Integer days,
                                   BigDecimal morning, BigDecimal noon, BigDecimal afternoon, BigDecimal night,
                                   String hdsd) {
            this.itemId = itemId;
            this.code = code;
            this.name = name;
            this.unit = unit;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.route = route;
            this.dosageSummary = dosageSummary;
            this.frequency = frequency;
            this.days = days;
            this.morning = morning;
            this.noon = noon;
            this.afternoon = afternoon;
            this.night = night;
            this.hdsd = hdsd;
        }
    }

    private final Runnable onCloseCallback;
    private final KhamBenhGhepNoiGoc.Services services;
    private int currentVisitId = 0;
    private int currentDoctorId = 0;
    private int currentFacilityId = 1;
    private KhamBenhManHinh.AdminPatientItem currentPatient;

    private JTable medicineTable;
    private DefaultTableModel tableModel;
    private JTabbedPane mainTabs;
    private DraftOrderTabPanel draftOrderTabPanel;
    private JTree categoryTree;
    private DefaultTreeModel categoryTreeModel;
    private String activeTypeFilter = "ALL";
    private final List<MedicineSearchItem> allMedicines = new ArrayList<>();
    private final List<MedicineSearchItem> filteredMedicines = new ArrayList<>();
    private final DonThuocDraftModel draftModel = new DonThuocDraftModel();
    private MedicineSearchItem selectedMedicineItem;

    private PatientOrderBannerPanel patientBanner;
    private JComboBox<String> cboOrderDate;
    private JComboBox<String> cboIcdCode;
    private JComboBox<String> cboIcdName;
    private IcdDiagnosisSelectorSupport icdSelector;
    private JTextField txtOrderDoctor;
    private JTextField txtDoctorAdvice;

    private JTextField txtDrugName;
    private JTextField txtKho;
    private JTextField txtPack;
    private JTextField txtUnit;
    private JTextField txtQty;
    private JTextField txtRoute;
    private JTextField txtHdsd;
    private JTextField txtDays;
    private JTextField txtMorning;
    private JTextField txtNoon;
    private JTextField txtAfternoon;
    private JTextField txtNight;
    private JButton btnOk;
    private JPanel orderStatusBar;
    private JLabel lblOrderStar;
    private DonThuocAutocompleteSupport autocompleteSupport;

    private JLabel lblValTienDon;
    private JLabel lblValTongChiPhi;
    private JLabel lblValBhyt;
    private JLabel lblValNguonKhac;
    private JLabel lblValBenhNhan;
    private JLabel lblValTamUng;
    private JLabel lblValDaThu;
    private JLabel lblValMienGiam;
    private JLabel lblValConDu;

    public DonThuocYeuCauManHinh() { this(null, resolveDefaultServices(), true); }
    public DonThuocYeuCauManHinh(Runnable onCloseCallback) { this(onCloseCallback, resolveDefaultServices(), true); }
    public DonThuocYeuCauManHinh(Runnable onCloseCallback, KhamBenhGhepNoiGoc.Services services) { this(onCloseCallback, services, true); }

    public DonThuocYeuCauManHinh(Runnable onCloseCallback, KhamBenhGhepNoiGoc.Services services, boolean loadCatalog) {
        this.onCloseCallback = onCloseCallback;
        this.services = services != null ? services : resolveDefaultServices();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        add(buildHeaderSection(), BorderLayout.NORTH);
        add(buildBodySection(), BorderLayout.CENTER);
        add(buildFooterSection(), BorderLayout.SOUTH);

        if (loadCatalog) initMedicineCatalog();
        loadIcdCatalog();
        updateFinancialSummary();
    }

    private static KhamBenhGhepNoiGoc.Services resolveDefaultServices() {
        try { return KhamBenhGhepNoiGoc.createDefault(); } catch (Exception ex) { return null; }
    }

    public void setVisitContext(int visitId, int doctorId, int facilityId) {
        boolean visitChanged = this.currentVisitId != visitId;
        this.currentVisitId = visitId;
        this.currentDoctorId = doctorId;
        this.currentFacilityId = facilityId > 0 ? facilityId : 1;
        if (visitChanged) clearDraftPrescription();
    }

    public void setPatientContext(KhamBenhManHinh.AdminPatientItem patient) {
        this.currentPatient = patient;
        if (patient == null) return;
        if (patient.visitId > 0 && this.currentVisitId != patient.visitId) {
            this.currentVisitId = patient.visitId;
            clearDraftPrescription();
        }
        if (patientBanner != null) patientBanner.bindPatient(patient);
        if (cboOrderDate != null) {
            String dateVal = patient.admissionDate != null && !patient.admissionDate.isBlank()
                    ? patient.admissionDate : LocalDateTime.now().format(ORDER_TIME_FORMATTER);
            cboOrderDate.setModel(new DefaultComboBoxModel<>(new String[]{dateVal}));
        }
        if (txtOrderDoctor != null) {
            txtOrderDoctor.setText(ExaminationUiFactory.safeText(patient.doctor, defaultDoctorLabel()));
        }
        if (icdSelector != null) {
            icdSelector.setDiagnosis(patient.diagnosis);
        }
    }

    private void initMedicineCatalog() {
        List<MedicineSearchItem> loaded = new ArrayList<>();
        if (services != null && services.inventoryRepository() != null) {
            try {
                for (MatHang item : services.inventoryRepository().searchItems("", true)) {
                    if (item == null) continue;
                    BigDecimal stockTotal = BigDecimal.ZERO;
                    if (item.getTonKhos() != null) {
                        for (TonKho tk : item.getTonKhos()) {
                            if (tk != null && tk.getSoLuongTon() != null) stockTotal = stockTotal.add(tk.getSoLuongTon());
                        }
                    }
                    BigDecimal priceVal = item.getGiaBanMacDinh() != null ? item.getGiaBanMacDinh() : BigDecimal.ZERO;
                    String typeCode = item.getLoaiItem() != null ? item.getLoaiItem().name() : LoaiMatHang.THUOC.name();
                    String defaultRoute = item.getDangBaoChe() != null && item.getDangBaoChe().toLowerCase(Locale.ROOT).contains("bôi")
                            ? "Dùng ngoài" : "Uống";
                    loaded.add(new MedicineSearchItem(
                            item.getId() != null ? item.getId() : 0, typeCode, item.getMaItem(), item.getTenItem(),
                            item.getDonViCoBan(), item.getQuyCachDongGoi(), stockTotal,
                            ExaminationUiFactory.formatMoney(stockTotal), priceVal,
                            ExaminationUiFactory.formatMoney(priceVal), defaultRoute, ""));
                }
            } catch (Exception ignored) {
            }
        }
        loadMedicineCatalogItems(loaded);
    }

    public void loadMedicineCatalogItems(List<MedicineSearchItem> items) {
        allMedicines.clear();
        if (items != null) allMedicines.addAll(items);
        rebuildCategoryTree();
        applyTypeFilter();
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
        leftActions.add(ExaminationUiFactory.createToolbarButton("Menu", "menu", false, true));
        leftActions.add(ExaminationUiFactory.createToolbarButton("Đơn Thuốc Mẫu", "template", true, true));
        leftActions.add(ExaminationUiFactory.createToolbarButton("Đơn Thuốc Cũ", "history", false, true));
        if (onCloseCallback != null) {
            JButton btnBack = ExaminationUiFactory.createToolbarButton("Quay lại", "menu", false, true);
            btnBack.addActionListener(e -> closeWithoutSaving());
            leftActions.add(btnBack);
        }
        topRow.add(leftActions, BorderLayout.WEST);

        patientBanner = new PatientOrderBannerPanel("Chưa chọn bệnh nhân | -- | --", "Mã VP : -- | Viện phí", "--");
        topRow.add(patientBanner, BorderLayout.EAST);

        headerPanel.add(topRow);
        headerPanel.add(buildOrderInfoGrid());
        return headerPanel;
    }

    private JPanel buildOrderInfoGrid() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(248, 250, 254));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)),
                new EmptyBorder(4, 12, 4, 12)));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 4, 2, 4);

        c.gridy = 0;
        c.gridx = 0; p.add(createFieldLabel("Ngày y lệnh:"), c);
        cboOrderDate = createDropdown(LocalDateTime.now().format(ORDER_TIME_FORMATTER), 140);
        c.gridx = 1; p.add(cboOrderDate, c);
        c.gridx = 2; p.add(createFieldLabel("Chẩn đoán:"), c);
        cboIcdCode = createDropdown("", 65);
        c.gridx = 3; p.add(cboIcdCode, c);
        JButton btnBrowseIcdCode = ExaminationUiFactory.createMiniBrowseButton(22, 23);
        c.gridx = 4; p.add(btnBrowseIcdCode, c);
        cboIcdName = createDropdown("", 320);
        c.gridx = 5; p.add(cboIcdName, c);
        JButton btnBrowseIcdName = ExaminationUiFactory.createMiniBrowseButton(22, 23);
        c.gridx = 6; p.add(btnBrowseIcdName, c);
        icdSelector = new IcdDiagnosisSelectorSupport(
                cboIcdCode, btnBrowseIcdCode, cboIcdName, btnBrowseIcdName,
                this::fetchIcdCatalogFromServices, this);

        JLabel lblSecondary = new JLabel("Bệnh kèm theo");
        lblSecondary.setFont(UngDungPhongChu.SMALL_BOLD);
        lblSecondary.setForeground(new Color(0, 75, 180));
        c.gridx = 7; c.gridwidth = 3; p.add(lblSecondary, c);

        c.gridwidth = 1; c.gridy = 1;
        c.gridx = 0; p.add(createFieldLabel("Bác sĩ điều trị:"), c);
        txtOrderDoctor = new JTextField(defaultDoctorLabel());
        txtOrderDoctor.setFont(UngDungPhongChu.SMALL);
        txtOrderDoctor.setPreferredSize(new Dimension(140, 23));
        c.gridx = 1; p.add(txtOrderDoctor, c);
        c.gridx = 2; p.add(createFieldLabel("Ngày tái khám:"), c);
        c.gridx = 3; p.add(createDropdown("", 65), c);

        JPanel pDays = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        pDays.setOpaque(false);
        pDays.add(new JLabel("Sau:"));
        JTextField txtDaysAfter = new JTextField("0");
        txtDaysAfter.setPreferredSize(new Dimension(30, 22));
        txtDaysAfter.setHorizontalAlignment(SwingConstants.CENTER);
        pDays.add(txtDaysAfter);
        JLabel lblNgayRed = new JLabel("ngày");
        lblNgayRed.setFont(UngDungPhongChu.SMALL_BOLD);
        lblNgayRed.setForeground(new Color(180, 25, 25));
        pDays.add(lblNgayRed);
        c.gridx = 4; c.gridwidth = 2; p.add(pDays, c);

        c.gridwidth = 1;
        c.gridx = 6; p.add(createFieldLabel("Lời dặn BS:"), c);
        txtDoctorAdvice = new JTextField();
        txtDoctorAdvice.setPreferredSize(new Dimension(280, 23));
        c.gridx = 7; p.add(txtDoctorAdvice, c);
        c.gridx = 8; p.add(ExaminationUiFactory.createMiniBrowseButton(22, 23), c);

        c.gridy = 2;
        c.gridx = 0; p.add(createFieldLabel("Đợt dùng thuốc:"), c);
        JTextField txtDot = new JTextField("0");
        txtDot.setPreferredSize(new Dimension(140, 23));
        txtDot.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridx = 1; p.add(txtDot, c);
        c.gridx = 2; p.add(createFieldLabel("Từ ngày:"), c);
        c.gridx = 3; p.add(createDropdown("//", 65), c);
        c.gridx = 4; p.add(createFieldLabel("Đến:"), c);
        c.gridx = 5; p.add(createDropdown("//", 120), c);
        return p;
    }

    private JPanel buildBodySection() {
        JPanel bodyPanel = new JPanel(new BorderLayout());
        bodyPanel.setBackground(Color.WHITE);

        mainTabs = ExaminationUiFactory.createStyledOrderTabbedPane(false);
        mainTabs.addTab("Danh Sách Thuốc", buildDrugListContent());
        draftOrderTabPanel = new DraftOrderTabPanel(
                "Xóa thuốc đang chọn",
                new String[]{"STT", "Mã Thuốc", "Tên Thuốc", "Đơn Vị", "SL", "Đơn Giá", "Thành Tiền", "Đường Dùng", "HDSD"},
                row -> removeDraftItemAt(row));
        mainTabs.addTab("Đơn Đang Kê (0)", draftOrderTabPanel);

        JPanel tabHeaderContainer = new JPanel(new BorderLayout());
        tabHeaderContainer.setBackground(Color.WHITE);
        JPanel pNotif = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        pNotif.setOpaque(false);
        JLabel lblNotif = new JLabel("Thông báo (0)", new ExaminationOrderIcons.EnvelopeIcon(), SwingConstants.RIGHT);
        lblNotif.setFont(UngDungPhongChu.SMALL_BOLD);
        lblNotif.setForeground(new Color(0, 75, 175));
        pNotif.add(lblNotif);
        pNotif.add(new JLabel(new ExaminationOrderIcons.PauseBarsIcon()));

        tabHeaderContainer.add(mainTabs, BorderLayout.CENTER);
        tabHeaderContainer.add(pNotif, BorderLayout.EAST);
        bodyPanel.add(tabHeaderContainer, BorderLayout.CENTER);
        return bodyPanel;
    }

    private JComponent buildDrugListContent() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);

        splitPane.setLeftComponent(buildTreePanel());
        splitPane.setRightComponent(buildCatalogRightPanel());
        return splitPane;
    }

    private JPanel buildTreePanel() {
        JPanel treeContainer = new JPanel(new BorderLayout());
        treeContainer.setBackground(Color.WHITE);
        treeContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(205, 214, 226)));

        JLabel lblTreeHeader = new JLabel("Nhà thuốc");
        lblTreeHeader.setFont(UngDungPhongChu.SMALL_BOLD);
        lblTreeHeader.setForeground(new Color(135, 20, 20));
        lblTreeHeader.setBorder(new EmptyBorder(4, 6, 4, 6));
        treeContainer.add(lblTreeHeader, BorderLayout.NORTH);

        categoryTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Tất Cả Thuốc (0)"));
        categoryTree = new JTree(categoryTreeModel);
        categoryTree.setFont(UngDungPhongChu.SMALL_BOLD);
        categoryTree.setRootVisible(true);
        categoryTree.setRowHeight(22);
        categoryTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean exp, boolean leaf, int row, boolean hasFocus) {
                JLabel l = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, hasFocus);
                l.setFont(UngDungPhongChu.SMALL_BOLD);
                l.setOpaque(sel);
                if (sel) {
                    l.setBackground(new Color(0, 120, 215));
                    l.setForeground(Color.WHITE);
                } else {
                    l.setForeground(new Color(25, 25, 25));
                }
                return l;
            }
        });
        categoryTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) categoryTree.getLastSelectedPathComponent();
            if (node == null) return;
            String label = String.valueOf(node.getUserObject());
            activeTypeFilter = label.startsWith("Thuốc") ? "THUOC" : (label.startsWith("Vật Tư") ? "VAT_TU" : "ALL");
            applyTypeFilter();
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

        JButton btnBrowseDrug = ExaminationUiFactory.createMiniBrowseButton(22, 23);
        rightContent.add(buildEntryPanel(btnBrowseDrug), BorderLayout.NORTH);
        autocompleteSupport = new DonThuocAutocompleteSupport(txtDrugName, btnBrowseDrug, allMedicines, this::selectMedicineItem);

        String[] cols = {"Tên Thuốc", "Đơn Vị", "Quy Cách", "Tồn kho", "Giá Bán BV", "Đối Tượng"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        medicineTable = new JTable(tableModel);
        medicineTable.setFont(UngDungPhongChu.SMALL);
        medicineTable.setRowHeight(21);
        medicineTable.setGridColor(new Color(230, 235, 243));
        medicineTable.setSelectionBackground(new Color(214, 232, 255));
        medicineTable.setSelectionForeground(new Color(15, 20, 25));
        medicineTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int r = medicineTable.getSelectedRow();
                if (r >= 0 && r < filteredMedicines.size()) selectMedicineItem(filteredMedicines.get(r));
            }
        });

        JTableHeader header = medicineTable.getTableHeader();
        header.setFont(UngDungPhongChu.SMALL_BOLD);
        header.setBackground(new Color(242, 246, 252));
        header.setForeground(new Color(30, 35, 45));
        header.setPreferredSize(new Dimension(100, 24));

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);

        int[] widths = {280, 60, 100, 65, 85, 75};
        for (int i = 0; i < widths.length; i++) medicineTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        medicineTable.getColumnModel().getColumn(1).setCellRenderer(centerRender);
        medicineTable.getColumnModel().getColumn(3).setCellRenderer(rightRender);
        medicineTable.getColumnModel().getColumn(4).setCellRenderer(rightRender);
        medicineTable.getColumnModel().getColumn(5).setCellRenderer(centerRender);

        JScrollPane tableScroll = new JScrollPane(medicineTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);
        rightContent.add(tableScroll, BorderLayout.CENTER);

        orderStatusBar = new JPanel(new BorderLayout());
        orderStatusBar.setBackground(new Color(0, 120, 215));
        orderStatusBar.setPreferredSize(new Dimension(800, 22));
        orderStatusBar.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
        lblOrderStar = new JLabel("", new ExaminationOrderIcons.StarVectorIcon(), SwingConstants.LEFT);
        lblOrderStar.setFont(UngDungPhongChu.SMALL_BOLD);
        lblOrderStar.setForeground(Color.WHITE);
        orderStatusBar.add(lblOrderStar, BorderLayout.CENTER);
        orderStatusBar.setVisible(false);
        rightContent.add(orderStatusBar, BorderLayout.SOUTH);

        return rightContent;
    }

    private JPanel buildEntryPanel(JButton btnBrowseDrug) {
        JPanel entryPanel = new JPanel(new GridBagLayout());
        entryPanel.setBackground(new Color(248, 250, 253));
        entryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 218, 228)),
                new EmptyBorder(4, 6, 4, 6)));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 3, 2, 3);

        c.gridy = 0; c.gridx = 0; entryPanel.add(createFieldLabel("Tên thuốc:"), c);
        JPanel pDrugName = new JPanel(new BorderLayout(2, 0));
        pDrugName.setOpaque(false);
        txtDrugName = new JTextField();
        txtDrugName.setFont(UngDungPhongChu.BODY);
        txtDrugName.setPreferredSize(new Dimension(320, 23));
        txtDrugName.setBackground(Color.WHITE);
        pDrugName.add(txtDrugName, BorderLayout.CENTER);
        pDrugName.add(btnBrowseDrug, BorderLayout.EAST);
        c.gridx = 1; c.gridwidth = 4; entryPanel.add(pDrugName, c);

        c.gridwidth = 1;
        c.gridx = 5; entryPanel.add(createFieldLabel("Tồn kho:"), c);
        txtKho = new JTextField("0");
        txtKho.setPreferredSize(new Dimension(55, 23));
        txtKho.setEditable(false);
        txtKho.setBackground(Color.WHITE);
        txtKho.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridx = 6; entryPanel.add(txtKho, c);
        c.gridx = 7; entryPanel.add(createFieldLabel("Đóng gói:"), c);
        txtPack = new JTextField();
        txtPack.setPreferredSize(new Dimension(70, 23));
        c.gridx = 8; c.gridwidth = 2; entryPanel.add(txtPack, c);

        c.gridwidth = 1; c.gridy = 1;
        c.gridx = 0; entryPanel.add(createFieldLabel("Đơn vị:"), c);
        txtUnit = new JTextField();
        txtUnit.setPreferredSize(new Dimension(65, 23));
        c.gridx = 1; entryPanel.add(txtUnit, c);
        c.gridx = 2; entryPanel.add(createFieldLabel("Số lượng:"), c);
        txtQty = new JTextField("0");
        txtQty.setFont(UngDungPhongChu.BODY_BOLD);
        txtQty.setPreferredSize(new Dimension(45, 23));
        txtQty.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridx = 3; entryPanel.add(txtQty, c);

        c.gridx = 4; entryPanel.add(createFieldLabel("Đường dùng:"), c);
        JPanel pRoute = new JPanel(new BorderLayout(2, 0));
        pRoute.setOpaque(false);
        txtRoute = new JTextField();
        txtRoute.setPreferredSize(new Dimension(120, 23));
        pRoute.add(txtRoute, BorderLayout.CENTER);
        pRoute.add(ExaminationUiFactory.createMiniBrowseButton(22, 23), BorderLayout.EAST);
        c.gridx = 5; c.gridwidth = 2; entryPanel.add(pRoute, c);

        c.gridwidth = 1; c.gridx = 7; entryPanel.add(createFieldLabel("HDSD:"), c);
        JPanel pHdsd = new JPanel(new BorderLayout(2, 0));
        pHdsd.setOpaque(false);
        txtHdsd = new JTextField();
        txtHdsd.setPreferredSize(new Dimension(280, 23));
        pHdsd.add(txtHdsd, BorderLayout.CENTER);
        pHdsd.add(ExaminationUiFactory.createMiniBrowseButton(22, 23), BorderLayout.EAST);
        c.gridx = 8; c.gridwidth = 2; entryPanel.add(pHdsd, c);

        c.gridwidth = 1; c.gridy = 2;
        c.gridx = 0; entryPanel.add(createFieldLabel("Ngày dùng:"), c);
        txtDays = new JTextField("0");
        txtDays.setPreferredSize(new Dimension(65, 23));
        txtDays.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridx = 1; entryPanel.add(txtDays, c);

        JPanel pDoseTimes = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pDoseTimes.setOpaque(false);
        txtMorning = new JTextField(); txtMorning.setPreferredSize(new Dimension(40, 23));
        txtNoon = new JTextField(); txtNoon.setPreferredSize(new Dimension(40, 23));
        txtAfternoon = new JTextField(); txtAfternoon.setPreferredSize(new Dimension(40, 23));
        txtNight = new JTextField(); txtNight.setPreferredSize(new Dimension(40, 23));
        pDoseTimes.add(new JLabel("Sáng:")); pDoseTimes.add(txtMorning);
        pDoseTimes.add(new JLabel("Trưa:")); pDoseTimes.add(txtNoon);
        pDoseTimes.add(new JLabel("Chiều:")); pDoseTimes.add(txtAfternoon);
        pDoseTimes.add(new JLabel("Tối:")); pDoseTimes.add(txtNight);
        c.gridx = 2; c.gridwidth = 5; entryPanel.add(pDoseTimes, c);

        JPanel pOkButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pOkButtons.setOpaque(false);
        btnOk = new JButton(" OK (Ctrl+A)", new ExaminationOrderIcons.CheckmarkBlueIcon(new Color(160, 165, 175)));
        btnOk.setFont(UngDungPhongChu.BODY_BOLD);
        btnOk.setBackground(new Color(238, 241, 246));
        btnOk.setForeground(new Color(150, 155, 165));
        btnOk.setOpaque(true);
        btnOk.setFocusPainted(false);
        btnOk.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOk.setPreferredSize(new Dimension(115, 24));
        btnOk.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(205, 212, 222), 1), new EmptyBorder(1, 4, 1, 4)));
        btnOk.addActionListener(e -> onConfirmDrugOrder());

        pOkButtons.add(btnOk);
        c.gridwidth = 3; c.gridx = 7; entryPanel.add(pOkButtons, c);
        return entryPanel;
    }

    private void selectMedicineItem(MedicineSearchItem item) {
        if (item == null) return;
        this.selectedMedicineItem = item;
        autocompleteSupport.setDrugTextProgrammatically(item.name);
        txtDrugName.setBackground(Color.WHITE);
        autocompleteSupport.hidePopup();

        txtUnit.setText(item.unit);
        txtPack.setText(item.pack);
        txtRoute.setText(item.route);
        txtHdsd.setText(item.hdsd != null && !item.hdsd.isBlank() ? item.hdsd : "Uống theo chỉ định bác sĩ");
        txtKho.setText(item.stock != null ? item.stock : "0");
        if (txtQty.getText().trim().isEmpty() || "0".equals(txtQty.getText().trim())) txtQty.setText("10");
        if (txtDays.getText().trim().isEmpty() || "0".equals(txtDays.getText().trim())) txtDays.setText("5");

        txtHdsd.setBackground(new Color(255, 255, 224));
        txtQty.requestFocusInWindow();
        txtQty.selectAll();
        setOkButtonHighlighted(true);
    }

    public void setOkButtonHighlighted(boolean highlight) {
        if (btnOk == null) return;
        btnOk.setEnabled(highlight);
        btnOk.setBackground(highlight ? new Color(255, 241, 168) : new Color(238, 241, 246));
        btnOk.setForeground(highlight ? new Color(20, 20, 20) : new Color(150, 155, 165));
        btnOk.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(highlight ? new Color(212, 163, 0) : new Color(205, 212, 222), 1),
                new EmptyBorder(1, 4, 1, 4)));
        btnOk.setIcon(new ExaminationOrderIcons.CheckmarkBlueIcon(
                highlight ? new Color(0, 140, 60) : new Color(160, 165, 175)));
        btnOk.repaint();
    }

    private void onConfirmDrugOrder() {
        String name = txtDrugName.getText().trim();
        if (name.isEmpty()) return;
        MedicineSearchItem matched = selectedMedicineItem;
        if (matched == null || !matched.name.equalsIgnoreCase(name)) {
            for (MedicineSearchItem cand : allMedicines) {
                if (cand.name.equalsIgnoreCase(name) || cand.code.equalsIgnoreCase(name)) {
                    matched = cand;
                    break;
                }
            }
        }
        if (matched == null || matched.itemId <= 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thuốc hợp lệ từ danh mục CSDL trước khi bấm OK.",
                    "Thuốc chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal morning = ExaminationUiFactory.parseDecimalSafe(txtMorning.getText());
        BigDecimal noon = ExaminationUiFactory.parseDecimalSafe(txtNoon.getText());
        BigDecimal afternoon = ExaminationUiFactory.parseDecimalSafe(txtAfternoon.getText());
        BigDecimal night = ExaminationUiFactory.parseDecimalSafe(txtNight.getText());
        int days = (int) Math.max(0, ExaminationUiFactory.parseDecimalSafe(txtDays.getText()).longValue());

        BigDecimal qty = ExaminationUiFactory.parseDecimalSafe(txtQty.getText());
        if (qty.compareTo(BigDecimal.ZERO) <= 0 && days > 0) {
            qty = morning.add(noon).add(afternoon).add(night).multiply(BigDecimal.valueOf(days));
        }
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "Số lượng thuốc kê phải lớn hơn 0.", "Thiếu số lượng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String route = !txtRoute.getText().trim().isEmpty() ? txtRoute.getText().trim() : matched.route;
        String hdsd = !txtHdsd.getText().trim().isEmpty() ? txtHdsd.getText().trim() : "Uống theo chỉ định bác sĩ";
        PrescribedDraftItem draft = new PrescribedDraftItem(
                matched.itemId, matched.code, matched.name,
                !txtUnit.getText().trim().isEmpty() ? txtUnit.getText().trim() : matched.unit,
                qty, matched.priceValue, route,
                buildDosageSummary(morning, noon, afternoon, night, matched.unit),
                days > 0 ? days + " ngày" : "Theo đơn", days > 0 ? days : null,
                morning, noon, afternoon, night, hdsd);

        draftModel.addOrReplace(draft);
        syncDraftUi();
        resetDrugEntryFields();
    }

    public void removeDraftItemAt(int index) {
        draftModel.removeAt(index);
        syncDraftUi();
    }

    private void resetDrugEntryFields() {
        selectedMedicineItem = null;
        autocompleteSupport.setDrugTextProgrammatically("");
        txtDrugName.setBackground(Color.WHITE);
        txtUnit.setText("");
        txtPack.setText("");
        txtKho.setText("0");
        txtQty.setText("0");
        txtRoute.setText("");
        txtHdsd.setText("");
        txtHdsd.setBackground(Color.WHITE);
        txtDays.setText("0");
        txtMorning.setText("");
        txtNoon.setText("");
        txtAfternoon.setText("");
        txtNight.setText("");
        setOkButtonHighlighted(false);
    }

    public void showSearchState(String query) { autocompleteSupport.populateForSearchDemo(query); }

    public void showSelectedMedicineDemoState(String code) {
        for (MedicineSearchItem item : allMedicines) {
            if (item.code.equals(code)) { selectMedicineItem(item); break; }
        }
    }

    public void showPrescribedAfterOkDemoState() { onConfirmDrugOrder(); }
    public JPopupMenu getSuggestPopup() { return autocompleteSupport.getSuggestPopup(); }

    private JPanel buildFooterSection() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(245, 248, 252));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(205, 214, 226)),
                new EmptyBorder(4, 10, 4, 10)));
        footer.setPreferredSize(new Dimension(1000, 48));

        JPanel statsPanel = new JPanel(new GridLayout(2, 5, 10, 2));
        statsPanel.setOpaque(false);
        Color red = new Color(185, 25, 25);
        Color navy = new Color(0, 45, 140);
        lblValTienDon = metricValue("0", red);
        lblValTongChiPhi = metricValue("0", red);
        lblValBhyt = metricValue("0", red);
        lblValNguonKhac = metricValue("0", red);
        lblValBenhNhan = metricValue("0", red);
        lblValTamUng = metricValue("0", navy);
        lblValDaThu = metricValue("0", navy);
        lblValMienGiam = metricValue("0", navy);
        lblValConDu = metricValue("0", navy);

        statsPanel.add(ExaminationUiFactory.createMetricBox("Tiền đơn", lblValTienDon));
        statsPanel.add(ExaminationUiFactory.createMetricBox("Tổng chi phí:", lblValTongChiPhi));
        statsPanel.add(ExaminationUiFactory.createMetricBox("BHYT:", lblValBhyt));
        statsPanel.add(ExaminationUiFactory.createMetricBox("Nguồn khác:", lblValNguonKhac));
        statsPanel.add(ExaminationUiFactory.createMetricBox("Bệnh nhân:", lblValBenhNhan));
        statsPanel.add(new JLabel(""));
        statsPanel.add(ExaminationUiFactory.createMetricBox("Tạm ứng:", lblValTamUng));
        statsPanel.add(ExaminationUiFactory.createMetricBox("Đã thu:", lblValDaThu));
        statsPanel.add(ExaminationUiFactory.createMetricBox("Miễn giảm:", lblValMienGiam));
        statsPanel.add(ExaminationUiFactory.createMetricBox("Còn dư:", lblValConDu));
        footer.add(statsPanel, BorderLayout.CENTER);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        rightButtons.setOpaque(false);
        JButton btnPrint = ExaminationUiFactory.createFooterButton(" In (Ctrl + P) ▾",
                new ExaminationOrderIcons.SuppliesToolbarIcon("print"), 140, null);
        btnPrint.addActionListener(e -> openDraftPrintPreview());
        JButton btnSave = ExaminationUiFactory.createFooterButton(" Lưu (Ctrl + S)",
                new ExaminationOrderIcons.FloppyDiskIcon(new Color(125, 45, 135)), 135, null);
        btnSave.addActionListener(e -> handleSavePrescription(false));
        JButton btnSaveAndClose = ExaminationUiFactory.createFooterButton(" Lưu + Đóng",
                new ExaminationOrderIcons.FloppyDiskIcon(new Color(40, 145, 75)), 125, null);
        btnSaveAndClose.addActionListener(e -> handleSavePrescription(true));
        JButton btnClose = ExaminationUiFactory.createFooterButton(" Đóng",
                new ExaminationOrderIcons.CloseCrossIcon(), 86, new Color(175, 30, 30));
        btnClose.addActionListener(e -> closeWithoutSaving());

        rightButtons.add(btnPrint);
        rightButtons.add(btnSave);
        rightButtons.add(btnSaveAndClose);
        rightButtons.add(btnClose);
        footer.add(rightButtons, BorderLayout.EAST);
        return footer;
    }

    public void closeWithoutSaving() {
        if (!draftModel.isEmpty() && !GraphicsEnvironment.isHeadless()
                && (System.getProperty("surefire.test.class.path") == null
                || System.getProperty("surefire.test.class.path").isBlank())) {
            int ret = JOptionPane.showConfirmDialog(this,
                    "Đơn thuốc đang kê chưa được lưu. Bạn có chắc muốn đóng và quay lại màn hình Khám Bệnh?",
                    "Xác Nhận Đóng", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ret != JOptionPane.YES_OPTION) return;
        }
        clearDraftPrescription();
        if (onCloseCallback != null) onCloseCallback.run();
    }

    private void handleSavePrescription(boolean andClose) {
        if (services == null || services.prescribeMedication() == null || currentVisitId <= 0
                || currentDoctorId <= 0 || currentFacilityId <= 0 || KhamBenhPhienNguCanh.current() == null) {
            JOptionPane.showMessageDialog(this,
                    "Chưa có lượt khám, bác sĩ hoặc phiên làm việc hợp lệ để kê đơn.",
                    "Không thể lưu đơn thuốc", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedMedicineItem != null && !txtDrugName.getText().trim().isEmpty()) onConfirmDrugOrder();
        if (draftModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thuốc và bấm OK để thêm vào đơn trước khi lưu.",
                    "Đơn thuốc trống", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PrescribeThuocCommand cmd = buildPrescribeCommand(KhamBenhPhienNguCanh.current());
        Integer selectedIcdId = icdSelector != null ? icdSelector.getSelectedIcdId() : null;
        String diagnosisText = icdSelector != null ? icdSelector.getDiagnosisTextOnly() : "";
        String diagnosisDisplay = icdSelector != null ? icdSelector.getDiagnosisDisplay() : "";
        String previousDiagnosis = currentPatient != null && currentPatient.diagnosis != null ? currentPatient.diagnosis : "";
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
                        String code = receipt != null ? "DT-" + receipt.prescriptionId() : "DT-SUCCESS";
                        clearDraftPrescription();
                        JOptionPane.showMessageDialog(DonThuocYeuCauManHinh.this,
                                "Đã lưu đơn thuốc vào CSDL thành công!\n▶ Mã đơn: " + code,
                                "Kê Đơn Thuốc Thành Công", JOptionPane.INFORMATION_MESSAGE);
                        if (andClose && onCloseCallback != null) onCloseCallback.run();
                    } else {
                        String errMsg = (res != null && res.getError().isPresent()) ? res.getError().get().message() : "Lỗi không xác định";
                        JOptionPane.showMessageDialog(DonThuocYeuCauManHinh.this, "Không thể lưu đơn thuốc: " + errMsg,
                                "Lỗi Nghiệp Vụ", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DonThuocYeuCauManHinh.this, "Lỗi khi thực thi giao dịch: " + ex.getMessage(),
                            "Lỗi Hệ Thống", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public PrescribeThuocCommand buildPrescribeCommand(PhanQuyenNguCanh auth) {
        String advice = txtDoctorAdvice != null ? txtDoctorAdvice.getText() : "";
        return draftModel.buildPrescribeCommand(currentVisitId, currentDoctorId, currentFacilityId, advice, auth);
    }

    private void rebuildCategoryTree() {
        if (categoryTreeModel == null) return;
        int drugCount = 0;
        int supplyCount = 0;
        for (MedicineSearchItem m : allMedicines) {
            if ("VAT_TU".equalsIgnoreCase(m.itemType)) supplyCount++; else drugCount++;
        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Tất Cả Thuốc (" + allMedicines.size() + ")");
        root.add(new DefaultMutableTreeNode("Thuốc (" + drugCount + ")"));
        if (supplyCount > 0) root.add(new DefaultMutableTreeNode("Vật Tư (" + supplyCount + ")"));
        categoryTreeModel.setRoot(root);
        ExaminationUiFactory.expandAllNodes(categoryTree, 0, categoryTree.getRowCount());
    }

    private void applyTypeFilter() {
        if (tableModel == null) return;
        filteredMedicines.clear();
        tableModel.setRowCount(0);
        for (MedicineSearchItem m : allMedicines) {
            if ("ALL".equals(activeTypeFilter) || activeTypeFilter.equalsIgnoreCase(m.itemType)) {
                filteredMedicines.add(m);
                tableModel.addRow(new Object[]{m.name, m.unit, m.pack, m.stock, m.price, ""});
            }
        }
    }

    private void syncDraftUi() {
        refreshDraftTable();
        updateFinancialSummary();
        if (mainTabs != null && mainTabs.getTabCount() > 1) {
            mainTabs.setTitleAt(1, "Đơn Đang Kê (" + draftModel.size() + ")");
        }
        if (orderStatusBar != null && lblOrderStar != null) {
            if (draftModel.isEmpty()) {
                orderStatusBar.setVisible(false);
                lblOrderStar.setText("");
            } else {
                orderStatusBar.setVisible(true);
                lblOrderStar.setIcon(new ExaminationOrderIcons.StarVectorIcon());
                lblOrderStar.setText(draftModel.buildStatusSummaryText());
            }
        }
    }

    private void refreshDraftTable() {
        if (draftOrderTabPanel != null) draftOrderTabPanel.setRows(draftModel.toTableRows());
    }

    private void clearDraftPrescription() {
        draftModel.clear();
        syncDraftUi();
        resetDrugEntryFields();
    }

    public boolean isOrderStatusBarVisible() { return orderStatusBar != null && orderStatusBar.isVisible(); }
    public String getOrderStatusText() { return lblOrderStar != null ? lblOrderStar.getText() : ""; }

    private void updateFinancialSummary() {
        String formatted = ExaminationUiFactory.formatMoney(draftModel.calculateTotal());
        if (lblValTienDon != null) lblValTienDon.setText(formatted);
        if (lblValTongChiPhi != null) lblValTongChiPhi.setText(formatted);
        if (lblValBenhNhan != null) lblValBenhNhan.setText(formatted);
        if (lblValBhyt != null) lblValBhyt.setText("0");
        if (lblValNguonKhac != null) lblValNguonKhac.setText("0");
        if (lblValTamUng != null) lblValTamUng.setText("0");
        if (lblValDaThu != null) lblValDaThu.setText("0");
        if (lblValMienGiam != null) lblValMienGiam.setText("0");
        if (lblValConDu != null) lblValConDu.setText(formatted);
    }

    private void openDraftPrintPreview() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        String doctor = txtOrderDoctor != null ? txtOrderDoctor.getText().trim() : defaultDoctorLabel();
        String diag = icdSelector != null ? icdSelector.getDiagnosisDisplay() : "";
        new DonThuocPrintPreviewHopThoai(owner, draftModel.buildPrintData(currentPatient, diag, doctor)).setVisible(true);
    }

    private List<vn.clinicmanagement.domain.examination.ICD> fetchIcdCatalogFromServices() {
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

    public IcdDiagnosisSelectorSupport getIcdSelector() {
        return icdSelector;
    }

    private static String buildDosageSummary(BigDecimal m, BigDecimal n, BigDecimal a, BigDecimal ni, String unit) {
        StringBuilder sb = new StringBuilder();
        if (m.signum() > 0) sb.append("Sáng ").append(m.stripTrailingZeros().toPlainString());
        if (n.signum() > 0) sb.append(sb.isEmpty() ? "" : ", ").append("Trưa ").append(n.stripTrailingZeros().toPlainString());
        if (a.signum() > 0) sb.append(sb.isEmpty() ? "" : ", ").append("Chiều ").append(a.stripTrailingZeros().toPlainString());
        if (ni.signum() > 0) sb.append(sb.isEmpty() ? "" : ", ").append("Tối ").append(ni.stripTrailingZeros().toPlainString());
        return sb.isEmpty() ? "Theo chỉ định" : sb + " " + (unit != null ? unit : "");
    }

    private static String defaultDoctorLabel() {
        var ctx = KhamBenhPhienNguCanh.current();
        return (ctx != null && ctx.employeeId() > 0)
                ? (ctx.roleCode() != null ? ctx.roleCode() : "BS") + " #" + ctx.employeeId()
                : "Bác sĩ điều trị";
    }

    public int getCatalogRowCount() { return tableModel != null ? tableModel.getRowCount() : 0; }
    public int getDraftItemCount() { return draftModel.size(); }
    public String getBannerPatientText() { return patientBanner != null ? patientBanner.getLine1Text() : ""; }
    public String getTienDonText() { return lblValTienDon != null ? lblValTienDon.getText() : ""; }
    public String getTongChiPhiText() { return lblValTongChiPhi != null ? lblValTongChiPhi.getText() : ""; }

    private static JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.SMALL);
        l.setForeground(new Color(30, 35, 45));
        return l;
    }

    private static JComboBox<String> createDropdown(String defaultVal, int width) {
        JComboBox<String> cb = new JComboBox<>(new String[]{defaultVal});
        cb.setFont(UngDungPhongChu.SMALL);
        cb.setBackground(Color.WHITE);
        cb.setPreferredSize(new Dimension(width, 23));
        return cb;
    }

    private static JLabel metricValue(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.BODY_BOLD);
        l.setForeground(color);
        return l;
    }
}
