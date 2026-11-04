package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện màn hình: "ĐHC - Chỉ Định Thuốc"
 * Kích hoạt khi nhấn "Chỉ Định Thuốc" (hoặc Ctrl+T) từ Menu Thuốc/VT trên màn hình Khám Bệnh.
 * Khớp 100% chi tiết trong ảnh chụp mẫu thực tế:
 * - Sub-Toolbar: Menu, Đơn Thuốc Mẫu ▾, Đơn Thuốc Cũ
 * - Banner bệnh nhân góc phải: TEST11 | 22000285 | Viện phí
 * - Khung thông tin y lệnh 3 hàng: Ngày y lệnh, Chẩn đoán, Bác sĩ điều trị, Ngày tái khám, Lời dặn BS, Đợt dùng thuốc
 * - Dải Tab: Danh Sách Thuốc (active) | Phiếu Chỉ thực | [✉ Thông báo (0)] ❚❚
 * - Cột trái: Cây danh mục Nhà thuốc -> Tất Cả Thuốc (204)
 * - Khung kê đơn 3 hàng: Tên thuốc (tự động gợi ý tìm kiếm khi gõ), Đóng gói, Đơn vị, Số lượng, Đường dùng, HDSD, Ngày dùng, Sáng, Trưa, Chiều, Tối, [✓ OK (Ctrl+A)]
 * - Bảng danh mục 18 thuốc mẫu 6 cột (kèm cột "Giá Bán BV")
 * - Thanh trạng thái đáy bảng thuốc đã kê: "★ Attapulgite;Aluminum hydroxyde and magnesium carbonate (Gastropulgite) 2,5g + 0,5g (10)"
 * - Thanh tài chính chân trang cập nhật tự động: Tiền đơn: 33,570, Tổng chi phí: 21,645,270, Bệnh nhân: 21,645,270, Còn dư: 996,879,430
 */
public class PrescriptionOrderPanel extends JPanel {

    public static class MedicineSearchItem {
        public final String code;
        public final String name;
        public final String displayString;
        public final String unit;
        public final String pack;
        public final String stock;
        public final String price;
        public final String route;
        public final String hdsd;

        public MedicineSearchItem(String code, String name, String unit, String pack, String stock, String price, String route, String hdsd) {
            this.code = code;
            this.name = name;
            this.displayString = code + "-" + name;
            this.unit = unit;
            this.pack = pack;
            this.stock = stock;
            this.price = price;
            this.route = route;
            this.hdsd = hdsd;
        }

        @Override
        public String toString() {
            return displayString;
        }
    }

    private final Runnable onCloseCallback;
    private JTable medicineTable;
    private DefaultTableModel tableModel;

    // Các trường form nhập thuốc
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

    // Thanh trạng thái đáy bảng thuốc đã kê
    private JPanel orderStatusBar;
    private JLabel lblOrderStar;

    // Nhãn số liệu tài chính chân trang (cập nhật động)
    private JLabel lblValTienDon;
    private JLabel lblValTongChiPhi;
    private JLabel lblValBhyt;
    private JLabel lblValNguonKhac;
    private JLabel lblValBenhNhan;
    private JLabel lblValTamUng;
    private JLabel lblValDaThu;
    private JLabel lblValMienGiam;
    private JLabel lblValConDu;

    // Popup gợi ý tìm kiếm thuốc thông minh khi gõ
    private JPopupMenu suggestPopup;
    private DefaultListModel<MedicineSearchItem> suggestListModel;
    private JList<MedicineSearchItem> suggestList;
    private final List<MedicineSearchItem> allMedicines = new ArrayList<>();
    private boolean isSettingTextProgrammatically = false;

    public PrescriptionOrderPanel() {
        this(null);
    }

    public PrescriptionOrderPanel(Runnable onCloseCallback) {
        this.onCloseCallback = onCloseCallback;
        initMedicineCatalog();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. THANH TIÊU ĐỀ PHỤ + BANNER THÔNG TIN BỆNH NHÂN GÓC PHẢI
        add(buildHeaderSection(), BorderLayout.NORTH);

        // 2. PHẦN THÂN CHÍNH: DẢI TAB + SPLIT PANE (CÂY DANH MỤC NHÀ THUỐC & BẢNG THUỐC)
        add(buildBodySection(), BorderLayout.CENTER);

        // 3. THANH TÀI CHÍNH & CỤM NÚT THAO TÁC CHÂN TRANG
        add(buildFooterSection(), BorderLayout.SOUTH);
    }

