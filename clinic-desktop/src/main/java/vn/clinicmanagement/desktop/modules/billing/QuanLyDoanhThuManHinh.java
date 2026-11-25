package vn.clinicmanagement.desktop.modules.billing;

import vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort;
import vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort.RevenueVisitRow;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.billing.QuanLyDoanhThuRepository;

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
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

/**
 * Màn hình Quản Lý Doanh Thu (mở từ Quản Lý -> Quản Lý Doanh Thu).
 * Đồng bộ bố cục EHC và đọc dữ liệu thật từ bảng LuotKham, BenhNhan, Khoa, PhongKham, HoaDon.
 */
public final class QuanLyDoanhThuManHinh extends CoSoManHinhManHinh {
    private static final Color GRID = new Color(207, 210, 215);
    private static final Color SELECTED = new Color(0, 145, 255);
    private static final Color TOOLBAR_BG = new Color(238, 242, 247);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");

    private static final String[] COLUMNS = {
            "", "Ngày ra viện", "Ngày vào viện", "Mã BN", "Tên Bệnh Nhân",
            "Tuổi", "Giới Tính", "Khoa", "Phòng", "Tổng Chi Phí", "BN TT"
    };
    private static final int[] COL_WIDTHS = {26, 95, 95, 85, 190, 75, 70, 140, 165, 105, 105};

    private final QuanLyDoanhThuReadPort readPort;
    final JRadioButton rbAdmitDate = new JRadioButton("Lọc theo ngày vào viện", true);
    final JRadioButton rbDischargeDate = new JRadioButton("Lọc theo ngày ra viện");
    final JTextField fromField = new JTextField();
    final JTextField toField = new JTextField();
    final JButton allPatientsBtn = new JButton("Tất cả bệnh nhân (0)");
    final JTree filterTree = new JTree(new DefaultMutableTreeNode("root"));
    final JTextField searchField = new JTextField() {
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
    private final JPanel leftPanel;
    private final JSplitPane splitPane;

    private record TreeFilter(String category, String label, String value, int count) {
        @Override public String toString() { return label + " (" + count + ")"; }
    }

    private List<RevenueVisitRow> allRows = List.of();
    private List<RevenueVisitRow> displayedRows = List.of();
    private String selectedCategory = "";
    private String selectedValue = "";
    private boolean changingTree;
    private int loadGen;

    public QuanLyDoanhThuManHinh() {
        super("revenue-management", "Quản Lý Doanh Thu", "💰");
        var uow = new JdbcUnitOfWork();
        var tx = new JdbcGiaoDichQuanLy(uow);
        this.readPort = new QuanLyDoanhThuRepository(new JdbcKetNoiNhaCungCap(tx));

        LocalDate today = LocalDate.now();
        fromField.setText("00:00 " + today.minusDays(30).format(DATE_FMT));
        toField.setText("23:59 " + today.format(DATE_FMT));

        setBackground(Color.WHITE);
        leftPanel = buildLeftPanel();
        table = buildTable();

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.WHITE);
        rightPanel.add(buildSearchBar(), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createLineBorder(GRID));
        scroll.getViewport().setBackground(Color.WHITE);
        rightPanel.add(scroll, BorderLayout.CENTER);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setBorder(null);
        splitPane.setDividerSize(3);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(235);

        add(buildToolbar(), BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control F"), "find");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("meta F"), "find");
        getActionMap().put("find", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { searchField.requestFocusInWindow(); }
        });

        refreshData();
    }

