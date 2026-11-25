package vn.clinicmanagement.desktop.modules.inventory;

import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.Kho;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.Lo;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBieuTuong;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Cửa sổ "Danh Sách Vật Tư Trong Kho" mở từ menu Danh Sách của Tủ Trực Vật Tư.
 * Hỗ trợ lọc theo kho tủ trực, tìm kiếm nhanh và menu chuột phải trực tiếp trên từng dòng vật tư.
 */
final class TuTrucVatTuTrongKhoHopThoai extends JDialog {
    private static final Color GRID = new Color(207, 210, 215);
    private static final Color SELECTED = new Color(0, 145, 255);
    private static final Color GUTTER = new Color(226, 226, 226);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat QTY_FMT = createQtyFormat();

    private static final String[] COLUMNS = {
            "Mã Vật Tư", "Tên Vật Tư", "Đơn Vị", "Số Lô", "Tồn Kho",
            "Chờ Xuất", "Đóng Băng", "Hạn Dùng", "Số Đăng Ký", "Đầu Kỳ (Khởi Tạo)"
    };
    private static final int[] COL_WIDTHS = {95, 250, 65, 90, 80, 75, 80, 90, 95, 120};

    private final TuTrucCongKetNoi data;
    private final String session;

    final JComboBox<Kho> cabinets = new JComboBox<>();
    final JCheckBox showEmptyLots = new JCheckBox("Hiển thị lô đã hết");
    final JCheckBox warnZeroStock = new JCheckBox("Hiển thị cảnh báo tồn kho = 0");
    final JCheckBox showSummary = new JCheckBox("Hiển thị tổng hợp");
    final JCheckBox showSummaryByBatch = new JCheckBox("Hiển thị tổng hợp khi cùng giá, số lô, HSD, SĐK");
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
    private final JLabel statusLabel = new JLabel("(Chưa kiểm kê)");
    private final JPopupMenu rowContextMenu;

    private List<Lo> rawStock = List.of();
    private List<Lo> displayed = List.of();
    private boolean changingCabinets;
    private int cabinetGen, stockGen;

    TuTrucVatTuTrongKhoHopThoai(Window owner, TuTrucCongKetNoi data, int initialCabinetId) {
        super(owner, "Danh Sách Vật Tư Trong Kho", ModalityType.MODELESS);
        this.data = Objects.requireNonNull(data);
        var ctx = KhamBenhPhienNguCanh.current();
        this.session = ctx == null ? "" : ctx.sessionId().toString();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1240, 720);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // 1. Toolbar
        add(buildToolbar(), BorderLayout.NORTH);

