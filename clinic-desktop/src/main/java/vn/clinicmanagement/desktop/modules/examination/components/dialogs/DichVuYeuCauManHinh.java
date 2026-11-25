package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.desktop.modules.examination.components.menus.DichVuNguCanhMenu;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

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
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.cls.cancel.CancelDichVuYeuCauBienNhan;
import vn.clinicmanagement.application.cls.cancel.CancelDichVuYeuCauCommand;
import vn.clinicmanagement.application.cls.DichVuYeuCauDuLieu;
import vn.clinicmanagement.application.cls.get.GetDichVuTheoLuotKhamQuery;
import vn.clinicmanagement.application.cls.request.YeuCauDichVuCommand;
import vn.clinicmanagement.application.cls.request.YeuCauDichVuBienNhan;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.print.examination.LamSangYeuCauPrintPreviewHopThoai;
import vn.clinicmanagement.desktop.modules.print.examination.LamSangYeuCauPrintPreviewManHinh;

/**
 * Giao diện màn hình: "ĐHC - Chỉ Định Dịch Vụ"
 * Chuyển hướng khi nhấn nút [Dịch Vụ] hoặc [Chỉ Định] từ bàn khám.
 * Tải danh mục dịch vụ cận lâm sàng (Xét nghiệm, CĐHA, DV kỹ thuật khác) trực tiếp từ CSDL `DanhMucDichVu`
 * theo phân loại `loaiDichVu` và `nhomDichVu`.
 */
public class DichVuYeuCauManHinh extends JPanel {