    @Override
    public void refreshData() {
        loadRows();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(TOOLBAR_BG);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(180, 188, 200)));
        bar.setPreferredSize(new Dimension(100, 56));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        left.setOpaque(false);

        JButton toggleTreeBtn = new JButton("☰");
        toggleTreeBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        toggleTreeBtn.setForeground(new Color(0, 102, 184));
        toggleTreeBtn.setPreferredSize(new Dimension(36, 46));
        toggleTreeBtn.setFocusPainted(false);
        toggleTreeBtn.setContentAreaFilled(false);
        toggleTreeBtn.setBorder(new EmptyBorder(2, 4, 2, 4));
        toggleTreeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleTreeBtn.addActionListener(e -> {
            boolean show = !leftPanel.isVisible();
            leftPanel.setVisible(show);
            if (show) splitPane.setDividerLocation(235);
            revalidate();
        });

        JButton reportBtn = new JButton("Báo Cáo ▾", reportIcon(24));
        reportBtn.setFont(UngDungPhongChu.SMALL);
        reportBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        reportBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        reportBtn.setIconTextGap(2);
        reportBtn.setPreferredSize(new Dimension(78, 48));
        reportBtn.setFocusPainted(false);
        reportBtn.setContentAreaFilled(false);
        reportBtn.setBorder(new EmptyBorder(2, 6, 2, 6));
        reportBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPopupMenu reportMenu = buildReportMenu();
        reportBtn.addActionListener(e -> reportMenu.show(reportBtn, 0, reportBtn.getHeight()));

        left.add(toggleTreeBtn);
        left.add(reportBtn);
        bar.add(left, BorderLayout.WEST);

        // Right filter area
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(2, 8, 2, 8));
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbAdmitDate);
        bg.add(rbDischargeDate);
        for (JRadioButton rb : List.of(rbAdmitDate, rbDischargeDate)) {
            rb.setOpaque(false);
            rb.setFont(UngDungPhongChu.SMALL);
            rb.setFocusPainted(false);
        }

        fromField.setFont(UngDungPhongChu.SMALL);
        fromField.setPreferredSize(new Dimension(130, 22));
        toField.setFont(UngDungPhongChu.SMALL);
        toField.setPreferredSize(new Dimension(130, 22));

        JButton searchBtn = new JButton("Tìm Kiếm ▾", searchIcon(22));
        searchBtn.setFont(UngDungPhongChu.SMALL);
        searchBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        searchBtn.setIconTextGap(6);
        searchBtn.setFocusPainted(false);
        searchBtn.setContentAreaFilled(false);
        searchBtn.setBorder(new EmptyBorder(4, 10, 4, 10));
        searchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchBtn.addActionListener(e -> loadRows());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(1, 4, 1, 6);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0; right.add(rbAdmitDate, gc);
        gc.gridy = 1; right.add(rbDischargeDate, gc);

        gc.gridx = 1; gc.gridy = 0; right.add(smallLabel("Từ ngày"), gc);
        gc.gridy = 1; right.add(smallLabel("Đến ngày"), gc);

        gc.gridx = 2; gc.gridy = 0; right.add(fromField, gc);
        gc.gridy = 1; right.add(toField, gc);

        gc.gridx = 3; gc.gridy = 0; gc.gridheight = 2; gc.fill = GridBagConstraints.VERTICAL;
        right.add(searchBtn, gc);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPopupMenu buildReportMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createLineBorder(new Color(165, 170, 178)));
        String[] items = {
                "Báo Cáo Doanh Thu Theo Phiếu Thu",
                "Báo Cáo Doanh Thu Theo Loại Hình Dịch Vụ",
                "Báo Cáo Doanh Thu Theo Phòng Khám",
                "Báo Cáo Theo Dịch Vụ (019)",
                "Báo Cáo Doanh Thu Theo Dịch Vụ Chi Tiết (019)"
        };
        for (String title : items) {
            JMenuItem mi = new JMenuItem(title);
            mi.setFont(UngDungPhongChu.BODY);
            mi.setBackground(Color.WHITE);
            mi.setBorder(new EmptyBorder(5, 16, 5, 16));
            if ("Báo Cáo Doanh Thu Theo Phiếu Thu".equals(title)) {
                mi.addActionListener(e -> new TuyChonBaoCaoDoanhThuPhieuThuHopThoai(
                        SwingUtilities.getWindowAncestor(this),
                        readPort,
                        title,
                        fromField.getText(),
                        toField.getText()).setVisible(true));
            } else if ("Báo Cáo Doanh Thu Theo Loại Hình Dịch Vụ".equals(title)) {
                mi.addActionListener(e -> new TuyChonBaoCaoLoaiHinhDichVuHopThoai(
                        SwingUtilities.getWindowAncestor(this),
                        readPort,
                        fromField.getText(),
                        toField.getText()).setVisible(true));
            } else if ("Báo Cáo Doanh Thu Theo Phòng Khám".equals(title)) {
                mi.addActionListener(e -> new TuyChonBaoCaoPhongKhamHopThoai(
                        SwingUtilities.getWindowAncestor(this),
                        readPort,
                        fromField.getText(),
                        toField.getText()).setVisible(true));
            } else if ("Báo Cáo Theo Dịch Vụ (019)".equals(title)) {
                mi.addActionListener(e -> new TuyChonBaoCaoTheoDichVuHopThoai(
                        SwingUtilities.getWindowAncestor(this),
                        readPort,
                        fromField.getText(),
                        toField.getText()).setVisible(true));
            } else if ("Báo Cáo Doanh Thu Theo Dịch Vụ Chi Tiết (019)".equals(title)) {
                mi.addActionListener(e -> new TuyChonBaoCaoDichVuChiTietHopThoai(
                        SwingUtilities.getWindowAncestor(this),
                        readPort,
                        fromField.getText(),
                        toField.getText()).setVisible(true));
            } else {
                mi.addActionListener(e -> exportReportCsv(title));
            }
            menu.add(mi);
        }
        return menu;
    }

    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Color.WHITE);
        left.setBorder(new MatteBorder(0, 0, 0, 1, GRID));

        allPatientsBtn.setFont(UngDungPhongChu.BODY_BOLD);
        allPatientsBtn.setForeground(Color.WHITE);
        allPatientsBtn.setBackground(new Color(0, 114, 198));
        allPatientsBtn.setOpaque(true);
        allPatientsBtn.setBorderPainted(false);
        allPatientsBtn.setHorizontalAlignment(SwingConstants.LEFT);
        allPatientsBtn.setPreferredSize(new Dimension(235, 28));
        allPatientsBtn.setBorder(new EmptyBorder(0, 8, 0, 8));
        allPatientsBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        allPatientsBtn.addActionListener(e -> {
            selectedCategory = "";
            selectedValue = "";
            filterTree.clearSelection();
            applyFilter();
        });
        left.add(allPatientsBtn, BorderLayout.NORTH);

        filterTree.setRootVisible(false);
        filterTree.setShowsRootHandles(true);
        filterTree.setFont(UngDungPhongChu.BODY);
        filterTree.setRowHeight(22);
        var renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        filterTree.setCellRenderer(renderer);
        filterTree.addTreeSelectionListener(e -> {
            if (changingTree) return;
            var node = (DefaultMutableTreeNode) filterTree.getLastSelectedPathComponent();
            if (node == null) return;
            if (node.getUserObject() instanceof TreeFilter tf) {
                selectedCategory = tf.category();
                selectedValue = tf.value();
            } else {
                selectedCategory = "";
                selectedValue = "";
            }
            applyFilter();
        });

        JScrollPane treeScroll = new JScrollPane(filterTree);
        treeScroll.setBorder(null);
        treeScroll.getViewport().setBackground(Color.WHITE);
        left.add(treeScroll, BorderLayout.CENTER);
        return left;
    }

    private JPanel buildSearchBar() {
        JPanel searchBar = new JPanel(new BorderLayout());
        searchBar.setPreferredSize(new Dimension(100, 28));
        searchField.setFont(UngDungPhongChu.BODY);
        searchField.setBackground(new Color(255, 255, 193));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(172, 172, 151)),
                new EmptyBorder(0, 6, 0, 4)));
        JButton clearBtn = new JButton("🔍");
        clearBtn.setFont(UngDungPhongChu.BODY);
        clearBtn.setPreferredSize(new Dimension(28, 26));
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            applyFilter();
        });
        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(clearBtn, BorderLayout.EAST);
        return searchBar;
    }

    private JTable buildTable() {
        JTable t = new JTable(model);
        t.setFont(UngDungPhongChu.BODY);
        t.setRowHeight(23);
        t.setShowGrid(true);
        t.setGridColor(GRID);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        t.getTableHeader().setFont(UngDungPhongChu.BODY);
        t.getTableHeader().setReorderingAllowed(false);

        for (int i = 0; i < COL_WIDTHS.length; i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(COL_WIDTHS[i]);
        }
        t.getColumnModel().getColumn(0).setMaxWidth(28);
        t.getColumnModel().getColumn(0).setMinWidth(24);

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel,
                                                                     boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                setBorder(new EmptyBorder(0, 6, 0, 6));
                if (col == 0) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setForeground(sel ? Color.WHITE : new Color(0, 145, 145));
                    setText("▶");
                } else if (col == 1 || col == 2 || col == 3 || col == 5 || col == 6) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col == 9 || col == 10) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                if (sel) {
                    setBackground(SELECTED);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(Color.WHITE);
                    if (col != 0) setForeground(Color.BLACK);
                }
                return this;
            }
        });
        return t;
    }

    private void loadRows() {
        int gen = ++loadGen;
        LocalDateTime fromDt = parseDateTime(fromField.getText(), LocalDate.now().minusDays(30).atStartOfDay());
        LocalDateTime toDt = parseDateTime(toField.getText(), LocalDate.now().atTime(23, 59, 59));
        boolean filterByDischarge = rbDischargeDate.isSelected();
        var ctx = KhamBenhPhienNguCanh.current();
        int facilityId = ctx == null ? 1 : ctx.facilityId();

        new SwingWorker<List<RevenueVisitRow>, Void>() {
            @Override protected List<RevenueVisitRow> doInBackground() {
                return readPort.listVisits(facilityId, filterByDischarge, fromDt, toDt);
            }

            @Override protected void done() {
                if (gen != loadGen || !isDisplayable()) return;
                try {
                    allRows = get();
                    applyFilter();
                } catch (Exception ex) {
                    allRows = List.of();
                    applyFilter();
                    JOptionPane.showMessageDialog(QuanLyDoanhThuManHinh.this,
                            "Không tải được dữ liệu doanh thu: " + ex.getMessage(),
                            "Quản Lý Doanh Thu", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void applyFilter() {
        String q = normalize(searchField.getText());
        displayedRows = allRows.stream()
                .filter(r -> matchesTree(r))
                .filter(r -> q.isEmpty() || normalize(
                        r.patientCode() + " " + r.patientName() + " " + r.department() + " " + r.room())
                        .contains(q))
                .toList();

        model.setRowCount(0);
        for (RevenueVisitRow r : displayedRows) {
            model.addRow(new Object[]{
                    "▶",
                    r.dischargedAt() == null ? (r.admittedAt() == null ? "" : r.admittedAt().format(DATE_FMT)) : r.dischargedAt().format(DATE_FMT),
                    r.admittedAt() == null ? "" : r.admittedAt().format(DATE_FMT),
                    Objects.toString(r.patientCode(), ""),
                    Objects.toString(r.patientName(), ""),
                    formatAge(r.birthDate()),
                    formatGender(r.gender()),
                    Objects.toString(r.department(), ""),
                    Objects.toString(r.room(), ""),
                    MONEY_FMT.format(r.totalCost() == null ? BigDecimal.ZERO : r.totalCost()),
                    MONEY_FMT.format(r.patientPaid() == null ? BigDecimal.ZERO : r.patientPaid())
            });
        }
        updateTree();
    }

    private boolean matchesTree(RevenueVisitRow r) {
        if (selectedCategory.isEmpty()) return true;
        return switch (selectedCategory) {
            case "AGE" -> isChild(r.birthDate()) ? "CHILD".equals(selectedValue) : "ADULT".equals(selectedValue);
            case "GENDER" -> formatGender(r.gender()).equalsIgnoreCase(selectedValue);
            case "SUBJECT" -> normalizeSubject(r.subjectType()).equals(selectedValue);
            case "FEE_TYPE" -> normalizeFeeType(r.treatmentType()).equals(selectedValue);
            case "STATUS" -> isCompleted(r.status()) ? "DONE".equals(selectedValue) : "ACTIVE".equals(selectedValue);
            default -> true;
        };
    }

    private void updateTree() {
        allPatientsBtn.setText("Tất cả bệnh nhân (" + allRows.size() + ")");
        changingTree = true;
        var root = new DefaultMutableTreeNode("root");

        int childCnt = (int) allRows.stream().filter(r -> isChild(r.birthDate())).count();
        int adultCnt = allRows.size() - childCnt;
        var ageGroup = new DefaultMutableTreeNode("Độ tuổi");
        if (childCnt > 0) ageGroup.add(new DefaultMutableTreeNode(new TreeFilter("AGE", "Trẻ em", "CHILD", childCnt)));
        if (adultCnt > 0) ageGroup.add(new DefaultMutableTreeNode(new TreeFilter("AGE", "Người lớn", "ADULT", adultCnt)));

        int maleCnt = (int) allRows.stream().filter(r -> "Nam".equalsIgnoreCase(formatGender(r.gender()))).count();
        int femaleCnt = (int) allRows.stream().filter(r -> "Nữ".equalsIgnoreCase(formatGender(r.gender()))).count();
        var genderGroup = new DefaultMutableTreeNode("Giới tính");
        genderGroup.add(new DefaultMutableTreeNode(new TreeFilter("GENDER", "Nam", "Nam", maleCnt)));
        genderGroup.add(new DefaultMutableTreeNode(new TreeFilter("GENDER", "Nữ", "Nữ", femaleCnt)));

        int thuPhiCnt = (int) allRows.stream().filter(r -> "Thu phí".equals(normalizeSubject(r.subjectType()))).count();
        int bhytCnt = allRows.size() - thuPhiCnt;
        var subjectGroup = new DefaultMutableTreeNode("Đối tượng");
        subjectGroup.add(new DefaultMutableTreeNode(new TreeFilter("SUBJECT", "Thu phí", "Thu phí", thuPhiCnt)));
        if (bhytCnt > 0) subjectGroup.add(new DefaultMutableTreeNode(new TreeFilter("SUBJECT", "BHYT", "BHYT", bhytCnt)));

        int outCnt = (int) allRows.stream().filter(r -> "Ngoại trú".equals(normalizeFeeType(r.treatmentType()))).count();
        int inCnt = allRows.size() - outCnt;
        var feeGroup = new DefaultMutableTreeNode("Loại viện phí");
        feeGroup.add(new DefaultMutableTreeNode(new TreeFilter("FEE_TYPE", "Ngoại trú", "Ngoại trú", outCnt)));
        if (inCnt > 0) feeGroup.add(new DefaultMutableTreeNode(new TreeFilter("FEE_TYPE", "Nội trú", "Nội trú", inCnt)));

        int doneCnt = (int) allRows.stream().filter(r -> isCompleted(r.status())).count();
        int activeCnt = allRows.size() - doneCnt;
        var statusGroup = new DefaultMutableTreeNode("Trạng thái");
        statusGroup.add(new DefaultMutableTreeNode(new TreeFilter("STATUS", "Đang điều trị", "ACTIVE", activeCnt)));
        statusGroup.add(new DefaultMutableTreeNode(new TreeFilter("STATUS", "Kết thúc điều trị", "DONE", doneCnt)));

        root.add(ageGroup);
        root.add(genderGroup);
        root.add(subjectGroup);
        root.add(feeGroup);
        root.add(statusGroup);
        filterTree.setModel(new DefaultTreeModel(root));
        for (int i = 0; i < filterTree.getRowCount(); i++) filterTree.expandRow(i);
        changingTree = false;
    }

    private void exportReportCsv(String reportTitle) {
        JFileChooser picker = new JFileChooser();
        picker.setSelectedFile(new File(normalize(reportTitle).replaceAll("[^a-z0-9]+", "-") + ".csv"));
        if (picker.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (var writer = new OutputStreamWriter(new FileOutputStream(picker.getSelectedFile()), StandardCharsets.UTF_8)) {
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
            JOptionPane.showMessageDialog(this, "Đã xuất \"" + reportTitle + "\".", reportTitle, JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không ghi được tệp CSV.", reportTitle, JOptionPane.ERROR_MESSAGE);
        }
    }

    private static LocalDateTime parseDateTime(String text, LocalDateTime fallback) {
        try {
            return LocalDateTime.parse(text == null ? "" : text.trim(), INPUT_FMT);
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }

    private static boolean isChild(LocalDate dob) {
        return dob != null && Period.between(dob, LocalDate.now()).getYears() < 16;
    }

    private static String formatAge(LocalDate dob) {
        if (dob == null) return "";
        Period p = Period.between(dob, LocalDate.now());
        int years = p.getYears();
        if (years >= 3) return String.valueOf(years);
        int months = years * 12 + p.getMonths();
        return Math.max(months, 1) + " tháng";
    }

    private static String formatGender(String g) {
        if (g == null) return "";
        if ("M".equalsIgnoreCase(g) || "NAM".equalsIgnoreCase(g)) return "Nam";
        if ("F".equalsIgnoreCase(g) || "NU".equalsIgnoreCase(g) || "NỮ".equalsIgnoreCase(g)) return "Nữ";
        return g;
    }

    private static String normalizeSubject(String s) {
        return s != null && s.toUpperCase(Locale.ROOT).contains("BHYT") ? "BHYT" : "Thu phí";
    }

    private static String normalizeFeeType(String s) {
        return s != null && s.toUpperCase(Locale.ROOT).contains("NOI_TRU") ? "Nội trú" : "Ngoại trú";
    }

    private static boolean isCompleted(String status) {
        if (status == null) return false;
        String u = status.toUpperCase(Locale.ROOT);
        return u.contains("HOAN_THANH") || u.contains("KET_THUC") || u.contains("DA_RA_VIEN");
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }

    private static JLabel smallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.SMALL);
        return l;
    }

    private static Icon reportIcon(int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);
                g.setColor(new Color(95, 165, 220));
                g.fillRoundRect(2, 7, size - 4, size - 10, 3, 3);
                g.setColor(Color.WHITE);
                g.fillRect(6, 2, size - 12, 7);
                g.fillRect(6, 13, size - 12, 8);
                g.setColor(new Color(55, 95, 140));
                g.drawRect(6, 2, size - 12, 7);
                g.drawRect(6, 13, size - 12, 8);
                g.dispose();
            }
        };
    }

    private static Icon searchIcon(int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);
                g.setColor(new Color(65, 135, 195));
                g.setStroke(new BasicStroke(2f));
                g.drawOval(2, 2, size - 9, size - 9);
                g.drawLine(size - 8, size - 8, size - 2, size - 2);
                g.dispose();
            }
        };
    }
}
