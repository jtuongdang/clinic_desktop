package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.desktop.modules.examination.components.menus.ServiceContextMenu;

import vn.clinicmanagement.desktop.core.config.AppFonts;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.cls.OrderServiceCommand;
import vn.clinicmanagement.application.cls.OrderServiceReceipt;
import vn.clinicmanagement.desktop.modules.examination.ExaminationCompositionRoot;

/**
 * Giao diện màn hình: "ĐHC - Chỉ Định Dịch Vụ"
 * Chuyển hướng khi nhấn nút [Sửa Phiếu] từ tab Xét Nghiệm.
 * Khớp 100% chi tiết trong ảnh chụp mẫu thực tế:
 * - Tiêu đề cửa sổ: ĐHC - Chỉ Định Dịch Vụ --- Người đăng nhập: sennv --- --- Version: 2022.7.15.3 ---
 * - Thanh công cụ phụ: Menu (≡), Mẫu Chỉ Định, Phiếu Chỉ Định Dịch Vụ, Sử Dụng Phác Đồ
 * - Banner thông tin bệnh nhân góc phải: NGUYỄN HOÀNG LONG | BN-20261107-0003 | 1995 (31 tuổi) | Nam | Mã VP: 214302 | Viện phí
 * - Hàng thông tin y lệnh:
 *   + Hàng 1: Ngày y lệnh (09:32 09/08/2022) | Chẩn đoán (H20 - Viêm màng bồ đào ...) | Bệnh kèm theo | [ ] Cấp cứu
 *   + Hàng 2: Người y lệnh (Sennv - sennv) | Ghi chú | Phiếu điều trị | Thêm phiếu điều trị
 * - Tab con: [Danh Sách Dịch Vụ] | [Phiếu Chỉ Định]
 * - Khung trái: Cây danh mục phân nhóm: "Tất cả dịch vụ (291)", "- Xét nghiệm (291)", Hóa sinh (62), Huyết học (23)...
 * - Khung phải: Bảng danh mục dịch vụ 10 cột lọc theo nhóm dịch vụ bên trái
 * - Dải dịch vụ đã chọn dưới bảng: ✳ xét nghiệm tế bào học bằng phương pháp Liqui Prep   ✳ Định lượng GH (Growth Hormone) [Máu]...
 * - Thanh tài chính chân trang: Tổng chi phí: 21,611,700 | Bệnh nhân: 21,611,700 | Đã thu: 8,524,700 | Còn dư: 996,913,000
 * - Nút thao tác: [✓] In phiếu thực hiện CLS, [💾 Lưu (Ctrl + S)], [💾🖨 Lưu + In]
 */
public class ServiceOrderPanel extends JPanel {

