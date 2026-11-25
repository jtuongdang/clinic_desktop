package vn.clinicmanagement.desktop.modules.billing;

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
 * Hộp thoại "EHC - Tùy Chọn Báo Cáo" mở khi bấm Báo Cáo -> Báo Cáo Doanh Thu Theo Phòng Khám.
 * Kết nối trực tiếp dữ liệu thật từ LuotKham, PhongKham, Khoa, HoaDon, ChiTietHoaDon trong clinicDb.
 */
public final class TuyChonBaoCaoPhongKhamHopThoai extends JDialog {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Color LINK_BLUE = new Color(0, 78, 178);

    private final QuanLyDoanhThuReadPort readPort;

    final JRadioButton rbFromTo = new JRadioButton("Từ ngày", true);
    final JTextField fromField = new JTextField();
    final JTextField toField = new JTextField();

    final JRadioButton rbMonth = new JRadioButton("Tháng");
    final JComboBox<String> monthCombo = new JComboBox<>();
    final JComboBox<String> monthYearCombo = new JComboBox<>();

    final JRadioButton rbQuarter = new JRadioButton("Quý");
    final JComboBox<String> quarterCombo = new JComboBox<>(new String[]{"Quý I", "Quý II", "Quý III", "Quý IV"});
    final JComboBox<String> quarterYearCombo = new JComboBox<>();

    final JRadioButton rbNoGroup = new JRadioButton("Không nhóm dữ liệu", true);
    final JRadioButton rbGroupDate = new JRadioButton("Nhóm theo ngày");

    final JCheckBox chkPatientDetail = new JCheckBox("Chi tiết bệnh nhân");
    final JCheckBox chkServiceDetail = new JCheckBox("Chi tiết dịch vụ");
    final JCheckBox chkPaidOnly = new JCheckBox("Chỉ in dịch vụ đã thu tiền");

    private Integer selectedRoomId = null;
    private String selectedRoomName = "";
    private final JLabel roomFilterLink = new JLabel("<html><u>Lọc phòng khám</u></html>");

