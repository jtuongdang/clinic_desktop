package vn.clinicmanagement.desktop.modules.examination.components.tabs;

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

/**
 * Tab "Vật Tư(1)" trong phân hệ Khám Bệnh.
 * Hiển thị chi tiết phiếu chỉ định vật tư đã kê sau khi nhấn "Lưu + Đóng".
 * Khớp 100% giao diện trong ảnh chụp mẫu thực tế:
 * - Cột phải: Khung tìm kiếm màu vàng nhạt + Thẻ phiếu VT220819.1 (màu xanh dương đậm)
 * - Khung chính:
 *   + Tiêu đề phiếu: Loại phiếu, Mã phiếu, Người chỉ định, Khoa, Ngày y lệnh, Ngày chỉ định, Nơi chỉ định, Trạng thái (Đã xuất), Chẩn đoán
 *   + Bảng vật tư chi tiết: STT, Mã Vật Tư, Tên Vật Tư, Đơn Vị, Số Lượng (dòng 1: VT026 | Bơm cho ăn 5ml | Cái | 1)
 *   + Cụm nút chân trang: [🖨 In Phiếu (F3)] và [❌ Hủy Gửi Phiếu]
 */
public class SuppliesOrderTabPanel extends JPanel {

    private JTable suppliesTable;
    private DefaultTableModel tableModel;
    private JLabel lblNgayYLenh;
    private JLabel lblNgayChiDinh;
    private JLabel lblMaPhieu;
    private JLabel lblTrangThai;
    private JLabel lblKhoa;
    private JPanel selectedTicketCard;

    public SuppliesOrderTabPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. CỘT PHẢI: KHUNG CHỌN PHIẾU VẬT TƯ (TÌM KIẾM + DANH SÁCH THẺ PHIẾU)
        add(buildTicketSelectorPanel(), BorderLayout.EAST);