    private void initMedicineCatalog() {
        allMedicines.clear();
        // 17 mặt hàng thuốc/vật tư khớp 100% kết quả tìm kiếm khi gõ "ma" trong ảnh chụp thực tế
        allMedicines.add(new MedicineSearchItem("23214", "Miếng Merocel cầm máu mũi", "Miếng", "Hộp 10 m....", "29,706", "138,000", "Đặt", "Cầm máu mũi theo chỉ định BS"));
        allMedicines.add(new MedicineSearchItem("22862", "Attapulgite;Aluminum hydroxyde and magnesium carbonate (Gastropulgite) 2,5g + 0,5g", "Gói", "Hộp 30 gói, Bột ph", "1,964", "3,357", "Uống", "uống ngày 1 lần"));
        allMedicines.add(new MedicineSearchItem("23131", "Azythromycin dihydrate (Zitromax) 200mg/5ml", "Lọ", "Hộp 1 lọ", "50", "120,000", "Uống", "Uống 1 lần trong ngày"));
        allMedicines.add(new MedicineSearchItem("17679", "Desogestrel + Ethinylestradiol (Marvelon Tab 21's) 0,15mg + 0,03mg", "Viên", "Hộp 1 vỉ", "120", "65,000", "Uống", "Uống ngày 1 viên"));
        allMedicines.add(new MedicineSearchItem("17961", "Domperidone maleate (Motilium-M) 10mg", "Viên", "Hộp 10 vỉ", "850", "2,500", "Uống", "Uống trước ăn 15-30 phút"));
        allMedicines.add(new MedicineSearchItem("19671", "Esomeprazole (dưới dạng Esomeprazole magnesi trihydrate) (Nexium) 10", "Gói", "Hộp 28 gói", "340", "23,000", "Uống", "Uống trước bữa ăn sáng"));
        allMedicines.add(new MedicineSearchItem("23209", "Monobasic natri phosphat + Dibasic natri phosphat (Golytie-enema) 21,4", "Chai", "Chai 133ml", "85", "45,000", "Thụt", "Thụt trực tràng theo chỉ định"));
        allMedicines.add(new MedicineSearchItem("22709", "Sắt (III) hydroxyd polymaltose (Fogyma) 50mg/ 10ml", "Ống", "Hộp 20 ống", "420", "8,500", "Uống", "Uống trong hoặc sau bữa ăn"));
        allMedicines.add(new MedicineSearchItem("22751", "DHA+Canxi+Vitamin C+Magiê +Sắt + Kẽm + Vitamin E + Vitamin B6 +", "Viên", "Lọ 60 viên", "150", "350,000", "Uống", "Uống sáng sau bữa ăn"));
        allMedicines.add(new MedicineSearchItem("22752", "Calcium Citrate , Vitamin D3 , Magnesium (Calcitrat [NMI]) 1500mg, Dịch", "Viên", "Lọ 60 viên", "210", "280,000", "Uống", "Uống sáng sau bữa ăn"));
        allMedicines.add(new MedicineSearchItem("13544", "Dịch chiết Trà xanh, Dịch chiết Cỏ mần trầu, Dịch chiết Khổ qua, Dịch chi", "Chai", "Chai 250ml", "95", "98,000", "Dùng ngoài", "Tắm gội ngoài da"));
        allMedicines.add(new MedicineSearchItem("22235", "Dầu hạt hướng dương, Dầu Ôliu,chiết xuất lô hội, dầu hạt Macca Mỏ hạt c", "Chai", "Chai 100ml", "60", "115,000", "Dùng ngoài", "Bôi ngoài da vùng tổn thương"));
        allMedicines.add(new MedicineSearchItem("23055", "Dịch chiết cây cơm cháy, Dịch chiết Cây bướm bạc, Chiết xuất Quả cơm ch", "Chai", "Chai 120ml", "75", "85,000", "Uống", "Uống ngày 2 lần"));
        allMedicines.add(new MedicineSearchItem("22948", "Mask thở khí dung trẻ em", "Cái", "Cái", "310", "18,000", "Thở", "Khí dung theo chỉ định"));
        allMedicines.add(new MedicineSearchItem("18131", "Nước biển phun sương vô trùng (đẳng trương) (Sterimar)", "Chai", "Chai 100ml", "240", "95,000", "Xịt mũi", "Xịt mũi ngày 2-3 lần"));
        allMedicines.add(new MedicineSearchItem("(18131", "Nước biển phun sương vô trùng (đẳng trương) (Sterimar) , dặm, đẳng (St", "Chai", "Chai 50ml", "180", "75,000", "Xịt mũi", "Xịt mũi ngày 2-3 lần"));
        allMedicines.add(new MedicineSearchItem("17915", "Nước biển phun sương vô trùng (đẳng trương) (Sterimar) , đẳng Maxcan", "Chai", "Chai 70ml", "130", "82,000", "Xịt mũi", "Xịt mũi vệ sinh"));

        // Danh mục các thuốc điều trị phổ biến khác
        allMedicines.add(new MedicineSearchItem("10214", "Paracetamol (Mypara 500) 500mg", "Viên", "Hộp 10 vỉ...", "4,601", "210", "Uống", "Uống khi sốt > 38.5 độ C"));
        allMedicines.add(new MedicineSearchItem("10215", "Natri clorid +Natri citrat +Kali clorid +Glucose khan +Kẽm Gluconat...", "Gói", "Bột uống ...", "7,987", "2,749.99", "Uống", "Pha 1 gói với 200ml nước đun sôi để nguội"));
        allMedicines.add(new MedicineSearchItem("10216", "Progesteron (Progesterone Injection BP 25mg) 25mg/ml", "Ống", "Hộp 1 vỉ x ...", "81", "17,850", "Tiêm", "Tiêm bắp sâu theo y lệnh"));
        allMedicines.add(new MedicineSearchItem("10218", "Dầu Parafin (vô trùng) (Dầu Parafin BFS) 5ml", "Ống", "Hộp 50 ốn....", "79", "6,600", "Uống", "Uống nhuận tràng"));
        allMedicines.add(new MedicineSearchItem("10219", "Progesterone 400mg (CYCLOGEST 400mg)", "Viên", "Hộp 15 vỉ....", "231", "27,333.33", "Đặt", "Đặt âm đạo/hậu môn buổi tối"));
        allMedicines.add(new MedicineSearchItem("10220", "Cliblicin (ZIBOTEX) 100mg/ml lọ 50ml", "Lọ", "Hộp 1 lọo ...", "85", "325,000", "Tiêm truyền", "Truyền tĩnh mạch chậm"));
        allMedicines.add(new MedicineSearchItem("10221", "Acetylcysteine (Exomuc) 200mg", "Gói", "Hộp 30 gó...", "190", "4,150", "Uống", "Hòa tan trong nửa ly nước"));
        allMedicines.add(new MedicineSearchItem("10222", "Albendazole (Zentel Tab 200mg)", "Viên", "Hộp 1 vỉ x ...", "46", "5,992", "Uống", "Nhai hoặc nuốt cả viên"));
        allMedicines.add(new MedicineSearchItem("10223", "Ambroxol (Halixol) 15mg/5ml, dung dịch, lnh, hỗn dịch uống 15m...", "Lọ", "Hộp 1 lọ ...", "415", "72,760", "Uống", "Uống sau khi ăn"));
        allMedicines.add(new MedicineSearchItem("10224", "Ambroxol (Oleosam) 30mg/5ml,100ml", "Lọ", "Hộp 1 lọ l ...", "83", "42,243", "Uống", "Uống sau khi ăn"));
        allMedicines.add(new MedicineSearchItem("10225", "Amoxicillin (dưới dạng Amoxicillin trihydrate) (Clamoxyl 250mg)", "Gói", "Hộp 12 gó...", "33", "5,446", "Uống", "Uống trước bữa ăn"));
        allMedicines.add(new MedicineSearchItem("10226", "Amoxicillin (dưới dạng Amoxicillin trihydrate) 500mg;Acid Clavulan...", "Viên", "Hộp 2 vỉ x...", "224", "12,771", "Uống", "Uống vào đầu bữa ăn"));
        allMedicines.add(new MedicineSearchItem("10227", "Amoxicillin + Acid Clavulanic (Vigentin 500/62,5 DT) 500mg + 62,5...", "Viên", "Hộp 12 gói", "6,873", "8,774", "Uống", "Uống trước bữa ăn"));
        allMedicines.add(new MedicineSearchItem("10228", "Amoxicillin trihydrate + Acid Clavulanic (Augmentin 250/31.25 Sac...", "Gói", "Hộp 12 gó...", "779", "11,416", "Uống", "Uống vào đầu bữa ăn"));
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
        leftActions.add(createSubToolbarButton("Đơn Thuốc Mẫu", "template", true));
        leftActions.add(createSubToolbarButton("Đơn Thuốc Cũ", "history", false));

        topRow.add(leftActions, BorderLayout.WEST);

        // Banner bệnh nhân góc phải (màu đỏ sẫm chuẩn ảnh)
        JPanel rightBanner = new JPanel(new BorderLayout(8, 0));
        rightBanner.setOpaque(false);
        rightBanner.setBorder(new EmptyBorder(2, 6, 2, 12));

        JPanel textPanel = new JPanel(new GridLayout(3, 1, 0, 0));
        textPanel.setOpaque(false);

        JLabel l1 = new JLabel("TEST11 | 22000285 | 2000 (22 tuổi) | Nam", SwingConstants.RIGHT);
        l1.setFont(AppFonts.SMALL_BOLD);
        l1.setForeground(new Color(125, 20, 20));

        JLabel l2 = new JLabel("Mã VP : 214302 | Viện phí", SwingConstants.RIGHT);
        l2.setFont(AppFonts.SMALL);
        l2.setForeground(new Color(125, 20, 20));

        JLabel l3 = new JLabel("Phường Đáp Cầu, TP. Bắc Ninh, Bắc Ninh", SwingConstants.RIGHT);
        l3.setFont(AppFonts.SMALL);
        l3.setForeground(new Color(125, 20, 20));

        textPanel.add(l1);
        textPanel.add(l2);
        textPanel.add(l3);

        JLabel lblTag = new JLabel("<html><center>Viện<br>phí</center></html>", SwingConstants.CENTER);
        lblTag.setFont(AppFonts.BODY_BOLD);
        lblTag.setForeground(new Color(145, 20, 20));
        lblTag.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(210, 220, 232)),
                new EmptyBorder(0, 10, 0, 4)
        ));

        rightBanner.add(textPanel, BorderLayout.CENTER);
        rightBanner.add(lblTag, BorderLayout.EAST);

        topRow.add(rightBanner, BorderLayout.EAST);
        headerPanel.add(topRow);

        // Hàng 2: Khung thông tin y lệnh 3 dòng
        headerPanel.add(buildOrderInfoGrid());

        return headerPanel;
    }

    private JPanel buildOrderInfoGrid() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(248, 250, 254));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)),
                new EmptyBorder(4, 12, 4, 12)
        ));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 4, 2, 4);

        // Hàng 1: Ngày y lệnh: [10:23 09/08/2022 ▾] | Chẩn đoán: [H20 ▾] [...] [Viêm màng bồ đào ... ▾] [...] Bệnh kèm theo
        c.gridy = 0;
        c.gridx = 0; p.add(createFieldLabel("Ngày y lệnh:"), c);
        c.gridx = 1; p.add(createDropdown("10:23 09/08/2022", 140), c);

        c.gridx = 2; p.add(createFieldLabel("Chẩn đoán:"), c);
        c.gridx = 3; p.add(createDropdown("H20", 65), c);
        c.gridx = 4; p.add(createMiniBrowseButton(), c);
        c.gridx = 5; p.add(createDropdown("Viêm màng bồ đào ...", 320), c);
        c.gridx = 6; p.add(createMiniBrowseButton(), c);

        JLabel lblSecondaryDisease = new JLabel("Bệnh kèm theo");
        lblSecondaryDisease.setFont(AppFonts.SMALL_BOLD);
        lblSecondaryDisease.setForeground(new Color(0, 75, 180));
        lblSecondaryDisease.setCursor(new Cursor(Cursor.HAND_CURSOR));
        c.gridx = 7; c.gridwidth = 3; p.add(lblSecondaryDisease, c);

        // Hàng 2: Bác sĩ điều trị: [Sennv - sennv] | Ngày tái khám: [// ▾] Sau: [0] ngày | Lời dặn BS: [ ... ] [...]
        c.gridwidth = 1;
        c.gridy = 1;
        c.gridx = 0; p.add(createFieldLabel("Bác sĩ điều trị:"), c);
        JTextField txtDoctor = new JTextField("Sennv - sennv");
        txtDoctor.setFont(AppFonts.SMALL);
        txtDoctor.setPreferredSize(new Dimension(140, 23));
        c.gridx = 1; p.add(txtDoctor, c);

        c.gridx = 2; p.add(createFieldLabel("Ngày tái khám:"), c);
        c.gridx = 3; p.add(createDropdown("//", 65), c);

        JPanel pDays = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        pDays.setOpaque(false);
        pDays.add(new JLabel("Sau:"));
        JTextField txtDaysAfter = new JTextField("0");
        txtDaysAfter.setPreferredSize(new Dimension(30, 22));
        txtDaysAfter.setHorizontalAlignment(SwingConstants.CENTER);
        pDays.add(txtDaysAfter);
        JLabel lblNgayRed = new JLabel("ngày");
        lblNgayRed.setFont(AppFonts.SMALL_BOLD);
        lblNgayRed.setForeground(new Color(180, 25, 25));
        pDays.add(lblNgayRed);
        c.gridx = 4; c.gridwidth = 2; p.add(pDays, c);

        c.gridwidth = 1;
        c.gridx = 6; p.add(createFieldLabel("Lời dặn BS:"), c);
        JTextField txtNote = new JTextField();
        txtNote.setPreferredSize(new Dimension(280, 23));
        c.gridx = 7; p.add(txtNote, c);
        c.gridx = 8; p.add(createMiniBrowseButton(), c);

        // Hàng 3: Đợt dùng thuốc: [0] | Từ ngày: [// ▾] Đến: [// ▾]
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

        // Dải Tab: Danh Sách Thuốc (active) | Phiếu Chỉ thực
        JTabbedPane mainTabs = new JTabbedPane();
        mainTabs.setFont(AppFonts.BODY_BOLD);
        mainTabs.setBackground(new Color(235, 240, 248));

        mainTabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected) {
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(new Color(212, 226, 243));
                }
                g2.fillRect(x, y, w, h);
                g2.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                g.setColor(new Color(175, 195, 220));
                g.drawRect(x, y, w, h);
            }
        });

        // Tab chính: Danh Sách Thuốc
        JComponent drugListTab = buildDrugListContent();
        mainTabs.addTab("Danh Sách Thuốc", drugListTab);
        mainTabs.addTab("Phiếu Chỉ thực", new JPanel());

        // Thanh tiêu đề dải tab kèm thông báo góc phải [✉ Thông báo (0)] ||
        JPanel tabHeaderContainer = new JPanel(new BorderLayout());
        tabHeaderContainer.setBackground(Color.WHITE);

        JPanel pNotif = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        pNotif.setOpaque(false);
        JLabel lblNotif = new JLabel("Thông báo (0)", new EnvelopeIcon(), SwingConstants.RIGHT);
        lblNotif.setFont(AppFonts.SMALL_BOLD);
        lblNotif.setForeground(new Color(0, 75, 175));
        JLabel lblPause = new JLabel(new PauseBarsIcon());
        pNotif.add(lblNotif);
        pNotif.add(lblPause);

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

        // --- CỘT TRÁI: CÂY DANH MỤC NHÀ THUỐC ---
        JPanel treeContainer = new JPanel(new BorderLayout());
        treeContainer.setBackground(Color.WHITE);
        treeContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(205, 214, 226)));

        JLabel lblTreeHeader = new JLabel("Nhà thuốc");
        lblTreeHeader.setFont(AppFonts.SMALL_BOLD);
        lblTreeHeader.setForeground(new Color(135, 20, 20)); // Màu đỏ sẫm chuẩn ảnh
        lblTreeHeader.setBorder(new EmptyBorder(4, 6, 4, 6));
        treeContainer.add(lblTreeHeader, BorderLayout.NORTH);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Tất Cả Thuốc (204)");
        DefaultMutableTreeNode nodeThuoc = new DefaultMutableTreeNode("Thuốc (204)");
        DefaultMutableTreeNode nodeBhyt = new DefaultMutableTreeNode("Nhóm BHYT (204)");
        root.add(nodeThuoc);
        root.add(nodeBhyt);

        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setFont(AppFonts.SMALL_BOLD);
        tree.setRootVisible(true);
        tree.setRowHeight(22);
        tree.setSelectionRow(0);

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean exp, boolean leaf, int row, boolean hasFocus) {
                JLabel l = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, hasFocus);
                l.setFont(AppFonts.SMALL_BOLD);
                if (sel) {
                    l.setOpaque(true);
                    l.setBackground(new Color(0, 120, 215)); // Màu xanh đậm hoàng gia chuẩn ảnh
                    l.setForeground(Color.WHITE);
                } else {
                    l.setOpaque(false);
                    l.setForeground(new Color(25, 25, 25));
                }
                return l;
            }
        });

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setBorder(BorderFactory.createEmptyBorder());
        treeScroll.getViewport().setBackground(Color.WHITE);
        treeContainer.add(treeScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(treeContainer);

        // --- CỘT PHẢI: KHUNG KÊ ĐƠN THUỐC 3 HÀNG + BẢNG DANH MỤC THUỐC ---
        JPanel rightContent = new JPanel(new BorderLayout());
        rightContent.setBackground(Color.WHITE);

        // Khung kê đơn thuốc 3 hàng
        JPanel entryPanel = new JPanel(new GridBagLayout());
        entryPanel.setBackground(new Color(248, 250, 253));
        entryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 218, 228)),
                new EmptyBorder(4, 6, 4, 6)
        ));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 3, 2, 3);

        // Hàng 1: Tên thuốc: [ TextField ] [...] Tên kho: [ 0 ] Đóng gói: [ ]
        c.gridy = 0; c.gridx = 0; entryPanel.add(createFieldLabel("Tên thuốc:"), c);

        JPanel pDrugName = new JPanel(new BorderLayout(2, 0));
        pDrugName.setOpaque(false);
        txtDrugName = new JTextField();
        txtDrugName.setFont(AppFonts.BODY);
        txtDrugName.setPreferredSize(new Dimension(320, 23));
        txtDrugName.setBackground(Color.WHITE);
        pDrugName.add(txtDrugName, BorderLayout.CENTER);
        JButton btnBrowseDrug = createMiniBrowseButton();
        pDrugName.add(btnBrowseDrug, BorderLayout.EAST);
        c.gridx = 1; c.gridwidth = 4; entryPanel.add(pDrugName, c);

        c.gridwidth = 1;
        c.gridx = 5; entryPanel.add(createFieldLabel("Tên kho:"), c);
        txtKho = new JTextField("0");
        txtKho.setPreferredSize(new Dimension(45, 23));
        txtKho.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridx = 6; entryPanel.add(txtKho, c);

        c.gridx = 7; entryPanel.add(createFieldLabel("Đóng gói:"), c);
        txtPack = new JTextField();
        txtPack.setPreferredSize(new Dimension(70, 23));
        c.gridx = 8; c.gridwidth = 2; entryPanel.add(txtPack, c);

        // Hàng 2: Đơn vị: [ ] Số lượng: [ 0 ] Đường dùng: [ ] [...] HDSD: [ ] [...]
        c.gridwidth = 1;
        c.gridy = 1; c.gridx = 0; entryPanel.add(createFieldLabel("Đơn vị:"), c);
        txtUnit = new JTextField();
        txtUnit.setPreferredSize(new Dimension(65, 23));
        c.gridx = 1; entryPanel.add(txtUnit, c);

        c.gridx = 2; entryPanel.add(createFieldLabel("Số lượng:"), c);
        txtQty = new JTextField("0");
        txtQty.setFont(AppFonts.BODY_BOLD);
        txtQty.setPreferredSize(new Dimension(45, 23));
        txtQty.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridx = 3; entryPanel.add(txtQty, c);

        c.gridx = 4; entryPanel.add(createFieldLabel("Đường dùng:"), c);
        JPanel pRoute = new JPanel(new BorderLayout(2, 0));
        pRoute.setOpaque(false);
        txtRoute = new JTextField();
        txtRoute.setPreferredSize(new Dimension(120, 23));
        pRoute.add(txtRoute, BorderLayout.CENTER);
        pRoute.add(createMiniBrowseButton(), BorderLayout.EAST);
        c.gridx = 5; c.gridwidth = 2; entryPanel.add(pRoute, c);

        c.gridwidth = 1;
        c.gridx = 7; entryPanel.add(createFieldLabel("HDSD:"), c);
        JPanel pHdsd = new JPanel(new BorderLayout(2, 0));
        pHdsd.setOpaque(false);
        txtHdsd = new JTextField();
        txtHdsd.setPreferredSize(new Dimension(280, 23));
        pHdsd.add(txtHdsd, BorderLayout.CENTER);
        pHdsd.add(createMiniBrowseButton(), BorderLayout.EAST);
        c.gridx = 8; c.gridwidth = 2; entryPanel.add(pHdsd, c);

        // Hàng 3: Ngày dùng: [ 0 ] Sáng: [ ] Trưa: [ ] Chiều: [ ] Tối: [ ] [✓ OK (Ctrl+A)] [OK (Ctrl+A)]
        c.gridwidth = 1;
        c.gridy = 2; c.gridx = 0; entryPanel.add(createFieldLabel("Ngày dùng:"), c);
        txtDays = new JTextField("0");
        txtDays.setPreferredSize(new Dimension(65, 23));
        txtDays.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridx = 1; entryPanel.add(txtDays, c);

        JPanel pDoseTimes = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pDoseTimes.setOpaque(false);
        pDoseTimes.add(new JLabel("Sáng:"));
        txtMorning = new JTextField(); txtMorning.setPreferredSize(new Dimension(40, 23));
        pDoseTimes.add(txtMorning);
        pDoseTimes.add(new JLabel("Trưa:"));
        txtNoon = new JTextField(); txtNoon.setPreferredSize(new Dimension(40, 23));
        pDoseTimes.add(txtNoon);
        pDoseTimes.add(new JLabel("Chiều:"));
        txtAfternoon = new JTextField(); txtAfternoon.setPreferredSize(new Dimension(40, 23));
        pDoseTimes.add(txtAfternoon);
        pDoseTimes.add(new JLabel("Tối:"));
        txtNight = new JTextField(); txtNight.setPreferredSize(new Dimension(40, 23));
        pDoseTimes.add(txtNight);
        c.gridx = 2; c.gridwidth = 5; entryPanel.add(pDoseTimes, c);

        // Cụm 2 nút OK bên phải hàng 3 khớp 100% ảnh chụp sau khi bấm OK
        JPanel pOkButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pOkButtons.setOpaque(false);

        btnOk = new JButton(" OK (Ctrl+A)", new CheckmarkBlueIcon(new Color(160, 165, 175)));
        btnOk.setFont(AppFonts.BODY_BOLD);
        btnOk.setBackground(new Color(238, 241, 246));
        btnOk.setForeground(new Color(150, 155, 165));
        btnOk.setOpaque(true);
        btnOk.setFocusPainted(false);
        btnOk.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOk.setPreferredSize(new Dimension(115, 24));
        btnOk.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(205, 212, 222), 1),
                new EmptyBorder(1, 4, 1, 4)
        ));
        btnOk.addActionListener(e -> onConfirmDrugOrder());

        JButton btnSecondaryOk = new JButton("OK (Ctrl+A)");
        btnSecondaryOk.setFont(AppFonts.SMALL);
        btnSecondaryOk.setBackground(Color.WHITE);
        btnSecondaryOk.setForeground(new Color(25, 25, 25));
        btnSecondaryOk.setOpaque(true);
        btnSecondaryOk.setFocusPainted(false);
        btnSecondaryOk.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSecondaryOk.setPreferredSize(new Dimension(95, 24));
        btnSecondaryOk.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(195, 205, 218), 1),
                new EmptyBorder(1, 4, 1, 4)
        ));
        btnSecondaryOk.addActionListener(e -> onConfirmDrugOrder());

        pOkButtons.add(btnOk);
        pOkButtons.add(btnSecondaryOk);

        c.gridwidth = 3;
        c.gridx = 7;
        entryPanel.add(pOkButtons, c);

        rightContent.add(entryPanel, BorderLayout.NORTH);

        // KHỞI TẠO POPUP GỢI Ý TÌM KIẾM THUỐC
        setupMedicineAutocomplete(btnBrowseDrug);

        // Bảng danh mục 18 thuốc mẫu 6 cột
        String[] cols = {
                "Tên Thuốc",
                "Đơn Vị",
                "Quy Cách",
                "Tồn kho",
                "Giá Bán BV",
                "Đối Tượng"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        // 18 dòng thuốc khớp 100% ảnh chụp mẫu
        tableModel.addRow(new Object[]{"Paracetamol (Mypara 500) 500mg", "Viên", "Hộp 10 vỉ ...", "4,601", "210", ""});
        tableModel.addRow(new Object[]{"Natri clorid +Natri citrat +Kali clorid +Glucose khan +Kẽm Gluconat...", "Gói", "Bột uống ...", "7,987", "2,749.99", ""});
        tableModel.addRow(new Object[]{"Progesteron (Progesterone Injection BP 25mg) 25mg/ml", "Ống", "Hộp 1 vỉ x ...", "81", "17,850", ""});
        tableModel.addRow(new Object[]{"Miếng Merocel cầm máu mũi", "Miếng", "Hộp 10 m....", "29,706", "138,000", ""});
        tableModel.addRow(new Object[]{"Dầu Parafin (vô trùng) (Dầu Parafin BFS) 5ml", "Ống", "Hộp 50 ốn....", "79", "6,600", ""});
        tableModel.addRow(new Object[]{"Progesterone 400mg (CYCLOGEST 400mg)", "Viên", "Hộp 15 vỉ....", "231", "27,333.33", ""});
        tableModel.addRow(new Object[]{"Cliblicin (ZIBOTEX) 100mg/ml lọ 50ml", "Lọ", "Hộp 1 lọo ...", "85", "325,000", ""});
        tableModel.addRow(new Object[]{"Acetylcysteine (Exomuc) 200mg", "Gói", "Hộp 30 gó...", "190", "4,150", ""});
        tableModel.addRow(new Object[]{"Albendazole (Zentel Tab 200mg)", "Viên", "Hộp 1 vỉ x ...", "46", "5,992", ""});
        tableModel.addRow(new Object[]{"Ambroxol (Halixol) 15mg/5ml, dung dịch, nhũ dịch, hỗn dịch uống 15m...", "Lọ", "Hộp 1 lọ ...", "415", "72,760", ""});
        tableModel.addRow(new Object[]{"Ambroxol (Oleosam) 30mg/5ml; 100ml", "Lọ", "Hộp 1 lọ l ...", "83", "42,243", ""});
        tableModel.addRow(new Object[]{"Amoxicillin (dưới dạng Amoxicillin trihydrate) (Clamoxyl 250mg)", "Gói", "Hộp 12 gó...", "33", "5,446", ""});
        tableModel.addRow(new Object[]{"Amoxicillin (dưới dạng Amoxicillin trihydrate) 500mg;Acid Clavulan...", "Viên", "Hộp 2 vỉ x...", "224", "12,771", ""});
        tableModel.addRow(new Object[]{"Amoxicillin (dưới dạng Amoxicillin trihydrate) 500mg;Acid Clavulan...", "Gói", "Hộp 12 gó...", "977", "17,134", ""});
        tableModel.addRow(new Object[]{"Amoxicillin + Acid Clavulanic (Vigentin 500/62,5 DT) 500mg + 62,5...", "Viên", "Hộp 12 gói", "6,873", "8,774", ""});
        tableModel.addRow(new Object[]{"Amoxicillin trihydrate + Acid Clavulanic (Augmentin 250/31.25 Sac...", "Gói", "Hộp 12 gó...", "779", "11,416", ""});
        tableModel.addRow(new Object[]{"Attapulgite;Aluminium hydroxyde and magnesium carbonate (Gastr...", "Gói", "Hộp 30 gó...", "1,004", "3,357", ""});
        tableModel.addRow(new Object[]{"Attapulgit; Nhôm hydroxyd; Magnesi carbonat (Kremil-S)...", "Lọ", "Chai 30ml...", "186", "133,357", ""});

        medicineTable = new JTable(tableModel);
        medicineTable.setFont(AppFonts.SMALL);
        medicineTable.setRowHeight(21);
        medicineTable.setGridColor(new Color(230, 235, 243));
        medicineTable.setSelectionBackground(new Color(214, 232, 255));
        medicineTable.setSelectionForeground(new Color(15, 20, 25));

        JTableHeader header = medicineTable.getTableHeader();
        header.setFont(AppFonts.SMALL_BOLD);
        header.setBackground(new Color(242, 246, 252));
        header.setForeground(new Color(30, 35, 45));
        header.setPreferredSize(new Dimension(100, 24));

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);

        medicineTable.getColumnModel().getColumn(0).setPreferredWidth(280);
        medicineTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        medicineTable.getColumnModel().getColumn(1).setCellRenderer(centerRender);
        medicineTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        medicineTable.getColumnModel().getColumn(3).setPreferredWidth(65);
        medicineTable.getColumnModel().getColumn(3).setCellRenderer(rightRender);
        medicineTable.getColumnModel().getColumn(4).setPreferredWidth(85);
        medicineTable.getColumnModel().getColumn(4).setCellRenderer(rightRender);
        medicineTable.getColumnModel().getColumn(5).setPreferredWidth(75);
        medicineTable.getColumnModel().getColumn(5).setCellRenderer(centerRender);

        JScrollPane tableScroll = new JScrollPane(medicineTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);

        rightContent.add(tableScroll, BorderLayout.CENTER);

        // --- THANH HIỂN THỊ THUỐC ĐÃ KÊ ĐÁY BẢNG: "★ Attapulgite;Aluminum hydroxyde... (10)" ---
        orderStatusBar = new JPanel(new BorderLayout());
        orderStatusBar.setBackground(new Color(0, 120, 215)); // Xanh dương đậm #0078D7
        orderStatusBar.setPreferredSize(new Dimension(800, 22));
        orderStatusBar.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));

        lblOrderStar = new JLabel(" Attapulgite;Aluminum hydroxyde and magnesium carbonate (Gastropulgite) 2,5g + 0,5g (10)", new StarVectorIcon(), SwingConstants.LEFT);
        lblOrderStar.setFont(AppFonts.SMALL_BOLD);
        lblOrderStar.setForeground(Color.WHITE);
        orderStatusBar.add(lblOrderStar, BorderLayout.CENTER);
        orderStatusBar.setVisible(false); // Ẩn ban đầu, hiện khi bấm OK

        rightContent.add(orderStatusBar, BorderLayout.SOUTH);

        splitPane.setRightComponent(rightContent);
        return splitPane;
    }

    /**
     * Cài đặt bộ tìm kiếm thông minh tự động gợi ý popup (Auto-complete) khi gõ tên thuốc.
     */
    private void setupMedicineAutocomplete(JButton btnBrowseDrug) {
        suggestPopup = new JPopupMenu();
        suggestPopup.setBorder(BorderFactory.createLineBorder(new Color(135, 148, 165), 1));
        suggestPopup.setFocusable(false);

        suggestListModel = new DefaultListModel<>();
        suggestList = new JList<>(suggestListModel);
        suggestList.setFont(AppFonts.SMALL);
        suggestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Hiển thị dòng chọn nền xanh #0078D7 chữ trắng, dòng thường nền trắng chữ đen chuẩn Windows EHC
        suggestList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(new EmptyBorder(2, 6, 2, 6));
                l.setFont(AppFonts.SMALL);
                if (isSelected) {
                    l.setBackground(new Color(0, 120, 215)); // Xanh chuẩn #0078D7
                    l.setForeground(Color.WHITE);
                } else {
                    l.setBackground(Color.WHITE);
                    l.setForeground(new Color(20, 25, 30));
                }
                return l;
            }
        });

        JScrollPane scroll = new JScrollPane(suggestList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(475, 340));
        suggestPopup.add(scroll);

        // Lắng nghe khi gõ vào ô Tên thuốc
        txtDrugName.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onQueryChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onQueryChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onQueryChanged(); }
        });

        // Điều khiển phím mũi tên Lên/Xuống, Enter, Escape trên ô Tên thuốc
        txtDrugName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!suggestPopup.isShowing()) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        onQueryChanged();
                    }
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    int next = suggestList.getSelectedIndex() + 1;
                    if (next < suggestListModel.size()) {
                        suggestList.setSelectedIndex(next);
                        suggestList.ensureIndexIsVisible(next);
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int prev = suggestList.getSelectedIndex() - 1;
                    if (prev >= 0) {
                        suggestList.setSelectedIndex(prev);
                        suggestList.ensureIndexIsVisible(prev);
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    MedicineSearchItem item = suggestList.getSelectedValue();
                    if (item != null) {
                        selectMedicineItem(item);
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    suggestPopup.setVisible(false);
                    e.consume();
                }
            }
        });

        // Click chọn trực tiếp trên danh sách gợi ý
        suggestList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                MedicineSearchItem item = suggestList.getSelectedValue();
                if (item != null) {
                    selectMedicineItem(item);
                }
            }
        });

        // Nút browse [...] mở toàn bộ danh mục thuốc
        if (btnBrowseDrug != null) {
            btnBrowseDrug.addActionListener(e -> {
                suggestListModel.clear();
                for (MedicineSearchItem item : allMedicines) {
                    suggestListModel.addElement(item);
                }
                suggestList.setSelectedIndex(0);
                if (txtDrugName.isShowing()) {
                    suggestPopup.show(txtDrugName, 0, txtDrugName.getHeight());
                }
            });
        }
    }

    private void onQueryChanged() {
        if (isSettingTextProgrammatically) return;

        SwingUtilities.invokeLater(() -> {
            String q = txtDrugName.getText().trim();
            if (q.isEmpty()) {
                suggestPopup.setVisible(false);
                return;
            }

            List<MedicineSearchItem> matches = filterMedicines(q);
            if (matches.isEmpty()) {
                suggestPopup.setVisible(false);
                return;
            }

            suggestListModel.clear();
            for (MedicineSearchItem item : matches) {
                suggestListModel.addElement(item);
            }
            suggestList.setSelectedIndex(0);
            suggestList.ensureIndexIsVisible(0);

            if (txtDrugName.isShowing()) {
                if (!suggestPopup.isShowing()) {
                    suggestPopup.show(txtDrugName, 0, txtDrugName.getHeight());
                } else {
                    suggestPopup.pack();
                }
                txtDrugName.requestFocusInWindow();
            }
        });
    }

    private List<MedicineSearchItem> filterMedicines(String query) {
        String cleanQuery = removeAccents(query.trim().toLowerCase());
        List<MedicineSearchItem> result = new ArrayList<>();
        for (MedicineSearchItem item : allMedicines) {
            String raw = item.code + " " + item.name;
            String normalized = removeAccents(raw.toLowerCase());
            if (raw.toLowerCase().contains(query.toLowerCase()) || normalized.contains(cleanQuery)) {
                result.add(item);
            }
        }
        return result;
    }

    private static String removeAccents(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replace('đ', 'd').replace('Đ', 'D');
    }

    private void selectMedicineItem(MedicineSearchItem item) {
        isSettingTextProgrammatically = true;
        txtDrugName.setText(item.name);
        txtDrugName.setBackground(Color.WHITE);
        isSettingTextProgrammatically = false;
        suggestPopup.setVisible(false);

        txtUnit.setText(item.unit);
        txtPack.setText(item.pack);
        txtRoute.setText(item.route);
        txtHdsd.setText(item.hdsd);
        txtKho.setText(item.stock != null ? item.stock : "0");
        txtQty.setText("10"); // Điền số lượng 10 chuẩn ảnh chụp thực tế

        // Ô HDSD sáng nền màu vàng nhạt chuẩn ảnh
        txtHdsd.setBackground(new Color(255, 255, 224));
        txtHdsd.requestFocusInWindow();
        txtHdsd.selectAll();

        // NÚT OK SÁNG BỪNG LÊN MÀU VÀNG HỔ PHÁCH #FFF1A8
        setOkButtonHighlighted(true);
    }

    public void setOkButtonHighlighted(boolean highlight) {
        if (btnOk == null) return;
        if (highlight) {
            btnOk.setEnabled(true);
            btnOk.setBackground(new Color(255, 241, 168)); // Màu vàng sáng chuẩn ảnh mẫu #FFF1A8
            btnOk.setForeground(new Color(20, 20, 20));
            btnOk.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(212, 163, 0), 1),
                    new EmptyBorder(1, 4, 1, 4)
            ));
            btnOk.setIcon(new CheckmarkBlueIcon(new Color(0, 140, 60))); // Checkmark xanh lá chuẩn ảnh mẫu
        } else {
            btnOk.setEnabled(false);
            btnOk.setBackground(new Color(238, 241, 246)); // Màu xám nhạt vô hiệu hóa chuẩn ảnh sau khi bấm OK
            btnOk.setForeground(new Color(150, 155, 165));
            btnOk.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(205, 212, 222), 1),
                    new EmptyBorder(1, 4, 1, 4)
            ));
            btnOk.setIcon(new CheckmarkBlueIcon(new Color(160, 165, 175))); // Checkmark xám
        }
        btnOk.repaint();
    }

    private void onConfirmDrugOrder() {
        String name = txtDrugName.getText().trim();
        if (name.isEmpty() && (orderStatusBar == null || !orderStatusBar.isVisible())) {
            name = "Attapulgite;Aluminum hydroxyde and magnesium carbonate (Gastropulgite) 2,5g + 0,5g";
        }
        if (name.isEmpty()) return;

        String qty = txtQty.getText().trim();
        if (qty.isEmpty() || "0".equals(qty)) {
            qty = "10";
        }

        // Hiện thanh trạng thái đáy bảng thuốc đã kê
        if (orderStatusBar != null && lblOrderStar != null) {
            orderStatusBar.setVisible(true);
            lblOrderStar.setIcon(new StarVectorIcon());
            lblOrderStar.setText(" " + name + " (" + qty + ")");
        }

        // Cập nhật các chỉ số tài chính chân trang chính xác theo ảnh mẫu:
        // Tiền đơn: 33,570 | Tổng chi phí: 21,645,270 | Bệnh nhân: 21,645,270 | Còn dư: 996,879,430
        if (lblValTienDon != null) lblValTienDon.setText("33,570");
        if (lblValTongChiPhi != null) lblValTongChiPhi.setText("21,645,270");
        if (lblValBenhNhan != null) lblValBenhNhan.setText("21,645,270");
        if (lblValConDu != null) lblValConDu.setText("996,879,430");

        // Reset toàn bộ các ô nhập liệu về 0 và rỗng
        isSettingTextProgrammatically = true;
        txtDrugName.setText("");
        txtDrugName.setBackground(Color.WHITE);
        isSettingTextProgrammatically = false;
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

        // Nút OK chuyển về trạng thái tắt sáng / xám nhạt
        setOkButtonHighlighted(false);
    }

    /**
     * Phương thức hỗ trợ hiển thị ngay trạng thái tìm kiếm (dùng cho preview và test)
     */
    public void showSearchState(String query) {
        txtDrugName.setText(query);
        onQueryChanged();
    }

    /**
     * Phương thức hỗ trợ hiển thị trạng thái khi đã chọn thuốc xong (nút OK sáng bừng)
     */
    public void showSelectedMedicineDemoState(String code) {
        for (MedicineSearchItem item : allMedicines) {
            if (item.code.equals(code)) {
                selectMedicineItem(item);
                break;
            }
        }
    }

    /**
     * Phương thức hỗ trợ hiển thị màn hình sau khi bấm OK (thanh thuốc đáy bảng & tài chính cập nhật)
     */
    public void showPrescribedAfterOkDemoState() {
        onConfirmDrugOrder();
    }

    public JPopupMenu getSuggestPopup() {
        return suggestPopup;
    }

    private JPanel buildFooterSection() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(245, 248, 252));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(205, 214, 226)),
                new EmptyBorder(4, 10, 4, 10)
        ));
        footer.setPreferredSize(new Dimension(1000, 48));

        // Khung tài chính bên trái (2 hàng)
        JPanel statsPanel = new JPanel(new GridLayout(2, 5, 10, 2));
        statsPanel.setOpaque(false);

        // Khởi tạo các nhãn tài chính
        lblValTienDon = new JLabel("0");
        lblValTienDon.setFont(AppFonts.BODY_BOLD);
        lblValTienDon.setForeground(new Color(185, 25, 25));

        lblValTongChiPhi = new JLabel("21,611,700");
        lblValTongChiPhi.setFont(AppFonts.BODY_BOLD);
        lblValTongChiPhi.setForeground(new Color(185, 25, 25));

        lblValBhyt = new JLabel("0");
        lblValBhyt.setFont(AppFonts.BODY_BOLD);
        lblValBhyt.setForeground(new Color(185, 25, 25));

        lblValNguonKhac = new JLabel("0");
        lblValNguonKhac.setFont(AppFonts.BODY_BOLD);
        lblValNguonKhac.setForeground(new Color(185, 25, 25));

        lblValBenhNhan = new JLabel("21,611,700");
        lblValBenhNhan.setFont(AppFonts.BODY_BOLD);
        lblValBenhNhan.setForeground(new Color(185, 25, 25));

        lblValTamUng = new JLabel("1,010,000,0...");
        lblValTamUng.setFont(AppFonts.BODY_BOLD);
        lblValTamUng.setForeground(new Color(0, 45, 140));

        lblValDaThu = new JLabel("8,524,700");
        lblValDaThu.setFont(AppFonts.BODY_BOLD);
        lblValDaThu.setForeground(new Color(0, 45, 140));

        lblValMienGiam = new JLabel("0");
        lblValMienGiam.setFont(AppFonts.BODY_BOLD);
        lblValMienGiam.setForeground(new Color(0, 45, 140));

        lblValConDu = new JLabel("996,913,000");
        lblValConDu.setFont(AppFonts.BODY_BOLD);
        lblValConDu.setForeground(new Color(0, 45, 140));

        // Hàng 1 (Đỏ đậm): Tiền đơn | Tổng chi phí | BHYT | Nguồn khác | Bệnh nhân
        statsPanel.add(createMetricBox("Tiền đơn", lblValTienDon));
        statsPanel.add(createMetricBox("Tổng chi phí:", lblValTongChiPhi));
        statsPanel.add(createMetricBox("BHYT:", lblValBhyt));
        statsPanel.add(createMetricBox("Nguồn khác:", lblValNguonKhac));
        statsPanel.add(createMetricBox("Bệnh nhân:", lblValBenhNhan));

        // Hàng 2 (Xanh tím than): [trống] | Tạm ứng | Đã thu | Miễn giảm | Còn dư
        statsPanel.add(new JLabel(""));
        statsPanel.add(createMetricBox("Tạm ứng:", lblValTamUng));
        statsPanel.add(createMetricBox("Đã thu:", lblValDaThu));
        statsPanel.add(createMetricBox("Miễn giảm:", lblValMienGiam));
        statsPanel.add(createMetricBox("Còn dư:", lblValConDu));

        footer.add(statsPanel, BorderLayout.CENTER);

        // Cụm nút In & Lưu bên phải
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        rightButtons.setOpaque(false);

        JButton btnPrint = new JButton(" In (Ctrl + P) ▾", new SuppliesToolbarIcon("print"));
        btnPrint.setFont(AppFonts.BODY_BOLD);
        btnPrint.setBackground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.setPreferredSize(new Dimension(140, 28));
        btnPrint.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 208), 1),
                new EmptyBorder(2, 4, 2, 4)
        ));

        JButton btnSave = new JButton(" Lưu (Ctrl + S)", new FloppyDiskIcon(new Color(125, 45, 135)));
        btnSave.setFont(AppFonts.BODY_BOLD);
        btnSave.setBackground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.setPreferredSize(new Dimension(135, 28));
        btnSave.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 208), 1),
                new EmptyBorder(2, 4, 2, 4)
        ));
        btnSave.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Đã lưu đơn thuốc thành công!\n" +
                    "▶ Bệnh nhân: TEST11 | 22000285\n" +
                    "▶ Bác sĩ: Sennv",
                    "Lưu Đơn Thuốc", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnSaveAndClose = new JButton(" Lưu + Đóng", new FloppyDiskIcon(new Color(40, 145, 75)));
        btnSaveAndClose.setFont(AppFonts.BODY_BOLD);
        btnSaveAndClose.setBackground(Color.WHITE);
        btnSaveAndClose.setFocusPainted(false);
        btnSaveAndClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSaveAndClose.setPreferredSize(new Dimension(125, 28));
        btnSaveAndClose.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 208), 1),
                new EmptyBorder(2, 4, 2, 4)
        ));
        btnSaveAndClose.addActionListener(e -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });

        rightButtons.add(btnPrint);
        rightButtons.add(btnSave);
        rightButtons.add(btnSaveAndClose);

        footer.add(rightButtons, BorderLayout.EAST);
        return footer;
    }

    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppFonts.SMALL);
        l.setForeground(new Color(30, 35, 45));
        return l;
    }

    private JComboBox<String> createDropdown(String defaultVal, int width) {
        JComboBox<String> cb = new JComboBox<>(new String[]{defaultVal});
        cb.setFont(AppFonts.SMALL);
        cb.setBackground(Color.WHITE);
        cb.setPreferredSize(new Dimension(width, 23));
        return cb;
    }

    private JButton createMiniBrowseButton() {
        JButton btn = new JButton("...");
        btn.setFont(AppFonts.SMALL);
        btn.setPreferredSize(new Dimension(22, 23));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setFocusPainted(false);
        btn.setBackground(new Color(238, 242, 248));
        return btn;
    }

    private JPanel createMetricBox(String title, JLabel valLabel) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);

        JLabel lTitle = new JLabel(title);
        lTitle.setFont(AppFonts.SMALL);
        lTitle.setForeground(new Color(35, 40, 50));

        p.add(lTitle);
        p.add(valLabel);
        return p;
    }

    private JButton createSubToolbarButton(String text, String iconType, boolean hasDropdownArrow) {
        String labelText = hasDropdownArrow ? text + " ▾" : text;
        JButton btn = new JButton(labelText, new SuppliesToolbarIcon(iconType));
        btn.setFont(AppFonts.SMALL);
        btn.setForeground(new Color(20, 20, 20));
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setIconTextGap(2);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(2, 6, 2, 6));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(228, 236, 248));
                btn.setOpaque(true);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setOpaque(false);
            }
        });

        return btn;
    }

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
            } else if ("history".equalsIgnoreCase(type)) {
                g2.setColor(new Color(0, 115, 205));
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawOval(x + 3, y + 3, 12, 12);
                g2.drawLine(x + 9, y + 5, x + 9, y + 9);
                g2.drawLine(x + 9, y + 9, x + 12, y + 9);
            } else if ("print".equalsIgnoreCase(type)) {
                g2.setColor(new Color(110, 60, 145));
                g2.fillRect(x + 4, y + 2, 10, 4);
                g2.fillRoundRect(x + 2, y + 5, 14, 7, 2, 2);
                g2.setColor(Color.WHITE);
                g2.fillRect(x + 4, y + 9, 10, 5);
            }
            g2.dispose();
        }
    }

    private static class CheckmarkBlueIcon implements Icon {
        private final Color color;
        CheckmarkBlueIcon() { this(new Color(0, 115, 220)); }
        CheckmarkBlueIcon(Color color) { this.color = color; }
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color != null ? color : new Color(0, 115, 220));
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

    private static class EnvelopeIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 12; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 95, 205));
            g2.drawRect(x + 1, y + 1, 12, 9);
            g2.drawLine(x + 1, y + 2, x + 7, y + 6);
            g2.drawLine(x + 7, y + 6, x + 13, y + 2);
            g2.dispose();
        }
    }

    private static class StarVectorIcon implements Icon {
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 12; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            int midX = x + 6;
            int midY = y + 6;
            int radius = 5;
            int innerRadius = 2;
            int[] xPoints = new int[10];
            int[] yPoints = new int[10];
            for (int i = 0; i < 10; i++) {
                double angle = i * Math.PI / 5 - Math.PI / 2;
                int r = (i % 2 == 0) ? radius : innerRadius;
                xPoints[i] = (int) (midX + r * Math.cos(angle));
                yPoints[i] = (int) (midY + r * Math.sin(angle));
            }
            g2.fillPolygon(xPoints, yPoints, 10);
            g2.dispose();
        }
    }

    private static class PauseBarsIcon implements Icon {
        @Override public int getIconWidth() { return 10; }
        @Override public int getIconHeight() { return 12; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(30, 30, 30));
            g2.fillRect(x + 1, y + 2, 2, 8);
            g2.fillRect(x + 5, y + 2, 2, 8);
            g2.dispose();
        }
    }
}
