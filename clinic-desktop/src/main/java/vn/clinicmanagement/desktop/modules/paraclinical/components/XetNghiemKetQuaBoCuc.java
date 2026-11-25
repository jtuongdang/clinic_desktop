package vn.clinicmanagement.desktop.modules.paraclinical.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import static vn.clinicmanagement.desktop.modules.paraclinical.components.LayMauBieuTuong.Kind.*;

/** Laboratory results workspace measured against the 1586 x 888 reference content area. */
public final class XetNghiemKetQuaBoCuc {
    public static final Color BLUE = new Color(0, 0, 205);
    public static final Color SELECT = new Color(0, 130, 255);
    private static final Color LINE = new Color(185, 185, 185);
    public final JComboBox<String> dateType = new JComboBox<>(new String[]{"Ngày y lệnh", "Ngày lấy mẫu", "Ngày trả kết quả"});
    public final JComboBox<String> status = new JComboBox<>(new String[]{"(Tất cả phiếu)", "Đã lấy mẫu", "Đang thực hiện", "Đã duyệt KQ", "Đã trả KQ", "Đã hủy"});
    public final JCheckBox outpatient = new JCheckBox("Ngoại trú", true), inpatient = new JCheckBox("Nội trú", true);
    public final JButton menu = tool(MENU, "Menu"), print = tool(PRINT, "In"), reload = tool(REFRESH, "Làm Mới"),
            search = tool(SEARCH, "Tìm Kiếm"), waiting = tool(LIST, "Màn Hình Chờ"), clear = flat(CLEAR, ""),
            expand = flat(EXPAND, "Phóng To"), save = flat(SAVE, "Lưu"), history = flat(HISTORY, "Lịch Sử"),
            chart = flat(CHART, "Biểu Đồ"), execute = flat(COLLECT, "Thực Hiện"),
            runFormula = flat(FORMULA, "Cấu hình CLS"), cancelExecute = flat(CANCEL_EXEC, "Hủy Thực Hiện"),
            approveResult = flat(APPROVE, "Duyệt Kết Quả"), unapproveResult = flat(CANCEL_EXEC, "Hủy Duyệt"),
            releaseResult = flat(COLLECT, "Trả Kết Quả"), printResultFooter = flat(PRINT, "In Kết Quả (F3)");
    public final JLabel patient = banner(true), insurance = banner(false), address = banner(false),
            insuranceType = label(" "), insuranceRate = label(" ");
    public final JTextField keyword = new JTextField();
    public final JTextField conclusion = new JTextField();
    public final JComboBox<String> room = new JComboBox<>(new String[]{"(Tất cả phòng xét nghiệm)"});
    public final JLabel orderCode = value(), executionCode = value(), barcode = value(), diagnosis = value(),
            ordered = value(), collected = value(), performed = value(), feedback = label(" ");
    private boolean executingMode = false;
    public final DefaultTableModel queueModel = new DefaultTableModel(new String[]{"", "Barcode", "Tên Bệnh Nhân", "STT Lấy"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    public final DefaultTableModel resultModel = new DefaultTableModel(new String[]{"Tên Xét Nghiệm", "SL", "Kết Quả", "Đơn Vị", "Loại Bệnh Phẩm", "Ghi Chú (Thực Hiện)", "Máy XN/Người Nhập", "Mã XN"}, 0) {
        @Override public boolean isCellEditable(int row, int col) {
            return executingMode && row > 0 && (col == 2 || col == 3 || col == 5);
        }
    };
    public final JTable queue = new JTable(queueModel) {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(220, 222, 226));
            int start = getRowCount() * getRowHeight();
            for (int y = start; y < getHeight(); y += getRowHeight()) g.drawLine(0, y, getWidth(), y);
            int x = 0;
            for (int c = 0; c < getColumnCount(); c++) {
                x += getColumnModel().getColumn(c).getWidth(); g.drawLine(x, start, x, getHeight());
            }
        }
    };
    public final JTable results = new JTable(resultModel);
    public final JPanel toolbar = toolbar();
    public final JPanel metadata = metadata();
    public final JPanel searchStrip = searchStrip();
    public final JPanel queueFooter = new JPanel(new BorderLayout());
    public final JPanel controls = controls();
    public final JScrollPane queueScroll;
    public final JScrollPane resultScroll;
    public final JSplitPane split;
    private double scale = 1;

