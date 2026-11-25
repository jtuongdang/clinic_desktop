package vn.clinicmanagement.desktop.modules.inventory;

import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.GiaoDichTon;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.Kho;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.Lo;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBieuTuong;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Cửa sổ "Lịch Sử Nhập Xuất" mở từ menu chuột phải trên một dòng vật tư trong kho tủ trực.
 */
final class TuTrucLichSuNhapXuatHopThoai extends JDialog {
    private static final Color GRID = new Color(207, 210, 215);
    private static final Color SELECTED = new Color(0, 145, 255);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DecimalFormat NUM_FMT = createNumFormat();

    private static final String[] COLUMNS = {
            "ID", "Số Phiếu", "Thời Gian", "Loại", "Số Lượng",
            "Đơn Giá", "Số Lô", "HSD", "SĐK", "Người Thực Hiện", "Mã BN"
    };
    private static final int[] COL_WIDTHS = {60, 85, 130, 55, 75, 75, 75, 90, 75, 140, 85};

    record LichSuDong(
            String id, String ticketCode, LocalDateTime time, String direction,
            BigDecimal quantity, BigDecimal unitPrice, String batch, LocalDate expiry,
            String registrationNo, String performer, String patientCode,
            String groupType, String department
    ) {}

    final JComboBox<Kho> cabinets = new JComboBox<>();
    final JSpinner fromDate;
    final JSpinner untilDate;
    final JButton allBtn = new JButton("Tất Cả Thuốc, Vật Tư (0)");
    final JTree categories = new JTree(new DefaultMutableTreeNode("root"));
    final JTextField search = new JTextField() {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                g.setFont(getFont());
                g.setColor(new Color(102, 102, 81));
                g.drawString("Tìm kiếm (Ctrl+F)", 6,
                        (getHeight() - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent());
            }
        }
    };
    final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    final JTable table;
    private final JLabel summaryLabel = new JLabel();

    private final TuTrucCongKetNoi data;
    private final Lo targetItem;
    private final List<LichSuDong> allRows = new ArrayList<>();
    private List<LichSuDong> displayed = List.of();
    private String selectedGroup = "";
    private boolean changingTree;
    private int loadGen;

    TuTrucLichSuNhapXuatHopThoai(Window owner, Lo item, List<Kho> cabinetList, Kho selectedCabinet) {
        this(owner, null, item, cabinetList, selectedCabinet);
    }

    TuTrucLichSuNhapXuatHopThoai(Window owner, TuTrucCongKetNoi data, Lo item, List<Kho> cabinetList, Kho selectedCabinet) {
        super(owner, buildTitle(item), ModalityType.MODELESS);
        this.data = data;
        this.targetItem = item;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1180, 680);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        LocalDate now = LocalDate.now();
        fromDate = dateSpinner(now.withDayOfYear(1));
        untilDate = dateSpinner(now.withMonth(12).withDayOfMonth(31));

        add(buildToolbar(), BorderLayout.NORTH);

        JPanel left = buildLeftPanel(cabinetList, selectedCabinet);
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Color.WHITE);

        JPanel searchBar = new JPanel(new BorderLayout());
        searchBar.setPreferredSize(new Dimension(100, 28));
        search.setName("tuTrucLichSuTimNhanh");
        search.setFont(UngDungPhongChu.BODY);
        search.setBackground(new Color(255, 255, 193));
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(172, 172, 151)),
                new EmptyBorder(0, 6, 0, 4)));
        JButton find = new JButton(TuTrucThanhCongCu.scaled(NhaThuocBieuTuong.createSearchIcon(), 16));
        find.setPreferredSize(new Dimension(28, 26));
        find.addActionListener(e -> applyFilter());
        searchBar.add(search, BorderLayout.CENTER);
        searchBar.add(find, BorderLayout.EAST);
        right.add(searchBar, BorderLayout.NORTH);

        table = buildTable();
        JScrollPane scroll = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createLineBorder(GRID));
        scroll.getViewport().setBackground(Color.WHITE);
        right.add(scroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setBorder(null);
        split.setDividerSize(3);
        split.setContinuousLayout(true);
        split.setDividerLocation(230);
        add(split, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(245, 245, 245));
        statusBar.setBorder(new MatteBorder(1, 0, 0, 0, GRID));
        summaryLabel.setFont(UngDungPhongChu.SMALL);
        summaryLabel.setForeground(new Color(0, 51, 153));
        summaryLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        statusBar.add(summaryLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("control F"), "find");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("meta F"), "find");
        getRootPane().getActionMap().put("find", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                search.requestFocusInWindow();
                search.selectAll();
            }
        });

        applyFilter();
        loadHistory();
    }

    private void loadHistory() {
        if (data == null) return;
        var ctx = KhamBenhPhienNguCanh.current();
        Kho cab = (Kho) cabinets.getSelectedItem();
        if (ctx == null || cab == null) return;
        int gen = ++loadGen;
        LocalDate from = spinnerDate(fromDate);
        LocalDate until = spinnerDate(untilDate);
        TuTrucVatTuManHinh.async(
                () -> data.getStockMovement(ctx, cab.id(), from.atStartOfDay(), until.atTime(23, 59, 59)),
                report -> {
                    if (gen != loadGen || !isDisplayable()) return;
                    allRows.clear();
                    if (report != null && report.transactions() != null) {
                        int idx = 1;
                        for (GiaoDichTon tx : report.transactions()) {
                            if (targetItem != null && targetItem.code() != null
                                    && !targetItem.code().equals(tx.code())) {
                                continue;
                            }
                            boolean isIn = tx.delta() != null && tx.delta().signum() >= 0;
                            BigDecimal qty = tx.delta() == null ? BigDecimal.ZERO : tx.delta().abs();
                            allRows.add(new LichSuDong(
                                    String.valueOf(idx++),
                                    Objects.toString(tx.documentCode(), ""),
                                    tx.occurredAt(),
                                    isIn ? "Nhập" : "Xuất",
                                    qty,
                                    null,
                                    Objects.toString(tx.batch(), ""),
                                    null,
                                    "",
                                    "",
                                    "",
                                    Objects.toString(tx.transactionType(), ""),
                                    cab.name()));
                        }
                    }
                    applyFilter();
                },
                err -> {});
    }

    private static String buildTitle(Lo item) {
        if (item == null) return "Lịch Sử Nhập Xuất";
        String code = item.code() == null ? "" : item.code();
        String name = item.name() == null ? "" : item.name();
        return "Lịch Sử Nhập Xuất - " + code + " - " + name;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(248, 248, 248));
        bar.setBorder(new MatteBorder(0, 0, 1, 0, GRID));
        bar.setPreferredSize(new Dimension(100, 54));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        left.setOpaque(false);

        JButton menuBtn = new JButton(TuTrucThanhCongCu.scaled(NhaThuocBieuTuong.createMenuIcon(), 22));
        menuBtn.setPreferredSize(new Dimension(38, 44));
        menuBtn.setFocusPainted(false);
        menuBtn.setContentAreaFilled(false);
        menuBtn.setBorder(new EmptyBorder(2, 4, 2, 4));
        menuBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPopupMenu sysMenu = new JPopupMenu();
        JMenuItem miRefresh = new JMenuItem("Làm mới");
        miRefresh.setFont(UngDungPhongChu.BODY);
        miRefresh.addActionListener(e -> applyFilter());
        JMenuItem miExport = new JMenuItem("Xuất CSV");
        miExport.setFont(UngDungPhongChu.BODY);
        miExport.addActionListener(e -> exportCsv());
        JMenuItem miClose = new JMenuItem("Đóng");
        miClose.setFont(UngDungPhongChu.BODY);
        miClose.addActionListener(e -> dispose());
        sysMenu.add(miRefresh);
        sysMenu.add(miExport);
        sysMenu.addSeparator();
        sysMenu.add(miClose);
        menuBtn.addActionListener(e -> sysMenu.show(menuBtn, 0, menuBtn.getHeight()));

        JButton refreshBtn = new JButton("Làm Mới", refreshIcon(20));
        refreshBtn.setFont(UngDungPhongChu.SMALL);
        refreshBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        refreshBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        refreshBtn.setIconTextGap(2);
        refreshBtn.setPreferredSize(new Dimension(62, 44));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setContentAreaFilled(false);
        refreshBtn.setBorder(new EmptyBorder(2, 4, 2, 4));
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> applyFilter());

        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(2, 36));

        JButton reportBtn = new JButton("Báo Cáo ▾",
                TuTrucThanhCongCu.scaled(NhaThuocBieuTuong.createReportIcon(), 20));
        reportBtn.setFont(UngDungPhongChu.BODY);
        reportBtn.setPreferredSize(new Dimension(106, 40));
        reportBtn.setFocusPainted(false);
        reportBtn.setContentAreaFilled(false);
        reportBtn.setBorder(new EmptyBorder(2, 4, 2, 4));
        reportBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPopupMenu reportMenu = new JPopupMenu();
        JMenuItem miPrint = new JMenuItem("In lịch sử nhập xuất");
        miPrint.setFont(UngDungPhongChu.BODY);
        miPrint.addActionListener(e -> {
            try { table.print(JTable.PrintMode.FIT_WIDTH); }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không in được danh sách.", "In", JOptionPane.WARNING_MESSAGE);
            }
        });
        JMenuItem miExportRep = new JMenuItem("Xuất danh sách CSV");
        miExportRep.setFont(UngDungPhongChu.BODY);
        miExportRep.addActionListener(e -> exportCsv());
        reportMenu.add(miPrint);
        reportMenu.add(miExportRep);
        reportBtn.addActionListener(e -> reportMenu.show(reportBtn, 0, reportBtn.getHeight()));

        JLabel fromLbl = new JLabel("Từ ngày");
        fromLbl.setFont(UngDungPhongChu.BODY);
        fromDate.setPreferredSize(new Dimension(112, 26));
        fromDate.addChangeListener(e -> applyFilter());

        JLabel untilLbl = new JLabel("Đến ngày");
        untilLbl.setFont(UngDungPhongChu.BODY);
        untilDate.setPreferredSize(new Dimension(112, 26));
        untilDate.addChangeListener(e -> applyFilter());

        left.add(menuBtn);
        left.add(refreshBtn);
        left.add(sep);
        left.add(reportBtn);
        left.add(fromLbl);
        left.add(fromDate);
        left.add(untilLbl);
        left.add(untilDate);

        bar.add(left, BorderLayout.WEST);

        JLabel title = new JLabel("LỊCH SỬ NHẬP XUẤT");
        title.setFont(UngDungPhongChu.HEADER);
        title.setForeground(new Color(175, 0, 0));
        title.setBorder(new EmptyBorder(0, 8, 0, 16));
        bar.add(title, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildLeftPanel(List<Kho> cabinetList, Kho selectedCabinet) {
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Color.WHITE);
        left.setMinimumSize(new Dimension(180, 0));

        if (cabinetList != null) {
            cabinetList.forEach(cabinets::addItem);
            if (selectedCabinet != null) cabinets.setSelectedItem(selectedCabinet);
        }
        cabinets.setFont(UngDungPhongChu.TU_TRUC_TEN_TU);
        cabinets.setForeground(Color.RED);
        cabinets.setBackground(Color.WHITE);
        cabinets.setPreferredSize(new Dimension(230, 28));
        cabinets.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value == null) setText("(Chọn tủ trực)");
                setForeground(selected ? Color.WHITE : Color.RED);
                return this;
            }
        });
        left.add(cabinets, BorderLayout.NORTH);

        JPanel treeArea = new JPanel(new BorderLayout());
        treeArea.setBackground(Color.WHITE);

        allBtn.setFont(UngDungPhongChu.TU_TRUC_BOLD);
        allBtn.setHorizontalAlignment(SwingConstants.LEFT);
        allBtn.setBorder(new EmptyBorder(2, 8, 2, 4));
        allBtn.setPreferredSize(new Dimension(230, 24));
        allBtn.setBackground(SELECTED);
        allBtn.setForeground(Color.WHITE);
        allBtn.setOpaque(true);
        allBtn.setFocusPainted(false);
        allBtn.addActionListener(e -> {
            selectedGroup = "";
            categories.clearSelection();
            allBtn.setBackground(SELECTED);
            allBtn.setForeground(Color.WHITE);
            applyFilter();
        });
        treeArea.add(allBtn, BorderLayout.NORTH);

        categories.setRootVisible(false);
        categories.setShowsRootHandles(true);
        categories.setRowHeight(22);
        categories.setFont(UngDungPhongChu.TU_TRUC_BOLD);
        categories.setBackground(Color.WHITE);
        categories.putClientProperty("JTree.lineStyle", "Angled");
        if (categories.getUI() instanceof javax.swing.plaf.basic.BasicTreeUI ui) {
            ui.setExpandedIcon(NhaThuocBieuTuong.createMinusBoxIcon());
            ui.setLeftChildIndent(10);
            ui.setRightChildIndent(16);
        }
        categories.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree t, Object value, boolean selected,
                                                          boolean expanded, boolean leaf, int row, boolean focus) {
                super.getTreeCellRendererComponent(t, value, selected, expanded, leaf, row, focus);
                setLeafIcon(null);
                setClosedIcon(null);
                setOpenIcon(null);
                setFont(UngDungPhongChu.TU_TRUC_BOLD);
                setTextSelectionColor(Color.WHITE);
                setBackgroundSelectionColor(SELECTED);
                return this;
            }
        });
        categories.addTreeSelectionListener(e -> {
            if (changingTree) return;
            var node = (DefaultMutableTreeNode) categories.getLastSelectedPathComponent();
            if (node != null && node.getUserObject() instanceof GroupNode gn) {
                selectedGroup = gn.key();
                allBtn.setBackground(Color.WHITE);
                allBtn.setForeground(Color.BLACK);
                applyFilter();
            }
        });

        JScrollPane treeScroll = new JScrollPane(categories);
        treeScroll.setBorder(null);
        treeArea.add(treeScroll, BorderLayout.CENTER);
        left.add(treeArea, BorderLayout.CENTER);
        return left;
    }

    private JTable buildTable() {
        JTable t = new JTable(model) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int y = getRowCount() * getRowHeight();
                g.setColor(GRID);
                for (int line = y; line < getHeight(); line += getRowHeight()) {
                    g.drawLine(0, line, getWidth(), line);
                }
                int x = 0;
                for (int c = 0; c < getColumnCount(); c++) {
                    x += getColumnModel().getColumn(c).getWidth();
                    g.drawLine(x - 1, y, x - 1, getHeight());
                }
            }
        };
        t.setRowHeight(24);
        t.setFont(UngDungPhongChu.BODY);
        t.setGridColor(GRID);
        t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setFillsViewportHeight(true);
        t.setBackground(Color.WHITE);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        t.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        t.getTableHeader().setPreferredSize(new Dimension(100, 28));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setBackground(new Color(250, 250, 250));

        for (int i = 0; i < COL_WIDTHS.length; i++) {
            var col = t.getColumnModel().getColumn(i);
            col.setPreferredWidth(COL_WIDTHS[i]);
            col.setWidth(COL_WIDTHS[i]);
        }

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                super.getTableCellRendererComponent(tbl, value, selected, false, row, column);
                setFont(UngDungPhongChu.BODY);
                setBorder(new EmptyBorder(0, 5, 0, 5));
                if (column == 0 || column == 1 || column == 2 || column == 3 || column == 6 || column == 7 || column == 8 || column == 10) {
                    setHorizontalAlignment(CENTER);
                } else if (column == 4 || column == 5) {
                    setHorizontalAlignment(RIGHT);
                } else {
                    setHorizontalAlignment(LEFT);
                }
                if (selected) {
                    setBackground(SELECTED);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                }
                return this;
            }
        });
        return t;
    }

    private void applyFilter() {
        String q = TuTrucVatTuManHinh.normalize(search.getText());
        LocalDate from = spinnerDate(fromDate);
        LocalDate until = spinnerDate(untilDate);

        displayed = allRows.stream()
                .filter(r -> r.time() == null || (!r.time().toLocalDate().isBefore(from) && !r.time().toLocalDate().isAfter(until)))
                .filter(r -> selectedGroup.isEmpty() || selectedGroup.equals(r.groupType()) || selectedGroup.equals(r.department()))
                .filter(r -> q.isEmpty() || TuTrucVatTuManHinh.normalize(
                        r.id() + " " + r.ticketCode() + " " + r.direction() + " "
                                + r.batch() + " " + r.registrationNo() + " " + r.performer() + " " + r.patientCode())
                        .contains(q))
                .toList();

        model.setRowCount(0);
        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        for (LichSuDong r : displayed) {
            model.addRow(new Object[]{
                    r.id(),
                    r.ticketCode(),
                    r.time() == null ? "" : TIME_FMT.format(r.time()),
                    r.direction(),
                    r.quantity() == null ? "" : NUM_FMT.format(r.quantity()),
                    r.unitPrice() == null ? "" : NUM_FMT.format(r.unitPrice()),
                    r.batch(),
                    r.expiry() == null ? "" : DATE_FMT.format(r.expiry()),
                    r.registrationNo(),
                    r.performer(),
                    r.patientCode()
            });
            if ("Nhập".equals(r.direction()) && r.quantity() != null) totalIn = totalIn.add(r.quantity());
            if ("Xuất".equals(r.direction()) && r.quantity() != null) totalOut = totalOut.add(r.quantity());
        }
        summaryLabel.setText("<html>Tổng số lượng nhập: <b>" + NUM_FMT.format(totalIn)
                + "</b> &nbsp;&nbsp;|&nbsp;&nbsp; Tổng số lượng xuất: <b>" + NUM_FMT.format(totalOut) + "</b></html>");
        updateTree();
    }

    private record GroupNode(String key, int count) {
        @Override public String toString() { return key + " (" + count + ")"; }
    }

    private void updateTree() {
        int totalEntries = allRows.size();
        allBtn.setText("Tất Cả Thuốc, Vật Tư (" + totalEntries + ")");
        changingTree = true;
        var root = new DefaultMutableTreeNode("root");
        Map<String, Integer> groups = new LinkedHashMap<>();
        for (LichSuDong r : allRows) {
            if (r.groupType() != null && !r.groupType().isBlank()) groups.merge(r.groupType(), 1, Integer::sum);
            if (r.department() != null && !r.department().isBlank()) groups.merge(r.department(), 1, Integer::sum);
        }
        for (var e : groups.entrySet()) {
            root.add(new DefaultMutableTreeNode(new GroupNode(e.getKey(), e.getValue())));
        }
        categories.setModel(new DefaultTreeModel(root));
        changingTree = false;
    }

    private void exportCsv() {
        JFileChooser picker = new JFileChooser();
        picker.setSelectedFile(new File("lich-su-nhap-xuat.csv"));
        if (picker.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = picker.getSelectedFile();
        try (var writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            for (int r = -1; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) writer.write(',');
                    String cell = String.valueOf(r < 0 ? model.getColumnName(c) : model.getValueAt(r, c));
                    if (cell.matches("^[=+@\\-].*")) cell = "'" + cell;
                    writer.write('"' + cell.replace("\"", "\"\"") + '"');
                }
                writer.write("\r\n");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không ghi được tệp CSV.", "Xuất CSV", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JSpinner dateSpinner(LocalDate date) {
        Date value = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner s = new JSpinner(new SpinnerDateModel(value, null, null, Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        s.setFont(UngDungPhongChu.BODY);
        return s;
    }

    private static LocalDate spinnerDate(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static DecimalFormat createNumFormat() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        return new DecimalFormat("#,##0.####", sym);
    }

    private static Icon refreshIcon(int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);
                g.setColor(new Color(0, 114, 198));
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int m = 3;
                int d = size - m * 2;
                g.drawArc(m, m, d, d, 30, 290);
                int ax = size - m - 2;
                int ay = m + 3;
                g.fillPolygon(new int[]{ax - 4, ax + 2, ax + 1}, new int[]{ay, ay - 3, ay + 4}, 3);
                g.dispose();
            }
        };
    }
}
