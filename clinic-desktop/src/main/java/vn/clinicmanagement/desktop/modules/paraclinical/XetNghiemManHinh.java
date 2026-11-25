package vn.clinicmanagement.desktop.modules.paraclinical;

import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.modules.paraclinical.model.PhieuXetNghiem;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.desktop.modules.paraclinical.components.LayMauBoCuc;
import vn.clinicmanagement.desktop.modules.paraclinical.components.LayMauBieuTuong;
import static vn.clinicmanagement.desktop.modules.paraclinical.components.LayMauBieuTuong.Kind.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.sql.*;
import java.text.MessageFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.List;

/**
 * Màn hình Xét Nghiệm — Lấy Mẫu và Trả Kết Quả (chuẩn EHC HIS).
 * Kết nối trực tiếp SQL Server (ChiDinhDichVu, DanhMucDichVu, LuotKham, BenhNhan, BaoHiem, ChanDoan)
 * và đấu nối CanLamSangKetQuaNhapLieuHopThoai để nhập/trả kết quả xét nghiệm.
 */
public final class XetNghiemManHinh extends CoSoManHinhManHinh {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd-MM-yy");
    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Color BLUE_LINK = new Color(0, 0, 210);
    private static final Color WINE_RED = new Color(145, 18, 18);
    private static final Color SELECT_BLUE = new Color(0, 157, 250);
    private static final Color TOOLBAR_BG = new Color(249, 249, 249);
    private static final Color BORDER_GRAY = new Color(170, 170, 170);

    private final CanLamSangGhepNoiGoc.Services services;
    private final List<PhieuXetNghiem> allOrders = new ArrayList<>();
    private final List<PhieuXetNghiem> filteredOrders = new ArrayList<>();
    private LocalDate fromDate = LocalDate.now(BUSINESS_ZONE);
    private LocalDate toDate = LocalDate.now(BUSINESS_ZONE);
    private String selectedRoomFilter = null;
    private String quickKeyword = "";
    private PhieuXetNghiem selectedOrder;
    private boolean loading;

    // Toolbar controls
    private JComboBox<String> cboDateType;
    private JComboBox<String> cboStatusFilter;
    private JCheckBox chkOutpatient;
    private JCheckBox chkInpatient;
    private JLabel lblPatientLine1;
    private JLabel lblPatientLine2;
    private JLabel lblPatientLine3;
    private JLabel lblBhytBadge;
    private JLabel lblBhytRate;

    // Left queue controls
    private JLabel lblDateRange;
    private JPanel quickSearchPanel;
    private JTextField txtQuickSearch;
    private DefaultTableModel queueModel;
    private JTable queueTable;
    private JLabel lblRoomFilter;

    // Right detail controls
    private final JLabel lblOrderCode = dottedValue(BLUE_LINK);
    private final JLabel lblBarcode = dottedValue(new Color(205, 18, 18));
    private final JLabel lblDiagnosis = dottedValue(BLUE_LINK);
    private final JLabel lblOrderedAt = dottedValue(BLUE_LINK);
    private final JLabel lblOrderedBy = dottedValue(BLUE_LINK);
    private final JLabel lblOrderPlace = dottedValue(BLUE_LINK);
    private final JLabel lblCollectStart = dottedValue(BLUE_LINK);
    private final JLabel lblCollector = dottedValue(BLUE_LINK);
    private final JLabel lblCollectPlace = dottedValue(BLUE_LINK);
    private final JLabel lblCollectEnd = dottedValue(BLUE_LINK);
    private final JLabel lblResultAt = dottedValue(BLUE_LINK);
    private final JLabel lblResultBy = dottedValue(BLUE_LINK);

    private JLabel lblGroupHeader;
    private DefaultTableModel detailModel;
    private JTable detailTable;
    private JLabel lblBottomStatus;
    private JButton btnCollectOrResult;
    private JButton btnPrintOrder;

    public XetNghiemManHinh() {
        this(GraphicsEnvironment.isHeadless() || System.getProperty("surefire.test.class.path") != null
                ? CanLamSangGhepNoiGoc.createDefault() : CanLamSangGhepNoiGoc.createProduction());
    }

    public XetNghiemManHinh(CanLamSangGhepNoiGoc.Services services) {
        super("specimen", "Lấy Mẫu Bệnh Phẩm", "");
        this.services = services != null ? services : CanLamSangGhepNoiGoc.createDefault();
        buildUI();
        bindShortcuts();
    }

