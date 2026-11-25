package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.FilterItem;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.XuatNhapTonOptions;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Hộp thoại Tùy Chọn Báo Cáo Xuất Nhập Tồn (khớp giao diện EHC - Tùy Chọn Báo Cáo). */
public final class TuyChonBaoCaoXuatNhapTonHopThoai extends JPanel {

    public interface ConfirmListener {
        void onConfirm(LocalDateTime from, LocalDateTime to, XuatNhapTonOptions options);
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Color LINK_BLUE = new Color(10, 86, 179);

    private final NhaThuocBaoCaoReadPort reportRead;
    private final ConfirmListener confirmListener;
    private Runnable closeAction;

    // Thời gian lấy báo cáo
    private final JRadioButton radRange = new JRadioButton("Từ ngày", true);
    private final JRadioButton radMonth = new JRadioButton("Tháng");
    private final JRadioButton radQuarter = new JRadioButton("Quý");
    private final JTextField txtFrom = new JTextField(13);
    private final JTextField txtTo = new JTextField(13);
    private final JComboBox<String> cboMonth = new JComboBox<>();
    private final JComboBox<String> cboMonthYear = new JComboBox<>();
    private final JComboBox<String> cboQuarter = new JComboBox<>(new String[]{"Quý I", "Quý II", "Quý III", "Quý IV"});
    private final JComboBox<String> cboQuarterYear = new JComboBox<>();

    // Nhóm dữ liệu
    private final JRadioButton radNoGroup = new JRadioButton("Không nhóm dữ liệu", true);
    private final JRadioButton radItemType = new JRadioButton("Loại thuốc, vật tư");
    private final JRadioButton radWarehouse = new JRadioButton("Phòng lưu");
    private final JRadioButton radSupplier = new JRadioButton("Nhà cung cấp");
    private final JRadioButton radImporter = new JRadioButton("Người nhập");
    private final JRadioButton radExporter = new JRadioButton("Người xuất");
    private final JRadioButton radPharmGroup = new JRadioButton("Nhóm dược lý");
    private final JComboBox<String> cboSort = new JComboBox<>(new String[]{
            "", "Tên thuốc", "Mã thuốc", "Giá trị tồn giảm dần", "Số lượng tồn giảm dần"
    });

    // Tùy chọn
    private final JCheckBox chkMergeCode = new JCheckBox("In gộp khi cùng mã");
    private final JCheckBox chkMergeFull = new JCheckBox("In gộp khi cùng mã, giá, số lô, HSD, SĐK");
    private final JCheckBox chkIncludeLocked = new JCheckBox("In cả thuốc/vật tư đã khóa");
    private final JComboBox<String> cboPrintMode = new JComboBox<>(new String[]{
            "In tất cả", "Chỉ in hàng có phát sinh", "Chỉ in hàng còn tồn > 0", "Chỉ in hàng tồn = 0"
    });
    private final JCheckBox chkCurrentUser = new JCheckBox("Chỉ in theo người dùng đang đăng nhập");

    // Bộ lọc liên kết
    private String supplierFilter = "";
    private String actorFilter = "";
    private String sourceFilter = "";
    private List<Integer> selectedWarehouseIds = List.of();
    private final JButton btnOk = new JButton("✓  OK");

    public TuyChonBaoCaoXuatNhapTonHopThoai(
            NhaThuocBaoCaoReadPort reportRead,
            LocalDateTime initialFrom,
            LocalDateTime initialTo,
            ConfirmListener confirmListener
    ) {
        this.reportRead = reportRead;
        this.confirmListener = Objects.requireNonNull(confirmListener, "confirmListener");

        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 14, 12, 14));
        setPreferredSize(new Dimension(760, 415));

