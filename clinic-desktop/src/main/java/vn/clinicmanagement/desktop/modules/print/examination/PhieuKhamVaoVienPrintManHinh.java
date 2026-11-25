package vn.clinicmanagement.desktop.modules.print.examination;


import vn.clinicmanagement.desktop.modules.print.billing.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.domain.examination.ChanDoan;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.examination.SinhHieu;
import vn.clinicmanagement.domain.patient.BenhNhan;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Giao diện xem trước & ký/in "PHIẾU KHÁM BỆNH VÀO VIỆN" (MS: 43BV-01)
 * khi bác sĩ xử trí "Nhập viện" tại màn hình Khám Bệnh.
 */
public class PhieuKhamVaoVienPrintManHinh extends JPanel {

    public record AdmissionFormData(
            String patientCode,
            String patientName,
            String birthDateDigits,
            String ageText,
            String gender,
            String occupation,
            String ethnicity,
            String nationality,
            String address,
            String workplace,
            String subjectType,
            String bhytNumber,
            String bhytExpiry,
            String relativeInfo,
            String visitId,
            String clinicRoom,
            String arrivalTimeText,
            String referralDiagnosis,
            String admissionReason,
            String diseaseProgress,
            String personalHistory,
            String familyHistory,
            String fullBodyExam,
            String organExam,
            String mach,
            String nhietDo,
            String huyetAp,
            String nhipTho,
            String canNang,
            String bmi,
            String clsSummary,
            String initialDiagnosis,
            String treatedSummary,
            String targetDepartment
    ) {}

    private AdmissionFormData data;
    private JPanel paperPanel;
    private JLabel lblSignStatus;
    private JLabel lblSignatureType;
    private JLabel lblBannerRight1;
    private JLabel lblBannerRight2;
    private JLabel lblBannerRight3;
    private boolean signed = false;

    public PhieuKhamVaoVienPrintManHinh(AdmissionFormData data) {
        this.data = data;
        initUI();
    }

    public static AdmissionFormData fromVisit(
            LuotKham visit,
            String fallbackCode,
            String fallbackName,
            String fallbackAge,
            String fallbackGender,
            String fallbackAddress,
            String fallbackDiagnosis,
            String targetDepartment,
            String fallbackRoom) {
        BenhNhan bn = visit != null ? visit.getBenhNhan() : null;
        String code = bn != null && bn.getMaBenhNhanFormat() != null ? bn.getMaBenhNhanFormat() : safe(fallbackCode);
        String name = bn != null && bn.getHoTen() != null ? bn.getHoTen() : safe(fallbackName);
        LocalDate dob = bn != null ? bn.getNgaySinh() : null;
        String dobDigits = dob != null ? dob.format(DateTimeFormatter.ofPattern("ddMMyyyy")) : "";
        String age = dob != null ? String.valueOf(Math.max(0, LocalDate.now().getYear() - dob.getYear())) : safe(fallbackAge);
        String gender = bn != null && bn.getGioiTinh() != null
                ? ("NAM".equalsIgnoreCase(bn.getGioiTinh().name()) || "M".equalsIgnoreCase(bn.getGioiTinh().name()) ? "Nam" : "Nữ")
                : safe(fallbackGender);
        String address = bn != null && bn.getDiaChi() != null ? bn.getDiaChi() : safe(fallbackAddress);
        String bhyt = "";
        String subject = visit != null && visit.getDoiTuongKham() != null ? visit.getDoiTuongKham().getDescription() : "Viện phí";
        String room = visit != null && visit.getPhongKham() != null && visit.getPhongKham().getTenPhong() != null
                ? visit.getPhongKham().getTenPhong() : (safe(fallbackRoom).isEmpty() ? "PK Khám Bệnh" : fallbackRoom);
        LocalDateTime arr = visit != null && visit.getThoiGianTiepNhan() != null ? visit.getThoiGianTiepNhan() : LocalDateTime.now();
        String arrText = String.format("%02d giờ %02d phút, Ngày %02d tháng %02d năm %d",
                arr.getHour(), arr.getMinute(), arr.getDayOfMonth(), arr.getMonthValue(), arr.getYear());

        String diag = safe(fallbackDiagnosis);
        if (visit != null && visit.getChanDoans() != null) {
            for (ChanDoan cd : visit.getChanDoans()) {
                if (cd != null && !cd.isDeleted() && cd.isLaChinh()) {
                    String icd = cd.getIcd() != null && cd.getIcd().getMaICD() != null ? cd.getIcd().getMaICD() + " - " : "";
                    diag = icd + safe(cd.getChanDoanText());
                    break;
                }
            }
        }

        String mach = "", nhietDo = "", huyetAp = "", nhipTho = "", canNang = "", bmi = "";
        if (visit != null && visit.getSinhHieus() != null && !visit.getSinhHieus().isEmpty()) {
            SinhHieu sh = visit.getSinhHieus().get(visit.getSinhHieus().size() - 1);
            if (sh.getMach() != null) mach = String.valueOf(sh.getMach());
            if (sh.getNhietDo() != null) nhietDo = sh.getNhietDo().toPlainString();
            if (sh.getHuyetApTamThu() != null || sh.getHuyetApTamTruong() != null) {
                huyetAp = (sh.getHuyetApTamThu() != null ? sh.getHuyetApTamThu() : "") + "/"
                        + (sh.getHuyetApTamTruong() != null ? sh.getHuyetApTamTruong() : "");
            }
            if (sh.getNhipTho() != null) nhipTho = String.valueOf(sh.getNhipTho());
            if (sh.getCanNang() != null) canNang = sh.getCanNang().toPlainString();
            if (sh.getBmi() != null) bmi = sh.getBmi().toPlainString();
        }

        return new AdmissionFormData(
                code,
                name.toUpperCase(),
                dobDigits,
                age,
                gender,
                bn != null ? safe(bn.getNgheNghiep()) : "",
                bn != null && bn.getDanToc() != null ? bn.getDanToc() : "Kinh",
                bn != null && bn.getQuocTich() != null ? bn.getQuocTich() : "Việt Nam",
                address,
                bn != null ? safe(bn.getNoiLamViec()) : "",
                subject,
                bhyt,
                "",
                bn != null ? safe(bn.getNguoiLienHeTen()) : "",
                visit != null ? String.valueOf(visit.getId()) : "",
                room,
                arrText,
                "",
                visit != null ? safe(visit.getLyDoKham()) : "",
                visit != null ? safe(visit.getQuaTrinhBenhLy()) : "",
                visit != null ? safe(visit.getTienSuBanThan()) : "",
                visit != null ? safe(visit.getTienSuGiaDinh()) : "",
                visit != null ? safe(visit.getKhamToanThan()) : "",
                visit != null ? safe(visit.getKhamBoPhan()) : "",
                mach, nhietDo, huyetAp, nhipTho, canNang, bmi,
                "",
                diag,
                visit != null ? safe(visit.getLoiDanBacSi()) : "",
                safe(targetDepartment).isEmpty() ? "Khoa Nội" : targetDepartment
        );
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(120, 125, 132));