    public XetNghiemKetQuaBoCuc(JPanel host) {
        host.setLayout(new BorderLayout()); host.setBackground(Color.WHITE);
        host.setPreferredSize(new Dimension(1586, 888));
        configureTable(queue); configureTable(results);
        queue.setFillsViewportHeight(true); queue.setShowGrid(true);
        queue.setGridColor(new Color(220, 222, 226));
        results.setShowGrid(false);
        queueScroll = scroll(queue); resultScroll = scroll(results);
        JPanel left = new JPanel(new BorderLayout()); left.setMinimumSize(new Dimension());
        left.add(searchStrip, BorderLayout.NORTH); left.add(queueScroll);
        room.setFont(UngDungPhongChu.XET_NGHIEM); room.setForeground(Color.RED);
        queueFooter.add(room); queueFooter.setBorder(new MatteBorder(1, 0, 0, 0, LINE));
        left.add(queueFooter, BorderLayout.SOUTH);
        JPanel right = new JPanel(new BorderLayout()); right.setBackground(Color.WHITE); right.setMinimumSize(new Dimension());
        right.add(metadata, BorderLayout.NORTH);
        // A real category row scrolls with the result, followed by test rows.
        resultScroll.setRowHeaderView(null);
        right.add(resultScroll);
        right.add(controls, BorderLayout.SOUTH);
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setBorder(null); split.setDividerSize(2); split.setContinuousLayout(true); split.setResizeWeight(388.0 / 1586);
        host.add(toolbar, BorderLayout.NORTH); host.add(split);
        save.setEnabled(false); execute.setEnabled(false); history.setEnabled(false); chart.setEnabled(false);
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer(); centered.setHorizontalAlignment(SwingConstants.CENTER);
        results.getColumnModel().getColumn(1).setCellRenderer(centered);
        results.getColumnModel().getColumn(2).setCellRenderer(centered);
        results.getColumnModel().getColumn(3).setCellRenderer(centered);
    }

    private JPanel toolbar() {
        JPanel badge = new JPanel(new GridLayout(2, 1)); badge.setOpaque(false);
        badge.setBorder(new MatteBorder(0, 1, 0, 0, LINE)); badge.add(insuranceType); badge.add(insuranceRate);
        JPanel panel = new JPanel(null) {
            @Override public void doLayout() {
                int w = Math.max(820, getWidth());
                menu.setBounds(4, 2, 46, 53);
                print.setBounds(52, 2, 42, 53);
                dateType.setBounds(100, 4, 152, 24);
                status.setBounds(100, 30, 152, 24);
                outpatient.setBounds(258, 4, 78, 22);
                inpatient.setBounds(258, 30, 78, 22);
                reload.setBounds(340, 2, 58, 53);
                search.setBounds(400, 2, 62, 53);
                waiting.setBounds(464, 2, 92, 53);

                int badgeW = 64;
                int bannerX = 562;
                int bannerW = Math.max(180, w - bannerX - badgeW - 8);
                patient.setBounds(bannerX, 3, bannerW, 18);
                insurance.setBounds(bannerX, 21, bannerW, 17);
                address.setBounds(bannerX, 38, bannerW, 17);
                badge.setBounds(w - badgeW, 1, badgeW, 55);
            }
        };
        panel.setOpaque(true); panel.setBackground(new Color(249, 249, 249));
        panel.setBorder(new MatteBorder(0, 0, 1, 0, LINE));
        panel.setPreferredSize(new Dimension(0, 58));
        for (JComboBox<String> combo : java.util.List.of(dateType, status)) combo.setFont(UngDungPhongChu.SMALL);
        for (JCheckBox check : java.util.List.of(outpatient, inpatient)) {
            check.setFont(UngDungPhongChu.SMALL); check.setOpaque(false);
            check.setMargin(new Insets(0, 0, 0, 0)); check.setIconTextGap(3);
        }
        insuranceType.setFont(UngDungPhongChu.LAY_MAU_BANNER); insuranceType.setForeground(Color.RED);
        insuranceType.setHorizontalAlignment(SwingConstants.CENTER);
        insuranceRate.setFont(UngDungPhongChu.LAY_MAU_BANNER); insuranceRate.setForeground(BLUE);
        insuranceRate.setHorizontalAlignment(SwingConstants.CENTER);
        for (Component c : java.util.List.of(menu, print, dateType, status, outpatient, inpatient,
                reload, search, waiting, patient, insurance, address, badge)) {
            panel.add(c);
        }
        return panel;
    }

