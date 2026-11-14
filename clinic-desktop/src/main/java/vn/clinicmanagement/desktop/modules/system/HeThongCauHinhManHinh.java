package vn.clinicmanagement.desktop.modules.system;

import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.repositories.IHeThongRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;
import vn.clinicmanagement.domain.system.ThamSoHeThong;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.system.HeThongRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Phân hệ Quản Lý Hệ Thống & Cấu Hình Tham Số / Ma Trận Phân Quyền (HD-INTEG-04).
 * Kết nối trực tiếp Clean Architecture qua {@link ISystemRepository}.
 */
public class HeThongCauHinhManHinh extends CoSoManHinhManHinh {

    /** Trường `systemRepository`: hệ thống kho dữ liệu. */
    private final IHeThongRepository systemRepository;
    private final AtomicInteger inFlightWorkers = new AtomicInteger(0);

    /** Trường `txtSearch`: `txt` tìm kiếm. */
    private JTextField txtSearch;
    /** Trường `configTable`: cauhinh bảng. */
    private JTable configTable;
    /** Trường `tableModel`: bảng mô hình. */
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    /** Trường `categoryList`: danhmuc `List`. */
    private JList<String> categoryList;
    /** Trường `parameterToolbar`: `parameter` thanhcongcu. */
    private JPanel parameterToolbar;
    /** Trường `btnSaveParameter`: `btn` lưu `Parameter`. */
    private JButton btnSaveParameter;
    private JButton btnEditParameter;
    /** Trường `btnRefreshParameter`: `btn` làm mới `Parameter`. */
    private JButton btnRefreshParameter;
    /** Trường `defaultHeader`: macdinh `Header`. */
    private JTableHeader defaultHeader;

    private static final Object[][] RAW_MANAGEMENT_DATA = {
            {"Danh Sách Người Dùng", ""},
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
            {"Danh Sách Dịch Vụ Kỹ Thuật", ""},
            {"Danh Sách Thuốc", ""},
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

    public HeThongCauHinhManHinh() {
        this(resolveDefaultSystemRepository());
    }

    public HeThongCauHinhManHinh(IHeThongRepository systemRepository) {
        super("system", "Quản Lý Hệ Thống", "⚙️");
        this.systemRepository = systemRepository != null ? systemRepository : resolveDefaultSystemRepository();
        buildUI();
    }

    /** Hàm `resolveDefaultSystemRepository`: `resolve` macdinh hệ thống kho dữ liệu. */
    private static IHeThongRepository resolveDefaultSystemRepository() {
        try {
            JdbcUnitOfWork uow = new JdbcUnitOfWork();
            JdbcGiaoDichQuanLy txMgr = new JdbcGiaoDichQuanLy(uow);
            JdbcKetNoiNhaCungCap provider = new JdbcKetNoiNhaCungCap(txMgr);
            return new HeThongRepository(provider, uow);
        } catch (Throwable ignored) {
            return new InMemorySystemRepository();
        }
    }

    /** Hàm `buildUI`: tạo giao diện. */
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

        JButton btnMenuToggle = new JButton("☰");
        btnMenuToggle.setFont(UngDungPhongChu.SECTION);
        btnMenuToggle.setForeground(new Color(45, 95, 145));
        btnMenuToggle.setBackground(new Color(245, 248, 252));
        btnMenuToggle.setFocusPainted(false);
        btnMenuToggle.setPreferredSize(new Dimension(36, 26));
        btnMenuToggle.setBorder(BorderFactory.createLineBorder(new Color(175, 195, 215), 1));
        btnMenuToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        subHeader.add(btnMenuToggle, BorderLayout.WEST);

        JLabel lblHeaderTitle = new JLabel("QUẢN LÝ HỆ THỐNG", SwingConstants.RIGHT);
        lblHeaderTitle.setFont(UngDungPhongChu.SECTION);
        lblHeaderTitle.setForeground(new Color(139, 20, 20));
        subHeader.add(lblHeaderTitle, BorderLayout.EAST);

        add(subHeader, BorderLayout.NORTH);

        // 2. Nội dung chính: SplitPane gồm Cột danh mục bên trái và Bảng bên phải
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(230);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);

        // --- CỘT TRÁI: ( TẤT CẢ CẤU HÌNH ) ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 225, 230)));

