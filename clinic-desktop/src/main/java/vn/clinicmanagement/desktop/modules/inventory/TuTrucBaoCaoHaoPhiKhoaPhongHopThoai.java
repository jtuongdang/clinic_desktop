package vn.clinicmanagement.desktop.modules.inventory;


import vn.clinicmanagement.desktop.modules.print.inventory.TuTrucBaoCaoXuatNhapTonPreviewHopThoai;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * Hộp thoại "EHC - Tùy Chọn Báo Cáo" mở khi bấm
 * Báo Cáo -> Báo Cáo Xuất Hao Phí Khoa Phòng.
 */
final class TuTrucBaoCaoHaoPhiKhoaPhongHopThoai extends JDialog {
    private static final Color BORDER = new Color(200, 204, 210);
    private static final Color LINK_BLUE = new Color(0, 85, 200);

    final JRadioButton rbRange = new JRadioButton("Từ ngày", true);
    final JRadioButton rbMonth = new JRadioButton("Tháng");
    final JRadioButton rbQuarter = new JRadioButton("Quý");

    final JSpinner fromSpinner;
    final JSpinner untilSpinner;
    final JComboBox<String> monthCombo = new JComboBox<>();
    final JComboBox<String> monthYearCombo = new JComboBox<>();
    final JComboBox<String> quarterCombo = new JComboBox<>(new String[]{"Quý I", "Quý II", "Quý III", "Quý IV"});
    final JComboBox<String> quarterYearCombo = new JComboBox<>();

    final JRadioButton rbReqQty = new JRadioButton("Lấy theo số lượng yêu cầu", true);
    final JRadioButton rbIssuedQty = new JRadioButton("Lấy theo số lượng phát");

    TuTrucBaoCaoHaoPhiKhoaPhongHopThoai(Window owner) {
        super(owner, buildTitle(), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(700, 310);
        setResizable(false);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 246, 248));

        LocalDate today = LocalDate.now();
        fromSpinner = dateTimeSpinner(today.atStartOfDay());
        untilSpinner = dateTimeSpinner(today.atTime(23, 59));

        for (int m = 1; m <= 12; m++) monthCombo.addItem("Tháng " + m);
        monthCombo.setSelectedIndex(today.getMonthValue() - 1);
        for (int y = today.getYear() - 5; y <= today.getYear() + 2; y++) {
            String label = "Năm " + y;
            monthYearCombo.addItem(label);
            quarterYearCombo.addItem(label);
        }
        monthYearCombo.setSelectedItem("Năm " + today.getYear());
        quarterYearCombo.setSelectedItem("Năm " + today.getYear());
        quarterCombo.setSelectedIndex((today.getMonthValue() - 1) / 3);

        // 1. Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        JLabel title = new JLabel("TÙY CHỌN BÁO CÁO");
        title.setFont(UngDungPhongChu.SECTION);
        title.setForeground(new Color(175, 0, 0));
        title.setBorder(new EmptyBorder(8, 12, 8, 12));
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // 2. Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(new Color(245, 246, 248));
        body.setBorder(new EmptyBorder(8, 10, 6, 10));
        body.add(buildTimeSection());
        body.add(Box.createVerticalStrut(6));
        body.add(buildOptionSection());
        add(body, BorderLayout.CENTER);

