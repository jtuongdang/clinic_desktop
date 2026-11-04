package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.desktop.modules.examination.components.tabs.TreatmentResolutionTabPanel;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

/**
 * Giao diện màn hình cửa sổ tra cứu danh mục bệnh ICD-10 (Bệnh lý / Bệnh truyền nhiễm).
 * Khớp 100% chi tiết trong ảnh chụp thực tế media_1793374103322_54623f7b.jpg:
 * - Thanh tiêu đề cửa sổ: DANH MỤC THUỐC BỆNH VIỆN -- Bệnh lý/Bệnh truyền nhiễm ---- Version: 2022.7.15.3
 * - Thanh tìm kiếm màu vàng nhạt ở trên cùng (Fh: AdB - EH 2.c2) kèm nút kính lúp 🔍
 * - Bảng danh mục 2 cột: [Mã ICD] | [Tên Bệnh]
 * - Dòng đang chọn được bôi sáng màu xanh lam nhạt chuẩn EHC
 * - Góc phải dưới cùng có nút [💾 Lưu]
 */
public class IcdCatalogPanel extends JPanel {

    private final BiConsumer<String, String> onSelectedCallback;
    private final Runnable onCloseCallback;
    private JTextField txtSearch;
    private JTable icdTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    public IcdCatalogPanel(String initialSearch, BiConsumer<String, String> onSelectedCallback, Runnable onCloseCallback) {
        this.onSelectedCallback = onSelectedCallback;
        this.onCloseCallback = onCloseCallback;

        initUI(initialSearch != null ? initialSearch : "A02");
    }

