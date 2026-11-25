package vn.clinicmanagement.desktop.modules.system;

import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.pharmacy.NhaThuocGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBieuTuong;
import vn.clinicmanagement.desktop.modules.pharmacy.components.ThemThuocManHinh;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.inventory.NhaThuocDanhSachRepository;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.*;
import java.util.List;

/**
 * Màn hình toàn màn hình "Danh Sách Vật Tư" mở từ Hệ Thống -> Quản Lý Hệ Thống -> Danh Sách Vật Tư.
 * Bố cục đồng bộ với Danh Sách Thuốc và đọc dữ liệu thật từ bảng Item (loaiItem IN ('VAT_TU', 'HOA_CHAT')).
 */
public final class HeThongDanhSachVatTuHopThoai extends JDialog {
    private static final Color GRID = new Color(207, 210, 215);
    private static final Color SELECTED = new Color(0, 145, 255);
    private static final String[] COLUMNS = {
            "Mã Vật Tư", "Mã Tương Đương", "Tên Vật Tư", "Đơn Vị", "Quy Cách", "Hãng SX", "Nước SX"
    };
    private static final int[] COL_WIDTHS = {120, 110, 270, 90, 140, 140, 110};

    private static final List<String> DEFAULT_TYPES = List.of(
            "Vật tư y tế", "Vật tư tiêu hao", "Vật tư hóa chất", "Vật tư nhà thuốc", "Vật tư khác"
    );

    record VatTuRow(
            int id, String code, String equivalentCode, String name,
            String unit, String packing, String manufacturer, String origin,
            String group, boolean insured
    ) {}

    private final JdbcKetNoiNhaCungCap connections;
    private final NhaThuocDanhSachReadCongKetNoi read;
    private final NhaThuocGhepNoiGoc.Services pharmacyServices;

    final JCheckBox chkApprovalMap = new JCheckBox("Ánh Xạ Danh Mục Phê Duyệt");
    final JCheckBox chkSummary = new JCheckBox("Hiển thị tổng hợp", true);
    final JButton allBtn = new JButton("Tất Cả Vật Tư (0)");
    final JTree tree = new JTree(new DefaultMutableTreeNode("root"));
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

    private List<VatTuRow> allSupplies = List.of();
    private List<VatTuRow> displayed = List.of();
    private String selectedGroupFilter = "";
    private Boolean selectedBHYTFilter = null;
    private boolean changingTree;
    private int loadGen;

    public HeThongDanhSachVatTuHopThoai(Window owner) {
        super(owner, buildTitle(), ModalityType.APPLICATION_MODAL);
        var uow = new JdbcUnitOfWork();
        var tx = new JdbcGiaoDichQuanLy(uow);
        this.connections = new JdbcKetNoiNhaCungCap(tx);
        this.read = new NhaThuocDanhSachRepository(connections);
        this.pharmacyServices = NhaThuocGhepNoiGoc.createProduction();

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
        JButton clearBtn = new JButton("✕");
        clearBtn.setFont(UngDungPhongChu.BODY_BOLD);
        clearBtn.setForeground(new Color(180, 40, 40));
        clearBtn.setPreferredSize(new Dimension(28, 26));
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> {
            search.setText("");
            applyFilter();
        });
        searchBar.add(search, BorderLayout.CENTER);
        searchBar.add(clearBtn, BorderLayout.EAST);
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
        split.setDividerLocation(245);
        add(split, BorderLayout.CENTER);

