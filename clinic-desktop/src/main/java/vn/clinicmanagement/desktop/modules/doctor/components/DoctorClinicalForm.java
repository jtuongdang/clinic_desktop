package vn.clinicmanagement.desktop.modules.doctor.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.modules.doctor.model.PatientExamDto;

import javax.swing.*;
import java.awt.*;

/**
 * Lưới thông tin hành chính, bảo hiểm y tế và chẩn đoán lâm sàng bệnh nhân
 */
public class DoctorClinicalForm extends JPanel {

    private final JLabel lblMaBN;
    private final JLabel lblTenBN;
    private final JLabel lblGioiTinh;
    private final JLabel lblNgaySinh;
    private final JLabel lblNS;
    private final JLabel lblNgheNghiep;
    private final JLabel lblDanToc;
    private final JLabel lblQuocTich;
    private final JLabel lblDiaChi;
    private final JLabel lblDonTiepLuc;
    private final JLabel lblLoaiDonTiep;
    private final JLabel lblDoiTuong;
    private final JLabel lblSoBHYT;
    private final JLabel lblMucHuong;
    private final JLabel lblNoiKCBBandau;
    private final JLabel lblHanThe;
    private final JLabel lblTuyenBHYT;
    private final JLabel lblMaBenhAn;
    private final JLabel lblSoVaoVien;
    private final JLabel lblKhoaPhong;
    private final JLabel lblBacSiDieuTri;
    private final JLabel lblNgayVao;
    private final JLabel lblChanDoan;

    public DoctorClinicalForm() {
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 4, 2, 4);

        int row = 0;

        // Hàng 1: Mã BN | Tên BN | Giới tính
        lblMaBN = createValueLabel("-", true);
        lblTenBN = createValueLabel("Chưa chọn bệnh nhân", true);
        lblGioiTinh = createValueLabel("-", false);
        addRow3(c, row++, "Mã BN:", lblMaBN, "Tên bệnh nhân:", lblTenBN, "Giới tính:", lblGioiTinh);

        // Hàng 2: Ngày sinh | NS | Nghề nghiệp
        lblNgaySinh = createValueLabel("-", false);
        lblNS = createValueLabel("-", false);
        lblNgheNghiep = createValueLabel("-", false);
        addRow3(c, row++, "Ngày sinh:", lblNgaySinh, "NS:", lblNS, "Nghề nghiệp:", lblNgheNghiep);

        // Hàng 3: Dân tộc | Quốc tịch | Điện thoại | Nhóm máu
        lblDanToc = createValueLabel("-", false);
        lblQuocTich = createValueLabel("Việt Nam", true);
        JLabel lblDienThoai = createValueLabel("", false);
        addRow3(c, row++, "Dân tộc:", lblDanToc, "Quốc tịch:", lblQuocTich, "Điện thoại:", lblDienThoai);

        // Hàng 4: Địa chỉ | Nơi làm việc
        lblDiaChi = createValueLabel("-", true);
        JLabel lblNoiLamViec = createValueLabel("", false);
        addRow2(c, row++, "Địa chỉ:", lblDiaChi, "Nơi làm việc:", lblNoiLamViec);

        // Hàng 5: Người nhà | Đón tiếp lúc | Loại đón tiếp
        JLabel lblNguoiNha = createValueLabel("", false);
        lblDonTiepLuc = createValueLabel("-", true);
        lblLoaiDonTiep = createValueLabel("Khám bệnh", true);
        addRow3(c, row++, "Người nhà:", lblNguoiNha, "Đón tiếp lúc:", lblDonTiepLuc, "Loại đón tiếp:", lblLoaiDonTiep);

        // Hàng 6: Đối tượng | Lý do khám | Số BHYT | Mức hưởng
        lblDoiTuong = createValueLabel("-", false);
        lblSoBHYT = createValueLabel("-", true);
        lblMucHuong = createValueLabel("-", false);
        addRow3(c, row++, "Đối tượng:", lblDoiTuong, "Số BHYT:", lblSoBHYT, "Mức hưởng:", lblMucHuong);

        // Hàng 7: Nơi KCBBĐ | Hạn thẻ | Tuyến BHYT
        lblNoiKCBBandau = createValueLabel("-", false);
        lblHanThe = createValueLabel("-", true);
        lblTuyenBHYT = createValueLabel("-", true);
        addRow3(c, row++, "Nơi KCBBĐ:", lblNoiKCBBandau, "Hạn thẻ:", lblHanThe, "Tuyến BHYT:", lblTuyenBHYT);

        // Hàng 8: Mã bệnh án | Số vào viện | Số vào khoa | Mã điều trị
        lblMaBenhAn = createValueLabel("-", true);
        lblSoVaoVien = createValueLabel("-", true);
        JLabel lblMaDieuTri = createValueLabel("-", false);
        addRow3(c, row++, "Mã bệnh án:", lblMaBenhAn, "Số vào viện:", lblSoVaoVien, "Mã điều trị:", lblMaDieuTri);

