package vn.clinicmanagement.desktop.modules.billing;


import vn.clinicmanagement.desktop.modules.print.billing.BaoCaoDoanhThuPhieuThuInHopThoai;
import vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

/**
 * Hộp thoại "EHC - Tùy Chọn Báo Cáo" mở khi chọn Báo Cáo -> Báo Cáo Doanh Thu Theo Phiếu Thu.
 * Kết nối trực tiếp bảng PhieuThu, HoaDon, LuotKham, BenhNhan, NhanVien trong clinicDb.
 */
public final class TuyChonBaoCaoDoanhThuPhieuThuHopThoai extends JDialog {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Color LINK_BLUE = new Color(0, 78, 178);

    private final QuanLyDoanhThuReadPort readPort;
    private final String reportTitle;

    // Thời gian lấy báo cáo
    final JCheckBox chkSearchBy = new JCheckBox("Tìm theo");
    final JComboBox<String> timeModeCombo = new JComboBox<>(new String[]{"(Tất cả thời gian)", "Ngày thu tiền", "Ngày lập hóa đơn"});
    final JComboBox<String> recordCombo = new JComboBox<>(new String[]{"(Tất cả bệnh án)", "Nội trú", "Ngoại trú", "Khám bệnh"});

    final JRadioButton rbFromTo = new JRadioButton("Từ ngày", true);
    final JTextField fromField = new JTextField();
    final JTextField toField = new JTextField();

    final JRadioButton rbMonth = new JRadioButton("Tháng");
    final JComboBox<String> monthCombo = new JComboBox<>();
    final JComboBox<String> monthYearCombo = new JComboBox<>();

    final JRadioButton rbQuarter = new JRadioButton("Quý");
    final JComboBox<String> quarterCombo = new JComboBox<>(new String[]{"Quý I", "Quý II", "Quý III", "Quý IV"});
    final JComboBox<String> quarterYearCombo = new JComboBox<>();

    // Tùy chọn nhóm & hiển thị
    final JRadioButton rbNoGroup = new JRadioButton("Không nhóm dữ liệu");
    final JRadioButton rbGroupDate = new JRadioButton("Nhóm theo ngày");
    final JRadioButton rbGroupCashier = new JRadioButton("Nhóm theo thu ngân", true);

    final JCheckBox chkPrintGroupOnly = new JCheckBox("Chỉ in nhóm");
    final JCheckBox chkIncludeCancelled = new JCheckBox("In cả phiếu thu đã hủy", true);
    final JCheckBox chkCancelledOnly = new JCheckBox("Chỉ in phiếu thu đã hủy");
    final JCheckBox chkServiceDetail = new JCheckBox("In chi tiết dịch vụ");

    final JCheckBox chkAllReceiptTypes = new JCheckBox("Tất cả loại phiếu thu", true);
    final JCheckBox chkAdvanceOnly = new JCheckBox("Chỉ lấy báo cáo tạm ứng");
    final JCheckBox chkRefundOnly = new JCheckBox("Chỉ lấy báo cáo hoàn ứng");

    final JCheckBox chkAllAccountTypes = new JCheckBox("Tất cả loại tài khoản", true);
    final JCheckBox chkBhytCopayOnly = new JCheckBox("Chỉ lấy báo cáo thu tiền ĐCT BH");
    final JCheckBox chkServiceOnly = new JCheckBox("Chỉ lấy báo cáo thu tiền dịch vụ");

    final JCheckBox chkPatientBhyt = new JCheckBox("BHYT", true);
    final JCheckBox chkPatientFee = new JCheckBox("Viện phí", true);

    final JCheckBox chkSvcBhyt = new JCheckBox("BHYT", true);
    final JCheckBox chkSvcService = new JCheckBox("Dịch vụ", true);
    final JCheckBox chkSvcFee = new JCheckBox("Viện phí", true);

    final JCheckBox chkShiftBook = new JCheckBox("Sổ trực", true);
    final JCheckBox chkNonShiftBook = new JCheckBox("Không phải sổ trực", true);

    final JCheckBox chkInpatient = new JCheckBox("Nội trú", true);
    final JCheckBox chkOutpatient = new JCheckBox("Ngoại trú", true);
    final JCheckBox chkExam = new JCheckBox("Khám bệnh", true);

