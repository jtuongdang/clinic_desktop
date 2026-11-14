package vn.clinicmanagement.desktop.modules.examination;

import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.components.menus.KhamBenhMenuBatLenMenu;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.XetNghiemYeuCauTheManHinh;
import vn.clinicmanagement.desktop.modules.examination.components.print.YTeVienPhiPhieuHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.print.YTeRecordPrintPreviewHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.YTeRecordLoaiHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.YTeVatTuYeuCauHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.YTeVatTuYeuCauManHinh;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ReopenYTeRecordHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.menus.ThuocVatTuBatLenMenu;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.DonThuocYeuCauManHinh;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.DonThuocYeuCauTheManHinh;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.YTeRecordTheManHinh;
import vn.clinicmanagement.desktop.modules.examination.components.menus.BenhNhanHangDoiNguCanhMenu;
import vn.clinicmanagement.desktop.modules.examination.components.menus.PrintBaoCaoBatLenMenu;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.DichVuYeuCauHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.DichVuYeuCauManHinh;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.VatTuYeuCauTheManHinh;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.TransferPhongConfirmHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.TransferPhongSelectHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.print.TransferPhieuPrintPreviewHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.DieuTriXuTriTheManHinh;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
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
public class KhamBenhManHinh extends CoSoManHinhManHinh {

    public static class AdminPatientItem {
        public final String stt;
        /** Trường `name`: tên. */
        public final String name;
        /** Trường `code`: mã. */
        public final String code;
        /** Trường `requestType`: yêu cầu loai. */
        public final String requestType;
        /** Trường `queueAge`: hangdoi `Age`. */
        public final String queueAge;
        /** Trường `hasOrangeDot`: có `Orange` `Dot`. */
        public final boolean hasOrangeDot;
        public final String gender;
        public final String birthYear;
        public final String bannerAge;
        /** Trường `formAge`: bieumau `Age`. */
        public final String formAge;
        /** Trường `birthDate`: `birth` ngày. */
        public final String birthDate;
        public final String address;
        public final String bannerAddress;
        /** Trường `phone`: dienthoai. */
        public final String phone;
        /** Trường `medicalRecordCode`: yte ghi nhận mã. */
        public final String medicalRecordCode;
        /** Trường `admissionDate`: `admission` ngày. */
        public final String admissionDate;
        /** Trường `department`: khoa. */
        public final String department;
        /** Trường `doctor`: bacsi. */
        public final String doctor;
        /** Trường `diagnosis`: chandoan. */
        public final String diagnosis;
        /** Trường `invoiceCode`: hóa đơn mã. */
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

    /** Trường `patientList`: bệnh nhân `List`. */
    private final List<AdminPatientItem> patientList = new ArrayList<>();
    /** Trường `queueTable`: hangdoi bảng. */
    private JTable queueTable;
    /** Trường `queueModel`: hangdoi mô hình. */
    private DefaultTableModel queueModel;

    // Quản lý chuyển hướng màn hình (Navigation) trong phân hệ Khám Bệnh
    private CardLayout cardLayout;
    private JPanel cardContainer;
    /** Trường `suppliesOrderPanel`: vattu yêu cầu màn hình. */
    private YTeVatTuYeuCauManHinh suppliesOrderPanel;
    /** Trường `prescriptionOrderPanel`: đơn thuốc yêu cầu màn hình. */
    private DonThuocYeuCauManHinh prescriptionOrderPanel;
    /** Trường `serviceOrderPanel`: dịch vụ yêu cầu màn hình. */
    private DichVuYeuCauManHinh serviceOrderPanel;
    /** Trường `clinicalTabs`: lamsang `Tabs`. */
    private JTabbedPane clinicalTabs;
    /** Trường `suppliesTabPanel`: vattu the màn hình. */
    private VatTuYeuCauTheManHinh suppliesTabPanel;
    /** Trường `prescriptionTabPanel`: đơn thuốc the màn hình. */
    private DonThuocYeuCauTheManHinh prescriptionTabPanel;
    /** Trường `labOrderTabPanel`: xetnghiem yêu cầu the màn hình. */
    private XetNghiemYeuCauTheManHinh labOrderTabPanel;
    /** Trường `medicalRecordTabPanel`: yte ghi nhận the màn hình. */
    private YTeRecordTheManHinh medicalRecordTabPanel;
    /** Trường `treatmentResolutionTabPanel`: dieutri xutri the màn hình. */
    private DieuTriXuTriTheManHinh treatmentResolutionTabPanel;

    // Header Right Patient Banner
    private JLabel lblBannerLine1;
    private JLabel lblBannerReqTag;
    /** Trường `lblBannerLine2`: `lbl` `Banner` đóng `2`. */
    private JLabel lblBannerLine2;
    /** Trường `lblBannerLine3`: `lbl` `Banner` đóng `3`. */
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
    /** Trường `historyTable`: tiensu bảng. */
    private JTable historyTable;
    /** Trường `services`: dịch vụ. */
    private final KhamBenhGhepNoiGoc.Services services;

    public KhamBenhManHinh() {
        this(resolveDefaultServices());
    }