        // Hàng 9: Khoa phòng | Bác sĩ điều trị | Bệnh án
        lblKhoaPhong = createValueLabel("-", true);
        lblBacSiDieuTri = createValueLabel("-", true);
        JLabel lblBenhAn = createValueLabel("-", false);
        addRow3(c, row++, "Khoa phòng:", lblKhoaPhong, "Bác sĩ điều trị:", lblBacSiDieuTri, "Bệnh án:", lblBenhAn);

        // Hàng 10: Ngày vào | Ngày ra | Chẩn đoán
        lblNgayVao = createValueLabel("-", true);
        JLabel lblNgayRa = createValueLabel("", false);
        lblChanDoan = createValueLabel("-", true);
        lblChanDoan.setFont(AppFonts.BODY_BOLD);
        addRow3(c, row++, "Ngày vào:", lblNgayVao, "Ngày ra:", lblNgayRa, "Chẩn đoán:", lblChanDoan);

        // Hàng 11: Bệnh kèm theo | Xử trí
        JLabel lblBenhKemTheo = createValueLabel("", false);
        JLabel lblXuTri = createValueLabel("", false);
        addRow2(c, row++, "Bệnh kèm theo:", lblBenhKemTheo, "Xử trí:", lblXuTri);
    }

    public void updatePatient(PatientExamDto p) {
        if (p == null) {
            lblMaBN.setText("-");
            lblTenBN.setText("Chưa chọn bệnh nhân");
            lblGioiTinh.setText("-");
            lblNgaySinh.setText("-");
            lblNS.setText("-");
            lblDiaChi.setText("-");
            lblSoBHYT.setText("-");
            lblMucHuong.setText("-");
            lblKhoaPhong.setText("-");
            lblBacSiDieuTri.setText("-");
            lblNgayVao.setText("-");
            lblChanDoan.setText("-");
            return;
        }
        lblMaBN.setText(p.getShortCode() != null && !p.getShortCode().isBlank() ? p.getShortCode() : "-");
        lblTenBN.setText(p.getName() != null && !p.getName().isBlank() ? p.getName() : "-");
        lblGioiTinh.setText(p.getGender() != null && !p.getGender().isBlank() ? p.getGender() : "-");
        lblNgaySinh.setText(p.getBirthYear() != null && !p.getBirthYear().isBlank() ? p.getBirthYear() : "-");
        lblNS.setText(p.getBirthYear() != null && !p.getBirthYear().isBlank() ? p.getBirthYear() : "-");
        lblDiaChi.setText(p.getAddress() != null && !p.getAddress().isBlank() ? p.getAddress() : "-");
        lblSoBHYT.setText(p.getBhytCode() != null && !p.getBhytCode().isBlank() ? p.getBhytCode() : "-");
        lblMucHuong.setText(p.getBhytRate() != null && !p.getBhytRate().isBlank() ? p.getBhytRate() : "-");
        lblKhoaPhong.setText(p.getDepartment() != null && !p.getDepartment().isBlank() ? p.getDepartment() : "-");
        lblBacSiDieuTri.setText(p.getDoctor() != null && !p.getDoctor().isBlank() ? p.getDoctor() : "-");
        lblNgayVao.setText(p.getAdmissionDate() != null && !p.getAdmissionDate().isBlank() ? p.getAdmissionDate() : "-");
        lblChanDoan.setText(p.getDiagnosis() != null && !p.getDiagnosis().isBlank() ? p.getDiagnosis() : "-");
    }

    private void addRow3(GridBagConstraints c, int row,
                         String l1, JComponent v1, String l2, JComponent v2, String l3, JComponent v3) {
        c.gridy = row;

        c.gridx = 0; c.weightx = 0;
        add(createFieldTitle(l1), c);
        c.gridx = 1; c.weightx = 0.35;
        add(v1, c);

        c.gridx = 2; c.weightx = 0;
        add(createFieldTitle(l2), c);
        c.gridx = 3; c.weightx = 0.35;
        add(v2, c);

        c.gridx = 4; c.weightx = 0;
        add(createFieldTitle(l3), c);
        c.gridx = 5; c.weightx = 0.30;
        add(v3, c);
    }

    private void addRow2(GridBagConstraints c, int row,
                         String l1, JComponent v1, String l2, JComponent v2) {
        c.gridy = row;

        c.gridx = 0; c.weightx = 0;
        add(createFieldTitle(l1), c);
        c.gridx = 1; c.gridwidth = 3; c.weightx = 0.70;
        add(v1, c);

        c.gridx = 4; c.gridwidth = 1; c.weightx = 0;
        add(createFieldTitle(l2), c);
        c.gridx = 5; c.weightx = 0.30;
        add(v2, c);

        c.gridwidth = 1; // reset
    }

    private JLabel createFieldTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppFonts.BODY);
        lbl.setForeground(new Color(30, 30, 30));
        return lbl;
    }

    private JLabel createValueLabel(String text, boolean isLink) {
        JLabel lbl = new JLabel(text != null && !text.isEmpty() ? text : " ");
        lbl.setFont(AppFonts.BODY);
        if (isLink) {
            lbl.setForeground(new Color(15, 65, 145)); // Navy link
            lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            lbl.setForeground(new Color(20, 20, 20));
        }
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 205, 215)));
        return lbl;
    }
}
