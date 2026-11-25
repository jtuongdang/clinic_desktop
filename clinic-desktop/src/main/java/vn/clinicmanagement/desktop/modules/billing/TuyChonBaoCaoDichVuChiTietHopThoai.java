package vn.clinicmanagement.desktop.modules.billing;


import vn.clinicmanagement.desktop.modules.print.billing.BaoCaoDoanhThuDichVuChiTietInHopThoai;
import vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

/**
 * Hộp thoại "EHC - Tùy Chọn Báo Cáo" dành riêng cho "Báo Cáo Doanh Thu Theo Dịch Vụ Chi Tiết (019)".
 * Tách riêng hoàn toàn khỏi TuyChonBaoCaoTheoDichVuHopThoai và gắn toàn bộ tùy chọn vào truy vấn clinicDb.
 */
public final class TuyChonBaoCaoDichVuChiTietHopThoai extends JDialog {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Color LINK_BLUE = new Color(0, 78, 178);
    private static final Color CLEAR_RED = new Color(176, 40, 40);

    private final QuanLyDoanhThuReadPort readPort;

    final JComboBox<String> searchByCombo = new JComboBox<>(new String[]{"Ngày tạo phiếu thu", "Ngày tiếp nhận", "Ngày lập hóa đơn"});
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
    final JRadioButton rbOther = new JRadioButton("Khác");

    final JRadioButton rbNoGroup = new JRadioButton("Không nhóm dữ liệu", true);
    final JRadioButton rbGroupDate = new JRadioButton("Nhóm theo ngày");
    final JRadioButton rbGroupOrderRoom = new JRadioButton("Nhóm theo phòng chỉ định");
    final JRadioButton rbGroupExecRoom = new JRadioButton("Nhóm theo phòng thực hiện");
    final JRadioButton rbGroupAcct = new JRadioButton("Nhóm theo nhóm tài khoản");
    final JRadioButton rbGroupSvcType = new JRadioButton("Nhóm theo loại dịch vụ");
    final JRadioButton rbGroupBhyt = new JRadioButton("Nhóm theo nhóm chi phí BHYT/loại dịch vụ");

    final JCheckBox chkPatientDetail = new JCheckBox("Chi tiết bệnh nhân");
    final JCheckBox chkMergeDiff = new JCheckBox("In gộp chênh lệch dịch vụ");
    final JCheckBox chkContinue = new JCheckBox("Tiếp tục lấy báo cáo", true);

    private LocalDateTime customOtherFrom;
    private LocalDateTime customOtherTo;
    private final Map<String, String> activeFilters = new LinkedHashMap<>();
    private final Map<String, JLabel> filterValueLabels = new LinkedHashMap<>();

    public TuyChonBaoCaoDichVuChiTietHopThoai(Window owner, QuanLyDoanhThuReadPort readPort,
                                                  String initialFrom, String initialTo) {
        super(owner, buildWindowTitle(), ModalityType.APPLICATION_MODAL);
        this.readPort = readPort;

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

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(6, 12, 4, 12));
        body.add(buildTimePanel());
        body.add(Box.createVerticalStrut(6));
        body.add(buildOptionsPanel());
        add(body, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(790, 650));
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
        for (JRadioButton rb : List.of(rbFromTo, rbMonth, rbQuarter, rbOther)) {
            tg.add(rb);
            rb.setOpaque(false);
            rb.setFont(UngDungPhongChu.BODY);
        }
        for (JComboBox<?> cb : List.of(searchByCombo, recordCombo, monthCombo, monthYearCombo, quarterCombo, quarterYearCombo)) {
            cb.setFont(UngDungPhongChu.BODY);
            cb.setPreferredSize(new Dimension(150, 22));
        }
        fromField.setFont(UngDungPhongChu.BODY);
        fromField.setPreferredSize(new Dimension(150, 22));
        toField.setFont(UngDungPhongChu.BODY);
        toField.setPreferredSize(new Dimension(150, 22));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 6, 2, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridy = 0; gc.gridx = 0; gc.weightx = 0; p.add(lbl("Tìm theo"), gc);
        gc.gridx = 1; gc.weightx = 0.25; p.add(searchByCombo, gc);
        gc.gridx = 2; gc.weightx = 0; p.add(lbl("Bệnh án"), gc);
        gc.gridx = 3; gc.weightx = 0.25; p.add(recordCombo, gc);
        gc.gridx = 4; gc.gridwidth = 2; gc.weightx = 0.5; p.add(Box.createHorizontalGlue(), gc);
        gc.gridwidth = 1;