        // 2. KHUNG TRUNG TÂM: CHI TIẾT PHIẾU VẬT TƯ (THÔNG TIN HÀNH CHÍNH + BẢNG VẬT TƯ + NÚT IN)
        add(buildTicketDetailPanel(), BorderLayout.CENTER);
    }

    private JPanel buildTicketSelectorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(205, 500));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));

        // Hàng tìm kiếm màu vàng nhạt trên cùng
        JPanel searchBar = new JPanel(new BorderLayout(4, 0));
        searchBar.setBackground(new Color(255, 255, 208));
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 212, 195)),
                new EmptyBorder(2, 6, 2, 6)
        ));
        searchBar.setPreferredSize(new Dimension(205, 24));

        JLabel lblSearch = new JLabel("Tìm kiếm");
        lblSearch.setFont(AppFonts.SMALL);
        lblSearch.setForeground(new Color(40, 40, 40));

        JLabel lblSearchIcon = new JLabel("🔍");
        lblSearchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        searchBar.add(lblSearch, BorderLayout.WEST);
        searchBar.add(lblSearchIcon, BorderLayout.EAST);
        panel.add(searchBar, BorderLayout.NORTH);

        // Danh sách thẻ phiếu
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        // Thẻ phiếu 1: VT7730BZL1 (đang chọn màu xanh dương đậm khớp ảnh chụp)
        JPanel card1 = createTicketCard("VT7730BZL 1 - 10/09/2022", "(Khám bệnh) Lúc 15:25 19/08/2022", true);
        selectedTicketCard = card1;

        // Thẻ phiếu 2: VT7730KAL 1
        JPanel card2 = createTicketCard("VT7730KAL 1 - 20/08/2022", "(Khám bệnh) Lúc 15:25 19/08/2022", false);

        // Thẻ phiếu 3: V/P7/C... - Lịch sử khám bệnh
        JPanel card3 = createTicketCard("V/P7/C... - Lịch sử khám bệnh", "(Khám bệnh) Lúc 15:25 19/08/2022", false);

        listPanel.add(card1);
        listPanel.add(card2);
        listPanel.add(card3);
        panel.add(listPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTicketCard(String title, String subtitle, boolean isSelected) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 2));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 230, 238)),
                new EmptyBorder(4, 8, 4, 6)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel l1 = new JLabel(title);
        l1.setFont(AppFonts.SMALL_BOLD);

        JLabel l2 = new JLabel(subtitle);
        l2.setFont(AppFonts.SMALL);

        if (isSelected) {
            card.setBackground(new Color(0, 120, 215)); // Màu xanh dương hoàng gia EHC
            l1.setForeground(Color.WHITE);
            l2.setForeground(new Color(235, 245, 255));
        } else {
            card.setBackground(Color.WHITE);
            l1.setForeground(new Color(30, 35, 45));
            l2.setForeground(new Color(90, 95, 105));
        }

        card.add(l1);
        card.add(l2);

        // Tạo Popup Menu khi bấm chuột phải: [ ✓ ] Chuyển Ngày Y Lệnh
        JPopupMenu contextMenu = createOrderDateContextMenu(title, l2);

        MouseAdapter rightClickAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };

        card.addMouseListener(rightClickAdapter);
        l1.addMouseListener(rightClickAdapter);
        l2.addMouseListener(rightClickAdapter);

        return card;
    }

    private JPopupMenu createOrderDateContextMenu(String ticketCode, JLabel lblSubtitle) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createLineBorder(new Color(130, 145, 165), 1));

        JMenuItem miChangeDate = new JMenuItem("Chuyển Ngày Y Lệnh", new CheckmarkMiniIcon());
        miChangeDate.setFont(AppFonts.SMALL);
        miChangeDate.setForeground(new Color(25, 30, 40));
        miChangeDate.setBackground(Color.WHITE);
        miChangeDate.setPreferredSize(new Dimension(175, 24));
        miChangeDate.setBorder(new EmptyBorder(2, 6, 2, 8));
        miChangeDate.setCursor(new Cursor(Cursor.HAND_CURSOR));

        miChangeDate.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                miChangeDate.setBackground(new Color(225, 238, 252)); // Nền xanh nhạt hover chuẩn EHC
            }
            @Override
            public void mouseExited(MouseEvent e) {
                miChangeDate.setBackground(Color.WHITE);
            }
        });

        miChangeDate.addActionListener(e -> {
            String current = lblNgayYLenh != null ? lblNgayYLenh.getText() : "18:20 21/08/2022";
            String newDate = null;
            if (!GraphicsEnvironment.isHeadless()) {
                newDate = JOptionPane.showInputDialog(this,
                        "Nhập ngày y lệnh mới cho phiếu " + ticketCode + ":",
                        current);
            }
            if (newDate != null && !newDate.trim().isEmpty()) {
                if (lblNgayYLenh != null) {
                    lblNgayYLenh.setText(newDate.trim());
                }
                lblSubtitle.setText("(Khám bệnh) Lúc " + newDate.trim());
            }
        });

        menu.add(miChangeDate);
        return menu;
    }

    public JPanel getSelectedTicketCard() {
        return selectedTicketCard;
    }

    private JPanel buildTicketDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // 1. Khung thông tin hành chính của phiếu
        JPanel headerForm = buildTicketHeaderForm();
        panel.add(headerForm, BorderLayout.NORTH);

        // 2. Bảng vật tư chi tiết 5 cột
        String[] cols = {"STT", "Mã Vật Tư", "Tên Vật Tư", "Đơn Vị", "Số Lượng"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        // Dòng 1: Vật tư đã kê (khớp 100% ảnh chụp mẫu: VT026 | Bơm cho ăn 5ml | Cái | 50)
        tableModel.addRow(new Object[]{"1", "VT026", "Bơm cho ăn 5ml", "Cái", "50"});

        // Thêm các dòng trống tạo lưới chuẩn
        for (int i = 0; i < 18; i++) {
            tableModel.addRow(new Object[]{"", "", "", "", ""});
        }

        suppliesTable = new JTable(tableModel);
        suppliesTable.setRowHeight(21);
        suppliesTable.setFont(AppFonts.BODY);
        suppliesTable.setShowGrid(true);
        suppliesTable.setGridColor(new Color(230, 235, 242));
        suppliesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suppliesTable.setBackground(Color.WHITE);

        JTableHeader th = suppliesTable.getTableHeader();
        th.setFont(AppFonts.SMALL_BOLD);
        th.setPreferredSize(new Dimension(800, 24));
        th.setBackground(new Color(242, 246, 252));
        th.setForeground(new Color(25, 30, 40));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);

        suppliesTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        suppliesTable.getColumnModel().getColumn(0).setMaxWidth(50);
        suppliesTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        suppliesTable.getColumnModel().getColumn(1).setPreferredWidth(95);
        suppliesTable.getColumnModel().getColumn(1).setMaxWidth(110);

        suppliesTable.getColumnModel().getColumn(2).setPreferredWidth(340);

        suppliesTable.getColumnModel().getColumn(3).setPreferredWidth(65);
        suppliesTable.getColumnModel().getColumn(3).setMaxWidth(75);
        suppliesTable.getColumnModel().getColumn(3).setCellRenderer(centerRender);

        suppliesTable.getColumnModel().getColumn(4).setPreferredWidth(65);
        suppliesTable.getColumnModel().getColumn(4).setMaxWidth(75);
        suppliesTable.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        JScrollPane scrollPane = new JScrollPane(suppliesTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        panel.add(scrollPane, BorderLayout.CENTER);

        // 3. Cụm nút In & Hủy chân trang
        JPanel actionFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 6));
        actionFooter.setBackground(new Color(248, 250, 253));
        actionFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 218, 228)));
        actionFooter.setPreferredSize(new Dimension(800, 38));

        JButton btnPrint = new JButton(" In Phiếu (F3)", new PrinterIcon());
        btnPrint.setFont(AppFonts.BODY_BOLD);
        btnPrint.setForeground(new Color(25, 25, 25));
        btnPrint.setBackground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 208), 1),
                new EmptyBorder(3, 10, 3, 10)
        ));
        btnPrint.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Đang xuất lệnh in phiếu chỉ định vật tư:\n" +
                    "▶ Mã phiếu: VT220819.1\n" +
                    "▶ Bệnh nhân: NGUYỄN THỊ BÌNH AN | 22000308\n" +
                    "▶ Vật tư: Bơm cho ăn 5ml (SL: 1)",
                    "In Phiếu Chỉ Định Vật Tư", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnCancel = new JButton(" Hủy Gửi Phiếu", new RedCrossIcon());
        btnCancel.setFont(AppFonts.BODY_BOLD);
        btnCancel.setForeground(new Color(185, 20, 20));
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 208), 1),
                new EmptyBorder(3, 10, 3, 10)
        ));
        btnCancel.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn hủy phiếu chỉ định vật tư VT220819.1?",
                    "Xác Nhận Hủy Gửi Phiếu", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(this, "Đã hủy phiếu vật tư thành công!");
            }
        });

        actionFooter.add(btnPrint);
        actionFooter.add(btnCancel);

        panel.add(actionFooter, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTicketHeaderForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(6, 8, 8, 8));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 4, 2, 4);

        int r = 0;

        // Hàng 1: Loại phiếu | Ngày y lệnh | Ngày chỉ định
        JLabel lblLoaiPhieu = createUnderlineValue("Phiếu chỉ định");
        this.lblNgayYLenh = createUnderlineValue("18:20 21/08/2022");
        this.lblNgayChiDinh = createUnderlineValue("19:21 19/08/2022");
        addHeaderRow(form, c, r++,
                "Loại phiếu:", lblLoaiPhieu, 1,
                "Ngày y lệnh:", lblNgayYLenh, 1,
                "Ngày chỉ định:", lblNgayChiDinh, 1);

        // Hàng 2: Mã phiếu | Nơi chỉ định
        this.lblMaPhieu = createUnderlineValue("VT220621.1");
        JLabel lblNoiChiDinh = createUnderlineValue("PK Nhi 1");
        addHeaderRow(form, c, r++,
                "Mã phiếu:", lblMaPhieu, 1,
                "Nơi chỉ định:", lblNoiChiDinh, 3,
                null, null, 0);

        // Hàng 3: Người chỉ định | Trạng thái (Đã xuất...)
        JLabel lblNguoiChiDinh = createUnderlineValue("Sennv");
        this.lblTrangThai = createUnderlineValue("Đã xuất [Mã phiếu : 1608773]");
        addHeaderRow(form, c, r++,
                "Người chỉ định :", lblNguoiChiDinh, 1,
                "Trạng thái:", lblTrangThai, 3,
                null, null, 0);

        // Hàng 4: Khoa | Chẩn đoán
        this.lblKhoa = createUnderlineValue("TTVT Phòng Phẩm");
        JLabel lblChanDoan = createUnderlineValue("A62.0 Viêm ruột do Salmonella");
        addHeaderRow(form, c, r,
                "Khoa:", lblKhoa, 1,
                "Chẩn đoán:", lblChanDoan, 3,
                null, null, 0);

        return form;
    }

    private void addHeaderRow(JPanel panel, GridBagConstraints c, int row,
                              String l1, JComponent v1, int span1,
                              String l2, JComponent v2, int span2,
                              String l3, JComponent v3, int span3) {
        c.gridy = row;
        int col = 0;

        String[] labels = {l1, l2, l3};
        JComponent[] vals = {v1, v2, v3};
        int[] spans = {span1, span2, span3};

        for (int i = 0; i < 3; i++) {
            if (labels[i] == null || vals[i] == null) continue;

            c.gridx = col++;
            c.gridwidth = 1;
            c.weightx = 0;
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(AppFonts.SMALL);
            lbl.setForeground(new Color(30, 30, 30));
            panel.add(lbl, c);

            c.gridx = col;
            c.gridwidth = spans[i];
            c.weightx = spans[i] >= 3 ? 0.6 : 0.3;
            panel.add(vals[i], c);
            col += spans[i];
        }
    }

    private JLabel createUnderlineValue(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppFonts.SMALL);
        lbl.setForeground(new Color(18, 45, 130)); // Chữ màu xanh dương đậm
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(185, 195, 210)));
        return lbl;
    }

    // =========================================================================
    // VECTOR ICONS
    // =========================================================================

    private static class PrinterIcon implements Icon {
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(60, 60, 60));
            g2.fillRect(x + 3, y + 1, 10, 4);
            g2.fillRoundRect(x + 1, y + 4, 14, 7, 2, 2);
            g2.setColor(new Color(230, 230, 230));
            g2.fillRect(x + 3, y + 8, 10, 5);
            g2.setColor(new Color(100, 100, 100));
            g2.drawRect(x + 3, y + 8, 10, 5);
            g2.dispose();
        }
    }

    private static class RedCrossIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(205, 30, 30));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 2, x + 12, y + 12);
            g2.drawLine(x + 12, y + 2, x + 2, y + 12);
            g2.dispose();
        }
    }

    public static class CheckmarkMiniIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(30, 35, 45));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 7, x + 5, y + 10);
            g2.drawLine(x + 5, y + 10, x + 11, y + 3);
            g2.dispose();
        }
    }
}
