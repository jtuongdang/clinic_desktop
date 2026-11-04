package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Cây danh mục kho / loại phiếu bên trái (Hóa chất khoa sinh hóa).
 * Khớp 100% figmar thiết kế:
 * - Dòng banner vàng cam chữ đỏ sẫm "Hóa chất khoa sinh hóa"
 * - Hàng đang chọn màu xanh dương chữ trắng "Tất Cả Phiếu (1)"
 * - Nút nhánh cây "[-] Loại Phiếu" và "Nhập bù (1)"
 */
public class PharmacyCategoryTreePanel extends JPanel {

    private int selectedIndex = 0; // 0 = Tất Cả Phiếu (1), 1 = Nhập bù (1)
    private final JPanel rowAllTickets;
    private final JPanel rowImportReplenish;

    public PharmacyCategoryTreePanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(215, 600));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(195, 205, 216)));

        // --- 1. BANNER TIÊU ĐỀ VÀNG VÀ CHỮ ĐỎ SẪM ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(215, 26));
        headerPanel.setBackground(new Color(255, 242, 178)); // Nền vàng nhạt chuẩn EHC
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(223, 207, 139)));

        JLabel lblTitle = new JLabel("Hóa chất khoa sinh hóa");
        lblTitle.setFont(AppFonts.BODY_BOLD);
        lblTitle.setForeground(new Color(138, 30, 0)); // Chữ đỏ cam sẫm chuẩn EHC
        lblTitle.setBorder(new EmptyBorder(0, 8, 0, 0));

        // Nút thu gọn nhỏ bên phải
        JLabel btnCollapse = new JLabel("<");
        btnCollapse.setFont(AppFonts.SMALL_BOLD);
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

        // Row 1: Tất Cả Phiếu (1) - ĐANG CHỌN (Solid Blue, White Text)
        rowAllTickets = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                if (selectedIndex == 0) {
                    g.setColor(new Color(16, 104, 188)); // Xanh dương đậm chọn hàng
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    super.paintComponent(g);
                }
            }
        };
        rowAllTickets.setOpaque(selectedIndex != 0);
        rowAllTickets.setBackground(Color.WHITE);
        rowAllTickets.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        rowAllTickets.setPreferredSize(new Dimension(215, 24));
        rowAllTickets.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblAll = new JLabel("Tất Cả Phiếu (1)");
        lblAll.setFont(AppFonts.BODY_BOLD);
        lblAll.setForeground(selectedIndex == 0 ? Color.WHITE : Color.BLACK);
        lblAll.setBorder(new EmptyBorder(0, 12, 0, 0));
        rowAllTickets.add(lblAll, BorderLayout.WEST);

        // Row 2: Nút nhánh cha "[-] Loại Phiếu"
        JPanel rowCategoryNode = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        rowCategoryNode.setBackground(Color.WHITE);
        rowCategoryNode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        rowCategoryNode.setPreferredSize(new Dimension(215, 24));
        rowCategoryNode.setBorder(new EmptyBorder(0, 8, 0, 0));

        JLabel lblMinus = new JLabel(PharmacyIcons.createMinusBoxIcon());
        JLabel lblCategory = new JLabel("Loại Phiếu");
        lblCategory.setFont(AppFonts.BODY);
        lblCategory.setForeground(new Color(30, 30, 30));

        rowCategoryNode.add(lblMinus);
        rowCategoryNode.add(lblCategory);

        // Row 3: Nhánh con "Nhập bù (1)" với đường nét nối cây
        rowImportReplenish = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                if (selectedIndex == 1) {
                    g.setColor(new Color(16, 104, 188));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    super.paintComponent(g);
                    // Vẽ đường nối cây thư mục
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(180, 185, 195));
                    g2.drawLine(14, 0, 14, 12);
                    g2.drawLine(14, 12, 28, 12);
                    g2.dispose();
                }
            }
        };
        rowImportReplenish.setOpaque(selectedIndex != 1);
        rowImportReplenish.setBackground(Color.WHITE);
        rowImportReplenish.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        rowImportReplenish.setPreferredSize(new Dimension(215, 24));
        rowImportReplenish.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblReplenish = new JLabel("Nhập bù (1)");
        lblReplenish.setFont(AppFonts.BODY);
        lblReplenish.setForeground(selectedIndex == 1 ? Color.WHITE : Color.BLACK);
        lblReplenish.setBorder(new EmptyBorder(0, 32, 0, 0));
        rowImportReplenish.add(lblReplenish, BorderLayout.WEST);

        // Sự kiện click chuyển chọn
        rowAllTickets.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectedIndex = 0;
                lblAll.setForeground(Color.WHITE);
                lblReplenish.setForeground(Color.BLACK);
                rowAllTickets.setOpaque(false);
                rowImportReplenish.setOpaque(true);
                repaint();
            }
        });

        rowImportReplenish.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectedIndex = 1;
                lblAll.setForeground(Color.BLACK);
                lblReplenish.setForeground(Color.WHITE);
                rowAllTickets.setOpaque(true);
                rowImportReplenish.setOpaque(false);
                repaint();
            }
        });

        treeContent.add(rowAllTickets);
        treeContent.add(rowCategoryNode);
        treeContent.add(rowImportReplenish);

        // Filler trống phía dưới
        JPanel filler = new JPanel();
        filler.setBackground(Color.WHITE);
        treeContent.add(filler);

        add(treeContent, BorderLayout.CENTER);
    }
}
