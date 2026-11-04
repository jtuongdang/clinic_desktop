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
        lblMaBN = createValueLabel("18039327", true);
        lblTenBN = createValueLabel("TEST BN 033", true);
        lblGioiTinh = createValueLabel("Nam", false);
        addRow3(c, row++, "Mã BN:", lblMaBN, "Tên bệnh nhân:", lblTenBN, "Giới tính:", lblGioiTinh);

        // Hàng 2: Ngày sinh | NS | Nghề nghiệp
        lblNgaySinh = createValueLabel("01/02/1995", false);
        lblNS = createValueLabel("1995", false);
        lblNgheNghiep = createValueLabel("Nhân dân", false);
        addRow3(c, row++, "Ngày sinh:", lblNgaySinh, "NS:", lblNS, "Nghề nghiệp:", lblNgheNghiep);

        // Hàng 3: Dân tộc | Quốc tịch | Điện thoại | Nhóm máu
        lblDanToc = createValueLabel("Kinh", false);
        lblQuocTich = createValueLabel("Việt Nam", true);
        JLabel lblDienThoai = createValueLabel("", false);
        addRow3(c, row++, "Dân tộc:", lblDanToc, "Quốc tịch:", lblQuocTich, "Điện thoại:", lblDienThoai);

        // Hàng 4: Địa chỉ | Nơi làm việc
        lblDiaChi = createValueLabel("Xã Việt Hồng, H. Thanh Hà, Hải Dương", true);
        JLabel lblNoiLamViec = createValueLabel("", false);
        addRow2(c, row++, "Địa chỉ:", lblDiaChi, "Nơi làm việc:", lblNoiLamViec);

        // Hàng 5: Người nhà | Đón tiếp lúc | Loại đón tiếp
        JLabel lblNguoiNha = createValueLabel("", false);
        lblDonTiepLuc = createValueLabel("08:26 07/11/2018", true);
        lblLoaiDonTiep = createValueLabel("Khám bệnh", true);
        addRow3(c, row++, "Người nhà:", lblNguoiNha, "Đón tiếp lúc:", lblDonTiepLuc, "Loại đón tiếp:", lblLoaiDonTiep);

        // Hàng 6: Đối tượng | Lý do khám | Số BHYT | Mức hưởng
        lblDoiTuong = createValueLabel("Bảo hiểm", false);
        lblSoBHYT = createValueLabel("GD-4-21-21-212-12125", true);
        lblMucHuong = createValueLabel("80%", false);
        addRow3(c, row++, "Đối tượng:", lblDoiTuong, "Số BHYT:", lblSoBHYT, "Mức hưởng:", lblMucHuong);

        // Hàng 7: Nơi KCBBĐ | Hạn thẻ | Tuyến BHYT
        lblNoiKCBBandau = createValueLabel("30001 - Bệnh viện đa khoa thành phố Hải Dương", false);
        lblHanThe = createValueLabel("01/01/18 - 31/12/18", true);
        lblTuyenBHYT = createValueLabel("Đúng tuyến", true);
        addRow3(c, row++, "Nơi KCBBĐ:", lblNoiKCBBandau, "Hạn thẻ:", lblHanThe, "Tuyến BHYT:", lblTuyenBHYT);

        // Hàng 8: Mã bệnh án | Số vào viện | Số vào khoa | Mã điều trị
        lblMaBenhAn = createValueLabel("18KNGDC001599", true);
        lblSoVaoVien = createValueLabel("028850/18", true);
        JLabel lblMaDieuTri = createValueLabel("346777", false);
        addRow3(c, row++, "Mã bệnh án:", lblMaBenhAn, "Số vào viện:", lblSoVaoVien, "Mã điều trị:", lblMaDieuTri);

        // Hàng 9: Khoa phòng | Bác sĩ điều trị | Bệnh án
        lblKhoaPhong = createValueLabel("Ngoại Đầu cổ > P.Điều Trị Ngoại DC", true);
        lblBacSiDieuTri = createValueLabel("Lê Tiến Quân", true);
        JLabel lblBenhAn = createValueLabel("Ung bướu", false);
        addRow3(c, row++, "Khoa phòng:", lblKhoaPhong, "Bác sĩ điều trị:", lblBacSiDieuTri, "Bệnh án:", lblBenhAn);

        // Hàng 10: Ngày vào | Ngày ra | Chẩn đoán
        lblNgayVao = createValueLabel("09:40 11/11/2018", true);
        JLabel lblNgayRa = createValueLabel("", false);
        lblChanDoan = createValueLabel("D21-Bướu lành khác của mô liên kết và mô mềm khác", true);
        lblChanDoan.setFont(AppFonts.BODY_BOLD);
        addRow3(c, row++, "Ngày vào:", lblNgayVao, "Ngày ra:", lblNgayRa, "Chẩn đoán:", lblChanDoan);

        // Hàng 11: Bệnh kèm theo | Xử trí
        JLabel lblBenhKemTheo = createValueLabel("", false);
        JLabel lblXuTri = createValueLabel("", false);
        addRow2(c, row++, "Bệnh kèm theo:", lblBenhKemTheo, "Xử trí:", lblXuTri);
    }

    public void updatePatient(PatientExamDto p) {
        if (p == null) return;
        lblMaBN.setText(p.getShortCode());
        lblTenBN.setText(p.getName());
        lblGioiTinh.setText(p.getGender());
        lblNgaySinh.setText("01/02/" + p.getBirthYear());
        lblNS.setText(p.getBirthYear());
        lblDiaChi.setText(p.getAddress());
        lblSoBHYT.setText(p.getBhytCode());
        lblMucHuong.setText(p.getBhytRate());
        lblKhoaPhong.setText(p.getDepartment());
        lblBacSiDieuTri.setText(p.getDoctor());
        lblNgayVao.setText(p.getAdmissionDate());
        lblChanDoan.setText(p.getDiagnosis());
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
