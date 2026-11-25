package vn.clinicmanagement.desktop.modules.reports.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.reports.catalog.BaoCaoDanhMucMoHinh;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import static vn.clinicmanagement.desktop.modules.reports.components.BaoCaoBieuTuong.Kind.*;

/** Chuẩn hóa đồng bộ mật độ, font 13pt, toolbar 58px và bảng màu với toàn hệ thống. */
public final class BaoCaoDanhMucBoCuc {
    private static final Color TOOLBAR_BG = new Color(235, 241, 247);
    private static final Color TOOLBAR_BORDER = new Color(189, 202, 215);
    private static final Color HEADER_BG = new Color(234, 239, 245);
    private static final Color GRID = new Color(221, 227, 234);
    private static final Color BLUE = new Color(0, 142, 255);
    private static final Color LINE = new Color(119, 133, 187);

    public final JButton menu = tool("Menu", MENU, 54),
            refresh = tool("Làm Mới", REFRESH, 68),
            add = tool("Thêm", ADD, 56),
            fetch = tool("Lấy Danh Sách Báo Cáo", FETCH, 150),
            extended = tool("Danh Mục Báo Cáo Mở Rộng", LIST, 174),
            up = inline("Lên", UP, 64),
            down = inline("Xuống", DOWN, 74),
            save = inline("Lưu Thứ Tự Sắp Xếp", SAVE, 160),
            search = new JButton(new BaoCaoBieuTuong(SEARCH, 18));
    public final JTextField keyword = new JTextField();
    public final JTree tree = new JTree(new DefaultMutableTreeNode("Tất Cả Báo Cáo (0)")) {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(LINE);
            g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{1, 2}, 0));
            Rectangle visible = getVisibleRect();
            for (int row = 1; row < getRowCount(); row++) {
                Rectangle r = getRowBounds(row);
                if (r == null || !r.intersects(visible)) continue;
                TreePath path = getPathForRow(row);
                if (path.getPathCount() == 3) {
                    int x = r.x - 16, center = r.y + r.height / 2;
                    var node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    g.drawLine(x, r.y, x, node.getNextSibling() == null ? center : r.y + r.height);
                    g.drawLine(x, center, r.x - 3, center);
                }
            }
            if (isRowSelected(0) && getRowCount() > 0) {
                Rectangle r = getRowBounds(0);
                if (r != null) {
                    g.setColor(BLUE);
                    g.fillRect(0, r.y, getWidth(), r.height);
                    g.setFont(UngDungPhongChu.BODY_BOLD);
                    g.setColor(Color.WHITE);
                    g.drawString(getModel().getRoot().toString(), 26, r.y + (r.height - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent());
                    g.setStroke(new BasicStroke(1));
                    g.drawLine(10, r.y + r.height / 2, 20, r.y + r.height / 2);
                }
            }
            g.dispose();
        }
    };
    public final JTable table;
    public final JSplitPane split;
    public final JLabel status = new JLabel(" ");
    public final JPanel footer = new JPanel(new BorderLayout());
    public final JPanel toolbar = new JPanel(new BorderLayout());
    public final JPanel searchBar = new JPanel(new BorderLayout());
    private final JScrollPane treeScroll, tableScroll;
    private final JPanel right = new JPanel(new BorderLayout()), catalogArea = new JPanel(new BorderLayout());
    private JSplitPane detailSplit;
    private JComponent detail;
    private int lastDetailDivider = -1;
    private final BaoCaoDanhMucMoHinh model;

    public BaoCaoDanhMucBoCuc(JPanel host, BaoCaoDanhMucMoHinh model) {
        this.model = model;
        host.setLayout(new BorderLayout());
        host.setBackground(Color.WHITE);
        host.setPreferredSize(new Dimension(1440, 760));

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        leftTools.setOpaque(false);
        for (JButton button : List.of(menu, refresh, add, fetch, extended)) leftTools.add(button);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 3));
        rightTools.setOpaque(false);
        rightTools.setBorder(new EmptyBorder(0, 0, 0, 6));
        for (JButton button : List.of(up, down, save)) rightTools.add(button);

        toolbar.setBackground(TOOLBAR_BG);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, TOOLBAR_BORDER));
        toolbar.setPreferredSize(new Dimension(0, 58));
        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(rightTools, BorderLayout.EAST);
        host.add(toolbar, BorderLayout.NORTH);

        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(24);
        tree.setFont(UngDungPhongChu.BODY);
        tree.setBorder(new EmptyBorder(4, 2, 4, 2));
        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.setUI(new BasicTreeUI() {
            @Override
            protected int getRowX(int row, int depth) {
                return 10 + depth * 22;
            }

            @Override
            protected void paintExpandControl(Graphics g, Rectangle clip, Insets insets, Rectangle bounds, TreePath path, int row, boolean expanded, boolean visited, boolean leaf) {
                if (leaf) return;
                int size = 10, x = bounds.x - 23, y = bounds.y + (bounds.height - size) / 2;
                g.setColor(Color.WHITE);
                g.fillRect(x, y, size, size);
                g.setColor(Color.GRAY);
                g.drawRect(x, y, size, size);
                g.drawLine(x + 2, y + size / 2, x + size - 2, y + size / 2);
                if (!expanded) g.drawLine(x + size / 2, y + 2, x + size / 2, y + size - 2);
            }
        });
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree t, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean focus) {
                super.getTreeCellRendererComponent(t, value, selected, expanded, leaf, row, false);
                setLeafIcon(null);
                setOpenIcon(null);
                setClosedIcon(null);
                setIcon(null);
                setFont(row == 0 || !leaf ? UngDungPhongChu.BODY_BOLD : UngDungPhongChu.BODY);
                setBackgroundSelectionColor(BLUE);
                setTextSelectionColor(Color.WHITE);
                setBorderSelectionColor(BLUE);
                return this;
            }
        });
        treeScroll = scroll(tree);
        treeScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        treeScroll.setMinimumSize(new Dimension(220, 0));

        table = new JTable(model) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D canvas = (Graphics2D) graphics.create();
                canvas.setColor(GRID);
                int start = getRowCount() * getRowHeight();
                for (int y = start; y < getHeight(); y += getRowHeight()) canvas.drawLine(0, y, getWidth(), y);
                int x = 0;
                for (int col = 0; col < getColumnCount(); col++) {
                    x += getColumnModel().getColumn(col).getWidth();
                    canvas.drawLine(x, start, x, getHeight());
                }
                canvas.dispose();
            }
        };
        table.setFont(UngDungPhongChu.BODY);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setGridColor(GRID);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(BLUE);
        table.setSelectionForeground(Color.WHITE);
        table.setDefaultRenderer(Object.class, new RowRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 26));
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setFont(UngDungPhongChu.BODY_BOLD);
                setBackground(HEADER_BG);
                setForeground(new Color(35, 43, 53));
                setHorizontalAlignment(c == 2 ? SwingConstants.CENTER : SwingConstants.LEFT);
                setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 1, GRID), new EmptyBorder(0, 8, 0, 4)));
                return this;
            }
        });
        tableScroll = scroll(table);
        tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setColumnHeaderView(table.getTableHeader());

        keyword.setFont(UngDungPhongChu.BODY);
        keyword.setBackground(new Color(255, 255, 204));
        keyword.setBorder(new EmptyBorder(0, 10, 0, 5));
        keyword.putClientProperty("JTextField.placeholderText", "Tìm kiếm (Ctrl+F)");
        search.setPreferredSize(new Dimension(28, 26));
        search.setBorder(new MatteBorder(0, 1, 0, 0, new Color(222, 220, 170)));
        search.setContentAreaFilled(false);
        search.setFocusPainted(false);

        searchBar.setBackground(new Color(255, 255, 204));
        searchBar.setBorder(new MatteBorder(0, 0, 1, 0, GRID));
        searchBar.setPreferredSize(new Dimension(0, 26));
        searchBar.add(keyword, BorderLayout.CENTER);
        searchBar.add(search, BorderLayout.EAST);

        right.setMinimumSize(new Dimension());
        catalogArea.setMinimumSize(new Dimension(0, 100));
        catalogArea.add(searchBar, BorderLayout.NORTH);
        catalogArea.add(tableScroll, BorderLayout.CENTER);
        right.add(catalogArea, BorderLayout.CENTER);

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, right);
        split.setDividerSize(2);
        split.setBorder(null);
        split.setContinuousLayout(true);
        split.setResizeWeight(0.1875);
        host.add(split, BorderLayout.CENTER);

        footer.setBackground(new Color(246, 248, 251));
        footer.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, new Color(211, 219, 228)), new EmptyBorder(0, 8, 0, 8)));
        footer.setPreferredSize(new Dimension(0, 24));
        status.setFont(UngDungPhongChu.SMALL);
        status.setVerticalAlignment(SwingConstants.CENTER);
        footer.add(status, BorderLayout.CENTER);
        host.add(footer, BorderLayout.SOUTH);

        save.setToolTipText("Lưu danh mục và thứ tự trên máy này");
    }

    public void showDetails(JComponent panel) {
        if (detail == panel && detailSplit != null) return;
        if (detailSplit != null) {
            lastDetailDivider = detailSplit.getDividerLocation();
            detailSplit.setTopComponent(null);
            detailSplit.setBottomComponent(null);
        }
        right.removeAll();
        detail = panel;
        panel.setMinimumSize(new Dimension(0, 210));
        detailSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, catalogArea, panel);
        detailSplit.setBorder(null);
        detailSplit.setDividerSize(2);
        detailSplit.setResizeWeight(0.36);
        detailSplit.setContinuousLayout(true);
        int initial = lastDetailDivider >= 0 ? lastDetailDivider : Math.max(140, Math.min(245, (int) (right.getHeight() * 0.36)));
        detailSplit.setDividerLocation(initial);
        right.add(detailSplit);
        right.revalidate();
        right.repaint();
        SwingUtilities.invokeLater(() -> {
            int selected = table.getSelectedRow();
            if (selected >= 0) table.scrollRectToVisible(table.getCellRect(selected, 0, true));
        });
    }

    public void hideDetails() {
        if (detailSplit == null) return;
        lastDetailDivider = detailSplit.getDividerLocation();
        detailSplit.setTopComponent(null);
        detailSplit.setBottomComponent(null);
        right.removeAll();
        right.add(catalogArea);
        detailSplit = null;
        detail = null;
        right.revalidate();
        right.repaint();
    }

    public boolean isDetailsVisible() { return detailSplit != null; }
    public JSplitPane getDetailSplit() { return detailSplit; }
    public JComponent getDetailComponent() { return detail; }
    public double scale() { return 1.0; }

    public void resize(JPanel host) {
        toolbar.setPreferredSize(new Dimension(0, 58));
        footer.setPreferredSize(new Dimension(0, 24));
        int sidebarWidth = Math.max(220, Math.min(280, (int) Math.round(host.getWidth() * 0.1875)));
        split.setDividerLocation(sidebarWidth);
        int tableWidth = Math.max(400, host.getWidth() - sidebarWidth - split.getDividerSize());
        int idWidth = 70;
        int fileWidth = Math.max(220, Math.min(380, (int) Math.round(tableWidth * 0.34)));
        int nameWidth = Math.max(200, tableWidth - idWidth - fileWidth);
        int[] widths = {nameWidth, fileWidth, idWidth};
        for (int i = 0; i < widths.length; i++) {
            var c = table.getColumnModel().getColumn(i);
            c.setMinWidth(i == 2 ? 50 : 120);
            c.setPreferredWidth(widths[i]);
            c.setWidth(widths[i]);
        }
    }

    private final class RowRenderer extends DefaultTableCellRenderer {
        private BaoCaoDanhMucMoHinh.Row row;
        private int column;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int r, int c) {
            super.getTableCellRendererComponent(t, value, selected, false, r, c);
            row = model.row(r);
            column = c;
            setFont(row.kind() == BaoCaoDanhMucMoHinh.Kind.REPORT ? UngDungPhongChu.BODY : UngDungPhongChu.BODY_BOLD);
            setBackground(selected ? t.getSelectionBackground() : r % 2 == 0 ? Color.WHITE : new Color(246, 248, 250));
            setForeground(selected ? Color.WHITE : Color.BLACK);
            setHorizontalAlignment(c == 2 ? SwingConstants.CENTER : SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, c == 0 ? 22 + row.depth() * 22 : 8, 0, 4));
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (column != 0 || row == null) return;
            Graphics2D g = (Graphics2D) graphics.create();
            int center = getHeight() / 2, x = 5 + row.depth() * 22;
            g.setColor(LINE);
            g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{1, 2}, 0));
            if (row.depth() > 0) {
                int parent = x - 16;
                g.drawLine(parent, 0, parent, center);
                g.drawLine(parent, center, x + 5, center);
            }
            if (row.kind() != BaoCaoDanhMucMoHinh.Kind.REPORT) {
                int size = 10;
                g.setStroke(new BasicStroke(1));
                g.setColor(Color.WHITE);
                g.fillRect(x, center - size / 2, size, size);
                g.setColor(row.kind() == BaoCaoDanhMucMoHinh.Kind.CATEGORY ? new Color(50, 90, 235) : new Color(90, 90, 90));
                g.drawRect(x, center - size / 2, size, size);
                g.drawLine(x + 2, center, x + size - 2, center);
                if (!model.expanded(row)) g.drawLine(x + size / 2, center - size / 2 + 2, x + size / 2, center + size / 2 - 2);
            }
            g.dispose();
        }
    }

    private static JButton tool(String text, BaoCaoBieuTuong.Kind kind, int width) {
        JButton b = new JButton(text, new BaoCaoBieuTuong(kind, 24));
        b.setFont(UngDungPhongChu.BODY);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setIconTextGap(3);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(3, 4, 3, 4));
        b.setPreferredSize(new Dimension(width, 54));
        return b;
    }

    private static JButton inline(String text, BaoCaoBieuTuong.Kind kind, int width) {
        JButton b = new JButton(text, new BaoCaoBieuTuong(kind, 22));
        b.setFont(UngDungPhongChu.BODY);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(3, 6, 3, 6));
        b.setPreferredSize(new Dimension(width, 52));
        return b;
    }

    private static JScrollPane scroll(Component view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(null);
        pane.getViewport().setBackground(Color.WHITE);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return pane;
    }
}