        gc.gridy = 1; gc.gridx = 0; gc.weightx = 0; p.add(rbFromTo, gc);
        gc.gridx = 1; gc.weightx = 0.25; p.add(fromField, gc);
        gc.gridx = 2; gc.weightx = 0; p.add(lbl("Đến ngày"), gc);
        gc.gridx = 3; gc.weightx = 0.25; p.add(toField, gc);
        gc.gridx = 4; gc.weightx = 0.15; p.add(quickLink("Trong Ngày", () -> setRangeDays(0, 0)), gc);
        gc.gridx = 5; gc.weightx = 0.15; p.add(quickLink("Hôm Qua", () -> setRangeDays(-1, -1)), gc);

        gc.gridy = 2; gc.gridx = 0; gc.weightx = 0; p.add(rbMonth, gc);
        gc.gridx = 1; gc.weightx = 0.25; p.add(monthCombo, gc);
        gc.gridx = 2; gc.weightx = 0; p.add(lbl("Năm"), gc);
        gc.gridx = 3; gc.weightx = 0.25; p.add(monthYearCombo, gc);
        gc.gridx = 4; gc.weightx = 0.15; p.add(quickLink("Trong Tuần", () -> {
            LocalDate now = LocalDate.now();
            setRange(now.with(DayOfWeek.MONDAY), now);
        }), gc);
        gc.gridx = 5; gc.weightx = 0.15; p.add(quickLink("7 Ngày Gần Đây", () -> setRangeDays(-6, 0)), gc);

        gc.gridy = 3; gc.gridx = 0; gc.weightx = 0; p.add(rbQuarter, gc);
        gc.gridx = 1; gc.weightx = 0.25; p.add(quarterCombo, gc);
        gc.gridx = 2; gc.weightx = 0; p.add(lbl("Năm"), gc);
        gc.gridx = 3; gc.weightx = 0.25; p.add(quarterYearCombo, gc);
        gc.gridx = 4; gc.weightx = 0.15; p.add(quickLink("Trong Tháng", () -> {
            LocalDate now = LocalDate.now();
            setRange(now.withDayOfMonth(1), now);
        }), gc);
        gc.gridx = 5; gc.weightx = 0.15; p.add(quickLink("30 Ngày Gần Đây", () -> setRangeDays(-29, 0)), gc);

        JLabel otherLink = quickLink("Chọn nhiều khoảng thời gian", this::pickOtherRange);
        gc.gridy = 4; gc.gridx = 0; gc.weightx = 0; p.add(rbOther, gc);
        gc.gridx = 1; gc.gridwidth = 3; p.add(otherLink, gc);
        rbOther.addActionListener(e -> pickOtherRange());

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
        for (JRadioButton rb : List.of(rbNoGroup, rbGroupDate, rbGroupOrderRoom, rbGroupExecRoom,
                rbGroupAcct, rbGroupSvcType, rbGroupBhyt)) {
            grp.add(rb);
            rb.setOpaque(false);
            rb.setFont(UngDungPhongChu.BODY);
        }

        JPanel r1 = new JPanel(new GridLayout(1, 4, 8, 0));
        r1.setOpaque(false);
        r1.add(rbNoGroup);
        r1.add(rbGroupDate);
        r1.add(rbGroupOrderRoom);
        r1.add(rbGroupExecRoom);
        p.add(r1);

        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        r2.setOpaque(false);
        r2.add(rbGroupAcct);
        r2.add(rbGroupSvcType);
        r2.add(rbGroupBhyt);
        p.add(r2);