        JLabel lblCategoryTitle = new JLabel("( TẤT CẢ CẤU HÌNH )");
        lblCategoryTitle.setFont(UngDungPhongChu.BODY_BOLD);
        lblCategoryTitle.setForeground(new Color(30, 30, 30));
        lblCategoryTitle.setBorder(new EmptyBorder(8, 10, 6, 10));
        leftPanel.add(lblCategoryTitle, BorderLayout.NORTH);

        String[] categories = {
                "Cấu Hình Hệ Thống",
                "Quản Lý Hệ Thống",
                "Công Cụ Quản Trị Hệ Thống"
        };
        categoryList = new JList<>(categories);
        categoryList.setFont(UngDungPhongChu.BODY_BOLD);
        categoryList.setSelectionBackground(new Color(33, 150, 243));
        categoryList.setSelectionForeground(Color.WHITE);
        categoryList.setFixedCellHeight(26);
        categoryList.setBorder(new EmptyBorder(2, 6, 2, 6));

        leftPanel.add(new JScrollPane(categoryList), BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);

        // --- CỘT PHẢI: Thanh tìm kiếm + Bảng + Thanh công cụ ---
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.WHITE);

        // Thanh tìm kiếm màu vàng nhạt chuẩn EHC (#FFFFCC)
        JPanel searchBar = new JPanel(new BorderLayout(5, 0));
        searchBar.setBackground(new Color(255, 255, 204));
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 180), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));

        txtSearch = new JTextField("Tìm kiếm (Ctrl+F)");
        txtSearch.setFont(UngDungPhongChu.BODY);
        txtSearch.setForeground(new Color(120, 120, 120));
        txtSearch.setBackground(new Color(255, 255, 204));
        txtSearch.setBorder(null);

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
        lblSearchIcon.setFont(UngDungPhongChu.BODY);
        lblSearchIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));

        searchBar.add(txtSearch, BorderLayout.CENTER);
        searchBar.add(lblSearchIcon, BorderLayout.EAST);
        rightPanel.add(searchBar, BorderLayout.NORTH);

        // Khởi tạo bảng ban đầu
        tableModel = new DefaultTableModel();
        configTable = new JTable(tableModel);
        configTable.setFont(UngDungPhongChu.BODY);
        configTable.setRowHeight(24);
        defaultHeader = configTable.getTableHeader();
        defaultHeader.setFont(UngDungPhongChu.BODY_BOLD);
        defaultHeader.setBackground(new Color(240, 244, 250));

        rowSorter = new TableRowSorter<>(tableModel);
        configTable.setRowSorter(rowSorter);

        // Lắng nghe thay đổi tìm kiếm realtime
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            /** Hàm `insertUpdate`: chèn cập nhật. */
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            /** Hàm `removeUpdate`: `remove` cập nhật. */
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            /** Hàm `changedUpdate`: thaydoi cập nhật. */
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        // Double-click mở chi tiết cấu hình
        configTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = configTable.getSelectedRow();
                    if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
                        String itemName = Objects.toString(configTable.getValueAt(selectedRow, 0), "");
                        if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                            JOptionPane.showMessageDialog(HeThongCauHinhManHinh.this,
                                    "Đang khởi tạo giao diện cấu hình:\n" + itemName,
                                    "Quản Lý Hệ Thống", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(configTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        // Thanh công cụ tham số (chỉ hiển thị ở tab Cấu Hình Hệ Thống)
        parameterToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        parameterToolbar.setBackground(new Color(245, 247, 250));
        parameterToolbar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 230)));

        btnEditParameter = new JButton("✏️ Sửa Giá Trị");
        btnEditParameter.setFont(UngDungPhongChu.SMALL_BOLD);
        btnEditParameter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEditParameter.addActionListener(e -> onEditParameterClicked());

        btnSaveParameter = new JButton("💾 Lưu Cấu Hình");
        btnSaveParameter.setFont(UngDungPhongChu.SMALL_BOLD);
        btnSaveParameter.setBackground(new Color(16, 104, 188));
        btnSaveParameter.setForeground(Color.WHITE);
        btnSaveParameter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSaveParameter.addActionListener(e -> onSaveParameterClicked());

        btnRefreshParameter = new JButton("🔄 Tải Lại");
        btnRefreshParameter.setFont(UngDungPhongChu.SMALL);
        btnRefreshParameter.addActionListener(e -> refreshData());

        parameterToolbar.add(btnRefreshParameter);
        parameterToolbar.add(btnEditParameter);
        parameterToolbar.add(btnSaveParameter);
        rightPanel.add(parameterToolbar, BorderLayout.SOUTH);

        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);

        // Lắng nghe chuyển đổi danh mục
        categoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadCategoryView(categoryList.getSelectedIndex());
            }
        });

        // Mặc định chọn mục 1 ("Quản Lý Hệ Thống") như mẫu EHC ban đầu
        categoryList.setSelectedIndex(1);
    }

    private void applyFilter() {
        if (rowSorter == null) return;
        String text = txtSearch.getText();
        if (text == null || text.trim().isEmpty() || "Tìm kiếm (Ctrl+F)".equals(text)) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text.trim())));
        }
    }

    /**
     * Tải giao diện tương ứng theo danh mục được chọn (0: Tham số, 1: Cây chức năng, 2: Ma trận quyền).
     */
    public void loadCategoryView(int index) {
        if (index == 0) {
            loadSystemParametersView();
        } else if (index == 2) {
            loadPermissionMatrixView();
        } else {
            loadManagementItemsView();
        }
    }

    /** Hàm `loadSystemParametersView`: tải hệ thống tham số màn hình. */
    private void loadSystemParametersView() {
        parameterToolbar.setVisible(true);
        configTable.setTableHeader(defaultHeader);
        configTable.setShowGrid(true);
        configTable.setGridColor(new Color(230, 235, 240));

        String[] cols = {"Mã Tham Số", "Tên Tham Số", "Giá Trị Cấu Hình", "Mô Tả"};
        tableModel = new DefaultTableModel(cols, 0) {
            /** Hàm `isCellEditable`: là `Cell` `Editable`. */
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Cho phép sửa trực tiếp cột Giá trị cấu hình
            }
        };
        configTable.setModel(tableModel);
        rowSorter = new TableRowSorter<>(tableModel);
        configTable.setRowSorter(rowSorter);

        DefaultTableCellRenderer boldRenderer = new DefaultTableCellRenderer();
        boldRenderer.setFont(UngDungPhongChu.BODY_BOLD);
        configTable.getColumnModel().getColumn(0).setCellRenderer(boldRenderer);
        configTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer());

        inFlightWorkers.incrementAndGet();
        SwingWorker<List<ThamSoHeThong>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ThamSoHeThong> doInBackground() {
                try {
                    return systemRepository.getAllSystemParameters();
                } catch (Exception ex) {
                    return fallbackParameters();
                }
            }

            @Override
            protected void done() {
                try {
                    List<ThamSoHeThong> list = get();
                    if (list == null || list.isEmpty()) {
                        list = fallbackParameters();
                    }
                    tableModel.setRowCount(0);
                    for (ThamSoHeThong p : list) {
                        tableModel.addRow(new Object[]{
                                p.getMaThamSo(),
                                p.getTenThamSo(),
                                p.getGiaTri(),
                                p.getMoTa() != null ? p.getMoTa() : ""
                        });
                    }
                } catch (Exception ignored) {
                } finally {
                    inFlightWorkers.decrementAndGet();
                }
            }
        };
        worker.execute();
    }

    /** Hàm `loadManagementItemsView`: tải `Management` `Items` màn hình. */
    private void loadManagementItemsView() {
        parameterToolbar.setVisible(false);
        configTable.setTableHeader(null);
        configTable.setShowGrid(false);
        configTable.setIntercellSpacing(new Dimension(0, 0));

        String[] columns = {"Chức Năng Quản Lý", "Người Phụ Trách / Ghi Chú"};
        tableModel = new DefaultTableModel(RAW_MANAGEMENT_DATA, columns) {
            /** Hàm `isCellEditable`: là `Cell` `Editable`. */
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        configTable.setModel(tableModel);
        rowSorter = new TableRowSorter<>(tableModel);
        configTable.setRowSorter(rowSorter);

        // Renderer cho cột 1: Thêm nét nối nhánh cây thư mục ├─
        configTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            /** Hàm `getTableCellRendererComponent`: lấy bảng `Cell` `Renderer` thành phần. */
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

        // Renderer cho cột 2
        configTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            /** Hàm `getTableCellRendererComponent`: lấy bảng `Cell` `Renderer` thành phần. */
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

        if (configTable.getRowCount() > 6) {
            configTable.setRowSelectionInterval(6, 6);
        }
    }

    /** Hàm `loadPermissionMatrixView`: tải quyền matran màn hình. */
    private void loadPermissionMatrixView() {
        parameterToolbar.setVisible(false);
        configTable.setTableHeader(defaultHeader);
        configTable.setShowGrid(true);
        configTable.setGridColor(new Color(230, 235, 240));

        String[] cols = {"Phân Hệ", "Hành Động", "Phạm Vi Dữ Liệu", "Được Phép"};
        tableModel = new DefaultTableModel(cols, 0) {
            /** Hàm `isCellEditable`: là `Cell` `Editable`. */
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        configTable.setModel(tableModel);
        rowSorter = new TableRowSorter<>(tableModel);
        configTable.setRowSorter(rowSorter);

        configTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer());
        configTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer());

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        configTable.getColumnModel().getColumn(2).setCellRenderer(centerRender);
        configTable.getColumnModel().getColumn(3).setCellRenderer(centerRender);

        inFlightWorkers.incrementAndGet();
        SwingWorker<List<MaTranPhanQuyen>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<MaTranPhanQuyen> doInBackground() {
                try {
                    int roleId = 1;
                    try {
                        NguoiDungPhien session = NguoiDungPhien.getInstance();
                        if (session != null && session.getRoleId() > 0) roleId = session.getRoleId();
                    } catch (Exception ignored) {}
                    return systemRepository.getMatrixByRoleId(roleId);
                } catch (Exception ex) {
                    return fallbackPermissions();
                }
            }

            @Override
            protected void done() {
                try {
                    List<MaTranPhanQuyen> list = get();
                    if (list == null || list.isEmpty()) {
                        list = fallbackPermissions();
                    }
                    tableModel.setRowCount(0);
                    for (MaTranPhanQuyen m : list) {
                        tableModel.addRow(new Object[]{
                                m.getPhanHe(),
                                m.getHanhDong() != null ? m.getHanhDong().name() : "XEM",
                                m.getPhamViDuLieu() != null ? m.getPhamViDuLieu().name() : "KHOA_PHONG",
                                m.isDuocPhep() ? "Được phép" : "Bị cấm"
                        });
                    }
                } catch (Exception ignored) {
                } finally {
                    inFlightWorkers.decrementAndGet();
                }
            }
        };
        worker.execute();
    }

    /** Hàm `onEditParameterClicked`: tải `Edit` `Parameter` `Clicked`. */
    private void onEditParameterClicked() {
        int row = configTable.getSelectedRow();
        if (row < 0) {
            if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một tham số để chỉnh sửa.", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }
        String key = Objects.toString(configTable.getValueAt(row, 0), "");
        String currentVal = Objects.toString(configTable.getValueAt(row, 2), "");

        if (!GraphicsEnvironment.isHeadless()) {
            String newVal = JOptionPane.showInputDialog(this, "Nhập giá trị mới cho tham số " + key + ":", currentVal);
            if (newVal != null) {
                saveParameter(key, newVal.trim());
            }
        }
    }

    /** Hàm `onSaveParameterClicked`: tải lưu `Parameter` `Clicked`. */
    private void onSaveParameterClicked() {
        if (configTable.isEditing()) {
            configTable.getCellEditor().stopCellEditing();
        }
        int row = configTable.getSelectedRow();
        if (row >= 0 && row < tableModel.getRowCount()) {
            String key = Objects.toString(configTable.getValueAt(row, 0), "");
            String val = Objects.toString(configTable.getValueAt(row, 2), "");
            saveParameter(key, val);
        } else {
            // Lưu tất cả các dòng hiện có
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                String k = Objects.toString(tableModel.getValueAt(r, 0), "");
                String v = Objects.toString(tableModel.getValueAt(r, 2), "");
                saveParameter(k, v);
            }
        }
    }

    /**
     * Cập nhật tham số hệ thống qua Clean Architecture repository và cập nhật giao diện.
     */
    public void saveParameter(String key, String newValue) {
        if (key == null || key.isBlank()) return;
        int updatedBy = 1;
        try {
            NguoiDungPhien session = NguoiDungPhien.getInstance();
            if (session != null && session.getUserId() > 0) {
                updatedBy = session.getUserId();
            }
        } catch (Exception ignored) {}

        try {
            systemRepository.updateSystemParameter(key, newValue, updatedBy);
        } catch (Exception ignored) {}

        // Cập nhật giá trị hiển thị trên bảng
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (key.equals(tableModel.getValueAt(i, 0))) {
                tableModel.setValueAt(newValue, i, 2);
                break;
            }
        }

        if (isShowing() && !GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(this,
                    "Cập nhật tham số " + key + " thành công!",
                    "Cấu Hình Hệ Thống", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Hàm `fallbackParameters`: `fallback` tham số. */
    private List<ThamSoHeThong> fallbackParameters() {
        List<ThamSoHeThong> list = new ArrayList<>();
        list.add(createParam("CLINIC_NAME", "Phòng Khám Đa Khoa EHC", "Tên cơ sở phòng khám", "Tên đơn vị phòng khám"));
        list.add(createParam("MAX_QUEUE_PER_DAY", "150", "Số lượt khám tối đa / ngày", "Giới hạn tiếp nhận trong ngày"));
        list.add(createParam("SESSION_TIMEOUT_MINUTES", "30", "Thời gian hết hạn phiên (phút)", "Thời gian tự động khóa tài khoản"));
        list.add(createParam("DEFAULT_EXAM_PRICE", "150000", "Giá khám mặc định (VNĐ)", "Đơn giá khám ban đầu"));
        list.add(createParam("AUTO_PRINT_PRESCRIPTION", "TRUE", "Tự động in đơn thuốc", "In ngay sau khi kết thúc khám"));
        return list;
    }

    /** Hàm `createParam`: tạo `Param`. */
    private ThamSoHeThong createParam(String code, String val, String name, String desc) {
        ThamSoHeThong p = new ThamSoHeThong();
        p.setMaThamSo(code);
        p.setGiaTri(val);
        p.setTenThamSo(name);
        p.setMoTa(desc);
        return p;
    }

    /** Hàm `fallbackPermissions`: `fallback` quyền. */
    private List<MaTranPhanQuyen> fallbackPermissions() {
        List<MaTranPhanQuyen> list = new ArrayList<>();
        list.add(createPerm(1, "SYSTEM", HanhDongQuyen.DUYET, PhamViDuLieu.TOAN_HE_THONG, true));
        list.add(createPerm(1, "EXAMINATION", HanhDongQuyen.XEM, PhamViDuLieu.KHOA_PHONG, true));
        list.add(createPerm(1, "PHARMACY", HanhDongQuyen.SUA, PhamViDuLieu.KHOA_PHONG, true));
        list.add(createPerm(1, "PARACLINICAL", HanhDongQuyen.TAO, PhamViDuLieu.KHOA_PHONG, true));
        list.add(createPerm(1, "BILLING", HanhDongQuyen.XEM, PhamViDuLieu.TOAN_HE_THONG, true));
        return list;
    }

    /** Hàm `createPerm`: tạo `Perm`. */
    private MaTranPhanQuyen createPerm(int roleId, String phanHe, HanhDongQuyen action, PhamViDuLieu scope, boolean allowed) {
        MaTranPhanQuyen perm = new MaTranPhanQuyen();
        perm.setMaVaiTro(roleId);
        perm.setPhanHe(phanHe);
        perm.setHanhDong(action);
        perm.setPhamViDuLieu(scope);
        perm.setDuocPhep(allowed);
        return perm;
    }

    /** Hàm `onShow`: tải `Show`. Gốc khai báo tại `vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh#onShow`. */
    @Override
    public void onShow() {
        refreshData();
    }

    /** Hàm `refreshData`: làm mới dữ liệu. Gốc khai báo tại `vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh#refreshData`. */
    @Override
    public void refreshData() {
        if (categoryList != null) {
            loadCategoryView(categoryList.getSelectedIndex());
        }
    }

    // Getters phục vụ kiểm thử và tương tác ngoại vi
    public JTable getTable() { return configTable; }
    /** Hàm `getTableModel`: lấy bảng mô hình. */
    public DefaultTableModel getTableModel() { return tableModel; }
    /** Hàm `getCategoryList`: lấy danhmuc `List`. */
    public JList<String> getCategoryList() { return categoryList; }
    /** Hàm `getSearchField`: lấy tìm kiếm `Field`. */
    public JTextField getSearchField() { return txtSearch; }
    /** Hàm `getSystemRepository`: lấy hệ thống kho dữ liệu. */
    public IHeThongRepository getSystemRepository() { return systemRepository; }
    /** Hàm `getBtnSaveParameter`: lấy `Btn` lưu `Parameter`. */
    public JButton getBtnSaveParameter() { return btnSaveParameter; }
    /** Hàm `getBtnEditParameter`: lấy `Btn` `Edit` `Parameter`. */
    public JButton getBtnEditParameter() { return btnEditParameter; }
    /** Hàm `getInFlightWorkersCount`: lấy trong `Flight` `Workers` dem. */
    public int getInFlightWorkersCount() { return inFlightWorkers.get(); }

    /**
     * Triển khai InMemory phục vụ kiểm thử và chạy độc lập.
     */
    public static final class InMemorySystemRepository implements IHeThongRepository {
        /** Trường `parameters`: tham số. */
        private final Map<String, ThamSoHeThong> parameters = new ConcurrentHashMap<>();
        /** Trường `permissions`: quyền. */
        private final List<MaTranPhanQuyen> permissions = new ArrayList<>();

        public InMemorySystemRepository() {
            seedDefaults();
        }

        /** Hàm `seedDefaults`: `seed` macdinh. */
        private void seedDefaults() {
            addParam("CLINIC_NAME", "Phòng Khám Đa Khoa EHC", "Tên cơ sở phòng khám", "Tên đơn vị phòng khám");
            addParam("MAX_QUEUE_PER_DAY", "150", "Số lượt khám tối đa / ngày", "Giới hạn số lượng bệnh nhân tiếp nhận");
            addParam("SESSION_TIMEOUT_MINUTES", "30", "Thời gian hết hạn phiên (phút)", "Thời gian tự động khóa tài khoản");
            addParam("DEFAULT_EXAM_PRICE", "150000", "Giá khám mặc định (VNĐ)", "Đơn giá khám ban đầu");
            addParam("AUTO_PRINT_PRESCRIPTION", "TRUE", "Tự động in đơn thuốc", "In ngay sau khi bác sĩ hoàn tất khám");

            addPerm(1, "SYSTEM", HanhDongQuyen.DUYET, PhamViDuLieu.TOAN_HE_THONG, true);
            addPerm(1, "EXAMINATION", HanhDongQuyen.XEM, PhamViDuLieu.KHOA_PHONG, true);
            addPerm(1, "PHARMACY", HanhDongQuyen.SUA, PhamViDuLieu.KHOA_PHONG, true);
            addPerm(1, "PARACLINICAL", HanhDongQuyen.TAO, PhamViDuLieu.KHOA_PHONG, true);
            addPerm(1, "BILLING", HanhDongQuyen.XEM, PhamViDuLieu.TOAN_HE_THONG, true);
        }

        /** Hàm `addParam`: thêm `Param`. */
        public void addParam(String code, String value, String name, String desc) {
            ThamSoHeThong p = new ThamSoHeThong();
            p.setMaThamSo(code);
            p.setGiaTri(value);
            p.setTenThamSo(name);
            p.setMoTa(desc);
            parameters.put(code, p);
        }

        /** Hàm `addPerm`: thêm `Perm`. */
        public void addPerm(int roleId, String module, HanhDongQuyen action, PhamViDuLieu scope, boolean allowed) {
            MaTranPhanQuyen perm = new MaTranPhanQuyen();
            perm.setMaVaiTro(roleId);
            perm.setPhanHe(module);
            perm.setHanhDong(action);
            perm.setPhamViDuLieu(scope);
            perm.setDuocPhep(allowed);
            permissions.add(perm);
        }

        /** Hàm `getMatrixByRoleId`: lấy matran theo vai trò mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHeThongRepository#getMatrixByRoleId`. */
        @Override
        public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) {
            return permissions.stream().filter(p -> p.getMaVaiTro() == roleId).toList();
        }

        /** Hàm `getSystemParameterValue`: lấy hệ thống `Parameter` giá trị. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHeThongRepository#getSystemParameterValue`. */
        @Override
        public Optional<String> getSystemParameterValue(String key) {
            ThamSoHeThong p = parameters.get(key);
            return p != null ? Optional.ofNullable(p.getGiaTri()) : Optional.empty();
        }

        /** Hàm `getAllSystemParameters`: lấy `All` hệ thống tham số. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHeThongRepository#getAllSystemParameters`. */
        @Override
        public List<ThamSoHeThong> getAllSystemParameters() {
            return new ArrayList<>(parameters.values());
        }

        /** Hàm `updateSystemParameter`: cập nhật hệ thống `Parameter`. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHeThongRepository#updateSystemParameter`. */
        @Override
        public void updateSystemParameter(String key, String value, Integer updatedBy) {
            ThamSoHeThong p = parameters.get(key);
            if (p != null) {
                p.setGiaTri(value);
                p.setUpdatedBy(updatedBy);
            } else {
                addParam(key, value, key, "");
            }
        }
    }
}
