package vn.clinicmanagement.desktop.modules.inventory;


import vn.clinicmanagement.desktop.modules.print.inventory.TuTrucBaoCaoXuatNhapTonPreviewHopThoai;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.Kho;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.Phieu;
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
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Màn hình toàn màn hình "Báo Cáo Xuất Nhập Tồn" (Kho Vật Tư) mở khi bấm
 * Báo Cáo -> Báo Cáo Xuất Nhập Tồn trong tab Tủ Trực Vật Tư.
 */
final class TuTrucBaoCaoXuatNhapTonHopThoai extends JDialog {
    private static final Color GRID = new Color(207, 210, 215);
    private static final Color SELECTED = new Color(0, 145, 255);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private static final String[] COLUMNS = {
            "", "Trạng Thái Phiếu", "Mã Phiếu", "Loại Phiếu",
            "Ngày Tạo", "Ngày Y Lệnh", "Ngày Duyệt", "Ngày Nhập/Xuất", "Khoa Lập Phiếu"
    };
    private static final int[] COL_WIDTHS = {26, 125, 85, 165, 125, 125, 125, 125, 180};

    private final TuTrucCongKetNoi data;
    private final String session;

    final JComboBox<Kho> warehouses = new JComboBox<>();
    final JTextField ticketCodeField = new JTextField();
    final JComboBox<String> findByCombo = new JComboBox<>(new String[]{
            "Hiển thị tổng hợp", "Chi tiết theo lô", "Theo loại phiếu"
    });
    final JSpinner fromDate;
    final JSpinner untilDate;
    final JButton allBtn = new JButton("Tất Cả Phiếu (0)");
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

    private List<Phieu> tickets = List.of();
    private List<Phieu> displayed = List.of();
    private String selectedType = "";
    private String selectedDept = "";
    private boolean changingTree, changingWarehouses;
    private int whGen, ticketGen;

    TuTrucBaoCaoXuatNhapTonHopThoai(Window owner, TuTrucCongKetNoi data) {
        super(owner, "PHẦN MỀM QUẢN LÝ BỆNH VIỆN", ModalityType.MODELESS);
        this.data = Objects.requireNonNull(data);
        var ctx = KhamBenhPhienNguCanh.current();
        this.session = ctx == null ? "" : ctx.sessionId().toString();

        LocalDate today = LocalDate.now();
        fromDate = dateTimeSpinner(today.atStartOfDay());
        untilDate = dateTimeSpinner(today.atTime(23, 59));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        add(buildToolbar(), BorderLayout.NORTH);

        JPanel left = buildLeftPanel();
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Color.WHITE);

        JPanel searchBar = new JPanel(new BorderLayout());
        searchBar.setPreferredSize(new Dimension(100, 28));
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
        split.setDividerLocation(235);
        add(split, BorderLayout.CENTER);