    public KhamBenhManHinh(KhamBenhGhepNoiGoc.Services services) {
        super("examination", "Khám Bệnh", "🩺");
        this.services = services != null ? services : resolveDefaultServices();
        initData();
        buildUI();
        selectPatientByIndex(0);
    }

    /** Hàm `resolveDefaultServices`: `resolve` macdinh dịch vụ. */
    private static KhamBenhGhepNoiGoc.Services resolveDefaultServices() {
        try {
            return KhamBenhGhepNoiGoc.createDefault();
        } catch (Exception ex) {
            return null;
        }
    }

    /** Hàm `initData`: `init` dữ liệu. */
    private void initData() {
        patientList.clear();
        try (Connection conn = CoSoDuLieuKetNoi.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT bn.id, bn.maBenhNhanFormat, bn.ho, bn.ten, bn.gioiTinh, bn.ngaySinh, " +
                     "       bn.diaChi, bn.dienThoai, lk.id AS maLuotKham, lk.trangThai, " +
                     "       lk.soThuTuDisplay, pk.tenPhong, kp.tenKhoa, " +
                     "       ISNULL(nv.ho + ' ' + nv.ten, N'BS. Tiến') AS tenBacSi, " +
                     "       lk.thoiGianTiepNhan, lk.lyDoKham " +
                     "FROM BenhNhan bn " +
                     "LEFT JOIN LuotKham lk ON lk.maBenhNhan = bn.id AND lk.isDeleted = 0 " +
                     "LEFT JOIN PhongKham pk ON lk.maPhongKham = pk.id " +
                     "LEFT JOIN Khoa kp ON lk.maKhoa = kp.id " +
                     "LEFT JOIN NhanVien nv ON lk.maBacSi = nv.id " +
                     "WHERE bn.isDeleted = 0 " +
                     "ORDER BY lk.id DESC, bn.id DESC");
             ResultSet rs = ps.executeQuery()) {
            int stt = 1;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            while (rs.next()) {
                String fullName = (rs.getString("ho") + " " + rs.getString("ten")).trim().toUpperCase();
                String code = rs.getString("maBenhNhanFormat");
                java.sql.Date birthDateSql = rs.getDate("ngaySinh");
                int birthYear = birthDateSql != null ? birthDateSql.toLocalDate().getYear() : 1990;
                int age = java.time.LocalDate.now().getYear() - birthYear;
                String genderStr = "F".equalsIgnoreCase(rs.getString("gioiTinh")) ? "Nữ" : "Nam";
                String address = rs.getString("diaChi") != null ? rs.getString("diaChi") : "Bắc Ninh";
                String phone = rs.getString("dienThoai") != null ? rs.getString("dienThoai") : "";
                int visitId = rs.getInt("maLuotKham");
                String admissionTime = rs.getTimestamp("thoiGianTiepNhan") != null
                        ? rs.getTimestamp("thoiGianTiepNhan").toLocalDateTime().format(dtf)
                        : "08:00 07/11/2026";
                String dept = (rs.getString("tenKhoa") != null ? rs.getString("tenKhoa") : "Khoa Khám Bệnh") +
                        " > " + (rs.getString("tenPhong") != null ? rs.getString("tenPhong") : "PK Nhi 1");
                String doctorName = rs.getString("tenBacSi");
                String reason = rs.getString("lyDoKham") != null ? rs.getString("lyDoKham") : "Chờ khám";

                patientList.add(new AdminPatientItem(
                        String.valueOf(stt++),
                        fullName,
                        code != null ? code : "BN-" + rs.getInt("id"),
                        "Viện phí",
                        String.valueOf(age),
                        true,
                        genderStr,
                        String.valueOf(birthYear),
                        age + " tuổi",
                        String.valueOf(age),
                        birthDateSql != null ? birthDateSql.toString() : String.valueOf(birthYear),
                        address,
                        address,
                        phone,
                        "BA-" + (visitId > 0 ? visitId : rs.getInt("id")),
                        admissionTime,
                        dept,
                        doctorName,
                        reason,
                        "VP-" + (visitId > 0 ? visitId : rs.getInt("id"))
                ));
            }
        } catch (Exception ignored) {
        }

