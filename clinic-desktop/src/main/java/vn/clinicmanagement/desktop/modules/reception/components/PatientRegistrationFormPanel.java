package vn.clinicmanagement.desktop.modules.reception.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

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
            "PK TEST COVID",
            "PK TEST PCR",
            "PK theo yêu cầu nhi số 1",
            "PK theo yêu cầu nhi số 2",
            "PK theo yêu cầu Sản - Phụ khoa",
            "PK sàng lọc chẩn đoán trước sinh và sơ sinh",
            "PHÒNG QUẢN LÝ HS",
            "PK Phụ nội"
    };

    private MedicalPopupChooser chooserService;
    private MedicalPopupChooser chooserClinicRoom;

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
        JComboBox<String> cbDoiTuong = new JComboBox<>(new String[]{"Viện phí", "BHYT", "Dịch vụ"});
        cbDoiTuong.setFont(AppFonts.BODY);
        cbDoiTuong.setPreferredSize(new Dimension(95, 20));

        JCheckBox chkUuTien = new JCheckBox("ĐN vn tiền");
        chkUuTien.setFont(AppFonts.SMALL_BOLD);
        chkUuTien.setForeground(new Color(205, 35, 35)); // Màu đỏ chuẩn
        chkUuTien.setOpaque(false);

        JLabel lblTimKiem = new JLabel("Tìm kiếm :");
        lblTimKiem.setFont(AppFonts.BODY_BOLD);
        lblTimKiem.setForeground(new Color(125, 25, 125)); // Tím đậm chuẩn EHC

        JLabel lblPhone = new JLabel("Số điện thoại:");
        lblPhone.setFont(AppFonts.BODY);
        JTextField txtPhoneSearch = createInput(80, 22);

        JLabel lblCccdSearch = new JLabel("Số CCCD:");
        lblCccdSearch.setFont(AppFonts.BODY);
        JTextField txtCccdSearch = createInput(110, 22);

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
    }

    /** Cột trái: Thông tin hành chính bệnh nhân */
    private JPanel buildPatientDemographicsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        // Dòng 1: Mã BN, Họ và tên
        JPanel r1 = createRowPanel();
        r1.add(createLabel("Mã BN:", 65));
        JTextField txtMaBn = createInput(100, 22);
        txtMaBn.setText("22000326");
        txtMaBn.setFont(AppFonts.BODY_BOLD);
        r1.add(txtMaBn);
        r1.add(createLabel("Họ và tên:"));
        JTextField txtHoTen = createInput(285, 22);
        txtHoTen.setText("NGUYỄN THỊ LIÊN");
        txtHoTen.setFont(AppFonts.BODY_BOLD);
        r1.add(txtHoTen);
        p.add(r1);

        // Dòng 2: Giới tính, Ngày sinh (3 ô), 22 tuổi, Nơi sinh
        JPanel r2 = createRowPanel();
        r2.add(createLabel("Giới tính:", 65));
        JComboBox<String> cbGioiTinh = new JComboBox<>(new String[]{"Nữ", "Nam"});
        cbGioiTinh.setFont(AppFonts.BODY);
        cbGioiTinh.setPreferredSize(new Dimension(100, 20));
        r2.add(cbGioiTinh);

        r2.add(createLabel("Ngày sinh:", 63));
        JPanel birthDateFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        birthDateFields.setOpaque(false);
        JTextField txtDay = createInput(26, 22); txtDay.setText("25"); txtDay.setHorizontalAlignment(SwingConstants.CENTER);
        JTextField txtMonth = createInput(26, 22); txtMonth.setText("10"); txtMonth.setHorizontalAlignment(SwingConstants.CENTER);
        JTextField txtYear = createInput(44, 22); txtYear.setText("2000"); txtYear.setHorizontalAlignment(SwingConstants.CENTER);
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
        JTextField txtNoiSinh = createInput(216, 22); txtNoiSinh.setText("bắc ninh");
        r2.add(txtNoiSinh);
        p.add(r2);

        // Dòng 3: SN/thôn/phố, Ty/Hộ/Xp, Địa chỉ gợi ý, Nghề nghiệp
        JPanel r3 = createRowPanel();
        r3.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 0));
        r3.add(createLabel("SN/thôn/phố:", 77));
        r3.add(createInput(100, 22));
        r3.add(createLabel("Ty/Hộ/Xp:", 61));
        r3.add(createInput(100, 22));

        r3.add(createLabel("Bắc Ninh - H. Quế Võ - Thị trấn Phố Mới", 304));

        r3.add(createLabel("Nghề nghiệp:"));
        JTextField txtJob = createInput(65, 22); txtJob.setText("Nhân dân");
        r3.add(txtJob);
        r3.add(createBrowseButton());
        p.add(r3);

        // Dòng 4: Điện thoại, Email, Dân tộc, Quốc tịch
        JPanel r4 = createRowPanel();
        r4.add(createLabel("Điện thoại:", 65));
        JTextField txtPhone = createInput(100, 22); txtPhone.setText("012345678");
        r4.add(txtPhone);
        r4.add(createLabel("Email:", 55));
        r4.add(createInput(100, 22));
        r4.add(Box.createHorizontalStrut(10));
        r4.add(createLabel("Dân tộc:", 50));
        JComboBox<String> cbDanToc = new JComboBox<>(new String[]{"Kinh", "Tày", "Thái", "Mường", "Khác"});
        cbDanToc.setFont(AppFonts.BODY);
        cbDanToc.setPreferredSize(new Dimension(109, 20));
        r4.add(cbDanToc);
        r4.add(Box.createHorizontalStrut(3));
        r4.add(createLabel("Quốc tịch:", 61));
        JComboBox<String> cbQuocTich = new JComboBox<>(new String[]{"Việt nam", "Khác"});
        cbQuocTich.setFont(AppFonts.BODY);
        cbQuocTich.setPreferredSize(new Dimension(212, 20));
        r4.add(cbQuocTich);
        p.add(r4);

        // Dòng 5: CMND/CCCD, Ngày cấp, Nơi cấp
        JPanel r5 = createRowPanel();
        r5.add(createLabel("CMND/CCCD:", 83));
        JTextField txtCccd = createInput(100, 22); txtCccd.setText("111122222333");
        r5.add(txtCccd);
        r5.add(createLabel("Ngày cấp:", 60));
        JPanel issueDateFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        issueDateFields.setOpaque(false);
        JTextField txtCccdD = createInput(26, 22); txtCccdD.setText("01"); txtCccdD.setHorizontalAlignment(SwingConstants.CENTER);
        JTextField txtCccdM = createInput(26, 22); txtCccdM.setText("01"); txtCccdM.setHorizontalAlignment(SwingConstants.CENTER);
        JTextField txtCccdY = createInput(44, 22); txtCccdY.setText("2018"); txtCccdY.setHorizontalAlignment(SwingConstants.CENTER);
        issueDateFields.add(txtCccdD);
        issueDateFields.add(txtCccdM);
        issueDateFields.add(txtCccdY);
        r5.add(issueDateFields);
        r5.add(Box.createHorizontalStrut(11));
        r5.add(createLabel("Nơi cấp:", 51));
        JTextField txtNoiCap = createInput(374, 22); txtNoiCap.setText("bắc ninh");
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
        JTextField txtWork = createInput(267, 22); txtWork.setText("hà nội");
        r6.add(txtWork);
        p.add(r6);

        // Dòng 7: ĐK khám, Lúc, Lý do khám
        JPanel r7 = createRowPanel();
        r7.add(createLabel("ĐK khám:", 65));
        JComboBox<String> cbDkKham = new JComboBox<>(new String[]{"Khám bệnh", "Tái khám", "Cấp cứu"});
        cbDkKham.setFont(AppFonts.BODY);
        cbDkKham.setPreferredSize(new Dimension(100, 20));
        r7.add(cbDkKham);
        r7.add(createLabel("Lúc:", 29));
        JTextField txtTime = createInput(126, 22); txtTime.setText("10:39 23/08/2022");
        r7.add(txtTime);
        r7.add(Box.createHorizontalStrut(6));
        r7.add(createLabel("Lý do khám:", 73));
        JTextField txtReason = createInput(109, 22); txtReason.setText("đau mỏi");
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

        JCheckBox chkTe = new JCheckBox("TE có giấy khai sinh");
        chkTe.setFont(AppFonts.SMALL);
        chkTe.setForeground(new Color(205, 30, 30));
        chkTe.setOpaque(false);
        chkTe.setPreferredSize(new Dimension(chkTe.getPreferredSize().width, 20));
        rHead.add(chkTe);

        JCheckBox chkKhongGiuThe = new JCheckBox("Không giữ thẻ BHYT");
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
        insuranceNumberFields.add(createInput(35, 22));
        insuranceNumberFields.add(createInput(35, 22));
        insuranceNumberFields.add(createInput(35, 22));
        insuranceNumberFields.add(createInput(115, 22));
        r1.add(insuranceNumberFields);
        r1.add(createLabel("Nơi sống:", 58));
        JComboBox<String> cbNoiSong = new JComboBox<>(new String[]{"", "K1", "K2", "K3"});
        cbNoiSong.setFont(AppFonts.BODY);
        cbNoiSong.setPreferredSize(new Dimension(50, 20));
        r1.add(cbNoiSong);
        p.add(r1);

        // Dòng 2: KCBBĐ
        JPanel r2 = createInsuranceRowPanel();
        r2.add(createLabel("KCBBĐ:", 64));
        r2.add(createInput(65, 22));
        r2.add(createInput(270, 22));
        p.add(r2);

        // Dòng 3: Hạn thẻ
        JPanel r3 = createInsuranceRowPanel();
        r3.add(createLabel("Hạn thẻ:", 64));
        r3.add(createInput(100, 22));
        r3.add(new JLabel(" - "));
        r3.add(createInput(100, 22));
        p.add(r3);

        // Dòng 4: Địa chỉ thẻ
        JPanel r4 = createInsuranceRowPanel();
        r4.add(createLabel("Địa chỉ thẻ:", 66));
        JTextField txtDiaChiThe = createInput(335, 22);
        txtDiaChiThe.setText("Bắc Ninh - H. Quế Võ - Thị trấn Phố Mới");
        r4.add(txtDiaChiThe);
        p.add(r4);

        // Dòng 5: Tuyến KCB
        JPanel r5 = createInsuranceRowPanel();
        r5.add(createLabel("Tuyến KCB:", 71));
        JComboBox<String> cbTuyen = new JComboBox<>(new String[]{"", "Đúng tuyến", "Trái tuyến"});
        cbTuyen.setFont(AppFonts.BODY);
        cbTuyen.setPreferredSize(new Dimension(224, 20));
        r5.add(cbTuyen);
        JLabel lblRate = new JLabel("0%");
        lblRate.setFont(AppFonts.BODY_BOLD);
        lblRate.setForeground(new Color(0, 95, 205));
        lblRate.setBorder(new EmptyBorder(0, 4, 0, 0));
        r5.add(lblRate);
        p.add(r5);

        // Dòng 6: Giấy chứng nhận không cùng chi trả
        JPanel r6 = createInsuranceRowPanel();
        JCheckBox chkKhongCungChiTra = new JCheckBox("Giấy chứng nhận không cùng chi trả trong năm");
        chkKhongCungChiTra.setFont(AppFonts.SMALL);
        chkKhongCungChiTra.setForeground(new Color(0, 80, 185)); // Màu xanh lam chuẩn
        chkKhongCungChiTra.setOpaque(false);
        chkKhongCungChiTra.setPreferredSize(new Dimension(chkKhongCungChiTra.getPreferredSize().width, 20));
        r6.add(chkKhongCungChiTra);
        p.add(r6);

        // Dòng 7: Ngày miễn cùng chi trả
        JPanel r7 = createInsuranceRowPanel();
        r7.add(createLabel("Ngày miễn cùng chi trả:", 188));
        r7.add(createInput(25, 22));
        r7.add(createInput(25, 22));
        r7.add(createInput(45, 22));
        p.add(r7);

        // Dòng 8: Ngày đóng đủ 5 năm liên tục
        JPanel r8 = createInsuranceRowPanel();
        r8.add(createLabel("Ngày đóng đủ 5 năm liên tục:", 188));
        r8.add(createInput(25, 22));
        r8.add(createInput(25, 22));
        r8.add(createInput(45, 22));
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
}
