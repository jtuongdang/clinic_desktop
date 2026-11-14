package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.inventory.DispenseDonThuocCommand;
import vn.clinicmanagement.application.inventory.DispenseDonThuocBienNhan;
import vn.clinicmanagement.application.inventory.GetTonKhoKhaDungQuery;
import vn.clinicmanagement.application.inventory.TonKhoKhaDungDuLieu;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.pharmacy.NhaThuocGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocDanhMucCayManHinh.CategoryFilter;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Bảng dữ liệu danh sách phiếu kho dược / vật tư / đơn thuốc chờ cấp phát.
 * Khớp 100% giao diện HIS EHC:
 * - Thanh tìm kiếm vàng nhạt "Tìm kiếm (Ctrl+F)" và icon kính lúp.
 * - 9 cột dữ liệu chuẩn HIS với vạch kẻ ô DataGridView tràn màn hình.
 * - Cột "Trạng Thái Phiếu" có chấm tròn đỏ 🔴 và badge bo góc sinh động.
 * - 2 phím cuộn nhanh nổi bên phải (Nút tam giác xám tròn Lên / Xuống).
 * - Đấu nối Clean Architecture Handlers với SwingWorker không block EDT.
 */
public class NhaThuocPhieuBangManHinh extends JPanel {

    public static class TicketRow {
        /** Trường `id`: mã. */
        public final int id;
        /** Trường `type`: loai. */
        public final String type; // "NHAP_KHO", "XUAT_KHO", "DON_THUOC"
        /** Trường `status`: trạng thái. */
        public final String status; // "Đã nhập kho", "Đã xuất kho", "Chờ cấp phát", "Đã cấp phát"
        /** Trường `createdDate`: datao ngày. */
        public final String createdDate;
        /** Trường `orderDate`: yêu cầu ngày. */
        public final String orderDate;
        /** Trường `code`: mã. */
        public final String code;
        /** Trường `refCode`: `ref` mã. */
        public final String refCode;
        /** Trường `ticketTypeDisplay`: phiếu loai `Display`. */
        public final String ticketTypeDisplay;
        /** Trường `targetPatient`: `target` bệnh nhân. */
        public final String targetPatient;
        /** Trường `department`: khoa. */
        public final String department;
        public final String location;
        /** Trường `warehouseId`: kho mã. */
        public final int warehouseId;
        /** Trường `prescriptionId`: đơn thuốc mã. */
        public final Integer prescriptionId;
        /** Trường `isPrescription`: là đơn thuốc. */
        public final boolean isPrescription;

        public TicketRow(int id, String type, String status, String createdDate, String orderDate,
                         String code, String refCode, String ticketTypeDisplay, String targetPatient,
                         String department, String location, int warehouseId, Integer prescriptionId,
                         boolean isPrescription) {
            this.id = id;
            this.type = type;
            this.status = status;
            this.createdDate = createdDate;
            this.orderDate = orderDate;
            this.code = code;
            this.refCode = refCode;
            this.ticketTypeDisplay = ticketTypeDisplay;
            this.targetPatient = targetPatient;
            this.department = department;
            this.location = location;
            this.warehouseId = warehouseId;
            this.prescriptionId = prescriptionId;
            this.isPrescription = isPrescription;
        }
    }

    /** Trường `services`: dịch vụ. */
    private final NhaThuocGhepNoiGoc.Services services;
    /** Trường `categoryTreePanel`: danhmuc cay màn hình. */
    private NhaThuocDanhMucCayManHinh categoryTreePanel;
    /** Trường `currentFilter`: hientai `Filter`. */
    private CategoryFilter currentFilter = new CategoryFilter(null, CategoryFilter.ALL);

    private final List<TicketRow> allTickets = new ArrayList<>();
    /** Trường `displayedRows`: `displayed` đóng. */
    private final List<TicketRow> displayedRows = new ArrayList<>();

    /** Trường `table`: bảng. */
    private final JTable table;
    /** Trường `tableModel`: bảng mô hình. */
    private final DefaultTableModel tableModel;
    private final JScrollPane scrollPane;
    /** Trường `txtSearch`: `txt` tìm kiếm. */
    private final JTextField txtSearch;
    /** Trường `floatingUpButton`: `floating` tiep nút. */
    private final JButton floatingUpButton;
    /** Trường `floatingDownButton`: `floating` `Down` nút. */
    private final JButton floatingDownButton;