        if (patientList.isEmpty()) {
            patientList.add(new AdminPatientItem("1", "NGUYỄN HOÀNG LONG", "BN-20261107-0003", "Viện phí", "31", true,
                    "Nam", "1995", "31 tuổi", "31", "12/04/1995", "Số 12 Lý Thái Tổ, TP. Bắc Ninh",
                    "Số 12 Lý Thái Tổ, TP. Bắc Ninh", "0912888999", "BA-2026110701",
                    "08:30 07/11/2026", "Khoa Khám Bệnh > PK Nhi 1", "BS. Nguyễn Văn Tiến", "Sốt nhẹ kèm ho khan 2 ngày", "VP-2026110701"));
            patientList.add(new AdminPatientItem("2", "PHẠM QUỲNH TRANG", "BN-20261107-0004", "BHYT", "8", true,
                    "Nữ", "2018", "8 tuổi", "8", "20/09/2018", "Đồng Kỵ, Từ Sơn, Bắc Ninh",
                    "Đồng Kỵ, Từ Sơn, Bắc Ninh", "0988777666", "BA-2026110702",
                    "08:45 07/11/2026", "Khoa Khám Bệnh > PK Nhi 1", "BS. Nguyễn Văn Tiến", "Kiểm tra định kỳ và viêm họng", "VP-2026110702"));
            patientList.add(new AdminPatientItem("3", "TRẦN VĂN BÌNH", "BN-20261104-35777CEC", "Viện phí", "38", true,
                    "Nam", "1988", "38 tuổi", "38", "15/05/1988", "Hà Nội",
                    "Hà Nội", "0985906271", "BA-2026110401",
                    "09:00 07/11/2026", "Khoa Khám Bệnh > Cấp cứu nhi", "BS. Nguyễn Văn Tiến", "Đau bụng vùng thượng vị", "VP-2026110401"));
            patientList.add(new AdminPatientItem("4", "LÊ THỊ MAI", "BN-20261104-308B2B4D", "BHYT", "34", true,
                    "Nữ", "1992", "34 tuổi", "34", "10/10/1992", "Bắc Ninh",
                    "Bắc Ninh", "0975906413", "BA-2026110402",
                    "09:15 07/11/2026", "Khoa Khám Bệnh > Cấp cứu nhi", "BS. Nguyễn Văn Tiến", "Đau đầu, chóng mặt", "VP-2026110402"));
        }
    }

    /** Hàm `buildUI`: tạo giao diện. */
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
        lblBannerLine1.setFont(UngDungPhongChu.SECTION);
        lblBannerLine1.setForeground(new Color(125, 15, 15));

        lblBannerLine2 = new JLabel("VP: 20553 | Yêu cầu", SwingConstants.RIGHT);
        lblBannerLine2.setFont(UngDungPhongChu.BODY);
        lblBannerLine2.setForeground(new Color(125, 15, 15));

        lblBannerLine3 = new JLabel("GARDEIA | 0938888884", SwingConstants.RIGHT);
        lblBannerLine3.setFont(UngDungPhongChu.BODY);
        lblBannerLine3.setForeground(new Color(125, 15, 15));

        bannerTextPanel.add(lblBannerLine1);
        bannerTextPanel.add(lblBannerLine2);
        bannerTextPanel.add(lblBannerLine3);

        lblBannerReqTag = new JLabel("<html><center>Yêu<br>cầu</center></html>", SwingConstants.CENTER);
        lblBannerReqTag.setFont(UngDungPhongChu.BODY_BOLD);
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
        lblTuyChon.setFont(UngDungPhongChu.SMALL);
        lblTuyChon.setForeground(new Color(30, 30, 30));
        leftTuyChonBox.add(lblArrowLeft);
        leftTuyChonBox.add(lblTuyChon);