        // 2. Left sidebar + Right table split
        JPanel left = buildLeftPanel();
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Color.WHITE);

        JPanel searchBar = new JPanel(new BorderLayout());
        searchBar.setPreferredSize(new Dimension(100, 28));
        search.setName("tuTrucKhoTimNhanh");
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
        rowContextMenu = buildRowContextMenu();
        wireTableInteractions();

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
        split.setDividerLocation(240);
        add(split, BorderLayout.CENTER);

        // 3. Bottom status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(245, 245, 245));
        statusBar.setBorder(new MatteBorder(1, 0, 0, 0, GRID));
        statusLabel.setFont(UngDungPhongChu.SMALL);
        statusLabel.setForeground(new Color(0, 51, 153));
        statusLabel.setBorder(new EmptyBorder(3, 8, 3, 8));
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);

        // Listeners
        cabinets.addActionListener(e -> {
            if (!changingCabinets) loadStock();
        });
        ItemListener filterListener = e -> applyFilter();
        showEmptyLots.addItemListener(filterListener);
        warnZeroStock.addItemListener(filterListener);
        showSummary.addItemListener(filterListener);
        showSummaryByBatch.addItemListener(filterListener);
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

        loadCabinets(initialCabinetId);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(248, 248, 248));
        bar.setBorder(new MatteBorder(0, 0, 1, 0, GRID));
        bar.setPreferredSize(new Dimension(100, 56));

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        leftTools.setOpaque(false);

        JButton menuBtn = new JButton(TuTrucThanhCongCu.scaled(NhaThuocBieuTuong.createMenuIcon(), 24));
        menuBtn.setPreferredSize(new Dimension(42, 48));
        menuBtn.setFocusPainted(false);
        menuBtn.setContentAreaFilled(false);
        menuBtn.setBorder(new EmptyBorder(2, 4, 2, 4));
        menuBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPopupMenu sysMenu = new JPopupMenu();
        JMenuItem miRefresh = new JMenuItem("Làm mới");
        miRefresh.setFont(UngDungPhongChu.BODY);
        miRefresh.addActionListener(e -> loadStock());
        JMenuItem miExport = new JMenuItem("Xuất danh sách CSV");
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

        JButton refreshBtn = new JButton("Làm Mới", refreshIcon(22));
        refreshBtn.setFont(UngDungPhongChu.SMALL);
        refreshBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        refreshBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        refreshBtn.setIconTextGap(2);
        refreshBtn.setPreferredSize(new Dimension(64, 48));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setContentAreaFilled(false);
        refreshBtn.setBorder(new EmptyBorder(2, 4, 2, 4));
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> loadStock());

        leftTools.add(menuBtn);
        leftTools.add(refreshBtn);

        // Checkboxes grid (2x2)
        JPanel checks = new JPanel(new GridLayout(2, 2, 12, 0));
        checks.setOpaque(false);
        checks.setBorder(new EmptyBorder(4, 12, 4, 8));
        for (JCheckBox cb : List.of(showEmptyLots, showSummary, warnZeroStock, showSummaryByBatch)) {
            cb.setFont(UngDungPhongChu.SMALL);
            cb.setOpaque(false);
            cb.setFocusPainted(false);
        }
        checks.add(showEmptyLots);
        checks.add(showSummary);
        checks.add(warnZeroStock);
        checks.add(showSummaryByBatch);
        leftTools.add(checks);

        bar.add(leftTools, BorderLayout.WEST);

        JLabel title = new JLabel("DANH SÁCH VẬT TƯ TRONG KHO");
        title.setFont(UngDungPhongChu.HEADER);
        title.setForeground(new Color(175, 0, 0));
        title.setBorder(new EmptyBorder(0, 8, 0, 16));
        bar.add(title, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Color.WHITE);
        left.setMinimumSize(new Dimension(190, 0));

        cabinets.setFont(UngDungPhongChu.TU_TRUC_TEN_TU);
        cabinets.setForeground(Color.RED);
        cabinets.setBackground(Color.WHITE);
        cabinets.setPreferredSize(new Dimension(240, 28));
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
        allBtn.setPreferredSize(new Dimension(240, 24));
        allBtn.setBackground(SELECTED);
        allBtn.setForeground(Color.WHITE);
        allBtn.setOpaque(true);
        allBtn.setFocusPainted(false);
        allBtn.addActionListener(e -> {
            categories.clearSelection();
            allBtn.setBackground(SELECTED);
            allBtn.setForeground(Color.WHITE);
            applyFilter();
        });
        treeArea.add(allBtn, BorderLayout.NORTH);

        categories.setRootVisible(false);
        categories.setShowsRootHandles(true);
        categories.setRowHeight(22);
        categories.setFont(UngDungPhongChu.TU_TRUC);
        categories.setBackground(Color.WHITE);
        categories.putClientProperty("JTree.lineStyle", "Angled");
        if (categories.getUI() instanceof javax.swing.plaf.basic.BasicTreeUI ui) {
            ui.setExpandedIcon(NhaThuocBieuTuong.createMinusBoxIcon());
            ui.setLeftChildIndent(10);
            ui.setRightChildIndent(18);
        }
        categories.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree t, Object value, boolean selected,
                                                          boolean expanded, boolean leaf, int row, boolean focus) {
                super.getTreeCellRendererComponent(t, value, selected, expanded, leaf, row, focus);
                setLeafIcon(null);
                setClosedIcon(null);
                setOpenIcon(null);
                setFont(leaf ? UngDungPhongChu.TU_TRUC : UngDungPhongChu.TU_TRUC_BOLD);
                setTextSelectionColor(Color.WHITE);
                setBackgroundSelectionColor(SELECTED);
                return this;
            }
        });
        categories.addTreeSelectionListener(e -> {
            if (categories.getSelectionCount() > 0) {
                allBtn.setBackground(Color.WHITE);
                allBtn.setForeground(Color.BLACK);
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
                if (column == 2 || column == 3 || column == 7 || column == 8) {
                    setHorizontalAlignment(CENTER);
                } else if (column == 4 || column == 5 || column == 6 || column == 9) {
                    setHorizontalAlignment(RIGHT);
                } else {
                    setHorizontalAlignment(LEFT);
                }

                if (selected) {
                    setBackground(SELECTED);
                    setForeground(Color.WHITE);
                } else {
                    Lo item = row >= 0 && row < displayed.size() ? displayed.get(row) : null;
                    boolean zeroWarn = warnZeroStock.isSelected() && item != null
                            && item.quantity() != null && item.quantity().signum() <= 0;
                    if (zeroWarn) {
                        setBackground(new Color(255, 232, 232));
                        setForeground(new Color(175, 0, 0));
                    } else if (item != null && item.expiry() != null && item.expiry().isBefore(LocalDate.now())) {
                        setBackground(new Color(255, 240, 240));
                        setForeground(new Color(165, 0, 0));
                    } else {
                        setBackground(Color.WHITE);
                        setForeground(Color.BLACK);
                    }
                }
                return this;
            }
        });
        return t;
    }

    private JPopupMenu buildRowContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Color.WHITE);
        menu.setBorder(new LineBorder(new Color(160, 160, 155)));

        addMenuGroup(menu, "Vật tư");
        addMenuItem(menu, "Lịch Sử Nhập Xuất", null, false, () -> onRowAction("Lịch Sử Nhập Xuất"));
        addMenuItem(menu, "Lịch Sử Thay Đổi Đầu Kỳ, Tồn Kho, Duyệt Xuất", null, false,
                () -> onRowAction("Lịch Sử Thay Đổi Đầu Kỳ, Tồn Kho, Duyệt Xuất"));
        addMenuItem(menu, "Danh Sách Phiếu Chờ Xuất", null, false,
                () -> onRowAction("Danh Sách Phiếu Chờ Xuất"));
        addMenuItem(menu, "Thẻ Kho", printerIcon(), true,
                () -> onRowAction("Thẻ Kho"));

        addMenuGroup(menu, "Sửa");
        Icon editIcon = editDocIcon();
        addMenuItem(menu, "Sửa Cơ Số Tủ Trực", editIcon, false, () -> onRowAction("Sửa Cơ Số Tủ Trực"));
        addMenuItem(menu, "Sửa Số Lượng Đóng Băng", editIcon, false, () -> onRowAction("Sửa Số Lượng Đóng Băng"));
        addMenuItem(menu, "Sửa Số Lượng Sử Dụng", editIcon, false, () -> onRowAction("Sửa Số Lượng Sử Dụng"));
        addMenuItem(menu, "Thêm/Sửa Phòng Lưu", null, false, () -> onRowAction("Thêm/Sửa Phòng Lưu"));
        addMenuItem(menu, "Xóa Phòng Lưu", null, false, () -> onRowAction("Xóa Phòng Lưu"));

        addMenuGroup(menu, "Khóa Vật Tư");
        addMenuItem(menu, "Bỏ Khóa Vật Tư", null, false, () -> onRowAction("Bỏ Khóa Vật Tư"));
        addMenuItem(menu, "Khóa Vật Tư", null, false, () -> onRowAction("Khóa Vật Tư"));

        return menu;
    }

    private void addMenuGroup(JPopupMenu menu, String title) {
        JLabel heading = new JLabel(title) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, new Color(244, 244, 241),
                        0, getHeight(), new Color(218, 218, 216)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        heading.setFont(UngDungPhongChu.TU_TRUC_MENU_NHOM);
        heading.setForeground(Color.BLACK);
        heading.setBorder(new EmptyBorder(0, 10, 0, 8));
        sizeMenuRow(heading, 21);
        menu.add(heading);
    }

    private void addMenuItem(JPopupMenu menu, String text, Icon icon, boolean separatorAbove, Runnable action) {
        JMenuItem item = new JMenuItem(text) {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                graphics.setColor(GUTTER);
                graphics.drawLine(24, 0, 24, getHeight() - 1);
                if (separatorAbove) graphics.drawLine(32, 0, getWidth() - 3, 0);
            }
        };
        item.setUI(new BasicMenuItemUI() {
            @Override protected void installDefaults() {
                super.installDefaults();
                selectionBackground = new Color(255, 232, 166);
                selectionForeground = Color.BLACK;
            }
        });
        item.setFont(UngDungPhongChu.TU_TRUC_MENU);
        item.setForeground(Color.BLACK);
        item.setBackground(Color.WHITE);
        item.setOpaque(true);
        if (icon != null) {
            item.setIcon(icon);
            item.setIconTextGap(10);
            item.setBorder(new EmptyBorder(0, 5, 0, 12));
        } else {
            item.setIconTextGap(0);
            item.setBorder(new EmptyBorder(0, 32, 0, 12));
        }
        item.addActionListener(e -> action.run());
        sizeMenuRow(item, 22);
        menu.add(item);
    }

    private static void sizeMenuRow(JComponent row, int height) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(296, height));
        row.setMinimumSize(new Dimension(296, height));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    private void wireTableInteractions() {
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { maybeShowContextMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowContextMenu(e); }

            private void maybeShowContextMenu(MouseEvent e) {
                if (!e.isPopupTrigger() && !SwingUtilities.isRightMouseButton(e)) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0 || row >= displayed.size()) return;
                table.setRowSelectionInterval(row, row);
                rowContextMenu.show(table, e.getX(), e.getY());
            }
        });
    }

    private void onRowAction(String actionName) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayed.size()) return;
        Lo item = displayed.get(row);
        statusLabel.setText("Đã chọn [" + actionName + "] cho vật tư: " + item.code() + " — " + item.name());
        if ("Lịch Sử Nhập Xuất".equals(actionName)) {
            List<Kho> cabinetList = new ArrayList<>();
            for (int i = 0; i < cabinets.getItemCount(); i++) cabinetList.add(cabinets.getItemAt(i));
            var history = new TuTrucLichSuNhapXuatHopThoai(this, data, item, cabinetList, (Kho) cabinets.getSelectedItem());
            history.setVisible(true);
            return;
        }
        JOptionPane.showMessageDialog(this,
                "Thao tác: " + actionName + "\nVật tư: " + item.code() + " - " + item.name()
                        + "\nSố lô: " + (item.batch() == null ? "-" : item.batch())
                        + " | Tồn kho: " + formatQty(item.quantity()) + " " + (item.unit() == null ? "" : item.unit()),
                actionName,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadCabinets(int initialCabinetId) {
        var ctx = KhamBenhPhienNguCanh.current();
        if (ctx == null) {
            statusLabel.setText("Vui lòng đăng nhập lại.");
            return;
        }
        int gen = ++cabinetGen;
        TuTrucVatTuManHinh.async(() -> data.getCabinets(ctx), items -> {
            if (gen != cabinetGen || !TuTrucVatTuManHinh.current(session)) return;
            changingCabinets = true;
            cabinets.removeAllItems();
            items.forEach(cabinets::addItem);
            items.stream().filter(k -> k.id() == initialCabinetId).findFirst()
                    .ifPresent(cabinets::setSelectedItem);
            changingCabinets = false;
            loadStock();
        }, err -> {
            if (gen == cabinetGen) statusLabel.setText("Không tải được danh sách tủ trực.");
        });
    }

    private void loadStock() {
        var ctx = KhamBenhPhienNguCanh.current();
        Kho selected = (Kho) cabinets.getSelectedItem();
        if (ctx == null || selected == null) {
            rawStock = List.of();
            applyFilter();
            return;
        }
        int gen = ++stockGen;
        statusLabel.setText("Đang tải vật tư trong kho…");
        TuTrucVatTuManHinh.async(() -> data.getStock(ctx, selected.id()), stock -> {
            if (gen != stockGen || !TuTrucVatTuManHinh.current(session)) return;
            rawStock = stock == null ? List.of() : List.copyOf(stock);
            statusLabel.setText("(Chưa kiểm kê)");
            applyFilter();
        }, err -> {
            if (gen == stockGen) statusLabel.setText("Không tải được vật tư trong kho.");
        });
    }

    private void applyFilter() {
        String q = TuTrucVatTuManHinh.normalize(search.getText());
        List<Lo> filtered = rawStock.stream()
                .filter(lo -> showEmptyLots.isSelected() || (lo.quantity() != null && lo.quantity().signum() > 0))
                .filter(lo -> q.isEmpty() || TuTrucVatTuManHinh.normalize(
                        lo.code() + " " + lo.name() + " " + lo.unit() + " " + lo.batch()).contains(q))
                .toList();

        if (showSummary.isSelected()) {
            filtered = aggregate(filtered, false);
        } else if (showSummaryByBatch.isSelected()) {
            filtered = aggregate(filtered, true);
        }

        displayed = filtered;
        model.setRowCount(0);
        for (Lo lo : displayed) {
            model.addRow(new Object[]{
                    lo.code(),
                    lo.name(),
                    lo.unit(),
                    lo.batch() == null ? "" : lo.batch(),
                    formatQty(lo.quantity()),
                    "",
                    "",
                    lo.expiry() == null ? "" : DATE_FMT.format(lo.expiry()),
                    "",
                    "0"
            });
        }
        updateTreeCounts(displayed.size());
    }

    private static List<Lo> aggregate(List<Lo> source, boolean byBatch) {
        Map<String, Lo> grouped = new LinkedHashMap<>();
        for (Lo lo : source) {
            String key = byBatch
                    ? lo.code() + "|" + lo.name() + "|" + lo.unit() + "|" + lo.batch() + "|" + lo.expiry()
                    : lo.code() + "|" + lo.name() + "|" + lo.unit();
            grouped.merge(key,
                    byBatch ? lo : new Lo(lo.id(), lo.code(), lo.name(), lo.unit(), "", null, lo.quantity()),
                    (a, b) -> new Lo(a.id(), a.code(), a.name(), a.unit(), a.batch(), a.expiry(),
                            safeQty(a.quantity()).add(safeQty(b.quantity()))));
        }
        return List.copyOf(grouped.values());
    }

    private void updateTreeCounts(int count) {
        allBtn.setText("Tất Cả Thuốc, Vật Tư (" + count + ")");
        var root = new DefaultMutableTreeNode("root");
        var supplies = new DefaultMutableTreeNode("Vật tư (" + count + ")");
        supplies.add(new DefaultMutableTreeNode("Vật tư hóa chất (" + count + ")"));
        var rooms = new DefaultMutableTreeNode("Phòng lưu (" + (count > 0 ? 1 : 0) + ")");
        rooms.add(new DefaultMutableTreeNode("Không có phòng lưu (" + count + ")"));
        root.add(supplies);
        root.add(rooms);
        categories.setModel(new DefaultTreeModel(root));
        for (int i = 0; i < categories.getRowCount(); i++) {
            categories.expandRow(i);
        }
    }

    private void exportCsv() {
        JFileChooser picker = new JFileChooser();
        picker.setSelectedFile(new File("danh-sach-vat-tu-trong-kho.csv"));
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
            statusLabel.setText("Đã xuất " + model.getRowCount() + " dòng vật tư.");
        } catch (IOException ex) {
            statusLabel.setText("Không ghi được tệp CSV.");
        }
    }

    private static BigDecimal safeQty(BigDecimal q) {
        return q == null ? BigDecimal.ZERO : q;
    }

    private static String formatQty(BigDecimal q) {
        return QTY_FMT.format(safeQty(q));
    }

    private static DecimalFormat createQtyFormat() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        DecimalFormat df = new DecimalFormat("#,##0.####", sym);
        df.setGroupingUsed(true);
        return df;
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

    private static Icon printerIcon() {
        return new Icon() {
            @Override public int getIconWidth() { return 14; }
            @Override public int getIconHeight() { return 14; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.translate(x, y);
                g.setColor(new Color(90, 95, 105));
                g.drawRect(3, 1, 7, 4);
                g.setColor(new Color(70, 100, 140));
                g.fillRect(1, 5, 12, 5);
                g.setColor(Color.WHITE);
                g.fillRect(3, 8, 8, 5);
                g.setColor(new Color(90, 95, 105));
                g.drawRect(3, 8, 8, 5);
                g.dispose();
            }
        };
    }

    private static Icon editDocIcon() {
        return new Icon() {
            @Override public int getIconWidth() { return 14; }
            @Override public int getIconHeight() { return 14; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.translate(x, y);
                g.setColor(Color.WHITE);
                g.fillRect(2, 1, 9, 12);
                g.setColor(new Color(60, 115, 185));
                g.drawRect(2, 1, 9, 12);
                g.drawLine(4, 5, 9, 5);
                g.drawLine(4, 8, 9, 8);
                g.dispose();
            }
        };
    }
}
