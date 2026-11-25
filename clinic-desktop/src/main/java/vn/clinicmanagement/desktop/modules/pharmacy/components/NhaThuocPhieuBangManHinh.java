package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocDanhMucCayManHinh.CategoryFilter;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.inventory.dispense.DispenseDonThuocCommand;
import vn.clinicmanagement.application.inventory.dispense.DispenseDonThuocBienNhan;
import vn.clinicmanagement.application.inventory.get.GetTonKhoKhaDungQuery;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi;
import vn.clinicmanagement.application.inventory.TonKhoKhaDungDuLieu;
import vn.clinicmanagement.application.prescription.approve.ApproveDonThuocCommand;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.pharmacy.NhaThuocGhepNoiGoc;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
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

    private vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi liveRead;
    private NhaThuocDanhSachReadCongKetNoi.NgayLoc dateFilterMode=NhaThuocDanhSachReadCongKetNoi.NgayLoc.TONG_HOP;
    private java.time.LocalDateTime fromDate=java.time.LocalDate.now().atStartOfDay(),toDate=java.time.LocalDate.now().plusDays(1).atStartOfDay();
    private String statusFilter="ALL";
    private boolean liveColumnsOrdered;
    public void filterStatus(String status){statusFilter=status;applyCurrentFilter();}
    private List<vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.Phieu> liveTickets=List.of();
    private final java.util.concurrent.atomic.AtomicLong loadSequence=new java.util.concurrent.atomic.AtomicLong();
    public void setLiveRead(vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi read){liveRead=read;}
    public void setDateRange(java.time.LocalDateTime from,java.time.LocalDateTime to){fromDate=from;toDate=to;}
    public void setDateFilterMode(NhaThuocDanhSachReadCongKetNoi.NgayLoc mode){dateFilterMode=mode!=null?mode:NhaThuocDanhSachReadCongKetNoi.NgayLoc.TONG_HOP;}
    public static class TicketRow {
        public final int id;
        public final String type; // "NHAP_KHO", "XUAT_KHO", "DON_THUOC"
        public final String status; // "Đã nhập kho", "Đã xuất kho", "Chờ cấp phát", "Đã cấp phát"
        public final String createdDate;
        public final String orderDate;
        public final String code;
        public final String refCode;
        public final String ticketTypeDisplay;
        public final String targetPatient;
        public final String department;
        public final String location;
        public final int warehouseId;
        public final Integer prescriptionId;
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

    private final NhaThuocGhepNoiGoc.Services services;
    private NhaThuocDanhMucCayManHinh categoryTreePanel;
    private CategoryFilter currentFilter = new CategoryFilter(null, CategoryFilter.ALL);

    private final List<TicketRow> allTickets = new ArrayList<>();
    private final List<TicketRow> displayedRows = new ArrayList<>();

    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JScrollPane scrollPane;
    private final JTextField txtSearch;
    private final JButton floatingUpButton;
    private final JButton floatingDownButton;

    public NhaThuocPhieuBangManHinh() {
        this(NhaThuocGhepNoiGoc.createDefault());
    }

    public NhaThuocPhieuBangManHinh(NhaThuocGhepNoiGoc.Services services) {this(services,null);}
    public NhaThuocPhieuBangManHinh(NhaThuocGhepNoiGoc.Services services,vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi read) {
        liveRead=read;
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
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer,int row,int column){
                Component cell=super.prepareRenderer(renderer,row,column);
                boolean cancelled=row<displayedRows.size()&&"Đã hủy".equals(displayedRows.get(row).status);
                var attributes=new java.util.HashMap<java.awt.font.TextAttribute,Object>(getFont().getAttributes());
                if(cancelled)attributes.put(java.awt.font.TextAttribute.STRIKETHROUGH,java.awt.font.TextAttribute.STRIKETHROUGH_ON);
                cell.setFont(getFont().deriveFont(attributes));if(!isRowSelected(row))cell.setForeground(cancelled?Color.RED:Color.BLACK);
                return cell;
            }
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

        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.setFont(UngDungPhongChu.BODY);
        table.setShowGrid(true);
        table.setGridColor(new Color(226, 231, 237));
        table.setSelectionBackground(new Color(16, 104, 188));
        table.setSelectionForeground(Color.WHITE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Đầu trang. bảng
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
                        showTicketDetailsDialog(tr);
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
        scrollPane.setColumnHeaderView(table.getTableHeader());
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
        // Cuộn bằng thanh cuộn chuẩn để không che dữ liệu phiếu.


        add(layeredPane, BorderLayout.CENTER);

        // Nạp dữ liệu
        loadData();
    }

    public NhaThuocGhepNoiGoc.Services getServices() {
        return services;
    }

    public void setCategoryTreePanel(NhaThuocDanhMucCayManHinh categoryTreePanel) {
        this.categoryTreePanel = categoryTreePanel;
        updateCategoryCounts();
    }

    public NhaThuocDanhMucCayManHinh getCategoryTreePanel() {
        return categoryTreePanel;
    }

    public CategoryFilter getCurrentFilter() {
        return currentFilter;
    }

    public JTable getTable() {
        return table;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JTextField getTxtSearch() {
        return txtSearch;
    }

    public List<TicketRow> getAllTickets() {
        return allTickets;
    }

    public List<TicketRow> getDisplayedRows() {
        return displayedRows;
    }

    public void applyFilter(CategoryFilter filter) {
        statusFilter="ALL";
        this.currentFilter = filter != null ? filter : new CategoryFilter(null, CategoryFilter.ALL);
        applyCurrentFilter();
    }

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

    public void filterByTicketCode(String code) {
        searchByKeyword(code);
    }

    public void loadSampleData() {
        loadData();
    }

    public void loadData() {
        if(liveRead!=null){loadLiveData();return;}
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

    private void loadLiveData(){
        long sequence=loadSequence.incrementAndGet();
        var auth=vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh.current();
        if(auth==null||!java.util.Set.of("ADMIN","PHARMACIST","WAREHOUSE_MANAGER").contains(auth.roleCode())){allTickets.clear();liveTickets=List.of();applyCurrentFilter();updateCategoryCounts();return;}
        var from=fromDate;var to=toDate;var mode=dateFilterMode;
        new SwingWorker<Object[],Void>(){
            protected Object[] doInBackground(){
                var tickets=liveRead.getTickets(auth.facilityId(),from,to,mode);
                var prescriptions=liveRead.getPrescriptions(auth.facilityId(),from,to,mode);
                return new Object[]{tickets,prescriptions};
            }
            @SuppressWarnings("unchecked")
            protected void done(){
                if(sequence!=loadSequence.get())return;
                var current=vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh.current();
                if(current==null||!auth.sessionId().equals(current.sessionId()))return;
                try{
                    Object[] result=get();
                    liveTickets=(List<vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.Phieu>)result[0];
                    var prescriptions=(List<vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DonThuocChoCap>)result[1];
                    allTickets.clear();var format=DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    for(var p:liveTickets){String status=p.cancelledAt()!=null||"DA_HUY".equals(p.status())?"Đã hủy":switch(Objects.toString(p.status(),"")){case "HOAN_THANH","DA_NHAP"->"Đã nhập kho";case "DA_XUAT"->"Đã xuất kho";case "CHO_DUYET"->"Chờ duyệt";case "TAM"->"Lưu tạm";default->Objects.toString(p.status(),"");};
                        allTickets.add(new TicketRow(p.id(),p.type(),status,p.createdAt()==null?"":p.createdAt().format(format),p.date()==null?"":p.date().format(format),p.code(),p.cancelledAt()==null?"":p.cancelledAt().format(format),typeName(p.detailType()),p.cancelledBy(),p.warehouse(),p.destination(),p.warehouseId(),null,false));
                    }
                    for(var d:prescriptions){
                        String st=switch(Objects.toString(d.status(),"")){
                            case "DA_DUYET","TAM"->d.paid()?"Đã thu - Chờ xuất":"Chờ thu tiền";
                            case "DA_XUAT_HET"->"Đã xuất kho";
                            case "HUY"->"Đã hủy";
                            default->Objects.toString(d.status(),"");
                        };
                        String target=d.patientName().isBlank()?"Đơn ngoại trú":d.patientName();
                        String dest=d.doctorName().isBlank()?d.visitCode():d.visitCode()+" - BS "+d.doctorName();
                        allTickets.add(new TicketRow(d.id(),"DON_THUOC",st,d.createdAt()==null?"":d.createdAt().format(format),d.date()==null?"":d.date().format(format),d.code(),"","Xuất bệnh nhân",target,d.warehouse(),dest,d.warehouseId()!=null&&d.warehouseId()>0?d.warehouseId():1,d.id(),true));
                    }
                    // Model remains compatible with dispensing actions; visible headings follow the warehouse ledger.
                    String[] headers={"Trạng thái","Ngày tạo","Ngày nhập xuất","Mã phiếu","Ngày hủy","Loại phiếu","Người hủy / BN","Kho thuốc","Kho đích / Bác sĩ"};
                    for(int i=0;i<table.getColumnCount();i++){var column=table.getColumnModel().getColumn(i);column.setHeaderValue(headers[column.getModelIndex()]);}
                    if(!liveColumnsOrdered){table.moveColumn(4,3);table.moveColumn(6,4);liveColumnsOrdered=true;
                        int[] widths={130,95,110,110,100,105,145,140,145};for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);}
                    table.getTableHeader().repaint();
                    if(categoryTreePanel!=null&&liveTickets.stream().map(p->p.warehouse()).distinct().count()==1)categoryTreePanel.setWarehouseTitle(liveTickets.get(0).warehouse());
                    applyCurrentFilter();updateCategoryCounts();
                }catch(Exception ex){allTickets.clear();liveTickets=List.of();applyCurrentFilter();updateCategoryCounts();if(isShowing())JOptionPane.showMessageDialog(NhaThuocPhieuBangManHinh.this,"Không thể tải phiếu từ CSDL. Vui lòng thử lại.","Lỗi tải dữ liệu",JOptionPane.ERROR_MESSAGE);}
            }
        }.execute();
    }
    private static boolean isPendingStatus(String status) {
        return "Chờ duyệt".equals(status) || "Lưu tạm".equals(status)
                || "Chờ thu tiền".equals(status) || "Đã thu - Chờ xuất".equals(status);
    }

    private static String typeName(String type){return switch(Objects.toString(type,"")){case "BAN_THUOC","XUAT_LE"->"Xuất lẻ";case "XUAT_DON","XUAT_DON_THUOC"->"Xuất bệnh nhân";case "TRA_NCC"->"Trả nhà cung cấp";case "CHUYEN_KHO"->"Chuyển kho";default->Objects.toString(type,"");};}
    private void updateLiveTotals(){
        if(liveRead==null||categoryTreePanel==null)return;
        java.math.BigDecimal imports=java.math.BigDecimal.ZERO,returns=java.math.BigDecimal.ZERO,retail=java.math.BigDecimal.ZERO;
        for(var p:liveTickets){if(p.cancelledAt()!=null||"DA_HUY".equals(p.status()))continue;
            if(currentFilter!=null&&currentFilter.warehouseId()!=null&&p.warehouseId()!=currentFilter.warehouseId())continue;
            if("NHAP_KHO".equals(p.type())&&("HOAN_THANH".equals(p.status())||"DA_NHAP".equals(p.status())))imports=imports.add(p.amount());
            if("DA_XUAT".equals(p.status())){if("TRA_NCC".equals(p.detailType()))returns=returns.add(p.amount());if("BAN_THUOC".equals(p.detailType())||"XUAT_LE".equals(p.detailType()))retail=retail.add(p.amount());}
        }
        categoryTreePanel.updateMoney(imports,returns,retail,java.math.BigDecimal.ZERO);
    }

    private void applyCurrentFilter() {
        updateLiveTotals();
        tableModel.setRowCount(0);
        displayedRows.clear();

        String rawKw = txtSearch.getText().trim();
        if ("Tìm kiếm (Ctrl+F)".equalsIgnoreCase(rawKw)) {
            rawKw = "";
        }
        String kw = rawKw.toLowerCase();

        for (TicketRow r : allTickets) {
            if("ACTIVE".equals(statusFilter)&&"Đã hủy".equals(r.status)||"CANCELLED".equals(statusFilter)&&!"Đã hủy".equals(r.status)
                    ||"EXPORTED".equals(statusFilter)&&!"Đã xuất kho".equals(r.status)
                    ||"PENDING".equals(statusFilter)&&!isPendingStatus(r.status))continue;
            // Lọc theo kho
            if (currentFilter != null && currentFilter.warehouseId() != null) {
                if (r.warehouseId != currentFilter.warehouseId()) {
                    continue;
                }
            }

            // Lọc theo loại phiếu
            if (currentFilter != null && currentFilter.ticketType() != null
                    && !CategoryFilter.ALL.equalsIgnoreCase(currentFilter.ticketType())) {
                boolean matches = switch(currentFilter.ticketType()) {
                    case CategoryFilter.XUAT_LE -> "Xuất lẻ".equals(r.ticketTypeDisplay);
                    case CategoryFilter.XUAT_BENH_NHAN -> "Xuất bệnh nhân".equals(r.ticketTypeDisplay);
                    default -> currentFilter.ticketType().equalsIgnoreCase(r.type);
                };
                if (!matches) continue;
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
            if(liveRead!=null){
                int retail=(int)allTickets.stream().filter(p->"Xuất lẻ".equals(p.ticketTypeDisplay)).count();
                int patient=(int)allTickets.stream().filter(p->"Xuất bệnh nhân".equals(p.ticketTypeDisplay)).count();
                categoryTreePanel.updateTicketCounts(allCount,importCount,retail,patient);
            }else categoryTreePanel.updateTicketCounts(allCount, importCount, exportCount, rxCount);
            if(liveRead!=null){int cancelled=(int)allTickets.stream().filter(p->"Đã hủy".equals(p.status)).count();
                int pending=(int)allTickets.stream().filter(p->isPendingStatus(p.status)).count();
                categoryTreePanel.updateStatusCounts(allCount-cancelled,cancelled,(int)allTickets.stream().filter(p->"Đã xuất kho".equals(p.status)).count(),pending);}
        }
    }

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

    public void dispensePrescription(TicketRow tr) {
        if (tr == null || !tr.isPrescription || tr.prescriptionId == null) {
            if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Chỉ có thể cấp phát cho các dòng đơn thuốc.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        if ("Đã cấp phát".equalsIgnoreCase(tr.status) || "Đã xuất kho".equalsIgnoreCase(tr.status)) {
            if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Đơn thuốc này đã được cấp phát trước đó.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }
        if ("Đã hủy".equalsIgnoreCase(tr.status)) {
            if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Không thể cấp phát đơn thuốc đã hủy.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        PhanQuyenNguCanh auth = currentAuthorizationContext();
        boolean isDraft = "Lưu tạm".equalsIgnoreCase(tr.status);
        boolean canApproveAndDispense = isDraft
                && services.approvePrescription() != null
                && auth != null && auth.isValid()
                && ("ADMIN".equalsIgnoreCase(auth.roleCode()) || "DOCTOR".equalsIgnoreCase(auth.roleCode()));

        if (isDraft && !canApproveAndDispense) {
            if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this,
                        "Đơn thuốc " + tr.code + " đang ở trạng thái Lưu tạm.\nCần Bác sĩ hoặc Quản trị viên duyệt đơn trước khi cấp phát.",
                        "Chưa thể cấp phát", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        if (isShowing() && !GraphicsEnvironment.isHeadless()) {
            String prompt = canApproveAndDispense
                    ? "Đơn " + tr.code + " đang ở trạng thái Lưu tạm.\nDuyệt đơn và cấp phát thuốc theo FEFO cho " + tr.targetPatient + " ngay?"
                    : "Xác nhận cấp phát thuốc theo FEFO cho đơn " + tr.code + " (Bệnh nhân: " + tr.targetPatient + ")?";
            int confirm = JOptionPane.showConfirmDialog(this,
                    prompt,
                    "Xác nhận cấp phát thuốc",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        int pharmacistId = auth != null && auth.isValid() ? auth.employeeId() : 1;
        int warehouseId = tr.warehouseId > 0 ? tr.warehouseId : 1;
        DispenseDonThuocCommand cmd = new DispenseDonThuocCommand(
                tr.prescriptionId,
                pharmacistId,
                warehouseId,
                auth,
                currentAuditActorContext()
        );

        SwingWorker<KetQua<DispenseDonThuocBienNhan>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<DispenseDonThuocBienNhan> doInBackground() {
                if (canApproveAndDispense) {
                    var approved = services.approvePrescription().handle(
                            new ApproveDonThuocCommand(tr.prescriptionId, pharmacistId, auth));
                    if (approved.isFailure()) {
                        var err = approved.getError().orElseThrow();
                        return KetQua.failure(err.code(), err.message());
                    }
                }
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
        PhanQuyenNguCanh auth = currentAuthorizationContext();
        KiemToanTacNhanNguCanh actor = currentAuditActorContext();

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                if (tr.isPrescription && tr.prescriptionId != null) {
                    var rxOpt = services.prescriptionRepository().getById(tr.prescriptionId);
                    if (rxOpt.isEmpty() || rxOpt.get().getChiTietDonThuocs() == null || rxOpt.get().getChiTietDonThuocs().isEmpty()) {
                        return "Không tìm thấy chi tiết thuốc trong đơn " + tr.code + ".";
                    }
                    StringBuilder sb = new StringBuilder("Tồn kho khả dụng cho " + tr.code + " (" + tr.department + "):\n\n");
                    for (ChiTietDonThuoc ct : rxOpt.get().getChiTietDonThuocs()) {
                        String name = ct.getItem() != null && ct.getItem().getTenItem() != null
                                ? ct.getItem().getTenItem()
                                : "Item #" + ct.getMaItem();
                        var res = services.checkStock().handle(new GetTonKhoKhaDungQuery(warehouseId, ct.getMaItem(), auth, actor));
                        if (res.isSuccess()) {
                            TonKhoKhaDungDuLieu dto = res.valueOrThrow();
                            var available = dto.tongTonKhaDung();
                            var needed = ct.getSoLuongKe() != null ? ct.getSoLuongKe() : java.math.BigDecimal.ZERO;
                            String enough = available.compareTo(needed) >= 0 ? "Đủ tồn" : "THIẾU";
                            sb.append("- ").append(name)
                                    .append(" | Kê: ").append(needed.stripTrailingZeros().toPlainString())
                                    .append(" | Tồn khả dụng: ").append(available.stripTrailingZeros().toPlainString())
                                    .append(" -> ").append(enough).append("\n");
                        } else {
                            sb.append("- ").append(name).append(": ")
                                    .append(res.getError().map(e -> e.message()).orElse("Không tra cứu được")).append("\n");
                        }
                    }
                    return sb.toString();
                }
                var res = services.checkStock().handle(new GetTonKhoKhaDungQuery(warehouseId, 1, auth, actor));
                if (res.isSuccess()) {
                    return "Tổng tồn khả dụng: " + res.valueOrThrow().tongTonKhaDung();
                }
                return "Không tra cứu được tồn kho: " + res.getError().map(e -> e.message()).orElse("Không tìm thấy");
            }

            @Override
            protected void done() {
                try {
                    String message = get();
                    if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                        JOptionPane.showMessageDialog(NhaThuocPhieuBangManHinh.this,
                                message,
                                "Thông tin tồn kho",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private PhanQuyenNguCanh currentAuthorizationContext() {
        if(liveRead!=null)return vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh.current();
        return new PhanQuyenNguCanh(1, 1, "PHARMACIST", 1, 1, UUID.randomUUID());
    }

    private KiemToanTacNhanNguCanh currentAuditActorContext() {
        PhanQuyenNguCanh auth = currentAuthorizationContext();
        if (auth != null && auth.isValid()) {
            return new KiemToanTacNhanNguCanh(
                    auth.employeeId(), auth.roleId(), auth.roleCode(),
                    auth.facilityId(), auth.departmentId(), "DESKTOP", auth.sessionId());
        }
        return new KiemToanTacNhanNguCanh(1, 1, "PHARMACIST", 1, 1, "WS01", UUID.randomUUID());
    }

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
            itemView.addActionListener(ev -> showTicketDetailsDialog(tr));
            menu.add(itemView);

            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void showTicketDetailsDialog(TicketRow tr) {
        if (tr == null || GraphicsEnvironment.isHeadless()) return;
        PhanQuyenNguCanh auth = currentAuthorizationContext();
        if (liveRead == null || auth == null || !auth.isValid()) {
            JOptionPane.showMessageDialog(this,
                    "Mã phiếu: " + tr.code + "\nTrạng thái: " + tr.status + "\nĐối tượng: " + tr.targetPatient,
                    "Chi Tiết Phiếu",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        var panel = new NhaThuocChiTietPhieuManHinh(tr, liveRead, services, this::loadData);
        JDialog dialog = new JDialog(owner, "Chi Tiết Phiếu - " + tr.code, Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(panel);
        dialog.pack();
        var gc = dialog.getGraphicsConfiguration();
        if (gc != null) {
            Rectangle area = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            dialog.setSize(
                    Math.min(dialog.getWidth(), area.width - insets.left - insets.right),
                    Math.min(dialog.getHeight(), area.height - insets.top - insets.bottom));
        }
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

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
    private static class TicketStatusCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int column){
            super.getTableCellRendererComponent(table,value,selected,focus,row,column);
            String text=Objects.toString(value,"");
            Color dot="Đã xuất kho".equals(text)?new Color(0,150,30)
                    :"Đã thu - Chờ xuất".equals(text)?new Color(0,120,215)
                    :"Chờ thu tiền".equals(text)||"Đã hủy".equals(text)?new Color(240,155,0)
                    :new Color(210,0,0);
            setIcon(new Icon(){public int getIconWidth(){return 14;}public int getIconHeight(){return 14;}
                public void paintIcon(Component c,Graphics g,int x,int y){g.setColor(dot);g.fillOval(x+2,y+2,10,10);}});
            setIconTextGap(4);setBorder(new EmptyBorder(0,2,0,2));
            return this;
        }
    }
}
