package vn.clinicmanagement.desktop.modules.reception.components;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.reception.RegisterPatientCommand;
import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.facility.PhongKham;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Khung nhập liệu thông tin bệnh nhân và thông tin thẻ BHYT.
 * Khớp 100% chi tiết trong ảnh chụp mẫu.
 */
public class PatientRegistrationFormPanel extends JPanel {

    public static final String[] CLINICAL_SERVICES = {
            "Khám Nội",
            "Khám Nhi",
            "Khám Ngoại",
            "Khám Mắt",
            "Khám Răng",
            "Khám Phụ sản",
            "Khám Tai mũi họng",
            "Khám Răng hàm mặt",
            "Khám Ổ đồng",
            "Hội chẩn ca bệnh khó chuyên ngành Nhi",
            "Khám bệnh theo yêu cầu mời bác sỹ tuyến tỉnh [YC]",
            "Khám bệnh theo yêu cầu mời BS tuyến TW [YC]",
            "Khám tâm thần"
    };

    public static final String[] CLINIC_ROOMS = {
            "Cấp cứu nhi",
            "Cấp cứu sản",
            "Phòng Tiêm Chủng",
            "PK cách ly",
            "PK ĐT ngoại trú tâm thần",
            "PK Nhi 1",
            "PK Nhi 2",
            "PK Nhi 3",
            "PK Nhi 4",
            "PK Dinh dưỡng",
            "PK Tai Mũi Họng",
            "PK theo yêu cầu nhi số 1",
            "PK theo yêu cầu nhi số 2",
            "PK theo yêu cầu Sản - Phụ khoa",
            "PK sàng lọc chẩn đoán trước sinh và sơ sinh",
            "PHÒNG QUẢN LÝ HS",
            "PK Phụ nội"
    };

    private static final String[] ETHNICITIES = {"Kinh", "Tày", "Thái", "Mường", "Khác"};
    private static final String[] NATIONALITIES = {"Việt nam", "Khác"};
    private static final String[] INSURANCE_RESIDENCE_CODES = {"", "K1", "K2", "K3"};

    private MedicalPopupChooser chooserService;
    private MedicalPopupChooser chooserClinicRoom;
    private final List<PhongKham> clinicRooms = new ArrayList<>();
    private Integer patientId;
    private BiConsumer<String, String> onSearchListener;

    private JComboBox<String> cbDoiTuong;
    private JCheckBox chkUuTien;
    private JTextField txtPhoneSearch;
    private JTextField txtCccdSearch;
    private JTextField txtMaBn;
    private JTextField txtHoTen;
    private JComboBox<String> cbGioiTinh;
    private JTextField txtDay;
    private JTextField txtMonth;
    private JTextField txtYear;
    private JTextField txtNoiSinh;
    private JTextField txtSoNhaThonPho;
    private JTextField txtAdministrativeCode;
    private JLabel lblAddress;
    private JTextField txtJob;
    private JTextField txtPhone;
    private JTextField txtEmail;
    private JComboBox<String> cbDanToc;
    private JComboBox<String> cbQuocTich;
    private JTextField txtCccd;
    private JTextField txtCccdD;
    private JTextField txtCccdM;
    private JTextField txtCccdY;
    private JTextField txtNoiCap;
    private JTextField txtWork;
    private JComboBox<String> cbDkKham;
    private JTextField txtTime;
    private JTextField txtReason;
    private JCheckBox chkCoGiayKhaiSinh;
    private JCheckBox chkKhongGiuThe;
    private final JTextField[] insuranceNumberFields = new JTextField[4];
    private JComboBox<String> cbNoiSong;
    private JTextField txtInsuranceFacilityCode;
    private JTextField txtInsuranceFacilityName;
    private JTextField txtInsuranceStartDate;
    private JTextField txtInsuranceEndDate;
    private JTextField txtDiaChiThe;
    private JComboBox<String> cbTuyen;
    private JLabel lblRate;
    private JCheckBox chkKhongCungChiTra;
    private final JTextField[] insuranceExemptionDateFields = new JTextField[3];
    private final JTextField[] insuranceFiveYearDateFields = new JTextField[3];

