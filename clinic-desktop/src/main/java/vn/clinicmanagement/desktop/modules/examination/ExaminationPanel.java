package vn.clinicmanagement.desktop.modules.examination;

import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.modules.examination.components.menus.ExaminationMenuPopupMenu;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.LabOrderTabPanel;
import vn.clinicmanagement.desktop.modules.examination.components.print.MedicalBillingSlipDialog;
import vn.clinicmanagement.desktop.modules.examination.components.print.MedicalRecordPrintPreviewDialog;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.MedicalRecordTypeDialog;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.MedicalSuppliesOrderDialog;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.MedicalSuppliesOrderPanel;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ReopenMedicalRecordDialog;
import vn.clinicmanagement.desktop.modules.examination.components.menus.MedicineSuppliesPopupMenu;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.PrescriptionOrderPanel;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.PrescriptionOrderTabPanel;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.MedicalRecordTabPanel;
import vn.clinicmanagement.desktop.modules.examination.components.menus.PatientQueueContextMenu;
import vn.clinicmanagement.desktop.modules.examination.components.menus.PrintReportPopupMenu;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ServiceOrderDialog;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ServiceOrderPanel;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.SuppliesOrderTabPanel;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.TransferRoomConfirmDialog;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.TransferRoomSelectDialog;
import vn.clinicmanagement.desktop.modules.examination.components.print.TransferSlipPrintPreviewDialog;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.TreatmentResolutionTabPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Phân hệ Khám Bệnh - Giao diện Quản trị viên (Admin Examination Workspace)
 * Thiết kế tỷ lệ 1:1 chuẩn EHC Hospital tối ưu cho màn hình 13-inch MacBook M1:
 * - Thanh công cụ vector sắc nét: Menu, In ▾, Làm mới, Tìm Kiếm, Dịch Vụ ▾, Thuốc/VT ▾, Thanh Toán, Màn Hình Chờ
 * - Banner thông tin bệnh nhân góc phải: NGUYỄN MẠNH HÙNG | 21000325 | 1984 (39 tuổi) | Nam | Yêu cầu
 * - Cột trái: ◀ Tùy Chọn | Tất cả bệnh nhân (40) | 21-08-23 >> 21-08-23 - BH(0) VP(40) | 11 dòng bệnh nhân | (Tất cả phòng khám) ▾
 * - Khung phải: 8 Tab lâm sàng + Lưới hồ sơ 9 hàng + Bảng Lịch Sử Khám và Điều Trị (với lưới trắng toàn khung)
 */
public class ExaminationPanel extends BaseViewPanel {

    public static class AdminPatientItem {
        public final String stt;
        public final String name;
        public final String code;
        public final String requestType;
        public final String queueAge;
        public final boolean hasOrangeDot;
        public final String gender;
        public final String birthYear;
        public final String bannerAge;
        public final String formAge;
        public final String birthDate;
        public final String address;
        public final String bannerAddress;
        public final String phone;
        public final String medicalRecordCode;
        public final String admissionDate;
        public final String department;
        public final String doctor;
        public final String diagnosis;
        public final String invoiceCode;

        public AdminPatientItem(String stt, String name, String code, String requestType, String queueAge,
                                boolean hasOrangeDot, String gender, String birthYear, String bannerAge, String formAge,
                                String birthDate, String address, String bannerAddress, String phone,
                                String medicalRecordCode, String admissionDate, String department, String doctor,
                                String diagnosis, String invoiceCode) {
            this.stt = stt;
            this.name = name;
            this.code = code;
            this.requestType = requestType;
            this.queueAge = queueAge;
            this.hasOrangeDot = hasOrangeDot;
            this.gender = gender;
            this.birthYear = birthYear;
            this.bannerAge = bannerAge;
            this.formAge = formAge;
            this.birthDate = birthDate;
            this.address = address;
            this.bannerAddress = bannerAddress;
            this.phone = phone;
            this.medicalRecordCode = medicalRecordCode;
            this.admissionDate = admissionDate;
            this.department = department;
            this.doctor = doctor;
            this.diagnosis = diagnosis;
            this.invoiceCode = invoiceCode;
        }
    }

    private final List<AdminPatientItem> patientList = new ArrayList<>();
    private JTable queueTable;
    private DefaultTableModel queueModel;

    // Quản lý chuyển hướng màn hình (Navigation) trong phân hệ Khám Bệnh
    private CardLayout cardLayout;
    private JPanel cardContainer;
    private MedicalSuppliesOrderPanel suppliesOrderPanel;
    private PrescriptionOrderPanel prescriptionOrderPanel;
    private ServiceOrderPanel serviceOrderPanel;
    private JTabbedPane clinicalTabs;
    private SuppliesOrderTabPanel suppliesTabPanel;
    private PrescriptionOrderTabPanel prescriptionTabPanel;
    private LabOrderTabPanel labOrderTabPanel;
    private MedicalRecordTabPanel medicalRecordTabPanel;
    private TreatmentResolutionTabPanel treatmentResolutionTabPanel;

    // Header Right Patient Banner
    private JLabel lblBannerLine1;
    private JLabel lblBannerReqTag;
    private JLabel lblBannerLine2;
    private JLabel lblBannerLine3;

    // Form value labels
    private JLabel lblMaBN;
    private JLabel lblMaBA;
    private JLabel lblTenBN;
    private JLabel lblGioiTinh;
    private JLabel lblNgaySinh;
    private JLabel lblTuoi;
    private JLabel lblDanToc;
    private JLabel lblNgheNghiep;
    private JLabel lblQuocTich;
    private JLabel lblDienThoai;
    private JLabel lblDiaChi;
    private JLabel lblCmnd;
    private JLabel lblDonTiepLuc;
    private JLabel lblLoaiDonTiep;
    private JLabel lblDoiTuong;
    private JLabel lblSoVaoKhoa;
    private JLabel lblMaDieuTri;
    private JLabel lblLyDoKham;
    private JLabel lblKhoaPhong;
    private JLabel lblBacSiKham;
    private JLabel lblBenhAn;
    private JLabel lblNgayVao;
    private JLabel lblChanDoan;

    // History Table
    private DefaultTableModel historyModel;
    private JTable historyTable;

    public ExaminationPanel() {
        super("examination", "Khám Bệnh", "🩺");
        initData();
        buildUI();
        // Mặc định chọn bệnh nhân NGUYỄN THỊ BÌNH AN (index 0) khớp 100% ảnh thiết kế mới
        selectPatientByIndex(0);
    }