        JPanel tabPatients = new JPanel(new BorderLayout());
        tabPatients.setBackground(new Color(254, 240, 205));
        tabPatients.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 0, new Color(220, 185, 120)));
        JLabel lblTabPatients = new JLabel("Tất cả bệnh nhân (29)", SwingConstants.CENTER);
        lblTabPatients.setFont(UngDungPhongChu.BODY_BOLD);
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
        lblFilter.setFont(UngDungPhongChu.SMALL);
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
            /** Hàm `isCellEditable`: là `Cell` `Editable`. */
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
        qHeader.setFont(UngDungPhongChu.SMALL_BOLD);
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
            /** Hàm `getTableCellRendererComponent`: lấy bảng `Cell` `Renderer` thành phần. */
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(UngDungPhongChu.SMALL);
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

            /** Hàm `checkPopup`: kiểm tra batlen. */
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
        lblAllClinics.setFont(UngDungPhongChu.SMALL);
        lblAllClinics.setForeground(new Color(145, 35, 35));
        lblAllClinics.setBorder(new EmptyBorder(0, 6, 0, 4));

        JLabel btnDrop = new JLabel("▾", SwingConstants.CENTER);
        btnDrop.setFont(UngDungPhongChu.SMALL);
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
        medicalRecordTabPanel = new YTeRecordTheManHinh(services);
        clinicalTabs.addTab("Bệnh Án", medicalRecordTabPanel);
        clinicalTabs.addTab("Công Khám(1)", createPlaceholderTab("Dịch vụ công khám chuyên khoa"));
        labOrderTabPanel = new XetNghiemYeuCauTheManHinh(() -> cardLayout.show(cardContainer, "SERVICE_ORDER"), services);
        clinicalTabs.addTab("Xét Nghiệm(3)", labOrderTabPanel);
        clinicalTabs.addTab("CDHA(2)", createPlaceholderTab("Chẩn đoán hình ảnh: Siêu âm, X-Quang"));
        clinicalTabs.addTab("Chuyên khoa(1)", createPlaceholderTab("Khám chuyên khoa mắt, tai mũi họng"));
        prescriptionTabPanel = new DonThuocYeuCauTheManHinh(services);
        clinicalTabs.addTab("Thuốc(1)", prescriptionTabPanel);
        suppliesTabPanel = new VatTuYeuCauTheManHinh();
        clinicalTabs.addTab("Vật Tư(1)", suppliesTabPanel);
        treatmentResolutionTabPanel = new DieuTriXuTriTheManHinh(services);
        clinicalTabs.addTab("Xử Trí", treatmentResolutionTabPanel);
        clinicalTabs.addTab("BA Điện Tử(2)", createPlaceholderTab("Hồ sơ Bệnh Án Điện Tử (EMR Version 2.0)"));

        rightPanel.add(clinicalTabs, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);

        mainExamView.add(topToolbar, BorderLayout.NORTH);
        mainExamView.add(splitPane, BorderLayout.CENTER);

        // --- MÀN HÌNH 2: CHỈ ĐỊNH VẬT TƯ (SUPPLIES_ORDER) ---
        // Chuyển hướng trực tiếp trong cùng khung làm việc, khi Lưu + Đóng tự quay về tab Vật Tư(1)
        suppliesOrderPanel = new YTeVatTuYeuCauManHinh(() -> {
            cardLayout.show(cardContainer, "EXAM_MAIN");
            clinicalTabs.setSelectedComponent(suppliesTabPanel);
        });
        suppliesOrderPanel.setOnSuppliesSavedCallback(supplies -> {
            if (suppliesTabPanel != null) {
                suppliesTabPanel.setSupplies(supplies);
            }
        });

        // --- MÀN HÌNH 3: CHỈ ĐỊNH THUỐC (PRESCRIPTION_ORDER) ---
        // Chuyển hướng trực tiếp trong cùng khung làm việc, khi Lưu + Đóng tự quay về tab Thuốc(1)
        prescriptionOrderPanel = new DonThuocYeuCauManHinh(() -> {
            cardLayout.show(cardContainer, "EXAM_MAIN");
            if (!patientList.isEmpty()) selectPatientByIndex(0);
            clinicalTabs.setSelectedComponent(prescriptionTabPanel);
        }, services);

        // --- MÀN HÌNH 4: CHỈ ĐỊNH DỊCH VỤ (SERVICE_ORDER) ---
        // Chuyển hướng trực tiếp trong cùng khung làm việc khi bấm "Sửa Phiếu" từ tab Xét Nghiệm
        serviceOrderPanel = new DichVuYeuCauManHinh(() -> {
            cardLayout.show(cardContainer, "EXAM_MAIN");
            if (!patientList.isEmpty()) selectPatientByIndex(0);
            clinicalTabs.setSelectedComponent(labOrderTabPanel);
        }, services);

        cardContainer.add(mainExamView, "EXAM_MAIN");
        cardContainer.add(suppliesOrderPanel, "SUPPLIES_ORDER");
        cardContainer.add(prescriptionOrderPanel, "PRESCRIPTION_ORDER");
        cardContainer.add(serviceOrderPanel, "SERVICE_ORDER");

        // Đăng ký phím tắt Ctrl+T (hoặc Cmd+T trên Mac) chuyển nhanh đến Chỉ Định Thuốc
        int shortcutMask = resolveShortcutMask(
                GraphicsEnvironment.isHeadless(),
                () -> Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        KeyStroke ctrlT = KeyStroke.getKeyStroke('T', shortcutMask);
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ctrlT, "openPrescriptionOrder");
        this.getActionMap().put("openPrescriptionOrder", new AbstractAction() {
            /** Hàm `actionPerformed`: hành động `Performed`. */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cardLayout.show(cardContainer, "PRESCRIPTION_ORDER");
            }
        });

        add(cardContainer, BorderLayout.CENTER);
    }

    /** Hàm `resolveShortcutMask`: `resolve` phimtat che. */
    static int resolveShortcutMask(boolean headless, java.util.function.IntSupplier platformMaskSupplier) {
        if (headless) {
            return java.awt.event.InputEvent.CTRL_DOWN_MASK;
        }
        try {
            return platformMaskSupplier.getAsInt();
        } catch (RuntimeException unavailablePlatformMask) {
            return java.awt.event.InputEvent.CTRL_DOWN_MASK;
        }
    }

    /** Hàm `buildPatientFormPanel`: tạo bệnh nhân bieumau màn hình. */
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
        lblMaBN.setFont(UngDungPhongChu.BODY_BOLD);
        lblMaBN.setForeground(new Color(18, 45, 135));

        lblMaBA = createValueLabel("23XXB004424", false);
        lblMaBA.setFont(UngDungPhongChu.BODY_BOLD);
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

    /** Hàm `buildHistoryTablePanel`: tạo tiensu bảng màn hình. */
    private JPanel buildHistoryTablePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);

        String[] cols = {"", "Ngày Vào", "Hình Thức Vào", "Khoa", "Phòng", "Thẻ BHYT", "Ngày Sinh", "Ngày Ra", "Xử Trí"};
        historyModel = new DefaultTableModel(cols, 0) {
            /** Hàm `isCellEditable`: là `Cell` `Editable`. */
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        // Bảng tự vẽ các dòng kẻ lưới mờ trên toàn vùng trắng bên dưới (giống hệt DataGridView trong ảnh)
        historyTable = new JTable(historyModel) {
            /** Hàm `paintComponent`: `paint` thành phần. */
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

            /** Hàm `getScrollableTracksViewportHeight`: lấy `Scrollable` `Tracks` `Viewport` `Height`. */
            @Override
            public boolean getScrollableTracksViewportHeight() {
                return getPreferredSize().height < getParent().getHeight();
            }
        };

        historyTable.setFont(UngDungPhongChu.SMALL);
        historyTable.setRowHeight(24);
        historyTable.setShowGrid(true);
        historyTable.setGridColor(new Color(235, 239, 244));
        historyTable.setBackground(Color.WHITE);
        historyTable.setSelectionBackground(new Color(215, 232, 250));
        historyTable.setSelectionForeground(Color.BLACK);

        JTableHeader header = historyTable.getTableHeader();
        header.setPreferredSize(new Dimension(100, 25));
        header.setFont(UngDungPhongChu.SMALL_BOLD);
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
            /** Hàm `getTableCellRendererComponent`: lấy bảng `Cell` `Renderer` thành phần. */
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                return new OrangeDotComponent(isSelected);
            }
        });

        // Cột 3 (Khoa): Hộp tích vuông + ". Khám Bệnh"
        historyTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            /** Hàm `getTableCellRendererComponent`: lấy bảng `Cell` `Renderer` thành phần. */
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setIcon(new CheckBoxMiniIcon());
                lbl.setText(". " + (value != null ? value.toString() : ""));
                lbl.setFont(UngDungPhongChu.SMALL);
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

    /** Hàm `selectPatient`: chọn bệnh nhân. */
    private void selectPatient(AdminPatientItem p) {
        if (p == null) return;

        boolean isBhyt = p.requestType != null && (p.requestType.contains("BHYT") || p.requestType.contains("hiểm"));
        lblBannerLine1.setText(p.name + " | " + p.code + " | " + p.birthYear + " (" + p.bannerAge + ") | " + p.gender);
        lblBannerLine2.setText("VP: " + p.invoiceCode + " | " + p.requestType);
        lblBannerLine3.setText(p.bannerAddress + " | " + p.phone);
        lblBannerReqTag.setText(isBhyt
                ? "<html><center><font color='#800000'><b>BHYT</b></font><br><font color='#0044CC'><b>80%</b></font></center></html>"
                : "<html><center><font color='#800000'><b>Viện<br>phí</b></font></center></html>");

        lblMaBN.setText(p.code);
        lblMaBA.setText(p.medicalRecordCode);
        lblTenBN.setText(p.name);
        lblGioiTinh.setText(p.gender);
        lblNgaySinh.setText(p.birthDate);
        lblTuoi.setText(p.formAge);
        lblDanToc.setText("Kinh");
        lblNgheNghiep.setText("Nhân dân");
        lblQuocTich.setText("Việt Nam");
        lblDienThoai.setText(p.phone);
        lblDiaChi.setText(p.address);
        lblDonTiepLuc.setText(p.admissionDate);
        lblLoaiDonTiep.setText("Khám bệnh");
        lblDoiTuong.setText(p.requestType);
        lblSoVaoKhoa.setText("KKB/" + p.code);
        lblMaDieuTri.setText(p.invoiceCode);
        lblLyDoKham.setText(p.diagnosis != null ? p.diagnosis : "Khám bệnh");
        lblKhoaPhong.setText(p.department);
        lblBacSiKham.setText(p.doctor);
        lblBenhAn.setText("Khám đa khoa");
        lblNgayVao.setText(p.admissionDate);
        lblChanDoan.setText(p.diagnosis);

        if (treatmentResolutionTabPanel != null) {
            treatmentResolutionTabPanel.setPatientInfo(p.code, p.name, p.formAge, p.gender, p.address, p.doctor != null ? p.doctor : "BS. Tiến");
        }

        if (historyModel != null) {
            historyModel.setRowCount(0);
            historyModel.addRow(new Object[]{"", p.admissionDate, "Trực tiếp", "Khám Bệnh", p.department, isBhyt ? "DN401292929222" : "", p.birthDate, "", "Đang khám"});
        }

        int visitId = 1;
        try {
            if (p.invoiceCode != null && p.invoiceCode.startsWith("VP-")) {
                visitId = Integer.parseInt(p.invoiceCode.substring(3).trim());
            } else if (p.code != null && p.code.contains("-")) {
                String numStr = p.code.substring(p.code.lastIndexOf('-') + 1);
                visitId = Integer.parseInt(numStr);
            }
        } catch (Exception ignored) {}

        if (prescriptionOrderPanel != null) {
            prescriptionOrderPanel.setVisitContext(visitId, 1, 1);
        }
        if (prescriptionTabPanel != null) {
            prescriptionTabPanel.setVisitContext(visitId, 1);
        }
        if (serviceOrderPanel != null) {
            serviceOrderPanel.setVisitContext(visitId, 1, 1);
        }
        if (labOrderTabPanel != null) {
            labOrderTabPanel.loadServicesForVisit(visitId);
        }
        if (treatmentResolutionTabPanel != null) {
            treatmentResolutionTabPanel.setVisitContext(visitId, 1);
        }
        if (medicalRecordTabPanel != null) {
            medicalRecordTabPanel.setVisitContext(visitId, 1);
        }
        if (suppliesTabPanel != null) {
            suppliesTabPanel.setPatient(p);
        }
        if (suppliesOrderPanel != null) {
            suppliesOrderPanel.setPatient(p);
        }
    }

    /** Hàm `selectPatientByIndex`: chọn bệnh nhân theo `Index`. */
    public void selectPatientByIndex(int index) {
        if (index >= 0 && index < patientList.size()) {
            if (queueTable != null) {
                queueTable.setRowSelectionInterval(index, index);
            }
            selectPatient(patientList.get(index));
        }
    }

    /** Hàm `selectPatientByName`: chọn bệnh nhân theo tên. */
    public void selectPatientByName(String name) {
        if (name == null) return;
        for (int i = 0; i < patientList.size(); i++) {
            if (name.equalsIgnoreCase(patientList.get(i).name)) {
                selectPatientByIndex(i);
                return;
            }
        }
    }

    /** Hàm `showQueueContextMenu`: `show` hangdoi ngữ cảnh menu. */
    public void showQueueContextMenu(Component comp, int x, int y) {
        int row = queueTable.getSelectedRow();
        AdminPatientItem patient = (row >= 0 && row < patientList.size()) ? patientList.get(row) : null;
        String name = patient != null ? patient.name : "Bệnh nhân";
        String code = patient != null ? patient.code : "";
        boolean isExamined = patient != null && !patient.hasOrangeDot;

        BenhNhanHangDoiNguCanhMenu menu = new BenhNhanHangDoiNguCanhMenu(name, code, isExamined, action -> {
            handlePatientQueueAction(action, patient);
        });
        menu.show(comp, x, y);
    }

    /** Hàm `handlePatientQueueAction`: `handle` bệnh nhân hangdoi hành động. */
    private void handlePatientQueueAction(String action, AdminPatientItem patient) {
        String name = patient != null ? patient.name : "Bệnh nhân";
        String code = patient != null ? patient.code : "";

        if ("Bệnh Án".equalsIgnoreCase(action)) {
            selectMedicalRecordTab();
        } else if ("Mở Lại Bệnh Án".equalsIgnoreCase(action)) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            ReopenYTeRecordHopThoai dlg = new ReopenYTeRecordHopThoai(owner, patient, req -> {
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
            boolean confirmed = TransferPhongConfirmHopThoai.showConfirm(this);
            if (confirmed) {
                // Bước 2: Hộp thoại chọn phòng khám đích (Khớp media_1793381550077_6c743cbd.jpg)
                String chosenRoom = TransferPhongSelectHopThoai.selectRoom(this);
                if (chosenRoom != null && !chosenRoom.trim().isEmpty()) {
                    // Bước 3: Xem trước và In Phiếu Khám Bệnh / Phiếu Chuyển Phòng Khám (Khớp media_1793381633910_5fdd704b.jpg)
                    Window owner = SwingUtilities.getWindowAncestor(this);
                    TransferPhieuPrintPreviewHopThoai printDlg = new TransferPhieuPrintPreviewHopThoai(owner, name, code, chosenRoom);
                    printDlg.setVisible(true);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Đang thực hiện chức năng: " + action + "\n▶ Bệnh nhân: " + name + " (" + code + ")",
                    "Thao Tác Bệnh Nhân", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Hàm `getLabOrderTabPanel`: lấy xetnghiem yêu cầu the màn hình. */
    public XetNghiemYeuCauTheManHinh getLabOrderTabPanel() {
        return labOrderTabPanel;
    }

    /** Hàm `getSuppliesTabPanel`: lấy vattu the màn hình. */
    public VatTuYeuCauTheManHinh getSuppliesTabPanel() {
        return suppliesTabPanel;
    }

    /** Hàm `getPrescriptionTabPanel`: lấy đơn thuốc the màn hình. */
    public DonThuocYeuCauTheManHinh getPrescriptionTabPanel() {
        return prescriptionTabPanel;
    }

    /** Hàm `getTreatmentResolutionTabPanel`: lấy dieutri xutri the màn hình. */
    public DieuTriXuTriTheManHinh getTreatmentResolutionTabPanel() {
        return treatmentResolutionTabPanel;
    }

    /** Hàm `getMedicalRecordTabPanel`: lấy yte ghi nhận the màn hình. */
    public YTeRecordTheManHinh getMedicalRecordTabPanel() {
        return medicalRecordTabPanel;
    }

    /** Hàm `selectMedicalRecordTab`: chọn yte ghi nhận the. */
    public void selectMedicalRecordTab() {
        if (clinicalTabs != null && medicalRecordTabPanel != null) {
            clinicalTabs.setSelectedComponent(medicalRecordTabPanel);
        }
    }

    /** Hàm `selectTreatmentTab`: chọn dieutri the. */
    public void selectTreatmentTab() {
        if (clinicalTabs != null && treatmentResolutionTabPanel != null) {
            clinicalTabs.setSelectedComponent(treatmentResolutionTabPanel);
        }
    }

    /** Hàm `getServiceOrderPanel`: lấy dịch vụ yêu cầu màn hình. */
    public DichVuYeuCauManHinh getServiceOrderPanel() {
        return serviceOrderPanel;
    }

    /** Hàm `getClinicalTabs`: lấy lamsang `Tabs`. */
    public JTabbedPane getClinicalTabs() {
        return clinicalTabs;
    }

    /** Hàm `createStyledTabbedPane`: tạo cokieu `Tabbed` `Pane`. */
    private JTabbedPane createStyledTabbedPane() {
        JTabbedPane tp = new JTabbedPane();
        tp.setFont(UngDungPhongChu.SMALL_BOLD);
        tp.setBackground(new Color(214, 228, 245));
        tp.setUI(new BasicTabbedPaneUI() {
            /** Hàm `installDefaults`: `install` macdinh. */
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(2, 4, 0, 4);
                contentBorderInsets = new Insets(1, 0, 0, 0);
                tabInsets = new Insets(4, 10, 4, 10);
            }

            /** Hàm `paintTabArea`: `paint` the `Area`. */
            @Override
            protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
                g.setColor(new Color(214, 228, 245));
                g.fillRect(0, 0, tabPane.getWidth(), calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight));
                super.paintTabArea(g, tabPlacement, selectedIndex);
            }

            /** Hàm `paintTabBackground`: `paint` the `Background`. */
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

            /** Hàm `paintTabBorder`: `paint` the `Border`. */
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

    /** Hàm `createFieldTitle`: tạo `Field` tiêu đề. */
    private JLabel createFieldTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UngDungPhongChu.SMALL);
        lbl.setForeground(new Color(25, 25, 25));
        return lbl;
    }

    /** Hàm `createValueLabel`: tạo giá trị nhãn. */
    private JLabel createValueLabel(String text, boolean isNavy) {
        JLabel lbl = new JLabel(text != null && !text.isEmpty() ? text : " ");
        lbl.setFont(UngDungPhongChu.SMALL);
        lbl.setForeground(isNavy ? new Color(18, 45, 130) : new Color(25, 25, 25));
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(175, 185, 200)));
        return lbl;
    }

    /** Hàm `createToolbarButton`: tạo thanhcongcu nút. */
    private JButton createToolbarButton(String text, String iconType, boolean hasDropdownArrow) {
        String labelText = hasDropdownArrow ? text + " ▾" : text;
        JButton btn = new JButton(labelText, new SubToolbarVectorIcon(iconType));
        btn.setFont(UngDungPhongChu.SMALL);
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
            KhamBenhMenuBatLenMenu menuPopup = new KhamBenhMenuBatLenMenu(btn, actionTitle -> {
                handleMenuAction(actionTitle);
            });
            btn.addActionListener(e -> {
                if (btn.isShowing()) {
                    menuPopup.show(btn, 0, btn.getHeight());
                }
            });
        } else if ("In".equalsIgnoreCase(text)) {
            PrintBaoCaoBatLenMenu printMenu = new PrintBaoCaoBatLenMenu(btn, reportTitle -> {
                if (reportTitle != null && reportTitle.contains("Đăng Ký Khám Bệnh, Chữa Bệnh")) {
                    YTeVienPhiPhieuHopThoai dialog = new YTeVienPhiPhieuHopThoai(SwingUtilities.getWindowAncestor(this));
                    dialog.setVisible(true);
                } else if (reportTitle != null && reportTitle.contains("Bệnh Án Khám Bệnh")) {
                    YTeRecordPrintPreviewHopThoai dialog = new YTeRecordPrintPreviewHopThoai(SwingUtilities.getWindowAncestor(this));
                    dialog.setVisible(true);
                } else {
                    if (!GraphicsEnvironment.isHeadless()) {
                        int r = queueTable != null ? queueTable.getSelectedRow() : -1;
                        AdminPatientItem curP = (r >= 0 && r < patientList.size()) ? patientList.get(r) : (!patientList.isEmpty() ? patientList.get(0) : null);
                        String pInfo = curP != null ? curP.name + " | " + curP.code + " (" + curP.requestType + ")" : "Bệnh nhân hiện tại";
                        String pDoc = curP != null && curP.doctor != null ? curP.doctor : "BS. Nguyễn Văn Tiến";
                        String pDept = curP != null && curP.department != null ? curP.department : "Khoa Khám Bệnh > PK Nhi 1";
                        JOptionPane.showMessageDialog(this,
                                "Hệ thống đang chuẩn bị xuất bản in:\n" +
                                "▶ " + reportTitle + "\n\n" +
                                "Bệnh nhân: " + pInfo + "\n" +
                                "Bác sĩ chỉ định: " + pDoc + "\n" +
                                "Khoa phòng: " + pDept,
                                "Xuất & In Phiếu Khám Bệnh EHC", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });
            btn.addActionListener(e -> printMenu.show(btn, 0, btn.getHeight()));
        } else if ("Thuốc/VT".equalsIgnoreCase(text)) {
            ThuocVatTuBatLenMenu medMenu = new ThuocVatTuBatLenMenu(btn, actionTitle -> {
                if (actionTitle != null && actionTitle.contains("Chỉ Định Vật Tư")) {
                    // Chuyển hướng trực tiếp trong cùng màn hình, không mở form dialog riêng biệt
                    cardLayout.show(cardContainer, "SUPPLIES_ORDER");
                } else if (actionTitle != null && actionTitle.contains("Chỉ Định Thuốc")) {
                    // Chuyển hướng trực tiếp đến màn hình Chỉ Định Thuốc trong cùng workspace
                    cardLayout.show(cardContainer, "PRESCRIPTION_ORDER");
                } else {
                    int r = queueTable != null ? queueTable.getSelectedRow() : -1;
                    AdminPatientItem curP = (r >= 0 && r < patientList.size()) ? patientList.get(r) : (!patientList.isEmpty() ? patientList.get(0) : null);
                    String pInfo = curP != null ? curP.name + " | " + curP.code + " (" + curP.requestType + ")" : "Bệnh nhân hiện tại";
                    String pDoc = curP != null && curP.doctor != null ? curP.doctor : "BS. Nguyễn Văn Tiến";
                    String pDept = curP != null && curP.department != null ? curP.department : "Khoa Khám Bệnh > PK Nhi 1";
                    JOptionPane.showMessageDialog(this,
                            "Hệ thống đang mở phân hệ:\n" +
                            "▶ " + actionTitle + "\n\n" +
                            "Bệnh nhân: " + pInfo + "\n" +
                            "Bác sĩ chỉ định: " + pDoc + "\n" +
                            "Khoa phòng: " + pDept,
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

    /** Hàm `createPlaceholderTab`: tạo `Placeholder` the. */
    private JPanel createPlaceholderTab(String desc) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        JLabel lbl = new JLabel(desc, SwingConstants.CENTER);
        lbl.setFont(UngDungPhongChu.BODY);
        lbl.setForeground(Color.GRAY);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    /** Hàm `handleMenuAction`: `handle` menu hành động. */
    private void handleMenuAction(String actionTitle) {
        if ("Đóng".equalsIgnoreCase(actionTitle)) {
            return;
        }
        if ("Chuyển Loại Bệnh Án".equalsIgnoreCase(actionTitle)) {
            Window win = SwingUtilities.getWindowAncestor(this);
            YTeRecordLoaiHopThoai dialog = new YTeRecordLoaiHopThoai(win, selectedType -> {
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

    /** Hàm `onShow`: tải `Show`. Gốc khai báo tại `vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh#onShow`. */
    @Override
    public void onShow() {
        refreshData();
    }

    /** Hàm `refreshData`: làm mới dữ liệu. Gốc khai báo tại `vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh#refreshData`. */
    @Override
    public void refreshData() {}

    // =========================================================================
    // CUSTOM CELL RENDERERS & VECTOR ICONS (ĐẢM BẢO HIỂN THỊ 1:1 TRÊN MỌI OS)
    // =========================================================================

    /** Cột 0 của bảng hàng đợi: Vẽ chấm cam / tick xanh + Số thứ tự căn giữa */
    private static class SttCellRenderer extends JComponent implements javax.swing.table.TableCellRenderer {
        /** Trường `item`: mathang. */
        private AdminPatientItem item;
        /** Trường `selected`: duocchon. */
        private boolean selected;

        /** Hàm `getTableCellRendererComponent`: lấy bảng `Cell` `Renderer` thành phần. Gốc khai báo tại `JComponent#getTableCellRendererComponent`. */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.item = value instanceof AdminPatientItem ? (AdminPatientItem) value : null;
            this.selected = isSelected;
            return this;
        }

        /** Hàm `paintComponent`: `paint` thành phần. Gốc khai báo tại `JComponent#paintComponent`. */
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

                g2.setFont(UngDungPhongChu.SMALL);
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
        /** Trường `item`: mathang. */
        private AdminPatientItem item;
        /** Trường `selected`: duocchon. */
        private boolean selected;

        /** Hàm `getTableCellRendererComponent`: lấy bảng `Cell` `Renderer` thành phần. Gốc khai báo tại `JComponent#getTableCellRendererComponent`. */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.item = value instanceof AdminPatientItem ? (AdminPatientItem) value : null;
            this.selected = isSelected;
            return this;
        }

        /** Hàm `paintComponent`: `paint` thành phần. Gốc khai báo tại `JComponent#paintComponent`. */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(selected ? new Color(0, 114, 198) : Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (item != null) {
                g2.setFont(UngDungPhongChu.SMALL_BOLD);
                g2.setColor(selected ? Color.WHITE : new Color(15, 15, 15));
                g2.drawString(item.name, 4, 18);

                g2.setFont(UngDungPhongChu.SMALL);
                g2.setColor(selected ? new Color(235, 245, 255) : new Color(45, 45, 45));
                g2.drawString(item.code + " - " + item.requestType, 4, 34);
            }
            g2.dispose();
        }
    }

    /** Chấm tròn cam ở cột 0 của bảng Lịch sử khám */
    private static class OrangeDotComponent extends JComponent {
        /** Trường `selected`: duocchon. */
        private final boolean selected;
        OrangeDotComponent(boolean selected) { this.selected = selected; }

        /** Hàm `paintComponent`: `paint` thành phần. Gốc khai báo tại `JComponent#paintComponent`. */
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
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 12; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
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
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 12; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
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
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
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
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 12; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
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
        /** Trường `type`: loai. */
        private final String type;
        SubToolbarVectorIcon(String type) { this.type = type; }

        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 26; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
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