    private void initUI(String initialSearch) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new LineBorder(new Color(175, 185, 200), 1));
        setPreferredSize(new Dimension(560, 480));

        // 1. THANH TIÊU ĐỀ CỬA SỔ CHUẨN WINDOWS EHC
        JPanel windowTitleBar = new JPanel(new BorderLayout());
        windowTitleBar.setBackground(new Color(240, 243, 248));
        windowTitleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));
        windowTitleBar.setPreferredSize(new Dimension(560, 26));

        JLabel lblTitle = new JLabel("  📑 DANH MỤC THUỐC BỆNH VIỆN -- Bệnh lý/Bệnh truyền nhiễm ---- Version: 2022.7.15.3");
        lblTitle.setFont(AppFonts.SMALL_BOLD);
        lblTitle.setForeground(new Color(30, 40, 55));
        windowTitleBar.add(lblTitle, BorderLayout.WEST);

        JPanel winBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        winBtns.setOpaque(false);

        JLabel btnClose = new JLabel("✕ ");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setForeground(new Color(100, 110, 125));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onCloseCallback != null) onCloseCallback.run();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                btnClose.setForeground(Color.RED);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnClose.setForeground(new Color(100, 110, 125));
            }
        });
        winBtns.add(btnClose);
        windowTitleBar.add(winBtns, BorderLayout.EAST);

        // Gom TitleBar + SearchBar lên NORTH
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.add(windowTitleBar);

        // 2. THANH TÌM KIẾM NỀN VÀNG NHẠT (Khớp 100% media_1793374103322_54623f7b.jpg)
        JPanel searchBar = new JPanel(new BorderLayout(6, 0));
        searchBar.setBackground(new Color(255, 253, 205)); // Màu vàng nhạt đặc trưng EHC
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 218, 228)),
                new EmptyBorder(3, 8, 3, 8)
        ));
        searchBar.setPreferredSize(new Dimension(560, 26));

        txtSearch = new JTextField(initialSearch);
        txtSearch.setFont(AppFonts.SMALL_BOLD);
        txtSearch.setBackground(new Color(255, 253, 205));
        txtSearch.setBorder(BorderFactory.createEmptyBorder());

        JLabel lblSearchIcon = new JLabel("🔍");
        lblSearchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSearchIcon.setForeground(new Color(0, 102, 204));
        lblSearchIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));

        searchBar.add(txtSearch, BorderLayout.CENTER);
        searchBar.add(lblSearchIcon, BorderLayout.EAST);
        topContainer.add(searchBar);

        add(topContainer, BorderLayout.NORTH);

        // 3. BẢNG DANH MỤC ICD-10
        String[] columns = {"Mã ICD", "Tên Bệnh"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Dữ liệu danh mục ICD-10 chuẩn các bệnh truyền nhiễm & thường gặp
        Object[][] rawData = {
                {"A00", "Bệnh tả (Cholera)"},
                {"A01", "Bệnh thương hàn và phó thương hàn"},
                {"A01.0", "Bệnh thương hàn (Typhoid fever)"},
                {"A01.1", "Bệnh phó thương hàn A"},
                {"A01.2", "Bệnh phó thương hàn B"},
                {"A01.3", "Bệnh phó thương hàn C"},
                {"A02", "Nhiễm salmonella khác"},
                {"A02.0", "Viêm ruột do salmonella"},
                {"A02.1", "Nhiễm khuẩn huyết do salmonella"},
                {"A02.2", "Nhiễm salmonella khu trú"},
                {"A02.8", "Nhiễm salmonella xác định khác"},
                {"A02.9", "Nhiễm salmonella, không xác định"},
                {"A03", "Nhiễm lỵ trực trùng (Shigellosis)"},
                {"A04", "Nhiễm trùng đường ruột do vi khuẩn khác"},
                {"A05", "Ngộ độc thức ăn do vi khuẩn khác"},
                {"A06", "Bệnh do amip (Amoebiasis)"},
                {"A07", "Bệnh đường ruột do động vật nguyên sinh khác"},
                {"A08", "Nhiễm virus và các bệnh nhiễm trùng đường ruột khác"},
                {"A09", "Tiêu chảy và viêm dạ dày ruột do nhiễm khuẩn"},
                {"B01", "Bệnh thủy đậu [Varicella]"},
                {"B05", "Bệnh sởi [Measles]"},
                {"B15", "Viêm gan virus A cấp"},
                {"B16", "Viêm gan virus B cấp"},
                {"B18", "Viêm gan virus mạn tính"},
                {"J00", "Viêm mũi họng cấp [Cảm thường]"},
                {"J02", "Viêm họng cấp"},
                {"J03", "Viêm amiđan cấp"},
                {"J20", "Viêm phế quản cấp"},
                {"K29", "Viêm dạ dày và tá tràng"}
        };

        for (Object[] row : rawData) {
            tableModel.addRow(row);
        }

        icdTable = new JTable(tableModel);
        icdTable.setFont(AppFonts.SMALL);
        icdTable.setRowHeight(21);
        icdTable.setShowGrid(true);
        icdTable.setGridColor(new Color(238, 242, 248));
        icdTable.setSelectionBackground(new Color(214, 232, 255)); // Màu xanh lam nhạt chuẩn ảnh chụp
        icdTable.setSelectionForeground(new Color(15, 25, 40));

        JTableHeader header = icdTable.getTableHeader();
        header.setFont(AppFonts.SMALL_BOLD);
        header.setBackground(new Color(242, 246, 252));
        header.setForeground(new Color(30, 35, 45));
        header.setPreferredSize(new Dimension(560, 24));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));

        icdTable.getColumnModel().getColumn(0).setPreferredWidth(85);
        icdTable.getColumnModel().getColumn(0).setMaxWidth(110);
        icdTable.getColumnModel().getColumn(1).setPreferredWidth(450);

        // Renderer in đậm các nhóm chính
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

                Object codeVal = table.getValueAt(row, 0);
                boolean isMainGroup = codeVal != null && !codeVal.toString().contains(".");

                if (isMainGroup) {
                    lbl.setFont(AppFonts.SMALL_BOLD);
                } else {
                    lbl.setFont(AppFonts.SMALL);
                }

                if (col == 0) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                }

                return lbl;
            }
        };

        icdTable.getColumnModel().getColumn(0).setCellRenderer(cellRenderer);
        icdTable.getColumnModel().getColumn(1).setCellRenderer(cellRenderer);

        // Tìm kiếm lọc thời gian thực
        sorter = new TableRowSorter<>(tableModel);
        icdTable.setRowSorter(sorter);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = txtSearch.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        // Bắt sự kiện chọn dòng
        icdTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    confirmSelection();
                }
            }
        });

        icdTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmSelection();
                    e.consume();
                }
            }
        });

        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && icdTable.getRowCount() > 0) {
                    icdTable.requestFocusInWindow();
                    icdTable.setRowSelectionInterval(0, 0);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmSelection();
                }
            }
        });

        selectInitialRow(initialSearch);

        JScrollPane scrollPane = new JScrollPane(icdTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // 4. CHÂN TRANG: NÚT [💾 Lưu] (Góc phải dưới)
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        footer.setBackground(new Color(248, 250, 253));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 218, 228)));

        JButton btnSave = new JButton("Lưu", new TreatmentResolutionTabPanel.SaveDiskIcon());
        btnSave.setFont(AppFonts.SMALL_BOLD);
        btnSave.setForeground(new Color(25, 30, 40));
        btnSave.setBackground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(3, 10, 3, 10)
        ));
        btnSave.addActionListener(e -> confirmSelection());

        footer.add(btnSave);
        add(footer, BorderLayout.SOUTH);
    }

    private void selectInitialRow(String code) {
        if (code == null || code.trim().isEmpty()) {
            if (icdTable.getRowCount() > 0) {
                icdTable.setRowSelectionInterval(0, 0);
            }
            return;
        }

        String search = code.trim().toUpperCase();
        for (int i = 0; i < icdTable.getRowCount(); i++) {
            Object val = icdTable.getValueAt(i, 0);
            if (val != null && val.toString().toUpperCase().contains(search)) {
                icdTable.setRowSelectionInterval(i, i);
                icdTable.scrollRectToVisible(icdTable.getCellRect(i, 0, true));
                return;
            }
        }

        if (icdTable.getRowCount() > 0) {
            icdTable.setRowSelectionInterval(0, 0);
        }
    }

    private void confirmSelection() {
        int r = icdTable.getSelectedRow();
        if (r >= 0) {
            String code = icdTable.getValueAt(r, 0).toString();
            String name = icdTable.getValueAt(r, 1).toString();
            if (onSelectedCallback != null) {
                onSelectedCallback.accept(code, name);
            }
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        }
    }

    public JTable getIcdTable() {
        return icdTable;
    }

    public JTextField getTxtSearch() {
        return txtSearch;
    }
}