        chkSummary.addActionListener(e -> applyFilter());
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
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "closeDialog");
        getRootPane().getActionMap().put("closeDialog", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });

        loadSupplies();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.setBackground(new Color(248, 248, 248));
        bar.setBorder(new MatteBorder(0, 0, 1, 0, GRID));
        bar.setPreferredSize(new Dimension(100, 56));

        JButton menuBtn = new JButton(scaleIcon(NhaThuocBieuTuong.createMenuIcon(), 22));
        menuBtn.setPreferredSize(new Dimension(40, 46));
        menuBtn.setFocusPainted(false);
        menuBtn.setContentAreaFilled(false);
        menuBtn.setBorder(new EmptyBorder(2, 4, 2, 4));
        menuBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPopupMenu sysMenu = new JPopupMenu();
        JMenuItem miRefresh = new JMenuItem("Làm mới");
        miRefresh.setFont(UngDungPhongChu.BODY);
        miRefresh.addActionListener(e -> loadSupplies());
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

        JButton refreshBtn = toolBtn("Làm Mới", refreshIcon(22), 62);
        refreshBtn.addActionListener(e -> loadSupplies());

        JButton addBtn = toolBtn("Thêm", scaleIcon(NhaThuocBieuTuong.createPlusIcon(), 22), 54);
        addBtn.addActionListener(e -> openAddSupply());

        chkApprovalMap.setFont(UngDungPhongChu.BODY);
        chkApprovalMap.setOpaque(false);
        chkApprovalMap.setFocusPainted(false);
        chkSummary.setFont(UngDungPhongChu.BODY);
        chkSummary.setOpaque(false);
        chkSummary.setFocusPainted(false);

        bar.add(menuBtn);
        bar.add(refreshBtn);
        bar.add(addBtn);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(chkApprovalMap);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(chkSummary);

        return bar;
    }

    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Color.WHITE);
        left.setMinimumSize(new Dimension(190, 0));

        allBtn.setFont(UngDungPhongChu.BODY_BOLD);
        allBtn.setHorizontalAlignment(SwingConstants.LEFT);
        allBtn.setBorder(new EmptyBorder(3, 8, 3, 4));
        allBtn.setPreferredSize(new Dimension(245, 25));
        allBtn.setBackground(SELECTED);
        allBtn.setForeground(Color.WHITE);
        allBtn.setOpaque(true);
        allBtn.setFocusPainted(false);
        allBtn.addActionListener(e -> {
            selectedGroupFilter = "";
            selectedBHYTFilter = null;
            tree.clearSelection();
            allBtn.setBackground(SELECTED);
            allBtn.setForeground(Color.WHITE);
            applyFilter();
        });
        left.add(allBtn, BorderLayout.NORTH);

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(21);
        tree.setFont(UngDungPhongChu.SMALL);
        tree.setBackground(Color.WHITE);
        tree.putClientProperty("JTree.lineStyle", "Angled");
        if (tree.getUI() instanceof javax.swing.plaf.basic.BasicTreeUI ui) {
            ui.setExpandedIcon(NhaThuocBieuTuong.createMinusBoxIcon());
            ui.setLeftChildIndent(10);
            ui.setRightChildIndent(16);
        }
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree t, Object value, boolean selected,
                                                          boolean expanded, boolean leaf, int row, boolean focus) {
                super.getTreeCellRendererComponent(t, value, selected, expanded, leaf, row, false);
                setLeafIcon(null);
                setClosedIcon(null);
                setOpenIcon(null);
                setFont(leaf ? UngDungPhongChu.SMALL : UngDungPhongChu.SMALL_BOLD);
                setTextSelectionColor(Color.WHITE);
                setBackgroundSelectionColor(SELECTED);
                return this;
            }
        });
        tree.addTreeSelectionListener(e -> {
            if (changingTree) return;
            var node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null) return;
            if (node.getUserObject() instanceof TreeFilter tf) {
                selectedGroupFilter = tf.group();
                selectedBHYTFilter = tf.insured();
                allBtn.setBackground(Color.WHITE);
                allBtn.setForeground(Color.BLACK);
                applyFilter();
            }
        });

        JScrollPane scroll = new JScrollPane(tree);
        scroll.setBorder(null);
        left.add(scroll, BorderLayout.CENTER);
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
        t.setRowHeight(23);
        t.setFont(UngDungPhongChu.BODY);
        t.setGridColor(GRID);
        t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setFillsViewportHeight(true);
        t.setBackground(Color.WHITE);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        t.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        t.getTableHeader().setPreferredSize(new Dimension(100, 26));
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
                setHorizontalAlignment(column == 1 || column == 3 ? CENTER : LEFT);
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
        t.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) openEditSupply();
            }
        });
        return t;
    }

    private void loadSupplies() {
        int gen = ++loadGen;
        new SwingWorker<List<VatTuRow>, Void>() {
            @Override protected List<VatTuRow> doInBackground() {
                String sql = "SELECT i.id, i.maItem, ISNULL(i.maTuongDuong, N''), i.tenItem, "
                        + "ISNULL(i.donViCoBan, N''), ISNULL(i.quyCachDongGoi, N''), "
                        + "ISNULL(i.hangSanXuat, N''), ISNULL(i.nuocSanXuat, N''), "
                        + "COALESCE(NULLIF(i.nhomThuoc, N''), N'Vật tư y tế'), i.bhytChiTra "
                        + "FROM dbo.Item i "
                        + "WHERE i.isDeleted = 0 AND i.hoatDong = 1 AND i.loaiItem IN ('VAT_TU', 'HOA_CHAT') "
                        + "ORDER BY i.tenItem, i.id";
                List<VatTuRow> rows = new ArrayList<>();
                try (var lease = connections.acquire();
                     var ps = lease.connection().prepareStatement(sql);
                     var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new VatTuRow(
                                rs.getInt(1),
                                rs.getString(2),
                                rs.getString(3),
                                rs.getString(4),
                                rs.getString(5),
                                rs.getString(6),
                                rs.getString(7),
                                rs.getString(8),
                                rs.getString(9),
                                rs.getBoolean(10)));
                    }
                    return List.copyOf(rows);
                } catch (SQLException ex) {
                    throw new IllegalStateException("Không tải được danh mục vật tư.", ex);
                }
            }

            @Override protected void done() {
                if (gen != loadGen || !isDisplayable()) return;
                try {
                    allSupplies = get();
                    applyFilter();
                } catch (Exception ex) {
                    allSupplies = List.of();
                    applyFilter();
                }
            }
        }.execute();
    }

    private void applyFilter() {
        String q = normalize(search.getText());
        List<VatTuRow> source = chkSummary.isSelected() ? deduplicateById(allSupplies) : allSupplies;

        displayed = source.stream()
                .filter(d -> selectedGroupFilter.isEmpty() || selectedGroupFilter.equals(d.group()))
                .filter(d -> selectedBHYTFilter == null || selectedBHYTFilter == d.insured())
                .filter(d -> q.isEmpty() || normalize(
                        d.code() + " " + d.equivalentCode() + " " + d.name() + " " + d.unit() + " " + d.packing())
                        .contains(q))
                .toList();

        model.setRowCount(0);
        for (VatTuRow d : displayed) {
            model.addRow(new Object[]{
                    Objects.toString(d.code(), ""),
                    Objects.toString(d.equivalentCode(), ""),
                    Objects.toString(d.name(), ""),
                    Objects.toString(d.unit(), ""),
                    Objects.toString(d.packing(), ""),
                    Objects.toString(d.manufacturer(), ""),
                    Objects.toString(d.origin(), "")
            });
        }
        updateTree(source);
    }

    private static List<VatTuRow> deduplicateById(List<VatTuRow> list) {
        Map<Integer, VatTuRow> byId = new LinkedHashMap<>();
        for (VatTuRow d : list) byId.putIfAbsent(d.id(), d);
        return List.copyOf(byId.values());
    }

    private record TreeFilter(String label, String group, Boolean insured, int count) {
        @Override public String toString() { return label + " (" + count + ")"; }
    }

    private void updateTree(List<VatTuRow> source) {
        allBtn.setText("Tất Cả Vật Tư (" + source.size() + ")");
        changingTree = true;
        var root = new DefaultMutableTreeNode("root");

        var typeGroup = new DefaultMutableTreeNode("Loại Vật Tư");
        Set<String> groups = new LinkedHashSet<>(DEFAULT_TYPES);
        for (VatTuRow d : source) {
            if (d.group() != null && !d.group().isBlank()) groups.add(d.group());
        }
        for (String g : groups) {
            int cnt = (int) source.stream().filter(d -> g.equals(d.group())).count();
            typeGroup.add(new DefaultMutableTreeNode(new TreeFilter(g, g, null, cnt)));
        }

        var bhytGroup = new DefaultMutableTreeNode("Nhóm BHYT");
        int inBhyt = (int) source.stream().filter(VatTuRow::insured).count();
        int outBhyt = (int) source.stream().filter(d -> !d.insured()).count();
        bhytGroup.add(new DefaultMutableTreeNode(new TreeFilter("Vật tư trong danh mục BHYT", "", Boolean.TRUE, inBhyt)));
        bhytGroup.add(new DefaultMutableTreeNode(new TreeFilter("Vật tư ngoài danh mục", "", Boolean.FALSE, outBhyt)));

        var statusGroup = new DefaultMutableTreeNode("Trạng Thái Nhập Nhà Cung Cấp");
        statusGroup.add(new DefaultMutableTreeNode(new TreeFilter("Được nhập mới", "", null, source.size())));

        root.add(typeGroup);
        root.add(bhytGroup);
        root.add(statusGroup);
        tree.setModel(new DefaultTreeModel(root));
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
        changingTree = false;
    }

    private void openAddSupply() {
        JDialog dlg = new JDialog(this, "Thêm Vật Tư", ModalityType.APPLICATION_MODAL);
        dlg.setContentPane(new ThemThuocManHinh(pharmacyServices.saveDrug(), () -> {
            dlg.dispose();
            loadSupplies();
        }));
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void openEditSupply() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayed.size()) return;
        VatTuRow selected = displayed.get(row);
        var ctx = KhamBenhPhienNguCanh.current();
        int facilityId = ctx == null ? 1 : ctx.facilityId();
        Map<String, String> vals = read.getDrugFullValues(selected.id(), facilityId);
        JDialog dlg = new JDialog(this, "Thông Tin Vật Tư - " + selected.code(), ModalityType.APPLICATION_MODAL);
        dlg.setContentPane(new ThemThuocManHinh(pharmacyServices.saveDrug(), selected.id(), vals, () -> {
            dlg.dispose();
            loadSupplies();
        }));
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void exportCsv() {
        JFileChooser picker = new JFileChooser();
        picker.setSelectedFile(new File("danh-sach-vat-tu.csv"));
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

    private static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }

    private static JButton toolBtn(String text, Icon icon, int width) {
        JButton b = new JButton(text, icon);
        b.setFont(UngDungPhongChu.SMALL);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setIconTextGap(2);
        b.setPreferredSize(new Dimension(width, 46));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(2, 4, 2, 4));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static Icon scaleIcon(Icon icon, int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D copy = (Graphics2D) g.create();
                copy.translate(x, y);
                copy.scale((double) size / icon.getIconWidth(), (double) size / icon.getIconHeight());
                icon.paintIcon(c, copy, 0, 0);
                copy.dispose();
            }
        };
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

    private static String buildTitle() {
        String user = Objects.toString(NguoiDungPhien.getInstance().getFullName(), "").trim();
        if (user.isEmpty()) user = "Quản Trị Hệ Thống";
        return "EHC - Danh Sách Vật Tư   ----- Đăng nhập : " + user + " -----";
    }
}
