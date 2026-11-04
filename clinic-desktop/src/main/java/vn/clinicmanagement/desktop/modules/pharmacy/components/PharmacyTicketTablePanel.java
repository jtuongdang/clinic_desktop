package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Bảng dữ liệu danh sách phiếu kho dược / vật tư.
 * Khớp 100% chi tiết trong ảnh chụp mẫu:
 * - Thanh tìm kiếm vàng nhạt "Tìm kiếm (Ctrl+F)" và icon kính lúp
 * - 9 cột dữ liệu chuẩn HIS với vạch kẻ ô DataGridView đầy màn hình
 * - Cột "Trạng Thái Phiếu" có chấm tròn đỏ 🔴 và badge xanh ngọc "Đã nhập kho"
 * - 2 phím cuộn nhanh nổi bên phải (Nút tam giác xám tròn Lên / Xuống)
 */
public class PharmacyTicketTablePanel extends JPanel {

    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JScrollPane scrollPane;
    private final JTextField txtSearch;

    public PharmacyTicketTablePanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // --- 1. THANH TÌM KIẾM VÀNG NHẠT (Ctrl+F) ---
        JPanel searchBar = new JPanel(new BorderLayout());
        searchBar.setPreferredSize(new Dimension(800, 26));
        searchBar.setBackground(new Color(255, 254, 198)); // Nền vàng nhạt đặc trưng EHC
        searchBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(221, 217, 157)));

        txtSearch = new JTextField("Tìm kiếm (Ctrl+F)");
        txtSearch.setFont(AppFonts.BODY);
        txtSearch.setForeground(new Color(90, 90, 90));
        txtSearch.setOpaque(false);
        txtSearch.setBorder(new EmptyBorder(2, 8, 2, 8));

        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if ("Tìm kiếm (Ctrl+F)".equals(txtSearch.getText())) {
                    txtSearch.setText("");
                    txtSearch.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (txtSearch.getText().trim().isEmpty()) {
                    txtSearch.setText("Tìm kiếm (Ctrl+F)");
                    txtSearch.setForeground(new Color(90, 90, 90));
                }
            }
        });

        // Nút kính lúp bên phải thanh tìm kiếm
        JPanel searchIconBox = new JPanel(new GridBagLayout());
        searchIconBox.setOpaque(false);
        searchIconBox.setPreferredSize(new Dimension(28, 26));
        searchIconBox.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(221, 217, 157)));
        JLabel lblSearchIcon = new JLabel(PharmacyIcons.createSearchIcon());
        lblSearchIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchIconBox.add(lblSearchIcon);

        searchBar.add(txtSearch, BorderLayout.CENTER);
        searchBar.add(searchIconBox, BorderLayout.EAST);
        add(searchBar, BorderLayout.NORTH);

        // --- 2. BẢNG DỮ LIỆU 9 CỘT CHUẨN HIS ---
        String[] columns = {
                "Trạng Thái Phiếu",
                "Ngày Tạo",
                "Ngày Y Lệnh",
                "Mã Phiếu",
                "Mã Phiếu Th...",
                "Loại Phiếu",
                "Đối Tượng BN",
                "Khoa Lập Phiếu",
                "Nơi Lập Phiếu"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Vẽ vạch lưới kéo dài xuống tận đáy khung nhìn giống DataGridView / Excel
                int rowCount = getRowCount();
                int rowHeight = getRowHeight();
                int tableHeight = rowCount * rowHeight;
                int viewHeight = getHeight();

                if (tableHeight < viewHeight) {
                    g.setColor(getGridColor());
                    for (int y = tableHeight; y < viewHeight; y += rowHeight) {
                        g.drawLine(0, y, getWidth(), y);
                    }

                    int x = 0;
                    for (int col = 0; col < getColumnCount(); col++) {
                        x += getColumnModel().getColumn(col).getWidth();
                        g.drawLine(x - 1, tableHeight, x - 1, viewHeight);
                    }
                }
            }
        };

        table.setRowHeight(26);
        table.setFont(AppFonts.BODY);
        table.setShowGrid(true);
        table.setGridColor(new Color(226, 231, 237));
        table.setSelectionBackground(new Color(16, 104, 188));
        table.setSelectionForeground(Color.WHITE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Header bảng
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(100, 26));
        header.setFont(AppFonts.BODY_BOLD);
        header.setBackground(new Color(235, 240, 246));
        header.setForeground(new Color(30, 40, 50));
        header.setReorderingAllowed(false);

        // Đặt độ rộng các cột
        int[] widths = {145, 130, 110, 85, 95, 95, 105, 135, 220};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Custom renderer cho cột "Trạng Thái Phiếu": chấm đỏ 🔴 + pill "Đã nhập kho"
        table.getColumnModel().getColumn(0).setCellRenderer(new TicketStatusCellRenderer());

        // Renderer cho các cột ngày tháng, mã phiếu (căn giữa)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        // --- 3. KHUNG NỔI CHỨA 2 PHÍM CUỘN TRÒN LÊN / XUỐNG (FLOATING SCROLL) ---
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new LayoutManager() {
            @Override
            public void addLayoutComponent(String name, Component comp) {}
            @Override
            public void removeLayoutComponent(Component comp) {}
            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return scrollPane.getPreferredSize();
            }
            @Override
            public Dimension minimumLayoutSize(Container parent) {
                return scrollPane.getMinimumSize();
            }
            @Override
            public void layoutContainer(Container parent) {
                int w = parent.getWidth();
                int h = parent.getHeight();
                scrollPane.setBounds(0, 0, w, h);

                // Đặt cụm nút cuộn ở lề phải bảng (ngay tầm mắt giống ảnh mẫu)
                int btnW = 46;
                int btnH = 42;
                int btnX = Math.max(10, w - btnW - 12);
                int btnY = Math.max(10, (h / 2) - btnH - 10);

                floatingUpButton.setBounds(btnX, btnY, btnW, btnH);
                floatingDownButton.setBounds(btnX, btnY + btnH + 6, btnW, btnH);
            }
        });

        floatingUpButton = createFloatingScrollButton(PharmacyIcons.createChevronUpIcon(), true);
        floatingDownButton = createFloatingScrollButton(PharmacyIcons.createChevronDownIcon(), false);

        layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(floatingUpButton, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(floatingDownButton, JLayeredPane.PALETTE_LAYER);

        add(layeredPane, BorderLayout.CENTER);

        // Nạp dữ liệu mẫu khớp 100% ảnh chụp
        loadSampleData();
    }

    private final JButton floatingUpButton;
    private final JButton floatingDownButton;

    private JButton createFloatingScrollButton(Icon icon, boolean isUp) {
        JButton btn = new JButton(icon) {
            private boolean isHovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Nền bo góc xám đen chuẩn EHC
                g2.setColor(isHovered ? new Color(95, 105, 115, 230) : new Color(125, 135, 145, 215));
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 8, 8));
                g2.dispose();

                super.paintComponent(g);
            }
        };

        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            JScrollBar vbar = scrollPane.getVerticalScrollBar();
            if (vbar != null) {
                int amount = vbar.getBlockIncrement() * (isUp ? -1 : 1);
                vbar.setValue(vbar.getValue() + amount);
            }
        });

        return btn;
    }

    public void loadSampleData() {
        tableModel.setRowCount(0);

        // Hàng mẫu 1 từ ảnh chụp của người dùng:
        // Đã nhập kho | 09:56 14/09/2021 | 14/09/2021 | 000587 | | Nhập bù | | Khoa Hóa Sinh | Hóa chất khoa sinh hó...
        tableModel.addRow(new Object[]{
                "Đã nhập kho",
                "09:56 14/09/2021",
                "14/09/2021",
                "000587",
                "",
                "Nhập bù",
                "",
                "Khoa Hóa Sinh",
                "Hóa chất khoa sinh hó..."
        });
    }

    /**
     * CellRenderer đặc biệt cho cột "Trạng Thái Phiếu":
     * - Dấu chấm tròn đỏ đặc trưng
     * - Khối Badge nền xanh ngọc bo góc nhẹ bao quanh chữ "Đã nhập kho"
     */
    private static class TicketStatusCellRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private final JLabel lblText = new JLabel();
        private boolean isSelected = false;

        TicketStatusCellRenderer() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 6, 2));
            setOpaque(true);

            lblText.setFont(AppFonts.BODY_BOLD);
            lblText.setForeground(new Color(0, 79, 110)); // Màu chữ xanh lam sẫm
            add(lblText);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            this.isSelected = isSelected;
            String text = value != null ? value.toString() : "";
            lblText.setText(text);

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                lblText.setForeground(Color.WHITE);
            } else {
                setBackground(Color.WHITE);
                lblText.setForeground(new Color(0, 79, 110));
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Chấm tròn đỏ 🔴 bên trái
            g2.setColor(new Color(225, 30, 30));
            g2.fillOval(8, (getHeight() - 10) / 2, 10, 10);

            // 2. Vẽ badge bo góc xanh ngọc nhạt #C6F1FA bao chữ "Đã nhập kho" nếu hàng không bị chọn
            if (!isSelected && lblText.getText() != null && !lblText.getText().isEmpty()) {
                g2.setColor(new Color(198, 241, 250)); // Nền xanh ngọc nhạt EHC
                int badgeX = 24;
                int badgeY = 3;
                int badgeW = getWidth() - 30;
                int badgeH = getHeight() - 6;
                g2.fill(new RoundRectangle2D.Float(badgeX, badgeY, badgeW, badgeH, 4, 4));
            }

            g2.dispose();
        }
    }
}
