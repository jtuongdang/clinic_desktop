package vn.clinicmanagement.desktop.modules.system;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.config.UIConstants;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Phân hệ Quản Lý Hệ Thống - Giao diện chuẩn EHC Hospital
 * Bao gồm menu phân cấp, thanh tìm kiếm (Ctrl+F) và danh sách cấu hình chi tiết
 */
public class SystemConfigPanel extends BaseViewPanel {

    private JTextField txtSearch;
    private JTable configTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JList<String> categoryList;

    public SystemConfigPanel() {
        super("system", "Quản Lý Hệ Thống", "⚙️");
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. Sub-Header: Nút Menu ☰ bên trái và Tiêu đề "QUẢN LÝ HỆ THỐNG" màu đỏ sẫm bên phải
        JPanel subHeader = new JPanel(new BorderLayout());
        subHeader.setBackground(Color.WHITE);
        subHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 220, 228)),
                new EmptyBorder(4, 8, 4, 15)
        ));

        // Nút Hamburger ☰
        JButton btnMenuToggle = new JButton("☰");
        btnMenuToggle.setFont(AppFonts.SECTION);
        btnMenuToggle.setForeground(new Color(45, 95, 145));
        btnMenuToggle.setBackground(new Color(245, 248, 252));
        btnMenuToggle.setFocusPainted(false);
        btnMenuToggle.setPreferredSize(new Dimension(36, 26));
        btnMenuToggle.setBorder(BorderFactory.createLineBorder(new Color(175, 195, 215), 1));
        btnMenuToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        subHeader.add(btnMenuToggle, BorderLayout.WEST);

        // Tiêu đề đỏ sẫm góc phải
        JLabel lblHeaderTitle = new JLabel("QUẢN LÝ HỆ THỐNG", SwingConstants.RIGHT);
        lblHeaderTitle.setFont(AppFonts.SECTION);
        lblHeaderTitle.setForeground(new Color(139, 20, 20)); // Dark red matching screenshot
        subHeader.add(lblHeaderTitle, BorderLayout.EAST);

        add(subHeader, BorderLayout.NORTH);

        // 2. Nội dung chính: SplitPane gồm Cột danh mục bên trái và Bảng chức năng bên phải
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(230);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);

        // --- CỘT TRÁI: ( TẤT CẢ CẤU HÌNH ) ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 225, 230)));

        JLabel lblCategoryTitle = new JLabel("( TẤT CẢ CẤU HÌNH )");
        lblCategoryTitle.setFont(AppFonts.BODY_BOLD);
        lblCategoryTitle.setForeground(new Color(30, 30, 30));
        lblCategoryTitle.setBorder(new EmptyBorder(8, 10, 6, 10));
        leftPanel.add(lblCategoryTitle, BorderLayout.NORTH);

        String[] categories = {
                "Cấu Hình Hệ Thống",
                "Quản Lý Hệ Thống",
                "Công Cụ Quản Trị Hệ Thống"
        };
        categoryList = new JList<>(categories);
        categoryList.setFont(AppFonts.BODY_BOLD);
        categoryList.setSelectedIndex(1); // Chọn mặc định "Quản Lý Hệ Thống" như ảnh
        categoryList.setSelectionBackground(new Color(33, 150, 243));
        categoryList.setSelectionForeground(Color.WHITE);
        categoryList.setFixedCellHeight(26);
        categoryList.setBorder(new EmptyBorder(2, 6, 2, 6));

        leftPanel.add(new JScrollPane(categoryList), BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);

        // --- CỘT PHẢI: Thanh tìm kiếm (Ctrl+F) + Danh sách chức năng ---
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.WHITE);

        // Thanh tìm kiếm màu vàng nhạt chuẩn EHC (#FFFFD0 / #FFFDE7)
        JPanel searchBar = new JPanel(new BorderLayout(5, 0));
        searchBar.setBackground(new Color(255, 255, 204));
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 180), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));

        txtSearch = new JTextField("Tìm kiếm (Ctrl+F)");
        txtSearch.setFont(AppFonts.BODY);
        txtSearch.setForeground(new Color(120, 120, 120));
        txtSearch.setBackground(new Color(255, 255, 204));
        txtSearch.setBorder(null);

        // Hiệu ứng placeholder cho ô tìm kiếm
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
                    txtSearch.setForeground(new Color(120, 120, 120));
                }
            }
        });

        JLabel lblSearchIcon = new JLabel("🔍");
        lblSearchIcon.setFont(AppFonts.BODY);
        lblSearchIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));

        searchBar.add(txtSearch, BorderLayout.CENTER);
        searchBar.add(lblSearchIcon, BorderLayout.EAST);

        rightPanel.add(searchBar, BorderLayout.NORTH);

        // Bảng danh sách chức năng (Tree-Table view)
        String[] columns = {"Chức Năng Quản Lý", "Người Phụ Trách / Ghi Chú"};
        Object[][] rawData = {
                {"Danh Sách Người Dùng",},
                {"Danh Sách Phiếu, Báo Cáo", ""},
                {"Danh Sách Mẫu In Barcode", ""},
                {"Danh Sách Nhóm Thông Báo", ""},
                {"Danh Sách Cơ Sở Y Tế Phân Tán", ""},
                {"Danh Sách Khoa", ""},
                {"Danh Sách Phòng", ""},
                {"Danh Sách Buồng Điều Trị", ""},
                {"Danh Sách Phòng Lưu Thuốc", ""},
                {"Danh Sách Isolator", ""},
                {"Danh Sách Khu Thực Hiện Xét Nghiệm", ""},
                {"Danh Sách Khu Thực Hiện Chẩn Đoán Hình Ảnh", ""},
                {"Danh Sách Giường", ""},
                {"Danh Sách Nơi Chuyển Đến", ""},
                {"Danh Sách Dịch Vụ Kỹ Thuật",},
                {"Danh Sách Thuốc "},
                {"Danh Sách Thuốc Bán Lẻ", ""},
                {"Danh Sách Thuốc Trúng Thầu", ""},
                {"Danh Sách Gói Bán Thuốc", ""},
                {"Danh Sách Thặng Dư Thuốc/Vật Tư", ""},
                {"Danh Sách Vật Tư", ""},
                {"Danh Sách Vật Tư Trúng Thầu", ""},
                {"Danh Sách Mẫu, Chữ Thăm Mẫu", ""},
                {"Danh Sách Suất Ăn", ""},
                {"Danh Sách Hàng Hóa", ""},
                {"Danh Sách Mẫu Chỉ Định Dịch Vụ", ""}
        };

        tableModel = new DefaultTableModel(rawData, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        configTable = new JTable(tableModel);
        configTable.setFont(AppFonts.BODY);
        configTable.setRowHeight(22);
        configTable.setShowGrid(false);
        configTable.setIntercellSpacing(new Dimension(0, 0));
        configTable.setTableHeader(null); // Không hiển thị header bảng như ảnh thực tế

        // Thiết lập bộ lọc tìm kiếm realtime
        rowSorter = new TableRowSorter<>(tableModel);
        configTable.setRowSorter(rowSorter);

        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = txtSearch.getText();
                if (text == null || text.trim().isEmpty() || "Tìm kiếm (Ctrl+F)".equals(text)) {
                    rowSorter.setRowFilter(null);
                } else {
                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text.trim(), 0, 1));
                }
            }
        });

        // Renderer cho cột 1: Thêm nét nối nhánh cây thư mục ├─
        configTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String valStr = value != null ? value.toString() : "";
                lbl.setText(" ├─ " + valStr);
                lbl.setBorder(new EmptyBorder(0, 4, 0, 4));

                if (isSelected) {
                    lbl.setBackground(new Color(33, 150, 243));
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(new Color(25, 25, 25));
                }
                return lbl;
            }
        });

        // Renderer cho cột 2: Thông tin người phụ trách
        configTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setBorder(new EmptyBorder(0, 8, 0, 8));
                if (isSelected) {
                    lbl.setBackground(new Color(33, 150, 243));
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(new Color(60, 60, 60));
                }
                return lbl;
            }
        });

        // Chọn mặc định dòng "Danh Sách Phòng" (hàng index 6) như trong ảnh chụp
        if (configTable.getRowCount() > 6) {
            configTable.setRowSelectionInterval(6, 6);
        }

        // Bắt sự kiện double-click để mở chi tiết cấu hình
        configTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = configTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        String itemName = configTable.getValueAt(selectedRow, 0).toString();
                        JOptionPane.showMessageDialog(SystemConfigPanel.this,
                                "Đang khởi tạo giao diện cấu hình:\n" + itemName,
                                "Quản Lý Hệ Thống", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(configTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        rightPanel.add(scrollPane, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {
        // Tải mới danh sách danh mục nếu có thay đổi
    }
}