    final JCheckBox chkClosed = new JCheckBox("Đã chốt số liệu", true);
    final JCheckBox chkUnclosed = new JCheckBox("Chưa chốt số liệu", true);

    final JRadioButton rbSortTime = new JRadioButton("Sắp xếp theo thời gian", true);
    final JRadioButton rbSortType = new JRadioButton("Sắp xếp theo loại phiếu");
    final JRadioButton rbSortCode = new JRadioButton("Sắp xếp theo ký hiệu");

    final JCheckBox chkContinue = new JCheckBox("Tiếp tục lấy báo cáo", true);

    private final Map<String, Set<String>> selectedLinkFilters = new LinkedHashMap<>();

    public TuyChonBaoCaoDoanhThuPhieuThuHopThoai(Window owner, QuanLyDoanhThuReadPort readPort, String reportTitle,
                                         String initialFrom, String initialTo) {
        super(owner, buildWindowTitle(), ModalityType.APPLICATION_MODAL);
        this.readPort = readPort;
        this.reportTitle = reportTitle;

        for (int m = 1; m <= 12; m++) monthCombo.addItem("Tháng " + m);
        int curYear = LocalDate.now().getYear();
        for (int y = curYear - 5; y <= curYear + 2; y++) {
            monthYearCombo.addItem("Năm " + y);
            quarterYearCombo.addItem("Năm " + y);
        }
        monthCombo.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        monthYearCombo.setSelectedItem("Năm " + curYear);
        quarterYearCombo.setSelectedItem("Năm " + curYear);
        quarterCombo.setSelectedIndex((LocalDate.now().getMonthValue() - 1) / 3);

        fromField.setText(initialFrom != null && !initialFrom.isBlank()
                ? initialFrom : "00:00 " + LocalDate.now().minusDays(30).format(DATE_FMT));
        toField.setText(initialTo != null && !initialTo.isBlank()
                ? initialTo : "23:59 " + LocalDate.now().format(DATE_FMT));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 6));
        getContentPane().setBackground(new Color(245, 245, 245));

        JLabel header = new JLabel("TÙY CHỌN BÁO CÁO");
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        header.setForeground(new Color(180, 0, 0));
        header.setBorder(new EmptyBorder(8, 12, 2, 12));
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 10, 4, 10));
        body.add(buildTimePanel());
        body.add(Box.createVerticalStrut(6));
        body.add(buildOptionsPanel());
        add(body, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(880, 620));
        setLocationRelativeTo(owner);
    }

    private JPanel buildTimePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(195, 198, 204)),
                "Thời gian lấy báo cáo", TitledBorder.LEFT, TitledBorder.TOP,
                UngDungPhongChu.SMALL, Color.DARK_GRAY));

        ButtonGroup tg = new ButtonGroup();
        tg.add(rbFromTo);
        tg.add(rbMonth);
        tg.add(rbQuarter);
        for (JComponent c : List.of(chkSearchBy, rbFromTo, rbMonth, rbQuarter)) {
            c.setOpaque(false);
            c.setFont(UngDungPhongChu.BODY);
        }
        for (JComboBox<?> cb : List.of(timeModeCombo, recordCombo, monthCombo, monthYearCombo, quarterCombo, quarterYearCombo)) {
            cb.setFont(UngDungPhongChu.BODY);
            cb.setPreferredSize(new Dimension(145, 23));
        }
        fromField.setFont(UngDungPhongChu.BODY);
        fromField.setPreferredSize(new Dimension(145, 23));
        toField.setFont(UngDungPhongChu.BODY);
        toField.setPreferredSize(new Dimension(145, 23));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 6, 2, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0
        gc.gridy = 0; gc.gridx = 0; gc.weightx = 0; p.add(chkSearchBy, gc);
        gc.gridx = 1; gc.weightx = 0.25; p.add(timeModeCombo, gc);
        gc.gridx = 2; gc.weightx = 0; p.add(lbl("Bệnh án"), gc);
        gc.gridx = 3; gc.weightx = 0.25; p.add(recordCombo, gc);
        gc.gridx = 4; gc.gridwidth = 2; gc.weightx = 0.5; p.add(Box.createHorizontalGlue(), gc);
        gc.gridwidth = 1;

        // Row 1
        gc.gridy = 1; gc.gridx = 0; gc.weightx = 0; p.add(rbFromTo, gc);
        gc.gridx = 1; gc.weightx = 0.25; p.add(fromField, gc);
        gc.gridx = 2; gc.weightx = 0; p.add(lbl("Đến ngày"), gc);
        gc.gridx = 3; gc.weightx = 0.25; p.add(toField, gc);
        gc.gridx = 4; gc.weightx = 0.15; p.add(quickLink("Trong Ngày", () -> setRangeDays(0, 0)), gc);
        gc.gridx = 5; gc.weightx = 0.15; p.add(quickLink("Hôm Qua", () -> setRangeDays(-1, -1)), gc);

        // Row 2
        gc.gridy = 2; gc.gridx = 0; gc.weightx = 0; p.add(rbMonth, gc);
        gc.gridx = 1; gc.weightx = 0.25; p.add(monthCombo, gc);
        gc.gridx = 2; gc.weightx = 0; p.add(lbl("Năm"), gc);
        gc.gridx = 3; gc.weightx = 0.25; p.add(monthYearCombo, gc);
        gc.gridx = 4; gc.weightx = 0.15; p.add(quickLink("Trong Tuần", () -> {
            LocalDate now = LocalDate.now();
            setRange(now.with(DayOfWeek.MONDAY), now);
        }), gc);
        gc.gridx = 5; gc.weightx = 0.15; p.add(quickLink("7 Ngày Gần Đây", () -> setRangeDays(-6, 0)), gc);

        // Row 3
        gc.gridy = 3; gc.gridx = 0; gc.weightx = 0; p.add(rbQuarter, gc);
        gc.gridx = 1; gc.weightx = 0.25; p.add(quarterCombo, gc);
        gc.gridx = 2; gc.weightx = 0; p.add(lbl("Năm"), gc);
        gc.gridx = 3; gc.weightx = 0.25; p.add(quarterYearCombo, gc);
        gc.gridx = 4; gc.weightx = 0.15; p.add(quickLink("Trong Tháng", () -> {
            LocalDate now = LocalDate.now();
            setRange(now.withDayOfMonth(1), now);
        }), gc);
        gc.gridx = 5; gc.weightx = 0.15; p.add(quickLink("30 Ngày Gần Đây", () -> setRangeDays(-29, 0)), gc);

        return p;
    }

    private JPanel buildOptionsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(195, 198, 204)),
                "Tùy chọn", TitledBorder.LEFT, TitledBorder.TOP,
                UngDungPhongChu.SMALL, Color.DARK_GRAY));

        ButtonGroup grp = new ButtonGroup();
        grp.add(rbNoGroup);
        grp.add(rbGroupDate);
        grp.add(rbGroupCashier);

        JPanel r1 = new JPanel(new GridLayout(1, 3, 12, 0));
        r1.setOpaque(false);
        r1.add(style(rbNoGroup));
        r1.add(style(rbGroupDate));
        r1.add(style(rbGroupCashier));
        p.add(r1);

        JPanel r2 = new JPanel(new GridLayout(1, 4, 8, 0));
        r2.setOpaque(false);
        r2.add(style(chkPrintGroupOnly));
        r2.add(style(chkIncludeCancelled));
        r2.add(style(chkCancelledOnly));
        r2.add(style(chkServiceDetail));
        p.add(r2);
        p.add(Box.createVerticalStrut(4));

        // Blue filter links
        JPanel links1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        links1.setOpaque(false);
        for (String s : List.of("Lọc theo nhóm tài khoản", "Lọc theo sổ thu", "Lọc theo hình thức phiếu thu",
                "Lọc theo loại phiếu thu", "Lọc theo thu ngân", "Lọc theo khoa")) {
            links1.add(filterLink(s));
        }
        JPanel links2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        links2.setOpaque(false);
        for (String s : List.of("Lọc theo phòng chỉ định", "Lọc theo đối tượng chi tiết",
                "Lọc theo loại phiếu thu chi tiết", "Lọc theo thứ", "Lọc theo loại công khám")) {
            links2.add(filterLink(s));
        }
        p.add(links1);
        p.add(links2);
        p.add(Box.createVerticalStrut(6));

        // 4-column checkboxes block 1
        JPanel cols1 = new JPanel(new GridLayout(1, 4, 8, 0));
        cols1.setOpaque(false);

        JPanel c1 = verticalCol();
        c1.add(style(chkAllReceiptTypes));
        c1.add(style(chkAdvanceOnly));
        c1.add(style(chkRefundOnly));

        JPanel c2 = verticalCol();
        c2.add(style(chkAllAccountTypes));
        c2.add(style(chkBhytCopayOnly));
        c2.add(style(chkServiceOnly));

        JPanel c3 = verticalCol();
        c3.add(boldLbl("Đối tượng bệnh nhân"));
        c3.add(style(chkPatientBhyt));
        c3.add(style(chkPatientFee));

        JPanel c4 = verticalCol();
        c4.add(boldLbl("Đối tượng dịch vụ"));
        JPanel svcRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        svcRow.setOpaque(false);
        svcRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        svcRow.add(style(chkSvcBhyt));
        svcRow.add(style(chkSvcService));
        c4.add(svcRow);
        c4.add(style(chkSvcFee));

        cols1.add(c1);
        cols1.add(c2);
        cols1.add(c3);
        cols1.add(c4);
        p.add(cols1);
        p.add(Box.createVerticalStrut(6));

        // 4-column checkboxes block 2
        JPanel cols2 = new JPanel(new GridLayout(1, 4, 8, 0));
        cols2.setOpaque(false);

        JPanel b1 = verticalCol();
        b1.add(boldLbl("Sổ thu"));
        b1.add(style(chkShiftBook));
        b1.add(style(chkNonShiftBook));

        JPanel b2 = verticalCol();
        b2.add(boldLbl("Bệnh án (thời điểm tạo phiếu)"));
        b2.add(style(chkInpatient));
        b2.add(style(chkOutpatient));
        b2.add(style(chkExam));

        JPanel b3 = verticalCol();
        b3.add(Box.createVerticalStrut(18));
        b3.add(style(chkClosed));

        JPanel b4 = verticalCol();
        b4.add(Box.createVerticalStrut(18));
        b4.add(style(chkUnclosed));

        cols2.add(b1);
        cols2.add(b2);
        cols2.add(b3);
        cols2.add(b4);
        p.add(cols2);
        p.add(Box.createVerticalStrut(6));

        // Sort radios
        ButtonGroup sortGrp = new ButtonGroup();
        sortGrp.add(rbSortTime);
        sortGrp.add(rbSortType);
        sortGrp.add(rbSortCode);
        JPanel sortRow = new JPanel(new GridLayout(1, 3, 12, 0));
        sortRow.setOpaque(false);
        sortRow.add(style(rbSortTime));
        sortRow.add(style(rbSortType));
        sortRow.add(style(rbSortCode));
        p.add(sortRow);

        return p;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 5));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(4, 12, 8, 12));

        JPanel controls = new JPanel(new BorderLayout());
        controls.setOpaque(false);
        chkContinue.setOpaque(false);
        chkContinue.setFont(UngDungPhongChu.BODY);
        chkContinue.setForeground(new Color(180, 0, 0));
        controls.add(chkContinue, BorderLayout.WEST);

        chkSearchBy.addActionListener(e -> updateTimeControls());
        chkAllReceiptTypes.addActionListener(e -> { if (chkAllReceiptTypes.isSelected()) { chkAdvanceOnly.setSelected(false); chkRefundOnly.setSelected(false); } });
        chkAdvanceOnly.addActionListener(e -> { if (chkAdvanceOnly.isSelected()) chkAllReceiptTypes.setSelected(false); });
        chkRefundOnly.addActionListener(e -> { if (chkRefundOnly.isSelected()) chkAllReceiptTypes.setSelected(false); });
        chkAllAccountTypes.addActionListener(e -> { if (chkAllAccountTypes.isSelected()) { chkBhytCopayOnly.setSelected(false); chkServiceOnly.setSelected(false); } });
        chkBhytCopayOnly.addActionListener(e -> { if (chkBhytCopayOnly.isSelected()) chkAllAccountTypes.setSelected(false); });
        chkServiceOnly.addActionListener(e -> { if (chkServiceOnly.isSelected()) chkAllAccountTypes.setSelected(false); });
        timeModeCombo.addActionListener(e -> updateTimeControls());
        rbFromTo.addActionListener(e -> updateTimeControls());
        rbMonth.addActionListener(e -> updateTimeControls());
        rbQuarter.addActionListener(e -> updateTimeControls());
        chkCancelledOnly.addActionListener(e -> { if (chkCancelledOnly.isSelected()) chkIncludeCancelled.setSelected(true); });
        updateTimeControls();

        JButton okBtn = new JButton("✓ OK");
        okBtn.setFont(UngDungPhongChu.BODY_BOLD);
        okBtn.setForeground(new Color(0, 110, 60));
        okBtn.setPreferredSize(new Dimension(88, 30));
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.addActionListener(e -> runReport());
        controls.add(okBtn, BorderLayout.EAST);
        footer.add(controls, BorderLayout.SOUTH);

        return footer;
    }

    private void runReport() {
        LocalDateTime[] range;
        boolean filterByDate = chkSearchBy.isSelected() && !"(Tất cả thời gian)".equals(timeModeCombo.getSelectedItem());
        try {
            range = filterByDate ? resolveDateRange() : new LocalDateTime[]{null, null};
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), reportTitle, JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!hasAnySelected(chkPatientBhyt, chkPatientFee)
                || !hasAnySelected(chkSvcBhyt, chkSvcService, chkSvcFee)
                || !hasAnySelected(chkShiftBook, chkNonShiftBook)
                || !hasAnySelected(chkInpatient, chkOutpatient, chkExam)
                || !hasAnySelected(chkClosed, chkUnclosed)
                || !hasAnySelected(chkAllReceiptTypes, chkAdvanceOnly, chkRefundOnly)
                || !hasAnySelected(chkAllAccountTypes, chkBhytCopayOnly, chkServiceOnly)) {
            JOptionPane.showMessageDialog(this,
                    "Mỗi nhóm lọc phải có ít nhất một lựa chọn được tích.", reportTitle, JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (chkPrintGroupOnly.isSelected() && rbNoGroup.isSelected()) {
            JOptionPane.showMessageDialog(this, "Hãy chọn nhóm theo ngày hoặc theo thu ngân để chỉ in nhóm.",
                    reportTitle, JOptionPane.WARNING_MESSAGE);
            return;
        }
        var ctx = KhamBenhPhienNguCanh.current();
        int facilityId = ctx == null ? 1 : ctx.facilityId();

        int timeModeIdx = "Ngày lập hóa đơn".equals(timeModeCombo.getSelectedItem()) ? 2 : 1;
        int sortModeIdx = rbSortType.isSelected() ? 1 : rbSortCode.isSelected() ? 2 : 0;

        List<QuanLyDoanhThuReadPort.ReceiptReportRow> rawRows;
        try {
            rawRows = readPort.listReceiptReport(facilityId, filterByDate, timeModeIdx, range[0], range[1], sortModeIdx);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Không tải được dữ liệu báo cáo: " + ex.getMessage(),
                    reportTitle, JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<BaoCaoDoanhThuPhieuThuInHopThoai.ReceiptPrintRow> printRows = new ArrayList<>();
        for (var r : rawRows) {
            String status = r.invoiceStatus();
            boolean cancelled = "DA_HUY".equalsIgnoreCase(status) || "HUY".equalsIgnoreCase(status);
            if (chkCancelledOnly.isSelected() && !cancelled) continue;
            if (!chkIncludeCancelled.isSelected() && !chkCancelledOnly.isSelected() && cancelled) continue;

            String loaiPhieu = Objects.toString(r.receiptType(), "THU_TIEN").toUpperCase(Locale.ROOT);
            boolean advance = "TAM_UNG".equals(loaiPhieu) || r.totalAdvance().compareTo(BigDecimal.ZERO) > 0;
            boolean refund = "HOAN_UNG".equals(loaiPhieu) || r.totalRefund().compareTo(BigDecimal.ZERO) > 0;
            if (!chkAllReceiptTypes.isSelected()
                    && !(chkAdvanceOnly.isSelected() && advance)
                    && !(chkRefundOnly.isSelected() && refund)) continue;

            boolean isShiftBook = r.shiftBook();
            if (isShiftBook && !chkShiftBook.isSelected()) continue;
            if (!isShiftBook && !chkNonShiftBook.isSelected()) continue;

            String accountType = Objects.toString(r.subjectType(), "THU_PHI").toUpperCase(Locale.ROOT);
            boolean isBhytAccount = accountType.contains("BHYT");
            boolean isServiceAccount = !isBhytAccount;
            if (!chkAllAccountTypes.isSelected()
                    && !(chkBhytCopayOnly.isSelected() && isBhytAccount)
                    && !(chkServiceOnly.isSelected() && isServiceAccount)) continue;

            String subj = r.subjectType();
            boolean isBhyt = subj != null && subj.toUpperCase(Locale.ROOT).contains("BHYT");
            if (isBhyt && !chkPatientBhyt.isSelected()) continue;
            if (!isBhyt && !chkPatientFee.isSelected()) continue;

            int lineCount = r.lineCount();
            if (lineCount > 0 && !chkSvcBhyt.isSelected() && !chkSvcService.isSelected() && !chkSvcFee.isSelected()) continue;
            if (lineCount > 0 && !((chkSvcBhyt.isSelected() && r.bhytCount() > 0)
                    || (chkSvcService.isSelected() && (r.serviceCount() > 0 || "DICH_VU_YEU_CAU".equalsIgnoreCase(subj)))
                    || (chkSvcFee.isSelected() && r.feeCount() > 0))) continue;

            String treatment = Objects.toString(r.treatmentType(), "").toUpperCase(Locale.ROOT);
            boolean inpatient = treatment.contains("NOI_TRU");
            boolean exam = Objects.toString(r.examType(), "").toUpperCase(Locale.ROOT).contains("KHÁM");
            if (inpatient && !chkInpatient.isSelected()) continue;
            if (!inpatient && exam && !chkExam.isSelected()) continue;
            if (!inpatient && !exam && !chkOutpatient.isSelected()) continue;
            String recordType = Objects.toString(recordCombo.getSelectedItem(), "");
            if ("Nội trú".equals(recordType) && !inpatient) continue;
            if ("Ngoại trú".equals(recordType) && (inpatient || exam)) continue;
            if ("Khám bệnh".equals(recordType) && !exam) continue;
            boolean closed = r.closed();
            if (closed && !chkClosed.isSelected()) continue;
            if (!closed && !chkUnclosed.isSelected()) continue;

            if (!matchesLinkFilters(r)) continue;

            var dob = r.birthDate();
            String yearStr = dob == null ? "" : String.valueOf(dob.getYear());
            String gender = r.gender();
            boolean isFemale = "F".equalsIgnoreCase(gender) || "NU".equalsIgnoreCase(gender) || "NỮ".equalsIgnoreCase(gender);
            String birthMale = isFemale ? "" : yearStr;
            String birthFemale = isFemale ? yearStr : "";

            BigDecimal amt = r.amount();
            String note = r.note();
            if (note == null || note.isBlank()) note = chkServiceDetail.isSelected() ? r.serviceSummary() : "";
            else if (chkServiceDetail.isSelected() && r.serviceSummary() != null
                    && !r.serviceSummary().isBlank()) note += " | " + r.serviceSummary();

            String pm = r.paymentMethod();
            String pmLabel = "TIEN_MAT".equalsIgnoreCase(pm) ? "Tiền mặt"
                    : "CHUYEN_KHOAN".equalsIgnoreCase(pm) ? "Chuyển khoản" : pm;

            printRows.add(new BaoCaoDoanhThuPhieuThuInHopThoai.ReceiptPrintRow(
                    r.cashierName(),
                    r.receiptCode(),
                    r.receiptTime(),
                    r.visitDate(),
                    r.patientName(),
                    birthMale,
                    birthFemale,
                    r.address(),
                    advance ? "Tạm ứng" : refund ? "Hoàn ứng" : "Thu tiền",
                    String.valueOf(r.invoiceId()),
                    pmLabel,
                    note,
                    cancelled ? amt : BigDecimal.ZERO,
                    cancelled ? "Đã hủy" : "",
                    cancelled ? BigDecimal.ZERO : amt,
                    r.refundAmount(),
                    r.advanceAmount(),
                    BigDecimal.ZERO,
                    r.discountAmount(),
                    r.discountReason()
            ));
        }

        new BaoCaoDoanhThuPhieuThuInHopThoai(
                this,
                printRows,
                rbGroupCashier.isSelected() ? "CASHIER" : rbGroupDate.isSelected() ? "DATE" : "NONE",
                chkPrintGroupOnly.isSelected(),
                range[0],
                range[1]).setVisible(true);
        if (!chkContinue.isSelected()) dispose();
    }

    private LocalDateTime[] resolveDateRange() {
        if (rbMonth.isSelected()) {
            int m = monthCombo.getSelectedIndex() + 1;
            int y = parseYear((String) monthYearCombo.getSelectedItem());
            LocalDate start = LocalDate.of(y, m, 1);
            return new LocalDateTime[]{start.atStartOfDay(), start.withDayOfMonth(start.lengthOfMonth()).atTime(23, 59, 59)};
        }
        if (rbQuarter.isSelected()) {
            int q = quarterCombo.getSelectedIndex();
            int y = parseYear((String) quarterYearCombo.getSelectedItem());
            LocalDate start = LocalDate.of(y, q * 3 + 1, 1);
            LocalDate end = start.plusMonths(2);
            return new LocalDateTime[]{start.atStartOfDay(), end.withDayOfMonth(end.lengthOfMonth()).atTime(23, 59, 59)};
        }
        LocalDateTime from = parseRequiredDt(fromField.getText(), "Từ ngày");
        LocalDateTime to = parseRequiredDt(toField.getText(), "Đến ngày");
        if (to.getSecond() == 0 && to.getNano() == 0) to = to.plusSeconds(59);
        if (from.isAfter(to)) throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        return new LocalDateTime[]{from, to};
    }

    private static LocalDateTime parseRequiredDt(String value, String label) {
        try {
            return LocalDateTime.parse(value == null ? "" : value.trim(), DT_FMT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(label + " không hợp lệ. Định dạng cần dùng: HH:mm dd/MM/yyyy.");
        }
    }

    private void updateTimeControls() {
        boolean enabled = chkSearchBy.isSelected();
        timeModeCombo.setEnabled(enabled);
        recordCombo.setEnabled(true);
        boolean rangeEnabled = enabled && !"(Tất cả thời gian)".equals(timeModeCombo.getSelectedItem());
        rbFromTo.setEnabled(rangeEnabled);
        rbMonth.setEnabled(rangeEnabled);
        rbQuarter.setEnabled(rangeEnabled);
        boolean customRange = rangeEnabled && rbFromTo.isSelected();
        fromField.setEnabled(customRange);
        toField.setEnabled(customRange);
        monthCombo.setEnabled(rangeEnabled && rbMonth.isSelected());
        monthYearCombo.setEnabled(rangeEnabled && rbMonth.isSelected());
        quarterCombo.setEnabled(rangeEnabled && rbQuarter.isSelected());
        quarterYearCombo.setEnabled(rangeEnabled && rbQuarter.isSelected());
    }

    private static boolean hasAnySelected(JCheckBox... boxes) {
        for (JCheckBox box : boxes) if (box.isSelected()) return true;
        return false;
    }

    private boolean matchesLinkFilters(QuanLyDoanhThuReadPort.ReceiptReportRow r) {
        for (var filter : selectedLinkFilters.entrySet()) {
            String value = switch (filter.getKey()) {
                case "account", "detailAccount" -> r.subjectType();
                case "payment", "detailPayment" -> r.paymentMethod();
                case "cashier" -> r.cashierName();
                case "department" -> r.department();
                case "room" -> r.room();
                case "examType" -> r.examType();
                case "book" -> r.receiptBook();
                case "weekday" -> {
                    LocalDateTime ts = r.receiptTime();
                    yield ts == null ? "" : switch (ts.getDayOfWeek()) {
                        case MONDAY -> "Thứ Hai"; case TUESDAY -> "Thứ Ba"; case WEDNESDAY -> "Thứ Tư";
                        case THURSDAY -> "Thứ Năm"; case FRIDAY -> "Thứ Sáu"; case SATURDAY -> "Thứ Bảy";
                        case SUNDAY -> "Chủ Nhật";
                    };
                }
                case "receiptType", "detailReceiptType" -> {
                    boolean advance = r.totalAdvance().compareTo(BigDecimal.ZERO) > 0;
                    boolean refund = r.totalRefund().compareTo(BigDecimal.ZERO) > 0;
                    yield advance ? "Tạm ứng" : refund ? "Hoàn ứng" : "Thu tiền";
                }
                default -> "";
            };
            String normalized = Objects.toString(value, "").trim();
            if (filter.getValue().stream().noneMatch(v -> v.equalsIgnoreCase(normalized))) return false;
        }
        return true;
    }

    private static int parseYear(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D+", ""));
        } catch (Exception ex) {
            return LocalDate.now().getYear();
        }
    }

    private static LocalDateTime parseDt(String s, LocalDateTime fallback) {
        try {
            return LocalDateTime.parse(s == null ? "" : s.trim(), DT_FMT);
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }

    private void setRangeDays(int startOffset, int endOffset) {
        LocalDate today = LocalDate.now();
        setRange(today.plusDays(startOffset), today.plusDays(endOffset));
    }

    private void setRange(LocalDate start, LocalDate end) {
        chkSearchBy.setSelected(true);
        timeModeCombo.setSelectedItem("Ngày thu tiền");
        rbFromTo.setSelected(true);
        fromField.setText("00:00 " + start.format(DATE_FMT));
        toField.setText("23:59 " + end.format(DATE_FMT));
    }

    private JLabel quickLink(String text, Runnable action) {
        JLabel l = new JLabel("<html><u>" + text + "</u></html>");
        l.setFont(UngDungPhongChu.BODY);
        l.setForeground(LINK_BLUE);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { action.run(); }
        });
        return l;
    }

    private JLabel filterLink(String text) {
        String key = switch (text) {
            case "Lọc theo nhóm tài khoản" -> "account";
            case "Lọc theo sổ thu" -> "book";
            case "Lọc theo hình thức phiếu thu" -> "payment";
            case "Lọc theo loại phiếu thu" -> "receiptType";
            case "Lọc theo thu ngân" -> "cashier";
            case "Lọc theo khoa" -> "department";
            case "Lọc theo phòng chỉ định" -> "room";
            case "Lọc theo đối tượng chi tiết" -> "detailAccount";
            case "Lọc theo loại phiếu thu chi tiết" -> "detailReceiptType";
            case "Lọc theo thứ" -> "weekday";
            case "Lọc theo loại công khám" -> "examType";
            default -> "unsupported";
        };
        return quickLink(text, () -> {
            if ("unsupported".equals(key)) {
                JOptionPane.showMessageDialog(this,
                        "Cơ sở dữ liệu hiện chưa có thông tin sổ thu để lọc.", text, JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            List<String> options = loadFilterOptions(key);
            if (options.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không có giá trị để lọc.", text, JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JList<String> list = new JList<>(options.toArray(String[]::new));
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            Set<String> current = selectedLinkFilters.getOrDefault(key, Set.of());
            int[] selected = new ArrayList<>(options).stream().filter(current::contains)
                    .mapToInt(options::indexOf).toArray();
            list.setSelectedIndices(selected);
            int result = JOptionPane.showConfirmDialog(this, new JScrollPane(list), text,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                List<String> values = list.getSelectedValuesList();
                if (values.isEmpty()) selectedLinkFilters.remove(key);
                else selectedLinkFilters.put(key, new LinkedHashSet<>(values));
            }
        });
    }

    private List<String> loadFilterOptions(String key) {
        if ("receiptType".equals(key) || "detailReceiptType".equals(key))
            return List.of("Thu tiền", "Tạm ứng", "Hoàn ứng");
        if ("weekday".equals(key))
            return List.of("Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật");

        var ctx = KhamBenhPhienNguCanh.current();
        try {
            return readPort.listReceiptFilterValues(ctx == null ? 1 : ctx.facilityId(), key);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Không tải được danh sách lọc: " + ex.getMessage(),
                    reportTitle, JOptionPane.ERROR_MESSAGE);
            return List.of();
        }
    }

    private static <T extends AbstractButton> T style(T btn) {
        btn.setOpaque(false);
        btn.setFont(UngDungPhongChu.BODY);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    private static JPanel verticalCol() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    private static JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.BODY);
        return l;
    }

    private static JLabel boldLbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.BODY_BOLD);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static String buildWindowTitle() {
        String user = Objects.toString(NguoiDungPhien.getInstance().getFullName(), "").trim();
        if (user.isEmpty()) user = "Quản Trị Hệ Thống";
        return "EHC - Tùy Chọn Báo Cáo   --- Người đăng nhập : " + user + " ---";
    }
}