    public NhaThuocPhieuBangManHinh() {
        this(NhaThuocGhepNoiGoc.createDefault());
    }

    public NhaThuocPhieuBangManHinh(NhaThuocGhepNoiGoc.Services services) {
        this.services = Objects.requireNonNull(services, "services");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // --- 1. THANH TÌM KIẾM VÀNG NHẠT (Ctrl+F) ---
        JPanel searchBar = new JPanel(new BorderLayout());
        searchBar.setPreferredSize(new Dimension(800, 26));
        searchBar.setBackground(new Color(255, 254, 198)); // Nền vàng nhạt đặc trưng EHC
        searchBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(221, 217, 157)));

        txtSearch = new JTextField("Tìm kiếm (Ctrl+F)");
        txtSearch.setFont(UngDungPhongChu.BODY);
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

        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyCurrentFilter();
            }
        });

        // Nút kính lúp bên phải thanh tìm kiếm
        JPanel searchIconBox = new JPanel(new GridBagLayout());
        searchIconBox.setOpaque(false);
        searchIconBox.setPreferredSize(new Dimension(28, 26));
        searchIconBox.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(221, 217, 157)));
        JLabel lblSearchIcon = new JLabel(NhaThuocBieuTuong.createSearchIcon());
        lblSearchIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblSearchIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                applyCurrentFilter();
            }
        });
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
            /** Hàm `isCellEditable`: là `Cell` `Editable`. */
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel) {
            /** Hàm `paintComponent`: `paint` thành phần. */
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
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
        table.setFont(UngDungPhongChu.BODY);
        table.setShowGrid(true);
        table.setGridColor(new Color(226, 231, 237));
        table.setSelectionBackground(new Color(16, 104, 188));
        table.setSelectionForeground(Color.WHITE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Header bảng
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(100, 26));
        header.setFont(UngDungPhongChu.BODY_BOLD);
        header.setBackground(new Color(235, 240, 246));
        header.setForeground(new Color(30, 40, 50));
        header.setReorderingAllowed(false);

        // Đặt độ rộng các cột
        int[] widths = {145, 130, 110, 85, 95, 95, 105, 135, 220};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Custom renderer cho cột "Trạng Thái Phiếu"
        table.getColumnModel().getColumn(0).setCellRenderer(new TicketStatusCellRenderer());

        // Renderer cho các cột ngày tháng, mã phiếu (căn giữa)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // Context menu và tương tác nhấp đúp
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = table.getSelectedRow();
                    if (row >= 0 && row < displayedRows.size()) {
                        TicketRow tr = displayedRows.get(row);
                        if (tr.isPrescription) {
                            dispensePrescription(tr);
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowContextMenu(e);
            }
        });

        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        // --- 3. KHUNG NỔI CHỨA 2 PHÍM CUỘN TRÒN LÊN / XUỐNG (FLOATING SCROLL) ---
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new LayoutManager() {
            /** Hàm `addLayoutComponent`: thêm `Layout` thành phần. */
            @Override
            public void addLayoutComponent(String name, Component comp) {}
            /** Hàm `removeLayoutComponent`: `remove` `Layout` thành phần. */
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

                int btnW = 46;
                int btnH = 42;
                int btnX = Math.max(10, w - btnW - 12);
                int btnY = Math.max(10, (h / 2) - btnH - 10);

                floatingUpButton.setBounds(btnX, btnY, btnW, btnH);
                floatingDownButton.setBounds(btnX, btnY + btnH + 6, btnW, btnH);
            }
        });

        floatingUpButton = createFloatingScrollButton(NhaThuocBieuTuong.createChevronUpIcon(), true);
        floatingDownButton = createFloatingScrollButton(NhaThuocBieuTuong.createChevronDownIcon(), false);

        layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(floatingUpButton, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(floatingDownButton, JLayeredPane.PALETTE_LAYER);

        add(layeredPane, BorderLayout.CENTER);

        // Nạp dữ liệu
        loadData();
    }

    /** Hàm `getServices`: lấy dịch vụ. */
    public NhaThuocGhepNoiGoc.Services getServices() {
        return services;
    }

    /** Hàm `setCategoryTreePanel`: gán danhmuc cay màn hình. */
    public void setCategoryTreePanel(NhaThuocDanhMucCayManHinh categoryTreePanel) {
        this.categoryTreePanel = categoryTreePanel;
        updateCategoryCounts();
    }

    /** Hàm `getCategoryTreePanel`: lấy danhmuc cay màn hình. */
    public NhaThuocDanhMucCayManHinh getCategoryTreePanel() {
        return categoryTreePanel;
    }

    /** Hàm `getCurrentFilter`: lấy hientai `Filter`. */
    public CategoryFilter getCurrentFilter() {
        return currentFilter;
    }

    /** Hàm `getTable`: lấy bảng. */
    public JTable getTable() {
        return table;
    }

    /** Hàm `getTableModel`: lấy bảng mô hình. */
    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    /** Hàm `getTxtSearch`: lấy `Txt` tìm kiếm. */
    public JTextField getTxtSearch() {
        return txtSearch;
    }

    /** Hàm `getAllTickets`: lấy `All` `Tickets`. */
    public List<TicketRow> getAllTickets() {
        return allTickets;
    }

    /** Hàm `getDisplayedRows`: lấy `Displayed` đóng. */
    public List<TicketRow> getDisplayedRows() {
        return displayedRows;
    }

    public void applyFilter(CategoryFilter filter) {
        this.currentFilter = filter != null ? filter : new CategoryFilter(null, CategoryFilter.ALL);
        applyCurrentFilter();
    }

    /** Hàm `searchByKeyword`: tìm kiếm theo `Keyword`. */
    public void searchByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            txtSearch.setText("");
            txtSearch.setForeground(new Color(90, 90, 90));
        } else {
            txtSearch.setText(keyword.trim());
            txtSearch.setForeground(Color.BLACK);
        }
        applyCurrentFilter();
    }

    /** Hàm `filterByTicketCode`: `filter` theo phiếu mã. */
    public void filterByTicketCode(String code) {
        searchByKeyword(code);
    }

    /** Hàm `loadSampleData`: tải `Sample` dữ liệu. */
    public void loadSampleData() {
        loadData();
    }

    /** Hàm `loadData`: tải dữ liệu. */
    public void loadData() {
        SwingWorker<List<TicketRow>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TicketRow> doInBackground() {
                List<TicketRow> list = new ArrayList<>();
                boolean dbLoaded = false;

                if (!GraphicsEnvironment.isHeadless()) {
                    try (Connection conn = CoSoDuLieuKetNoi.getConnection()) {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

                        // 1. PhieuNhapKho
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT pnk.id, pnk.soPhieu, pnk.ngayNhap, pnk.trangThai, k.id AS khoId, k.tenKho " +
                                "FROM PhieuNhapKho pnk " +
                                "JOIN Kho k ON pnk.maKho = k.id " +
                                "WHERE pnk.isDeleted = 0 ORDER BY pnk.id DESC");
                             ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String rawStatus = rs.getString("trangThai");
                                String displayStatus = "Đã nhập kho";
                                if (rawStatus != null && !rawStatus.isBlank()) {
                                    if ("DA_NHAP".equalsIgnoreCase(rawStatus)) displayStatus = "Đã nhập kho";
                                    else displayStatus = rawStatus;
                                }
                                String created = rs.getTimestamp("ngayNhap") != null
                                        ? rs.getTimestamp("ngayNhap").toLocalDateTime().format(dtf) : "";
                                String orderDate = rs.getTimestamp("ngayNhap") != null
                                        ? rs.getTimestamp("ngayNhap").toLocalDateTime().toLocalDate().toString() : "";

                                list.add(new TicketRow(
                                        rs.getInt("id"),
                                        "NHAP_KHO",
                                        displayStatus,
                                        created,
                                        orderDate,
                                        rs.getString("soPhieu"),
                                        "",
                                        "Nhập bù",
                                        "Nội bộ",
                                        "Khoa Dược",
                                        rs.getString("tenKho"),
                                        rs.getInt("khoId"),
                                        null,
                                        false
                                ));
                                dbLoaded = true;
                            }
                        } catch (Exception ignored) {}

                        // 2. PhieuXuatKho
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT pxk.id, pxk.soPhieuXuat, pxk.ngayXuat, pxk.trangThai, pxk.loaiXuat, k.id AS khoId, k.tenKho " +
                                "FROM PhieuXuatKho pxk " +
                                "JOIN Kho k ON pxk.maKho = k.id " +
                                "WHERE pxk.isDeleted = 0 ORDER BY pxk.id DESC");
                             ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String rawStatus = rs.getString("trangThai");
                                String displayStatus = "Đã xuất kho";
                                if (rawStatus != null && !rawStatus.isBlank()) {
                                    if ("DA_XUAT".equalsIgnoreCase(rawStatus)) displayStatus = "Đã xuất kho";
                                    else displayStatus = rawStatus;
                                }
                                String created = rs.getTimestamp("ngayXuat") != null
                                        ? rs.getTimestamp("ngayXuat").toLocalDateTime().format(dtf) : "";
                                String orderDate = rs.getTimestamp("ngayXuat") != null
                                        ? rs.getTimestamp("ngayXuat").toLocalDateTime().toLocalDate().toString() : "";

                                list.add(new TicketRow(
                                        rs.getInt("id"),
                                        "XUAT_KHO",
                                        displayStatus,
                                        created,
                                        orderDate,
                                        rs.getString("soPhieuXuat"),
                                        "",
                                        "Xuất kho",
                                        "Nội bộ",
                                        "Khoa Dược",
                                        rs.getString("tenKho"),
                                        rs.getInt("khoId"),
                                        null,
                                        false
                                ));
                                dbLoaded = true;
                            }
                        } catch (Exception ignored) {}

                        // 3. DonThuoc
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT dt.id, dt.maDonThuoc, dt.ngayKe, dt.trangThai, dt.maLuotKham, bn.hoTen " +
                                "FROM DonThuoc dt " +
                                "JOIN LuotKham lk ON dt.maLuotKham = lk.id " +
                                "JOIN BenhNhan bn ON lk.maBenhNhan = bn.id " +
                                "WHERE dt.isDeleted = 0 ORDER BY dt.id DESC");
                             ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String rawStatus = rs.getString("trangThai");
                                String displayStatus = "Chờ cấp phát";
                                if ("DA_XUAT_HET".equalsIgnoreCase(rawStatus) || "DA_CAP_PHAT".equalsIgnoreCase(rawStatus)) {
                                    displayStatus = "Đã cấp phát";
                                }
                                String created = rs.getTimestamp("ngayKe") != null
                                        ? rs.getTimestamp("ngayKe").toLocalDateTime().format(dtf) : "";
                                String orderDate = rs.getTimestamp("ngayKe") != null
                                        ? rs.getTimestamp("ngayKe").toLocalDateTime().toLocalDate().toString() : "";

                                list.add(new TicketRow(
                                        rs.getInt("id"),
                                        "DON_THUOC",
                                        displayStatus,
                                        created,
                                        orderDate,
                                        rs.getString("maDonThuoc"),
                                        "LK-" + rs.getInt("maLuotKham"),
                                        "Đơn ngoại trú",
                                        rs.getString("hoTen"),
                                        "Khoa Khám Bệnh",
                                        "Nhà Thuốc Bệnh Viện",
                                        1,
                                        rs.getInt("id"),
                                        true
                                ));
                                dbLoaded = true;
                            }
                        } catch (Exception ignored) {}

                    } catch (Exception ignored) {}
                }

                // Fallback nếu CSDL chưa có dữ liệu hoặc offline
                if (!dbLoaded || list.isEmpty()) {
                    if (services != null && services.prescriptionRepository() != null) {
                        List<DonThuoc> rxs = services.prescriptionRepository().getAll();
                        for (DonThuoc dt : rxs) {
                            String st = (dt.getTrangThai() == TrangThaiDonThuoc.DA_DUYET) ? "Chờ cấp phát" : "Đã cấp phát";
                            list.add(new TicketRow(
                                    dt.getId() != null ? dt.getId() : 1,
                                    "DON_THUOC",
                                    st,
                                    "10:00 14/09/2021",
                                    "2021-09-14",
                                    "DT-00" + (dt.getId() != null ? dt.getId() : 1),
                                    "LK-" + dt.getMaLuotKham(),
                                    "Đơn ngoại trú",
                                    "Bệnh nhân ngoại trú",
                                    "Khoa Khám Bệnh",
                                    "Nhà Thuốc",
                                    1,
                                    dt.getId(),
                                    true
                            ));
                        }
                    }

                    if (list.isEmpty()) {
                        list.add(new TicketRow(
                                1,
                                "NHAP_KHO",
                                "Đã nhập kho",
                                "08:14 14/09/2021",
                                "2021-09-14",
                                "PNK0001",
                                "",
                                "Nhập bù",
                                "Nội bộ",
                                "Hóa chất khoa sinh hóa",
                                "Kho Hóa Chất",
                                1,
                                null,
                                false
                        ));
                    }
                }
                return list;
            }

            @Override
            protected void done() {
                try {
                    List<TicketRow> result = get();
                    allTickets.clear();
                    allTickets.addAll(result);
                    applyCurrentFilter();
                    updateCategoryCounts();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    /** Hàm `applyCurrentFilter`: `apply` hientai `Filter`. */
    private void applyCurrentFilter() {
        tableModel.setRowCount(0);
        displayedRows.clear();

        String rawKw = txtSearch.getText().trim();
        if ("Tìm kiếm (Ctrl+F)".equalsIgnoreCase(rawKw)) {
            rawKw = "";
        }
        String kw = rawKw.toLowerCase();

        for (TicketRow r : allTickets) {
            // Lọc theo kho
            if (currentFilter != null && currentFilter.warehouseId() != null) {
                if (r.warehouseId != currentFilter.warehouseId()) {
                    continue;
                }
            }

            // Lọc theo loại phiếu
            if (currentFilter != null && currentFilter.ticketType() != null
                    && !CategoryFilter.ALL.equalsIgnoreCase(currentFilter.ticketType())) {
                if (!currentFilter.ticketType().equalsIgnoreCase(r.type)) {
                    continue;
                }
            }

            // Lọc theo từ khóa tìm kiếm
            if (!kw.isEmpty()) {
                boolean match = (r.code != null && r.code.toLowerCase().contains(kw))
                        || (r.refCode != null && r.refCode.toLowerCase().contains(kw))
                        || (r.targetPatient != null && r.targetPatient.toLowerCase().contains(kw))
                        || (r.status != null && r.status.toLowerCase().contains(kw))
                        || (r.location != null && r.location.toLowerCase().contains(kw))
                        || (r.ticketTypeDisplay != null && r.ticketTypeDisplay.toLowerCase().contains(kw));
                if (!match) continue;
            }

            displayedRows.add(r);
            tableModel.addRow(new Object[]{
                    r.status,
                    r.createdDate,
                    r.orderDate,
                    r.code,
                    r.refCode,
                    r.ticketTypeDisplay,
                    r.targetPatient,
                    r.department,
                    r.location
            });
        }
    }

    /** Hàm `updateCategoryCounts`: cập nhật danhmuc `Counts`. */
    private void updateCategoryCounts() {
        int importCount = 0;
        int exportCount = 0;
        int rxCount = 0;

        for (TicketRow r : allTickets) {
            if ("NHAP_KHO".equalsIgnoreCase(r.type)) importCount++;
            else if ("XUAT_KHO".equalsIgnoreCase(r.type)) exportCount++;
            else if ("DON_THUOC".equalsIgnoreCase(r.type)) rxCount++;
        }
        int allCount = importCount + exportCount + rxCount;

        if (categoryTreePanel != null) {
            categoryTreePanel.updateTicketCounts(allCount, importCount, exportCount, rxCount);
        }
    }

    /** Hàm `dispenseSelectedPrescription`: cấp phát duocchon đơn thuốc. */
    public void dispenseSelectedPrescription() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayedRows.size()) {
            if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một đơn thuốc trong danh sách.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        dispensePrescription(displayedRows.get(row));
    }

    /** Hàm `dispensePrescription`: cấp phát đơn thuốc. */
    public void dispensePrescription(TicketRow tr) {
        if (tr == null || !tr.isPrescription || tr.prescriptionId == null) {
            if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Chỉ có thể cấp phát cho các dòng đơn thuốc.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        if ("Đã cấp phát".equalsIgnoreCase(tr.status)) {
            if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Đơn thuốc này đã được cấp phát trước đó.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }

        if (isShowing() && !GraphicsEnvironment.isHeadless()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Xác nhận cấp phát thuốc cho đơn " + tr.code + " (Bệnh nhân: " + tr.targetPatient + ")?",
                    "Xác nhận cấp phát thuốc",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        int pharmacistId = 1;
        int warehouseId = tr.warehouseId > 0 ? tr.warehouseId : 1;
        DispenseDonThuocCommand cmd = new DispenseDonThuocCommand(
                tr.prescriptionId,
                pharmacistId,
                warehouseId,
                currentAuthorizationContext(),
                currentAuditActorContext()
        );

        SwingWorker<KetQua<DispenseDonThuocBienNhan>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<DispenseDonThuocBienNhan> doInBackground() {
                return services.dispensePrescription().handle(cmd);
            }

            @Override
            protected void done() {
                try {
                    KetQua<DispenseDonThuocBienNhan> res = get();
                    if (res != null && res.isSuccess()) {
                        DispenseDonThuocBienNhan rc = res.getValue().orElse(null);
                        String info = (rc != null && rc.soPhieuXuat() != null)
                                ? " (Số phiếu xuất: " + rc.soPhieuXuat() + ")"
                                : "";
                        if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                            JOptionPane.showMessageDialog(NhaThuocPhieuBangManHinh.this,
                                     "Cấp phát đơn thuốc " + tr.code + " thành công!" + info,
                                     "Thành công",
                                     JOptionPane.INFORMATION_MESSAGE);
                        }
                        loadData();
                    } else {
                        String msg = (res != null && res.getError().isPresent())
                                ? res.getError().get().message()
                                : "Lỗi không xác định khi cấp phát thuốc.";
                        if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                            JOptionPane.showMessageDialog(NhaThuocPhieuBangManHinh.this,
                                     "Không thể cấp phát: " + msg,
                                     "Thông báo lỗi",
                                     JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                        JOptionPane.showMessageDialog(NhaThuocPhieuBangManHinh.this,
                                 "Lỗi hệ thống khi cấp phát: " + ex.getMessage(),
                                 "Lỗi",
                                 JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        worker.execute();
    }

    /** Hàm `checkStockForSelectedPrescription`: kiểm tra tồn kho cho duocchon đơn thuốc. */
    public void checkStockForSelectedPrescription() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayedRows.size()) {
            if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một dòng để kiểm tra tồn kho.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        TicketRow tr = displayedRows.get(row);
        int warehouseId = tr.warehouseId > 0 ? tr.warehouseId : 1;
        int itemId = 1; // Default item id check

        GetTonKhoKhaDungQuery query = new GetTonKhoKhaDungQuery(
                warehouseId,
                itemId,
                currentAuthorizationContext(),
                currentAuditActorContext()
        );

        SwingWorker<KetQua<TonKhoKhaDungDuLieu>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<TonKhoKhaDungDuLieu> doInBackground() {
                return services.checkStock().handle(query);
            }

            @Override
            protected void done() {
                try {
                    KetQua<TonKhoKhaDungDuLieu> res = get();
                    if (res != null && res.isSuccess()) {
                        TonKhoKhaDungDuLieu dto = res.getValue().orElse(null);
                        String stockInfo = dto != null
                                ? "Tổng tồn khả dụng: " + dto.tongTonKhaDung()
                                : "Có dữ liệu tồn kho khả dụng.";
                        if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                            JOptionPane.showMessageDialog(NhaThuocPhieuBangManHinh.this,
                                     stockInfo,
                                     "Thông tin tồn kho",
                                     JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        String msg = (res != null && res.getError().isPresent()) ? res.getError().get().message() : "Không tìm thấy";
                        if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                            JOptionPane.showMessageDialog(NhaThuocPhieuBangManHinh.this,
                                     "Không tra cứu được tồn kho: " + msg,
                                     "Thông báo",
                                     JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    /** Hàm `currentAuthorizationContext`: hientai phanquyen ngữ cảnh. */
    private PhanQuyenNguCanh currentAuthorizationContext() {
        return new PhanQuyenNguCanh(1, 1, "PHARMACIST", 1, 1, UUID.randomUUID());
    }

    /** Hàm `currentAuditActorContext`: hientai kiểm toán tacnhan ngữ cảnh. */
    private KiemToanTacNhanNguCanh currentAuditActorContext() {
        return new KiemToanTacNhanNguCanh(1, 1, "PHARMACIST", 1, 1, "WS01", UUID.randomUUID());
    }

    /** Hàm `maybeShowContextMenu`: `maybe` `Show` ngữ cảnh menu. */
    private void maybeShowContextMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        int row = table.rowAtPoint(e.getPoint());
        if (row >= 0 && row < displayedRows.size()) {
            table.setRowSelectionInterval(row, row);
            TicketRow tr = displayedRows.get(row);

            JPopupMenu menu = new JPopupMenu();
            if (tr.isPrescription) {
                JMenuItem itemDispense = new JMenuItem("💊 Cấp Phát Thuốc (FEFO)");
                itemDispense.addActionListener(ev -> dispensePrescription(tr));
                menu.add(itemDispense);

                JMenuItem itemStock = new JMenuItem("📦 Kiểm Tra Tồn Kho");
                itemStock.addActionListener(ev -> checkStockForSelectedPrescription());
                menu.add(itemStock);
                menu.addSeparator();
            }

            JMenuItem itemView = new JMenuItem("📄 Xem Chi Tiết Phiếu");
            itemView.addActionListener(ev -> JOptionPane.showMessageDialog(NhaThuocPhieuBangManHinh.this,
                    "Mã phiếu: " + tr.code + "\nTrạng thái: " + tr.status + "\nĐối tượng: " + tr.targetPatient,
                    "Chi Tiết Phiếu",
                    JOptionPane.INFORMATION_MESSAGE));
            menu.add(itemView);

            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /** Hàm `createFloatingScrollButton`: tạo `Floating` `Scroll` nút. */
    private JButton createFloatingScrollButton(Icon icon, boolean isUp) {
        JButton btn = new JButton(icon) {
            /** Trường `isHovered`: là `Hovered`. */
            private boolean isHovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                });
            }

            /** Hàm `paintComponent`: `paint` thành phần. */
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

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

    /**
     * CellRenderer đặc biệt cho cột "Trạng Thái Phiếu":
     * - Dấu chấm tròn đỏ đặc trưng
     * - Khối Badge nền bo góc nhẹ bao quanh chữ trạng thái
     */
    private static class TicketStatusCellRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        /** Trường `lblText`: `lbl` văn bản. */
        private final JLabel lblText = new JLabel();
        /** Trường `isSelected`: là duocchon. */
        private boolean isSelected = false;

        TicketStatusCellRenderer() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 6, 2));
            setOpaque(true);

            lblText.setFont(UngDungPhongChu.BODY_BOLD);
            lblText.setForeground(new Color(0, 79, 110));
            add(lblText);
        }

        /** Hàm `getTableCellRendererComponent`: lấy bảng `Cell` `Renderer` thành phần. Gốc khai báo tại `JPanel#getTableCellRendererComponent`. */
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

        /** Hàm `paintComponent`: `paint` thành phần. Gốc khai báo tại `JPanel#paintComponent`. */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(225, 30, 30));
            g2.fillOval(8, (getHeight() - 10) / 2, 10, 10);

            if (!isSelected && lblText.getText() != null && !lblText.getText().isEmpty()) {
                String st = lblText.getText().trim();
                Color badgeBg = new Color(198, 241, 250); // Xanh ngọc nhạt
                if ("Chờ cấp phát".equalsIgnoreCase(st)) {
                    badgeBg = new Color(255, 243, 205); // Vàng nhạt
                } else if ("Đã xuất kho".equalsIgnoreCase(st)) {
                    badgeBg = new Color(212, 237, 218); // Xanh lá nhạt
                } else if ("Đã cấp phát".equalsIgnoreCase(st)) {
                    badgeBg = new Color(209, 236, 241); // Xanh lam nhạt
                }

                g2.setColor(badgeBg);
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
