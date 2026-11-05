package vn.clinicmanagement.desktop.modules.reception.components;

import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.reception.RegisterPatientCommand;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Reception form bound to patient, BHYT and visit models. */
public class PatientRegistrationFormPanel extends JPanel {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField txtSearchPhone = input("searchPhone", 100);
    private final JTextField txtSearchCccd = input("searchCccd", 120);
    private final JTextField txtPatientCode = input("patientCode", 105);
    private final JTextField txtFullName = input("fullName", 220);
    private final JComboBox<String> cbGender = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
    private final JTextField txtDateOfBirth = input("dateOfBirth", 92);
    private final JTextField txtBirthPlace = input(150);
    private final JTextField txtStreet = input(150);
    private final JTextField txtProvinceCode = input(48);
    private final JTextField txtDistrictCode = input(48);
    private final JTextField txtWardCode = input(48);
    private final JTextField txtAddress = input("address", 180);
    private final JTextField txtOccupation = input(110);
    private final JTextField txtPhone = input("phone", 100);
    private final JTextField txtEmail = input(140);
    private final JComboBox<String> cbEthnicity = new JComboBox<>(new String[]{"Kinh", "Tày", "Thái", "Mường", "Khác"});
    private final JComboBox<String> cbNationality = new JComboBox<>(new String[]{"Việt Nam", "Khác"});
    private final JTextField txtCccd = input("cccd", 120);
    private final JTextField txtCccdIssueDate = input(92);
    private final JTextField txtCccdIssuePlace = input(165);
    private final JTextField txtWorkplace = input(150);
    private final JTextField txtContactName = input(120);
    private final JTextField txtContactPhone = input(100);
    private final JComboBox<String> cbRegistrationType = new JComboBox<>(new String[]{"Khám bệnh", "Tái khám", "Cấp cứu"});
    private final JTextField txtReason = input("reason", 150);
    private final JComboBox<String> cbPayerType = new JComboBox<>(new String[]{"Viện phí", "BHYT", "Dịch vụ theo yêu cầu"});
    private final JCheckBox chkPriority = new JCheckBox("Ưu tiên");
    private final MedicalPopupChooser chooserService = new MedicalPopupChooser(new String[0], "", 200, 22);
    private final MedicalPopupChooser chooserClinicRoom = new MedicalPopupChooser(new String[0], "", 220, 22);

    private final JTextField txtInsuranceNumber = input("insuranceNumber", 205);
    private final JTextField txtInsuranceStartDate = input(92);
    private final JTextField txtInsuranceEndDate = input(92);
    private final JTextField txtInsuranceRate = input(42);
    private final JComboBox<String> cbLivingArea = new JComboBox<>(new String[]{"", "K1", "K2", "K3"});
    private final JTextField txtFacilityCode = input(90);
    private final JTextField txtFacilityName = input(170);
    private final JTextField txtInsuranceAddress = input(260);
    private final JComboBox<String> cbRoute = new JComboBox<>(new String[]{"Đúng tuyến", "Trái tuyến"});
    private final JCheckBox chkBirthCertificate = new JCheckBox("Trẻ em có giấy khai sinh");
    private final JCheckBox chkNotHoldingCard = new JCheckBox("Không giữ thẻ BHYT");
    private final JCheckBox chkExemptCopay = new JCheckBox("Miễn cùng chi trả trong năm");
    private final JTextField txtCopayExemptDate = input(92);
    private final JTextField txtFiveYearDate = input(92);

    private final Map<String, PhongKham> roomsByName = new HashMap<>();
    private BenhNhan selectedPatient;
    private BaoHiem selectedInsurance;
    private Consumer<String> onClinicRoomChanged;
    private Consumer<String> onServiceChanged;
    private BiConsumer<String, String> onSearch;

