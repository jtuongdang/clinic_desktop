package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Cây danh mục kho / loại phiếu bên trái (Hóa chất khoa sinh hóa / Kho Dược).
 * Khớp 100% thiết kế giao diện chuẩn EHC HIS:
 * - Banner vàng nhạt header "Hóa chất khoa sinh hóa" / kho dược đang chọn.
 * - Các dòng danh mục: Tất Cả Phiếu, Phiếu Nhập Kho, Phiếu Xuất Kho, Đơn Thuốc Chờ Phát.
 * - Màu sắc EHC chuẩn: Nền chọn xanh dương #1068BC chữ trắng, vạch viền #C3CDD8, icon minus box.
 */
public class NhaThuocDanhMucCayManHinh extends JPanel {

    public static final String DEFAULT_WAREHOUSE_TITLE = "Hóa chất khoa sinh hóa";

    /**
     * Model / record filter định tuyến danh mục kho và loại phiếu.
     */
    public record CategoryFilter(Integer warehouseId, String ticketType) {
        public static final String ALL = "ALL";
        public static final String NHAP_KHO = "NHAP_KHO";
        public static final String XUAT_KHO = "XUAT_KHO";
        public static final String DON_THUOC = "DON_THUOC";
    }

    /** Trường `warehouseId`: kho mã. */
    private Integer warehouseId;
    /** Trường `warehouseTitle`: kho tiêu đề. */
    private String warehouseTitle = DEFAULT_WAREHOUSE_TITLE;

    /** Trường `selectedIndex`: duocchon `Index`. */
    private int selectedIndex = 0; // 0 = Tất Cả, 1 = Nhập Kho, 2 = Xuất Kho, 3 = Đơn Thuốc
    /** Trường `onCategorySelected`: tải danhmuc duocchon. */
    private Consumer<CategoryFilter> onCategorySelected;

    /** Trường `allCount`: `all` dem. */
    private int allCount;
    /** Trường `importCount`: `import` dem. */
    private int importCount;
    /** Trường `exportCount`: xuất dem. */
    private int exportCount;
    /** Trường `prescriptionCount`: đơn thuốc dem. */
    private int prescriptionCount;

    /** Trường `lblTitle`: `lbl` tiêu đề. */
    private final JLabel lblTitle;
    private final JPanel rowAllTickets;
    private final JPanel rowImport;
    /** Trường `rowExport`: `row` xuất. */
    private final JPanel rowExport;
    /** Trường `rowPrescription`: `row` đơn thuốc. */
    private final JPanel rowPrescription;

    private final JLabel lblAll;
    private final JLabel lblImport;
    /** Trường `lblExport`: `lbl` xuất. */
    private final JLabel lblExport;
    /** Trường `lblPrescription`: `lbl` đơn thuốc. */
    private final JLabel lblPrescription;