        JLabel heading = new JLabel("TÙY CHỌN BÁO CÁO");
        heading.setFont(UngDungPhongChu.BODY_BOLD.deriveFont(15f));
        heading.setForeground(new Color(163, 29, 36));
        add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(buildTimeSection(initialFrom, initialTo));
        center.add(Box.createVerticalStrut(6));
        center.add(buildGroupSection());
        center.add(Box.createVerticalStrut(6));
        center.add(buildOptionsSection());
        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        btnOk.setFont(UngDungPhongChu.BODY_BOLD);
        btnOk.setPreferredSize(new Dimension(112, 32));
        btnOk.setBackground(new Color(242, 246, 250));
        btnOk.setFocusPainted(false);
        btnOk.setBorder(new LineBorder(new Color(145, 165, 185)));
        btnOk.addActionListener(e -> confirm());
        bottom.add(btnOk);
        add(bottom, BorderLayout.SOUTH);
    }

    public void setCloseAction(Runnable closeAction) {
        this.closeAction = closeAction;
    }

    public JButton getBtnOk() {
        return btnOk;
    }

    public JCheckBox getChkIncludeLocked() {
        return chkIncludeLocked;
    }

    public JComboBox<String> getCboPrintMode() {
        return cboPrintMode;
    }

    public JRadioButton getRadWarehouse() {
        return radWarehouse;
    }

    private JPanel buildTimeSection(LocalDateTime initialFrom, LocalDateTime initialTo) {
        JPanel panel = createTitledPanel("Thời gian lấy báo cáo");
        panel.setLayout(new BorderLayout(12, 0));

        ButtonGroup bg = new ButtonGroup();
        bg.add(radRange);
        bg.add(radMonth);
        bg.add(radQuarter);

        LocalDate today = initialFrom != null ? initialFrom.toLocalDate() : LocalDate.now();
        txtFrom.setText((initialFrom != null ? initialFrom : today.atStartOfDay()).format(FMT));
        txtTo.setText((initialTo != null ? initialTo : LocalDateTime.of(today, LocalTime.of(23, 59))).format(FMT));

        for (int m = 1; m <= 12; m++) cboMonth.addItem("Tháng " + m);
        cboMonth.setSelectedIndex(today.getMonthValue() - 1);

        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear - 5; y <= currentYear + 1; y++) {
            cboMonthYear.addItem("Năm " + y);
            cboQuarterYear.addItem("Năm " + y);
        }
        cboMonthYear.setSelectedItem("Năm " + today.getYear());
        cboQuarterYear.setSelectedItem("Năm " + today.getYear());
        cboQuarter.setSelectedIndex((today.getMonthValue() - 1) / 3);

        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; left.add(radRange, g);
        g.gridx = 1; left.add(txtFrom, g);
        g.gridx = 2; left.add(new JLabel("Đến ngày"), g);
        g.gridx = 3; left.add(txtTo, g);

        g.gridx = 0; g.gridy = 1; left.add(radMonth, g);
        g.gridx = 1; left.add(cboMonth, g);
        g.gridx = 2; left.add(new JLabel("Năm"), g);
        g.gridx = 3; left.add(cboMonthYear, g);

        g.gridx = 0; g.gridy = 2; left.add(radQuarter, g);
        g.gridx = 1; left.add(cboQuarter, g);
        g.gridx = 2; left.add(new JLabel("Năm"), g);
        g.gridx = 3; left.add(cboQuarterYear, g);

        styleRadio(radRange, radMonth, radQuarter);
        cboMonth.setPreferredSize(new Dimension(130, 22));
        cboMonthYear.setPreferredSize(new Dimension(130, 22));
        cboQuarter.setPreferredSize(new Dimension(130, 22));
        cboQuarterYear.setPreferredSize(new Dimension(130, 22));

        JPanel links = new JPanel(new GridLayout(3, 2, 16, 4));
        links.setOpaque(false);
        links.add(createLink("Trong Ngày", () -> setDatePreset(LocalDate.now(), LocalDate.now())));
        links.add(createLink("Hôm Qua", () -> setDatePreset(LocalDate.now().minusDays(1), LocalDate.now().minusDays(1))));
        links.add(createLink("Trong Tuần", () -> setDatePreset(LocalDate.now().with(DayOfWeek.MONDAY), LocalDate.now())));
        links.add(createLink("7 Ngày Gần Đây", () -> setDatePreset(LocalDate.now().minusDays(6), LocalDate.now())));
        links.add(createLink("Trong Tháng", () -> setDatePreset(LocalDate.now().withDayOfMonth(1), LocalDate.now())));
        links.add(createLink("30 Ngày Gần Đây", () -> setDatePreset(LocalDate.now().minusDays(29), LocalDate.now())));

        panel.add(left, BorderLayout.WEST);
        panel.add(links, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildGroupSection() {
        JPanel panel = createTitledPanel("Nhóm dữ liệu");
        panel.setLayout(new GridBagLayout());
        ButtonGroup bg = new ButtonGroup();
        JRadioButton[] radios = {radNoGroup, radItemType, radWarehouse, radSupplier, radImporter, radExporter, radPharmGroup};
        for (JRadioButton r : radios) bg.add(r);
        styleRadio(radios);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 6, 2, 14);
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;

        g.gridx = 0; g.gridy = 0; panel.add(radNoGroup, g);
        g.gridx = 1; panel.add(radItemType, g);
        g.gridx = 2; panel.add(radWarehouse, g);

        g.gridx = 0; g.gridy = 1; panel.add(radSupplier, g);
        g.gridx = 1; panel.add(radImporter, g);
        g.gridx = 2; panel.add(radExporter, g);

        g.gridx = 0; g.gridy = 2; panel.add(radPharmGroup, g);
        JPanel sortBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        sortBox.setOpaque(false);
        JLabel lblSort = new JLabel("Sắp xếp");
        lblSort.setFont(UngDungPhongChu.BODY);
        cboSort.setPreferredSize(new Dimension(210, 22));
        sortBox.add(lblSort);
        sortBox.add(cboSort);
        g.gridx = 1; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL;
        panel.add(sortBox, g);
        return panel;
    }

    private JPanel buildOptionsSection() {
        JPanel panel = createTitledPanel("Tùy chọn");
        panel.setLayout(new GridBagLayout());
        chkMergeCode.setOpaque(false);
        chkMergeFull.setOpaque(false);
        chkIncludeLocked.setOpaque(false);
        chkCurrentUser.setOpaque(false);
        chkMergeCode.setFont(UngDungPhongChu.BODY);
        chkMergeFull.setFont(UngDungPhongChu.BODY);
        chkIncludeLocked.setFont(UngDungPhongChu.BODY);
        chkCurrentUser.setFont(UngDungPhongChu.BODY);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 6, 3, 10);
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;

        g.gridx = 0; g.gridy = 0; panel.add(chkMergeCode, g);
        g.gridx = 1; panel.add(chkMergeFull, g);
        g.gridx = 2; panel.add(chkIncludeLocked, g);

        JPanel modeBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        modeBox.setOpaque(false);
        JLabel lblMode = new JLabel("Tùy chọn in xuất nhập tồn");
        lblMode.setFont(UngDungPhongChu.BODY);
        cboPrintMode.setPreferredSize(new Dimension(210, 22));
        modeBox.add(lblMode);
        modeBox.add(cboPrintMode);
        g.gridx = 0; g.gridy = 1; g.gridwidth = 2;
        panel.add(modeBox, g);
        g.gridx = 2; g.gridwidth = 1;
        panel.add(chkCurrentUser, g);

        JPanel filterLinks = new JPanel(new GridLayout(1, 4, 12, 0));
        filterLinks.setOpaque(false);
        filterLinks.add(createLink("Lọc theo nhà cung cấp", this::chooseSupplierFilter));
        filterLinks.add(createLink("Lọc theo người nhập, xuất", this::chooseActorFilter));
        filterLinks.add(createLink("Tổng hợp nhiều kho", this::chooseWarehouses));
        filterLinks.add(createLink("Lọc theo nguồn nhập", this::chooseSourceFilter));
        g.gridx = 0; g.gridy = 2; g.gridwidth = 3; g.fill = GridBagConstraints.HORIZONTAL;
        panel.add(filterLinks, g);
        return panel;
    }

    private void setDatePreset(LocalDate from, LocalDate to) {
        radRange.setSelected(true);
        txtFrom.setText(from.atStartOfDay().format(FMT));
        txtTo.setText(LocalDateTime.of(to, LocalTime.of(23, 59)).format(FMT));
    }

    private void chooseSupplierFilter() {
        var auth = KhamBenhPhienNguCanh.current();
        List<FilterItem> items = (reportRead != null && auth != null) ? reportRead.getSuppliers(auth.facilityId()) : List.of();
        String picked = promptFilterValue("Lọc theo nhà cung cấp", "Nhập hoặc chọn tên nhà cung cấp (để trống = tất cả):",
                items.stream().map(FilterItem::name).toList(), supplierFilter);
        if (picked != null) supplierFilter = picked;
    }

    private void chooseActorFilter() {
        var auth = KhamBenhPhienNguCanh.current();
        List<FilterItem> items = (reportRead != null && auth != null) ? reportRead.getActors(auth.facilityId()) : List.of();
        String picked = promptFilterValue("Lọc theo người nhập, xuất", "Nhập hoặc chọn nhân viên (để trống = tất cả):",
                items.stream().map(FilterItem::name).toList(), actorFilter);
        if (picked != null) actorFilter = picked;
    }

    private void chooseSourceFilter() {
        String picked = promptFilterValue("Lọc theo nguồn nhập", "Chọn hoặc nhập trạng thái/nguồn nhập (để trống = tất cả):",
                List.of("", "HOAN_THANH", "DA_DUYET", "NHAP_NCC"), sourceFilter);
        if (picked != null) sourceFilter = picked;
    }

    private void chooseWarehouses() {
        var auth = KhamBenhPhienNguCanh.current();
        List<FilterItem> items = (reportRead != null && auth != null) ? reportRead.getWarehouses(auth.facilityId()) : List.of();
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có danh sách kho để tổng hợp.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        List<JCheckBox> checks = new ArrayList<>();
        for (FilterItem wh : items) {
            JCheckBox cb = new JCheckBox(wh.name(), selectedWarehouseIds.contains(wh.id()));
            checks.add(cb);
            box.add(cb);
        }
        int res = JOptionPane.showConfirmDialog(this, new JScrollPane(box), "Tổng hợp nhiều kho", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            List<Integer> next = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                if (checks.get(i).isSelected()) next.add(items.get(i).id());
            }
            selectedWarehouseIds = List.copyOf(next);
        }
    }

    private String promptFilterValue(String title, String message, List<String> suggestions, String currentValue) {
        JComboBox<String> combo = new JComboBox<>();
        combo.setEditable(true);
        combo.addItem("");
        for (String s : suggestions) {
            if (s != null && !s.isBlank()) combo.addItem(s);
        }
        combo.setSelectedItem(currentValue == null ? "" : currentValue);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(new JLabel(message), BorderLayout.NORTH);
        panel.add(combo, BorderLayout.CENTER);
        int res = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return null;
        return Objects.toString(combo.getEditor().getItem(), "").trim();
    }

    private void confirm() {
        LocalDateTime[] range;
        try {
            range = resolveDateRange();
        } catch (IllegalArgumentException ex) {
            if (isShowing()) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Kiểm tra ngày", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        String groupBy = radItemType.isSelected() ? "ITEM_TYPE"
                : radWarehouse.isSelected() ? "WAREHOUSE"
                : radSupplier.isSelected() ? "SUPPLIER"
                : radImporter.isSelected() ? "IMPORTER"
                : radExporter.isSelected() ? "EXPORTER"
                : radPharmGroup.isSelected() ? "PHARMACY_GROUP"
                : "NONE";
        String sortBy = switch (cboSort.getSelectedIndex()) {
            case 1 -> "NAME";
            case 2 -> "CODE";
            case 3 -> "STOCK_VALUE_DESC";
            case 4 -> "STOCK_QTY_DESC";
            default -> "";
        };
        String printFilter = switch (cboPrintMode.getSelectedIndex()) {
            case 1 -> "WITH_MOVEMENT";
            case 2 -> "STOCK_GT_ZERO";
            case 3 -> "STOCK_ZERO";
            default -> "ALL";
        };
        var auth = KhamBenhPhienNguCanh.current();
        Integer currentUserId = auth != null ? auth.employeeId() : null;
        XuatNhapTonOptions options = new XuatNhapTonOptions(
                groupBy,
                sortBy,
                chkMergeCode.isSelected(),
                chkMergeFull.isSelected(),
                chkIncludeLocked.isSelected(),
                printFilter,
                chkCurrentUser.isSelected(),
                currentUserId,
                selectedWarehouseIds,
                supplierFilter,
                actorFilter,
                sourceFilter
        );
        if (closeAction != null) closeAction.run();
        confirmListener.onConfirm(range[0], range[1], options);
    }

    private LocalDateTime[] resolveDateRange() {
        if (radMonth.isSelected()) {
            int month = cboMonth.getSelectedIndex() + 1;
            int year = parseYear((String) cboMonthYear.getSelectedItem());
            YearMonth ym = YearMonth.of(year, month);
            return new LocalDateTime[]{ym.atDay(1).atStartOfDay(), ym.atEndOfMonth().plusDays(1).atStartOfDay()};
        }
        if (radQuarter.isSelected()) {
            int startMonth = cboQuarter.getSelectedIndex() * 3 + 1;
            int year = parseYear((String) cboQuarterYear.getSelectedItem());
            LocalDate start = LocalDate.of(year, startMonth, 1);
            LocalDate end = start.plusMonths(3);
            return new LocalDateTime[]{start.atStartOfDay(), end.atStartOfDay()};
        }
        try {
            LocalDateTime from = LocalDateTime.parse(txtFrom.getText().trim(), FMT);
            LocalDateTime to = LocalDateTime.parse(txtTo.getText().trim(), FMT).plusMinutes(1);
            if (!from.isBefore(to)) throw new IllegalArgumentException();
            return new LocalDateTime[]{from, to};
        } catch (Exception ex) {
            throw new IllegalArgumentException("Ngày phải có dạng HH:mm dd/MM/yyyy và Từ ngày phải trước Đến ngày.");
        }
    }

    private static int parseYear(String label) {
        return Integer.parseInt(Objects.toString(label, "2023").replace("Năm", "").trim());
    }

    private static JPanel createTitledPanel(String title) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(189, 202, 215)),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UngDungPhongChu.BODY,
                new Color(45, 58, 72)
        ));
        return p;
    }

    private static void styleRadio(JRadioButton... radios) {
        for (JRadioButton r : radios) {
            r.setOpaque(false);
            r.setFont(UngDungPhongChu.BODY);
        }
    }

    private static JLabel createLink(String text, Runnable action) {
        JLabel link = new JLabel("<html><u>" + text + "</u></html>");
        link.setFont(UngDungPhongChu.BODY);
        link.setForeground(LINK_BLUE);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        return link;
    }
}