    public PatientRegistrationFormPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 215, 226)));

        // --- 1. THANH TÌM KIẾM NHANH TRÊN CÙNG ---
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        searchBar.setBackground(new Color(248, 250, 252));
        searchBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 232, 240)));
        searchBar.setPreferredSize(new Dimension(1000, 32));
        searchBar.setMinimumSize(new Dimension(0, 32));
        searchBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lblDoiTuong = new JLabel("Đối tượng:");
        lblDoiTuong.setFont(AppFonts.BODY_BOLD);
        cbDoiTuong = new JComboBox<>(new String[]{"Viện phí", "BHYT", "Dịch vụ"});
        cbDoiTuong.setFont(AppFonts.BODY);
        cbDoiTuong.setPreferredSize(new Dimension(95, 20));

        chkUuTien = new JCheckBox("ĐN vn tiền");
        chkUuTien.setFont(AppFonts.SMALL_BOLD);
        chkUuTien.setForeground(new Color(205, 35, 35)); // Màu đỏ chuẩn
        chkUuTien.setOpaque(false);

        JLabel lblTimKiem = new JLabel("Tìm kiếm :");
        lblTimKiem.setFont(AppFonts.BODY_BOLD);
        lblTimKiem.setForeground(new Color(125, 25, 125)); // Tím đậm chuẩn EHC

        JLabel lblPhone = new JLabel("Số điện thoại:");
        lblPhone.setFont(AppFonts.BODY);
        txtPhoneSearch = createInput(80, 22);

        JLabel lblCccdSearch = new JLabel("Số CCCD:");
        lblCccdSearch.setFont(AppFonts.BODY);
        txtCccdSearch = createInput(110, 22);
        txtPhoneSearch.addActionListener(e -> fireSearch());
        txtCccdSearch.addActionListener(e -> fireSearch());

        searchBar.add(lblDoiTuong);
        searchBar.add(cbDoiTuong);
        searchBar.add(chkUuTien);
        searchBar.add(lblTimKiem);
        searchBar.add(lblPhone);
        searchBar.add(txtPhoneSearch);
        searchBar.add(lblCccdSearch);
        searchBar.add(txtCccdSearch);

        // --- 2. VÙNG FORM CHÍNH: CỘT TRÁI TÌM KIẾM/HÀNH CHÍNH, CỘT PHẢI BHYT ---
        JPanel formContainer = new JPanel(new BorderLayout());
        formContainer.setBackground(Color.WHITE);
        formContainer.setBorder(new EmptyBorder(0, 2, 0, 2));

        JPanel patientPanel = new JPanel(new BorderLayout());
        patientPanel.setBackground(Color.WHITE);
        patientPanel.add(searchBar, BorderLayout.NORTH);
        patientPanel.add(buildPatientDemographicsPanel(), BorderLayout.CENTER);
        JPanel insurancePanel = buildInsuranceCardPanel();
        insurancePanel.setOpaque(true);
        insurancePanel.setBackground(Color.WHITE);
        insurancePanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        patientPanel.setMinimumSize(new Dimension(0, 0));
        insurancePanel.setMinimumSize(new Dimension(0, 0));

        JSplitPane columns = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, patientPanel, insurancePanel);
        columns.setBorder(null);
        columns.setDividerSize(1);
        columns.setResizeWeight(0.58);
        columns.setBackground(new Color(215, 224, 235));
        columns.addComponentListener(new ComponentAdapter() {
            private boolean initialized;

            @Override
            public void componentResized(ComponentEvent e) {
                if (!initialized && columns.getWidth() > 0) {
                    columns.setDividerLocation(0.58);
                    initialized = true;
                }
            }
        });
        formContainer.add(columns, BorderLayout.CENTER);

        add(formContainer, BorderLayout.CENTER);
        txtTime.setText(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
    }

    /** Cột trái: Thông tin hành chính bệnh nhân */
    private JPanel buildPatientDemographicsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        // Dòng 1: Mã BN, Họ và tên
        JPanel r1 = createRowPanel();
        r1.add(createLabel("Mã BN:", 65));
        txtMaBn = createInput(100, 22);
        txtMaBn.setFont(AppFonts.BODY_BOLD);
        r1.add(txtMaBn);
        r1.add(createLabel("Họ và tên:"));
        txtHoTen = createInput(285, 22);
        txtHoTen.setFont(AppFonts.BODY_BOLD);
        r1.add(txtHoTen);
        p.add(r1);

        // Dòng 2: Giới tính, Ngày sinh (3 ô), 22 tuổi, Nơi sinh
        JPanel r2 = createRowPanel();
        r2.add(createLabel("Giới tính:", 65));
        cbGioiTinh = new JComboBox<>(new String[]{"Nữ", "Nam", "Khác"});
        cbGioiTinh.setFont(AppFonts.BODY);
        cbGioiTinh.setPreferredSize(new Dimension(100, 20));
        r2.add(cbGioiTinh);

        r2.add(createLabel("Ngày sinh:", 63));
        JPanel birthDateFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        birthDateFields.setOpaque(false);
        txtDay = createInput(26, 22); txtDay.setHorizontalAlignment(SwingConstants.CENTER);
        txtMonth = createInput(26, 22); txtMonth.setHorizontalAlignment(SwingConstants.CENTER);
        txtYear = createInput(44, 22); txtYear.setHorizontalAlignment(SwingConstants.CENTER);
        birthDateFields.add(txtDay);
        birthDateFields.add(txtMonth);
        birthDateFields.add(txtYear);
        r2.add(birthDateFields);
        r2.add(Box.createHorizontalStrut(52));

        JLabel lblAge = new JLabel("22 tuổi");
        lblAge.setFont(AppFonts.BODY_BOLD);
        lblAge.setForeground(new Color(0, 85, 204)); // Xanh lam chuẩn
        lblAge.setBorder(new EmptyBorder(0, 4, 0, 4));
        r2.add(lblAge);

        r2.add(Box.createHorizontalStrut(72));
        r2.add(createLabel("Nơi sinh:", 56));
        txtNoiSinh = createInput(216, 22);
        r2.add(txtNoiSinh);
        p.add(r2);

        // Dòng 3: SN/thôn/phố, Ty/Hộ/Xp, Địa chỉ gợi ý, Nghề nghiệp
        JPanel r3 = createRowPanel();
        r3.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 0));
        r3.add(createLabel("SN/thôn/phố:", 77));
        txtSoNhaThonPho = createInput(100, 22);
        r3.add(txtSoNhaThonPho);
        r3.add(createLabel("Ty/Hộ/Xp:", 61));
        txtAdministrativeCode = createInput(100, 22);
        r3.add(txtAdministrativeCode);

        lblAddress = createLabel("", 304);
        r3.add(lblAddress);

        r3.add(createLabel("Nghề nghiệp:"));
        txtJob = createInput(65, 22);
        r3.add(txtJob);
        r3.add(createBrowseButton());
        p.add(r3);

        // Dòng 4: Điện thoại, Email, Dân tộc, Quốc tịch
        JPanel r4 = createRowPanel();
        r4.add(createLabel("Điện thoại:", 65));
        txtPhone = createInput(100, 22);
        r4.add(txtPhone);
        r4.add(createLabel("Email:", 55));
        txtEmail = createInput(100, 22);
        r4.add(txtEmail);
        r4.add(Box.createHorizontalStrut(10));
        r4.add(createLabel("Dân tộc:", 50));
        cbDanToc = new JComboBox<>(ETHNICITIES);
        cbDanToc.setFont(AppFonts.BODY);
        cbDanToc.setPreferredSize(new Dimension(109, 20));
        r4.add(cbDanToc);
        r4.add(Box.createHorizontalStrut(3));
        r4.add(createLabel("Quốc tịch:", 61));
        cbQuocTich = new JComboBox<>(NATIONALITIES);
        cbQuocTich.setFont(AppFonts.BODY);
        cbQuocTich.setPreferredSize(new Dimension(212, 20));
        r4.add(cbQuocTich);
        p.add(r4);

        // Dòng 5: CMND/CCCD, Ngày cấp, Nơi cấp
        JPanel r5 = createRowPanel();
        r5.add(createLabel("CMND/CCCD:", 83));
        txtCccd = createInput(100, 22);
        r5.add(txtCccd);
        r5.add(createLabel("Ngày cấp:", 60));
        JPanel issueDateFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        issueDateFields.setOpaque(false);
        txtCccdD = createInput(26, 22); txtCccdD.setHorizontalAlignment(SwingConstants.CENTER);
        txtCccdM = createInput(26, 22); txtCccdM.setHorizontalAlignment(SwingConstants.CENTER);
        txtCccdY = createInput(44, 22); txtCccdY.setHorizontalAlignment(SwingConstants.CENTER);
        issueDateFields.add(txtCccdD);
        issueDateFields.add(txtCccdM);
        issueDateFields.add(txtCccdY);
        r5.add(issueDateFields);
        r5.add(Box.createHorizontalStrut(11));
        r5.add(createLabel("Nơi cấp:", 51));
        txtNoiCap = createInput(374, 22);
        r5.add(txtNoiCap);
        p.add(r5);

        // Dòng 6: TT khác, Nơi làm việc
        JPanel r6 = createRowPanel();
        JButton btnTtKhac = new JButton("TT khác");
        btnTtKhac.setFont(AppFonts.SMALL);
        btnTtKhac.setPreferredSize(new Dimension(74, 22));
        btnTtKhac.setMargin(new Insets(1, 4, 1, 4));
        r6.add(btnTtKhac);
        r6.add(Box.createHorizontalStrut(372));
        r6.add(createLabel("Nơi làm việc:", 78));
        txtWork = createInput(267, 22);
        r6.add(txtWork);
        p.add(r6);

        // Dòng 7: ĐK khám, Lúc, Lý do khám
        JPanel r7 = createRowPanel();
        r7.add(createLabel("ĐK khám:", 65));
        cbDkKham = new JComboBox<>(new String[]{"Khám bệnh", "Tái khám", "Cấp cứu"});
        cbDkKham.setFont(AppFonts.BODY);
        cbDkKham.setPreferredSize(new Dimension(100, 20));
        r7.add(cbDkKham);
        r7.add(createLabel("Lúc:", 29));
        txtTime = createInput(126, 22);
        r7.add(txtTime);
        r7.add(Box.createHorizontalStrut(6));
        r7.add(createLabel("Lý do khám:", 73));
        txtReason = createInput(109, 22);
        r7.add(txtReason);
        p.add(r7);

        // Dòng 8: Dịch vụ, Phòng khám (Trạng thái sau khi chọn dịch vụ xong: Khám Nhi & Phòng Tiêm Chủng vàng)
        JPanel r8 = createRowPanel();
        r8.add(createLabel("Dịch vụ:", 65));
        chooserService = new MedicalPopupChooser(CLINICAL_SERVICES, "Khám Nhi", 258, 20);
        r8.add(chooserService);

        r8.add(createLabel("Phòng khám:", 78));
        chooserClinicRoom = new MedicalPopupChooser(CLINIC_ROOMS, "Phòng Tiêm Chủng", 395, 20);
        chooserClinicRoom.setHighlighted(true); // Highlight nền vàng sau khi chọn dịch vụ xong khớp 100% ảnh chụp
        r8.add(chooserClinicRoom);

        // Sau khi chọn Dịch Vụ: Chuyển focus sang Phòng Khám và bôi vàng
        chooserService.setOnSelectListener(svc -> {
            chooserClinicRoom.requestFocusAndHighlight();
            if (onServiceListener != null) {
                onServiceListener.accept(svc);
            }
        });

        p.add(r8);

        return p;
    }

    private java.util.function.Consumer<String> onServiceListener;

    public void setOnClinicRoomChanged(java.util.function.Consumer<String> listener) {
        if (chooserClinicRoom != null) {
            chooserClinicRoom.setOnSelectListener(listener);
        }
    }

    public String enteredCccd() {
        return txtCccd != null ? txtCccd.getText().trim() : "";
    }

    public void prepareDuplicateCccdSearch() {
        if (txtCccd != null && txtCccdSearch != null) {
            txtCccdSearch.setText(txtCccd.getText().trim());
        }
    }

    public void setOnServiceChanged(java.util.function.Consumer<String> listener) {
        this.onServiceListener = listener;
    }

    /** Cột phải: Thông tin thẻ BHYT */
    private JPanel buildInsuranceCardPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        // Tiêu đề & 2 Checkbox đỏ
        JPanel rHead = createInsuranceRowPanel();
        JLabel lblBhyt = new JLabel("Thông tin thẻ BHYT");
        lblBhyt.setFont(AppFonts.BODY_BOLD);
        rHead.add(lblBhyt);

        chkCoGiayKhaiSinh = new JCheckBox("TE có giấy khai sinh");
        chkCoGiayKhaiSinh.setFont(AppFonts.SMALL);
        chkCoGiayKhaiSinh.setForeground(new Color(205, 30, 30));
        chkCoGiayKhaiSinh.setOpaque(false);
        chkCoGiayKhaiSinh.setPreferredSize(new Dimension(chkCoGiayKhaiSinh.getPreferredSize().width, 20));
        rHead.add(chkCoGiayKhaiSinh);

        chkKhongGiuThe = new JCheckBox("Không giữ thẻ BHYT");
        chkKhongGiuThe.setFont(AppFonts.SMALL);
        chkKhongGiuThe.setForeground(new Color(205, 30, 30));
        chkKhongGiuThe.setOpaque(false);
        chkKhongGiuThe.setPreferredSize(new Dimension(chkKhongGiuThe.getPreferredSize().width, 20));
        rHead.add(chkKhongGiuThe);
        p.add(rHead);

        // Dòng 1: Số BHYT (4 ô phân đoạn), Nơi sống
        JPanel r1 = createInsuranceRowPanel();
        r1.add(createLabel("Số BHYT:", 64));
        JPanel insuranceNumberFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        insuranceNumberFields.setOpaque(false);
        this.insuranceNumberFields[0] = createInput(35, 22);
        this.insuranceNumberFields[1] = createInput(35, 22);
        this.insuranceNumberFields[2] = createInput(35, 22);
        this.insuranceNumberFields[3] = createInput(115, 22);
        insuranceNumberFields.add(this.insuranceNumberFields[0]);
        insuranceNumberFields.add(this.insuranceNumberFields[1]);
        insuranceNumberFields.add(this.insuranceNumberFields[2]);
        insuranceNumberFields.add(this.insuranceNumberFields[3]);
        r1.add(insuranceNumberFields);
        r1.add(createLabel("Nơi sống:", 58));
        cbNoiSong = new JComboBox<>(INSURANCE_RESIDENCE_CODES);
        cbNoiSong.setFont(AppFonts.BODY);
        cbNoiSong.setPreferredSize(new Dimension(50, 20));
        r1.add(cbNoiSong);
        p.add(r1);

        // Dòng 2: KCBBĐ
        JPanel r2 = createInsuranceRowPanel();
        r2.add(createLabel("KCBBĐ:", 64));
        txtInsuranceFacilityCode = createInput(65, 22);
        txtInsuranceFacilityName = createInput(270, 22);
        r2.add(txtInsuranceFacilityCode);
        r2.add(txtInsuranceFacilityName);
        p.add(r2);

        // Dòng 3: Hạn thẻ
        JPanel r3 = createInsuranceRowPanel();
        r3.add(createLabel("Hạn thẻ:", 64));
        txtInsuranceStartDate = createInput(100, 22);
        r3.add(txtInsuranceStartDate);
        r3.add(new JLabel(" - "));
        txtInsuranceEndDate = createInput(100, 22);
        r3.add(txtInsuranceEndDate);
        p.add(r3);

        // Dòng 4: Địa chỉ thẻ
        JPanel r4 = createInsuranceRowPanel();
        r4.add(createLabel("Địa chỉ thẻ:", 66));
        txtDiaChiThe = createInput(335, 22);
        r4.add(txtDiaChiThe);
        p.add(r4);

        // Dòng 5: Tuyến KCB
        JPanel r5 = createInsuranceRowPanel();
        r5.add(createLabel("Tuyến KCB:", 71));
        cbTuyen = new JComboBox<>(new String[]{"", "Đúng tuyến", "Trái tuyến"});
        cbTuyen.setFont(AppFonts.BODY);
        cbTuyen.setPreferredSize(new Dimension(224, 20));
        r5.add(cbTuyen);
        lblRate = new JLabel("0%");
        lblRate.setFont(AppFonts.BODY_BOLD);
        lblRate.setForeground(new Color(0, 95, 205));
        lblRate.setBorder(new EmptyBorder(0, 4, 0, 0));
        r5.add(lblRate);
        p.add(r5);

        // Dòng 6: Giấy chứng nhận không cùng chi trả
        JPanel r6 = createInsuranceRowPanel();
        chkKhongCungChiTra = new JCheckBox("Giấy chứng nhận không cùng chi trả trong năm");
        chkKhongCungChiTra.setFont(AppFonts.SMALL);
        chkKhongCungChiTra.setForeground(new Color(0, 80, 185)); // Màu xanh lam chuẩn
        chkKhongCungChiTra.setOpaque(false);
        chkKhongCungChiTra.setPreferredSize(new Dimension(chkKhongCungChiTra.getPreferredSize().width, 20));
        r6.add(chkKhongCungChiTra);
        p.add(r6);

        // Dòng 7: Ngày miễn cùng chi trả
        JPanel r7 = createInsuranceRowPanel();
        r7.add(createLabel("Ngày miễn cùng chi trả:", 188));
        insuranceExemptionDateFields[0] = createInput(25, 22);
        insuranceExemptionDateFields[1] = createInput(25, 22);
        insuranceExemptionDateFields[2] = createInput(45, 22);
        r7.add(insuranceExemptionDateFields[0]);
        r7.add(insuranceExemptionDateFields[1]);
        r7.add(insuranceExemptionDateFields[2]);
        p.add(r7);

        // Dòng 8: Ngày đóng đủ 5 năm liên tục
        JPanel r8 = createInsuranceRowPanel();
        r8.add(createLabel("Ngày đóng đủ 5 năm liên tục:", 188));
        insuranceFiveYearDateFields[0] = createInput(25, 22);
        insuranceFiveYearDateFields[1] = createInput(25, 22);
        insuranceFiveYearDateFields[2] = createInput(45, 22);
        r8.add(insuranceFiveYearDateFields[0]);
        r8.add(insuranceFiveYearDateFields[1]);
        r8.add(insuranceFiveYearDateFields[2]);
        p.add(r8);

        return p;
    }

    private JPanel createRowPanel() {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return r;
    }

    private JPanel createInsuranceRowPanel() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.setMinimumSize(new Dimension(0, 20));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return row;
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppFonts.BODY);
        l.setForeground(new Color(25, 25, 25));
        return l;
    }

    private JLabel createLabel(String text, int width) {
        JLabel l = createLabel(text);
        l.setPreferredSize(new Dimension(width, 20));
        return l;
    }

    private JTextField createInput(int w, int h) {
        JTextField tf = new JTextField();
        tf.setFont(AppFonts.BODY);
        tf.setPreferredSize(new Dimension(w, Math.min(h, 20)));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(175, 188, 202), 1),
                new EmptyBorder(1, 4, 1, 4)
        ));
        return tf;
    }

    private JButton createBrowseButton() {
        JButton btn = new JButton("...");
        btn.setFont(AppFonts.SMALL);
        btn.setPreferredSize(new Dimension(22, 20));
        btn.setMargin(new Insets(0, 0, 0, 0));
        return btn;
    }

    public void setClinicRooms(List<PhongKham> rooms) {
        Objects.requireNonNull(rooms, "rooms");
        clinicRooms.clear();
        clinicRooms.addAll(rooms);
        String selectedRoom = chooserClinicRoom.getText();
        String[] roomLabels = clinicRoomChooserLabels(rooms);
        chooserClinicRoom.setItems(roomLabels);
        if (java.util.Arrays.stream(roomLabels).anyMatch(label -> label.equalsIgnoreCase(selectedRoom))) {
            chooserClinicRoom.setText(selectedRoom);
        } else {
            chooserClinicRoom.setText(roomLabels.length == 0 ? "" : roomLabels[0]);
        }
    }

    private static String[] clinicRoomChooserLabels(List<PhongKham> rooms) {
        Map<String, Long> nameCounts = new HashMap<>();
        for (PhongKham room : rooms) {
            nameCounts.merge(normalizedRoomName(room), 1L, Long::sum);
        }
        return rooms.stream()
                .map(room -> nameCounts.get(normalizedRoomName(room)) > 1
                        ? room.getTenPhong() + " (#" + room.getId() + ")"
                        : room.getTenPhong())
                .toArray(String[]::new);
    }

    private static String normalizedRoomName(PhongKham room) {
        return room.getTenPhong().trim().toLowerCase(Locale.ROOT);
    }

    public void setOnSearch(BiConsumer<String, String> listener) {
        this.onSearchListener = listener;
    }

    private void fireSearch() {
        if (onSearchListener != null) {
            onSearchListener.accept(txtPhoneSearch.getText().trim(), txtCccdSearch.getText().trim());
        }
    }

    public void selectPatient(BenhNhan patient, BaoHiem insurance) {
        Objects.requireNonNull(patient, "patient");
        patientId = patient.getId();
        txtMaBn.setText(nullToEmpty(patient.getMaBenhNhanFormat()));
        txtHoTen.setText(patient.getHoTen());
        GioiTinh gender = patient.getGioiTinh();
        cbGioiTinh.setSelectedItem(gender == null ? null : gender.getDescription());
        setDateFields(patient.getNgaySinh(), txtDay, txtMonth, txtYear);
        txtNoiSinh.setText(nullToEmpty(patient.getNoiSinh()));
        txtSoNhaThonPho.setText(nullToEmpty(patient.getSoNhaThonPho()));
        txtAdministrativeCode.setText(nullToEmpty(patient.getMaXa()));
        lblAddress.setText(nullToEmpty(patient.getDiaChi()));
        txtJob.setText(nullToEmpty(patient.getNgheNghiep()));
        txtPhone.setText(nullToEmpty(patient.getDienThoai()));
        txtEmail.setText(nullToEmpty(patient.getEmail()));
        selectComboValue(cbDanToc, ETHNICITIES, patient.getDanToc());
        selectComboValue(cbQuocTich, NATIONALITIES, patient.getQuocTich());
        txtCccd.setText(nullToEmpty(patient.getSoCCCD()));
        setDateFields(patient.getNgayCapCCCD(), txtCccdD, txtCccdM, txtCccdY);
        txtNoiCap.setText(nullToEmpty(patient.getNoiCapCCCD()));
        txtWork.setText(nullToEmpty(patient.getNoiLamViec()));
        if (insurance == null) {
            clearInsuranceFields();
        } else {
            setInsuranceFields(insurance);
        }
    }

    public void clearForm() {
        patientId = null;
        txtPhoneSearch.setText("");
        txtCccdSearch.setText("");
        txtMaBn.setText("");
        txtHoTen.setText("");
        cbGioiTinh.setSelectedIndex(0);
        clearDateFields(txtDay, txtMonth, txtYear);
        txtNoiSinh.setText("");
        txtSoNhaThonPho.setText("");
        txtAdministrativeCode.setText("");
        lblAddress.setText("");
        txtJob.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        cbDanToc.setModel(new DefaultComboBoxModel<>(ETHNICITIES));
        cbQuocTich.setModel(new DefaultComboBoxModel<>(NATIONALITIES));
        txtCccd.setText("");
        clearDateFields(txtCccdD, txtCccdM, txtCccdY);
        txtNoiCap.setText("");
        txtWork.setText("");
        cbDkKham.setSelectedIndex(0);
        txtTime.setText(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        txtReason.setText("");
        cbDoiTuong.setSelectedIndex(0);
        chkUuTien.setSelected(false);
        chooserService.setText("");
        chooserClinicRoom.setText(clinicRooms.isEmpty() ? "" : clinicRoomChooserLabels(clinicRooms)[0]);
        clearInsuranceFields();
    }

    public RegisterPatientCommand toRegisterCommand(AuthorizationContext authorizationContext,
                                                    AuditActorContext auditActorContext) {
        BenhNhan patient = new BenhNhan();
        patient.setId(patientId);
        patient.setMaBenhNhanFormat(txtMaBn.getText().trim());
        String fullName = txtHoTen.getText().trim();
        int lastSpace = fullName.lastIndexOf(' ');
        patient.setHo(lastSpace < 0 ? "" : fullName.substring(0, lastSpace).trim());
        patient.setTen(lastSpace < 0 ? fullName : fullName.substring(lastSpace + 1).trim());
        patient.setGioiTinh(switch (Objects.toString(cbGioiTinh.getSelectedItem(), "")) {
            case "Nữ" -> GioiTinh.F;
            case "Nam" -> GioiTinh.M;
            case "Khác" -> GioiTinh.O;
            default -> null;
        });
        patient.setNgaySinh(dateFromFields(txtDay, txtMonth, txtYear));
        patient.setNoiSinh(txtNoiSinh.getText().trim());
        patient.setSoNhaThonPho(txtSoNhaThonPho.getText().trim());
        patient.setMaXa(txtAdministrativeCode.getText().trim());
        patient.setDiaChi(lblAddress.getText());
        patient.setNgheNghiep(txtJob.getText().trim());
        patient.setDienThoai(txtPhone.getText().trim());
        patient.setEmail(txtEmail.getText().trim());
        patient.setDanToc((String) cbDanToc.getSelectedItem());
        patient.setQuocTich((String) cbQuocTich.getSelectedItem());
        patient.setSoCCCD(txtCccd.getText().trim());
        patient.setNgayCapCCCD(dateFromFields(txtCccdD, txtCccdM, txtCccdY));
        patient.setNoiCapCCCD(txtNoiCap.getText().trim());
        patient.setNoiLamViec(txtWork.getText().trim());

        BaoHiem insurance = buildInsurance();
        String selectedRoom = chooserClinicRoom.getText();
        String[] roomLabels = clinicRoomChooserLabels(clinicRooms);
        int roomId = java.util.stream.IntStream.range(0, clinicRooms.size())
                .filter(index -> roomLabels[index].equalsIgnoreCase(selectedRoom))
                .mapToObj(clinicRooms::get)
                .map(PhongKham::getId)
                .filter(Objects::nonNull)
                .findFirst()
                .or(() -> clinicRooms.stream()
                        .filter(room -> room.getTenPhong().equalsIgnoreCase(selectedRoom.trim()))
                        .map(PhongKham::getId)
                        .filter(Objects::nonNull)
                        .findFirst())
                .orElseThrow(() -> new IllegalStateException("A clinic room must be selected."));
        DoiTuongKham payerType = switch (Objects.toString(cbDoiTuong.getSelectedItem(), "")) {
            case "BHYT" -> DoiTuongKham.BHYT;
            case "Dịch vụ" -> DoiTuongKham.DICH_VU_YEU_CAU;
            default -> DoiTuongKham.THU_PHI;
        };
        return new RegisterPatientCommand(patient, insurance, roomId,
                Objects.toString(cbDkKham.getSelectedItem(), ""),
                txtReason.getText().trim(), payerType, chkUuTien.isSelected(),
                authorizationContext.employeeId(), authorizationContext, auditActorContext);
    }

    private BaoHiem buildInsurance() {
        String insuranceNumber = textFromFields(insuranceNumberFields);
        if (insuranceNumber.isBlank() && txtInsuranceFacilityCode.getText().isBlank()
                && txtInsuranceFacilityName.getText().isBlank()
                && txtInsuranceStartDate.getText().isBlank() && txtInsuranceEndDate.getText().isBlank()
                && txtDiaChiThe.getText().isBlank() && Objects.toString(cbNoiSong.getSelectedItem(), "").isBlank()
                && cbTuyen.getSelectedIndex() == 0 && "0%".equals(lblRate.getText())
                && textFromFields(insuranceExemptionDateFields).isBlank()
                && textFromFields(insuranceFiveYearDateFields).isBlank()
                && !chkCoGiayKhaiSinh.isSelected() && !chkKhongGiuThe.isSelected()
                && !chkKhongCungChiTra.isSelected()) {
            return null;
        }
        BaoHiem insurance = new BaoHiem();
        insurance.setMaBenhNhan(patientId == null ? 0 : patientId);
        insurance.setSoBaoHiem(insuranceNumber);
        insurance.setMaNoiSong((String) cbNoiSong.getSelectedItem());
        insurance.setMaNoiDangKyKCB(txtInsuranceFacilityCode.getText().trim());
        insurance.setNoiDangKyKCB(txtInsuranceFacilityName.getText().trim());
        insurance.setNgayBatDau(parseDate(txtInsuranceStartDate.getText()));
        insurance.setNgayHetHan(parseDate(txtInsuranceEndDate.getText()));
        insurance.setDiaChiThe(txtDiaChiThe.getText().trim());
        insurance.setDungTuyen(!"Trái tuyến".equals(cbTuyen.getSelectedItem()));
        insurance.setMucHuong(parsePercentage(lblRate.getText()));
        insurance.setCoGiayKhaiSinh(chkCoGiayKhaiSinh.isSelected());
        insurance.setKhongGiuThe(chkKhongGiuThe.isSelected());
        insurance.setMienCungChiTra(chkKhongCungChiTra.isSelected());
        insurance.setNgayMienCungChiTra(dateFromFields(insuranceExemptionDateFields));
        insurance.setNgayDu5NamLienTuc(dateFromFields(insuranceFiveYearDateFields));
        return insurance;
    }

    private void setInsuranceFields(BaoHiem insurance) {
        setTextFields(insuranceNumberFields, insurance.getSoBaoHiem(), new int[]{2, 1, 2});
        selectComboValue(cbNoiSong, INSURANCE_RESIDENCE_CODES, insurance.getMaNoiSong());
        txtInsuranceFacilityCode.setText(nullToEmpty(insurance.getMaNoiDangKyKCB()));
        txtInsuranceFacilityName.setText(nullToEmpty(insurance.getNoiDangKyKCB()));
        txtInsuranceStartDate.setText(formatDate(insurance.getNgayBatDau()));
        txtInsuranceEndDate.setText(formatDate(insurance.getNgayHetHan()));
        txtDiaChiThe.setText(nullToEmpty(insurance.getDiaChiThe()));
        cbTuyen.setSelectedItem(insurance.isDungTuyen() ? "Đúng tuyến" : "Trái tuyến");
        lblRate.setText(insurance.getMucHuong() + "%");
        chkCoGiayKhaiSinh.setSelected(insurance.isCoGiayKhaiSinh());
        chkKhongGiuThe.setSelected(insurance.isKhongGiuThe());
        chkKhongCungChiTra.setSelected(insurance.isMienCungChiTra());
        setDateFields(insurance.getNgayMienCungChiTra(), insuranceExemptionDateFields);
        setDateFields(insurance.getNgayDu5NamLienTuc(), insuranceFiveYearDateFields);
    }

    private void clearInsuranceFields() {
        for (JTextField field : insuranceNumberFields) field.setText("");
        cbNoiSong.setModel(new DefaultComboBoxModel<>(INSURANCE_RESIDENCE_CODES));
        txtInsuranceFacilityCode.setText("");
        txtInsuranceFacilityName.setText("");
        txtInsuranceStartDate.setText("");
        txtInsuranceEndDate.setText("");
        txtDiaChiThe.setText("");
        cbTuyen.setSelectedIndex(0);
        lblRate.setText("0%");
        chkCoGiayKhaiSinh.setSelected(false);
        chkKhongGiuThe.setSelected(false);
        chkKhongCungChiTra.setSelected(false);
        clearDateFields(insuranceExemptionDateFields);
        clearDateFields(insuranceFiveYearDateFields);
    }

    private static void setTextFields(JTextField[] fields, String value, int[] segmentLengths) {
        String remaining = nullToEmpty(value);
        for (int i = 0; i < fields.length; i++) {
            int length = i < segmentLengths.length ? segmentLengths[i] : remaining.length();
            int end = Math.min(length, remaining.length());
            fields[i].setText(remaining.substring(0, end));
            remaining = remaining.substring(end);
        }
    }

    private static String textFromFields(JTextField[] fields) {
        StringBuilder value = new StringBuilder();
        for (JTextField field : fields) value.append(field.getText().trim());
        return value.toString();
    }

    private static LocalDate dateFromFields(JTextField... fields) {
        String day = fields[0].getText().trim();
        String month = fields[1].getText().trim();
        String year = fields[2].getText().trim();
        if (day.isEmpty() && month.isEmpty() && year.isEmpty()) return null;
        try {
            return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
        } catch (NumberFormatException | DateTimeException exception) {
            throw new IllegalArgumentException("Date fields must contain a valid day, month, and year.", exception);
        }
    }

    private static LocalDate parseDate(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("d/M/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Date must use the day/month/year format.", exception);
        }
    }

    private static void setDateFields(LocalDate date, JTextField... fields) {
        if (date == null) {
            clearDateFields(fields);
            return;
        }
        fields[0].setText(String.format("%02d", date.getDayOfMonth()));
        fields[1].setText(String.format("%02d", date.getMonthValue()));
        fields[2].setText(Integer.toString(date.getYear()));
    }

    private static void clearDateFields(JTextField... fields) {
        for (JTextField field : fields) field.setText("");
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static int parsePercentage(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private static void selectComboValue(JComboBox<String> combo, String[] options, String value) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(options);
        if (value != null && model.getIndexOf(value) == -1) {
            model.addElement(value);
        }
        model.setSelectedItem(value);
        combo.setModel(model);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