        warehouses.addActionListener(e -> {
            if (!changingWarehouses) {
                selectedType = "";
                selectedDept = "";
                loadTickets();
            }
        });
        DocumentListener filterDoc = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        };
        ticketCodeField.getDocument().addDocumentListener(filterDoc);
        search.getDocument().addDocumentListener(filterDoc);

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
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "closeWindow");
        getRootPane().getActionMap().put("closeWindow", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });

        loadWarehouses();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(248, 248, 248));
        bar.setBorder(new MatteBorder(0, 0, 1, 0, GRID));
        bar.setPreferredSize(new Dimension(100, 62));

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        leftTools.setOpaque(false);

        JButton menuBtn = toolBtn("Menu", TuTrucThanhCongCu.scaled(NhaThuocBieuTuong.createMenuIcon(), 22), 50);
        JPopupMenu sysMenu = new JPopupMenu();
        JMenuItem miRefresh = new JMenuItem("Làm mới");
        miRefresh.setFont(UngDungPhongChu.BODY);
        miRefresh.addActionListener(e -> loadTickets());
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

        JButton reportBtn = toolBtn("Báo Cáo ▾", TuTrucThanhCongCu.scaled(NhaThuocBieuTuong.createReportIcon(), 22), 68);
        JPopupMenu reportMenu = new JPopupMenu();
        JMenuItem miPrint = new JMenuItem("In báo cáo xuất nhập tồn");
        miPrint.setFont(UngDungPhongChu.BODY);
        miPrint.addActionListener(e -> openReportOptions());
        JMenuItem miCsv = new JMenuItem("Xuất danh sách CSV");
        miCsv.setFont(UngDungPhongChu.BODY);
        miCsv.addActionListener(e -> exportCsv());
        reportMenu.add(miPrint);
        reportMenu.add(miCsv);
        reportBtn.addActionListener(e -> reportMenu.show(reportBtn, 0, reportBtn.getHeight()));

        JButton listBtn = toolBtn("Danh Sách ▾", TuTrucThanhCongCu.scaled(NhaThuocBieuTuong.createListIcon(), 22), 74);
        JPopupMenu listMenu = new JPopupMenu();
        JMenuItem miStock = new JMenuItem("Danh sách vật tư trong kho");
        miStock.setFont(UngDungPhongChu.BODY);
        miStock.addActionListener(e -> {
            Kho wh = (Kho) warehouses.getSelectedItem();
            new TuTrucVatTuTrongKhoHopThoai(this, data, wh == null ? 0 : wh.id()).setVisible(true);
        });
        listMenu.add(miStock);
        listBtn.addActionListener(e -> listMenu.show(listBtn, 0, listBtn.getHeight()));

        JButton createBtn = toolBtn("Tạo Phiếu ▾", TuTrucThanhCongCu.scaled(NhaThuocBieuTuong.createPlusIcon(), 22), 74);
        JPopupMenu createMenu = new TuTrucTaoPhieuMenu(type -> {});
        createBtn.addActionListener(e -> createMenu.show(createBtn, 0, createBtn.getHeight()));

        leftTools.add(menuBtn);
        leftTools.add(reportBtn);
        leftTools.add(listBtn);
        leftTools.add(createBtn);

        JLabel codeLbl = new JLabel("Mã phiếu:");
        codeLbl.setFont(UngDungPhongChu.BODY);
        codeLbl.setBorder(new EmptyBorder(0, 6, 0, 2));
        ticketCodeField.setFont(UngDungPhongChu.BODY);
        ticketCodeField.setPreferredSize(new Dimension(120, 26));
        leftTools.add(codeLbl);
        leftTools.add(ticketCodeField);

        JLabel findByLbl = new JLabel("Tìm theo:");
        findByLbl.setFont(UngDungPhongChu.BODY);
        findByLbl.setBorder(new EmptyBorder(0, 10, 0, 2));
        findByCombo.setFont(UngDungPhongChu.BODY);
        findByCombo.setPreferredSize(new Dimension(150, 26));
        leftTools.add(findByLbl);
        leftTools.add(findByCombo);

        bar.add(leftTools, BorderLayout.WEST);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        rightTools.setOpaque(false);

        JPanel dates = new JPanel(new GridLayout(2, 2, 4, 2));
        dates.setOpaque(false);
        JLabel fromLbl = new JLabel("Từ ngày");
        fromLbl.setFont(UngDungPhongChu.SMALL);
        JLabel untilLbl = new JLabel("Đến ngày");
        untilLbl.setFont(UngDungPhongChu.SMALL);
        fromDate.setPreferredSize(new Dimension(135, 24));
        untilDate.setPreferredSize(new Dimension(135, 24));
        dates.add(fromLbl);
        dates.add(fromDate);
        dates.add(untilLbl);
        dates.add(untilDate);

        JButton searchBtn = new JButton("Tìm Kiếm", TuTrucThanhCongCu.scaled(NhaThuocBieuTuong.createSearchIcon(), 22));
        searchBtn.setFont(UngDungPhongChu.BODY);
        searchBtn.setPreferredSize(new Dimension(102, 46));
        searchBtn.setFocusPainted(false);
        searchBtn.setContentAreaFilled(false);
        searchBtn.setBorder(new EmptyBorder(2, 6, 2, 6));
        searchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchBtn.addActionListener(e -> loadTickets());

        rightTools.add(dates);
        rightTools.add(searchBtn);
        bar.add(rightTools, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Color.WHITE);
        left.setMinimumSize(new Dimension(180, 0));

        warehouses.setFont(UngDungPhongChu.TU_TRUC_TEN_TU);
        warehouses.setForeground(Color.RED);
        warehouses.setBackground(Color.WHITE);
        warehouses.setPreferredSize(new Dimension(235, 28));
        warehouses.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value == null) setText("(Chọn kho vật tư)");
                setForeground(selected ? Color.WHITE : Color.RED);
                return this;
            }
        });
        left.add(warehouses, BorderLayout.NORTH);

        JPanel treeArea = new JPanel(new BorderLayout());
        treeArea.setBackground(Color.WHITE);

        allBtn.setFont(UngDungPhongChu.TU_TRUC_BOLD);
        allBtn.setHorizontalAlignment(SwingConstants.LEFT);
        allBtn.setBorder(new EmptyBorder(2, 8, 2, 4));
        allBtn.setPreferredSize(new Dimension(235, 24));
        allBtn.setBackground(SELECTED);
        allBtn.setForeground(Color.WHITE);
        allBtn.setOpaque(true);
        allBtn.setFocusPainted(false);
        allBtn.addActionListener(e -> {
            selectedType = "";
            selectedDept = "";
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
                setFont(leaf ? UngDungPhongChu.TU_TRUC : UngDungPhongChu.TU_TRUC_BOLD);
                setTextSelectionColor(Color.WHITE);
                setBackgroundSelectionColor(SELECTED);
                return this;
            }
        });
        categories.addTreeSelectionListener(e -> {
            if (changingTree) return;
            var node = (DefaultMutableTreeNode) categories.getLastSelectedPathComponent();
            if (node == null) return;
            if (node.getUserObject() instanceof FilterNode fn) {
                selectedType = fn.isDept() ? "" : fn.key();
                selectedDept = fn.isDept() ? fn.key() : "";
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
                setIcon(null);
                setHorizontalAlignment(column == 2 || (column >= 4 && column <= 7) ? CENTER : LEFT);

                Phieu p = row >= 0 && row < displayed.size() ? displayed.get(row) : null;
                if (column == 0 && p != null) {
                    setText("");
                    setHorizontalAlignment(CENTER);
                    setIcon(dot(p.status()));
                    setBackground(Color.WHITE);
                    return this;
                }

                if (selected) {
                    setBackground(column == 1 ? new Color(182, 250, 252) : SELECTED);
                    setForeground(column == 1 ? Color.BLACK : Color.WHITE);
                } else {
                    setBackground(Color.WHITE);
                    setForeground("DA_HUY".equals(p == null ? "" : p.status()) ? Color.RED : Color.BLACK);
                }
                return this;
            }
        });
        t.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) openReportOptions();
            }
        });
        t.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "openOptions");
        t.getActionMap().put("openOptions", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { openReportOptions(); }
        });
        return t;
    }

    private void openReportOptions() {
        var opts = new TuTrucTuyChonBaoCaoHopThoai[1];
        opts[0] = new TuTrucTuyChonBaoCaoHopThoai(this, () -> {
            Kho wh = (Kho) warehouses.getSelectedItem();
            String whName = wh == null ? "Kho hóa chất" : wh.name();
            var fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            String fromStr = fmt.format(spinnerDateTime(opts[0].fromSpinner));
            String untilStr = fmt.format(spinnerDateTime(opts[0].untilSpinner));
            String grp = "(" + selectedGroupText(opts[0]) + ")";
            new TuTrucBaoCaoXuatNhapTonPreviewHopThoai(this, whName, fromStr, untilStr, grp).setVisible(true);
        });
        opts[0].setVisible(true);
    }

    private static String selectedGroupText(TuTrucTuyChonBaoCaoHopThoai o) {
        for (JRadioButton rb : new JRadioButton[]{
                o.grpNone, o.grpType, o.grpRoom, o.grpSupplier, o.grpImporter, o.grpExporter, o.grpPharma
        }) {
            if (rb.isSelected()) return rb.getText();
        }
        return "Không nhóm dữ liệu";
    }

    private void loadWarehouses() {
        var ctx = KhamBenhPhienNguCanh.current();
        if (ctx == null) return;
        int gen = ++whGen;
        TuTrucVatTuManHinh.async(() -> data.getSupplyWarehouses(ctx), items -> {
            if (gen != whGen || !TuTrucVatTuManHinh.current(session)) return;
            changingWarehouses = true;
            warehouses.removeAllItems();
            if (items != null) items.forEach(warehouses::addItem);
            changingWarehouses = false;
            loadTickets();
        }, err -> {});
    }

    private void loadTickets() {
        var ctx = KhamBenhPhienNguCanh.current();
        Kho selected = (Kho) warehouses.getSelectedItem();
        if (ctx == null || selected == null) {
            tickets = List.of();
            applyFilter();
            return;
        }
        int gen = ++ticketGen;
        LocalDateTime from = spinnerDateTime(fromDate);
        LocalDateTime until = spinnerDateTime(untilDate);
        TuTrucVatTuManHinh.async(() -> data.getTickets(ctx, selected.id(), from, until), items -> {
            if (gen != ticketGen || !TuTrucVatTuManHinh.current(session)) return;
            tickets = items == null ? List.of() : List.copyOf(items);
            applyFilter();
        }, err -> {
            if (gen == ticketGen) {
                tickets = List.of();
                applyFilter();
            }
        });
    }

    private void applyFilter() {
        String codeQ = TuTrucVatTuManHinh.normalize(ticketCodeField.getText());
        String q = TuTrucVatTuManHinh.normalize(search.getText());
        displayed = tickets.stream()
                .filter(p -> selectedType.isEmpty() || selectedType.equals(p.type()))
                .filter(p -> selectedDept.isEmpty() || selectedDept.equals(p.department()))
                .filter(p -> codeQ.isEmpty() || TuTrucVatTuManHinh.normalize(p.code()).contains(codeQ))
                .filter(p -> q.isEmpty() || TuTrucVatTuManHinh.normalize(
                        p.code() + " " + TuTrucVatTuManHinh.status(p) + " " + p.type() + " " + p.department()).contains(q))
                .toList();

        model.setRowCount(0);
        for (Phieu p : displayed) {
            String created = p.createdAt() == null ? "" : TIME_FMT.format(p.createdAt());
            String order = p.orderDate() == null ? "" : TIME_FMT.format(p.orderDate());
            model.addRow(new Object[]{
                    p,
                    TuTrucVatTuManHinh.status(p),
                    p.code(),
                    p.type(),
                    created,
                    order,
                    "",
                    "",
                    p.department()
            });
        }
        updateTree();
    }

    private record FilterNode(String key, int count, boolean isDept) {
        @Override public String toString() { return key + " (" + count + ")"; }
    }

    private void updateTree() {
        allBtn.setText("Tất Cả Phiếu (" + tickets.size() + ")");
        changingTree = true;
        var root = new DefaultMutableTreeNode("root");
        var typeGroup = new DefaultMutableTreeNode("Loại Phiếu");
        var deptGroup = new DefaultMutableTreeNode("Khoa Lập Phiếu");

        Map<String, Integer> types = new LinkedHashMap<>();
        Map<String, Integer> depts = new LinkedHashMap<>();
        for (Phieu p : tickets) {
            if (p.type() != null && !p.type().isBlank()) types.merge(p.type(), 1, Integer::sum);
            if (p.department() != null && !p.department().isBlank()) depts.merge(p.department(), 1, Integer::sum);
        }
        types.forEach((k, v) -> typeGroup.add(new DefaultMutableTreeNode(new FilterNode(k, v, false))));
        depts.forEach((k, v) -> deptGroup.add(new DefaultMutableTreeNode(new FilterNode(k, v, true))));

        root.add(typeGroup);
        root.add(deptGroup);
        categories.setModel(new DefaultTreeModel(root));
        for (int i = 0; i < categories.getRowCount(); i++) categories.expandRow(i);
        changingTree = false;
    }

    private void exportCsv() {
        JFileChooser picker = new JFileChooser();
        picker.setSelectedFile(new File("bao-cao-xuat-nhap-ton.csv"));
        if (picker.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = picker.getSelectedFile();
        try (var writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            for (int r = -1; r < model.getRowCount(); r++) {
                for (int c = 1; c < model.getColumnCount(); c++) {
                    if (c > 1) writer.write(',');
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

    private static JButton toolBtn(String text, Icon icon, int width) {
        JButton b = new JButton(text, icon);
        b.setFont(UngDungPhongChu.SMALL);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setIconTextGap(2);
        b.setPreferredSize(new Dimension(width, 50));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(2, 4, 2, 4));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JSpinner dateTimeSpinner(LocalDateTime dt) {
        Date val = Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
        JSpinner s = new JSpinner(new SpinnerDateModel(val, null, null, Calendar.MINUTE));
        s.setEditor(new JSpinner.DateEditor(s, "HH:mm dd/MM/yyyy"));
        s.setFont(UngDungPhongChu.SMALL);
        return s;
    }

    private static LocalDateTime spinnerDateTime(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static Icon dot(String status) {
        return new Icon() {
            @Override public int getIconWidth() { return 12; }
            @Override public int getIconHeight() { return 12; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D copy = (Graphics2D) g.create();
                copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                copy.setColor(switch (status == null ? "" : status) {
                    case "DA_XUAT" -> new Color(0, 128, 0);
                    case "CHO_DUYET", "TAM" -> new Color(244, 170, 20);
                    case "DA_HUY", "TU_CHOI" -> Color.GRAY;
                    default -> new Color(185, 0, 0);
                });
                copy.fillOval(x, y, 12, 12);
                copy.dispose();
            }
        };
    }
}