    private JPanel searchStrip() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setBackground(new Color(255, 255, 216));
        panel.setBorder(new MatteBorder(0, 0, 1, 0, LINE));
        keyword.setFont(UngDungPhongChu.XET_NGHIEM_BOLD); keyword.setOpaque(false); keyword.setBorder(new EmptyBorder(0, 5, 0, 0));
        keyword.putClientProperty("JTextField.placeholderText", "Barcode, tên bệnh nhân");
        clear.setPreferredSize(new Dimension(37, 34)); clear.setBorder(BorderFactory.createLineBorder(new Color(190,190,190))); panel.add(keyword); panel.add(clear, BorderLayout.EAST); return panel;
    }

    private JPanel metadata() {
        LayMauBoCuc panel = new LayMauBoCuc(1198, 167);
        panel.setBorder(new MatteBorder(0, 0, 1, 0, LINE));
        panel.place(label("Mã phiếu chỉ định :"), 7, 2, 178, 31); panel.place(orderCode, 185, 2, 222, 29);
        panel.place(label("Mã phiếu thực hiện :"), 424, 2, 185, 31); panel.place(executionCode, 609, 2, 178, 29);
        panel.place(label("Barcode :"), 804, 2, 96, 31);
        barcode.setFont(UngDungPhongChu.LAY_MAU_BARCODE); barcode.setForeground(new Color(199, 0, 0));
        barcode.setHorizontalAlignment(SwingConstants.CENTER); panel.place(barcode, 900, 2, 193, 29);
        JLabel[] values = {diagnosis, ordered, collected, performed};
        String[] captions = {"Chẩn đoán :", "Thông tin chỉ định :", "Thông tin lấy mẫu :", "Thông tin thực hiện :"};
        for (int i = 0; i < 4; i++) {
            panel.place(label(captions[i]), 7, 34 + 32 * i, 178, 31);
            panel.place(values[i], 185, 34 + 32 * i, 1004, 29);
        }
        return panel;
    }

    private JPanel controls() {
        LayMauBoCuc panel = new LayMauBoCuc(1198, 114); panel.setOpaque(true); panel.setBackground(new Color(249, 249, 249));
        JPanel entry = new JPanel(new BorderLayout(6, 0)); entry.setBackground(new Color(245, 250, 253));
        entry.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), new EmptyBorder(0, 8, 0, 8)));
        JLabel lblConclusion = new JLabel("Kết luận:");
        lblConclusion.setFont(UngDungPhongChu.XET_NGHIEM);
        lblConclusion.setForeground(new Color(105, 112, 118));
        conclusion.setFont(UngDungPhongChu.XET_NGHIEM);
        conclusion.setOpaque(false);
        conclusion.setBorder(new EmptyBorder(0, 2, 0, 2));
        conclusion.setEditable(false);
        feedback.addPropertyChangeListener("text", e -> {
            String msg = String.valueOf(e.getNewValue());
            panel.setToolTipText(msg);
            conclusion.setToolTipText(msg);
        });
        entry.add(lblConclusion, BorderLayout.WEST);
        entry.add(conclusion, BorderLayout.CENTER);
        panel.place(entry, 0, 0, 888, 45);
        panel.place(expand, 889, 0, 184, 45);
        JPanel topDivider = new JPanel(); topDivider.setBackground(LINE); panel.place(topDivider, 1073, 4, 1, 37);
        panel.place(save, 1075, 0, 122, 45);
        JSeparator separator = new JSeparator(); panel.place(separator, 0, 45, 1198, 1);
        panel.place(history, 10, 55, 122, 48); panel.place(chart, 142, 55, 133, 48);
        panel.place(runFormula, 695, 54, 178, 49);
        panel.place(cancelExecute, 878, 54, 162, 49);
        panel.place(approveResult, 1044, 54, 152, 49);
        panel.place(unapproveResult, 882, 54, 158, 49);
        panel.place(releaseResult, 1044, 54, 152, 49);
        panel.place(printResultFooter, 1030, 54, 166, 49);
        JPanel divider = new JPanel(); divider.setBackground(LINE); panel.place(divider, 1042, 55, 1, 48);
        panel.place(execute, 1062, 54, 135, 49);
        runFormula.setVisible(false);
        cancelExecute.setVisible(false);
        approveResult.setVisible(false);
        unapproveResult.setVisible(false);
        releaseResult.setVisible(false);
        printResultFooter.setVisible(false);
        return panel;
    }

    public void setWorkflowStage(TrangThaiChiDinhDichVu st) {
        boolean isExecuting = st != null && st.isExecuting();
        boolean isApproved = st != null && st.isApproved();
        boolean isReleased = st != null && st.isResultReleased();
        this.executingMode = isExecuting;
        conclusion.setEditable(isExecuting);
        execute.setVisible(!isExecuting && !isApproved && !isReleased);
        runFormula.setVisible(isExecuting);
        cancelExecute.setVisible(isExecuting);
        approveResult.setVisible(isExecuting);
        unapproveResult.setText(isReleased ? "Hủy Trả KQ" : "Hủy Duyệt");
        unapproveResult.setVisible(isApproved || isReleased);
        releaseResult.setVisible(isApproved);
        printResultFooter.setVisible(isReleased);
        controls.revalidate();
        controls.repaint();
        results.repaint();
    }

    public void setWorkflowStage(String status) {
        setWorkflowStage(status == null || status.isBlank() ? null : TrangThaiChiDinhDichVu.fromCode(status));
    }

    public void setExecutingMode(boolean executing) {
        setWorkflowStage(executing ? TrangThaiChiDinhDichVu.DANG_THUC_HIEN : null);
    }

    public boolean isExecutingMode() { return executingMode; }

    public void resize(JPanel host) {
        scale = Math.max(0.45, Math.min(0.72, (host.getWidth() / 1586.0) * 0.68));
        toolbar.setPreferredSize(new Dimension(0, 58)); metadata.setPreferredSize(new Dimension(0, 114));
        searchStrip.setPreferredSize(new Dimension(0, 26)); queueFooter.setPreferredSize(new Dimension(0, 26));
        controls.setPreferredSize(new Dimension(0, 74)); clear.setPreferredSize(new Dimension(26, 24));
        queue.setRowHeight(40); results.setRowHeight(25);
        if (results.getRowCount() > 0) results.setRowHeight(0, 23);
        for (JTable table : java.util.List.of(queue, results)) table.getTableHeader().setPreferredSize(new Dimension(0, 25));
        int[] left = {22, 64, 152, 38}, right = {265, 45, 110, 78, 155, 195, 145, 95};
        widths(queue, left); widths(results, right);
        queueScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 14));
        resultScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 14));
        resizeIcons(host); split.setDividerLocation(Math.max(265, Math.min(310, (int) Math.round(host.getWidth() * 0.215))));
    }

    public double scale() { return scale; }
    private void widths(JTable table, int[] widths) {
        for (int i = 0; i < widths.length; i++) {
            TableColumn column = table.getColumnModel().getColumn(i); column.setMinWidth(0); column.setMaxWidth(Integer.MAX_VALUE);
            column.setPreferredWidth(widths[i]); column.setWidth(widths[i]);
        }
    }
    private void resizeIcons(Container parent) {
        for (Component c : parent.getComponents()) {
            if (c instanceof AbstractButton button && button.getIcon() instanceof LayMauBieuTuong icon)
                button.setIcon(new LayMauBieuTuong(icon.kind(), button == clear ? 16 : button == execute ? 20 : 22));
            if (c instanceof Container container) resizeIcons(container);
        }
    }
    private static JScrollPane scroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table); scroll.setBorder(null); scroll.getViewport().setBackground(Color.WHITE);
        scroll.setColumnHeaderView(table.getTableHeader()); scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); XetNghiemThanhCuon.install(scroll); return scroll;
    }
    private static void configureTable(JTable table) {
        table.setFont(UngDungPhongChu.XET_NGHIEM); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setSelectionBackground(SELECT); table.setSelectionForeground(Color.WHITE);
        table.setIntercellSpacing(new Dimension(1, 1)); table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(UngDungPhongChu.XET_NGHIEM_HEADER);
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int col) {
                super.getTableCellRendererComponent(t, v, false, false, r, col); setFont(t.getTableHeader().getFont()); setBackground(Color.WHITE);
                setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 1, new Color(225, 225, 225)), new EmptyBorder(0, t.getColumnCount() == 8 && col == 0 ? 40 : 8, 0, 2)));
                return this;
            }
        });
    }
    private static JLabel label(String text) { JLabel label = new JLabel(text); label.setFont(UngDungPhongChu.XET_NGHIEM); return label; }
    private static JLabel banner(boolean bold) {
        JLabel label = new JLabel(" ", SwingConstants.RIGHT); label.setFont(bold ? UngDungPhongChu.LAY_MAU_BANNER : UngDungPhongChu.XET_NGHIEM);
        label.setForeground(new Color(145, 0, 0)); return label;
    }
    private static JLabel value() {
        JLabel label = label(" "); label.setForeground(BLUE);
        label.setBorder(new AbstractBorder() {
            @Override public Insets getBorderInsets(Component c) { return new Insets(0, 0, 1, 0); }
            @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                g.setColor(new Color(120, 120, 120)); for (int i=x; i<x+w; i+=2) g.drawLine(i,y+h-1,i,y+h-1);
            }
        }); return label;
    }
    private static JButton flat(LayMauBieuTuong.Kind icon, String text) {
        JButton button = new JButton(text, new LayMauBieuTuong(icon, 38)); button.setFont(UngDungPhongChu.XET_NGHIEM);
        button.setFocusPainted(false); button.setContentAreaFilled(false); button.setBorder(new EmptyBorder(0, 2, 0, 2)); return button;
    }
    private static JButton tool(LayMauBieuTuong.Kind icon, String text) {
        JButton button = flat(icon, text); button.setHorizontalTextPosition(SwingConstants.CENTER); button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setIconTextGap(3); return button;
    }
}