    private void initData() {
        patientList.clear();

        // 0. TEST1 (khớp ảnh chụp thực tế màn hình khám bệnh khi nhấn Menu)
        patientList.add(new AdminPatientItem("1", "TEST1", "22000324", "bhmi", "22", true,
                "Nam", "2000", "2000 (22 tuổi)", "22", "2000", "Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh",
                "Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh", "0912345678", "22/000063",
                "11:26 22/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Khám bệnh", "214348"));

        // 1. TEST11 (khớp ảnh màn hình khám bệnh khi kê đơn thuốc)
        patientList.add(new AdminPatientItem("1", "TEST11", "22000285", "Viện phí", "22", true,
                "Nam", "2000", "2000 (22 tuổi)", "22", "2000", "Phường Đáp Cầu, TP. Bắc Ninh, Bắc Ninh",
                "Phường Đáp Cầu, TP. Bắc Ninh, Bắc Ninh", "0912345678", "22/000028",
                "10:23 09/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "H20-Viêm màng bồ đào", "214302"));

        // Danh sách 13 bệnh nhân hàng đợi khớp 100% ảnh chụp thực tế màn hình khám bệnh
        // 0. NGUYỄN ĐÌNH PHÓNG (Khớp 100% media_1793381410253_58084cf2.jpg)
        patientList.add(new AdminPatientItem("1", "NGUYỄN ĐÌNH PHÓNG", "22000306", "Viện phí", "27", true,
                "Nam", "1989", "1989 (33 tuổi)", "33", "1989", "Phường Cổ Nhuế 2, Q, Bắc Từ Liêm, TP Hà Nội",
                "Phường Cổ Nhuế 2, Q, Bắc Từ Liêm, TP Hà Nội", "0987654321", "22/000046",
                "15:36 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Chờ khám", "214330"));

        // 1. NGUYỄN THỊ BÌNH AN (đang chọn)
        patientList.add(new AdminPatientItem("1", "NGUYỄN THỊ BÌNH AN", "22000308", "bhmi", "23", true,
                "Nữ", "1995", "1995 (23 tuổi)", "23", "1995", "Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh",
                "Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh", "0945123456", "22/000047",
                "15:37 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "A02.0-Viêm ruột do Salmonella", "214332"));

        // 2. LỤC VĂN TIÊN
        patientList.add(new AdminPatientItem("2", "LỤC VĂN TIÊN", "22000308", "Viện phí", "23", true,
                "Nam", "1995", "1995 (23 tuổi)", "23", "1995", "Bắc Ninh",
                "Bắc Ninh", "0912345678", "22/000048",
                "15:38 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Chờ khám", "214333"));

        // 3. NGUYỄN ĐÌNH PHÙNG
        patientList.add(new AdminPatientItem("3", "NGUYỄN ĐÌNH PHÙNG", "22000304", "Viện phí", "33", true,
                "Nam", "1990", "1990 (33 tuổi)", "33", "1990", "Bắc Ninh",
                "Bắc Ninh", "0922334455", "22/000049",
                "15:38 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Chờ khám", "214334"));

        // 4. NGUYỄN HOÀNG LONG
        patientList.add(new AdminPatientItem("3", "NGUYỄN HOÀNG LONG", "22000304", "Viện phí", "33", true,
                "Nam", "1990", "1990 (33 tuổi)", "33", "1990", "Bắc Ninh",
                "Bắc Ninh", "0933445566", "22/000050",
                "15:39 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Chờ khám", "214335"));

        // 5. ĐỖ MỸ LINH (Chấm cam cảnh báo, khớp 100% media_1793373144739_058baf99.png)
        patientList.add(new AdminPatientItem("4", "ĐỖ MỸ LINH", "22000305", "Viện phí", "24", true,
                "Nam", "1998", "1998 (24 tuổi)", "24", "1998", "Phường Dịch Vọng Hậu, Q. Cầu Giấy, TP Hà Nội",
                "Phường Dịch Vọng Hậu, Q. Cầu Giấy, TP Hà Nội", "0944556677", "22/000051",
                "15:30 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Đã khám", "214329"));

        // 6. BN 34
        patientList.add(new AdminPatientItem("5", "BN 34", "22000305", "HG4", "25", false,
                "Nữ", "1998", "1998 (25 tuổi)", "25", "1998", "Bắc Ninh",
                "Bắc Ninh", "0955667788", "22/000052",
                "15:32 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Đã khám", "214337"));

        // 7. TEST DG
        patientList.add(new AdminPatientItem("6", "TEST DG", "22000303", "GD4", "26", false,
                "Nam", "1997", "1997 (26 tuổi)", "26", "1997", "Bắc Ninh",
                "Bắc Ninh", "0966778899", "22/000053",
                "15:33 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Đã khám", "214338"));

        // 8. TEST DG (Khớp media_1793376950126_1a3e6673.png)
        patientList.add(new AdminPatientItem("2", "TEST DG", "22000301", "GD4", "24", false,
                "Nam", "1998", "1998 (24 tuổi)", "24", "1998", "Xã Nam Thái, H. Nam Trực, Nam Định",
                "Xã Nam Thái, H. Nam Trực, Nam Định", "0977889900", "GD-4-45 2211002362 | Còn 131 ngày",
                "09:34 23/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Đã khám", "214326"));

        // 9. BN 03
        patientList.add(new AdminPatientItem("8", "BN 03", "22000303", "GD4", "28", false,
                "Nam", "1995", "1995 (28 tuổi)", "28", "1995", "Bắc Ninh",
                "Bắc Ninh", "0988990011", "22/000055",
                "15:35 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Đã khám", "214340"));

        // 10. BN 03
        patientList.add(new AdminPatientItem("1", "BN 03", "22000301", "GD4", "19", false,
                "Nam", "2004", "2004 (19 tuổi)", "19", "2004", "Bắc Ninh",
                "Bắc Ninh", "0999001122", "22/000056",
                "15:36 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Đã khám", "214341"));

        // 11. 111
        patientList.add(new AdminPatientItem("1", "111", "22000305", "TE1", "7 TH", false,
                "Nam", "2022", "7 TH", "7 TH", "2022", "Bắc Ninh",
                "Bắc Ninh", "0911223344", "22/000057",
                "15:36 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Đã khám", "214342"));

        // 12. TEST DG
        patientList.add(new AdminPatientItem("1", "TEST DG", "22000303", "GD4", "24", false,
                "Nam", "1999", "1999 (24 tuổi)", "24", "1999", "Bắc Ninh",
                "Bắc Ninh", "0922334455", "22/000058",
                "15:37 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Đã khám", "214343"));

        // 13. GIÁM
        patientList.add(new AdminPatientItem("1", "GIÁM", "22000302", "GD4", "21", false,
                "Nam", "2002", "2002 (21 tuổi)", "21", "2002", "Bắc Ninh",
                "Bắc Ninh", "0933445566", "22/000059",
                "15:37 11/08/2022", "Khoa khám bệnh > PK Nhi 1", "Sennv", "Đã khám", "214344"));

        // 14. NGUYỄN THỊ LIÊN (Khớp 100% media_1793379640679_89d5bbca.jpg)
        patientList.add(new AdminPatientItem("6", "NGUYỄN THỊ LIÊN", "22001770", "DMM", "1984", false,
                "Nữ", "1984", "1984 (38 tuổi)", "38", "1984", "Xã Mường Lai, Huyện Lục Yên, Tỉnh Yên Bái | 012345678",
                "Xã Mường Lai, Huyện Lục Yên, Tỉnh Yên Bái | 012345678", "012345678", "CN-4-27-1520162743 | Còn 142 ngày",
                "14:38 12/08/2022", "Khoa khám bệnh", "Sennv", "Đang khám", "1850"));
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);

        // --- MÀN HÌNH 1: BÀN KHÁM BỆNH CHÍNH (EXAM_MAIN) ---
        JPanel mainExamView = new JPanel(new BorderLayout());
        mainExamView.setBackground(Color.WHITE);

        // 1. THANH CÔNG CỤ PHỤ (SUB-TOOLBAR) + BANNER THÔNG TIN BỆNH NHÂN GÓC PHẢI
        JPanel topToolbar = new JPanel(new BorderLayout());
        topToolbar.setBackground(new Color(246, 248, 252));
        topToolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));
        topToolbar.setPreferredSize(new Dimension(100, 56));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        leftActions.setOpaque(false);

        leftActions.add(createToolbarButton("Menu", "menu", false));
        leftActions.add(createToolbarButton("In", "print", true));
        leftActions.add(createToolbarButton("Làm mới", "refresh", false));
        leftActions.add(createToolbarButton("Tìm Kiếm", "search", false));
        leftActions.add(createToolbarButton("Dịch Vụ", "service", true));
        leftActions.add(createToolbarButton("Thuốc/VT", "medicine", true));
        leftActions.add(createToolbarButton("Thanh Toán", "payment", false));
        leftActions.add(createToolbarButton("Màn Hình Chờ", "waiting", false));

        topToolbar.add(leftActions, BorderLayout.WEST);

        // Banner thông tin bệnh nhân góc phải (Chữ màu đỏ sẫm chuẩn ảnh mẫu)
        JPanel headerPatientBanner = new JPanel(new BorderLayout(0, 0));
        headerPatientBanner.setOpaque(false);
        headerPatientBanner.setBorder(new EmptyBorder(2, 8, 2, 10));

        JPanel bannerTextPanel = new JPanel(new GridLayout(3, 1, 0, 0));
        bannerTextPanel.setOpaque(false);
        bannerTextPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(205, 214, 226)),
                new EmptyBorder(0, 0, 0, 10)
        ));

        lblBannerLine1 = new JLabel("NGUYỄN MẠNH HÙNG | 21000325 | 1984 (39 tuổi) | Nam", SwingConstants.RIGHT);
        lblBannerLine1.setFont(AppFonts.SECTION);
        lblBannerLine1.setForeground(new Color(125, 15, 15));

        lblBannerLine2 = new JLabel("VP: 20553 | Yêu cầu", SwingConstants.RIGHT);
        lblBannerLine2.setFont(AppFonts.BODY);
        lblBannerLine2.setForeground(new Color(125, 15, 15));

        lblBannerLine3 = new JLabel("GARDEIA | 0938888884", SwingConstants.RIGHT);
        lblBannerLine3.setFont(AppFonts.BODY);
        lblBannerLine3.setForeground(new Color(125, 15, 15));

        bannerTextPanel.add(lblBannerLine1);
        bannerTextPanel.add(lblBannerLine2);
        bannerTextPanel.add(lblBannerLine3);

        lblBannerReqTag = new JLabel("<html><center>Yêu<br>cầu</center></html>", SwingConstants.CENTER);
        lblBannerReqTag.setFont(AppFonts.BODY_BOLD);
        lblBannerReqTag.setForeground(new Color(125, 15, 15));
        lblBannerReqTag.setBorder(new EmptyBorder(0, 10, 0, 4));

        headerPatientBanner.add(bannerTextPanel, BorderLayout.CENTER);
        headerPatientBanner.add(lblBannerReqTag, BorderLayout.EAST);

        topToolbar.add(headerPatientBanner, BorderLayout.EAST);
        add(topToolbar, BorderLayout.NORTH);

        // 2. MAIN SPLIT: CỘT TRÁI (300px) + CỘT PHẢI (HỒ SƠ KHÁM CHI TIẾT)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.24);
        splitPane.setDividerSize(3);
        splitPane.setBorder(null);

        // --- CỘT TRÁI: DANH SÁCH BỆNH NHÂN HÀNG ĐỢI ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(195, 205, 218)));

        // Thanh tiêu đề con: [◀] Tùy Chọn | [Tất cả bệnh nhân (40)]
        JPanel leftSubHeader = new JPanel(new BorderLayout());
        leftSubHeader.setBackground(new Color(235, 241, 250));
        leftSubHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 210, 224)));
        leftSubHeader.setPreferredSize(new Dimension(300, 28));

        JPanel leftTuyChonBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        leftTuyChonBox.setOpaque(false);
        JLabel lblArrowLeft = new JLabel(new LeftTriangleIcon());
        JLabel lblTuyChon = new JLabel("Tùy Chọn");
        lblTuyChon.setFont(AppFonts.SMALL);
        lblTuyChon.setForeground(new Color(30, 30, 30));
        leftTuyChonBox.add(lblArrowLeft);
        leftTuyChonBox.add(lblTuyChon);

        JPanel tabPatients = new JPanel(new BorderLayout());
        tabPatients.setBackground(new Color(254, 240, 205));
        tabPatients.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 0, new Color(220, 185, 120)));
        JLabel lblTabPatients = new JLabel("Tất cả bệnh nhân (29)", SwingConstants.CENTER);
        lblTabPatients.setFont(AppFonts.BODY_BOLD);
        lblTabPatients.setForeground(new Color(20, 20, 20));
        tabPatients.add(lblTabPatients, BorderLayout.CENTER);

        leftSubHeader.add(leftTuyChonBox, BorderLayout.WEST);
        leftSubHeader.add(tabPatients, BorderLayout.CENTER);

        // Thanh bộ lọc ngày: 20/07/22 >> 19/08/22 - BH(18) VP(11) [🔍]
        JPanel filterHeader = new JPanel(new BorderLayout(4, 0));
        filterHeader.setBackground(new Color(255, 255, 208));
        filterHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(195, 205, 185)),
                new EmptyBorder(2, 6, 2, 6)
        ));
        filterHeader.setPreferredSize(new Dimension(300, 24));

        JLabel lblFilter = new JLabel("20/07/22 >> 19/08/22 - BH(18) VP(11)");
        lblFilter.setFont(AppFonts.SMALL);
        lblFilter.setForeground(new Color(20, 20, 20));
        JLabel lblSearchIcon = new JLabel(new MiniMagnifierIcon());

        filterHeader.add(lblFilter, BorderLayout.CENTER);
        filterHeader.add(lblSearchIcon, BorderLayout.EAST);

        JPanel leftNorth = new JPanel(new BorderLayout());
        leftNorth.add(leftSubHeader, BorderLayout.NORTH);
        leftNorth.add(filterHeader, BorderLayout.SOUTH);
        leftPanel.add(leftNorth, BorderLayout.NORTH);

        // Bảng danh sách hàng đợi (11 dòng khớp vừa khít chiều cao màn hình 13 inch)
        String[] qCols = {"STT", "Tên Bệnh Nhân", "Tuổi"};
        queueModel = new DefaultTableModel(qCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        for (AdminPatientItem item : patientList) {
            queueModel.addRow(new Object[]{item, item, item.queueAge});
        }

        queueTable = new JTable(queueModel);
        queueTable.setRowHeight(47);
        queueTable.setShowHorizontalLines(true);
        queueTable.setShowVerticalLines(false);
        queueTable.setGridColor(new Color(238, 241, 246));
        queueTable.setIntercellSpacing(new Dimension(0, 1));
        queueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queueTable.setBackground(Color.WHITE);

        JTableHeader qHeader = queueTable.getTableHeader();
        qHeader.setFont(AppFonts.SMALL_BOLD);
        qHeader.setPreferredSize(new Dimension(300, 23));
        qHeader.setBackground(new Color(242, 245, 250));
        qHeader.setForeground(new Color(25, 25, 25));
        qHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 212, 222)));

        // Cột 0: STT + Chấm cam tròn (cho các dòng từ NGUYỄN MẠNH HÙNG trở xuống)
        queueTable.getColumnModel().getColumn(0).setPreferredWidth(46);
        queueTable.getColumnModel().getColumn(0).setMaxWidth(48);
        queueTable.getColumnModel().getColumn(0).setCellRenderer(new SttCellRenderer());

        // Cột 1: Tên Bệnh Nhân (2 dòng vẽ bằng Graphics2D tuyệt đối sắc nét)
        queueTable.getColumnModel().getColumn(1).setPreferredWidth(210);
        queueTable.getColumnModel().getColumn(1).setCellRenderer(new PatientNameCellRenderer());

        // Cột 2: Tuổi
        queueTable.getColumnModel().getColumn(2).setPreferredWidth(44);
        queueTable.getColumnModel().getColumn(2).setMaxWidth(48);
        queueTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(AppFonts.SMALL);
                if (isSelected) {
                    lbl.setBackground(new Color(0, 114, 198));
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(new Color(25, 25, 25));
                }
                return lbl;
            }
        });

        queueTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = queueTable.getSelectedRow();
                if (row >= 0 && row < patientList.size()) {
                    selectPatient(patientList.get(row));
                }
            }
        });

        // Chuột phải vào danh sách bệnh nhân hiển thị Context Menu khớp 100% media_1793381410253_58084cf2.jpg
        queueTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            private void checkPopup(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = queueTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < patientList.size()) {
                        queueTable.setRowSelectionInterval(row, row);
                        selectPatient(patientList.get(row));
                    }
                    showQueueContextMenu(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        JScrollPane queueScroll = new JScrollPane(queueTable);
        queueScroll.setColumnHeaderView(queueTable.getTableHeader());
        queueScroll.getViewport().setBackground(Color.WHITE);
        queueScroll.setBorder(BorderFactory.createEmptyBorder());
        leftPanel.add(queueScroll, BorderLayout.CENTER);

        // Footer: Ô chọn (Tất cả phòng khám) có viền trắng và nút mũi tên ▾ bên phải
        JPanel leftFooter = new JPanel(new BorderLayout());
        leftFooter.setBackground(Color.WHITE);
        leftFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(185, 192, 202)));
        leftFooter.setPreferredSize(new Dimension(300, 24));

        JLabel lblAllClinics = new JLabel("(Tất cả phòng khám)");
        lblAllClinics.setFont(AppFonts.SMALL);
        lblAllClinics.setForeground(new Color(145, 35, 35));
        lblAllClinics.setBorder(new EmptyBorder(0, 6, 0, 4));

        JLabel btnDrop = new JLabel("▾", SwingConstants.CENTER);
        btnDrop.setFont(AppFonts.SMALL);
        btnDrop.setForeground(new Color(90, 90, 90));
        btnDrop.setOpaque(true);
        btnDrop.setBackground(new Color(238, 240, 244));
        btnDrop.setPreferredSize(new Dimension(18, 22));
        btnDrop.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(195, 200, 208)));

        leftFooter.add(lblAllClinics, BorderLayout.CENTER);
        leftFooter.add(btnDrop, BorderLayout.EAST);

        leftPanel.add(leftFooter, BorderLayout.SOUTH);
        splitPane.setLeftComponent(leftPanel);

        // --- CỘT PHẢI: KHUNG CHI TIẾT BỆNH NHÂN KHI ĐƯỢC CHỌN ---
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(224, 235, 248));

        clinicalTabs = createStyledTabbedPane();
        JPanel patientDetailTab = buildPatientFormPanel();
        clinicalTabs.addTab("Bệnh Nhân", new SmallFormIcon(), patientDetailTab);
        clinicalTabs.addTab("Khám Bệnh", createPlaceholderTab("Ghi nhận diễn biến bệnh, khám các cơ quan & tiền sử"));
        medicalRecordTabPanel = new MedicalRecordTabPanel();
        clinicalTabs.addTab("Bệnh Án", medicalRecordTabPanel);
        clinicalTabs.addTab("Công Khám(1)", createPlaceholderTab("Dịch vụ công khám chuyên khoa"));
        labOrderTabPanel = new LabOrderTabPanel(() -> cardLayout.show(cardContainer, "SERVICE_ORDER"));
        clinicalTabs.addTab("Xét Nghiệm(3)", labOrderTabPanel);
        clinicalTabs.addTab("CDHA(2)", createPlaceholderTab("Chẩn đoán hình ảnh: Siêu âm, X-Quang"));
        clinicalTabs.addTab("Chuyên khoa(1)", createPlaceholderTab("Khám chuyên khoa mắt, tai mũi họng"));
        prescriptionTabPanel = new PrescriptionOrderTabPanel();
        clinicalTabs.addTab("Thuốc(1)", prescriptionTabPanel);
        suppliesTabPanel = new SuppliesOrderTabPanel();
        clinicalTabs.addTab("Vật Tư(1)", suppliesTabPanel);
        treatmentResolutionTabPanel = new TreatmentResolutionTabPanel();
        clinicalTabs.addTab("Xử Trí", treatmentResolutionTabPanel);
        clinicalTabs.addTab("BA Điện Tử(2)", createPlaceholderTab("Hồ sơ Bệnh Án Điện Tử (EMR Version 2.0)"));

        rightPanel.add(clinicalTabs, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);

        mainExamView.add(topToolbar, BorderLayout.NORTH);
        mainExamView.add(splitPane, BorderLayout.CENTER);

        // --- MÀN HÌNH 2: CHỈ ĐỊNH VẬT TƯ (SUPPLIES_ORDER) ---
        // Chuyển hướng trực tiếp trong cùng khung làm việc, khi Lưu + Đóng tự quay về tab Vật Tư(1)
        suppliesOrderPanel = new MedicalSuppliesOrderPanel(() -> {
            cardLayout.show(cardContainer, "EXAM_MAIN");
            clinicalTabs.setSelectedComponent(suppliesTabPanel);
        });

        // --- MÀN HÌNH 3: CHỈ ĐỊNH THUỐC (PRESCRIPTION_ORDER) ---
        // Chuyển hướng trực tiếp trong cùng khung làm việc, khi Lưu + Đóng tự quay về tab Thuốc(1)
        prescriptionOrderPanel = new PrescriptionOrderPanel(() -> {
            cardLayout.show(cardContainer, "EXAM_MAIN");
            selectPatientByName("TEST11");
            clinicalTabs.setSelectedComponent(prescriptionTabPanel);
        });

        // --- MÀN HÌNH 4: CHỈ ĐỊNH DỊCH VỤ (SERVICE_ORDER) ---
        // Chuyển hướng trực tiếp trong cùng khung làm việc khi bấm "Sửa Phiếu" từ tab Xét Nghiệm
        serviceOrderPanel = new ServiceOrderPanel(() -> {
            cardLayout.show(cardContainer, "EXAM_MAIN");
            selectPatientByName("TEST11");
            clinicalTabs.setSelectedComponent(labOrderTabPanel);
        });

        cardContainer.add(mainExamView, "EXAM_MAIN");
        cardContainer.add(suppliesOrderPanel, "SUPPLIES_ORDER");
        cardContainer.add(prescriptionOrderPanel, "PRESCRIPTION_ORDER");
        cardContainer.add(serviceOrderPanel, "SERVICE_ORDER");

        // Đăng ký phím tắt Ctrl+T (hoặc Cmd+T trên Mac) chuyển nhanh đến Chỉ Định Thuốc
        int shortcutMask = java.awt.event.InputEvent.CTRL_DOWN_MASK;
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            }
        } catch (Throwable ignored) {}
        KeyStroke ctrlT = KeyStroke.getKeyStroke('T', shortcutMask);
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ctrlT, "openPrescriptionOrder");
        this.getActionMap().put("openPrescriptionOrder", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cardLayout.show(cardContainer, "PRESCRIPTION_ORDER");
            }
        });

        add(cardContainer, BorderLayout.CENTER);
    }

    private JPanel buildPatientFormPanel() {
        JPanel container = new JPanel(new BorderLayout(0, 4));
        container.setBackground(new Color(238, 243, 250));
        container.setBorder(new EmptyBorder(4, 8, 2, 8));

        // 1. Khung Lưới Thông Tin Hành Chính & Lâm Sàng (9 hàng y hệt ảnh thiết kế)
        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 4, 2, 4);

        int r = 0;

        // Hàng 0: Mã BN (21000325 xanh đậm) | Mã Bệnh Án (23XXB004424 đỏ sẫm) | Bệnh nhân (NGUYỄN MẠNH HÙNG)
        lblMaBN = createValueLabel("21000325", true);
        lblMaBN.setFont(AppFonts.BODY_BOLD);
        lblMaBN.setForeground(new Color(18, 45, 135));

        lblMaBA = createValueLabel("23XXB004424", false);
        lblMaBA.setFont(AppFonts.BODY_BOLD);
        lblMaBA.setForeground(new Color(145, 20, 20));

        lblTenBN = createValueLabel("NGUYỄN MẠNH HÙNG", true);
        addRow(formGrid, c, r++,
                "Mã BN:", lblMaBN, 1,
                "Mã Bệnh Án:", lblMaBA, 1,
                "Bệnh nhân:", lblTenBN, 3,
                null, null, 0);

        // Hàng 1: Giới tính | Ngày sinh | Tuổi | Dân tộc
        lblGioiTinh = createValueLabel("Nam", true);
        lblNgaySinh = createValueLabel("12/10/1984", true);
        lblTuoi = createValueLabel("29", true);
        lblDanToc = createValueLabel("Kinh", true);
        addRow(formGrid, c, r++,
                "Giới tính:", lblGioiTinh, 1,
                "Ngày sinh:", lblNgaySinh, 1,
                "Tuổi:", lblTuoi, 1,
                "Dân tộc:", lblDanToc, 1);

        // Hàng 2: Nghề nghiệp | Quốc tịch | Điện thoại
        lblNgheNghiep = createValueLabel("Nhân dân", true);
        lblQuocTich = createValueLabel("Việt nam", true);
        lblDienThoai = createValueLabel("0938888884", true);
        addRow(formGrid, c, r++,
                "Nghề nghiệp:", lblNgheNghiep, 3,
                "Quốc tịch:", lblQuocTich, 1,
                "Điện thoại:", lblDienThoai, 1,
                null, null, 0);

        // Hàng 3: Địa chỉ | CMND/CCCD
        lblDiaChi = createValueLabel("GAMUDA", true);
        lblCmnd = createValueLabel("", false);
        addRow(formGrid, c, r++,
                "Địa chỉ:", lblDiaChi, 3,
                "CMND/CCCD:", lblCmnd, 3,
                null, null, 0,
                null, null, 0);

        // Hàng 4: Đón tiếp lúc | Loại đón tiếp | Đối tượng
        lblDonTiepLuc = createValueLabel("20:23 21/08/2023", true);
        lblLoaiDonTiep = createValueLabel("Khám bệnh", true);
        lblDoiTuong = createValueLabel("Yêu cầu", true);
        addRow(formGrid, c, r++,
                "Đón tiếp lúc:", lblDonTiepLuc, 1,
                "Loại đón tiếp:", lblLoaiDonTiep, 1,
                "Đối tượng:", lblDoiTuong, 3,
                null, null, 0);

        // Hàng 5: Số vào viện | Số vào khoa | Mã điều trị | Lý do khám
        JLabel lblSoVaoVien = createValueLabel("", false);
        lblSoVaoKhoa = createValueLabel("KKB/14404/23", true);
        lblMaDieuTri = createValueLabel("20566", true);
        lblLyDoKham = createValueLabel("KM", true);
        addRow(formGrid, c, r++,
                "Số vào viện:", lblSoVaoVien, 1,
                "Số vào khoa:", lblSoVaoKhoa, 1,
                "Mã điều trị:", lblMaDieuTri, 1,
                "Lý do khám:", lblLyDoKham, 1);

        // Hàng 6: Khoa phòng | Bác sĩ khám | Điều dưỡng
        lblKhoaPhong = createValueLabel("Khoa Khám Bệnh + Ph. Khám Bệnh - Gamuda Gardens", true);
        lblBacSiKham = createValueLabel("Ths. Bs, Nguyễn Văn Hải", true);
        JLabel lblDieuDuong = createValueLabel("", false);
        addRow(formGrid, c, r++,
                "Khoa phòng:", lblKhoaPhong, 3,
                "Bác sĩ khám:", lblBacSiKham, 1,
                "Điều dưỡng:", lblDieuDuong, 1,
                null, null, 0);

        // Hàng 7: Bệnh án | Ngày vào | Ngày ra | Xử trí
        lblBenhAn = createValueLabel("Khám R-H-M", true);
        lblNgayVao = createValueLabel("20:23 21/08/2023", true);
        JLabel lblNgayRa = createValueLabel("", false);
        JLabel lblXuTri = createValueLabel("", false);
        addRow(formGrid, c, r++,
                "Bệnh án:", lblBenhAn, 1,
                "Ngày vào:", lblNgayVao, 1,
                "Ngày ra:", lblNgayRa, 1,
                "Xử trí:", lblXuTri, 1);

        // Hàng 8: Chẩn đoán (VIÊM HỌNG CẤP / NGHỈ NGÀY)
        lblChanDoan = createValueLabel("VIÊM HỌNG CẤP / NGHỈ NGÀY", true);
        addRow(formGrid, c, r,
                "Chẩn đoán:", lblChanDoan, 3,
                null, null, 0,
                null, null, 0,
                null, null, 0);

        container.add(formGrid, BorderLayout.NORTH);

        // 2. DẢI TAB PHÍA DƯỚI: Lịch Sử Khám và Điều Trị | Thông Tin Xác Thực
        JTabbedPane bottomTabs = createStyledTabbedPane();
        JPanel histPanel = buildHistoryTablePanel();
        bottomTabs.addTab("Lịch Sử Khám và Điều Trị", histPanel);
        bottomTabs.addTab("Thông Tin Xác Thực", createPlaceholderTab("Thông tin thẻ BHYT & xác thực CCCD"));

        container.add(bottomTabs, BorderLayout.CENTER);
        return container;
    }

    /**
     * Lưới 8 cột chuẩn (4 cặp Nhãn - Giá trị). Mỗi cặp chuẩn chiếm 2 cột (1 nhãn + 1 giá trị);
     * nếu một ô kéo dài qua 2 cặp thì truyền valSpan = 3 (1 nhãn + 3 cột = 4 cột).
     */
    private void addRow(JPanel panel, GridBagConstraints c, int row,
                        String l1, JComponent v1, int span1,
                        String l2, JComponent v2, int span2,
                        String l3, JComponent v3, int span3,
                        String l4, JComponent v4, int span4) {
        c.gridy = row;
        int col = 0;

        String[] labels = {l1, l2, l3, l4};
        JComponent[] vals = {v1, v2, v3, v4};
        int[] spans = {span1, span2, span3, span4};

        for (int i = 0; i < 4; i++) {
            if (labels[i] == null || vals[i] == null) continue;
            c.gridx = col++;
            c.gridwidth = 1;
            c.weightx = 0;
            panel.add(createFieldTitle(labels[i]), c);

            c.gridx = col;
            c.gridwidth = spans[i];
            c.weightx = spans[i] >= 3 ? 0.50 : 0.25;
            panel.add(vals[i], c);
            col += spans[i];
        }
        c.gridwidth = 1;
    }

    private JPanel buildHistoryTablePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);

        String[] cols = {"", "Ngày Vào", "Hình Thức Vào", "Khoa", "Phòng", "Thẻ BHYT", "Ngày Sinh", "Ngày Ra", "Xử Trí"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        // Bảng tự vẽ các dòng kẻ lưới mờ trên toàn vùng trắng bên dưới (giống hệt DataGridView trong ảnh)
        historyTable = new JTable(historyModel) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int rowH = getRowHeight();
                int paintedH = getRowCount() * rowH;
                int totalH = getParent() != null ? getParent().getHeight() : getHeight();
                if (totalH > paintedH && rowH > 0) {
                    g.setColor(new Color(238, 241, 245));
                    for (int y = paintedH; y < totalH; y += rowH) {
                        g.drawLine(0, y, getWidth(), y);
                    }
                    int x = 0;
                    for (int i = 0; i < getColumnCount(); i++) {
                        x += getColumnModel().getColumn(i).getWidth();
                        g.drawLine(x - 1, paintedH, x - 1, totalH);
                    }
                }
            }

            @Override
            public boolean getScrollableTracksViewportHeight() {
                return getPreferredSize().height < getParent().getHeight();
            }
        };

        historyTable.setFont(AppFonts.SMALL);
        historyTable.setRowHeight(24);
        historyTable.setShowGrid(true);
        historyTable.setGridColor(new Color(235, 239, 244));
        historyTable.setBackground(Color.WHITE);
        historyTable.setSelectionBackground(new Color(215, 232, 250));
        historyTable.setSelectionForeground(Color.BLACK);

        JTableHeader header = historyTable.getTableHeader();
        header.setPreferredSize(new Dimension(100, 25));
        header.setFont(AppFonts.SMALL_BOLD);
        header.setBackground(new Color(242, 245, 250));
        header.setForeground(new Color(25, 25, 25));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 216, 224)));

        // Độ rộng cột hợp lý theo tỷ lệ 1:1
        int[] colWidths = {26, 125, 105, 130, 165, 110, 95, 95, 95};
        for (int i = 0; i < colWidths.length; i++) {
            historyTable.getColumnModel().getColumn(i).setPreferredWidth(colWidths[i]);
        }
        historyTable.getColumnModel().getColumn(0).setMaxWidth(28);

        // Cột 0: Chấm tròn cam
        historyTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                return new OrangeDotComponent(isSelected);
            }
        });

        // Cột 3 (Khoa): Hộp tích vuông + ". Khám Bệnh"
        historyTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setIcon(new CheckBoxMiniIcon());
                lbl.setText(". " + (value != null ? value.toString() : ""));
                lbl.setFont(AppFonts.SMALL);
                return lbl;
            }
        });

        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setColumnHeaderView(historyTable.getTableHeader());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(205, 214, 226)));
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    private void selectPatient(AdminPatientItem p) {
        if (p == null) return;

        if ("TEST1".equalsIgnoreCase(p.name)) {
            lblBannerLine1.setText("TEST1 | 22000324 | 2000 (22 tuổi) | Nam");
            lblBannerLine2.setText("Mã VP : 214348 | DN-4-01-2929299222 | Còn 864 ngày");
            lblBannerLine3.setText("Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh");
            lblBannerReqTag.setText("<html><center><font color='#800000'><b>BHYT</b></font><br><font color='#0044CC'><b>80%</b></font></center></html>");

            lblMaBN.setText("22000324");
            lblMaBA.setText("22/000063");
            lblTenBN.setText("TEST1");
            lblGioiTinh.setText("Nam");
            lblNgaySinh.setText("2000");
            lblTuoi.setText("22");
            lblDanToc.setText("Kinh");
            lblNgheNghiep.setText("Nhân dân");
            lblQuocTich.setText("Việt nam");
            lblDienThoai.setText("0912345678");
            lblDiaChi.setText("Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh");
            lblDonTiepLuc.setText("11:26 22/08/2022");
            lblLoaiDonTiep.setText("Khám bệnh");
            lblDoiTuong.setText("Bảo hiểm");
            lblSoVaoKhoa.setText("K01/00386/22");
            lblMaDieuTri.setText("317787");
            lblLyDoKham.setText("");
            lblKhoaPhong.setText("Khoa khám bệnh > PK Nhi 1");
            lblBacSiKham.setText("Sennv");
            lblBenhAn.setText("Khoa khám bệnh > PK Nhi 1");
            lblNgayVao.setText("11:26 22/08/2022");
            lblChanDoan.setText("Khám bệnh");

            if (historyModel != null) {
                historyModel.setRowCount(0);
                historyModel.addRow(new Object[]{"", "22/08/2022 11:26", "Trực tiếp", "Khoa K...", "PK Nhi 1", "PK Nhi 1", "DN401292929222", "2000", "", "Đang khám bệnh"});
            }
            return;
        }

        if ("TEST11".equalsIgnoreCase(p.name)) {
            lblBannerLine1.setText("TEST11 | 22000285 | 2000 (22 tuổi) | Nam");
            lblBannerLine2.setText("Mã VP : 214302 | Viện phí");
            lblBannerLine3.setText("Phường Đáp Cầu, TP. Bắc Ninh, Bắc Ninh");
            lblBannerReqTag.setText("<html><center><font color='#800000'><b>Viện<br>phí</b></font></center></html>");

            lblMaBN.setText("22000285");
            lblMaBA.setText("22/000028");
            lblTenBN.setText("TEST11");
            lblGioiTinh.setText("Nam");
            lblNgaySinh.setText("2000");
            lblTuoi.setText("22");
            lblDanToc.setText("Kinh");
            lblNgheNghiep.setText("Nhân dân");
            lblQuocTich.setText("Việt nam");
            lblDienThoai.setText("0912345678");
            lblDiaChi.setText("Phường Đáp Cầu, TP. Bắc Ninh, Bắc Ninh");
            lblDonTiepLuc.setText("10:23 09/08/2022");
            lblLoaiDonTiep.setText("Khám bệnh");
            lblDoiTuong.setText("Viện phí");
            lblSoVaoKhoa.setText("K01/00285/22");
            lblMaDieuTri.setText("214302");
            lblLyDoKham.setText("");
            lblKhoaPhong.setText("Khoa khám bệnh > PK Nhi 1");
            lblBacSiKham.setText("Sennv");
            lblBenhAn.setText("Khám bệnh");
            lblNgayVao.setText("10:23 09/08/2022");
            lblChanDoan.setText("H20-Viêm màng bồ đào");

            if (historyModel != null) {
                historyModel.setRowCount(0);
                historyModel.addRow(new Object[]{"", "10:23 09/08/2022", "Trực tiếp", "Khoa K", "PK Nhi 1", "", "2000", "", "Đang khám bệnh"});
            }
            return;
        }

        if ("NGUYỄN ĐÌNH PHÓNG".equalsIgnoreCase(p.name)) {
            lblBannerLine1.setText("NGUYỄN ĐÌNH PHÓNG | 22000306 | 1989 (33 tuổi) | Nam");
            lblBannerLine2.setText("Mã VP : 214330 | Viện phí");
            lblBannerLine3.setText("Phường Cổ Nhuế 2, Q, Bắc Từ Liêm, TP Hà Nội");
            lblBannerReqTag.setText("<html><center><font color='#800000'><b>Viện</b></font><br><font color='#800000'><b>phí</b></font></center></html>");

            lblMaBN.setText("22000306");
            lblMaBA.setText("22/000046");
            lblTenBN.setText("NGUYỄN ĐÌNH PHÓNG");
            lblGioiTinh.setText("Nam");
            lblNgaySinh.setText("1989");
            lblTuoi.setText("33");
            lblDanToc.setText("Kinh");
            lblNgheNghiep.setText("Nhân dân");
            lblQuocTich.setText("Việt nam");
            lblDienThoai.setText("0987654321");
            lblDiaChi.setText("Phường Cổ Nhuế 2, Q, Bắc Từ Liêm, TP Hà Nội");
            lblDonTiepLuc.setText("15:36 11/08/2022");
            lblLoaiDonTiep.setText("Khám bệnh");
            lblDoiTuong.setText("Viện phí");
            lblSoVaoKhoa.setText("KKB/14402/23");
            lblMaDieuTri.setText("20564");
            lblLyDoKham.setText("Khám bệnh");
            lblKhoaPhong.setText("Khoa khám bệnh > PK Nhi 1");
            lblBacSiKham.setText("Sennv");
            lblBenhAn.setText("Khám bệnh");
            lblNgayVao.setText("15:36 11/08/2022");
            lblChanDoan.setText("Chờ khám");

            if (historyModel != null) {
                historyModel.setRowCount(0);
                historyModel.addRow(new Object[]{"", "15:36 11/08/2022", "Trực tiếp", "Khoa K", "PK Nhi 1", "", "1989", "", "Đang chờ khám"});
            }
            return;
        }

        if ("NGUYỄN THỊ BÌNH AN".equalsIgnoreCase(p.name)) {
            lblBannerLine1.setText("NGUYỄN THỊ BÌNH AN | 22000308 | 1999 (23 tuổi) | Nữ");
            lblBannerLine2.setText("Mã VP : 214332 | DN-4-01-4337777888 | Còn 864 ngày");
            lblBannerLine3.setText("Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh");
            lblBannerReqTag.setText("<html><center><font color='#800000'><b>BHYT</b></font><br><font color='#0044CC'><b>80%</b></font></center></html>");

            lblMaBN.setText("22000308");
            lblMaBA.setText("22/000047");
            lblTenBN.setText("NGUYỄN THỊ BÌNH AN");
            lblGioiTinh.setText("Nữ");
            lblNgaySinh.setText("1995");
            lblTuoi.setText("23");
            lblDanToc.setText("Kinh");
            lblNgheNghiep.setText("Nhân dân");
            lblQuocTich.setText("Việt nam");
            lblDienThoai.setText("0945123456");
            lblDiaChi.setText("Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh");
            lblDonTiepLuc.setText("15:37 11/08/2022");
            lblLoaiDonTiep.setText("Khám bệnh");
            lblDoiTuong.setText("Bảo hiểm");
            lblSoVaoKhoa.setText("K01/00370/22");
            lblMaDieuTri.setText("217690");
            lblLyDoKham.setText("");
            lblKhoaPhong.setText("Khoa khám bệnh > PK Nhi 1");
            lblBacSiKham.setText("Sennv");
            lblBenhAn.setText("Khám bệnh");
            lblNgayVao.setText("15:39 11/08/2022");
            lblChanDoan.setText("A02.0-Viêm ruột do Salmonella");

            if (historyModel != null) {
                historyModel.setRowCount(0);
                historyModel.addRow(new Object[]{"", "15:39 11/08/2022", "Trực tiếp", "Khoa K", "PK Nhi 1", "DN-4-01-8337377888", "1995", "", "Đang khám bệnh"});
            }
            return;
        }

        lblBannerLine1.setText(p.name + " | " + p.code + " | " + p.birthYear + " (" + p.bannerAge + ") | " + p.gender);
        lblBannerLine2.setText("VP: " + p.invoiceCode + " | " + p.requestType);
        lblBannerLine3.setText(p.bannerAddress + " | " + p.phone);

        lblMaBN.setText(p.code);
        lblMaBA.setText(p.medicalRecordCode);
        lblTenBN.setText(p.name);
        lblGioiTinh.setText(p.gender);
        lblNgaySinh.setText(p.birthDate);
        lblTuoi.setText(p.formAge);
        lblDanToc.setText("Kinh");
        lblNgheNghiep.setText("Nhân dân");
        lblQuocTich.setText("Việt nam");
        lblDienThoai.setText(p.phone);
        lblDiaChi.setText(p.address);
        lblDonTiepLuc.setText(p.admissionDate);
        lblLoaiDonTiep.setText("Khám bệnh");
        lblDoiTuong.setText(p.requestType);
        lblSoVaoKhoa.setText("KKB/14404/23");
        lblMaDieuTri.setText("20566");
        lblLyDoKham.setText("KM");
        lblKhoaPhong.setText(p.department);
        lblBacSiKham.setText(p.doctor);
        lblBenhAn.setText("Khám R-H-M");
        lblNgayVao.setText("20:23:21/08/2023");
        lblChanDoan.setText(p.diagnosis);

        if (treatmentResolutionTabPanel != null) {
            treatmentResolutionTabPanel.setPatientInfo(p.code, p.name, p.formAge, p.gender, p.address, p.doctor != null ? p.doctor : "bieuCBT");
        }

        historyModel.setRowCount(0);
        historyModel.addRow(new Object[]{"", "20:23 21/08/2023", "Trực tiếp", "Khám Bệnh", "Ph. Khám Bệnh - ...", "", "12/10/1984", "", "Đang khám"});
        historyModel.addRow(new Object[]{"", "19:33 19/02/2021", "Trực tiếp", "Khám Bệnh", "Ph. Khám Bệnh - ...", "", "1984", "", "Đang khám"});
        historyModel.addRow(new Object[]{"", "20:26 02/02/2021", "Trực tiếp", "Khám Bệnh", "Ph. Khám Bệnh - ...", "", "1984", "", "Đang khám"});
    }

    public void selectPatientByIndex(int index) {
        if (index >= 0 && index < patientList.size()) {
            if (queueTable != null) {
                queueTable.setRowSelectionInterval(index, index);
            }
            selectPatient(patientList.get(index));
        }
    }

    public void selectPatientByName(String name) {
        if (name == null) return;
        for (int i = 0; i < patientList.size(); i++) {
            if (name.equalsIgnoreCase(patientList.get(i).name)) {
                selectPatientByIndex(i);
                return;
            }
        }
    }

    public void showQueueContextMenu(Component comp, int x, int y) {
        int row = queueTable.getSelectedRow();
        AdminPatientItem patient = (row >= 0 && row < patientList.size()) ? patientList.get(row) : null;
        String name = patient != null ? patient.name : "Bệnh nhân";
        String code = patient != null ? patient.code : "";
        boolean isExamined = patient != null && !patient.hasOrangeDot;

        PatientQueueContextMenu menu = new PatientQueueContextMenu(name, code, isExamined, action -> {
            handlePatientQueueAction(action, patient);
        });
        menu.show(comp, x, y);
    }

    private void handlePatientQueueAction(String action, AdminPatientItem patient) {
        String name = patient != null ? patient.name : "Bệnh nhân";
        String code = patient != null ? patient.code : "";

        if ("Bệnh Án".equalsIgnoreCase(action)) {
            selectMedicalRecordTab();
        } else if ("Mở Lại Bệnh Án".equalsIgnoreCase(action)) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            ReopenMedicalRecordDialog dlg = new ReopenMedicalRecordDialog(owner, patient, req -> {
                if ("Mở lại bệnh án, huỷ xử trí".equals(req.reopenType)) {
                    selectTreatmentTab();
                } else {
                    selectMedicalRecordTab();
                }
            });
            dlg.setVisible(true);
        } else if ("Lịch Sử Khám Bệnh".equalsIgnoreCase(action)) {
            selectMedicalRecordTab();
            JOptionPane.showMessageDialog(this,
                    "Đang mở Lịch Sử Khám Bệnh của bệnh nhân: " + name + " (" + code + ")",
                    "Lịch Sử Khám Bệnh", JOptionPane.INFORMATION_MESSAGE);
        } else if ("Chuyển Bệnh Nhân Sang Phòng Khác".equalsIgnoreCase(action)) {
            // Bước 1: Màn hình thông báo xác nhận chuyển phòng (Khớp media_1793381484099_6087d085.jpg)
            boolean confirmed = TransferRoomConfirmDialog.showConfirm(this);
            if (confirmed) {
                // Bước 2: Hộp thoại chọn phòng khám đích (Khớp media_1793381550077_6c743cbd.jpg)
                String chosenRoom = TransferRoomSelectDialog.selectRoom(this);
                if (chosenRoom != null && !chosenRoom.trim().isEmpty()) {
                    // Bước 3: Xem trước và In Phiếu Khám Bệnh / Phiếu Chuyển Phòng Khám (Khớp media_1793381633910_5fdd704b.jpg)
                    Window owner = SwingUtilities.getWindowAncestor(this);
                    TransferSlipPrintPreviewDialog printDlg = new TransferSlipPrintPreviewDialog(owner, name, code, chosenRoom);
                    printDlg.setVisible(true);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Đang thực hiện chức năng: " + action + "\n▶ Bệnh nhân: " + name + " (" + code + ")",
                    "Thao Tác Bệnh Nhân", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public LabOrderTabPanel getLabOrderTabPanel() {
        return labOrderTabPanel;
    }

    public SuppliesOrderTabPanel getSuppliesTabPanel() {
        return suppliesTabPanel;
    }

    public PrescriptionOrderTabPanel getPrescriptionTabPanel() {
        return prescriptionTabPanel;
    }

    public TreatmentResolutionTabPanel getTreatmentResolutionTabPanel() {
        return treatmentResolutionTabPanel;
    }

    public MedicalRecordTabPanel getMedicalRecordTabPanel() {
        return medicalRecordTabPanel;
    }

    public void selectMedicalRecordTab() {
        if (clinicalTabs != null && medicalRecordTabPanel != null) {
            clinicalTabs.setSelectedComponent(medicalRecordTabPanel);
        }
    }

    public void selectTreatmentTab() {
        if (clinicalTabs != null && treatmentResolutionTabPanel != null) {
            clinicalTabs.setSelectedComponent(treatmentResolutionTabPanel);
        }
    }

    public ServiceOrderPanel getServiceOrderPanel() {
        return serviceOrderPanel;
    }

    public JTabbedPane getClinicalTabs() {
        return clinicalTabs;
    }

    private JTabbedPane createStyledTabbedPane() {
        JTabbedPane tp = new JTabbedPane();
        tp.setFont(AppFonts.SMALL_BOLD);
        tp.setBackground(new Color(214, 228, 245));
        tp.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(2, 4, 0, 4);
                contentBorderInsets = new Insets(1, 0, 0, 0);
                tabInsets = new Insets(4, 10, 4, 10);
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
                    g2.setColor(new Color(242, 247, 253));
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
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
                // Không vẽ viền nét đứt focus
            }
        });
        return tp;
    }

    private JLabel createFieldTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppFonts.SMALL);
        lbl.setForeground(new Color(25, 25, 25));
        return lbl;
    }

    private JLabel createValueLabel(String text, boolean isNavy) {
        JLabel lbl = new JLabel(text != null && !text.isEmpty() ? text : " ");
        lbl.setFont(AppFonts.SMALL);
        lbl.setForeground(isNavy ? new Color(18, 45, 130) : new Color(25, 25, 25));
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(175, 185, 200)));
        return lbl;
    }

    private JButton createToolbarButton(String text, String iconType, boolean hasDropdownArrow) {
        String labelText = hasDropdownArrow ? text + " ▾" : text;
        JButton btn = new JButton(labelText, new SubToolbarVectorIcon(iconType));
        btn.setFont(AppFonts.SMALL);
        btn.setForeground(new Color(20, 20, 20));
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setIconTextGap(2);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(3, 8, 3, 8));
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

        if ("Menu".equalsIgnoreCase(text)) {
            ExaminationMenuPopupMenu menuPopup = new ExaminationMenuPopupMenu(btn, actionTitle -> {
                handleMenuAction(actionTitle);
            });
            btn.addActionListener(e -> {
                if (btn.isShowing()) {
                    menuPopup.show(btn, 0, btn.getHeight());
                }
            });
        } else if ("In".equalsIgnoreCase(text)) {
            PrintReportPopupMenu printMenu = new PrintReportPopupMenu(btn, reportTitle -> {
                if (reportTitle != null && reportTitle.contains("Đăng Ký Khám Bệnh, Chữa Bệnh")) {
                    MedicalBillingSlipDialog dialog = new MedicalBillingSlipDialog(SwingUtilities.getWindowAncestor(this));
                    dialog.setVisible(true);
                } else if (reportTitle != null && reportTitle.contains("Bệnh Án Khám Bệnh")) {
                    MedicalRecordPrintPreviewDialog dialog = new MedicalRecordPrintPreviewDialog(SwingUtilities.getWindowAncestor(this));
                    dialog.setVisible(true);
                } else {
                    if (!GraphicsEnvironment.isHeadless()) {
                        JOptionPane.showMessageDialog(this,
                                "Hệ thống đang chuẩn bị xuất bản in:\n" +
                                "▶ " + reportTitle + "\n\n" +
                                "Bệnh nhân: TEST1 | 22000324 (BHYT 80%)\n" +
                                "Bác sĩ chỉ định: BS. Sennv\n" +
                                "Khoa phòng: Khoa khám bệnh > PK Nhi 1",
                                "Xuất & In Phiếu Khám Bệnh EHC", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });
            btn.addActionListener(e -> printMenu.show(btn, 0, btn.getHeight()));
        } else if ("Thuốc/VT".equalsIgnoreCase(text)) {
            MedicineSuppliesPopupMenu medMenu = new MedicineSuppliesPopupMenu(btn, actionTitle -> {
                if (actionTitle != null && actionTitle.contains("Chỉ Định Vật Tư")) {
                    // Chuyển hướng trực tiếp trong cùng màn hình, không mở form dialog riêng biệt
                    cardLayout.show(cardContainer, "SUPPLIES_ORDER");
                } else if (actionTitle != null && actionTitle.contains("Chỉ Định Thuốc")) {
                    // Chuyển hướng trực tiếp đến màn hình Chỉ Định Thuốc trong cùng workspace
                    cardLayout.show(cardContainer, "PRESCRIPTION_ORDER");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Hệ thống đang mở phân hệ:\n" +
                            "▶ " + actionTitle + "\n\n" +
                            "Bệnh nhân: NGUYỄN THỊ BÌNH AN | 22000308 (BHYT 80%)\n" +
                            "Bác sĩ chỉ định: BS. Sennv\n" +
                            "Khoa phòng: Khoa khám bệnh > PK Nhi 1",
                            "Quản Lý Thuốc & Vật Tư Y Tế", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            btn.addActionListener(e -> medMenu.show(btn, 0, btn.getHeight()));
        } else if ("Dịch Vụ".equalsIgnoreCase(text)) {
            btn.addActionListener(e -> {
                cardLayout.show(cardContainer, "SERVICE_ORDER");
            });
        }

        return btn;
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

    private void handleMenuAction(String actionTitle) {
        if ("Đóng".equalsIgnoreCase(actionTitle)) {
            return;
        }
        if ("Chuyển Loại Bệnh Án".equalsIgnoreCase(actionTitle)) {
            Window win = SwingUtilities.getWindowAncestor(this);
            MedicalRecordTypeDialog dialog = new MedicalRecordTypeDialog(win, selectedType -> {
                if (lblBenhAn != null) {
                    lblBenhAn.setText(selectedType);
                }
            });
            dialog.setVisible(true);
            return;
        }
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(this,
                    "Chức năng: " + actionTitle + "\n" +
                    "Đang được kích hoạt trong phân hệ Khám Bệnh.",
                    actionTitle, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {}

    // =========================================================================
    // CUSTOM CELL RENDERERS & VECTOR ICONS (ĐẢM BẢO HIỂN THỊ 1:1 TRÊN MỌI OS)
    // =========================================================================

    /** Cột 0 của bảng hàng đợi: Vẽ chấm cam / tick xanh + Số thứ tự căn giữa */
    private static class SttCellRenderer extends JComponent implements javax.swing.table.TableCellRenderer {
        private AdminPatientItem item;
        private boolean selected;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.item = value instanceof AdminPatientItem ? (AdminPatientItem) value : null;
            this.selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(selected ? new Color(0, 114, 198) : Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (item != null) {
                // Vùng vẽ badge bên trái
                g2.setColor(selected ? new Color(0, 114, 198) : Color.WHITE);
                g2.fillRect(0, 0, 18, getHeight());

                int dy = (getHeight() - 13) / 2;
                if (item.hasOrangeDot) {
                    // Badge tròn cam với dấu cộng trắng ở giữa (hoặc chấm cam)
                    g2.setColor(new Color(245, 130, 32));
                    g2.fillOval(2, dy, 13, 13);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawLine(5, dy + 6, 11, dy + 6);
                    g2.drawLine(8, dy + 3, 8, dy + 9);
                } else {
                    // Badge tròn xanh ngọc (cyan / teal) với dấu tick trắng
                    g2.setColor(new Color(36, 160, 175));
                    g2.fillOval(2, dy, 13, 13);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawLine(4, dy + 6, 7, dy + 9);
                    g2.drawLine(7, dy + 9, 11, dy + 4);
                }

                g2.setFont(AppFonts.SMALL);
                g2.setColor(selected ? Color.WHITE : new Color(25, 25, 25));
                FontMetrics fm = g2.getFontMetrics();
                int tx = 18 + (getWidth() - 18 - fm.stringWidth(item.stt)) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(item.stt, tx, ty);
            }
            g2.dispose();
        }
    }

    /** Cột 1 của bảng hàng đợi: Vẽ 2 dòng (Tên in đậm ở trên + Mã BN - Yêu cầu ở dưới) */
    private static class PatientNameCellRenderer extends JComponent implements javax.swing.table.TableCellRenderer {
        private AdminPatientItem item;
        private boolean selected;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.item = value instanceof AdminPatientItem ? (AdminPatientItem) value : null;
            this.selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(selected ? new Color(0, 114, 198) : Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (item != null) {
                g2.setFont(AppFonts.SMALL_BOLD);
                g2.setColor(selected ? Color.WHITE : new Color(15, 15, 15));
                g2.drawString(item.name, 4, 18);

                g2.setFont(AppFonts.SMALL);
                g2.setColor(selected ? new Color(235, 245, 255) : new Color(45, 45, 45));
                g2.drawString(item.code + " - " + item.requestType, 4, 34);
            }
            g2.dispose();
        }
    }

    /** Chấm tròn cam ở cột 0 của bảng Lịch sử khám */
    private static class OrangeDotComponent extends JComponent {
        private final boolean selected;
        OrangeDotComponent(boolean selected) { this.selected = selected; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(selected ? new Color(215, 232, 250) : Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(235, 135, 20));
            g2.fillOval((getWidth() - 12) / 2, (getHeight() - 12) / 2, 12, 12);
            g2.dispose();
        }
    }

    /** Hộp tích vuông ở cột Khoa của bảng Lịch sử khám */
    private static class CheckBoxMiniIcon implements Icon {
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 12; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(140, 145, 155));
            g2.drawRect(x, y + 1, 10, 10);
            g2.setColor(new Color(70, 80, 90));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawLine(x + 2, y + 6, x + 4, y + 9);
            g2.drawLine(x + 4, y + 9, x + 9, y + 3);
            g2.dispose();
        }
    }

    private static class LeftTriangleIcon implements Icon {
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(55, 110, 170));
            g2.fillPolygon(new int[]{x + 10, x + 2, x + 10}, new int[]{y + 1, y + 7, y + 13}, 3);
            g2.dispose();
        }
    }

    private static class MiniMagnifierIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(55, 115, 175));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x + 1, y + 1, 8, 8);
            g2.drawLine(x + 8, y + 8, x + 12, y + 12);
            g2.dispose();
        }
    }

    private static class SmallFormIcon implements Icon {
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 12; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(new Color(80, 120, 170));
            g.drawRect(x + 1, y + 1, 9, 9);
            g.drawLine(x + 3, y + 4, x + 8, y + 4);
            g.drawLine(x + 3, y + 7, x + 8, y + 7);
        }
    }

    /** Bộ icon vector 24x24 cho Sub-Toolbar (Menu, In, Làm mới, Tìm Kiếm, Dịch Vụ, Thuốc/VT, Thanh Toán, Màn Hình Chờ) */
    private static class SubToolbarVectorIcon implements Icon {
        private final String type;
        SubToolbarVectorIcon(String type) { this.type = type; }

        @Override public int getIconWidth() { return 26; }
        @Override public int getIconHeight() { return 24; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);

            switch (type) {
                case "menu":
                    g2.setColor(new Color(55, 110, 165));
                    g2.fillRect(3, 4, 20, 3);
                    g2.fillRect(3, 10, 20, 3);
                    g2.fillRect(3, 16, 20, 3);
                    break;
                case "print":
                    g2.setColor(new Color(120, 135, 155));
                    g2.fillRect(5, 9, 16, 9);
                    g2.setColor(Color.WHITE);
                    g2.fillRect(7, 3, 12, 6);
                    g2.fillRect(7, 14, 12, 7);
                    g2.setColor(new Color(75, 115, 165));
                    g2.drawRect(7, 3, 12, 6);
                    g2.drawRect(7, 14, 12, 7);
                    break;
                case "refresh":
                    g2.setColor(new Color(75, 130, 185));
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.drawArc(4, 3, 17, 17, 35, 280);
                    g2.fillPolygon(new int[]{18, 23, 17}, new int[]{3, 8, 9}, 3);
                    break;
                case "search":
                    g2.setColor(new Color(180, 215, 245));
                    g2.fillOval(4, 2, 13, 13);
                    g2.setColor(new Color(70, 120, 175));
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawOval(4, 2, 13, 13);
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawLine(15, 13, 21, 20);
                    break;
                case "service":
                    // 2 ống nghiệm trên giá đỡ
                    g2.setColor(new Color(140, 150, 165));
                    g2.fillRect(4, 18, 18, 3);
                    g2.drawLine(6, 10, 20, 10);
                    g2.setColor(new Color(220, 80, 70));
                    g2.fillRect(8, 5, 3, 12);
                    g2.setColor(new Color(65, 145, 215));
                    g2.fillRect(15, 5, 3, 12);
                    break;
                case "medicine":
                    // 2 lọ thuốc cam-vàng
                    g2.setColor(new Color(230, 165, 95));
                    g2.fillRoundRect(4, 7, 8, 13, 2, 2);
                    g2.fillRoundRect(14, 9, 8, 11, 2, 2);
                    g2.setColor(Color.WHITE);
                    g2.fillRect(5, 11, 6, 5);
                    g2.fillRect(15, 13, 6, 4);
                    g2.setColor(new Color(165, 105, 45));
                    g2.drawRoundRect(4, 7, 8, 13, 2, 2);
                    g2.drawRoundRect(14, 9, 8, 11, 2, 2);
                    break;
                case "payment":
                    // Bảng phiếu thanh toán có chữ thập đỏ
                    g2.setColor(Color.WHITE);
                    g2.fillRect(5, 3, 15, 18);
                    g2.setColor(new Color(110, 115, 125));
                    g2.drawRect(5, 3, 15, 18);
                    g2.fillRect(9, 1, 7, 3);
                    g2.setColor(new Color(210, 55, 55));
                    g2.fillRect(11, 7, 3, 7);
                    g2.fillRect(9, 9, 7, 3);
                    break;
                default:
                    // Màn hình chờ (3 chấm + 3 gạch ngang)
                    g2.setColor(new Color(75, 115, 160));
                    for (int i = 0; i < 3; i++) {
                        int ry = 5 + i * 5;
                        g2.fillOval(5, ry, 3, 3);
                        g2.fillRect(10, ry, 11, 2);
                    }
                    break;
            }
            g2.dispose();
        }
    }
}