    public PatientRegistrationFormPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 215, 226)));
        add(buildSearchBar(), BorderLayout.NORTH);

        JPanel patientDetails = buildPatientDetails();
        JPanel insuranceDetails = buildInsuranceDetails();
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, patientDetails, insuranceDetails);
        split.setResizeWeight(.58);
        split.setDividerSize(2);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        chooserClinicRoom.setName("clinicRoom");
        chooserClinicRoom.setOnSelectListener(name -> {
            if (onClinicRoomChanged != null) onClinicRoomChanged.accept(name);
        });
        chooserService.setOnSelectListener(name -> {
            if (onServiceChanged != null) onServiceChanged.accept(name);
        });
        cbEthnicity.setSelectedItem("Kinh");
        cbNationality.setSelectedItem("Việt Nam");
        cbPayerType.setSelectedItem("Viện phí");
        cbRoute.setSelectedItem("Đúng tuyến");
    }

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        bar.setBackground(new Color(248, 250, 252));
        bar.add(label("Tìm hồ sơ:"));
        bar.add(label("SĐT"));
        bar.add(txtSearchPhone);
        bar.add(label("CCCD"));
        bar.add(txtSearchCccd);
        JButton search = new JButton("Tìm");
        search.setName("searchButton");
        Runnable triggerSearch = this::triggerSearch;
        search.addActionListener(e -> triggerSearch.run());
        txtSearchPhone.addActionListener(e -> triggerSearch.run());
        txtSearchCccd.addActionListener(e -> triggerSearch.run());
        bar.add(search);
        return bar;
    }

    private JPanel buildPatientDetails() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(2, 4, 2, 4));
        panel.setBackground(Color.WHITE);

        JPanel row = row();
        txtPatientCode.setEditable(false);
        row.add(label("Mã BN")); row.add(txtPatientCode);
        row.add(label("Họ tên*")); row.add(txtFullName);
        panel.add(row);

        row = row();
        row.add(label("Giới tính")); row.add(cbGender);
        row.add(label("Ngày sinh*")); row.add(txtDateOfBirth);
        row.add(label("Nơi sinh")); row.add(txtBirthPlace);
        panel.add(row);

        row = row();
        row.add(label("Số nhà/thôn/phố")); row.add(txtStreet);
        row.add(label("Tỉnh/Huyện/Xã")); row.add(txtProvinceCode); row.add(txtDistrictCode); row.add(txtWardCode);
        row.add(label("Địa chỉ")); row.add(txtAddress);
        panel.add(row);

        row = row();
        row.add(label("Điện thoại")); row.add(txtPhone);
        row.add(label("Email")); row.add(txtEmail);
        row.add(label("Nghề nghiệp")); row.add(txtOccupation);
        row.add(label("Dân tộc")); row.add(cbEthnicity);
        row.add(label("Quốc tịch")); row.add(cbNationality);
        panel.add(row);

        row = row();
        row.add(label("CCCD")); row.add(txtCccd);
        row.add(label("Ngày cấp")); row.add(txtCccdIssueDate);
        row.add(label("Nơi cấp")); row.add(txtCccdIssuePlace);
        panel.add(row);

        row = row();
        row.add(label("Nơi làm việc")); row.add(txtWorkplace);
        row.add(label("Người liên hệ")); row.add(txtContactName);
        row.add(label("SĐT liên hệ")); row.add(txtContactPhone);
        panel.add(row);

        row = row();
        row.add(label("Đăng ký")); row.add(cbRegistrationType);
        row.add(label("Đối tượng")); row.add(cbPayerType);
        row.add(chkPriority);
        row.add(label("Lý do")); row.add(txtReason);
        panel.add(row);

        row = row();
        row.add(label("Phòng khám*")); row.add(chooserClinicRoom);
        panel.add(row);
        return panel;
    }

    private JPanel buildInsuranceDetails() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(2, 8, 2, 4));
        panel.setBackground(Color.WHITE);

        JPanel row = row();
        row.add(label("Thông tin BHYT")); row.add(chkBirthCertificate); row.add(chkNotHoldingCard);
        panel.add(row);

        row = row();
        row.add(label("Số thẻ")); row.add(txtInsuranceNumber);
        row.add(label("Nơi sống")); row.add(cbLivingArea);
        panel.add(row);

        row = row();
        row.add(label("Giá trị từ")); row.add(txtInsuranceStartDate);
        row.add(label("Đến")); row.add(txtInsuranceEndDate);
        row.add(label("Mức hưởng %")); row.add(txtInsuranceRate);
        panel.add(row);

        row = row();
        row.add(label("Mã KCBBĐ")); row.add(txtFacilityCode);
        row.add(label("Nơi đăng ký KCB")); row.add(txtFacilityName);
        panel.add(row);

        row = row();
        row.add(label("Địa chỉ trên thẻ")); row.add(txtInsuranceAddress);
        panel.add(row);

        row = row();
        row.add(label("Tuyến KCB")); row.add(cbRoute);
        row.add(chkExemptCopay);
        panel.add(row);

        row = row();
        row.add(label("Ngày miễn cùng chi trả")); row.add(txtCopayExemptDate);
        row.add(label("Đủ 5 năm liên tục")); row.add(txtFiveYearDate);
        panel.add(row);
        return panel;
    }

    public void setClinicRooms(List<PhongKham> rooms) {
        roomsByName.clear();
        if (rooms != null) rooms.forEach(room -> roomsByName.put(room.getTenPhong(), room));
        String[] names = roomsByName.keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER).toArray(String[]::new);
        chooserClinicRoom.setItems(names);
        chooserClinicRoom.setText(names.length == 0 ? "" : names[0]);
        if (onClinicRoomChanged != null && names.length > 0) onClinicRoomChanged.accept(names[0]);
    }

    public void setClinicalServices(List<String> services) {
        String[] values = services == null ? new String[0] : services.toArray(String[]::new);
        chooserService.setItems(values);
        chooserService.setText(values.length == 0 ? "" : values[0]);
    }

    public void setOnClinicRoomChanged(Consumer<String> listener) {
        onClinicRoomChanged = listener;
        if (listener != null && !chooserClinicRoom.getText().isBlank()) listener.accept(chooserClinicRoom.getText());
    }

    public void setOnServiceChanged(Consumer<String> listener) { onServiceChanged = listener; }
    public void setOnSearch(BiConsumer<String, String> listener) { onSearch = listener; }

    public void triggerSearch() {
        if (onSearch != null) onSearch.accept(txtSearchPhone.getText().trim(), txtSearchCccd.getText().trim());
    }

    public String enteredCccd() {
        return txtCccd.getText().trim();
    }

    public String maskedEnteredCccd() {
        return maskCccd(enteredCccd());
    }

    public static String maskCccd(String cccd) {
        if (cccd == null) return "";
        String trimmed = cccd.trim();
        if (trimmed.isEmpty()) return "";
        if (trimmed.length() <= 4) return "****";
        return "***" + trimmed.substring(trimmed.length() - 4);
    }

    public void prepareDuplicateCccdSearch() {
        String cccd = enteredCccd();
        if (!cccd.isEmpty()) {
            txtSearchPhone.setText("");
            txtSearchCccd.setText(cccd);
            txtSearchCccd.requestFocusInWindow();
        } else {
            String phone = txtPhone.getText().trim();
            if (!phone.isEmpty()) txtSearchPhone.setText(phone);
            txtCccd.requestFocusInWindow();
        }
    }

    public void searchByEnteredCccd() {
        prepareDuplicateCccdSearch();
        triggerSearch();
    }

    public boolean hasUnsavedInput() {
        return !txtFullName.getText().isBlank()
                || !txtDateOfBirth.getText().isBlank()
                || !txtPhone.getText().isBlank()
                || !txtCccd.getText().isBlank()
                || !txtAddress.getText().isBlank()
                || !txtStreet.getText().isBlank()
                || !txtBirthPlace.getText().isBlank()
                || !txtReason.getText().isBlank()
                || !txtInsuranceNumber.getText().isBlank();
    }

    public String selectedClinicRoomName() { return chooserClinicRoom.getText().trim(); }

    public int selectedClinicRoomId() {
        PhongKham room = roomsByName.get(selectedClinicRoomName());
        return room == null ? 0 : room.getId();
    }

    public void selectPatient(BenhNhan patient, BaoHiem insurance) {
        selectedPatient = patient;
        selectedInsurance = insurance;
        txtPatientCode.setText(value(patient.getMaBenhNhanFormat()));
        txtFullName.setText(patient.getHoTen());
        GioiTinh gender = patient.getGioiTinh();
        cbGender.setSelectedItem(gender == GioiTinh.F ? "Nữ" : gender == GioiTinh.O ? "Khác" : "Nam");
        txtDateOfBirth.setText(format(patient.getNgaySinh()));
        txtBirthPlace.setText(value(patient.getNoiSinh()));
        txtStreet.setText(value(patient.getSoNhaThonPho()));
        txtProvinceCode.setText(value(patient.getMaTinh()));
        txtDistrictCode.setText(value(patient.getMaHuyen()));
        txtWardCode.setText(value(patient.getMaXa()));
        txtAddress.setText(value(patient.getDiaChi()));
        txtOccupation.setText(value(patient.getNgheNghiep()));
        txtPhone.setText(value(patient.getDienThoai()));
        txtEmail.setText(value(patient.getEmail()));
        cbEthnicity.setSelectedItem(valueOr(patient.getDanToc(), "Kinh"));
        cbNationality.setSelectedItem(valueOr(patient.getQuocTich(), "Việt Nam"));
        txtCccd.setText(value(patient.getSoCCCD()));
        txtCccdIssueDate.setText(format(patient.getNgayCapCCCD()));
        txtCccdIssuePlace.setText(value(patient.getNoiCapCCCD()));
        txtWorkplace.setText(value(patient.getNoiLamViec()));
        txtContactName.setText(value(patient.getNguoiLienHeTen()));
        txtContactPhone.setText(value(patient.getNguoiLienHeDienThoai()));
        if (insurance == null) clearInsuranceFields(); else loadInsurance(insurance);
    }

    public void clearForm() {
        selectedPatient = null;
        selectedInsurance = null;
        txtPatientCode.setText(""); txtFullName.setText(""); cbGender.setSelectedItem("Nam");
        txtDateOfBirth.setText(""); txtBirthPlace.setText(""); txtStreet.setText("");
        txtProvinceCode.setText(""); txtDistrictCode.setText(""); txtWardCode.setText(""); txtAddress.setText("");
        txtOccupation.setText(""); txtPhone.setText(""); txtEmail.setText(""); txtCccd.setText("");
        txtCccdIssueDate.setText(""); txtCccdIssuePlace.setText(""); txtWorkplace.setText("");
        txtContactName.setText(""); txtContactPhone.setText(""); txtReason.setText(""); chkPriority.setSelected(false);
        cbPayerType.setSelectedItem("Viện phí"); cbRegistrationType.setSelectedItem("Khám bệnh");
        clearInsuranceFields();
    }

    public RegisterPatientCommand toRegisterCommand(AuthorizationContext authorizationContext) {
        if (authorizationContext == null || !authorizationContext.isValid()) {
            throw new IllegalArgumentException("An active authenticated session is required.");
        }
        int userId = authorizationContext.employeeId();
        BenhNhan patient = new BenhNhan();
        String fullName = txtFullName.getText().trim().replaceAll("\\s+", " ");
        int split = fullName.lastIndexOf(' ');
        if (split < 0) { patient.setHo(fullName); patient.setTen(""); }
        else { patient.setHo(fullName.substring(0, split)); patient.setTen(fullName.substring(split + 1)); }
        patient.setNgaySinh(parseDate(txtDateOfBirth, "Ngày sinh"));
        patient.setGioiTinh(switch (String.valueOf(cbGender.getSelectedItem())) {
            case "Nữ" -> GioiTinh.F; case "Khác" -> GioiTinh.O; default -> GioiTinh.M;
        });
        patient.setDienThoai(txtPhone.getText().trim());
        patient.setEmail(blankToNull(txtEmail.getText()));
        patient.setSoCCCD(blankToNull(txtCccd.getText()));
        patient.setDiaChi(blankToNull(txtAddress.getText()));
        patient.setSoNhaThonPho(blankToEmpty(txtStreet.getText()));
        patient.setNoiSinh(blankToEmpty(txtBirthPlace.getText()));
        patient.setNgayCapCCCD(parseOptionalDate(txtCccdIssueDate, "Ngày cấp CCCD"));
        patient.setNoiCapCCCD(blankToEmpty(txtCccdIssuePlace.getText()));
        patient.setNoiLamViec(blankToEmpty(txtWorkplace.getText()));
        patient.setNgheNghiep(blankToEmpty(txtOccupation.getText()));
        patient.setDanToc(valueOr(String.valueOf(cbEthnicity.getSelectedItem()), "Kinh"));
        patient.setQuocTich(valueOr(String.valueOf(cbNationality.getSelectedItem()), "Việt Nam"));
        patient.setMaTinh(blankToNull(txtProvinceCode.getText()));
        patient.setMaHuyen(blankToNull(txtDistrictCode.getText()));
        patient.setMaXa(blankToNull(txtWardCode.getText()));
        patient.setNguoiLienHeTen(blankToNull(txtContactName.getText()));
        patient.setNguoiLienHeDienThoai(blankToNull(txtContactPhone.getText()));
        patient.setCreatedBy(userId > 0 ? userId : null);
        if (selectedPatient != null) {
            patient.setId(selectedPatient.getId());
            patient.setMaBenhNhanFormat(selectedPatient.getMaBenhNhanFormat());
            patient.setVersion(selectedPatient.getVersion());
            patient.setCreatedAt(selectedPatient.getCreatedAt());
            patient.setCreatedBy(selectedPatient.getCreatedBy());
            patient.setUpdatedBy(userId > 0 ? userId : null);
        }

        BaoHiem insurance = toInsurance(userId);
        DoiTuongKham payer = switch (String.valueOf(cbPayerType.getSelectedItem())) {
            case "BHYT" -> DoiTuongKham.BHYT;
            case "Dịch vụ theo yêu cầu" -> DoiTuongKham.DICH_VU_YEU_CAU;
            default -> DoiTuongKham.THU_PHI;
        };
        PhongKham room = roomsByName.get(selectedClinicRoomName());
        if (room == null) throw new IllegalArgumentException("Chọn phòng khám từ danh sách đã tải.");
        return new RegisterPatientCommand(patient, insurance, room.getId(),
                String.valueOf(cbRegistrationType.getSelectedItem()), txtReason.getText().trim(), payer,
                chkPriority.isSelected(), userId, authorizationContext);
    }

    private BaoHiem toInsurance(int userId) {
        String number = txtInsuranceNumber.getText().trim();
        if (number.isBlank()) return null;
        BaoHiem card = new BaoHiem();
        card.setSoBaoHiem(number);
        card.setNgayBatDau(parseOptionalDate(txtInsuranceStartDate, "Ngày bắt đầu thẻ BHYT"));
        card.setNgayHetHan(parseOptionalDate(txtInsuranceEndDate, "Hạn thẻ BHYT"));
        card.setMucHuong(parseInteger(txtInsuranceRate, 0, "Mức hưởng BHYT"));
        card.setMaNoiSong(valueOr(String.valueOf(cbLivingArea.getSelectedItem()), ""));
        card.setMaNoiDangKyKCB(blankToEmpty(txtFacilityCode.getText()));
        card.setNoiDangKyKCB(blankToEmpty(txtFacilityName.getText()));
        card.setDiaChiThe(blankToEmpty(txtInsuranceAddress.getText()));
        card.setDungTuyen("Đúng tuyến".equals(cbRoute.getSelectedItem()));
        card.setCoGiayKhaiSinh(chkBirthCertificate.isSelected());
        card.setKhongGiuThe(chkNotHoldingCard.isSelected());
        card.setMienCungChiTra(chkExemptCopay.isSelected());
        card.setNgayMienCungChiTra(parseOptionalDate(txtCopayExemptDate, "Ngày miễn cùng chi trả"));
        card.setNgayDu5NamLienTuc(parseOptionalDate(txtFiveYearDate, "Ngày đủ 5 năm liên tục"));
        card.setCreatedBy(userId > 0 ? userId : null);
        if (selectedInsurance != null) {
            card.setId(selectedInsurance.getId());
            card.setVersion(selectedInsurance.getVersion());
            card.setCreatedAt(selectedInsurance.getCreatedAt());
            card.setCreatedBy(selectedInsurance.getCreatedBy());
            card.setUpdatedBy(userId > 0 ? userId : null);
        }
        return card;
    }

    private void loadInsurance(BaoHiem insurance) {
        txtInsuranceNumber.setText(value(insurance.getSoBaoHiem()));
        txtInsuranceStartDate.setText(format(insurance.getNgayBatDau()));
        txtInsuranceEndDate.setText(format(insurance.getNgayHetHan()));
        txtInsuranceRate.setText(String.valueOf(insurance.getMucHuong()));
        cbLivingArea.setSelectedItem(value(insurance.getMaNoiSong()));
        txtFacilityCode.setText(value(insurance.getMaNoiDangKyKCB()));
        txtFacilityName.setText(value(insurance.getNoiDangKyKCB()));
        txtInsuranceAddress.setText(value(insurance.getDiaChiThe()));
        cbRoute.setSelectedItem(insurance.isDungTuyen() ? "Đúng tuyến" : "Trái tuyến");
        chkBirthCertificate.setSelected(insurance.isCoGiayKhaiSinh());
        chkNotHoldingCard.setSelected(insurance.isKhongGiuThe());
        chkExemptCopay.setSelected(insurance.isMienCungChiTra());
        txtCopayExemptDate.setText(format(insurance.getNgayMienCungChiTra()));
        txtFiveYearDate.setText(format(insurance.getNgayDu5NamLienTuc()));
    }

    private void clearInsuranceFields() {
        txtInsuranceNumber.setText(""); txtInsuranceStartDate.setText(""); txtInsuranceEndDate.setText("");
        txtInsuranceRate.setText(""); cbLivingArea.setSelectedItem(""); txtFacilityCode.setText("");
        txtFacilityName.setText(""); txtInsuranceAddress.setText(""); cbRoute.setSelectedItem("Đúng tuyến");
        chkBirthCertificate.setSelected(false); chkNotHoldingCard.setSelected(false); chkExemptCopay.setSelected(false);
        txtCopayExemptDate.setText(""); txtFiveYearDate.setText(""); selectedInsurance = null;
    }

    private static JPanel row() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static JLabel label(String value) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        return label;
    }

    private static JTextField input(String name, int width) {
        JTextField field = input(width);
        field.setName(name);
        return field;
    }

    private static JTextField input(int width) {
        JTextField field = new JTextField();
        field.setFont(new Font("SansSerif", Font.PLAIN, 12));
        field.setPreferredSize(new Dimension(width, 21));
        field.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(175, 188, 202), 1), new EmptyBorder(1, 4, 1, 4)));
        return field;
    }

    private static LocalDate parseDate(JTextField field, String label) {
        LocalDate date = parseOptionalDate(field, label);
        if (date == null) throw new IllegalArgumentException(label + " là bắt buộc (dd/MM/yyyy).");
        return date;
    }

    private static LocalDate parseOptionalDate(JTextField field, String label) {
        String value = field.getText().trim();
        if (value.isBlank()) return null;
        try { return LocalDate.parse(value, DATE_FORMAT); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException(label + " phải theo định dạng dd/MM/yyyy."); }
    }

    private static int parseInteger(JTextField field, int fallback, String label) {
        String value = field.getText().trim();
        if (value.isBlank()) return fallback;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(label + " phải là số nguyên."); }
    }

    private static String format(LocalDate date) { return date == null ? "" : date.format(DATE_FORMAT); }
    private static String value(String value) { return value == null ? "" : value; }
    private static String valueOr(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String blankToEmpty(String value) { return value == null ? "" : value.trim(); }
}