        JPanel r3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 2));
        r3.setOpaque(false);
        chkPatientDetail.setOpaque(false);
        chkPatientDetail.setFont(UngDungPhongChu.BODY);
        chkMergeDiff.setOpaque(false);
        chkMergeDiff.setFont(UngDungPhongChu.BODY);
        r3.add(chkPatientDetail);
        r3.add(chkMergeDiff);
        p.add(r3);
        p.add(Box.createVerticalStrut(4));

        String[][] filterRows = {
                {"ORDER_ROOM", "Lọc phòng chỉ định"},
                {"EXEC_ROOM", "Lọc phòng thực hiện"},
                {"SVC_GROUP", "Lọc nhóm - loại dịch vụ"},
                {"SERVICE", "Lọc dịch vụ"},
                {"DRUG_SUPPLY", "Lọc thuốc, vật tư"},
                {"ACCT_GROUP", "Lọc nhóm tài khoản"},
                {"SVC_SUBJECT", "Lọc đối tượng dịch vụ"},
                {"RECEIPT_BOOK", "Lọc sổ thu"},
                {"CASHIER", "Lọc thu ngân"},
                {"BHYT_GROUP", "Lọc nhóm chi phí BHYT"},
                {"ORDER_DOCTOR", "Lọc nhân viên chỉ định"},
                {"PATIENT_SUBJECT", "Lọc đối tượng BN"}
        };

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 6, 2, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < filterRows.length; i++) {
            String key = filterRows[i][0];
            String label = filterRows[i][1];

            JLabel pickLink = quickLink(label, () -> chooseFilter(key, label));
            JLabel valLbl = new JLabel("---");
            valLbl.setFont(UngDungPhongChu.SMALL);
            valLbl.setForeground(CLEAR_RED);
            filterValueLabels.put(key, valLbl);

            JLabel clearLink = new JLabel("<html><u>Bỏ (X)</u></html>");
            clearLink.setFont(UngDungPhongChu.SMALL);
            clearLink.setForeground(CLEAR_RED);
            clearLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            clearLink.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    activeFilters.remove(key);
                    valLbl.setText("---");
                }
            });

            gc.gridy = i;
            gc.gridx = 0; gc.weightx = 0.28; grid.add(pickLink, gc);
            gc.gridx = 1; gc.weightx = 0.62; grid.add(valLbl, gc);
            gc.gridx = 2; gc.weightx = 0.10; gc.anchor = GridBagConstraints.EAST; grid.add(clearLink, gc);
            gc.anchor = GridBagConstraints.WEST;
        }
        p.add(grid);
        return p;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(4, 12, 8, 12));

        chkContinue.setOpaque(false);
        chkContinue.setFont(UngDungPhongChu.BODY);
        chkContinue.setForeground(new Color(180, 0, 0));
        footer.add(chkContinue, BorderLayout.WEST);

        JButton okBtn = new JButton("✓ OK");
        okBtn.setFont(UngDungPhongChu.BODY_BOLD);
        okBtn.setForeground(new Color(0, 110, 60));
        okBtn.setPreferredSize(new Dimension(92, 30));
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.addActionListener(e -> runReport());
        getRootPane().setDefaultButton(okBtn);
        footer.add(okBtn, BorderLayout.EAST);
        return footer;
    }

    private void pickOtherRange() {
        String input = JOptionPane.showInputDialog(this,
                "Nhập khoảng thời gian (vd: 00:00 01/11/2026 - 23:59 30/11/2026):",
                fromField.getText() + " - " + toField.getText());
        if (input == null || !input.contains("-")) return;
        String[] parts = input.split("-", 2);
        customOtherFrom = parseDt(parts[0], LocalDate.now().minusDays(30).atStartOfDay());
        customOtherTo = parseDt(parts[1], LocalDate.now().atTime(23, 59, 59));
        rbOther.setSelected(true);
    }

    private void chooseFilter(String key, String title) {
        var ctx = KhamBenhPhienNguCanh.current();
        int facilityId = ctx == null ? 1 : ctx.facilityId();
        List<String> options = new ArrayList<>();
        try {
            options.addAll(readPort.listService019FilterValues(facilityId, key));
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Không tải được danh sách lọc: " + ex.getMessage(), title, JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (options.isEmpty()) {
            if ("PATIENT_SUBJECT".equals(key)) options.addAll(List.of("BHYT", "THU_PHI", "DICH_VU_YEU_CAU"));
            else if ("BHYT_GROUP".equals(key)) options.addAll(List.of("Trong danh mục BHYT", "Ngoài danh mục / Thu phí"));
        }

        if (options.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có dữ liệu cho \"" + title + "\".", title, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Object chosen = JOptionPane.showInputDialog(this, title + ":", title, JOptionPane.PLAIN_MESSAGE,
                null, options.toArray(), options.get(0));
        if (chosen instanceof String val) {
            activeFilters.put(key, val);
            filterValueLabels.get(key).setText(val);
        }
    }

    private void runReport() {
        LocalDateTime[] range = resolveDateRange();
        var ctx = KhamBenhPhienNguCanh.current();
        int facilityId = ctx == null ? 1 : ctx.facilityId();

        List<QuanLyDoanhThuReadPort.Service019RawRow> raw;
        try {
            raw = readPort.listService019Report(facilityId, searchByCombo.getSelectedIndex(), range[0], range[1]);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Không tải được báo cáo doanh thu dịch vụ chi tiết: " + ex.getMessage(),
                    "Báo Cáo Doanh Thu Theo Dịch Vụ Chi Tiết (019)", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<BaoCaoDoanhThuDichVuChiTietInHopThoai.DetailedServiceRow> rows = new ArrayList<>();
        String rec = Objects.toString(recordCombo.getSelectedItem(), "");
        for (var r : raw) {
            String hinhThuc = r.treatmentType().toUpperCase(Locale.ROOT);
            String dangKy = r.examType().toUpperCase(Locale.ROOT);
            if ("Nội trú".equals(rec) && !hinhThuc.contains("NOI_TRU")) continue;
            if ("Ngoại trú".equals(rec) && hinhThuc.contains("NOI_TRU")) continue;
            if ("Khám bệnh".equals(rec) && !dangKy.contains("KHAM") && !dangKy.contains("KHÁM")) continue;

            if (activeFilters.containsKey("ORDER_ROOM") && !activeFilters.get("ORDER_ROOM").equals(r.roomName())) continue;
            if (activeFilters.containsKey("EXEC_ROOM") && !activeFilters.get("EXEC_ROOM").equals(r.roomName())) continue;
            if (activeFilters.containsKey("SVC_GROUP") && !activeFilters.get("SVC_GROUP").equals(r.groupType())) continue;
            if (activeFilters.containsKey("SERVICE") && !activeFilters.get("SERVICE").equals(r.serviceName())) continue;
            if (activeFilters.containsKey("DRUG_SUPPLY") && !activeFilters.get("DRUG_SUPPLY").equals(r.serviceName())) continue;
            if (activeFilters.containsKey("ACCT_GROUP") && !activeFilters.get("ACCT_GROUP").equals(r.serviceSubject())) continue;
            if (activeFilters.containsKey("SVC_SUBJECT") && !r.serviceSubject().toUpperCase(Locale.ROOT).contains(activeFilters.get("SVC_SUBJECT"))) continue;
            if (activeFilters.containsKey("RECEIPT_BOOK") && !activeFilters.get("RECEIPT_BOOK").equals(r.receiptBook())) continue;
            if (activeFilters.containsKey("CASHIER") && !activeFilters.get("CASHIER").equals(r.cashierName())) continue;
            if (activeFilters.containsKey("ORDER_DOCTOR") && !activeFilters.get("ORDER_DOCTOR").equals(r.performer())) continue;
            if (activeFilters.containsKey("PATIENT_SUBJECT") && !r.patientSubject().toUpperCase(Locale.ROOT).contains(activeFilters.get("PATIENT_SUBJECT"))) continue;
            if (activeFilters.containsKey("BHYT_GROUP")) {
                boolean bhyt = r.serviceSubject().toUpperCase(Locale.ROOT).contains("BHYT");
                if ("Trong danh mục BHYT".equals(activeFilters.get("BHYT_GROUP")) && !bhyt) continue;
                if ("Ngoài danh mục / Thu phí".equals(activeFilters.get("BHYT_GROUP")) && bhyt) continue;
            }

            String outerGroup = rbGroupDate.isSelected() ? r.dateStr()
                    : (rbGroupOrderRoom.isSelected() || rbGroupExecRoom.isSelected()) ? r.roomName()
                    : rbGroupAcct.isSelected() ? r.serviceSubject()
                    : rbGroupSvcType.isSelected() ? formatGroupLabel(r.groupType())
                    : rbGroupBhyt.isSelected() ? (r.serviceSubject().toUpperCase(Locale.ROOT).contains("BHYT") ? "BHYT" : "Thu phí") + " / " + formatGroupLabel(r.groupType())
                    : "(Không nhóm dữ liệu)";

            rows.add(new BaoCaoDoanhThuDichVuChiTietInHopThoai.DetailedServiceRow(
                    outerGroup,
                    formatGroupLabel(r.groupType()),
                    r.serviceName(),
                    r.patientName(),
                    r.performer(),
                    r.qty(),
                    r.unitPrice(),
                    r.amount(),
                    r.discount(),
                    r.patientPay(),
                    r.paid()
            ));
        }

        new BaoCaoDoanhThuDichVuChiTietInHopThoai(
                this,
                rows,
                chkPatientDetail.isSelected(),
                chkMergeDiff.isSelected(),
                range[0],
                range[1]).setVisible(true);
        if (!chkContinue.isSelected()) dispose();
    }

    private static String formatGroupLabel(String code) {
        if (code == null) return "Khác";
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "KHAM_BENH" -> "Khám bệnh";
            case "DICH_VU_CLS" -> "Chẩn đoán hình ảnh / CLS";
            case "DON_THUOC" -> "Thuốc";
            default -> code;
        };
    }

    private LocalDateTime[] resolveDateRange() {
        if (rbOther.isSelected() && customOtherFrom != null && customOtherTo != null) {
            return new LocalDateTime[]{customOtherFrom, customOtherTo};
        }
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
        return new LocalDateTime[]{
                parseDt(fromField.getText(), LocalDate.now().minusDays(30).atStartOfDay()),
                parseDt(toField.getText(), LocalDate.now().atTime(23, 59, 59))
        };
    }

    private static int parseYear(String s) {
        try { return Integer.parseInt(s.replaceAll("\\D+", "")); }
        catch (Exception ex) { return LocalDate.now().getYear(); }
    }

    private static LocalDateTime parseDt(String s, LocalDateTime fallback) {
        try { return LocalDateTime.parse(s == null ? "" : s.trim(), DT_FMT); }
        catch (DateTimeParseException ex) { return fallback; }
    }

    private void setRangeDays(int startOffset, int endOffset) {
        LocalDate today = LocalDate.now();
        setRange(today.plusDays(startOffset), today.plusDays(endOffset));
    }

    private void setRange(LocalDate start, LocalDate end) {
        rbFromTo.setSelected(true);
        fromField.setText("00:00 " + start.format(DATE_FMT));
        toField.setText("23:59 " + end.format(DATE_FMT));
    }

    private static JLabel quickLink(String text, Runnable action) {
        JLabel l = new JLabel("<html><u>" + text + "</u></html>");
        l.setFont(UngDungPhongChu.BODY);
        l.setForeground(LINK_BLUE);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { action.run(); }
        });
        return l;
    }

    private static JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.BODY);
        return l;
    }

    private static String buildWindowTitle() {
        String user = Objects.toString(NguoiDungPhien.getInstance().getFullName(), "").trim();
        if (user.isEmpty()) user = "Quản Trị Hệ Thống";
        return "EHC - Tùy Chọn Báo Cáo   --- Người đăng nhập : " + user + " ---";
    }
}