        // 3. Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        footer.setBackground(new Color(245, 246, 248));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));
        JButton okBtn = new JButton("OK", greenCheckIcon());
        okBtn.setFont(UngDungPhongChu.BODY_BOLD);
        okBtn.setPreferredSize(new Dimension(102, 28));
        okBtn.addActionListener(e -> {
            var fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            String fromStr = fmt.format(spinnerDateTime(fromSpinner));
            String untilStr = fmt.format(spinnerDateTime(untilSpinner));
            String qtyMode = rbReqQty.isSelected() ? "Theo số lượng yêu cầu" : "Theo số lượng phát";
            dispose();
            new TuTrucBaoCaoXuatNhapTonPreviewHopThoai(
                    owner,
                    TuTrucBaoCaoXuatNhapTonPreviewHopThoai.ReportTemplate.HAO_PHI_KHOA_PHONG,
                    "BÁO CÁO XUẤT HAO PHÍ KHOA PHÒNG",
                    fromStr,
                    untilStr,
                    "(" + qtyMode + ")")
                    .setVisible(true);
        });
        footer.add(okBtn);
        add(footer, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okBtn);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "closeDialog");
        getRootPane().getActionMap().put("closeDialog", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    private JPanel buildTimeSection() {
        JPanel panel = sectionPanel("Thời gian lấy báo cáo");
        panel.setLayout(new GridBagLayout());
        ButtonGroup bg = new ButtonGroup();
        for (JRadioButton rb : new JRadioButton[]{rbRange, rbMonth, rbQuarter}) {
            rb.setFont(UngDungPhongChu.BODY);
            rb.setOpaque(false);
            bg.add(rb);
        }

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 4, 3, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridy = 0;
        gc.gridx = 0; gc.weightx = 0; panel.add(rbRange, gc);
        gc.gridx = 1; gc.weightx = 1; fromSpinner.setPreferredSize(new Dimension(135, 24)); panel.add(fromSpinner, gc);
        gc.gridx = 2; gc.weightx = 0; JLabel lblDen = new JLabel("Đến ngày"); lblDen.setFont(UngDungPhongChu.BODY); panel.add(lblDen, gc);
        gc.gridx = 3; gc.weightx = 1; untilSpinner.setPreferredSize(new Dimension(135, 24)); panel.add(untilSpinner, gc);
        gc.gridx = 4; gc.weightx = 0; panel.add(quickLink("Trong Ngày", () -> setRange(LocalDate.now(), LocalDate.now())), gc);
        gc.gridx = 5; gc.weightx = 0; panel.add(quickLink("Hôm Qua", () -> {
            LocalDate y = LocalDate.now().minusDays(1);
            setRange(y, y);
        }), gc);

        gc.gridy = 1;
        gc.gridx = 0; gc.weightx = 0; panel.add(rbMonth, gc);
        gc.gridx = 1; gc.weightx = 1; monthCombo.setFont(UngDungPhongChu.BODY); monthCombo.setPreferredSize(new Dimension(135, 24)); panel.add(monthCombo, gc);
        gc.gridx = 2; gc.weightx = 0; JLabel lblNam1 = new JLabel("Năm"); lblNam1.setFont(UngDungPhongChu.BODY); panel.add(lblNam1, gc);
        gc.gridx = 3; gc.weightx = 1; monthYearCombo.setFont(UngDungPhongChu.BODY); monthYearCombo.setPreferredSize(new Dimension(135, 24)); panel.add(monthYearCombo, gc);
        gc.gridx = 4; gc.weightx = 0; panel.add(quickLink("Trong Tuần", () -> {
            LocalDate now = LocalDate.now();
            setRange(now.with(DayOfWeek.MONDAY), now);
        }), gc);
        gc.gridx = 5; gc.weightx = 0; panel.add(quickLink("7 Ngày Gần Đây", () -> {
            LocalDate now = LocalDate.now();
            setRange(now.minusDays(6), now);
        }), gc);

        gc.gridy = 2;
        gc.gridx = 0; gc.weightx = 0; panel.add(rbQuarter, gc);
        gc.gridx = 1; gc.weightx = 1; quarterCombo.setFont(UngDungPhongChu.BODY); quarterCombo.setPreferredSize(new Dimension(135, 24)); panel.add(quarterCombo, gc);
        gc.gridx = 2; gc.weightx = 0; JLabel lblNam2 = new JLabel("Năm"); lblNam2.setFont(UngDungPhongChu.BODY); panel.add(lblNam2, gc);
        gc.gridx = 3; gc.weightx = 1; quarterYearCombo.setFont(UngDungPhongChu.BODY); quarterYearCombo.setPreferredSize(new Dimension(135, 24)); panel.add(quarterYearCombo, gc);
        gc.gridx = 4; gc.weightx = 0; panel.add(quickLink("Trong Tháng", () -> {
            LocalDate now = LocalDate.now();
            setRange(now.withDayOfMonth(1), now);
        }), gc);
        gc.gridx = 5; gc.weightx = 0; panel.add(quickLink("30 Ngày Gần Đây", () -> {
            LocalDate now = LocalDate.now();
            setRange(now.minusDays(29), now);
        }), gc);

        return panel;
    }

    private JPanel buildOptionSection() {
        JPanel panel = sectionPanel("Tùy chọn");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 4));
        ButtonGroup bg = new ButtonGroup();
        for (JRadioButton rb : new JRadioButton[]{rbReqQty, rbIssuedQty}) {
            rb.setFont(UngDungPhongChu.BODY);
            rb.setOpaque(false);
            bg.add(rb);
        }

        panel.add(quickLink("Lọc theo kho", () ->
                JOptionPane.showMessageDialog(this, "Lọc theo kho.", "Tùy Chọn", JOptionPane.INFORMATION_MESSAGE)));
        panel.add(quickLink("Lọc theo khoa", () ->
                JOptionPane.showMessageDialog(this, "Lọc theo khoa.", "Tùy Chọn", JOptionPane.INFORMATION_MESSAGE)));
        panel.add(quickLink("Lọc theo thuốc/vật tư", () ->
                JOptionPane.showMessageDialog(this, "Lọc theo thuốc/vật tư.", "Tùy Chọn", JOptionPane.INFORMATION_MESSAGE)));
        panel.add(rbReqQty);
        panel.add(rbIssuedQty);

        return panel;
    }

    private void setRange(LocalDate from, LocalDate until) {
        rbRange.setSelected(true);
        fromSpinner.setValue(Date.from(from.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        untilSpinner.setValue(Date.from(until.atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant()));
    }

    private static JPanel sectionPanel(String title) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER), title);
        tb.setTitleFont(UngDungPhongChu.SMALL);
        p.setBorder(BorderFactory.createCompoundBorder(tb, new EmptyBorder(4, 8, 6, 8)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static JLabel quickLink(String text, Runnable action) {
        JLabel lbl = new JLabel("<html><u>" + text + "</u></html>");
        lbl.setFont(UngDungPhongChu.SMALL);
        lbl.setForeground(LINK_BLUE);
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (action != null) action.run();
            }
        });
        return lbl;
    }

    private static JSpinner dateTimeSpinner(LocalDateTime dt) {
        Date val = Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
        JSpinner s = new JSpinner(new SpinnerDateModel(val, null, null, Calendar.MINUTE));
        s.setEditor(new JSpinner.DateEditor(s, "HH:mm dd/MM/yyyy"));
        s.setFont(UngDungPhongChu.BODY);
        return s;
    }

    private static LocalDateTime spinnerDateTime(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static String buildTitle() {
        String user = Objects.toString(NguoiDungPhien.getInstance().getFullName(), "").trim();
        if (user.isEmpty()) user = "Quản Trị Hệ Thống";
        return "EHC - Tùy Chọn Báo Cáo   ----- Người đăng nhập : " + user + " -----";
    }

    private static Icon greenCheckIcon() {
        return new Icon() {
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);
                g.setColor(new Color(46, 139, 87));
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(new int[]{2, 6, 14}, new int[]{9, 13, 3}, 3);
                g.dispose();
            }
        };
    }
}