    public TuyChonBaoCaoPhongKhamHopThoai(Window owner, QuanLyDoanhThuReadPort readPort,
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

        JLabel header = new JLabel("TÙY CHỌN BÁO CÁO");
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        header.setForeground(new Color(180, 0, 0));
        header.setBorder(new EmptyBorder(10, 14, 2, 14));
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 12, 4, 12));
        body.add(buildTimePanel());
        body.add(Box.createVerticalStrut(6));
        body.add(buildOptionsPanel());
        add(body, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(690, 390));
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
        for (JComponent c : List.of(rbFromTo, rbMonth, rbQuarter)) {
            c.setOpaque(false);
            c.setFont(UngDungPhongChu.BODY);
        }
        for (JComboBox<?> cb : List.of(monthCombo, monthYearCombo, quarterCombo, quarterYearCombo)) {
            cb.setFont(UngDungPhongChu.BODY);
            cb.setPreferredSize(new Dimension(145, 23));
        }
        fromField.setFont(UngDungPhongChu.BODY);
        fromField.setPreferredSize(new Dimension(145, 23));
        toField.setFont(UngDungPhongChu.BODY);
        toField.setPreferredSize(new Dimension(145, 23));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 6, 3, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0
        gc.gridy = 0; gc.gridx = 0; gc.weightx = 0; p.add(rbFromTo, gc);
        gc.gridx = 1; gc.weightx = 0.25; p.add(fromField, gc);
        gc.gridx = 2; gc.weightx = 0; p.add(lbl("Đến ngày"), gc);
        gc.gridx = 3; gc.weightx = 0.25; p.add(toField, gc);
        gc.gridx = 4; gc.weightx = 0.15; p.add(quickLink("Trong Ngày", () -> setRangeDays(0, 0)), gc);
        gc.gridx = 5; gc.weightx = 0.15; p.add(quickLink("Hôm Qua", () -> setRangeDays(-1, -1)), gc);

        // Row 1
        gc.gridy = 1; gc.gridx = 0; gc.weightx = 0; p.add(rbMonth, gc);
        gc.gridx = 1; gc.weightx = 0.25; p.add(monthCombo, gc);
        gc.gridx = 2; gc.weightx = 0; p.add(lbl("Năm"), gc);
        gc.gridx = 3; gc.weightx = 0.25; p.add(monthYearCombo, gc);
        gc.gridx = 4; gc.weightx = 0.15; p.add(quickLink("Trong Tuần", () -> {
            LocalDate now = LocalDate.now();
            setRange(now.with(DayOfWeek.MONDAY), now);
        }), gc);
        gc.gridx = 5; gc.weightx = 0.15; p.add(quickLink("7 Ngày Gần Đây", () -> setRangeDays(-6, 0)), gc);

        // Row 2
        gc.gridy = 2; gc.gridx = 0; gc.weightx = 0; p.add(rbQuarter, gc);
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
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(195, 198, 204)),
                "Tùy chọn", TitledBorder.LEFT, TitledBorder.TOP,
                UngDungPhongChu.SMALL, Color.DARK_GRAY));

        ButtonGroup grp = new ButtonGroup();
        grp.add(rbNoGroup);
        grp.add(rbGroupDate);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 10, 4, 16);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridy = 0; gc.gridx = 0; p.add(style(rbNoGroup), gc);
        gc.gridx = 1; p.add(style(rbGroupDate), gc);

        gc.gridy = 1; gc.gridx = 0; p.add(style(chkPatientDetail), gc);
        gc.gridx = 1; p.add(style(chkServiceDetail), gc);
        gc.gridx = 2; gc.weightx = 1.0; p.add(style(chkPaidOnly), gc);

        roomFilterLink.setFont(UngDungPhongChu.BODY);
        roomFilterLink.setForeground(LINK_BLUE);
        roomFilterLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        roomFilterLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { pickClinicRoom(); }
        });
        gc.gridy = 2; gc.gridx = 0; gc.gridwidth = 3;
        p.add(roomFilterLink, gc);
        return p;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        footer.setOpaque(false);

        JButton okBtn = new JButton("✓ OK");
        okBtn.setFont(UngDungPhongChu.BODY_BOLD);
        okBtn.setForeground(new Color(0, 110, 60));
        okBtn.setBackground(new Color(252, 232, 166));
        okBtn.setPreferredSize(new Dimension(112, 32));
        okBtn.setBorder(BorderFactory.createLineBorder(new Color(210, 168, 72)));
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.addActionListener(e -> runReport());
        getRootPane().setDefaultButton(okBtn);
        footer.add(okBtn);
        return footer;
    }

    private void pickClinicRoom() {
        var ctx = KhamBenhPhienNguCanh.current();
        int facilityId = ctx == null ? 1 : ctx.facilityId();
        List<QuanLyDoanhThuReadPort.RoomOption> options = new ArrayList<>();
        options.add(new QuanLyDoanhThuReadPort.RoomOption(null, "(Tất cả phòng khám)"));
        try {
            options.addAll(readPort.listClinicRooms(facilityId));
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Không tải được danh sách phòng khám.", "Lọc phòng khám", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Object chosen = JOptionPane.showInputDialog(this, "Chọn phòng khám:", "Lọc phòng khám",
                JOptionPane.PLAIN_MESSAGE, null, options.toArray(), options.get(0));
        if (chosen instanceof QuanLyDoanhThuReadPort.RoomOption opt) {
            selectedRoomId = opt.id();
            selectedRoomName = opt.id() == null ? "" : opt.name();
            roomFilterLink.setText(opt.id() == null
                    ? "<html><u>Lọc phòng khám</u></html>"
                    : "<html><u>Lọc phòng khám: " + opt.name() + "</u></html>");
        }
    }

    private void runReport() {
        LocalDateTime[] range = resolveDateRange();
        var ctx = KhamBenhPhienNguCanh.current();
        int facilityId = ctx == null ? 1 : ctx.facilityId();
        boolean groupByDate = rbGroupDate.isSelected();
        boolean withPatient = chkPatientDetail.isSelected();
        boolean withService = chkServiceDetail.isSelected();
        boolean paidOnly = chkPaidOnly.isSelected();

        List<BaoCaoDoanhThuPhongKhamInHopThoai.VisitDetail> visits;
        try {
            visits = readPort.listRoomReport(facilityId, range[0], range[1], selectedRoomId, groupByDate, withService, paidOnly)
                    .stream()
                    .map(v -> new BaoCaoDoanhThuPhongKhamInHopThoai.VisitDetail(
                            v.visitId(), v.dateStr(), v.roomName(), v.patientName(),
                            v.birthMale(), v.birthFemale(), v.address(),
                            v.amount(), v.discount(), v.patientPay(), v.paid(),
                            v.services().stream()
                                    .map(s -> new BaoCaoDoanhThuPhongKhamInHopThoai.ServiceLine(
                                            s.name(), s.qty(), s.amount(), s.discount(), s.patientPay()))
                                    .toList()))
                    .toList();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Không tải được báo cáo theo phòng khám: " + ex.getMessage(),
                    "Báo Cáo Doanh Thu Theo Phòng Khám", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new BaoCaoDoanhThuPhongKhamInHopThoai(this, visits, groupByDate, withPatient, withService, selectedRoomName, range[0], range[1]).setVisible(true);
    }

    private record RoomOption(Integer id, String name) {
        @Override public String toString() { return name; }
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

    private static <T extends AbstractButton> T style(T btn) {
        btn.setOpaque(false);
        btn.setFont(UngDungPhongChu.BODY);
        return btn;
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
