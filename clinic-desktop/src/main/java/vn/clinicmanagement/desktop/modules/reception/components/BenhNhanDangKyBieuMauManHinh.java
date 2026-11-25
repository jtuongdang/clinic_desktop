package vn.clinicmanagement.desktop.modules.reception.components;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.reception.register.RegisterBenhNhanCommand;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

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
public class BenhNhanDangKyBieuMauManHinh extends JPanel {
    /*/ Trường `ETHNICITIES`: dân tộc. */
    private static final String[] ETHNICITIES = {"Kinh", "Tày", "Thái", "Mường", "Khác"};
    private static final String[] NATIONALITIES = {"Việt nam", "Khác"};
    private static final String[] INSURANCE_RESIDENCE_CODES = {"", "K1", "K2", "K3"};

    private YTeBatLenBoChon chooserService;
    private YTeBatLenBoChon chooserClinicRoom;
    private final List<PhongKham> clinicRooms = new ArrayList<>();
    private final List<DanhMucDichVu> clinicalServices = new ArrayList<>();
    private Integer patientId;
    private boolean clinicalServicesReady;
    private TiepDonNgayNhapManHinh insuranceStartInput;
    private TiepDonNgayNhapManHinh insuranceEndInput;
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

    public BenhNhanDangKyBieuMauManHinh() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 215, 226)));

        // --- 1. THANH TÌM KIẾM NHANH TRÊN CÙNG ---
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        searchBar.setBackground(new Color(250, 250, 250));
        searchBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 232, 240)));
        searchBar.setPreferredSize(new Dimension(1000, 32));
        searchBar.setMinimumSize(new Dimension(0, 32));
        searchBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lblDoiTuong = new JLabel("Đối tượng:");
        lblDoiTuong.setFont(UngDungPhongChu.BODY_BOLD);
        cbDoiTuong = new JComboBox<>(new String[]{"Viện phí", "BHYT", "Dịch vụ"});
        cbDoiTuong.setFont(UngDungPhongChu.BODY);
        cbDoiTuong.setPreferredSize(new Dimension(95, 20));

        chkUuTien = new JCheckBox("ĐN vn tiền");
        chkUuTien.setFont(UngDungPhongChu.SMALL_BOLD);
        chkUuTien.setForeground(new Color(205, 35, 35)); // Màu đỏ chuẩn
        chkUuTien.setOpaque(false);

        JLabel lblTimKiem = new JLabel("Tìm kiếm :");
        lblTimKiem.setFont(UngDungPhongChu.BODY_BOLD);
        lblTimKiem.setForeground(new Color(125, 25, 125)); // Tím đậm chuẩn EHC

        JLabel lblPhone = new JLabel("Số điện thoại:");
        lblPhone.setFont(UngDungPhongChu.BODY);
        txtPhoneSearch = createInput(80, 22);

        JLabel lblCccdSearch = new JLabel("Số CCCD:");
        lblCccdSearch.setFont(UngDungPhongChu.BODY);
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

        searchBar.setLayout(new TiepDonBoCucMau(806, 34,
                new int[]{4, 72}, new int[]{78, 99}, new int[]{182, 76},
                new int[]{259, 64}, new int[]{326, 77}, new int[]{404, 88},
                new int[]{495, 52}, new int[]{549, 106}));
        searchBar.setPreferredSize(new Dimension(806, 34));

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
        insurancePanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        patientPanel.setMinimumSize(new Dimension(0, 0));
        insurancePanel.setMinimumSize(new Dimension(0, 0));

        JPanel columns = new JPanel(null) {
            @Override public void doLayout() {
                int left = (int) Math.round(getWidth() * 806.0 / 1392.0);
                patientPanel.setBounds(0, 0, left, getHeight());
                insurancePanel.setBounds(left + 1, 0, getWidth() - left - 1, getHeight());
            }
        };
        columns.setBackground(new Color(150, 150, 150));
        columns.add(patientPanel);
        columns.add(insurancePanel);
        formContainer.add(columns, BorderLayout.CENTER);

        add(formContainer, BorderLayout.CENTER);
        TiepDonKieuMau.apply(this);
        chkUuTien.setFont(UngDungPhongChu.TIEP_DON);

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
        txtMaBn.setFont(UngDungPhongChu.BODY_BOLD);
        r1.add(txtMaBn);
        r1.add(createLabel("Họ và tên:"));
        txtHoTen = createInput(285, 22);
        txtHoTen.setFont(UngDungPhongChu.BODY_BOLD);
        r1.add(txtHoTen);
        p.add(r1);

        // Dòng 2: Giới tính, Ngày sinh (3 ô), 22 tuổi, Nơi sinh
        JPanel r2 = createRowPanel();
        r2.add(createLabel("Giới tính:", 65));
        cbGioiTinh = new JComboBox<>(new String[]{"Nữ", "Nam", "Khác"});
        cbGioiTinh.setFont(UngDungPhongChu.BODY);
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
        birthDateFields.setLayout(new TiepDonBoCucMau(97, 20, new int[]{0, 26}, new int[]{26, 26}, new int[]{52, 45}));
        r2.add(birthDateFields);
        r2.add(Box.createHorizontalStrut(52));

        JLabel lblAge = new JLabel("");
        lblAge.setFont(UngDungPhongChu.BODY_BOLD);
        lblAge.setForeground(new Color(0, 85, 204)); // Xanh lam chuẩn
        lblAge.setBorder(new EmptyBorder(0, 4, 0, 4));
        r2.add(lblAge);
        javax.swing.event.DocumentListener ageListener = new javax.swing.event.DocumentListener() {
            private void updateAge() {
                try {
                    LocalDate birth = LocalDate.of(Integer.parseInt(txtYear.getText()),
                            Integer.parseInt(txtMonth.getText()), Integer.parseInt(txtDay.getText()));
                    LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
                    lblAge.setText(birth.isAfter(today) ? "" : java.time.Period.between(birth, today).getYears() + " tuổi");
                } catch (RuntimeException invalidDate) { lblAge.setText(""); }
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateAge(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateAge(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateAge(); }
        };
        for (JTextField field : new JTextField[]{txtDay, txtMonth, txtYear})
            field.getDocument().addDocumentListener(ageListener);

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
        r3.add(createLabel("Tụ/hộ/Xp:", 61));
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
        cbDanToc.setFont(UngDungPhongChu.BODY);
        cbDanToc.setPreferredSize(new Dimension(109, 20));
        r4.add(cbDanToc);
        r4.add(Box.createHorizontalStrut(3));
        r4.add(createLabel("Quốc tịch:", 61));
        cbQuocTich = new JComboBox<>(NATIONALITIES);
        cbQuocTich.setFont(UngDungPhongChu.BODY);
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
        issueDateFields.setLayout(new TiepDonBoCucMau(97, 20, new int[]{0, 26}, new int[]{26, 26}, new int[]{52, 45}));
        r5.add(issueDateFields);
        r5.add(Box.createHorizontalStrut(11));
        r5.add(createLabel("Nơi cấp:", 51));
        txtNoiCap = createInput(374, 22);
        r5.add(txtNoiCap);
        p.add(r5);

        // Dòng 6: TT khác, Nơi làm việc
        JPanel r6 = createRowPanel();
        JButton btnTtKhac = new JButton("TT khác");
        btnTtKhac.setFont(UngDungPhongChu.SMALL);
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
        cbDkKham.setFont(UngDungPhongChu.BODY);
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

        // Dòng 8: Dịch vụ, Phòng khám (Nạp động hoàn toàn từ CSDL)
        JPanel r8 = createRowPanel();
        r8.add(createLabel("Dịch vụ:", 65));
        chooserService = new YTeBatLenBoChon(new String[0], "", 258, 20);
        r8.add(chooserService);

        r8.add(createLabel("Phòng khám:", 78));
        chooserClinicRoom = new YTeBatLenBoChon(new String[0], "", 395, 20);
        chooserClinicRoom.setHighlighted(true);
        r8.add(chooserClinicRoom);

        // Sau khi chọn Dịch Vụ: Chuyển focus sang Phòng Khám và bôi vàng
        chooserService.setOnSelectListener(svc -> {
            chooserClinicRoom.requestFocusAndHighlight();
            if (onServiceListener != null) {
                onServiceListener.accept(svc);
            }
        });

        p.add(r8);
        r1.setLayout(new TiepDonBoCucMau(806, 22, new int[]{4, 72}, new int[]{78, 99}, new int[]{182, 60}, new int[]{243, 276}));
        r2.setLayout(new TiepDonBoCucMau(806, 22, new int[]{4, 72}, new int[]{78, 99}, new int[]{182, 60}, new int[]{243, 97}, new int[]{340, 61}, new int[]{401, 50}, new int[]{451, 78}, new int[]{529, 59}, new int[]{589, 212}));
        r3.setLayout(new TiepDonBoCucMau(806, 22, new int[]{4, 72}, new int[]{78, 99}, new int[]{182, 60}, new int[]{243, 97}, new int[]{342, 297}, new int[]{642, 73}, new int[]{716, 70}, new int[]{786, 14}));
        r4.setLayout(new TiepDonBoCucMau(806, 22, new int[]{4, 72}, new int[]{78, 99}, new int[]{182, 60}, new int[]{243, 97}, new int[]{340, 17}, new int[]{357, 54}, new int[]{411, 108}, new int[]{519, 10}, new int[]{529, 59}, new int[]{589, 212}));
        r5.setLayout(new TiepDonBoCucMau(806, 22, new int[]{4, 72}, new int[]{78, 99}, new int[]{182, 60}, new int[]{243, 97}, new int[]{340, 18}, new int[]{358, 53}, new int[]{411, 390}));
        r6.setLayout(new TiepDonBoCucMau(806, 22, new int[]{4, 72}, new int[]{76, 390}, new int[]{467, 73}, new int[]{541, 260}));
        r7.setLayout(new TiepDonBoCucMau(806, 22, new int[]{4, 72}, new int[]{78, 99}, new int[]{182, 29}, new int[]{212, 117}, new int[]{329, 7}, new int[]{336, 75}, new int[]{411, 390}));
        r8.setLayout(new TiepDonBoCucMau(806, 22, new int[]{4, 72}, new int[]{78, 252}, new int[]{336, 75}, new int[]{411, 390}));
        txtMaBn.setHorizontalAlignment(SwingConstants.CENTER);
        txtTime.setBorder(BorderFactory.createEmptyBorder());
        txtTime.setOpaque(false);
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
        lblBhyt.setFont(UngDungPhongChu.BODY_BOLD);
        rHead.add(lblBhyt);

        chkCoGiayKhaiSinh = new JCheckBox("TE có giấy khai sinh");
        chkCoGiayKhaiSinh.setFont(UngDungPhongChu.SMALL);
        chkCoGiayKhaiSinh.setForeground(new Color(205, 30, 30));
        chkCoGiayKhaiSinh.setOpaque(false);
        chkCoGiayKhaiSinh.setPreferredSize(new Dimension(chkCoGiayKhaiSinh.getPreferredSize().width, 20));
        rHead.add(chkCoGiayKhaiSinh);

        chkKhongGiuThe = new JCheckBox("Không giữ thẻ BHYT");
        chkKhongGiuThe.setFont(UngDungPhongChu.SMALL);
        chkKhongGiuThe.setForeground(new Color(205, 30, 30));
        chkKhongGiuThe.setOpaque(false);
        chkKhongGiuThe.setPreferredSize(new Dimension(chkKhongGiuThe.getPreferredSize().width, 20));
        rHead.add(chkKhongGiuThe);
        rHead.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
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
        insuranceNumberFields.setLayout(new TiepDonBoCucMau(222, 20, new int[]{0, 32}, new int[]{32, 32}, new int[]{64, 32}, new int[]{96, 126}));
        r1.add(insuranceNumberFields);
        r1.add(createLabel("Nơi sống:", 58));
        cbNoiSong = new JComboBox<>(INSURANCE_RESIDENCE_CODES);
        cbNoiSong.setFont(UngDungPhongChu.BODY);
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
        insuranceStartInput = new TiepDonNgayNhapManHinh(txtInsuranceStartDate);
        r3.add(insuranceStartInput);
        r3.add(new JLabel(" - "));
        txtInsuranceEndDate = createInput(100, 22);
        insuranceEndInput = new TiepDonNgayNhapManHinh(txtInsuranceEndDate);
        r3.add(insuranceEndInput);
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
        cbTuyen.setFont(UngDungPhongChu.BODY);
        cbTuyen.setPreferredSize(new Dimension(224, 20));
        r5.add(cbTuyen);
        lblRate = new JLabel("0%");
        lblRate.setFont(UngDungPhongChu.BODY_BOLD);
        lblRate.setForeground(new Color(0, 95, 205));
        lblRate.setBorder(new EmptyBorder(0, 4, 0, 0));
        r5.add(lblRate);
        p.add(r5);

        // Dòng 6: Giấy chứng nhận không cùng chi trả
        JPanel r6 = createInsuranceRowPanel();
        chkKhongCungChiTra = new JCheckBox("Giấy chứng nhận không cùng chi trả trong năm");
        chkKhongCungChiTra.setFont(UngDungPhongChu.SMALL);
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
        rHead.setLayout(new TiepDonBoCucMau(586, 36, new int[]{5, 132}, new int[]{139, 134}, new int[]{275, 160}));
        r1.setLayout(new TiepDonBoCucMau(586, 22, new int[]{5, 68}, new int[]{73, 222}, new int[]{298, 51}, new int[]{351, 52}));
        r2.setLayout(new TiepDonBoCucMau(586, 22, new int[]{5, 68}, new int[]{73, 64}, new int[]{137, 266}));
        r3.setLayout(new TiepDonBoCucMau(586, 22, new int[]{5, 68}, new int[]{73, 98}, new int[]{171, 23}, new int[]{194, 98}));
        r4.setLayout(new TiepDonBoCucMau(586, 22, new int[]{5, 68}, new int[]{73, 330}));
        r5.setLayout(new TiepDonBoCucMau(586, 22, new int[]{5, 68}, new int[]{73, 220}, new int[]{313, 35}));
        r6.setLayout(new TiepDonBoCucMau(586, 22, new int[]{5, 400}));
        r7.setLayout(new TiepDonBoCucMau(586, 22, new int[]{5, 190}, new int[]{195, 26}, new int[]{221, 26}, new int[]{247, 45}));
        r8.setLayout(new TiepDonBoCucMau(586, 22, new int[]{5, 190}, new int[]{195, 26}, new int[]{221, 26}, new int[]{247, 45}));
        return p;
    }

    private JPanel createRowPanel() {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        return r;
    }

    private JPanel createInsuranceRowPanel() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.setMinimumSize(new Dimension(0, 20));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        return row;
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.BODY);
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
        tf.setFont(UngDungPhongChu.BODY);
        tf.setPreferredSize(new Dimension(w, Math.min(h, 20)));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(120, 120, 120), 1),
                new EmptyBorder(1, 4, 1, 4)
        ));
        return tf;
    }

    private JButton createBrowseButton() {
        JButton btn = new JButton("...");
        btn.setFont(UngDungPhongChu.SMALL);
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

    /** Tạm chặn lưu khi tải lại danh mục, giữ lựa chọn đang hiển thị đến khi nhận kết quả. */
    public void beginClinicalServiceLoad() { clinicalServicesReady = false; }

    public void setClinicalServices(List<DanhMucDichVu> services) {
        Objects.requireNonNull(services, "services");
        clinicalServices.clear();
        clinicalServices.addAll(services);
        clinicalServicesReady = !services.isEmpty();
        String selectedService = chooserService.getText();
        String[] serviceLabels = clinicalServiceChooserLabels(services);
        chooserService.setItems(serviceLabels);
        if (java.util.Arrays.stream(serviceLabels).anyMatch(label -> label.equalsIgnoreCase(selectedService))) {
            chooserService.setText(selectedService);
        } else {
            chooserService.setText(serviceLabels.length == 0 ? "" : serviceLabels[0]);
        }
    }

    public static String[] clinicalServiceChooserLabels(List<DanhMucDichVu> services) {
        if (services == null || services.isEmpty()) {
            return new String[0];
        }
        return services.stream()
                .map(DanhMucDichVu::getTenDichVu)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }

    public List<DanhMucDichVu> getClinicalServices() {
        return List.copyOf(clinicalServices);
    }

    public YTeBatLenBoChon getChooserService() {
        return chooserService;
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
        clearValidationMarkers(this);
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
        String[] serviceLabels = clinicalServiceChooserLabels(clinicalServices);
        chooserService.setText(serviceLabels.length == 0 ? "" : serviceLabels[0]);
        chooserClinicRoom.setText(clinicRooms.isEmpty() ? "" : clinicRoomChooserLabels(clinicRooms)[0]);
        clearInsuranceFields();
    }

    public RegisterBenhNhanCommand toRegisterCommand(PhanQuyenNguCanh authorizationContext,
                                                    KiemToanTacNhanNguCanh auditActorContext) {
        BenhNhan patient = new BenhNhan();
        patient.setId(patientId);
        patient.setMaBenhNhanFormat(txtMaBn.getText().trim());
        String fullName = txtHoTen.getText().trim();
        int lastSpace = fullName.lastIndexOf(' ');
        if (lastSpace < 1 || fullName.substring(0, lastSpace).isBlank())
            throw invalid("Vui lòng nhập đầy đủ họ và tên người bệnh.", txtHoTen);
        patient.setHo(lastSpace < 0 ? "" : fullName.substring(0, lastSpace).trim());
        patient.setTen(lastSpace < 0 ? fullName : fullName.substring(lastSpace + 1).trim());
        patient.setGioiTinh(switch (Objects.toString(cbGioiTinh.getSelectedItem(), "")) {
            case "Nữ" -> GioiTinh.F;
            case "Nam" -> GioiTinh.M;
            case "Khác" -> GioiTinh.O;
            default -> null;
        });
        if (patient.getGioiTinh() == null) throw invalid("Vui lòng chọn giới tính.", cbGioiTinh);
        LocalDate birth = readDate("Ngày sinh", true, txtDay, txtMonth, txtYear);
        if (birth.isAfter(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))))
            throw invalid("Ngày sinh không được nằm trong tương lai.", txtDay);
        patient.setNgaySinh(birth);
        patient.setNoiSinh(txtNoiSinh.getText().trim());
        patient.setSoNhaThonPho(txtSoNhaThonPho.getText().trim());
        patient.setMaXa(txtAdministrativeCode.getText().trim());
        patient.setDiaChi(lblAddress.getText());
        patient.setNgheNghiep(txtJob.getText().trim());
        if (!txtPhone.getText().isBlank()) {
            try { new vn.clinicmanagement.domain.common.valueobjects.DienThoaiSo(txtPhone.getText()); }
            catch (IllegalArgumentException error) { throw invalid("Số điện thoại không hợp lệ. Vui lòng nhập số di động đúng định dạng, ví dụ 0912345678.", txtPhone); }
        }
        patient.setDienThoai(txtPhone.getText().trim());
        patient.setEmail(txtEmail.getText().trim());
        patient.setDanToc((String) cbDanToc.getSelectedItem());
        patient.setQuocTich((String) cbQuocTich.getSelectedItem());
        patient.setSoCCCD(txtCccd.getText().trim());
        patient.setNgayCapCCCD(readDate("Ngày cấp CMND/CCCD", false, txtCccdD, txtCccdM, txtCccdY));
        patient.setNoiCapCCCD(txtNoiCap.getText().trim());
        patient.setNoiLamViec(txtWork.getText().trim());

        if (cbDkKham.getSelectedItem() == null || cbDkKham.getSelectedItem().toString().isBlank())
            throw invalid("Vui lòng chọn loại đăng ký khám.", cbDkKham);
        if (cbDoiTuong.getSelectedItem() == null) throw invalid("Vui lòng chọn đối tượng khám.", cbDoiTuong);
        if ("BHYT".equals(cbDoiTuong.getSelectedItem()) && textFromFields(insuranceNumberFields).isBlank())
            throw invalid("Đối tượng BHYT cần có số thẻ bảo hiểm. Vui lòng nhập số thẻ BHYT.", insuranceNumberFields[0]);
        BaoHiem insurance = buildInsurance();
        Integer serviceId = resolveSelectedServiceId();
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
                .orElseThrow(() -> invalid(clinicRooms.isEmpty() ? "Chưa tải được danh sách phòng khám. Vui lòng bấm Làm Mới và thử lại." : "Vui lòng chọn phòng khám có trong danh sách.", chooserClinicRoom.getTextField()));
        DoiTuongKham payerType = switch (Objects.toString(cbDoiTuong.getSelectedItem(), "")) {
            case "BHYT" -> DoiTuongKham.BHYT;
            case "Dịch vụ" -> DoiTuongKham.DICH_VU_YEU_CAU;
            default -> DoiTuongKham.THU_PHI;
        };
        return new RegisterBenhNhanCommand(patient, insurance, roomId, serviceId,
                Objects.toString(cbDkKham.getSelectedItem(), ""),
                txtReason.getText().trim(), payerType, chkUuTien.isSelected(),
                authorizationContext.employeeId(), authorizationContext, auditActorContext);
    }

    private Integer resolveSelectedServiceId() {
        if (!clinicalServicesReady || clinicalServices.isEmpty()) {
            throw invalid("Chưa có danh mục dịch vụ khám để đăng ký. Vui lòng bấm Làm Mới để tải lại; nếu danh mục vẫn trống, hãy kiểm tra cấu hình dịch vụ.", chooserService.getTextField());
        }
        String selectedService = chooserService.getText() == null ? "" : chooserService.getText().trim();
        if (selectedService.isEmpty()) {
            throw invalid("Vui lòng chọn dịch vụ khám.", chooserService.getTextField());
        }
        return clinicalServices.stream()
                .filter(service -> service.getTenDichVu() != null
                        && service.getTenDichVu().trim().equalsIgnoreCase(selectedService))
                .map(DanhMucDichVu::getId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> invalid("Dịch vụ khám đang chọn không có trong danh mục. Vui lòng chọn lại dịch vụ.", chooserService.getTextField()));
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
        LocalDate start = readInsuranceDate("Ngày bắt đầu thẻ BHYT", false, txtInsuranceStartDate, insuranceStartInput);
        LocalDate end = readInsuranceDate("Ngày hết hạn thẻ BHYT", !insuranceNumber.isBlank(), txtInsuranceEndDate, insuranceEndInput);
        if (start != null && end != null && start.isAfter(end))
            throw invalid("Ngày bắt đầu thẻ BHYT phải trước hoặc bằng ngày hết hạn.", dateFocus(insuranceStartInput));
        insurance.setNgayBatDau(start);
        insurance.setNgayHetHan(end);
        insurance.setDiaChiThe(txtDiaChiThe.getText().trim());
        insurance.setDungTuyen(!"Trái tuyến".equals(cbTuyen.getSelectedItem()));
        int rate;
        try { rate = parsePercentage(lblRate.getText()); }
        catch (IllegalArgumentException error) { throw invalid("Mức hưởng BHYT phải từ 0 đến 100%.", cbTuyen); }
        if (rate < 0 || rate > 100) throw invalid("Mức hưởng BHYT phải từ 0 đến 100%.", cbTuyen);
        insurance.setMucHuong(rate);
        insurance.setCoGiayKhaiSinh(chkCoGiayKhaiSinh.isSelected());
        insurance.setKhongGiuThe(chkKhongGiuThe.isSelected());
        insurance.setMienCungChiTra(chkKhongCungChiTra.isSelected());
        insurance.setNgayMienCungChiTra(readDate("Ngày miễn cùng chi trả", false, insuranceExemptionDateFields));
        insurance.setNgayDu5NamLienTuc(readDate("Ngày đóng đủ 5 năm liên tục", false, insuranceFiveYearDateFields));
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

    private static void clearValidationMarkers(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JComponent component) DangKyKhamNhapLieuNgoaiLe.clearInput(component);
            if (child instanceof Container container) clearValidationMarkers(container);
        }
    }

    private static DangKyKhamNhapLieuNgoaiLe invalid(String message, JComponent field) {
        return new DangKyKhamNhapLieuNgoaiLe(message, field);
    }

    /** Kiểm tra từng nhóm ngày và chỉ tới ô còn thiếu thay vì hiển thị lỗi chung. */
    private static LocalDate readDate(String label, boolean required, JTextField... fields) {
        boolean allBlank = java.util.Arrays.stream(fields).allMatch(field -> field.getText().isBlank());
        if (allBlank && !required) return null;
        for (JTextField field : fields) if (field.getText().isBlank())
            throw invalid(allBlank ? "Vui lòng nhập " + Character.toLowerCase(label.charAt(0)) + label.substring(1) + "."
                    : label + " cần đủ ngày, tháng và năm.", field);
        try { return dateFromFields(fields); }
        catch (IllegalArgumentException error) { throw invalid(label + " không hợp lệ. Vui lòng kiểm tra ngày, tháng và năm.", fields[0]); }
    }

    private static JTextField dateFocus(TiepDonNgayNhapManHinh panel) {
        for (Component component : panel.getComponents()) if (component instanceof JTextField field && field.getText().isBlank()) return field;
        return (JTextField) panel.getComponent(0);
    }

    private static LocalDate readInsuranceDate(String label, boolean required, JTextField value, TiepDonNgayNhapManHinh visible) {
        if (value.getText().isBlank()) {
            if (!required) return null;
            throw invalid("Vui lòng nhập " + Character.toLowerCase(label.charAt(0)) + label.substring(1) + ".", dateFocus(visible));
        }
        try { return parseDate(value.getText()); }
        catch (IllegalArgumentException error) { throw invalid(label + " không hợp lệ. Vui lòng nhập đủ ngày, tháng, năm và kiểm tra ngày có tồn tại.", dateFocus(visible)); }
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