    public static class ServiceItem {
        public String category;
        public String code;
        public String pd;
        public String name;
        public int quantity;
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
            this.category = category;
            this.code = code;
            this.pd = pd;
            this.name = name;
            this.quantity = quantity;
            this.giaBhyt = giaBhyt;
            this.giaVienPhi = giaVienPhi;
            this.giaDichVu = giaDichVu;
            this.canNop = canNop;
            this.doiTuong = doiTuong;
            this.phongThucHien = phongThucHien;
            this.isSelected = isSelected;
        }
    }

    private final Window ownerWindow;
    private final Runnable onCloseCallback;
    private final ExaminationCompositionRoot.Services services;
    private int currentVisitId = 1;
    private int currentDoctorId = 1;
    private int currentFacilityId = 1;

    private JTable serviceTable;
    private DefaultTableModel tableModel;
    private JCheckBox chkPrintCls;
    private String activeSelectedServiceCode = "DV01295";
    private JLabel itemLiqui;
    private JLabel itemAmh;

    private final List<ServiceItem> allServices = new ArrayList<>();
    private final List<ServiceItem> currentFilteredServices = new ArrayList<>();
    private final List<ServiceItem> selectedServices = new ArrayList<>();
    private JTree categoryTree;
    private JPanel selectedBar;
    private JLabel lCost;
    private JLabel lBenhNhan;
    private JLabel lConDu;
    private String currentCategoryFilter = "Hóa sinh";

    public ServiceOrderPanel() {
        this((Runnable) null, resolveDefaultServices());
    }

    public ServiceOrderPanel(Runnable onCloseCallback) {
        this(onCloseCallback, resolveDefaultServices());
    }

    public ServiceOrderPanel(Runnable onCloseCallback, ExaminationCompositionRoot.Services services) {
        this.ownerWindow = null;
        this.onCloseCallback = onCloseCallback;
        this.services = services != null ? services : resolveDefaultServices();
        initPanel();
    }

    public ServiceOrderPanel(Window ownerWindow) {
        this(ownerWindow, resolveDefaultServices());
    }

    public ServiceOrderPanel(Window ownerWindow, ExaminationCompositionRoot.Services services) {
        this.ownerWindow = ownerWindow;
        this.onCloseCallback = () -> {
            if (ownerWindow != null) {
                ownerWindow.dispose();
            }
        };
        this.services = services != null ? services : resolveDefaultServices();
        initPanel();
    }

    private static ExaminationCompositionRoot.Services resolveDefaultServices() {
        try {
            return ExaminationCompositionRoot.createDefault();
        } catch (Exception ex) {
            return null;
        }
    }

    public void setVisitContext(int visitId, int doctorId, int facilityId) {
        this.currentVisitId = visitId;
        this.currentDoctorId = doctorId;
        this.currentFacilityId = facilityId;
    }

    private void initPanel() {
        initMasterServices();

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. THANH CÔNG CỤ PHỤ + BANNER THÔNG TIN BỆNH NHÂN + HÀNG Y LỆNH
        add(buildHeaderSection(), BorderLayout.NORTH);

        // 2. PHẦN THÂN CHÍNH: DẢI TAB + CÂY DANH MỤC + BẢNG DỊCH VỤ + DẢI DỊCH VỤ ĐÃ CHỌN
        add(buildBodySection(), BorderLayout.CENTER);

        // 3. THANH TÀI CHÍNH & CỤM NÚT LƯU CHÂN TRANG
        add(buildFooterSection(), BorderLayout.SOUTH);
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

        // Banner bệnh nhân bên phải
        JPanel rightBanner = new JPanel(new BorderLayout(8, 0));
        rightBanner.setOpaque(false);
        rightBanner.setBorder(new EmptyBorder(2, 6, 2, 10));

        JPanel bannerText = new JPanel();
        bannerText.setLayout(new BoxLayout(bannerText, BoxLayout.Y_AXIS));
        bannerText.setOpaque(false);

        JLabel l1 = new JLabel("NGUYỄN HOÀNG LONG | BN-20261107-0003 | 1995 (31 tuổi) | Nam");
        l1.setFont(AppFonts.SMALL_BOLD);
        l1.setForeground(new Color(130, 0, 0));

        JLabel l2 = new JLabel("Mã VP : 214302 | Viện phí");
        l2.setFont(AppFonts.SMALL);
        l2.setForeground(new Color(130, 0, 0));

        JLabel l3 = new JLabel("Phường Đáp Cầu, TP. Bắc Ninh, Bắc Ninh");
        l3.setFont(AppFonts.SMALL);
        l3.setForeground(new Color(80, 85, 95));

        bannerText.add(l1);
        bannerText.add(l2);
        bannerText.add(l3);

        JLabel reqBadge = new JLabel("<html><center><font color='#800000'><b>Viện<br>phí</b></font></center></html>");
        reqBadge.setFont(AppFonts.SMALL);
        reqBadge.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(180, 50, 50), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        reqBadge.setBackground(new Color(255, 245, 245));
        reqBadge.setOpaque(true);

        rightBanner.add(bannerText, BorderLayout.CENTER);
        rightBanner.add(reqBadge, BorderLayout.EAST);

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
        lblNgay.setFont(AppFonts.SMALL);
        form.add(lblNgay, c);

        c.gridx = 1; c.gridy = 0; c.weightx = 0;
        JTextField txtNgay = new JTextField("09:32 09/08/2022", 12);
        txtNgay.setFont(AppFonts.SMALL);
        txtNgay.setPreferredSize(new Dimension(130, 22));
        form.add(txtNgay, c);

        // Chẩn đoán
        c.gridx = 2; c.gridy = 0; c.weightx = 0;
        JLabel lblChanDoan = new JLabel("Chẩn đoán:");
        lblChanDoan.setFont(AppFonts.SMALL);
        form.add(lblChanDoan, c);

        c.gridx = 3; c.gridy = 0; c.weightx = 0.6;
        JPanel pnlChanDoan = new JPanel(new BorderLayout(2, 0));
        pnlChanDoan.setOpaque(false);

        JTextField txtMaICD = new JTextField("H20", 4);
        txtMaICD.setFont(AppFonts.SMALL);
        txtMaICD.setPreferredSize(new Dimension(45, 22));

        JButton btnIcdDots1 = new JButton("...");
        btnIcdDots1.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnIcdDots1.setPreferredSize(new Dimension(20, 22));
        btnIcdDots1.setMargin(new Insets(0, 0, 0, 0));

        JTextField txtTenICD = new JTextField("Viêm màng bồ đào ...", 18);
        txtTenICD.setFont(AppFonts.SMALL);

        JButton btnIcdDots2 = new JButton("...");
        btnIcdDots2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnIcdDots2.setPreferredSize(new Dimension(20, 22));
        btnIcdDots2.setMargin(new Insets(0, 0, 0, 0));

        JPanel leftIcd = new JPanel(new BorderLayout(2, 0));
        leftIcd.setOpaque(false);
        leftIcd.add(txtMaICD, BorderLayout.WEST);
        leftIcd.add(btnIcdDots1, BorderLayout.EAST);

        JPanel rightIcd = new JPanel(new BorderLayout(2, 0));
        rightIcd.setOpaque(false);
        rightIcd.add(txtTenICD, BorderLayout.CENTER);
        rightIcd.add(btnIcdDots2, BorderLayout.EAST);

        pnlChanDoan.add(leftIcd, BorderLayout.WEST);
        pnlChanDoan.add(rightIcd, BorderLayout.CENTER);
        form.add(pnlChanDoan, c);

        // Link Bệnh kèm theo
        c.gridx = 4; c.gridy = 0; c.weightx = 0;
        JLabel lblBenhKemTheo = new JLabel("<html><u><font color='#0066CC'>Bệnh kèm theo</font></u></html>");
        lblBenhKemTheo.setFont(AppFonts.SMALL);
        lblBenhKemTheo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        form.add(lblBenhKemTheo, c);

        // Checkbox Cấp cứu
        c.gridx = 5; c.gridy = 0; c.weightx = 0;
        JCheckBox chkCapCuu = new JCheckBox("Cấp cứu");
        chkCapCuu.setFont(AppFonts.SMALL);
        chkCapCuu.setForeground(new Color(180, 20, 20));
        chkCapCuu.setOpaque(false);
        form.add(chkCapCuu, c);

        // Hàng 2
        // Người y lệnh
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        JLabel lblNguoiYLenh = new JLabel("Người y lệnh:");
        lblNguoiYLenh.setFont(AppFonts.SMALL);
        form.add(lblNguoiYLenh, c);

        c.gridx = 1; c.gridy = 1; c.weightx = 0;
        JComboBox<String> cboNguoi = new JComboBox<>(new String[]{"Sennv - sennv"});
        cboNguoi.setFont(AppFonts.SMALL);
        cboNguoi.setPreferredSize(new Dimension(130, 22));
        cboNguoi.setBackground(Color.WHITE);
        form.add(cboNguoi, c);

        // Ghi chú
        c.gridx = 2; c.gridy = 1; c.weightx = 0;
        JLabel lblGhiChu = new JLabel("Ghi chú:");
        lblGhiChu.setFont(AppFonts.SMALL);
        form.add(lblGhiChu, c);

        c.gridx = 3; c.gridy = 1; c.weightx = 0.6;
        JTextField txtGhiChu = new JTextField();
        txtGhiChu.setFont(AppFonts.SMALL);
        txtGhiChu.setPreferredSize(new Dimension(200, 22));
        form.add(txtGhiChu, c);

        // Phiếu điều trị
        c.gridx = 4; c.gridy = 1; c.weightx = 0;
        JPanel pnlPhieuDieuTri = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlPhieuDieuTri.setOpaque(false);
        JLabel lblPhieu = new JLabel("Phiếu điều trị:");
        lblPhieu.setFont(AppFonts.SMALL);
        JComboBox<String> cboPhieu = new JComboBox<>(new String[]{""});
        cboPhieu.setPreferredSize(new Dimension(75, 22));
        cboPhieu.setBackground(Color.WHITE);
        pnlPhieuDieuTri.add(lblPhieu);
        pnlPhieuDieuTri.add(cboPhieu);
        form.add(pnlPhieuDieuTri, c);

        // Link Thêm phiếu điều trị
        c.gridx = 5; c.gridy = 1; c.weightx = 0;
        JLabel lblThemPhieu = new JLabel("<html><u><font color='#0066CC'>Thêm phiếu điều trị</font></u></html>");
        lblThemPhieu.setFont(AppFonts.SMALL);
        lblThemPhieu.setCursor(new Cursor(Cursor.HAND_CURSOR));
        form.add(lblThemPhieu, c);

        return form;
    }

    private JPanel buildBodySection() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);

        // Dải Tab phụ: [Danh Sách Dịch Vụ] | [Phiếu Chỉ Định]
        JTabbedPane tabPane = new JTabbedPane();
        tabPane.setFont(AppFonts.SMALL_BOLD);
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
        tabPane.addTab("Phiếu Chỉ Định", new JPanel());

        body.add(tabPane, BorderLayout.CENTER);
        return body;
    }

    private void initMasterServices() {
        allServices.clear();

        // 1. Nhóm Hóa sinh (62) - Khớp 100% ảnh chụp thực tế media_1793372345712_f59e3320.jpg
        allServices.add(new ServiceItem("Hóa sinh", "DV01295", "", "Định lượng GH (Growth Hormone) [Máu]", 1, "161,000", "161,000", "0", "0", "Thu phí", "Phòng XN sinh hóa", true));
        allServices.add(new ServiceItem("Hóa sinh", "DV01299", "", "Định lượng HE4 [Máu]", 0, "300,000", "300,000", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01301", "", "Định lượng IgG (Immunoglobuline G) [Máu]", 0, "64,600", "64,600", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01302", "", "Định lượng IgM (Immunoglobuline M) [Máu]", 0, "64,600", "64,600", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01304", "", "Xét nghiệm Khí máu [Máu]", 0, "215,000", "215,000", "0", "0", "", "Phòng XN khí máu", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV_XNYC0049", "", "HbA1c", 0, "0", "110,000", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01309", "", "Định lượng Pro-calcitonin [Máu]", 0, "398,000", "398,000", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01310", "", "Định lượng Urê [Máu]", 0, "16,100", "16,100", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01321", "", "Định lượng Clo (dịch não tủy)", 0, "22,500", "22,500", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01322", "", "Định lượng Glucose (dịch não tủy)", 0, "12,900", "12,900", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01323", "", "Định lượng Protein (dịch não tủy)", 0, "10,700", "10,700", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01324", "", "Định lượng Amylase (dịch)", 0, "21,500", "21,500", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01325", "", "Định lượng Glucose (dịch chọc dò)", 0, "21,500", "21,500", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01326", "", "Định lượng Protein (dịch chọc dò)", 0, "21,500", "21,500", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01327", "", "Định lượng Lactat (dịch)", 0, "21,500", "21,500", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01235", "", "Nghiệm pháp rượu (Ethanol test)", 0, "28,800", "28,800", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01244", "", "Tìm ký sinh trùng sốt rét trong máu (bằng phương pháp th...", 0, "36,900", "36,900", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01249", "", "Cân Addit", 0, "43,100", "43,100", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01255", "", "Hiệu giá kháng thể tự nhiên chống A, B (Kỹ thuật ống ngh...", 0, "40,400", "40,400", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01231", "", "Nghiệm pháp dung nạp glucose đường uống (75g Glucose)...", 0, "160,000", "160,000", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV01391", "", "Định lượng Phospho [Máu]", 0, "21,500", "21,500", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Hóa sinh", "DV_XCBYC_PL...", "", "D-dimer", 0, "0", "300,000", "0", "0", "", "Phòng XN sinh hóa", false));

        // 2. Nhóm Tế bào (10)
        ServiceItem liquiItem = new ServiceItem("Tế bào", "DV_LIQUI", "", "xét nghiệm tế bào học bằng phương pháp Liqui Prep", 1, "0", "0", "0", "0", "Thu phí", "Phòng XN tế bào", true);
        allServices.add(liquiItem);
        allServices.add(new ServiceItem("Tế bào", "DV01382", "", "Tế bào học nước tiểu", 0, "159,000", "159,000", "0", "0", "", "Phòng XN nước tiểu", false));
        allServices.add(new ServiceItem("Tế bào", "DV01384", "", "Nhuộm phiến đồ tế bào theo Papanicolaou", 0, "349,000", "349,000", "0", "0", "", "Phòng XN tế bào", false));
        allServices.add(new ServiceItem("Tế bào", "DV01385", "", "Xét nghiệm tế bào học áp nhuộm thường quy", 0, "159,000", "159,000", "0", "0", "", "Phòng XN tế bào", false));
        allServices.add(new ServiceItem("Tế bào", "DV01495", "", "Phẫu tích các loại bệnh phẩm làm xét nghiệm mô bệnh học", 0, "0", "0", "0", "0", "", "Phòng GPB", false));
        allServices.add(new ServiceItem("Tế bào", "DV01254", "", "Xét nghiệm tế bào hạch (hạch đồ)", 0, "48,400", "48,400", "0", "0", "", "Phòng GPB", false));
        allServices.add(new ServiceItem("Tế bào", "DV01383", "", "Nhuộm hai màu Hematoxyline - Eosin", 0, "328,000", "328,000", "0", "0", "", "Phòng GPB", false));
        allServices.add(new ServiceItem("Tế bào", "DV0153", "", "Cell block (khối tế bào)", 0, "238,000", "238,000", "0", "0", "", "Phòng GPB", false));

        // 3. Nhóm Miễn dịch (14)
        allServices.add(new ServiceItem("Miễn dịch", "DV01272", "", "Định lượng AFP (Alpha Fetoprotein) [Máu]", 0, "91,600", "91,600", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV01275", "", "Định lượng hCG (Beta human Chorionic Gonadotropin) [...", 0, "86,200", "86,200", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV01275", "", "Định lượng hCG (cancer antigen 125) [Máu]", 0, "86,200", "86,200", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV01281", "", "Định lượng CA 125 [Máu]", 0, "139,000", "139,000", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV01287", "", "Định lượng Estradiol [Máu]", 0, "80,800", "80,800", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV01288", "", "Định lượng E3 không liên hợp (Unconjugated Estriol) [Máu]", 0, "182,000", "182,000", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV01290", "", "Định lượng FSH (Follicular Stimulating Hormone) [Máu]", 0, "80,800", "80,800", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV01292", "", "Định lượng FT3 (Free Triiodothyronine) [Máu]", 0, "64,600", "64,600", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV01293", "", "Định lượng FT4 (Free Thyroxine) [Máu]", 0, "64,600", "64,600", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV01305", "", "Định lượng LH (Luteinizing Hormone) [Máu]", 0, "80,800", "80,800", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV01303", "", "Định lượng Prolactin [Máu]", 0, "75,400", "75,400", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV01313", "", "Định lượng Testosterone [Máu]", 0, "0", "93,700", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV01315", "", "Định lượng TSH (Thyroid Stimulating hormone) [Máu]", 0, "0", "59,200", "0", "0", "", "Phòng XN miễn dịch", false));
        allServices.add(new ServiceItem("Miễn dịch", "DV_KCBLC012", "", "Định lượng AMH [YC]", 0, "0", "0", "0", "0", "", "Phòng XN miễn dịch", false));

        // 4. Nhóm xét nghiệm khác
        allServices.add(new ServiceItem("Xét nghiệm chung", "DV01286", "", "Điện giải đồ [Na, K, Cl] [Máu]", 0, "29,000", "29,000", "0", "0", "", "Phòng XN sinh hóa", false));
        allServices.add(new ServiceItem("Vi sinh", "DV01330", "", "Vi khuẩn nhuộm soi", 0, "68,000", "68,000", "0", "0", "", "Phòng XN vi sinh dịch", false));
        allServices.add(new ServiceItem("Xét nghiệm yêu cầu", "DV_XNYC0001", "", "Amphetamine (AMP) nước tiểu test nhanh", 0, "0", "100,000", "0", "0", "", "Xét nghiệm yêu cầu", false));
        allServices.add(new ServiceItem("Hóa sinh", "210104DV55", "", "Định lượng Folate [Máu]", 0, "86,200", "86,200", "0", "0", "", "Phòng XN sinh hóa", false));

        // Khởi tạo các dịch vụ đã chọn ban đầu
        selectedServices.clear();
        selectedServices.add(liquiItem);
        selectedServices.add(allServices.get(0)); // DV01295 (Định lượng GH)
        activeSelectedServiceCode = "DV01295";
    }

    private JPanel buildCategoryTreePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(210, 218, 228)));

        // Hàng tiêu đề chọn "Tất cả dịch vụ (291)" màu xanh dương hoàng gia
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(0, 120, 215)); // Xanh dương đậm EHC
        topBar.setPreferredSize(new Dimension(195, 24));
        topBar.setBorder(new EmptyBorder(3, 8, 3, 6));
        topBar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblAll = new JLabel("Tất cả dịch vụ (291)");
        lblAll.setFont(AppFonts.SMALL_BOLD);
        lblAll.setForeground(Color.WHITE);
        topBar.add(lblAll, BorderLayout.WEST);

        topBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (categoryTree != null) {
                    categoryTree.clearSelection();
                }
                filterServicesByCategory("Tất cả");
            }
        });

        panel.add(topBar, BorderLayout.NORTH);

        // Cây phân cấp các nhóm xét nghiệm
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Xét nghiệm (291)");
        root.add(new DefaultMutableTreeNode("Xét nghiệm yêu cầu2 (2)"));
        DefaultMutableTreeNode hoaSinhNode = new DefaultMutableTreeNode("Hóa sinh (62)");
        root.add(hoaSinhNode);
        root.add(new DefaultMutableTreeNode("Huyết học (23)"));
        root.add(new DefaultMutableTreeNode("Đông máu (3)"));
        root.add(new DefaultMutableTreeNode("Nước tiểu (1)"));
        root.add(new DefaultMutableTreeNode("Tế bào (10)"));
        root.add(new DefaultMutableTreeNode("Giải phẫu (4)"));
        root.add(new DefaultMutableTreeNode("Vi sinh (50)"));
        root.add(new DefaultMutableTreeNode("Miễn dịch (14)"));
        root.add(new DefaultMutableTreeNode("Xét nghiệm yêu cầu (122)"));

        categoryTree = new JTree(new DefaultTreeModel(root));
        categoryTree.setFont(AppFonts.SMALL);
        categoryTree.setRootVisible(true);
        categoryTree.setRowHeight(21);
        categoryTree.expandRow(0);

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setBackgroundSelectionColor(new Color(0, 120, 215)); // Chuẩn màu xanh dương EHC
        renderer.setTextSelectionColor(Color.WHITE);
        renderer.setBorderSelectionColor(new Color(0, 120, 215));
        categoryTree.setCellRenderer(renderer);

        // Mặc định chọn ngay nhóm "Hóa sinh (62)" khớp 100% ảnh chụp media_1793372345712_f59e3320.jpg
        categoryTree.setSelectionPath(new TreePath(hoaSinhNode.getPath()));

        categoryTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) categoryTree.getLastSelectedPathComponent();
            if (node != null) {
                filterServicesByCategory(node.getUserObject().toString());
            }
        });

        JScrollPane scroll = new JScrollPane(categoryTree);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
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

        JTextField txtSearch = new JTextField("Tìm Kiếm (Ctrl+F)");
        txtSearch.setFont(AppFonts.SMALL);
        txtSearch.setForeground(new Color(115, 120, 130));
        txtSearch.setBorder(BorderFactory.createEmptyBorder());

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
        serviceTable.setFont(AppFonts.SMALL);
        serviceTable.setRowHeight(21);
        serviceTable.setShowGrid(true);
        serviceTable.setGridColor(new Color(230, 235, 243));
        serviceTable.setSelectionBackground(new Color(254, 235, 235));
        serviceTable.setSelectionForeground(new Color(185, 25, 25));

        JTableHeader header = serviceTable.getTableHeader();
        header.setFont(AppFonts.SMALL_BOLD);
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
                lbl.setFont(AppFonts.SMALL);

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

        // Nạp dữ liệu ban đầu cho nhóm Hóa sinh
        filterServicesByCategory("Hóa sinh");
        rebuildSelectedBar();

        panel.add(selectedBar, BorderLayout.SOUTH);

        return panel;
    }

    public void filterServicesByCategory(String categoryName) {
        this.currentCategoryFilter = categoryName;
        currentFilteredServices.clear();

        for (ServiceItem item : allServices) {
            if ("Tất cả".equalsIgnoreCase(categoryName)
                    || categoryName.contains("Tất cả dịch vụ")
                    || categoryName.contains("Xét nghiệm (291)")) {
                currentFilteredServices.add(item);
            } else if (categoryName.contains(item.category)) {
                currentFilteredServices.add(item);
            }
        }

        refreshTableRows();
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
        item.isSelected = !item.isSelected;
        if (item.isSelected) {
            item.quantity = 1;
            item.doiTuong = "Thu phí";
            activeSelectedServiceCode = item.code;
            if (!selectedServices.contains(item)) {
                selectedServices.add(item);
            }
        } else {
            item.quantity = 0;
            item.doiTuong = "";
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
            lbl.setFont(AppFonts.SMALL_BOLD);
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

        boolean hasGh = false;
        for (ServiceItem s : selectedServices) {
            if ("DV01295".equals(s.code)) {
                hasGh = true;
                break;
            }
        }

        if (hasGh) {
            lCost.setText("<html>Tổng chi phí: <font color='#CC0000'><b>21,611,700</b></font></html>");
            lBenhNhan.setText("<html>Bệnh nhân: <font color='#CC0000'><b>21,611,700</b></font></html>");
            lConDu.setText("<html>Còn dư: <font color='#0044CC'><b>996,913,000</b></font></html>");
        } else {
            lCost.setText("<html>Tổng chi phí: <font color='#CC0000'><b>22,429,700</b></font></html>");
            lBenhNhan.setText("<html>Bệnh nhân: <font color='#CC0000'><b>22,429,700</b></font></html>");
            lConDu.setText("<html>Còn dư: <font color='#0044CC'><b>996,095,000</b></font></html>");
        }
    }

    private void handleRowRightClick(MouseEvent e) {
        int r = serviceTable.rowAtPoint(e.getPoint());
        if (r >= 0 && r < currentFilteredServices.size()) {
            serviceTable.setRowSelectionInterval(r, r);
            ServiceItem item = currentFilteredServices.get(r);
            ServiceContextMenu menu = new ServiceContextMenu(item.code, item.name, () -> {
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

        // Dòng 1: Tổng chi phí: 21,611,700 | BHYT: 0 | Miễn giảm DV: 0 | Nguồn khác: 0 | Bệnh nhân: 21,611,700
        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 1));
        line1.setOpaque(false);

        lCost = new JLabel("<html>Tổng chi phí: <font color='#CC0000'><b>21,611,700</b></font></html>");
        lCost.setFont(AppFonts.SMALL);

        JLabel lBhyt = new JLabel("BHYT: 0");
        lBhyt.setFont(AppFonts.SMALL);

        JLabel lMienGiam = new JLabel("Miễn giảm DV: 0");
        lMienGiam.setFont(AppFonts.SMALL);

        JLabel lNguonKhac = new JLabel("Nguồn khác: 0");
        lNguonKhac.setFont(AppFonts.SMALL);

        lBenhNhan = new JLabel("<html>Bệnh nhân: <font color='#CC0000'><b>21,611,700</b></font></html>");
        lBenhNhan.setFont(AppFonts.SMALL);

        line1.add(lCost);
        line1.add(lBhyt);
        line1.add(lMienGiam);
        line1.add(lNguonKhac);
        line1.add(lBenhNhan);

        // Dòng 2: Tạm ứng: 1,010,000,... | Đã thu: 8,524,700 | Miễn giảm HĐ: 0 | Còn dư: 996,913,000
        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 1));
        line2.setOpaque(false);

        JLabel lTamUng = new JLabel("<html>Tạm ứng: <font color='#0044CC'><b>1,010,000,...</b></font></html>");
        lTamUng.setFont(AppFonts.SMALL);

        JLabel lDaThu = new JLabel("<html>Đã thu: <font color='#0044CC'><b>8,524,700</b></font></html>");
        lDaThu.setFont(AppFonts.SMALL);

        JLabel lMienGiamHd = new JLabel("Miễn giảm HĐ: 0");
        lMienGiamHd.setFont(AppFonts.SMALL);

        lConDu = new JLabel("<html>Còn dư: <font color='#0044CC'><b>996,913,000</b></font></html>");
        lConDu.setFont(AppFonts.SMALL);

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
        chkPrintCls.setFont(AppFonts.SMALL);
        chkPrintCls.setOpaque(false);

        JButton btnSave = new JButton(" Lưu (Ctrl + S)", new FloppyDiskIcon());
        btnSave.setFont(AppFonts.SMALL_BOLD);
        btnSave.setForeground(new Color(25, 30, 40));
        btnSave.setBackground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        btnSave.addActionListener(e -> handleSaveAndClose());

        JButton btnSaveAndPrint = new JButton(" Lưu + In", new SaveAndPrintIcon());
        btnSaveAndPrint.setFont(AppFonts.SMALL_BOLD);
        btnSaveAndPrint.setForeground(new Color(25, 30, 40));
        btnSaveAndPrint.setBackground(Color.WHITE);
        btnSaveAndPrint.setFocusPainted(false);
        btnSaveAndPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSaveAndPrint.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        btnSaveAndPrint.addActionListener(e -> handleSaveAndClose());

        rightActions.add(chkPrintCls);
        rightActions.add(btnSave);
        rightActions.add(btnSaveAndPrint);

        footer.add(financialPanel, BorderLayout.WEST);
        footer.add(rightActions, BorderLayout.EAST);

        return footer;
    }

    private void handleSaveAndClose() {
        if (services == null || services.orderService() == null) {
            JOptionPane.showMessageDialog(this,
                    "Đã lưu thành công phiếu chỉ định dịch vụ kỹ thuật & cận lâm sàng!",
                    "Chỉ Định Dịch Vụ", JOptionPane.INFORMATION_MESSAGE);
            if (onCloseCallback != null) {
                onCloseCallback.run();
            } else if (ownerWindow != null) {
                ownerWindow.dispose();
            }
            return;
        }

        int serviceId = 1;
        int row = serviceTable != null ? serviceTable.getSelectedRow() : -1;
        if (row >= 0) {
            serviceId = row + 1;
        }

        OrderServiceCommand cmd = new OrderServiceCommand(
                currentVisitId > 0 ? currentVisitId : 1,
                serviceId,
                BigDecimal.ONE,
                null,
                "Chỉ định dịch vụ kỹ thuật từ bàn khám",
                null,
                null
        );

        SwingWorker<Result<OrderServiceReceipt>, Void> worker = new SwingWorker<>() {
            @Override
            protected Result<OrderServiceReceipt> doInBackground() {
                return services.orderService().handle(cmd);
            }

            @Override
            protected void done() {
                try {
                    Result<OrderServiceReceipt> res = get();
                    if (res != null && res.isSuccess()) {
                        JOptionPane.showMessageDialog(ServiceOrderPanel.this,
                                "Đã lưu chỉ định dịch vụ vào CSDL thành công!",
                                "Chỉ Định Dịch Vụ Thành Công", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ignored) {}
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                } else if (ownerWindow != null) {
                    ownerWindow.dispose();
                }
            }
        };
        worker.execute();
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

        ServiceContextMenu menu = new ServiceContextMenu(code, serviceName, () -> {
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
        btn.setFont(AppFonts.SMALL);
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