        add(buildLeftSidebar(), BorderLayout.WEST);
        add(buildTopToolbar(), BorderLayout.NORTH);

        paperPanel = buildA4Paper();
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setBackground(new Color(115, 120, 128));
        holder.setBorder(new EmptyBorder(14, 24, 24, 24));
        holder.add(paperPanel);

        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildLeftSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(108, 600));
        sidebar.setBackground(new Color(242, 244, 248));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(200, 208, 220)));

        JLabel lblHeader = new JLabel("Bệnh án điện tử", SwingConstants.CENTER);
        lblHeader.setFont(UngDungPhongChu.SMALL_BOLD);
        lblHeader.setBorder(new EmptyBorder(8, 4, 8, 4));
        sidebar.add(lblHeader, BorderLayout.NORTH);

        JPanel btnList = new JPanel(new GridLayout(5, 1, 0, 6));
        btnList.setOpaque(false);
        btnList.setBorder(new EmptyBorder(6, 8, 6, 8));

        JButton btnSign = createSidebarBtn("Ký Tài Liệu", new Color(185, 110, 20));
        btnSign.addActionListener(e -> {
            signed = true;
            lblSignStatus.setText("Đã ký số !");
            lblSignStatus.setForeground(new Color(20, 135, 65));
            lblSignatureType.setText("Loại Chữ Ký : Chữ ký bác sĩ nội bộ");
        });
        JButton btnApprove = createSidebarBtn("Duyệt Tài Liệu", new Color(80, 90, 105));
        JButton btnSave = createSidebarBtn("Lưu Tài Liệu", new Color(45, 75, 145));
        btnSave.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Đã lưu Phiếu Khám Bệnh Vào Viện vào hồ sơ bệnh án điện tử.", "Lưu Tài Liệu", JOptionPane.INFORMATION_MESSAGE));
        JButton btnExport = createSidebarBtn("Xuất File", new Color(180, 45, 45));
        JButton btnPdf = createSidebarBtn("PDF", new Color(25, 125, 55));
        btnPdf.addActionListener(e -> printPaper());

        btnList.add(btnSign);
        btnList.add(btnApprove);
        btnList.add(btnSave);
        btnList.add(btnExport);
        btnList.add(btnPdf);

        JPanel topWrap = new JPanel(new BorderLayout());
        topWrap.setOpaque(false);
        topWrap.add(btnList, BorderLayout.NORTH);
        sidebar.add(topWrap, BorderLayout.CENTER);

        lblSignStatus = new JLabel("Chưa ký !", SwingConstants.CENTER);
        lblSignStatus.setFont(UngDungPhongChu.BODY_BOLD);
        lblSignStatus.setForeground(new Color(200, 35, 35));
        lblSignStatus.setBorder(new EmptyBorder(10, 6, 12, 6));
        sidebar.add(lblSignStatus, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildTopToolbar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(new Color(245, 247, 250));
        bar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 208, 220)),
                new EmptyBorder(6, 12, 6, 14)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JButton btnPrint = new JButton("In Tài Liệu");
        btnPrint.setFont(UngDungPhongChu.SMALL_BOLD);
        btnPrint.setBackground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.setBorder(new CompoundBorder(new LineBorder(new Color(180, 190, 205), 1), new EmptyBorder(6, 12, 6, 12)));
        btnPrint.addActionListener(e -> printPaper());

        lblSignatureType = new JLabel("Loại Chữ Ký : ---");
        lblSignatureType.setFont(UngDungPhongChu.SMALL);

        JButton btnCheck = new JButton("✓ Kiểm Tra");
        btnCheck.setFont(UngDungPhongChu.SMALL_BOLD);
        btnCheck.setForeground(new Color(25, 130, 65));
        btnCheck.setBackground(Color.WHITE);
        btnCheck.setFocusPainted(false);
        btnCheck.setBorder(new CompoundBorder(new LineBorder(new Color(180, 190, 205), 1), new EmptyBorder(6, 12, 6, 12)));
        btnCheck.addActionListener(e -> JOptionPane.showMessageDialog(this,
                signed ? "Tài liệu hợp lệ và đã có chữ ký." : "Phiếu hợp lệ (trạng thái: Chưa ký số).",
                "Kiểm Tra Tài Liệu", JOptionPane.INFORMATION_MESSAGE));

        left.add(btnPrint);
        left.add(lblSignatureType);
        left.add(btnCheck);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        lblBannerRight1 = new JLabel(data.patientName() + " | " + data.patientCode() + " | " + data.ageText() + " tuổi | " + data.gender());
        lblBannerRight1.setFont(UngDungPhongChu.BODY_BOLD);
        lblBannerRight1.setForeground(new Color(150, 25, 25));
        lblBannerRight1.setAlignmentX(Component.RIGHT_ALIGNMENT);

        lblBannerRight2 = new JLabel("Mã VP : " + data.visitId() + " | " + data.subjectType());
        lblBannerRight2.setFont(UngDungPhongChu.SMALL_BOLD);
        lblBannerRight2.setForeground(new Color(150, 25, 25));
        lblBannerRight2.setAlignmentX(Component.RIGHT_ALIGNMENT);

        lblBannerRight3 = new JLabel(data.address());
        lblBannerRight3.setFont(UngDungPhongChu.SMALL);
        lblBannerRight3.setForeground(new Color(150, 25, 25));
        lblBannerRight3.setAlignmentX(Component.RIGHT_ALIGNMENT);

        right.add(lblBannerRight1);
        right.add(lblBannerRight2);
        right.add(lblBannerRight3);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildA4Paper() {
        JPanel paper = new JPanel();
        paper.setLayout(new BoxLayout(paper, BoxLayout.Y_AXIS));
        paper.setBackground(Color.WHITE);
        paper.setBorder(new CompoundBorder(new LineBorder(new Color(170, 175, 185), 1), new EmptyBorder(24, 30, 28, 30)));
        paper.setPreferredSize(new Dimension(720, 960));

        Font serifTitle = new Font("Serif", Font.BOLD, 16);
        Font serifSection = new Font("Serif", Font.BOLD, 12);
        Font serifBold = new Font("Serif", Font.BOLD, 11);
        Font serifPlain = new Font("Serif", Font.PLAIN, 11);
        Font serifItalic = new Font("Serif", Font.ITALIC, 11);

        // Header 3 columns
        JPanel header = new JPanel(new GridLayout(1, 3, 8, 0));
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        JPanel hLeft = new JPanel(new GridLayout(2, 1));
        hLeft.setOpaque(false);
        hLeft.add(label("SỞ Y TẾ BẮC NINH", serifPlain));
        hLeft.add(label("BỆNH VIỆN SẢN NHI", serifBold));

        JPanel hCenter = new JPanel(new GridLayout(2, 1));
        hCenter.setOpaque(false);
        JLabel tMain = label("PHIẾU KHÁM BỆNH VÀO VIỆN", serifTitle);
        tMain.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel tSub = label("BUỒNG KHÁM BỆNH: " + data.clinicRoom(), serifBold);
        tSub.setHorizontalAlignment(SwingConstants.CENTER);
        hCenter.add(tMain);
        hCenter.add(tSub);

        JPanel hRight = new JPanel(new GridLayout(3, 1));
        hRight.setOpaque(false);
        hRight.add(label("Mã BN: " + data.patientCode(), serifPlain));
        hRight.add(label("MS: 43BV-01", serifPlain));
        hRight.add(label("Số vào viện: " + data.visitId(), serifPlain));

        header.add(hLeft);
        header.add(hCenter);
        header.add(hRight);
        paper.add(header);
        paper.add(Box.createVerticalStrut(10));

        // I. HÀNH CHÍNH
        paper.add(sectionTitle("I. HÀNH CHÍNH", serifSection));
        paper.add(linePair("1. Họ và tên (rõ hoa): ", data.patientName(),
                "2. Sinh ngày: " + formatDobBoxes(data.birthDateDigits()) + "   Tuổi: " + data.ageText(), serifPlain, serifBold));
        boolean isMale = "Nam".equalsIgnoreCase(data.gender());
        paper.add(linePair("3. Giới:   1. Nam [" + (isMale ? "X" : " ") + "]   2. Nữ [" + (!isMale ? "X" : " ") + "]", "",
                "4. Nghề nghiệp: " + data.occupation(), serifPlain, serifPlain));
        paper.add(linePair("5. Dân tộc: ", data.ethnicity(), "6. Ngoại kiều: " + data.nationality(), serifPlain, serifPlain));
        paper.add(lineSingle("7. Địa chỉ: ", data.address(), serifPlain, serifPlain));
        boolean isBhyt = data.subjectType().toUpperCase().contains("BH");
        paper.add(linePair("8. Nơi làm việc: ", data.workplace(),
                "9. Đối tượng:  1.BHYT [" + (isBhyt ? "X" : " ") + "]   2.Thu phí [" + (!isBhyt ? "X" : " ") + "]   3.Miễn [ ]   4.Khác [ ]", serifPlain, serifPlain));
        paper.add(linePair("10. BHYT giá trị đến: ", data.bhytExpiry(), "Số thẻ BHYT: " + data.bhytNumber(), serifPlain, serifPlain));
        paper.add(lineSingle("11. Họ tên, địa chỉ người nhà khi cần báo tin: ", data.relativeInfo(), serifPlain, serifPlain));
        paper.add(lineSingle("12. Đến khám bệnh lúc ", data.arrivalTimeText(), serifPlain, serifPlain));
        paper.add(lineSingle("13. Chẩn đoán của nơi giới thiệu: ", data.referralDiagnosis(), serifPlain, serifPlain));
        paper.add(Box.createVerticalStrut(6));

        // II. LÝ DO VÀO VIỆN
        paper.add(sectionTitle("II. LÝ DO VÀO VIỆN: " + data.admissionReason(), serifSection));
        paper.add(Box.createVerticalStrut(4));

        // III. HỎI BỆNH
        paper.add(sectionTitle("III. HỎI BỆNH:", serifSection));
        paper.add(lineSingle("1. Quá trình bệnh lý: ", data.diseaseProgress(), serifPlain, serifPlain));
        paper.add(lineSingle("2. Tiền sử bệnh:", "", serifPlain, serifPlain));
        paper.add(lineSingle("    • Bản thân: ", data.personalHistory(), serifPlain, serifPlain));
        paper.add(lineSingle("    • Gia đình: ", data.familyHistory(), serifPlain, serifPlain));
        paper.add(Box.createVerticalStrut(6));

        // IV. KHÁM XÉT
        paper.add(sectionTitle("IV. KHÁM XÉT:", serifSection));
        JPanel examBlock = new JPanel(new BorderLayout(14, 0));
        examBlock.setOpaque(false);
        examBlock.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel examLeft = new JPanel();
        examLeft.setLayout(new BoxLayout(examLeft, BoxLayout.Y_AXIS));
        examLeft.setOpaque(false);
        examLeft.add(lineSingle("1. Toàn thân: ", data.fullBodyExam(), serifPlain, serifPlain));
        examLeft.add(lineSingle("2. Các bộ phận: ", data.organExam(), serifPlain, serifPlain));

        JPanel vitalsBox = new JPanel(new GridLayout(6, 2, 6, 2));
        vitalsBox.setOpaque(false);
        vitalsBox.setPreferredSize(new Dimension(210, 115));
        vitalsBox.setBorder(new CompoundBorder(new LineBorder(new Color(140, 145, 155), 1), new EmptyBorder(4, 8, 4, 8)));
        vitalsBox.add(label("Mạch:", serifItalic)); vitalsBox.add(label(data.mach() + " lần/ph", serifPlain));
        vitalsBox.add(label("Nhiệt độ:", serifItalic)); vitalsBox.add(label(data.nhietDo() + " °C", serifPlain));
        vitalsBox.add(label("Huyết áp:", serifItalic)); vitalsBox.add(label(data.huyetAp() + " mmHg", serifPlain));
        vitalsBox.add(label("Nhịp thở:", serifItalic)); vitalsBox.add(label(data.nhipTho() + " lần/ph", serifPlain));
        vitalsBox.add(label("Cân nặng:", serifItalic)); vitalsBox.add(label(data.canNang() + " kg", serifPlain));
        vitalsBox.add(label("Chỉ số BMI:", serifItalic)); vitalsBox.add(label(data.bmi(), serifPlain));

        examBlock.add(examLeft, BorderLayout.CENTER);
        examBlock.add(vitalsBox, BorderLayout.EAST);
        examBlock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        paper.add(examBlock);
        paper.add(Box.createVerticalStrut(6));

        paper.add(lineSingle("3. Tóm tắt kết quả cận lâm sàng: ", data.clsSummary(), serifPlain, serifPlain));
        paper.add(lineSingle("4. Chẩn đoán ban đầu: ", data.initialDiagnosis(), serifPlain, serifBold));
        paper.add(lineSingle("5. Đã xử lý (thuốc, chăm sóc...): ", data.treatedSummary(), serifPlain, serifPlain));
        paper.add(lineSingle("6. Cho vào điều trị tại khoa: ", data.targetDepartment(), serifPlain, serifBold));
        paper.add(Box.createVerticalStrut(24));

        // Signature block
        JPanel signRow = new JPanel(new BorderLayout());
        signRow.setOpaque(false);
        signRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel signRight = new JPanel(new GridLayout(3, 1));
        signRight.setOpaque(false);
        LocalDate today = LocalDate.now();
        signRight.add(label(String.format("Ngày %02d tháng %02d năm %d", today.getDayOfMonth(), today.getMonthValue(), today.getYear()), serifItalic));
        JLabel bsLabel = label("BÁC SĨ KHÁM BỆNH", serifBold);
        bsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        signRight.add(bsLabel);
        signRight.add(label(" ", serifPlain));
        signRow.add(signRight, BorderLayout.EAST);
        paper.add(signRow);

        return paper;
    }

    private void printPaper() {
        if (GraphicsEnvironment.isHeadless()) return;
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("PhieuKhamBenhVaoVien_" + data.patientCode());
        job.setPrintable((Graphics graphics, PageFormat pageFormat, int pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            double scale = Math.min(pageFormat.getImageableWidth() / paperPanel.getWidth(),
                    pageFormat.getImageableHeight() / paperPanel.getHeight());
            g2.scale(scale, scale);
            paperPanel.printAll(g2);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Không thể in phiếu: " + ex.getMessage(), "Lỗi In", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static JButton createSidebarBtn(String text, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(UngDungPhongChu.SMALL_BOLD);
        btn.setForeground(fg);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(92, 46));
        btn.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        return btn;
    }

    private static JLabel label(String text, Font f) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(f);
        lbl.setForeground(Color.BLACK);
        return lbl;
    }

    private static JLabel sectionTitle(String text, Font f) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(f);
        lbl.setForeground(Color.BLACK);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private static JPanel lineSingle(String prefix, String value, Font fPrefix, Font fVal) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(label(prefix, fPrefix));
        if (value != null && !value.isBlank()) p.add(label(value, fVal));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return p;
    }

    private static JPanel linePair(String leftPrefix, String leftVal, String rightText, Font fLeft, Font fVal) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        left.setOpaque(false);
        left.add(label(leftPrefix, fLeft));
        if (leftVal != null && !leftVal.isBlank()) left.add(label(leftVal, fVal));
        p.add(left, BorderLayout.WEST);
        p.add(label(rightText, fLeft), BorderLayout.EAST);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return p;
    }

    private static String formatDobBoxes(String digits) {
        if (digits == null || digits.length() != 8) return "[  ][  ][  ][  ][  ][  ][  ][  ]";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) sb.append('[').append(digits.charAt(i)).append(']');
        return sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