    private static final String SEARCH_PLACEHOLDER = "Tìm Kiếm (Ctrl+F)";
    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DecimalFormat MONEY_FORMAT;
    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        symbols.setGroupingSeparator(',');
        MONEY_FORMAT = new DecimalFormat("#,##0", symbols);
    }

    public static class ServiceItem {
        public int serviceId;
        public String serviceType;
        public String category;
        public String code;
        public String pd;
        public String name;
        public int quantity;
        public BigDecimal bhytPriceValue;
        public BigDecimal unitPriceValue;
        public String giaBhyt;
        public String giaVienPhi;
        public String giaDichVu;
        public String canNop;
        public String doiTuong;
        public String phongThucHien;
        public boolean isSelected;

        public ServiceItem(String category, String code, String pd, String name, int quantity,
                           String giaBhyt, String giaVienPhi, String giaDichVu, String canNop,
                           String doiTuong, String phongThucHien, boolean isSelected) {
            this(0, "XET_NGHIEM", category, code, pd, name, quantity,
                    parseAmount(giaBhyt), parseAmount(giaVienPhi),
                    giaBhyt, giaVienPhi, giaDichVu, canNop, doiTuong, phongThucHien, isSelected);
        }

        public ServiceItem(int serviceId, String serviceType, String category, String code, String pd, String name,
                           int quantity, BigDecimal bhytPriceValue, BigDecimal unitPriceValue,
                           String giaBhyt, String giaVienPhi, String giaDichVu, String canNop,
                           String doiTuong, String phongThucHien, boolean isSelected) {
            this.serviceId = serviceId;
            this.serviceType = serviceType != null && !serviceType.isBlank() ? serviceType : "XET_NGHIEM";
            this.category = category != null && !category.isBlank() ? category : "Khác";
            this.code = code != null ? code : "";
            this.pd = pd != null ? pd : "";
            this.name = name != null ? name : "";
            this.quantity = quantity;
            this.bhytPriceValue = bhytPriceValue != null ? bhytPriceValue : BigDecimal.ZERO;
            this.unitPriceValue = unitPriceValue != null ? unitPriceValue : BigDecimal.ZERO;
            this.giaBhyt = giaBhyt != null ? giaBhyt : "0";
            this.giaVienPhi = giaVienPhi != null ? giaVienPhi : "0";
            this.giaDichVu = giaDichVu != null ? giaDichVu : "0";
            this.canNop = canNop != null ? canNop : "0";
            this.doiTuong = doiTuong != null ? doiTuong : "";
            this.phongThucHien = phongThucHien != null ? phongThucHien : "";
            this.isSelected = isSelected;
        }
    }

    private enum CategoryLevel { ALL, TYPE, GROUP }

    private static final class CategoryNodeData {
        final CategoryLevel level;
        final String serviceType;
        final String groupName;
        final String label;

        CategoryNodeData(CategoryLevel level, String serviceType, String groupName, int count) {
            this.level = level;
            this.serviceType = serviceType;
            this.groupName = groupName;
            String baseName = switch (level) {
                case ALL -> "Dịch vụ cận lâm sàng";
                case TYPE -> serviceTypeDisplayName(serviceType);
                case GROUP -> groupName;
            };
            this.label = baseName + " (" + count + ")";
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final Window ownerWindow;
    private final Runnable onCloseCallback;
    private final KhamBenhGhepNoiGoc.Services services;
    private int currentVisitId = 0;
    private int currentDoctorId = 0;
    private int currentFacilityId = 0;
    private boolean currentPatientHasBhyt = false;
    private int currentPatientBhytRate = 0;

    private JTable serviceTable;
    private DefaultTableModel tableModel;
    private JCheckBox chkPrintCls;
    private String activeSelectedServiceCode;
    private JLabel itemLiqui;
    private JLabel itemAmh;

    private final List<ServiceItem> allServices = new ArrayList<>();
    private final List<ServiceItem> currentFilteredServices = new ArrayList<>();
    private final List<ServiceItem> selectedServices = new ArrayList<>();
    private JTree categoryTree;
    private DefaultTreeModel categoryTreeModel;
    private JLabel lblAllCategories;
    private JTextField txtSearch;
    private CategoryNodeData currentCategoryNode;
    private JPanel selectedBar;

    private JLabel lblBannerPatient;
    private JLabel lblBannerVisit;
    private JLabel lblBannerAddress;
    private JLabel lblBannerBadge;
    private JTextField txtOrderDate;
    private JTextField txtIcdCode;
    private JTextField txtIcdName;
    private JComboBox<String> cboOrderDoctor;
    private JTextField txtOrderNote;

    private JLabel lCost;
    private JLabel lBhyt;
    private JLabel lBenhNhan;
    private JLabel lTamUng;
    private JLabel lDaThu;
    private JLabel lConDu;
    private String currentCategoryFilter = "Tất cả";

    private KhamBenhManHinh.AdminPatientItem currentPatient;
    private DefaultTableModel orderedTableModel;
    private JTable orderedServicesTable;
    private final List<DichVuYeuCauDuLieu> orderedVisitServices = new ArrayList<>();

    public DichVuYeuCauManHinh() {
        this((Runnable) null, resolveDefaultServices(), true);
    }

    public DichVuYeuCauManHinh(Runnable onCloseCallback) {
        this(onCloseCallback, resolveDefaultServices(), true);
    }

    public DichVuYeuCauManHinh(Runnable onCloseCallback, KhamBenhGhepNoiGoc.Services services) {
        this(onCloseCallback, services, true);
    }

    public DichVuYeuCauManHinh(Runnable onCloseCallback, KhamBenhGhepNoiGoc.Services services, boolean loadFromDb) {
        this.ownerWindow = null;
        this.onCloseCallback = onCloseCallback;
        this.services = services != null ? services : resolveDefaultServices();
        initPanel(loadFromDb);
    }

    public DichVuYeuCauManHinh(Window ownerWindow) {
        this(ownerWindow, resolveDefaultServices());
    }

    public DichVuYeuCauManHinh(Window ownerWindow, KhamBenhGhepNoiGoc.Services services) {
        this.ownerWindow = ownerWindow;
        this.onCloseCallback = () -> {
            if (ownerWindow != null) {
                ownerWindow.dispose();
            }
        };
        this.services = services != null ? services : resolveDefaultServices();
        initPanel(true);
    }

    private static KhamBenhGhepNoiGoc.Services resolveDefaultServices() {
        try {
            return KhamBenhGhepNoiGoc.createDefault();
        } catch (Exception ex) {
            return null;
        }
    }

    public void setVisitContext(int visitId, int doctorId, int facilityId) {
        boolean visitChanged = this.currentVisitId != visitId;
        this.currentVisitId = visitId;
        this.currentDoctorId = doctorId;
        this.currentFacilityId = facilityId;
        if (visitChanged) {
            clearSelectedServices();
        }
        refreshOrderedServicesForVisit();
    }

    /** Cập nhật thông tin hành chính bệnh nhân & y lệnh từ hàng đợi bàn khám. */
    public void setPatientContext(KhamBenhManHinh.AdminPatientItem patient) {
        this.currentPatient = patient;
        if (patient == null) {
            return;
        }
        if (patient.visitId > 0 && this.currentVisitId != patient.visitId) {
            this.currentVisitId = patient.visitId;
            clearSelectedServices();
            refreshOrderedServicesForVisit();
        }
        String objLabel = patient.requestType != null && !patient.requestType.isBlank()
                ? patient.requestType : "Viện phí";
        this.currentPatientHasBhyt = objLabel.toUpperCase(Locale.ROOT).contains("BHYT")
                || objLabel.toUpperCase(Locale.ROOT).contains("BH");
        this.currentPatientBhytRate = patient.bhytRate > 0 ? patient.bhytRate : (currentPatientHasBhyt ? 80 : 0);

        if (lblBannerPatient != null) {
            String agePart = (patient.birthYear != null && !patient.birthYear.isBlank() ? patient.birthYear : "")
                    + (patient.queueAge != null && !patient.queueAge.isBlank() ? " (" + patient.queueAge + " tuổi)" : "");
            lblBannerPatient.setText(String.format("%s | %s%s | %s",
                    safeText(patient.name, "--"),
                    safeText(patient.code, "--"),
                    agePart.isBlank() ? "" : " | " + agePart,
                    safeText(patient.gender, "--")));
        }
        if (lblBannerVisit != null) {
            lblBannerVisit.setText("Mã VP : " + safeText(patient.invoiceCode, "--") + " | " + objLabel);
        }
        if (lblBannerAddress != null) {
            String addr = patient.bannerAddress != null && !patient.bannerAddress.isBlank()
                    ? patient.bannerAddress : patient.address;
            lblBannerAddress.setText(safeText(addr, "--"));
        }
        if (lblBannerBadge != null) {
            String badgeHtml = currentPatientHasBhyt
                    ? "<html><center><font color='#005A9E'><b>BHYT<br>" + currentPatientBhytRate + "%</b></font></center></html>"
                    : "<html><center><font color='#800000'><b>Viện<br>phí</b></font></center></html>";
            lblBannerBadge.setText(badgeHtml);
        }
        if (txtOrderDate != null) {
            String dateText = patient.admissionDate != null && !patient.admissionDate.isBlank()
                    ? patient.admissionDate : LocalDateTime.now().format(ORDER_TIME_FORMATTER);
            txtOrderDate.setText(dateText);
        }
        if (cboOrderDoctor != null) {
            String docName = patient.doctor != null && !patient.doctor.isBlank()
                    ? patient.doctor : defaultOperatorLabel();
            cboOrderDoctor.setModel(new DefaultComboBoxModel<>(new String[]{docName}));
        }
        if (txtIcdCode != null && txtIcdName != null) {
            applyDiagnosisText(patient.diagnosis);
        }
        if (txtOrderNote != null) {
            txtOrderNote.setText("");
        }
        updateFinancialTotals();
    }

    private void initPanel(boolean loadFromDb) {
        if (loadFromDb) {
            initMasterServices();
        }

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. THANH CÔNG CỤ PHỤ + BANNER THÔNG TIN BỆNH NHÂN + HÀNG Y LỆNH
        add(buildHeaderSection(), BorderLayout.NORTH);

        // 2. PHẦN THÂN CHÍNH: DẢI TAB + CÂY DANH MỤC + BẢNG DỊCH VỤ + DẢI DỊCH VỤ ĐÃ CHỌN
        add(buildBodySection(), BorderLayout.CENTER);

        // 3. THANH TÀI CHÍNH & CỤM NÚT LƯU CHÂN TRANG
        add(buildFooterSection(), BorderLayout.SOUTH);
        updateFinancialTotals();
    }

    private JPanel buildHeaderSection() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Color.WHITE);

        // Hàng 1: Sub-Toolbar công cụ + Banner bệnh nhân góc phải
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(new Color(246, 248, 252));
        topRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));
        topRow.setPreferredSize(new Dimension(1000, 48));

        // Nút công cụ bên trái
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        leftActions.setOpaque(false);

        leftActions.add(createSubToolbarButton("Menu", "menu", false));
        leftActions.add(createSubToolbarButton("Mẫu Chỉ Định", "template", true));
        leftActions.add(createSubToolbarButton("Phiếu Chỉ Định Dịch Vụ", "refresh", true));
        leftActions.add(createSubToolbarButton("Sử Dụng Phác Đồ", "protocol", true));
        if (onCloseCallback != null) {
            JButton btnBack = createSubToolbarButton("Quay lại", "menu", false);
            btnBack.addActionListener(e -> onCloseCallback.run());
            leftActions.add(btnBack);
        }

        // Banner bệnh nhân bên phải
        JPanel rightBanner = new JPanel(new BorderLayout(8, 0));
        rightBanner.setOpaque(false);
        rightBanner.setBorder(new EmptyBorder(2, 6, 2, 10));

        JPanel bannerText = new JPanel();
        bannerText.setLayout(new BoxLayout(bannerText, BoxLayout.Y_AXIS));
        bannerText.setOpaque(false);

        lblBannerPatient = new JLabel("Chưa chọn bệnh nhân | -- | --");
        lblBannerPatient.setFont(UngDungPhongChu.SMALL_BOLD);
        lblBannerPatient.setForeground(new Color(130, 0, 0));

        lblBannerVisit = new JLabel("Mã VP : -- | Viện phí");
        lblBannerVisit.setFont(UngDungPhongChu.SMALL);
        lblBannerVisit.setForeground(new Color(130, 0, 0));

        lblBannerAddress = new JLabel("--");
        lblBannerAddress.setFont(UngDungPhongChu.SMALL);
        lblBannerAddress.setForeground(new Color(80, 85, 95));

        bannerText.add(lblBannerPatient);
        bannerText.add(lblBannerVisit);
        bannerText.add(lblBannerAddress);

        lblBannerBadge = new JLabel("<html><center><font color='#800000'><b>Viện<br>phí</b></font></center></html>");
        lblBannerBadge.setFont(UngDungPhongChu.SMALL);
        lblBannerBadge.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(180, 50, 50), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        lblBannerBadge.setBackground(new Color(255, 245, 245));
        lblBannerBadge.setOpaque(true);

        rightBanner.add(bannerText, BorderLayout.CENTER);
        rightBanner.add(lblBannerBadge, BorderLayout.EAST);

        topRow.add(leftActions, BorderLayout.WEST);
        topRow.add(rightBanner, BorderLayout.EAST);
        header.add(topRow);

        // Hàng 2: Form thông tin y lệnh & chẩn đoán
        header.add(buildOrderFormPanel());

        return header;
    }

    private JPanel buildOrderFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(248, 250, 253));
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 222, 232)),
                new EmptyBorder(4, 8, 4, 8)
        ));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 3, 2, 3);

        // Hàng 1
        // Ngày y lệnh
        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        JLabel lblNgay = new JLabel("Ngày y lệnh:");
        lblNgay.setFont(UngDungPhongChu.SMALL);
        form.add(lblNgay, c);

        c.gridx = 1; c.gridy = 0; c.weightx = 0;
        txtOrderDate = new JTextField(LocalDateTime.now().format(ORDER_TIME_FORMATTER), 12);
        txtOrderDate.setFont(UngDungPhongChu.SMALL);
        txtOrderDate.setPreferredSize(new Dimension(130, 22));
        form.add(txtOrderDate, c);

        // Chẩn đoán
        c.gridx = 2; c.gridy = 0; c.weightx = 0;
        JLabel lblChanDoan = new JLabel("Chẩn đoán:");
        lblChanDoan.setFont(UngDungPhongChu.SMALL);
        form.add(lblChanDoan, c);

        c.gridx = 3; c.gridy = 0; c.weightx = 0.6;
        JPanel pnlChanDoan = new JPanel(new BorderLayout(2, 0));
        pnlChanDoan.setOpaque(false);

        txtIcdCode = new JTextField("", 4);
        txtIcdCode.setFont(UngDungPhongChu.SMALL);
        txtIcdCode.setPreferredSize(new Dimension(45, 22));

        JButton btnIcdDots1 = new JButton("...");
        btnIcdDots1.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnIcdDots1.setPreferredSize(new Dimension(20, 22));
        btnIcdDots1.setMargin(new Insets(0, 0, 0, 0));

        txtIcdName = new JTextField("", 18);
        txtIcdName.setFont(UngDungPhongChu.SMALL);

        JButton btnIcdDots2 = new JButton("...");
        btnIcdDots2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnIcdDots2.setPreferredSize(new Dimension(20, 22));
        btnIcdDots2.setMargin(new Insets(0, 0, 0, 0));

        JPanel leftIcd = new JPanel(new BorderLayout(2, 0));
        leftIcd.setOpaque(false);
        leftIcd.add(txtIcdCode, BorderLayout.WEST);
        leftIcd.add(btnIcdDots1, BorderLayout.EAST);

        JPanel rightIcd = new JPanel(new BorderLayout(2, 0));
        rightIcd.setOpaque(false);
        rightIcd.add(txtIcdName, BorderLayout.CENTER);
        rightIcd.add(btnIcdDots2, BorderLayout.EAST);

        pnlChanDoan.add(leftIcd, BorderLayout.WEST);
        pnlChanDoan.add(rightIcd, BorderLayout.CENTER);
        form.add(pnlChanDoan, c);

        // Link Bệnh kèm theo
        c.gridx = 4; c.gridy = 0; c.weightx = 0;
        JLabel lblBenhKemTheo = new JLabel("<html><u><font color='#0066CC'>Bệnh kèm theo</font></u></html>");
        lblBenhKemTheo.setFont(UngDungPhongChu.SMALL);
        lblBenhKemTheo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        form.add(lblBenhKemTheo, c);

        // Checkbox Cấp cứu
        c.gridx = 5; c.gridy = 0; c.weightx = 0;
        JCheckBox chkCapCuu = new JCheckBox("Cấp cứu");
        chkCapCuu.setFont(UngDungPhongChu.SMALL);
        chkCapCuu.setForeground(new Color(180, 20, 20));
        chkCapCuu.setOpaque(false);
        form.add(chkCapCuu, c);

        // Hàng 2
        // Người y lệnh
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        JLabel lblNguoiYLenh = new JLabel("Người y lệnh:");
        lblNguoiYLenh.setFont(UngDungPhongChu.SMALL);
        form.add(lblNguoiYLenh, c);

        c.gridx = 1; c.gridy = 1; c.weightx = 0;
        cboOrderDoctor = new JComboBox<>(new String[]{defaultOperatorLabel()});
        cboOrderDoctor.setFont(UngDungPhongChu.SMALL);
        cboOrderDoctor.setPreferredSize(new Dimension(130, 22));
        cboOrderDoctor.setBackground(Color.WHITE);
        form.add(cboOrderDoctor, c);

        // Ghi chú
        c.gridx = 2; c.gridy = 1; c.weightx = 0;
        JLabel lblGhiChu = new JLabel("Ghi chú:");
        lblGhiChu.setFont(UngDungPhongChu.SMALL);
        form.add(lblGhiChu, c);

        c.gridx = 3; c.gridy = 1; c.weightx = 0.6;
        txtOrderNote = new JTextField();
        txtOrderNote.setFont(UngDungPhongChu.SMALL);
        txtOrderNote.setPreferredSize(new Dimension(200, 22));
        form.add(txtOrderNote, c);

        // Phiếu điều trị
        c.gridx = 4; c.gridy = 1; c.weightx = 0;
        JPanel pnlPhieuDieuTri = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlPhieuDieuTri.setOpaque(false);
        JLabel lblPhieu = new JLabel("Phiếu điều trị:");
        lblPhieu.setFont(UngDungPhongChu.SMALL);
        JComboBox<String> cboPhieu = new JComboBox<>(new String[]{""});
        cboPhieu.setPreferredSize(new Dimension(75, 22));
        cboPhieu.setBackground(Color.WHITE);
        pnlPhieuDieuTri.add(lblPhieu);
        pnlPhieuDieuTri.add(cboPhieu);
        form.add(pnlPhieuDieuTri, c);

        // Link Thêm phiếu điều trị
        c.gridx = 5; c.gridy = 1; c.weightx = 0;
        JLabel lblThemPhieu = new JLabel("<html><u><font color='#0066CC'>Thêm phiếu điều trị</font></u></html>");
        lblThemPhieu.setFont(UngDungPhongChu.SMALL);
        lblThemPhieu.setCursor(new Cursor(Cursor.HAND_CURSOR));
        form.add(lblThemPhieu, c);

        return form;
    }

    private JPanel buildBodySection() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);

        // Dải Tab phụ: [Danh Sách Dịch Vụ] | [Phiếu Chỉ Định]
        JTabbedPane tabPane = new JTabbedPane();
        tabPane.setFont(UngDungPhongChu.SMALL_BOLD);
        tabPane.setBackground(new Color(230, 238, 248));
        tabPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(2, 4, 0, 4);
                contentBorderInsets = new Insets(1, 0, 0, 0);
                tabInsets = new Insets(3, 10, 3, 10);
            }
        });

        // Tab Danh Sách Dịch Vụ
        JPanel serviceTabContent = new JPanel(new BorderLayout());
        serviceTabContent.setBackground(Color.WHITE);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(195);
        splitPane.setDividerSize(4);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        // Cột trái: Cây danh mục dịch vụ
        splitPane.setLeftComponent(buildCategoryTreePanel());

        // Cột phải: Bảng danh mục dịch vụ + dải dịch vụ đã chọn bên dưới
        splitPane.setRightComponent(buildServiceListPanel());

        serviceTabContent.add(splitPane, BorderLayout.CENTER);

        tabPane.addTab("Danh Sách Dịch Vụ", serviceTabContent);
        tabPane.addTab("Phiếu Chỉ Định", buildOrderedServicesTabPanel());

        tabPane.addChangeListener(e -> {
            if (tabPane.getSelectedIndex() == 1) {
                refreshOrderedServicesForVisit();
            }
        });

        body.add(tabPane, BorderLayout.CENTER);
        return body;
    }

    private JPanel buildOrderedServicesTabPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(new Color(245, 248, 252));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 218, 228)));

        JButton btnRefreshOrders = new JButton("Làm mới phiếu");
        btnRefreshOrders.setFont(UngDungPhongChu.SMALL_BOLD);
        btnRefreshOrders.setBackground(Color.WHITE);
        btnRefreshOrders.setFocusPainted(false);
        btnRefreshOrders.addActionListener(e -> refreshOrderedServicesForVisit());

        JButton btnPrintOrderSheet = new JButton("Xem / In Phiếu Chỉ Định", new SaveAndPrintIcon());
        btnPrintOrderSheet.setFont(UngDungPhongChu.SMALL_BOLD);
        btnPrintOrderSheet.setBackground(Color.WHITE);
        btnPrintOrderSheet.setFocusPainted(false);
        btnPrintOrderSheet.addActionListener(e -> openOrderedServicesPrintPreview());

        JButton btnCancelSelectedOrder = new JButton("Hủy Chỉ Định Đã Chọn");
        btnCancelSelectedOrder.setFont(UngDungPhongChu.SMALL_BOLD);
        btnCancelSelectedOrder.setForeground(new Color(185, 25, 25));
        btnCancelSelectedOrder.setBackground(Color.WHITE);
        btnCancelSelectedOrder.setFocusPainted(false);
        btnCancelSelectedOrder.addActionListener(e -> handleCancelSelectedOrderedServices());

        toolbar.add(btnRefreshOrders);
        toolbar.add(btnPrintOrderSheet);
        toolbar.add(btnCancelSelectedOrder);
        panel.add(toolbar, BorderLayout.NORTH);

        String[] cols = {
                "Mã Chỉ Định", "Mã Dịch Vụ", "Tên Dịch Vụ", "Nhóm Dịch Vụ",
                "SL", "Đơn Giá", "Thành Tiền", "Trạng Thái", "Thời Gian Chỉ Định"
        };
        orderedTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        orderedServicesTable = new JTable(orderedTableModel);
        orderedServicesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        orderedServicesTable.setFont(UngDungPhongChu.SMALL);
        orderedServicesTable.setRowHeight(22);
        orderedServicesTable.setShowGrid(true);
        orderedServicesTable.setGridColor(new Color(230, 235, 243));
        orderedServicesTable.getTableHeader().setFont(UngDungPhongChu.SMALL_BOLD);
        orderedServicesTable.getTableHeader().setBackground(new Color(242, 246, 252));
        orderedServicesTable.getColumnModel().getColumn(0).setPreferredWidth(85);
        orderedServicesTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        orderedServicesTable.getColumnModel().getColumn(2).setPreferredWidth(290);
        orderedServicesTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        orderedServicesTable.getColumnModel().getColumn(4).setPreferredWidth(45);
        orderedServicesTable.getColumnModel().getColumn(5).setPreferredWidth(90);
        orderedServicesTable.getColumnModel().getColumn(6).setPreferredWidth(95);
        orderedServicesTable.getColumnModel().getColumn(7).setPreferredWidth(110);
        orderedServicesTable.getColumnModel().getColumn(8).setPreferredWidth(130);

        JScrollPane scroll = new JScrollPane(orderedServicesTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /** Tải danh sách chỉ định cận lâm sàng đã lưu của lượt khám hiện tại để hiển thị trên tab Phiếu Chỉ Định. */
    public void refreshOrderedServicesForVisit() {
        if (orderedTableModel == null) {
            return;
        }
        if (services == null || services.getServicesByVisit() == null || currentVisitId <= 0) {
            applyOrderedVisitServices(List.of());
            return;
        }
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) {
            applyOrderedVisitServices(List.of());
            return;
        }
        int visitId = currentVisitId;
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                auth.employeeId(), auth.roleId(), auth.roleCode(), auth.facilityId(),
                auth.departmentId(), "DESKTOP_UI", java.util.UUID.randomUUID());
        SwingWorker<KetQua<List<DichVuYeuCauDuLieu>>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<List<DichVuYeuCauDuLieu>> doInBackground() {
                return services.getServicesByVisit().handle(new GetDichVuTheoLuotKhamQuery(visitId, auth, actor));
            }

            @Override
            protected void done() {
                if (visitId != currentVisitId) {
                    return;
                }
                try {
                    KetQua<List<DichVuYeuCauDuLieu>> res = get();
                    if (res != null && res.isSuccess()) {
                        applyOrderedVisitServices(res.getValue().orElse(List.of()));
                    } else {
                        applyOrderedVisitServices(List.of());
                    }
                } catch (Exception ignored) {
                    applyOrderedVisitServices(List.of());
                }
            }
        };
        worker.execute();
    }

    /** Đổ danh sách chỉ định đã lưu lên bảng tab `Phiếu Chỉ Định` (hỗ trợ cả unit test). */
    public void applyOrderedVisitServices(List<DichVuYeuCauDuLieu> orders) {
        orderedVisitServices.clear();
        if (orderedTableModel == null) {
            return;
        }
        orderedTableModel.setRowCount(0);
        if (orders == null) {
            return;
        }
        Map<Integer, String> groupMap = buildServiceGroupMap();
        for (DichVuYeuCauDuLieu o : orders) {
            if (o == null || "DA_HUY".equalsIgnoreCase(safeText(o.status(), ""))) {
                continue;
            }
            String normType = normalizeServiceType(o.serviceType());
            if ("KHAM_BENH".equals(normType)) {
                continue;
            }
            orderedVisitServices.add(o);
            BigDecimal qty = o.quantity() != null ? o.quantity() : BigDecimal.ONE;
            BigDecimal unitPrice = o.unitPrice() != null ? o.unitPrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = unitPrice.multiply(qty);
            String groupName = groupMap.getOrDefault(o.serviceId(), serviceTypeDisplayName(normType));
            String orderedAtStr = o.orderedAt() != null ? o.orderedAt().format(ORDER_TIME_FORMATTER) : "";
            orderedTableModel.addRow(new Object[]{
                    "CLS-" + o.orderId(),
                    safeText(o.serviceCode(), ""),
                    safeText(o.serviceName(), ""),
                    groupName,
                    qty.stripTrailingZeros().toPlainString(),
                    formatMoney(unitPrice),
                    formatMoney(lineTotal),
                    formatOrderedStatus(o.status()),
                    orderedAtStr
            });
        }
    }

    private void handleCancelSelectedOrderedServices() {
        if (orderedServicesTable == null) {
            return;
        }
        int[] selectedRows = orderedServicesTable.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn ít nhất 1 chỉ định trên bảng để hủy.",
                    "Chưa chọn chỉ định", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<DichVuYeuCauDuLieu> targets = new ArrayList<>();
        for (int r : selectedRows) {
            if (r >= 0 && r < orderedVisitServices.size()) {
                targets.add(orderedVisitServices.get(r));
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn hủy " + targets.size() + " chỉ định dịch vụ đã chọn không?\n"
                        + "(Sau khi hủy, bạn có thể sang tab Danh Sách Dịch Vụ để tích chọn lại.)",
                "Xác Nhận Hủy Chỉ Định", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null || services == null || services.cancelServiceOrder() == null) {
            JOptionPane.showMessageDialog(this,
                    "Phiên làm việc không hợp lệ hoặc dịch vụ hủy chưa sẵn sàng.",
                    "Không thể hủy", JOptionPane.ERROR_MESSAGE);
            return;
        }
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                auth.employeeId(), auth.roleId(), auth.roleCode(), auth.facilityId(),
                auth.departmentId(), "DESKTOP_UI", java.util.UUID.randomUUID());

        new SwingWorker<List<String>, Void>() {
            private int canceledCount = 0;

            @Override
            protected List<String> doInBackground() {
                List<String> errors = new ArrayList<>();
                for (DichVuYeuCauDuLieu order : targets) {
                    CancelDichVuYeuCauCommand cmd = new CancelDichVuYeuCauCommand(
                            order.orderId(), "Hủy để thay đổi chỉ định từ màn hình dịch vụ", auth, actor);
                    KetQua<CancelDichVuYeuCauBienNhan> res = services.cancelServiceOrder().handle(cmd);
                    if (res != null && res.isSuccess()) {
                        canceledCount++;
                    } else {
                        String msg = res == null ? "Không nhận được kết quả"
                                : res.getError().map(e -> e.message()).orElse("Không thể hủy");
                        errors.add(safeText(order.serviceName(), "CLS-" + order.orderId()) + ": " + msg);
                    }
                }
                return errors;
            }

            @Override
            protected void done() {
                try {
                    List<String> errors = get();
                    refreshOrderedServicesForVisit();
                    if (canceledCount > 0 && errors.isEmpty()) {
                        JOptionPane.showMessageDialog(DichVuYeuCauManHinh.this,
                                "Đã hủy " + canceledCount + " chỉ định dịch vụ thành công.",
                                "Hủy Chỉ Định", JOptionPane.INFORMATION_MESSAGE);
                    } else if (canceledCount > 0) {
                        JOptionPane.showMessageDialog(DichVuYeuCauManHinh.this,
                                "Đã hủy " + canceledCount + " dịch vụ.\nMột số dịch vụ không thể hủy:\n"
                                        + String.join("\n", errors),
                                "Hủy Một Phần", JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(DichVuYeuCauManHinh.this,
                                errors.isEmpty() ? "Không thể hủy chỉ định đã chọn." : String.join("\n", errors),
                                "Không thể hủy", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DichVuYeuCauManHinh.this,
                            "Hủy chỉ định thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openOrderedServicesPrintPreview() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        LamSangYeuCauPrintPreviewManHinh.ClinicalOrderData data =
                LamSangYeuCauPrintPreviewManHinh.fromVisitOrders(
                        currentPatient, orderedVisitServices, buildServiceGroupMap());
        new LamSangYeuCauPrintPreviewHopThoai(owner, data).setVisible(true);
    }

    private Map<Integer, String> buildServiceGroupMap() {
        Map<Integer, String> map = new LinkedHashMap<>();
        for (ServiceItem item : allServices) {
            if (item.serviceId > 0 && item.category != null && !item.category.isBlank()) {
                map.put(item.serviceId, item.category);
            }
        }
        return map;
    }

    private static String formatOrderedStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Đang chờ";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "DA_CHI_DINH" -> "Đã chỉ định";
            case "DA_TIEP_NHAN" -> "Đã tiếp nhận";
            case "CHO_KET_QUA" -> "Chờ kết quả";
            case "DA_CO_KET_QUA" -> "Đã có kết quả";
            case "DA_HUY" -> "Đã hủy";
            default -> status.trim();
        };
    }

    public int getOrderedServicesRowCount() {
        return orderedTableModel != null ? orderedTableModel.getRowCount() : 0;
    }

    private void initMasterServices() {
        List<ServiceItem> loaded = new ArrayList<>();
        try (var connection = vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT id, maDichVu, tenDichVu, loaiDichVu, " +
                     "COALESCE(NULLIF(LTRIM(RTRIM(nhomDichVu)), ''), N'Khác') AS nhomDichVu, " +
                     "donGia, giaBHYT, COALESCE(moTa, N'') AS moTa " +
                     "FROM DanhMucDichVu WHERE hoatDong=1 AND isDeleted=0 " +
                     "AND loaiDichVu IN ('XET_NGHIEM','CDHA','CHAN_DOAN_HINH_ANH','DVKHAC','DICH_VU_KY_THUAT') " +
                     "ORDER BY loaiDichVu DESC, nhomDichVu, tenDichVu");
             var rows = statement.executeQuery()) {
            while (rows.next()) {
                BigDecimal donGia = rows.getBigDecimal("donGia") != null ? rows.getBigDecimal("donGia") : BigDecimal.ZERO;
                BigDecimal giaBHYT = rows.getBigDecimal("giaBHYT") != null ? rows.getBigDecimal("giaBHYT") : BigDecimal.ZERO;
                String loaiDichVu = rows.getString("loaiDichVu");
                String nhomDichVu = rows.getString("nhomDichVu");
                String formattedPrice = formatMoney(donGia);
                String formattedBhyt = formatMoney(giaBHYT);
                loaded.add(new ServiceItem(
                        rows.getInt("id"),
                        loaiDichVu,
                        nhomDichVu,
                        rows.getString("maDichVu"),
                        "",
                        rows.getString("tenDichVu"),
                        0,
                        giaBHYT,
                        donGia,
                        formattedBhyt,
                        formattedPrice,
                        formattedPrice,
                        formattedPrice,
                        "",
                        serviceTypeRoomName(loaiDichVu),
                        false));
            }
        } catch (Exception ex) {
            // Danh mục rỗng và thao tác lưu sẽ báo lỗi nếu không kết nối được CSDL.
        }
        loadCatalogItems(loaded);
    }

    /** Nạp trực tiếp danh sách dịch vụ vào cây nhóm & bảng (hỗ trợ cả JDBC và unit test). */
    public void loadCatalogItems(List<ServiceItem> items) {
        allServices.clear();
        currentFilteredServices.clear();
        selectedServices.clear();
        if (items != null) {
            allServices.addAll(items);
        }
        rebuildCategoryTreeModel();
        applyFilters();
        rebuildSelectedBar();
        updateFinancialTotals();
    }

    private JPanel buildCategoryTreePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(210, 218, 228)));

        // Hàng tiêu đề chọn "Tất cả dịch vụ (N)" màu xanh dương
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(0, 120, 215));
        topBar.setPreferredSize(new Dimension(195, 24));
        topBar.setBorder(new EmptyBorder(3, 8, 3, 6));
        topBar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        lblAllCategories = new JLabel("Tất cả dịch vụ (" + allServices.size() + ")");
        lblAllCategories.setFont(UngDungPhongChu.SMALL_BOLD);
        lblAllCategories.setForeground(Color.WHITE);
        topBar.add(lblAllCategories, BorderLayout.WEST);

        topBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (categoryTree != null) {
                    categoryTree.clearSelection();
                }
                currentCategoryNode = null;
                filterServicesByCategory("Tất cả");
            }
        });

        panel.add(topBar, BorderLayout.NORTH);

        categoryTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Đang tải..."));
        categoryTree = new JTree(categoryTreeModel);
        categoryTree.setFont(UngDungPhongChu.SMALL);
        categoryTree.setRootVisible(true);
        categoryTree.setRowHeight(21);

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setBackgroundSelectionColor(new Color(0, 120, 215));
        renderer.setTextSelectionColor(Color.WHITE);
        renderer.setBorderSelectionColor(new Color(0, 120, 215));
        categoryTree.setCellRenderer(renderer);

        categoryTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) categoryTree.getLastSelectedPathComponent();
            if (node != null) {
                Object userObj = node.getUserObject();
                if (userObj instanceof CategoryNodeData data) {
                    currentCategoryNode = data;
                    this.currentCategoryFilter = data.label;
                    applyFilters();
                } else if (userObj != null) {
                    currentCategoryNode = null;
                    filterServicesByCategory(userObj.toString());
                }
            }
        });

        rebuildCategoryTreeModel();

        JScrollPane scroll = new JScrollPane(categoryTree);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void rebuildCategoryTreeModel() {
        if (lblAllCategories != null) {
            lblAllCategories.setText("Tất cả dịch vụ (" + allServices.size() + ")");
        }
        if (categoryTreeModel == null || categoryTree == null) {
            return;
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(
                new CategoryNodeData(CategoryLevel.ALL, null, null, allServices.size()));

        Map<String, Map<String, Integer>> groupedCounts = new LinkedHashMap<>();
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        for (ServiceItem item : allServices) {
            String typeKey = normalizeServiceType(item.serviceType);
            String groupKey = item.category != null && !item.category.isBlank() ? item.category : "Khác";
            typeCounts.merge(typeKey, 1, Integer::sum);
            groupedCounts.computeIfAbsent(typeKey, k -> new LinkedHashMap<>())
                    .merge(groupKey, 1, Integer::sum);
        }

        TreePath defaultSelection = new TreePath(root.getPath());
        boolean firstGroupSelected = false;

        for (Map.Entry<String, Map<String, Integer>> typeEntry : groupedCounts.entrySet()) {
            String typeKey = typeEntry.getKey();
            int typeTotal = typeCounts.getOrDefault(typeKey, 0);
            DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(
                    new CategoryNodeData(CategoryLevel.TYPE, typeKey, null, typeTotal));
            root.add(typeNode);

            for (Map.Entry<String, Integer> groupEntry : typeEntry.getValue().entrySet()) {
                DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(
                        new CategoryNodeData(CategoryLevel.GROUP, typeKey, groupEntry.getKey(), groupEntry.getValue()));
                typeNode.add(groupNode);
                if (!firstGroupSelected) {
                    defaultSelection = new TreePath(groupNode.getPath());
                    firstGroupSelected = true;
                }
            }
        }

        categoryTreeModel.setRoot(root);
        categoryTreeModel.reload();
        for (int i = 0; i < categoryTree.getRowCount(); i++) {
            categoryTree.expandRow(i);
        }
        categoryTree.setSelectionPath(defaultSelection);
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) defaultSelection.getLastPathComponent();
        if (selectedNode != null && selectedNode.getUserObject() instanceof CategoryNodeData data) {
            currentCategoryNode = data;
            currentCategoryFilter = data.label;
        } else {
            currentCategoryNode = null;
            currentCategoryFilter = "Tất cả";
        }
    }

    private JPanel buildServiceListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // Hàng Tìm Kiếm (Ctrl+F) phía trên bảng danh mục
        JPanel searchBar = new JPanel(new BorderLayout(4, 0));
        searchBar.setBackground(Color.WHITE);
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 218, 228)),
                new EmptyBorder(3, 8, 3, 8)
        ));
        searchBar.setPreferredSize(new Dimension(800, 26));

        txtSearch = new JTextField(SEARCH_PLACEHOLDER);
        txtSearch.setFont(UngDungPhongChu.SMALL);
        txtSearch.setForeground(new Color(115, 120, 130));
        txtSearch.setBorder(BorderFactory.createEmptyBorder());
        txtSearch.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (SEARCH_PLACEHOLDER.equals(txtSearch.getText())) {
                    txtSearch.setText("");
                    txtSearch.setForeground(new Color(25, 30, 40));
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (txtSearch.getText().trim().isEmpty()) {
                    txtSearch.setText(SEARCH_PLACEHOLDER);
                    txtSearch.setForeground(new Color(115, 120, 130));
                }
            }
        });
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        JLabel lblSearchIcon = new JLabel("🔍");
        lblSearchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSearchIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));

        searchBar.add(txtSearch, BorderLayout.CENTER);
        searchBar.add(lblSearchIcon, BorderLayout.EAST);
        panel.add(searchBar, BorderLayout.NORTH);

        // Bảng danh mục dịch vụ 10 cột khớp 100% ảnh chụp
        String[] cols = {
                "Mã Dịch Vụ", "PD", "Tên Dịch Vụ", "SL", "Giá BHYT",
                "Giá Viện Phí", "Giá Dịch Vụ", "Cần nộp", "Đối Tượng", "Phòng Thực Hiện"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        serviceTable = new JTable(tableModel);
        serviceTable.setFont(UngDungPhongChu.SMALL);
        serviceTable.setRowHeight(21);
        serviceTable.setShowGrid(true);
        serviceTable.setGridColor(new Color(230, 235, 243));
        serviceTable.setSelectionBackground(new Color(254, 235, 235));
        serviceTable.setSelectionForeground(new Color(185, 25, 25));

        JTableHeader header = serviceTable.getTableHeader();
        header.setFont(UngDungPhongChu.SMALL_BOLD);
        header.setBackground(new Color(242, 246, 252));
        header.setForeground(new Color(30, 35, 45));
        header.setPreferredSize(new Dimension(800, 24));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));

        // Custom Renderer: Hỗ trợ hiển thị checkbox và đổi màu đỏ toàn dòng khi dịch vụ được chọn
        DefaultTableCellRenderer generalRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setFont(UngDungPhongChu.SMALL);

                boolean isChecked = false;
                if (row >= 0 && row < currentFilteredServices.size()) {
                    isChecked = currentFilteredServices.get(row).isSelected;
                }

                if (isChecked) {
                    lbl.setForeground(new Color(185, 25, 25)); // Toàn bộ chữ màu đỏ tươi chuẩn ảnh mẫu
                    if (isSelected) {
                        lbl.setBackground(new Color(254, 235, 235));
                    } else {
                        lbl.setBackground(new Color(255, 248, 248));
                    }
                } else {
                    if (isSelected) {
                        lbl.setBackground(new Color(214, 232, 255));
                        lbl.setForeground(new Color(15, 20, 25));
                    } else {
                        lbl.setBackground(Color.WHITE);
                        lbl.setForeground(new Color(30, 35, 45));
                    }
                }

                if (col == 0) {
                    lbl.setText((isChecked ? "  ☑ " : "  ☐ ") + (value != null ? value.toString() : ""));
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                } else if (col == 1 || col == 3 || col == 8) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col >= 4 && col <= 7) {
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                }

                return lbl;
            }
        };

        for (int i = 0; i < serviceTable.getColumnCount(); i++) {
            serviceTable.getColumnModel().getColumn(i).setCellRenderer(generalRenderer);
        }

        serviceTable.getColumnModel().getColumn(0).setPreferredWidth(105);
        serviceTable.getColumnModel().getColumn(1).setPreferredWidth(35);
        serviceTable.getColumnModel().getColumn(1).setMaxWidth(40);
        serviceTable.getColumnModel().getColumn(2).setPreferredWidth(310);
        serviceTable.getColumnModel().getColumn(3).setPreferredWidth(35);
        serviceTable.getColumnModel().getColumn(3).setMaxWidth(45);
        serviceTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        serviceTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        serviceTable.getColumnModel().getColumn(6).setPreferredWidth(70);
        serviceTable.getColumnModel().getColumn(7).setPreferredWidth(60);
        serviceTable.getColumnModel().getColumn(8).setPreferredWidth(65);
        serviceTable.getColumnModel().getColumn(9).setPreferredWidth(150);

        // Bắt sự kiện click chuột: click chọn dịch vụ -> thêm vào dải bên dưới, click chuột phải -> mở menu
        serviceTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    int r = serviceTable.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < currentFilteredServices.size()) {
                        ServiceItem item = currentFilteredServices.get(r);
                        toggleServiceSelection(item);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    handleRowRightClick(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    handleRowRightClick(e);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(serviceTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        // Dải dịch vụ đã chọn bên dưới bảng: hiển thị động các dịch vụ đã chọn
        selectedBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 4));
        selectedBar.setBackground(Color.WHITE);
        selectedBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 222, 232)));

        // Nạp dữ liệu ban đầu theo node mặc định của cây danh mục
        applyFilters();
        rebuildSelectedBar();

        panel.add(selectedBar, BorderLayout.SOUTH);

        return panel;
    }

    public void filterServicesByCategory(String categoryName) {
        String normalizedFilter = categoryName != null ? categoryName.trim() : "Tất cả";
        this.currentCategoryFilter = normalizedFilter;
        if ("Tất cả".equalsIgnoreCase(normalizedFilter)
                || normalizedFilter.startsWith("Tất cả dịch vụ")
                || normalizedFilter.startsWith("Dịch vụ cận lâm sàng")) {
            this.currentCategoryNode = null;
        } else {
            this.currentCategoryNode = findMatchingCategoryNode(normalizedFilter);
        }
        applyFilters();
    }

    /** Áp dụng đồng thời bộ lọc cây danh mục bên trái và từ khóa tìm kiếm trên bảng dịch vụ. */
    public void applyFilters() {
        currentFilteredServices.clear();
        String query = activeSearchQuery();

        for (ServiceItem item : allServices) {
            if (!matchesCategory(item)) {
                continue;
            }
            if (!query.isEmpty() && !matchesSearch(item, query)) {
                continue;
            }
            currentFilteredServices.add(item);
        }

        refreshTableRows();
    }

    private CategoryNodeData findMatchingCategoryNode(String rawCategoryName) {
        String cleanName = stripCountSuffix(rawCategoryName);
        for (ServiceItem item : allServices) {
            if (cleanName.equalsIgnoreCase(item.category)) {
                return new CategoryNodeData(CategoryLevel.GROUP, normalizeServiceType(item.serviceType), item.category, 0);
            }
            String typeDisplay = serviceTypeDisplayName(normalizeServiceType(item.serviceType));
            if (cleanName.equalsIgnoreCase(typeDisplay) || cleanName.equalsIgnoreCase(normalizeServiceType(item.serviceType))) {
                return new CategoryNodeData(CategoryLevel.TYPE, normalizeServiceType(item.serviceType), null, 0);
            }
        }
        return new CategoryNodeData(CategoryLevel.GROUP, null, cleanName, 0);
    }

    private boolean matchesCategory(ServiceItem item) {
        if (currentCategoryNode != null) {
            return switch (currentCategoryNode.level) {
                case ALL -> true;
                case TYPE -> normalizeServiceType(item.serviceType).equalsIgnoreCase(currentCategoryNode.serviceType);
                case GROUP -> (currentCategoryNode.serviceType == null
                        || normalizeServiceType(item.serviceType).equalsIgnoreCase(currentCategoryNode.serviceType))
                        && item.category.equalsIgnoreCase(currentCategoryNode.groupName);
            };
        }
        if (currentCategoryFilter == null || currentCategoryFilter.isBlank()
                || "Tất cả".equalsIgnoreCase(currentCategoryFilter)
                || currentCategoryFilter.startsWith("Tất cả dịch vụ")
                || currentCategoryFilter.startsWith("Dịch vụ cận lâm sàng")) {
            return true;
        }
        String cleanFilter = stripCountSuffix(currentCategoryFilter);
        return item.category.equalsIgnoreCase(cleanFilter)
                || normalizeServiceType(item.serviceType).equalsIgnoreCase(cleanFilter)
                || serviceTypeDisplayName(normalizeServiceType(item.serviceType)).equalsIgnoreCase(cleanFilter);
    }

    private boolean matchesSearch(ServiceItem item, String query) {
        return item.code.toLowerCase(Locale.ROOT).contains(query)
                || item.name.toLowerCase(Locale.ROOT).contains(query)
                || item.category.toLowerCase(Locale.ROOT).contains(query);
    }

    private String activeSearchQuery() {
        if (txtSearch == null) {
            return "";
        }
        String text = txtSearch.getText();
        if (text == null || text.isBlank() || SEARCH_PLACEHOLDER.equals(text.trim())) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private static String stripCountSuffix(String text) {
        if (text == null) {
            return "";
        }
        int idx = text.lastIndexOf(" (");
        if (idx > 0 && text.endsWith(")")) {
            return text.substring(0, idx).trim();
        }
        return text.trim();
    }

    private void refreshTableRows() {
        if (tableModel == null) return;
        tableModel.setRowCount(0);

        int selectedRowIndex = -1;
        for (int i = 0; i < currentFilteredServices.size(); i++) {
            ServiceItem item = currentFilteredServices.get(i);
            tableModel.addRow(new Object[]{
                    item.code,
                    item.pd,
                    item.name,
                    String.valueOf(item.quantity),
                    item.giaBhyt,
                    item.giaVienPhi,
                    item.giaDichVu,
                    item.canNop,
                    item.doiTuong,
                    item.phongThucHien
            });
            if (item.isSelected && item.code.equals(activeSelectedServiceCode)) {
                selectedRowIndex = i;
            }
        }

        if (selectedRowIndex >= 0) {
            serviceTable.setRowSelectionInterval(selectedRowIndex, selectedRowIndex);
        } else if (serviceTable.getRowCount() > 0) {
            for (int i = 0; i < currentFilteredServices.size(); i++) {
                if (currentFilteredServices.get(i).isSelected) {
                    serviceTable.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
        serviceTable.repaint();
    }

    public void toggleServiceSelection(ServiceItem item) {
        if (item == null) {
            return;
        }
        item.isSelected = !item.isSelected;
        if (item.isSelected) {
            item.quantity = Math.max(1, item.quantity);
            item.doiTuong = currentPatientHasBhyt ? "BHYT" : "Thu phí";
            BigDecimal payable = calculatePayableUnitPrice(item);
            item.canNop = formatMoney(payable.multiply(BigDecimal.valueOf(item.quantity)));
            activeSelectedServiceCode = item.code;
            if (!selectedServices.contains(item)) {
                selectedServices.add(item);
            }
        } else {
            item.quantity = 0;
            item.doiTuong = "";
            item.canNop = item.giaDichVu;
            selectedServices.remove(item);
            if (item.code.equals(activeSelectedServiceCode)) {
                activeSelectedServiceCode = selectedServices.isEmpty() ? null
                        : selectedServices.get(selectedServices.size() - 1).code;
            }
        }
        refreshTableRows();
        rebuildSelectedBar();
        updateFinancialTotals();
    }

    private void rebuildSelectedBar() {
        if (selectedBar == null) return;
        selectedBar.removeAll();

        for (ServiceItem item : selectedServices) {
            JLabel lbl = new JLabel("✳ " + item.name);
            lbl.setFont(UngDungPhongChu.SMALL_BOLD);
            lbl.setForeground(new Color(0, 110, 210));
            lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));

            lbl.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectAndEditService(item.code, lbl);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                        selectAndEditService(item.code, lbl);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                        selectAndEditService(item.code, lbl);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    lbl.setForeground(new Color(0, 60, 170));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    lbl.setForeground(new Color(0, 110, 210));
                }
            });

            selectedBar.add(lbl);

            if ("DV_LIQUI".equals(item.code)) {
                itemLiqui = lbl;
            } else if ("DV_KCBLC012".equals(item.code)) {
                itemAmh = lbl;
            }
        }

        selectedBar.revalidate();
        selectedBar.repaint();
    }

    private void updateFinancialTotals() {
        if (lCost == null || lBenhNhan == null || lConDu == null) return;

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalBhytCovered = BigDecimal.ZERO;
        for (ServiceItem item : selectedServices) {
            int qty = Math.max(1, item.quantity);
            BigDecimal lineTotal = (item.unitPriceValue != null ? item.unitPriceValue : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(qty));
            totalCost = totalCost.add(lineTotal);
            if (currentPatientHasBhyt && item.bhytPriceValue != null && item.bhytPriceValue.signum() > 0) {
                BigDecimal coveredPerUnit = item.bhytPriceValue
                        .multiply(BigDecimal.valueOf(currentPatientBhytRate))
                        .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
                BigDecimal capped = coveredPerUnit.min(item.unitPriceValue != null ? item.unitPriceValue : coveredPerUnit);
                totalBhytCovered = totalBhytCovered.add(capped.multiply(BigDecimal.valueOf(qty)));
            }
        }
        BigDecimal patientPayable = totalCost.subtract(totalBhytCovered).max(BigDecimal.ZERO);

        lCost.setText("<html>Tổng chi phí: <font color='#CC0000'><b>" + formatMoney(totalCost) + "</b></font></html>");
        if (lBhyt != null) {
            lBhyt.setText("BHYT: " + formatMoney(totalBhytCovered));
        }
        lBenhNhan.setText("<html>Bệnh nhân: <font color='#CC0000'><b>" + formatMoney(patientPayable) + "</b></font></html>");
        if (lTamUng != null) {
            lTamUng.setText("<html>Tạm ứng: <font color='#0044CC'><b>0</b></font></html>");
        }
        if (lDaThu != null) {
            lDaThu.setText("<html>Đã thu: <font color='#0044CC'><b>0</b></font></html>");
        }
        lConDu.setText("<html>Còn phải thu: <font color='#0044CC'><b>" + formatMoney(patientPayable) + "</b></font></html>");
    }

    private void handleRowRightClick(MouseEvent e) {
        int r = serviceTable.rowAtPoint(e.getPoint());
        if (r >= 0 && r < currentFilteredServices.size()) {
            serviceTable.setRowSelectionInterval(r, r);
            ServiceItem item = currentFilteredServices.get(r);
            DichVuNguCanhMenu menu = new DichVuNguCanhMenu(item.code, item.name, () -> {
                int ret = JOptionPane.showConfirmDialog(this,
                        "Bạn có chắc chắn muốn xóa dịch vụ [" + item.code + " - " + item.name + "] không?",
                        "Xác Nhận Xóa Dịch Vụ", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(this, "Đã xóa dịch vụ thành công!");
                    toggleServiceSelection(item);
                }
            });
            menu.show(serviceTable, e.getX(), e.getY());
        }
    }

    private JPanel buildFooterSection() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(248, 250, 253));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(205, 214, 226)));
        footer.setPreferredSize(new Dimension(1000, 48));

        // Bên trái: Thông tin tài chính 2 dòng
        JPanel financialPanel = new JPanel();
        financialPanel.setLayout(new BoxLayout(financialPanel, BoxLayout.Y_AXIS));
        financialPanel.setOpaque(false);
        financialPanel.setBorder(new EmptyBorder(4, 10, 4, 10));

        // Dòng 1: Tổng chi phí | BHYT | Miễn giảm DV | Nguồn khác | Bệnh nhân
        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 1));
        line1.setOpaque(false);

        lCost = new JLabel("<html>Tổng chi phí: <font color='#CC0000'><b>0</b></font></html>");
        lCost.setFont(UngDungPhongChu.SMALL);

        lBhyt = new JLabel("BHYT: 0");
        lBhyt.setFont(UngDungPhongChu.SMALL);

        JLabel lMienGiam = new JLabel("Miễn giảm DV: 0");
        lMienGiam.setFont(UngDungPhongChu.SMALL);

        JLabel lNguonKhac = new JLabel("Nguồn khác: 0");
        lNguonKhac.setFont(UngDungPhongChu.SMALL);

        lBenhNhan = new JLabel("<html>Bệnh nhân: <font color='#CC0000'><b>0</b></font></html>");
        lBenhNhan.setFont(UngDungPhongChu.SMALL);

        line1.add(lCost);
        line1.add(lBhyt);
        line1.add(lMienGiam);
        line1.add(lNguonKhac);
        line1.add(lBenhNhan);

        // Dòng 2: Tạm ứng | Đã thu | Miễn giảm HĐ | Còn phải thu
        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 1));
        line2.setOpaque(false);

        lTamUng = new JLabel("<html>Tạm ứng: <font color='#0044CC'><b>0</b></font></html>");
        lTamUng.setFont(UngDungPhongChu.SMALL);

        lDaThu = new JLabel("<html>Đã thu: <font color='#0044CC'><b>0</b></font></html>");
        lDaThu.setFont(UngDungPhongChu.SMALL);

        JLabel lMienGiamHd = new JLabel("Miễn giảm HĐ: 0");
        lMienGiamHd.setFont(UngDungPhongChu.SMALL);

        lConDu = new JLabel("<html>Còn phải thu: <font color='#0044CC'><b>0</b></font></html>");
        lConDu.setFont(UngDungPhongChu.SMALL);

        line2.add(lTamUng);
        line2.add(lDaThu);
        line2.add(lMienGiamHd);
        line2.add(lConDu);

        financialPanel.add(line1);
        financialPanel.add(line2);

        // Bên phải: Checkbox [✓] In phiếu thực hiện CLS + [💾 Lưu (Ctrl + S)] + [💾🖨 Lưu + In]
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        rightActions.setOpaque(false);

        chkPrintCls = new JCheckBox("<html>In phiếu<br>thực hiện CLS</html>", true);
        chkPrintCls.setFont(UngDungPhongChu.SMALL);
        chkPrintCls.setOpaque(false);

        JButton btnSave = new JButton(" Lưu (Ctrl + S)", new FloppyDiskIcon());
        btnSave.setFont(UngDungPhongChu.SMALL_BOLD);
        btnSave.setForeground(new Color(25, 30, 40));
        btnSave.setBackground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        btnSave.addActionListener(e -> handleSaveAndClose(false));

        JButton btnSaveAndPrint = new JButton(" Lưu + In", new SaveAndPrintIcon());
        btnSaveAndPrint.setFont(UngDungPhongChu.SMALL_BOLD);
        btnSaveAndPrint.setForeground(new Color(25, 30, 40));
        btnSaveAndPrint.setBackground(Color.WHITE);
        btnSaveAndPrint.setFocusPainted(false);
        btnSaveAndPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSaveAndPrint.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        btnSaveAndPrint.addActionListener(e -> handleSaveAndClose(true));

        rightActions.add(chkPrintCls);
        rightActions.add(btnSave);
        rightActions.add(btnSaveAndPrint);

        footer.add(financialPanel, BorderLayout.WEST);
        footer.add(rightActions, BorderLayout.EAST);

        return footer;
    }

    private void handleSaveAndClose(boolean printAfterSave) {
        if (services == null || services.orderService() == null || currentVisitId <= 0) {
            JOptionPane.showMessageDialog(this, "Chưa có lượt khám hoặc dịch vụ lưu hợp lệ.",
                    "Không thể lưu", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<ServiceItem> itemsToSave = resolveItemsToSave();
        if (itemsToSave.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ít nhất một dịch vụ cần chỉ định.",
                    "Thiếu dịch vụ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        var context = KhamBenhPhienNguCanh.current();
        if (context == null) {
            JOptionPane.showMessageDialog(this, "Phiên làm việc không hợp lệ. Vui lòng đăng nhập lại.",
                    "Không thể lưu", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String doctorNotes = buildOrderNotes();

        SwingWorker<KetQua<SaveOrdersOutcome>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<SaveOrdersOutcome> doInBackground() {
                int savedCount = 0;
                List<DichVuYeuCauDuLieu> savedOrders = new ArrayList<>();
                for (ServiceItem item : itemsToSave) {
                    int resolvedServiceId = item.serviceId > 0 ? item.serviceId : lookupServiceIdByCode(item.code);
                    if (resolvedServiceId <= 0) {
                        return KetQua.failure("SERVICE_NOT_FOUND",
                                "Dịch vụ [" + item.code + "] chưa có trong danh mục CSDL.");
                    }
                    BigDecimal qty = BigDecimal.valueOf(Math.max(1, item.quantity));
                    var cmd = new YeuCauDichVuCommand(currentVisitId, resolvedServiceId, qty,
                            null, doctorNotes, context, null);
                    KetQua<YeuCauDichVuBienNhan> result = services.orderService().handle(cmd);
                    if (result == null || !result.isSuccess()) {
                        String msg = result == null ? "Không nhận được kết quả lưu."
                                : result.getError().map(e -> e.message()).orElse("Không thể lưu dịch vụ.");
                        return KetQua.failure("SAVE_FAILED", msg);
                    }
                    YeuCauDichVuBienNhan receipt = result.getValue().orElse(null);
                    int orderId = receipt != null ? receipt.orderId() : 0;
                    LocalDateTime orderedAt = receipt != null && receipt.orderedAt() != null
                            ? receipt.orderedAt() : LocalDateTime.now();
                    savedOrders.add(new DichVuYeuCauDuLieu(
                            orderId, currentVisitId, resolvedServiceId, item.code, item.name,
                            item.serviceType, currentDoctorId, qty,
                            item.unitPriceValue, "DA_CHI_DINH", null, null, null,
                            null, null, orderedAt, null, null, List.of()));
                    savedCount++;
                }

                List<DichVuYeuCauDuLieu> refreshedOrders = null;
                if (services.getServicesByVisit() != null) {
                    KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                            context.employeeId(), context.roleId(), context.roleCode(),
                            context.facilityId(), context.departmentId(),
                            "DESKTOP_UI", java.util.UUID.randomUUID());
                    KetQua<List<DichVuYeuCauDuLieu>> refreshed = services.getServicesByVisit()
                            .handle(new GetDichVuTheoLuotKhamQuery(currentVisitId, context, actor));
                    if (refreshed != null && refreshed.isSuccess()) {
                        refreshedOrders = refreshed.getValue().orElse(List.of());
                    }
                }
                return KetQua.success(new SaveOrdersOutcome(savedCount, savedOrders, refreshedOrders));
            }

            @Override
            protected void done() {
                try {
                    KetQua<SaveOrdersOutcome> result = get();
                    if (result == null || !result.isSuccess()) {
                        JOptionPane.showMessageDialog(DichVuYeuCauManHinh.this,
                                result == null ? "Không nhận được kết quả lưu."
                                        : result.getError().map(error -> error.message()).orElse("Không thể lưu dịch vụ."),
                                "Không thể lưu dịch vụ", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    SaveOrdersOutcome outcome = result.getValue().orElseThrow();
                    clearSelectedServices();
                    if (outcome.refreshedOrders() != null) {
                        applyOrderedVisitServices(outcome.refreshedOrders());
                    } else {
                        refreshOrderedServicesForVisit();
                    }
                    JOptionPane.showMessageDialog(DichVuYeuCauManHinh.this,
                            "Đã lưu " + outcome.savedCount() + " chỉ định dịch vụ.", "Chỉ định dịch vụ",
                            JOptionPane.INFORMATION_MESSAGE);
                    if (printAfterSave) {
                        List<DichVuYeuCauDuLieu> printOrders = outcome.refreshedOrders() != null
                                ? new ArrayList<>(orderedVisitServices)
                                : mergeOrdersForPrint(orderedVisitServices, outcome.savedOrders());
                        Window owner = SwingUtilities.getWindowAncestor(DichVuYeuCauManHinh.this);
                        var printData = LamSangYeuCauPrintPreviewManHinh.fromVisitOrders(
                                currentPatient, printOrders, buildServiceGroupMap());
                        new LamSangYeuCauPrintPreviewHopThoai(owner, printData).setVisible(true);
                    }
                    if (onCloseCallback != null) onCloseCallback.run();
                    else if (ownerWindow != null) ownerWindow.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DichVuYeuCauManHinh.this,
                            "Lưu dịch vụ thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private record SaveOrdersOutcome(
            int savedCount,
            List<DichVuYeuCauDuLieu> savedOrders,
            List<DichVuYeuCauDuLieu> refreshedOrders) {}

    /** Gộp danh sách chỉ định hiện có và danh sách vừa lưu, không lặp cùng `orderId`. */
    static List<DichVuYeuCauDuLieu> mergeOrdersForPrint(
            List<DichVuYeuCauDuLieu> persistedOrders,
            List<DichVuYeuCauDuLieu> justOrdered) {
        List<DichVuYeuCauDuLieu> merged = new ArrayList<>();
        java.util.Set<Integer> seenIds = new java.util.HashSet<>();
        if (persistedOrders != null) {
            for (DichVuYeuCauDuLieu order : persistedOrders) {
                if (order == null) continue;
                if (order.orderId() > 0 && !seenIds.add(order.orderId())) continue;
                merged.add(order);
            }
        }
        if (justOrdered != null) {
            for (DichVuYeuCauDuLieu order : justOrdered) {
                if (order == null) continue;
                if (order.orderId() > 0 && !seenIds.add(order.orderId())) continue;
                merged.add(order);
            }
        }
        return merged;
    }

    /** Lấy danh sách dịch vụ cần lưu (ưu tiên các dòng đã tích ☑, fallback dòng đang chọn trên bảng). */
    public List<ServiceItem> resolveItemsToSave() {
        if (!selectedServices.isEmpty()) {
            return new ArrayList<>(selectedServices);
        }
        int row = serviceTable != null ? serviceTable.getSelectedRow() : -1;
        if (row >= 0 && row < currentFilteredServices.size()) {
            return List.of(currentFilteredServices.get(row));
        }
        return List.of();
    }

    /** Xây dựng danh sách lệnh chỉ định cho unit test và thao tác lưu. */
    public List<YeuCauDichVuCommand> buildOrderCommands(
            vn.clinicmanagement.application.authorization.PhanQuyenNguCanh context) {
        List<YeuCauDichVuCommand> commands = new ArrayList<>();
        String notes = buildOrderNotes();
        for (ServiceItem item : resolveItemsToSave()) {
            if (item.serviceId > 0) {
                commands.add(new YeuCauDichVuCommand(
                        currentVisitId,
                        item.serviceId,
                        BigDecimal.valueOf(Math.max(1, item.quantity)),
                        null,
                        notes,
                        context,
                        null));
            }
        }
        return commands;
    }

    private int lookupServiceIdByCode(String serviceCode) {
        if (serviceCode == null || serviceCode.isBlank()) {
            return 0;
        }
        try (var connection = vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi.getConnection();
             var ps = connection.prepareStatement(
                     "SELECT id FROM DanhMucDichVu WHERE maDichVu=? AND hoatDong=1 AND isDeleted=0")) {
            ps.setString(1, serviceCode);
            try (var result = ps.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private String buildOrderNotes() {
        String note = txtOrderNote != null ? txtOrderNote.getText().trim() : "";
        return !note.isEmpty() ? note : "Chỉ định dịch vụ kỹ thuật từ bàn khám";
    }

    private void clearSelectedServices() {
        for (ServiceItem item : allServices) {
            item.isSelected = false;
            item.quantity = 0;
            item.doiTuong = "";
            item.canNop = item.giaDichVu;
        }
        selectedServices.clear();
        activeSelectedServiceCode = null;
        refreshTableRows();
        rebuildSelectedBar();
        updateFinancialTotals();
    }

    private BigDecimal calculatePayableUnitPrice(ServiceItem item) {
        BigDecimal price = item.unitPriceValue != null ? item.unitPriceValue : BigDecimal.ZERO;
        if (!currentPatientHasBhyt || item.bhytPriceValue == null || item.bhytPriceValue.signum() <= 0) {
            return price;
        }
        BigDecimal covered = item.bhytPriceValue
                .multiply(BigDecimal.valueOf(currentPatientBhytRate))
                .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP)
                .min(price);
        return price.subtract(covered).max(BigDecimal.ZERO);
    }

    private void applyDiagnosisText(String diagnosis) {
        String raw = diagnosis != null ? diagnosis.trim() : "";
        if (raw.isEmpty()) {
            txtIcdCode.setText("");
            txtIcdName.setText("");
            return;
        }
        int sep = raw.indexOf(" - ");
        if (sep > 0) {
            txtIcdCode.setText(raw.substring(0, sep).trim());
            txtIcdName.setText(raw.substring(sep + 3).trim());
        } else {
            txtIcdCode.setText("");
            txtIcdName.setText(raw);
        }
    }

    private static String defaultOperatorLabel() {
        var ctx = KhamBenhPhienNguCanh.current();
        if (ctx != null && ctx.employeeId() > 0) {
            return (ctx.roleCode() != null ? ctx.roleCode() : "NV") + " #" + ctx.employeeId();
        }
        return "Bác sĩ khám";
    }

    private static String normalizeServiceType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return "XET_NGHIEM";
        }
        return switch (rawType.trim().toUpperCase(Locale.ROOT)) {
            case "CDHA", "CHAN_DOAN_HINH_ANH" -> "CDHA";
            case "DVKHAC", "DICH_VU_KY_THUAT" -> "DVKHAC";
            case "KHAM_BENH", "KHAM" -> "KHAM_BENH";
            default -> "XET_NGHIEM";
        };
    }

    private static String serviceTypeDisplayName(String serviceType) {
        return switch (normalizeServiceType(serviceType)) {
            case "CDHA" -> "Chẩn đoán hình ảnh";
            case "DVKHAC" -> "Dịch vụ kỹ thuật khác";
            case "KHAM_BENH" -> "Khám bệnh";
            default -> "Xét nghiệm";
        };
    }

    private static String serviceTypeRoomName(String serviceType) {
        return switch (normalizeServiceType(serviceType)) {
            case "CDHA" -> "Phòng CĐHA";
            case "DVKHAC" -> "Phòng Thủ thuật";
            case "KHAM_BENH" -> "Phòng Khám";
            default -> "Phòng Xét nghiệm";
        };
    }

    private static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private static String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return MONEY_FORMAT.format(value);
    }

    private static String safeText(String val, String fallback) {
        return val != null && !val.isBlank() ? val.trim() : fallback;
    }

    public JTree getCategoryTree() {
        return categoryTree;
    }

    public List<ServiceItem> getCurrentFilteredServices() {
        return List.copyOf(currentFilteredServices);
    }

    public List<ServiceItem> getSelectedServices() {
        return List.copyOf(selectedServices);
    }

    public JTextField getSearchField() {
        return txtSearch;
    }

    public String getBannerPatientText() {
        return lblBannerPatient != null ? lblBannerPatient.getText() : "";
    }

    public String getTotalCostLabelText() {
        return lCost != null ? lCost.getText() : "";
    }

    public JTable getServiceTable() {
        return serviceTable;
    }

    public void selectAndEditService(String code, JComponent anchor) {
        this.activeSelectedServiceCode = code;

        // Cuộn tới dòng dịch vụ nếu đang hiển thị trên bảng
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            Object val = tableModel.getValueAt(r, 0);
            if (code != null && code.equals(val)) {
                serviceTable.setRowSelectionInterval(r, r);
                serviceTable.scrollRectToVisible(serviceTable.getCellRect(r, 0, true));
                break;
            }
        }
        serviceTable.repaint();

        String serviceName = "Dịch vụ " + code;
        ServiceItem targetItem = null;
        for (ServiceItem it : allServices) {
            if (it.code.equals(code)) {
                serviceName = it.name;
                targetItem = it;
                break;
            }
        }
        final ServiceItem finalItem = targetItem;

        DichVuNguCanhMenu menu = new DichVuNguCanhMenu(code, serviceName, () -> {
            int ret = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn xóa dịch vụ [" + code + " - " + (finalItem != null ? finalItem.name : "") + "] không?",
                    "Xác Nhận Xóa Dịch Vụ", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ret == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(this, "Đã xóa dịch vụ thành công!");
                if (finalItem != null) {
                    toggleServiceSelection(finalItem);
                }
            }
        });

        if (anchor != null && anchor.isShowing()) {
            int popupY = -menu.getPreferredSize().height - 2;
            menu.show(anchor, 0, popupY);
        }
    }

    public JLabel getItemAmh() {
        return itemAmh;
    }

    public JLabel getItemLiqui() {
        return itemLiqui;
    }

    private JButton createSubToolbarButton(String text, String type, boolean hasDropdown) {
        String label = hasDropdown ? text + " ▾" : text;
        Icon icon;
        switch (type) {
            case "menu":
                icon = new HamburgerIcon();
                break;
            case "template":
                icon = new TemplateTagIcon();
                break;
            case "refresh":
                icon = new ClockRefreshIcon();
                break;
            case "protocol":
                icon = new ProtocolCheckIcon();
                break;
            default:
                icon = new TemplateTagIcon();
                break;
        }

        JButton btn = new JButton(label, icon);
        btn.setFont(UngDungPhongChu.SMALL);
        btn.setForeground(new Color(30, 35, 45));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(new EmptyBorder(4, 6, 4, 6));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setContentAreaFilled(true);
                btn.setBackground(new Color(225, 235, 248));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setContentAreaFilled(false);
            }
        });

        return btn;
    }

    // =========================================================================
    // VECTOR ICONS
    // =========================================================================

    public static class HamburgerIcon implements Icon {
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 102, 204));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 1, y + 2, x + 15, y + 2);
            g2.drawLine(x + 1, y + 7, x + 15, y + 7);
            g2.drawLine(x + 1, y + 12, x + 15, y + 12);
            g2.dispose();
        }
    }

    public static class TemplateTagIcon implements Icon {
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 15; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 102, 204));
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(x + 2, y + 2, 11, 11, 3, 3);
            g2.drawLine(x + 2, y + 6, x + 13, y + 6);
            g2.dispose();
        }
    }

    public static class ClockRefreshIcon implements Icon {
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 15; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 102, 204));
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x + 1, y + 1, 12, 12);
            g2.drawLine(x + 7, y + 4, x + 7, y + 7);
            g2.drawLine(x + 7, y + 7, x + 10, y + 7);
            g2.dispose();
        }
    }

    public static class ProtocolCheckIcon implements Icon {
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 15; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 102, 204));
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRect(x + 1, y + 1, 12, 12);
            g2.drawLine(x + 4, y + 4, x + 10, y + 4);
            g2.drawLine(x + 4, y + 7, x + 10, y + 7);
            g2.drawLine(x + 4, y + 10, x + 8, y + 10);
            g2.dispose();
        }
    }

    public static class FloppyDiskIcon implements Icon {
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(40, 150, 70));
            g2.fillRoundRect(x + 1, y + 1, 14, 14, 2, 2);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 4, y + 2, 8, 5);
            g2.fillRect(x + 3, y + 9, 10, 5);
            g2.setColor(new Color(40, 150, 70));
            g2.fillRect(x + 8, y + 2, 2, 4);
            g2.dispose();
        }
    }

    public static class SaveAndPrintIcon implements Icon {
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(140, 70, 160));
            g2.fillRoundRect(x + 1, y + 1, 14, 14, 2, 2);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 4, y + 2, 8, 5);
            g2.fillRect(x + 3, y + 9, 10, 5);
            g2.dispose();
        }
    }
}