    private JSplitPane workspace;
    private JPanel toolbar;
    private JPanel metadata;
    private JPanel dateStrip;
    private JPanel queueFooter;
    private JPanel actionFooter;
    private JPanel tableHeading;
    private JScrollPane queueScroll;
    private double uiScale = 1;

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        toolbar = buildToolbar();
        add(toolbar, BorderLayout.NORTH);
        workspace = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftQueue(), buildRightDetail());
        workspace.setDividerSize(3);
        workspace.setContinuousLayout(true);
        workspace.setBorder(null);
        workspace.setResizeWeight(0.21);
        workspace.getLeftComponent().setMinimumSize(new Dimension(240, 0));
        workspace.getRightComponent().setMinimumSize(new Dimension(480, 0));
        add(workspace, BorderLayout.CENTER);
    }

    @Override public void doLayout() {
        super.doLayout();
        if (workspace != null) {
            int target = Math.max(265, Math.min(310, (int) Math.round(getWidth() * 0.215)));
            workspace.setDividerLocation(target);
        }
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(null) {
            @Override public void doLayout() {
                int w = getWidth();
                int badgeW = 62;
                int bannerX = 592;
                int bannerW = Math.max(120, w - bannerX - badgeW - 8);
                if (lblPatientLine1 != null) lblPatientLine1.setBounds(bannerX, 3, bannerW, 18);
                if (lblPatientLine2 != null) lblPatientLine2.setBounds(bannerX, 21, bannerW, 17);
                if (lblPatientLine3 != null) lblPatientLine3.setBounds(bannerX, 38, bannerW, 17);
                if (getComponentCount() > 0) {
                    Component last = getComponent(getComponentCount() - 1);
                    last.setBounds(w - badgeW, 0, badgeW, getHeight() - 1);
                }
            }
        };
        bar.setOpaque(true);
        bar.setBackground(TOOLBAR_BG);
        bar.setPreferredSize(new Dimension(980, 58));
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_GRAY));

        JButton menuButton = iconTopBtn(MENU, "Menu");
        JPopupMenu menu = new JPopupMenu();
        JMenuItem refresh = new JMenuItem("Làm mới danh sách");
        refresh.addActionListener(e -> refreshData());
        JMenuItem today = new JMenuItem("Xem phiếu hôm nay");
        today.addActionListener(e -> setDateRange(LocalDate.now(BUSINESS_ZONE), LocalDate.now(BUSINESS_ZONE)));
        JMenuItem week = new JMenuItem("Xem 7 ngày gần đây");
        week.addActionListener(e -> setDateRange(LocalDate.now(BUSINESS_ZONE).minusDays(6), LocalDate.now(BUSINESS_ZONE)));
        JMenuItem print = new JMenuItem("In Phiếu Chỉ Định (F3)");
        print.addActionListener(e -> printOrderSlip());
        menu.add(refresh); menu.add(today); menu.add(week); menu.addSeparator(); menu.add(print);
        menuButton.addActionListener(e -> menu.show(menuButton, 0, menuButton.getHeight()));
        menuButton.setBounds(4, 3, 48, 52);
        bar.add(menuButton);

        JButton printButton = iconTopBtn(PRINT, "In");
        printButton.addActionListener(e -> printOrderSlip());
        printButton.setBounds(54, 3, 44, 52);
        bar.add(printButton);

        JLabel lblFind = textLabel("Tìm theo");
        lblFind.setFont(UngDungPhongChu.SMALL);
        lblFind.setBounds(106, 5, 62, 22);
        bar.add(lblFind);

        JLabel lblState = textLabel("Trạng thái");
        lblState.setFont(UngDungPhongChu.SMALL);
        lblState.setBounds(106, 30, 62, 22);
        bar.add(lblState);

        cboDateType = new JComboBox<>(new String[]{"Ngày y lệnh", "Ngày lấy mẫu", "Ngày trả kết quả"});
        cboDateType.setFont(UngDungPhongChu.SMALL);
        cboDateType.addActionListener(e -> refreshData());
        cboDateType.setBounds(170, 5, 148, 23);
        bar.add(cboDateType);

        cboStatusFilter = new JComboBox<>(new String[]{"(Tất cả phiếu)", "Đang chờ", "Đã lấy mẫu", "Đã có kết quả", "Đã hủy"});
        cboStatusFilter.setFont(UngDungPhongChu.SMALL);
        cboStatusFilter.addActionListener(e -> applyFilters());
        cboStatusFilter.setBounds(170, 30, 148, 23);
        bar.add(cboStatusFilter);

        chkOutpatient = new JCheckBox("Ngoại trú", true);
        chkInpatient = new JCheckBox("Nội trú", true);
        for (JCheckBox check : List.of(chkOutpatient, chkInpatient)) {
            check.setOpaque(false);
            check.setFont(UngDungPhongChu.SMALL);
            check.setMargin(new Insets(0, 0, 0, 0));
            check.setIconTextGap(3);
            check.addActionListener(e -> applyFilters());
        }
        chkOutpatient.setBounds(324, 5, 80, 22);
        chkInpatient.setBounds(324, 30, 80, 22);
        bar.add(chkOutpatient);
        bar.add(chkInpatient);

        JButton reload = iconTopBtn(REFRESH, "Làm Mới");
        reload.addActionListener(e -> refreshData());
        reload.setBounds(406, 3, 58, 52);
        bar.add(reload);

        JButton search = iconTopBtn(SEARCH, "Tìm Kiếm");
        search.addActionListener(e -> toggleQuickSearch());
        search.setBounds(466, 3, 60, 52);
        bar.add(search);

        JButton screen = iconTopBtn(LIST, "Màn Hình");
        screen.addActionListener(e -> {
            if (selectedOrder != null) openSelectedLineResultDialog();
            else JOptionPane.showMessageDialog(this, "Vui lòng chọn một phiếu xét nghiệm.");
        });
        screen.setBounds(528, 3, 62, 52);
        bar.add(screen);

        lblPatientLine1 = bannerLabel(UngDungPhongChu.LAY_MAU_BANNER);
        lblPatientLine2 = bannerLabel(UngDungPhongChu.SMALL);
        lblPatientLine3 = bannerLabel(UngDungPhongChu.SMALL);
        bar.add(lblPatientLine1);
        bar.add(lblPatientLine2);
        bar.add(lblPatientLine3);

        lblBhytBadge = bannerLabel(UngDungPhongChu.BODY_BOLD);
        lblBhytBadge.setForeground(Color.RED);
        lblBhytBadge.setHorizontalAlignment(SwingConstants.CENTER);
        lblBhytRate = bannerLabel(UngDungPhongChu.BODY_BOLD);
        lblBhytRate.setForeground(BLUE_LINK);
        lblBhytRate.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel badge = new JPanel(new GridLayout(2, 1));
        badge.setOpaque(false);
        badge.setBorder(new MatteBorder(0, 1, 0, 0, BORDER_GRAY));
        badge.add(lblBhytBadge);
        badge.add(lblBhytRate);
        bar.add(badge);
        return bar;
    }

    private static JLabel textLabel(String text) {
        JLabel label = new JLabel(text); label.setFont(UngDungPhongChu.LAY_MAU); return label;
    }

    private static JLabel bannerLabel(Font font) {
        JLabel label = new JLabel(" ", SwingConstants.RIGHT);
        label.setFont(font); label.setForeground(WINE_RED); return label;
    }

    private JPanel buildLeftQueue() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        dateStrip = new JPanel(new BorderLayout());
        dateStrip.setBackground(new Color(255, 255, 207));
        dateStrip.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_GRAY));
        dateStrip.setPreferredSize(new Dimension(265, 25));
        lblDateRange = textLabel("");
        lblDateRange.setFont(UngDungPhongChu.SMALL);
        lblDateRange.setBorder(new EmptyBorder(0, 6, 0, 0));
        lblDateRange.setForeground(new Color(90, 83, 54));
        lblDateRange.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblDateRange.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showDateRangeMenu(dateStrip); }
        });
        JLabel search = new JLabel(new LayMauBieuTuong(SEARCH, 16));
        search.setBorder(new EmptyBorder(0, 6, 0, 6));
        search.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        search.setToolTipText("Tìm nhanh theo barcode, tên hoặc mã bệnh nhân");
        search.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggleQuickSearch(); }
        });
        dateStrip.add(lblDateRange); dateStrip.add(search, BorderLayout.EAST);
        quickSearchPanel = new JPanel(new BorderLayout(4, 0));
        quickSearchPanel.setPreferredSize(new Dimension(265, 25));
        txtQuickSearch = new JTextField(); txtQuickSearch.setFont(UngDungPhongChu.SMALL);
        txtQuickSearch.putClientProperty("JTextField.placeholderText", "Barcode, tên BN, mã BN...");
        txtQuickSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onQuickSearchChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onQuickSearchChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onQuickSearchChanged(); }
        });
        JButton clear = new JButton("×");
        clear.setFont(UngDungPhongChu.SMALL);
        clear.setMargin(new Insets(0, 4, 0, 4));
        clear.addActionListener(e -> { txtQuickSearch.setText(""); quickSearchPanel.setVisible(false); revalidate(); });
        quickSearchPanel.add(txtQuickSearch); quickSearchPanel.add(clear, BorderLayout.EAST);
        quickSearchPanel.setVisible(false);
        JPanel north = new JPanel(new BorderLayout());
        north.add(dateStrip, BorderLayout.NORTH); north.add(quickSearchPanel, BorderLayout.SOUTH);
        panel.add(north, BorderLayout.NORTH);
        queueModel = new DefaultTableModel(new String[]{"", "Barcode", "Tên Bệnh Nhân", "STT"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        queueTable = new JTable(queueModel);
        configureTable(queueTable);
        queueTable.setRowHeight(40);
        queueTable.getTableHeader().setPreferredSize(new Dimension(265, 24));
        int[] qWidths = {22, 62, 152, 34};
        int[] qMax = {26, 72, Integer.MAX_VALUE, 42};
        for (int i = 0; i < qWidths.length; i++) {
            var col = queueTable.getColumnModel().getColumn(i);
            col.setPreferredWidth(qWidths[i]);
            col.setMaxWidth(qMax[i]);
        }
        queueTable.getColumnModel().getColumn(0).setCellRenderer(new StatusCellRenderer());
        queueTable.getColumnModel().getColumn(1).setCellRenderer(new BarcodeCellRenderer());
        queueTable.getColumnModel().getColumn(2).setCellRenderer(new PatientQueueCellRenderer());
        DefaultTableCellRenderer number = new DefaultTableCellRenderer();
        number.setHorizontalAlignment(SwingConstants.CENTER);
        queueTable.getColumnModel().getColumn(3).setCellRenderer(number);
        queueTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = queueTable.getSelectedRow();
                selectOrder(row >= 0 && row < filteredOrders.size() ? filteredOrders.get(row) : null);
            }
        });
        JScrollPane scroll = new JScrollPane(queueTable); queueScroll = scroll;
        scroll.setBorder(null); scroll.setColumnHeaderView(queueTable.getTableHeader());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(14, 0));
        panel.add(scroll);
        queueFooter = new JPanel(new BorderLayout()); queueFooter.setBackground(Color.WHITE);
        queueFooter.setPreferredSize(new Dimension(265, 24));
        queueFooter.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_GRAY));
        lblRoomFilter = textLabel("(Tất cả phòng lấy mẫu bệnh phẩm)");
        lblRoomFilter.setFont(UngDungPhongChu.SMALL);
        lblRoomFilter.setForeground(Color.RED); lblRoomFilter.setBorder(new EmptyBorder(0, 6, 0, 0));
        lblRoomFilter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblRoomFilter.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showRoomFilterMenu(queueFooter); }
        });
        queueFooter.add(lblRoomFilter); panel.add(queueFooter, BorderLayout.SOUTH);
        updateDateRangeLabel(); return panel;
    }

    private static void configureTable(JTable table) {
        table.setFont(UngDungPhongChu.SMALL);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(SELECT_BLUE); table.setSelectionForeground(Color.WHITE);
        table.setShowHorizontalLines(true); table.setShowVerticalLines(true);
        table.setGridColor(new Color(235, 238, 242));
        table.setIntercellSpacing(new Dimension(1, 1));
        table.getTableHeader().setFont(UngDungPhongChu.SMALL_BOLD);
        table.getTableHeader().setBackground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer header = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable owner, Object value, boolean selected, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(owner, value, false, false, row, col);
                setFont(owner.getTableHeader().getFont()); setBackground(Color.WHITE);
                setForeground(Color.BLACK); setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 1, new Color(220, 220, 220)), new EmptyBorder(0, 6, 0, 2)));
                return this;
            }
        };
        table.getTableHeader().setDefaultRenderer(header);
    }

    private JPanel buildRightDetail() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setBackground(Color.WHITE);
        LayMauBoCuc meta = new LayMauBoCuc(1164, 185);
        metadata = meta;
        meta.setPreferredSize(new Dimension(700, 114));
        meta.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_GRAY));
        lblBarcode.setFont(UngDungPhongChu.LAY_MAU_BARCODE);
        JLabel[] values = {lblOrderCode, lblBarcode, lblDiagnosis, lblOrderedAt, lblOrderedBy, lblOrderPlace,
                lblCollectStart, lblCollector, lblCollectPlace, lblCollectEnd, lblResultAt, lblResultBy};
        String[] captions = {"Số phiếu chỉ định :", "Barcode :", "Chẩn đoán :", "Ngày chỉ định :", "Người chỉ định :", "Nơi chỉ định :",
                "Bắt đầu lấy mẫu :", "Người lấy mẫu :", "Nơi lấy mẫu :", "Kết thúc lấy mẫu :", "Trả kết quả :", "Người trả kết quả :"};
        int[] xs = {8, 392, 756}, widths = {371, 350, 376}, labelWidths = {152, 144, 124};
        for (int i = 0; i < values.length; i++) {
            int row = i < 9 ? i / 3 : i == 9 ? 3 : 4;
            int col = i < 9 ? i % 3 : i == 11 ? 1 : 0;
            LayMauBoCuc cell = new LayMauBoCuc(widths[col], 34);
            int labelWidth = i == 11 ? 164 : labelWidths[col];
            JLabel caption = textLabel(captions[i]); caption.setFont(UngDungPhongChu.LAY_MAU_CAPTION);
            cell.place(caption, 0, 0, labelWidth, 34);
            cell.place(values[i], labelWidth, 0, widths[col] - labelWidth, 30);
            meta.place(cell, xs[col], 4 + row * 34, widths[col], 34);
        }
        panel.add(meta, BorderLayout.NORTH);
        detailModel = new DefaultTableModel(new String[]{"Tên Xét Nghiệm", "Số Lượng", "Loại Bệnh Phẩm", "Ghi Chú (Chỉ Định)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        detailTable = new JTable(detailModel); configureTable(detailTable);
        detailTable.setFont(UngDungPhongChu.BODY);
        detailTable.setRowHeight(25);
        int[] dWidths = {430, 80, 190, 230};
        for (int i = 0; i < dWidths.length; i++) {
            detailTable.getColumnModel().getColumn(i).setPreferredWidth(dWidths[i]);
        }
        detailTable.setShowHorizontalLines(false); detailTable.setShowVerticalLines(false);
        DefaultTableCellRenderer quantity = new DefaultTableCellRenderer(); quantity.setHorizontalAlignment(SwingConstants.CENTER);
        detailTable.getColumnModel().getColumn(1).setCellRenderer(quantity);
        detailTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && selectedOrder != null) handlePrimaryAction();
            }
        });
        JScrollPane scroll = new JScrollPane(detailTable);
        scroll.setBorder(new EmptyBorder(0, 20, 0, 0));
        scroll.setColumnHeaderView(null); scroll.getViewport().setBackground(Color.WHITE);
        LayMauBoCuc heading = new LayMauBoCuc(1164, 75); tableHeading = heading;
        heading.setPreferredSize(new Dimension(700, 46));
        heading.place(detailTable.getTableHeader(), 20, 0, 1144, 42);
        lblGroupHeader = textLabel("XÉT NGHIỆM, Trạng thái : —"); lblGroupHeader.setForeground(BLUE_LINK);
        lblGroupHeader.setFont(UngDungPhongChu.BODY);
        lblGroupHeader.setBorder(new EmptyBorder(0, 12, 0, 20));
        heading.place(lblGroupHeader, 0, 43, 1133, 30);
        JLabel chevron = textLabel("⌃"); chevron.setForeground(BLUE_LINK);
        heading.place(chevron, 1138, 43, 24, 30);
        JPanel center = new JPanel(new BorderLayout()); center.setBackground(Color.WHITE);
        center.setBorder(new MatteBorder(0, 0, 1, 1, BORDER_GRAY));
        center.add(heading, BorderLayout.NORTH); center.add(scroll); panel.add(center);

        JPanel footer = new JPanel(new BorderLayout()); actionFooter = footer;
        footer.setOpaque(true); footer.setBackground(TOOLBAR_BG);
        footer.setPreferredSize(new Dimension(700, 48));
        footer.setBorder(new EmptyBorder(5, 10, 5, 10));
        lblBottomStatus = textLabel("Chọn phiếu xét nghiệm để lấy mẫu hoặc trả kết quả.");
        lblBottomStatus.setFont(UngDungPhongChu.SMALL);
        lblBottomStatus.setVisible(false);
        lblBottomStatus.addPropertyChangeListener("text", e -> {
            String message = String.valueOf(e.getNewValue());
            lblBottomStatus.setVisible(message.startsWith("Không tải") || message.startsWith("Không thể") || message.startsWith("Đang "));
            footer.setToolTipText(message);
            if (btnCollectOrResult != null) btnCollectOrResult.setToolTipText(String.valueOf(e.getNewValue()));
        });
        footer.add(lblBottomStatus, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        btnCollectOrResult = new JButton("Lấy Mẫu", new LayMauBieuTuong(COLLECT, 22));
        btnCollectOrResult.setFont(UngDungPhongChu.BODY);
        btnCollectOrResult.setPreferredSize(new Dimension(112, 36));
        btnCollectOrResult.setBackground(new Color(255, 231, 157));
        btnCollectOrResult.setBorder(BorderFactory.createLineBorder(new Color(241, 190, 74)));
        btnCollectOrResult.setFocusPainted(false); btnCollectOrResult.setEnabled(false);
        btnCollectOrResult.addActionListener(e -> handlePrimaryAction());
        btnPrintOrder = new JButton("<html><center>Phiếu Chỉ Định<br>(F3)</center></html>", new LayMauBieuTuong(PRINT, 22));
        btnPrintOrder.setFont(UngDungPhongChu.SMALL); btnPrintOrder.setContentAreaFilled(false);
        btnPrintOrder.setPreferredSize(new Dimension(132, 36));
        btnPrintOrder.setBorder(new EmptyBorder(0, 0, 0, 0)); btnPrintOrder.setEnabled(false);
        btnPrintOrder.addActionListener(e -> printOrderSlip());
        actions.add(btnCollectOrResult);
        actions.add(btnPrintOrder);
        footer.add(actions, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH); return panel;
    }

    private void bindShortcuts() {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F3"), "printOrder");
        getActionMap().put("printOrder", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { printOrderSlip(); }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F5"), "refreshLab");
        getActionMap().put("refreshLab", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { refreshData(); }
        });
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {
        if (loading) return;
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null || !auth.isValid()) {
            allOrders.clear();
            applyFilters();
            return;
        }
        loading = true;
        int facilityId = auth.facilityId();
        LocalDate start = fromDate;
        LocalDate end = toDate;
        int dateTypeIdx = cboDateType != null ? cboDateType.getSelectedIndex() : 0;
        Integer preferredId = selectedOrder != null ? selectedOrder.orderId() : null;
        lblBottomStatus.setText("Đang tải danh sách phiếu xét nghiệm...");

        new SwingWorker<List<PhieuXetNghiem>, Void>() {
            @Override protected List<PhieuXetNghiem> doInBackground() {
                return XetNghiemDuLieuNguon.load(facilityId, start, end, dateTypeIdx);
            }

            @Override protected void done() {
                loading = false;
                try {
                    allOrders.clear();
                    allOrders.addAll(get());
                    applyFilters(preferredId);
                } catch (Exception ex) {
                    lblBottomStatus.setText("Không tải được danh sách xét nghiệm: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void applyFilters() {
        applyFilters(selectedOrder != null ? selectedOrder.orderId() : null);
    }

    private void applyFilters(Integer preferredOrderId) {
        filteredOrders.clear();
        int statusIdx = cboStatusFilter != null ? cboStatusFilter.getSelectedIndex() : 0;
        boolean showOut = chkOutpatient == null || chkOutpatient.isSelected();
        boolean showIn = chkInpatient == null || chkInpatient.isSelected();

        for (PhieuXetNghiem item : allOrders) {
            if (item.isInpatient() && !showIn) continue;
            if (!item.isInpatient() && !showOut) continue;
            TrangThaiChiDinhDichVu st = item.statusEnum();
            if (statusIdx == 1 && !st.isWaitingForCollection()) continue;
            if (statusIdx == 2 && !(st.isSampleCollectedWaitingExec() || st.isExecuting() || st.isApproved())) continue;
            if (statusIdx == 3 && !st.isResultReleased()) continue;
            if (statusIdx == 4 && !st.isCancelled()) continue;
            if (selectedRoomFilter != null && !selectedRoomFilter.equalsIgnoreCase(item.roomName())) continue;
            if (!quickKeyword.isEmpty()) {
                String hay = (item.barcode() + " " + item.orderCode() + " " + item.patientName()
                        + " " + item.patientCode() + " " + item.serviceName()).toLowerCase(Locale.ROOT);
                if (!hay.contains(quickKeyword)) continue;
            }
            filteredOrders.add(item);
        }

        queueModel.setRowCount(0);
        int selectRow = -1;
        for (int i = 0; i < filteredOrders.size(); i++) {
            PhieuXetNghiem it = filteredOrders.get(i);
            queueModel.addRow(new Object[]{it, it, it, it.queueNumber() > 0 ? it.queueNumber() : (i + 1)});
            if (preferredOrderId != null && preferredOrderId == it.orderId()) {
                selectRow = i;
            }
        }
        updateDateRangeLabel();
        if (cboStatusFilter != null) {
            String base = "(Tất cả phiếu) - " + allOrders.size();
            DefaultComboBoxModel<String> m = (DefaultComboBoxModel<String>) cboStatusFilter.getModel();
            if (!base.equals(m.getElementAt(0))) {
                int prev = cboStatusFilter.getSelectedIndex();
                cboStatusFilter.setModel(new DefaultComboBoxModel<>(new String[]{
                        base,
                        TrangThaiChiDinhDichVu.CHO_THUC_HIEN.getDescription(),
                        TrangThaiChiDinhDichVu.DA_LAY_MAU.getDescription(),
                        TrangThaiChiDinhDichVu.DA_CO_KET_QUA.getDescription(),
                        TrangThaiChiDinhDichVu.DA_HUY.getDescription()
                }));
                cboStatusFilter.setSelectedIndex(Math.max(0, prev));
            }
        }

        if (selectRow >= 0) {
            queueTable.setRowSelectionInterval(selectRow, selectRow);
        } else if (!filteredOrders.isEmpty()) {
            queueTable.setRowSelectionInterval(0, 0);
        } else {
            selectOrder(null);
        }
    }

    private void selectOrder(PhieuXetNghiem item) {
        this.selectedOrder = item;
        if (item == null) {
            lblPatientLine1.setText(" ");
            lblPatientLine2.setText(" ");
            lblPatientLine3.setText(" ");
            lblBhytBadge.setText(" ");
            lblBhytRate.setText(" ");
            for (JLabel l : List.of(lblOrderCode, lblBarcode, lblDiagnosis, lblOrderedAt, lblOrderedBy,
                    lblOrderPlace, lblCollectStart, lblCollector, lblCollectPlace, lblCollectEnd, lblResultAt, lblResultBy)) {
                l.setText(" ");
            }
            lblGroupHeader.setText("XÉT NGHIỆM, Trạng thái : —");
            detailModel.setRowCount(0);
            btnCollectOrResult.setEnabled(false);
            btnPrintOrder.setEnabled(false);
            lblBottomStatus.setText("Không có phiếu xét nghiệm nào trong khoảng ngày đã chọn.");
            return;
        }

        int year = item.birthDate() != null ? item.birthDate().getYear() : 0;
        int age = year > 0 ? Math.max(0, LocalDate.now(BUSINESS_ZONE).getYear() - year) : 0;
        String genderStr = "M".equalsIgnoreCase(item.gender()) ? "Nam" : ("F".equalsIgnoreCase(item.gender()) ? "Nữ" : item.gender());
        lblPatientLine1.setText(item.patientName().toUpperCase(Locale.ROOT)
                + " | " + item.patientCode()
                + (year > 0 ? " | " + year + " (" + age + " tuổi)" : "")
                + (genderStr.isBlank() ? "" : " | " + genderStr));
        String bhytInfo = item.bhytNumber().isBlank() ? "Viện phí" : item.bhytNumber();
        lblPatientLine2.setText("Mã VP: " + item.visitId() + " | " + bhytInfo);
        lblPatientLine3.setText(item.address().isBlank() ? " " : item.address());
        lblBhytBadge.setText(item.isBhyt() ? "BHYT" : "VP");
        lblBhytRate.setText(item.isBhyt() ? (item.bhytRate() > 0 ? item.bhytRate() + "%" : "80%") : "");

        lblOrderCode.setText(item.orderCode());
        lblBarcode.setText(item.barcode());
        lblDiagnosis.setText(blankDash(item.diagnosis()));
        lblOrderedAt.setText(item.orderedAt() != null ? DT_FMT.format(item.orderedAt()) : " ");
        lblOrderedBy.setText(blankDash(item.orderedByName()));
        lblOrderPlace.setText(blankDash(item.departmentName()));
        lblCollectStart.setText(item.collectedAt() != null ? DT_FMT.format(item.collectedAt()) : " ");
        String collector = item.collectorName() != null && !item.collectorName().isBlank()
                ? item.collectorName() : item.performerName();
        lblCollector.setText(item.collectedAt() != null ? blankDash(collector) : " ");
        String place = item.collectPlace() != null && !item.collectPlace().isBlank()
                ? item.collectPlace() : item.roomName();
        lblCollectPlace.setText(blankDash(place));
        LocalDateTime endAt = item.collectEndAt() != null ? item.collectEndAt() : item.resultAt();
        lblCollectEnd.setText(endAt != null ? DT_FMT.format(endAt) : " ");
        lblResultAt.setText(item.resultAt() != null ? DT_FMT.format(item.resultAt()) : " ");
        lblResultBy.setText(item.resultAt() != null ? blankDash(item.performerName()) : " ");

        String statusLabel = item.statusEnum().getDescription();
        String grp = item.testGroupName() != null && !item.testGroupName().isBlank()
                ? " " + item.testGroupName().toUpperCase(Locale.ROOT) : "";
        lblGroupHeader.setText("XÉT NGHIỆM" + grp + ", Trạng thái : " + statusLabel);

        detailModel.setRowCount(0);
        if (item.tests() != null && !item.tests().isEmpty()) {
            for (var line : item.tests()) {
                detailModel.addRow(new Object[]{
                        line.serviceName(),
                        line.quantity().stripTrailingZeros().toPlainString(),
                        line.specimenType().isBlank() ? "Máu hóa sinh, miễn dịch" : line.specimenType(),
                        line.note().isBlank() ? line.resultText() : line.note()
                });
            }
        } else {
            detailModel.addRow(new Object[]{
                    item.serviceName(),
                    item.quantity().stripTrailingZeros().toPlainString(),
                    item.specimenType().isBlank() ? "Máu / Bệnh phẩm" : item.specimenType(),
                    item.conclusionText().isBlank() ? item.resultText() : item.conclusionText()
            });
        }
        if (detailModel.getRowCount() > 0) {
            detailTable.setRowSelectionInterval(0, 0);
        }

        btnPrintOrder.setEnabled(true);
        boolean canCollect = item.statusEnum().isWaitingForCollection();
        btnCollectOrResult.setText("Lấy Mẫu");
        btnCollectOrResult.setBackground(new Color(255, 231, 157));
        btnCollectOrResult.setEnabled(canCollect);
        lblBottomStatus.setText("Phiếu " + item.orderCode() + " | Barcode: " + item.barcode() + " | Trạng thái: " + statusLabel);
    }

    private void handlePrimaryAction() {
        if (selectedOrder == null) return;
        if (selectedOrder.statusEnum().isWaitingForCollection()) {
            markSampleCollected(selectedOrder);
        } else if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(this,
                    "Phiếu " + selectedOrder.orderCode() + " (Barcode " + selectedOrder.barcode() + ") đã được lấy mẫu.\n"
                            + "Vui lòng chuyển sang tab [Xét Nghiệm] để nhập và trả kết quả xét nghiệm.",
                    "Lấy Mẫu Bệnh Phẩm", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void markSampleCollected(PhieuXetNghiem item) {
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null || !auth.isValid()) return;
        if (!item.paid()) {
            int ans = JOptionPane.showConfirmDialog(this,
                    "Lượt khám của bệnh nhân " + item.patientName() + " còn khoản viện phí chưa thanh toán hết tại Thu Ngân.\n"
                            + "Bạn vẫn muốn tiến hành lấy mẫu cho phiếu " + item.orderCode() + " (" + item.barcode() + ")?",
                    "Cảnh Báo Viện Phí", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ans != JOptionPane.YES_OPTION) return;
        } else {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Xác nhận bắt đầu lấy mẫu cho phiếu " + item.orderCode() + " - Barcode " + item.barcode()
                            + " (" + item.patientName() + ")?",
                    "Lấy Mẫu Bệnh Phẩm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        btnCollectOrResult.setEnabled(false);
        lblBottomStatus.setText("Đang ghi nhận lấy mẫu...");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                String sql = "UPDATE dbo.ChiDinhDichVu "
                        + "SET trangThai = ?, "
                        + "    thoiGianThucHien = SYSDATETIME(), "
                        + "    thoiGianBatDauLayMau = COALESCE(thoiGianBatDauLayMau, SYSDATETIME()), "
                        + "    thoiGianKetThucLayMau = DATEADD(MINUTE, 2, SYSDATETIME()), "
                        + "    nguoiLayMau = ?, "
                        + "    bacSiThucHien = ?, "
                        + "    updatedAt = SYSDATETIME(), updatedBy = ?, version = version + 1 "
                        + "WHERE maLuotKham = ? AND (soBarcode = ? OR id = ?) AND isDeleted = 0 "
                        + "AND trangThai IN (?, ?)";
                try (Connection c = CoSoDuLieuKetNoi.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, TrangThaiChiDinhDichVu.DA_LAY_MAU.name());
                    ps.setInt(2, auth.employeeId());
                    ps.setInt(3, auth.employeeId());
                    ps.setInt(4, auth.employeeId());
                    ps.setInt(5, item.visitId());
                    ps.setString(6, item.barcode());
                    ps.setInt(7, item.orderId());
                    ps.setString(8, TrangThaiChiDinhDichVu.CHO_THUC_HIEN.name());
                    ps.setString(9, TrangThaiChiDinhDichVu.DA_CHI_DINH.name());
                    return ps.executeUpdate() >= 1;
                }
            }

            @Override protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        lblBottomStatus.setText("Đã lấy mẫu thành công cho phiếu " + item.orderCode() + " (Đã chuyển sang màn hình Xét Nghiệm).");
                        refreshData();
                    } else {
                        btnCollectOrResult.setEnabled(true);
                        JOptionPane.showMessageDialog(XetNghiemManHinh.this,
                                "Phiếu không còn ở trạng thái Đang chờ.", "Lấy Mẫu", JOptionPane.WARNING_MESSAGE);
                        refreshData();
                    }
                } catch (Exception ex) {
                    btnCollectOrResult.setEnabled(true);
                    JOptionPane.showMessageDialog(XetNghiemManHinh.this,
                            "Không thể cập nhật trạng thái lấy mẫu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openSelectedLineResultDialog() {
        if (selectedOrder == null) return;
        int row = detailTable != null ? detailTable.getSelectedRow() : -1;
        if (selectedOrder.tests() != null && row >= 0 && row < selectedOrder.tests().size()) {
            var line = selectedOrder.tests().get(row);
            openResultEntryDialog(line.orderId(), selectedOrder.patientName(), line.serviceName());
            return;
        }
        openResultEntryDialog(selectedOrder);
    }

    private void openResultEntryDialog(PhieuXetNghiem item) {
        if (item == null) return;
        openResultEntryDialog(item.orderId(), item.patientName(), item.serviceName());
    }

    private void openResultEntryDialog(int orderId, String patientName, String serviceName) {
        if (GraphicsEnvironment.isHeadless()) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        var dialog = new CanLamSangKetQuaNhapLieuHopThoai(
                owner instanceof Frame f ? f : null,
                orderId,
                patientName,
                serviceName,
                services,
                this::refreshData);
        dialog.setVisible(true);
    }

    private void printOrderSlip() {
        if (GraphicsEnvironment.isHeadless() || selectedOrder == null) return;
        try {
            String header = "PHIẾU CHỈ ĐỊNH XÉT NGHIỆM - " + selectedOrder.orderCode() + " | " + selectedOrder.patientName();
            String footer = "Barcode: " + selectedOrder.barcode() + " | " + selectedOrder.departmentName();
            detailTable.print(JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat("'" + header.replace("'", "''") + "'"),
                    new MessageFormat("'" + footer.replace("'", "''") + "'"),
                    true, null, true);
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Không thể in phiếu chỉ định: " + ex.getMessage(), "In Phiếu", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showDateRangeMenu(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        JMenuItem miToday = new JMenuItem("Hôm nay (" + today.format(SHORT_DATE) + ")");
        miToday.addActionListener(e -> setDateRange(today, today));
        menu.add(miToday);

        LocalDate yesterday = today.minusDays(1);
        JMenuItem miYesterday = new JMenuItem("Hôm qua (" + yesterday.format(SHORT_DATE) + ")");
        miYesterday.addActionListener(e -> setDateRange(yesterday, yesterday));
        menu.add(miYesterday);

        JMenuItem mi7 = new JMenuItem("7 ngày gần đây");
        mi7.addActionListener(e -> setDateRange(today.minusDays(6), today));
        menu.add(mi7);

        JMenuItem mi30 = new JMenuItem("30 ngày gần đây");
        mi30.addActionListener(e -> setDateRange(today.minusDays(29), today));
        menu.add(mi30);

        menu.addSeparator();
        JMenuItem miCustom = new JMenuItem("Chọn khoảng ngày...");
        miCustom.addActionListener(e -> promptCustomDateRange());
        menu.add(miCustom);
        menu.show(invoker, 0, invoker.getHeight());
    }

    private void promptCustomDateRange() {
        if (GraphicsEnvironment.isHeadless()) return;
        JTextField txtFrom = new JTextField(fromDate.format(INPUT_DATE), 12);
        JTextField txtTo = new JTextField(toDate.format(INPUT_DATE), 12);
        JPanel p = new JPanel(new GridLayout(2, 2, 6, 6));
        p.add(new JLabel("Từ ngày (dd/MM/yyyy):")); p.add(txtFrom);
        p.add(new JLabel("Đến ngày (dd/MM/yyyy):")); p.add(txtTo);
        if (JOptionPane.showConfirmDialog(this, p, "Lọc Phiếu Xét Nghiệm Theo Ngày",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            setDateRange(LocalDate.parse(txtFrom.getText().trim(), INPUT_DATE),
                    LocalDate.parse(txtTo.getText().trim(), INPUT_DATE));
        } catch (DateTimeException ex) {
            JOptionPane.showMessageDialog(this, "Ngày không hợp lệ (nhập dd/MM/yyyy).", "Lọc Ngày", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void setDateRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate s = from != null ? from : today;
        LocalDate e = to != null ? to : s;
        if (s.isAfter(e)) { LocalDate tmp = s; s = e; e = tmp; }
        this.fromDate = s;
        this.toDate = e;
        updateDateRangeLabel();
        refreshData();
    }

    private void showRoomFilterMenu(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem all = new JMenuItem("(Tất cả phòng lấy mẫu bệnh phẩm)");
        all.addActionListener(e -> {
            selectedRoomFilter = null;
            lblRoomFilter.setText("(Tất cả phòng lấy mẫu bệnh phẩm)");
            applyFilters();
        });
        menu.add(all);
        Set<String> rooms = new TreeSet<>();
        for (PhieuXetNghiem it : allOrders) {
            if (!it.roomName().isBlank()) rooms.add(it.roomName());
        }
        if (!rooms.isEmpty()) menu.addSeparator();
        for (String r : rooms) {
            JMenuItem mi = new JMenuItem(r);
            mi.addActionListener(e -> {
                selectedRoomFilter = r;
                lblRoomFilter.setText(r);
                applyFilters();
            });
            menu.add(mi);
        }
        menu.show(invoker, 0, -menu.getPreferredSize().height);
    }

    private void toggleQuickSearch() {
        boolean show = !quickSearchPanel.isVisible();
        quickSearchPanel.setVisible(show);
        if (show) txtQuickSearch.requestFocusInWindow();
        else txtQuickSearch.setText("");
        revalidate();
    }

    private void onQuickSearchChanged() {
        quickKeyword = txtQuickSearch.getText() == null ? "" : txtQuickSearch.getText().trim().toLowerCase(Locale.ROOT);
        applyFilters();
    }

    private void updateDateRangeLabel() {
        long bh = filteredOrders.stream().filter(PhieuXetNghiem::isBhyt).count();
        long vp = filteredOrders.size() - bh;
        lblDateRange.setText(fromDate.format(SHORT_DATE) + " >> " + toDate.format(SHORT_DATE) + " - BH(" + bh + ") VP(" + vp + ")");
    }

    private static JButton iconTopBtn(LayMauBieuTuong.Kind kind, String text) {
        JButton button = new JButton(text, new LayMauBieuTuong(kind, 22));
        button.setFont(UngDungPhongChu.SMALL);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setIconTextGap(2); button.setFocusPainted(false);
        button.setContentAreaFilled(false); button.setBorder(new EmptyBorder(0, 0, 0, 0));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return button;
    }

    private static JLabel dottedValue(Color foreground) {
        JLabel label = new JLabel(" "); label.setFont(UngDungPhongChu.LAY_MAU);
        label.setForeground(foreground);
        // Only the baseline is painted, leaving the metadata cells visually open.
        label.setBorder(new javax.swing.border.AbstractBorder() {
            @Override public Insets getBorderInsets(Component c) { return new Insets(0, 0, 1, 0); }
            @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                g.setColor(new Color(145, 145, 145));
                for (int i = x; i < x + w; i += 2) g.drawLine(i, y + h - 1, i, y + h - 1);
            }
        });
        return label;
    }

    private static String blankDash(String s) {
        return s == null || s.isBlank() ? " " : s.trim();
    }

    private static final class StatusCellRenderer extends DefaultTableCellRenderer {
        private Color dotColor;
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, "", selected, false, row, col);
            setBackground(Color.WHITE);
            dotColor = selected ? null : new Color(143, 0, 177);
            if (!selected && value instanceof PhieuXetNghiem item) {
                dotColor = switch (item.statusEnum()) {
                    case DA_CO_KET_QUA -> new Color(0, 148, 50);
                    case DA_LAY_MAU, DANG_THUC_HIEN, CHO_KET_QUA, DA_DUYET_KET_QUA -> SELECT_BLUE;
                    case DA_HUY -> new Color(180, 45, 45);
                    default -> new Color(143, 0, 177);
                };
            }
            return this;
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (dotColor == null) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(dotColor); int d = Math.max(8, (int) (getWidth() * .72));
            g.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d); g.dispose();
        }
    }

    private static final class BarcodeCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value instanceof PhieuXetNghiem item ? item.barcode() : "", selected, false, row, col);
            setFont(table.getFont()); setHorizontalAlignment(SwingConstants.CENTER);
            setBackground(selected ? SELECT_BLUE : row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
            return this;
        }
    }

    private static final class PatientQueueCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, "", selected, false, row, col);
            if (value instanceof PhieuXetNghiem item) {
                String name = html(item.patientName().toUpperCase(Locale.ROOT));
                setText("<html><b>" + name + "</b><br>" + html(item.patientCode() + " - " + item.shortSubjectTag()) + "</html>");
            }
            setFont(table.getFont());
            setBackground(selected ? SELECT_BLUE : row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
            setBorder(new EmptyBorder(0, Math.max(3, table.getRowHeight() / 8), 0, 0)); return this;
        }
        private static String html(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    }
}