    public NhaThuocDanhMucCayManHinh() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(215, 600));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(195, 205, 216)));

        // --- 1. BANNER TIÊU ĐỀ VÀNG VÀ CHỮ ĐỎ SẪM ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(215, 26));
        headerPanel.setBackground(new Color(255, 242, 178)); // Nền vàng nhạt chuẩn EHC
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(223, 207, 139)));

        lblTitle = new JLabel(warehouseTitle);
        lblTitle.setFont(UngDungPhongChu.BODY_BOLD);
        lblTitle.setForeground(new Color(138, 30, 0)); // Chữ đỏ cam sẫm chuẩn EHC
        lblTitle.setBorder(new EmptyBorder(0, 8, 0, 0));

        // Nút thu gọn nhỏ bên phải
        JLabel btnCollapse = new JLabel("<");
        btnCollapse.setFont(UngDungPhongChu.SMALL_BOLD);
        btnCollapse.setForeground(new Color(138, 30, 0));
        btnCollapse.setBorder(new EmptyBorder(0, 0, 0, 6));
        btnCollapse.setCursor(new Cursor(Cursor.HAND_CURSOR));

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(btnCollapse, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- 2. CÂY THƯ MỤC CÁC LOẠI PHIẾU ---
        JPanel treeContent = new JPanel();
        treeContent.setLayout(new BoxLayout(treeContent, BoxLayout.Y_AXIS));
        treeContent.setBackground(Color.WHITE);

        // Row 0: Tất Cả Phiếu - Mặc định ĐANG CHỌN (Solid Blue, White Text)
        lblAll = new JLabel("Tất Cả Phiếu (1)");
        lblAll.setFont(UngDungPhongChu.BODY_BOLD);
        lblAll.setForeground(Color.WHITE);
        lblAll.setBorder(new EmptyBorder(0, 12, 0, 0));

        rowAllTickets = new JPanel(new BorderLayout()) {
            /** Hàm `paintComponent`: `paint` thành phần. */
            @Override
            protected void paintComponent(Graphics g) {
                if (selectedIndex == 0) {
                    g.setColor(new Color(16, 104, 188)); // Xanh dương đậm chọn hàng #1068BC
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    super.paintComponent(g);
                }
            }
        };
        rowAllTickets.setOpaque(false);
        rowAllTickets.setBackground(Color.WHITE);
        rowAllTickets.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        rowAllTickets.setPreferredSize(new Dimension(215, 24));
        rowAllTickets.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rowAllTickets.add(lblAll, BorderLayout.WEST);
        rowAllTickets.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectRow(0);
            }
        });

        // Nút nhánh cha "[-] Loại Phiếu"
        JPanel rowCategoryNode = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        rowCategoryNode.setBackground(Color.WHITE);
        rowCategoryNode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        rowCategoryNode.setPreferredSize(new Dimension(215, 24));
        rowCategoryNode.setBorder(new EmptyBorder(0, 8, 0, 0));

        JLabel lblMinus = new JLabel(NhaThuocBieuTuong.createMinusBoxIcon());
        JLabel lblCategory = new JLabel("Loại Phiếu");
        lblCategory.setFont(UngDungPhongChu.BODY);
        lblCategory.setForeground(new Color(30, 30, 30));

        rowCategoryNode.add(lblMinus);
        rowCategoryNode.add(lblCategory);

        // Các nhánh con
        lblImport = new JLabel("Phiếu Nhập Kho (1)");
        rowImport = createTreeChildRow(1, lblImport, false);

        lblExport = new JLabel("Phiếu Xuất Kho (0)");
        rowExport = createTreeChildRow(2, lblExport, false);

        lblPrescription = new JLabel("Đơn Thuốc Chờ Phát (0)");
        rowPrescription = createTreeChildRow(3, lblPrescription, true);

        treeContent.add(rowAllTickets);
        treeContent.add(rowCategoryNode);
        treeContent.add(rowImport);
        treeContent.add(rowExport);
        treeContent.add(rowPrescription);

        // Filler trống phía dưới
        JPanel filler = new JPanel();
        filler.setBackground(Color.WHITE);
        treeContent.add(filler);

        add(treeContent, BorderLayout.CENTER);

        // Khởi tạo số lượng đếm ban đầu
        updateTicketCounts(1, 1, 0, 0);
    }

    /** Hàm `createTreeChildRow`: tạo cay `Child` `Row`. */
    private JPanel createTreeChildRow(int index, JLabel label, boolean isLast) {
        JPanel row = new JPanel(new BorderLayout()) {
            /** Hàm `paintComponent`: `paint` thành phần. */
            @Override
            protected void paintComponent(Graphics g) {
                if (selectedIndex == index) {
                    g.setColor(new Color(16, 104, 188));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    super.paintComponent(g);
                    // Vẽ đường nối cây thư mục
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(180, 185, 195));
                    int yEnd = isLast ? 12 : getHeight();
                    g2.drawLine(14, 0, 14, yEnd);
                    g2.drawLine(14, 12, 28, 12);
                    g2.dispose();
                }
            }
        };
        row.setOpaque(selectedIndex != index);
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setPreferredSize(new Dimension(215, 24));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        label.setFont(UngDungPhongChu.BODY);
        label.setForeground(selectedIndex == index ? Color.WHITE : Color.BLACK);
        label.setBorder(new EmptyBorder(0, 32, 0, 0));
        row.add(label, BorderLayout.WEST);

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectRow(index);
            }
        });

        return row;
    }

    /**
     * Cập nhật số lượng badge của từng danh mục.
     */
    public void updateTicketCounts(int allCount, int importCount, int exportCount, int prescriptionCount) {
        this.allCount = allCount;
        this.importCount = importCount;
        this.exportCount = exportCount;
        this.prescriptionCount = prescriptionCount;

        lblAll.setText(String.format("Tất Cả Phiếu (%d)", allCount));
        lblImport.setText(String.format("Phiếu Nhập Kho (%d)", importCount));
        lblExport.setText(String.format("Phiếu Xuất Kho (%d)", exportCount));
        lblPrescription.setText(String.format("Đơn Thuốc Chờ Phát (%d)", prescriptionCount));
    }

    /**
     * Thiết lập tiêu đề kho đang chọn hiển thị trên banner vàng.
     */
    public void setWarehouseTitle(String title) {
        this.warehouseTitle = (title != null && !title.isBlank()) ? title : DEFAULT_WAREHOUSE_TITLE;
        lblTitle.setText(this.warehouseTitle);
    }

    /** Hàm `getWarehouseTitle`: lấy kho tiêu đề. */
    public String getWarehouseTitle() {
        return lblTitle.getText();
    }

    /** Hàm `setWarehouseId`: gán kho mã. */
    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }

    /** Hàm `getWarehouseId`: lấy kho mã. */
    public Integer getWarehouseId() {
        return warehouseId;
    }

    /** Hàm `setWarehouse`: gán kho. */
    public void setWarehouse(Integer warehouseId, String title) {
        this.warehouseId = warehouseId;
        setWarehouseTitle(title);
    }

    /**
     * Đăng ký listener nhận sự kiện khi chọn danh mục phiếu.
     */
    public void setOnCategorySelected(Consumer<CategoryFilter> listener) {
        this.onCategorySelected = listener;
    }

    /** Hàm `getOnCategorySelected`: lấy tải danhmuc duocchon. */
    public Consumer<CategoryFilter> getOnCategorySelected() {
        return onCategorySelected;
    }

    /**
     * Chọn dòng theo chỉ số: 0 = ALL, 1 = NHAP_KHO, 2 = XUAT_KHO, 3 = DON_THUOC.
     */
    public void selectRow(int index) {
        if (index < 0 || index > 3) {
            return;
        }
        this.selectedIndex = index;

        rowAllTickets.setOpaque(selectedIndex != 0);
        lblAll.setForeground(selectedIndex == 0 ? Color.WHITE : Color.BLACK);

        rowImport.setOpaque(selectedIndex != 1);
        lblImport.setForeground(selectedIndex == 1 ? Color.WHITE : Color.BLACK);

        rowExport.setOpaque(selectedIndex != 2);
        lblExport.setForeground(selectedIndex == 2 ? Color.WHITE : Color.BLACK);

        rowPrescription.setOpaque(selectedIndex != 3);
        lblPrescription.setForeground(selectedIndex == 3 ? Color.WHITE : Color.BLACK);

        repaint();

        if (onCategorySelected != null) {
            onCategorySelected.accept(getSelectedFilter());
        }
    }

    /**
     * Chọn dòng theo mã loại phiếu ("ALL", "NHAP_KHO", "XUAT_KHO", "DON_THUOC").
     */
    public void selectCategory(String ticketType) {
        if (ticketType == null) return;
        switch (ticketType.trim().toUpperCase()) {
            case CategoryFilter.ALL, "TAT_CA" -> selectRow(0);
            case CategoryFilter.NHAP_KHO -> selectRow(1);
            case CategoryFilter.XUAT_KHO -> selectRow(2);
            case CategoryFilter.DON_THUOC -> selectRow(3);
            default -> {}
        }
    }

    /** Hàm `getTicketTypeByIndex`: lấy phiếu loai theo `Index`. */
    public String getTicketTypeByIndex(int index) {
        return switch (index) {
            case 1 -> CategoryFilter.NHAP_KHO;
            case 2 -> CategoryFilter.XUAT_KHO;
            case 3 -> CategoryFilter.DON_THUOC;
            default -> CategoryFilter.ALL;
        };
    }

    /** Hàm `getSelectedFilter`: lấy duocchon `Filter`. */
    public CategoryFilter getSelectedFilter() {
        return new CategoryFilter(warehouseId, getTicketTypeByIndex(selectedIndex));
    }

    /** Hàm `getSelectedIndex`: lấy duocchon `Index`. */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    // Getters phục vụ kiểm thử và liên kết
    public JLabel getLblTitle() { return lblTitle; }
    /** Hàm `getLblAll`: lấy `Lbl` `All`. */
    public JLabel getLblAll() { return lblAll; }
    /** Hàm `getLblImport`: lấy `Lbl` `Import`. */
    public JLabel getLblImport() { return lblImport; }
    /** Hàm `getLblExport`: lấy `Lbl` xuất. */
    public JLabel getLblExport() { return lblExport; }
    /** Hàm `getLblPrescription`: lấy `Lbl` đơn thuốc. */
    public JLabel getLblPrescription() { return lblPrescription; }

    /** Hàm `getRowAllTickets`: lấy `Row` `All` `Tickets`. */
    public JPanel getRowAllTickets() { return rowAllTickets; }
    /** Hàm `getRowImport`: lấy `Row` `Import`. */
    public JPanel getRowImport() { return rowImport; }
    /** Hàm `getRowImportReplenish`: lấy `Row` `Import` `Replenish`. */
    public JPanel getRowImportReplenish() { return rowImport; } // Alias tương thích
    /** Hàm `getRowExport`: lấy `Row` xuất. */
    public JPanel getRowExport() { return rowExport; }
    /** Hàm `getRowPrescription`: lấy `Row` đơn thuốc. */
    public JPanel getRowPrescription() { return rowPrescription; }

    /** Hàm `getAllCount`: lấy `All` dem. */
    public int getAllCount() { return allCount; }
    /** Hàm `getImportCount`: lấy `Import` dem. */
    public int getImportCount() { return importCount; }
    /** Hàm `getExportCount`: lấy xuất dem. */
    public int getExportCount() { return exportCount; }
    /** Hàm `getPrescriptionCount`: lấy đơn thuốc dem. */
    public int getPrescriptionCount() { return prescriptionCount; }
}
