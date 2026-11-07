package vn.clinicmanagement.desktop.modules.reception.components;

import vn.clinicmanagement.application.reception.ClinicRoomQueueSummary;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.modules.pharmacy.components.PharmacyIcons;

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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Khung hiển thị Trạng Thái Phòng Khám & DS Đăng Ký Khám.
 * Khớp 100% cả 2 trạng thái chụp từ hệ thống:
 * 1. Khi chọn Tab "DS Đăng Ký Khám": Bảng danh sách phiếu khám của người bệnh
 * 2. Khi chọn Tab "Trạng Thái Phòng Khám": Bố cục 3 cột (Cột 1 phòng khám, Cột 2 phòng khám có phòng chọn màu ĐỎ, Cột 3 bảng tìm kiếm STT khám)
 */
public class ClinicRoomStatusPanel extends JPanel {

    private int activeTabIndex = 2; // Mặc định hiển thị Tab 2: "Trạng Thái Phòng Khám" (Khớp 100% ảnh sau khi chọn dịch vụ)
    private String selectedRoomName = "Phòng Tiêm Chủng"; // Phòng đang được chọn trên form

    private final CardLayout cardLayout;
    private final JPanel cardsContainer;
    private final JPanel tabStrip;
    private DefaultTableModel queueModel;
    private DefaultTableModel firstRoomModel;
    private DefaultTableModel secondRoomModel;
    private JTable queueTable;
    private JTable firstRoomTable;
    private JTable secondRoomTable;

    public ClinicRoomStatusPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. DẢI 9 TAB NGANG
        tabStrip = buildTabStrip();
        add(tabStrip, BorderLayout.NORTH);

        // 2. VÙNG NỘI DUNG CHUYỂN ĐỔI THEO TAB (CARDLAYOUT)
        cardLayout = new CardLayout();
        cardsContainer = new JPanel(cardLayout);
        cardsContainer.setBackground(Color.WHITE);

        // Card 0: Bảng "DS Đăng Ký Khám" (Khớp ảnh 1)
        cardsContainer.add(buildRegistrationQueueView(), "TAB_REGISTRATION_QUEUE");

        // Card 2: 3 Cột "Trạng Thái Phòng Khám" (Khớp ảnh sau khi chọn dịch vụ)
        cardsContainer.add(buildClinicRoomStatusView(), "TAB_ROOM_STATUS");

        add(cardsContainer, BorderLayout.CENTER);

        // Mặc định hiển thị Tab 2 (Trạng Thái Phòng Khám)
        switchTab(2);
    }

    public void setSelectedClinicRoom(String roomName) {
        this.selectedRoomName = roomName;
        if (firstRoomTable != null) firstRoomTable.repaint();
        if (secondRoomTable != null) secondRoomTable.repaint();
    }

    public void setQueue(List<ReceptionQueueItem> items) {
        Objects.requireNonNull(items, "items");
        queueModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        for (ReceptionQueueItem item : items) {
            queueModel.addRow(new Object[]{
                    item.registeredAt() == null ? "" : item.registeredAt().format(formatter),
                    item.queueNumberDisplay() == null || item.queueNumberDisplay().isBlank()
                            ? Integer.toString(item.queueNumber()) : item.queueNumberDisplay(),
                    item.status(),
                    "",
                    "",
                    "",
                    ""
            });
        }
    }

    public void setRoomSummaries(List<ClinicRoomQueueSummary> rooms) {
        Objects.requireNonNull(rooms, "rooms");
        firstRoomModel.setRowCount(0);
        secondRoomModel.setRowCount(0);
        int firstColumnSize = (rooms.size() + 1) / 2;
        for (int i = 0; i < rooms.size(); i++) {
            ClinicRoomQueueSummary room = rooms.get(i);
            Object[] row = {
                    room.roomName(),
                    room.registeredCount(),
                    room.waitingCount(),
                    room.examiningCount()
            };
            (i < firstColumnSize ? firstRoomModel : secondRoomModel).addRow(row);
        }
        firstRoomTable.repaint();
        secondRoomTable.repaint();
    }

    JTable getQueueTable() {
        return queueTable;
    }

    JTable getFirstRoomTable() {
        return firstRoomTable;
    }

    JTable getSecondRoomTable() {
        return secondRoomTable;
    }

    private void switchTab(int index) {
        this.activeTabIndex = index;
        if (index == 2) {
            cardLayout.show(cardsContainer, "TAB_ROOM_STATUS");
        } else {
            cardLayout.show(cardsContainer, "TAB_REGISTRATION_QUEUE");
        }
        tabStrip.repaint();
    }

    /**
     * Giao diện Card 0: "DS Đăng Ký Khám" (Khớp 100% Ảnh 1)
     */
    private JPanel buildRegistrationQueueView() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);

        String[] cols = {
                "Ngày Đăng Ký | Số thứ tự",
                "STT Khám",
                "Trạng Thái",
                "Ngày Hẹn",
                "Ngày Khám",
                "Bác Sĩ Khám",
                "Ngày Ra"
        };

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        queueModel = model;
        queueTable = createGridTable(model);
        JTable table = queueTable;
        table.getColumnModel().getColumn(0).setPreferredWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(160);
        table.getColumnModel().getColumn(6).setPreferredWidth(120);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);

        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    /**
     * Giao diện Card 2: "Trạng Thái Phòng Khám" (Khớp 100% Ảnh 2 với 3 Cột song song)
     */
    private JPanel buildClinicRoomStatusView() {
        JPanel p = new JPanel(new GridLayout(1, 3, 2, 0));
        p.setBackground(new Color(225, 230, 238));

        // --- CỘT 1 (Bên trái): Tên Phòng Khám | ĐK | Chờ | Khám ---
        String[] cols1 = {"Tên Phòng Khám", "ĐK", "Chờ", "Khám"};
        firstRoomModel = new DefaultTableModel(cols1, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        firstRoomTable = createGridTable(firstRoomModel);
        JTable table1 = firstRoomTable;
        table1.getColumnModel().getColumn(0).setPreferredWidth(260);
        table1.getColumnModel().getColumn(1).setPreferredWidth(35);
        table1.getColumnModel().getColumn(1).setMaxWidth(40);
        table1.getColumnModel().getColumn(2).setPreferredWidth(35);
        table1.getColumnModel().getColumn(2).setMaxWidth(40);
        table1.getColumnModel().getColumn(3).setPreferredWidth(65);
        table1.getColumnModel().getColumn(3).setMaxWidth(65);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table1.getColumnModel().getColumn(1).setCellRenderer(center);
        table1.getColumnModel().getColumn(2).setCellRenderer(center);
        table1.getColumnModel().getColumn(3).setCellRenderer(center);

        JScrollPane sp1 = new JScrollPane(table1);
        sp1.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(215, 222, 232)));
        sp1.getViewport().setBackground(Color.WHITE);
        sp1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // --- CỘT 2 (Ở giữa): Tên Phòng Khám | ĐK | Chờ | Khám (Có phòng Tiêm Chủng màu ĐỎ) ---
        String[] cols2 = {"Tên Phòng Khám", "ĐK", "Chờ", "Khám"};
        secondRoomModel = new DefaultTableModel(cols2, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        secondRoomTable = createGridTable(secondRoomModel);
        JTable table2 = secondRoomTable;
        table2.getColumnModel().getColumn(0).setPreferredWidth(260);
        table2.getColumnModel().getColumn(1).setPreferredWidth(35);
        table2.getColumnModel().getColumn(1).setMaxWidth(40);
        table2.getColumnModel().getColumn(2).setPreferredWidth(35);
        table2.getColumnModel().getColumn(2).setMaxWidth(40);
        table2.getColumnModel().getColumn(3).setPreferredWidth(65);
        table2.getColumnModel().getColumn(3).setMaxWidth(65);

        table1.setDefaultRenderer(Object.class, createSelectedRoomRenderer());
        table2.setDefaultRenderer(Object.class, createSelectedRoomRenderer());

        JScrollPane sp2 = new JScrollPane(table2);
        sp2.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(215, 222, 232)));
        sp2.getViewport().setBackground(Color.WHITE);
        sp2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // --- CỘT 3 (Bên phải): Tên Phòng Khám | ĐK | Chờ | Khám (Lưới trống phủ kín khớp ảnh chụp) ---
        String[] cols3 = {"Tên Phòng Khám", "ĐK", "Chờ", "Khám"};
        DefaultTableModel model3 = new DefaultTableModel(cols3, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (int i = 0; i < 20; i++) {
            model3.addRow(new Object[]{"", "", "", ""});
        }
        JTable table3 = createGridTable(model3);
        table3.getColumnModel().getColumn(0).setPreferredWidth(260);
        table3.getColumnModel().getColumn(1).setPreferredWidth(35);
        table3.getColumnModel().getColumn(1).setMaxWidth(40);
        table3.getColumnModel().getColumn(2).setPreferredWidth(35);
        table3.getColumnModel().getColumn(2).setMaxWidth(40);
        table3.getColumnModel().getColumn(3).setPreferredWidth(65);
        table3.getColumnModel().getColumn(3).setMaxWidth(65);

        table3.getColumnModel().getColumn(1).setCellRenderer(center);
        table3.getColumnModel().getColumn(2).setCellRenderer(center);
        table3.getColumnModel().getColumn(3).setCellRenderer(center);

        JScrollPane sp3 = new JScrollPane(table3);
        sp3.setBorder(BorderFactory.createEmptyBorder());
        sp3.getViewport().setBackground(Color.WHITE);
        sp3.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        p.add(sp1);
        p.add(sp2);
        p.add(sp3);

        return p;
    }

    private JPanel buildTabStrip() {
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        strip.setBackground(new Color(230, 238, 246));
        strip.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(185, 200, 218)));

        String[] tabs = {
                "DS Đăng Ký Khám",
                "Lịch Sử Khám Và Điều Trị",
                "Trạng Thái Phòng Khám",
                "Sinh Hiệu",
                "Zalo",
                "Thông Tin Xác Thực",
                "Gia Đình",
                "Quân Nhân",
                "Ghi Chú"
        };

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            String name = tabs[i];

            JPanel tabItem = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    if (index == activeTabIndex) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setColor(Color.WHITE);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        // Gờ xanh dương đậm trên đỉnh
                        g2.setColor(new Color(24, 118, 210));
                        g2.fillRect(0, 0, getWidth(), 3);
                        g2.dispose();
                    } else {
                        super.paintComponent(g);
                    }
                }
            };
            tabItem.setOpaque(index != activeTabIndex);
            tabItem.setBackground(new Color(225, 234, 244));
            tabItem.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(195, 208, 222)));
            tabItem.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JLabel lbl = new JLabel(name);
            lbl.setFont(index == activeTabIndex ? AppFonts.BODY_BOLD : AppFonts.BODY);
            lbl.setForeground(index == activeTabIndex ? new Color(20, 60, 120) : new Color(40, 50, 60));
            lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
            tabItem.add(lbl, BorderLayout.CENTER);

            tabItem.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    switchTab(index);
                }
            });

            strip.add(tabItem);
        }

        return strip;
    }

    private JTable createGridTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int rowHeight = getRowHeight();
                int rowCount = getRowCount();
                int tableHeight = rowCount * rowHeight;
                int viewHeight = getHeight();

                if (tableHeight < viewHeight) {
                    g.setColor(getGridColor());
                    for (int y = tableHeight; y < viewHeight; y += rowHeight) {
                        g.drawLine(0, y, getWidth(), y);
                    }
                    int x = 0;
                    for (int c = 0; c < getColumnCount(); c++) {
                        x += getColumnModel().getColumn(c).getWidth();
                        g.drawLine(x - 1, tableHeight, x - 1, viewHeight);
                    }
                }
            }
        };

        table.setRowHeight(19);
        table.setFont(AppFonts.SMALL);
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 235, 242));
        table.setSelectionBackground(new Color(210, 232, 252));
        table.setSelectionForeground(Color.BLACK);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(100, 22));
        header.setFont(AppFonts.SMALL_BOLD);
        header.setBackground(new Color(236, 241, 247));
        header.setForeground(new Color(30, 40, 50));
        header.setReorderingAllowed(false);

        return table;
    }

    private JButton createFloatingButton(Icon icon, JScrollPane targetSp, boolean isUp) {
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

                g2.setColor(isHovered ? new Color(90, 100, 110, 235) : new Color(125, 135, 145, 210));
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
            JScrollBar vbar = targetSp.getVerticalScrollBar();
            if (vbar != null) {
                int amount = vbar.getBlockIncrement() * (isUp ? -1 : 1);
                vbar.setValue(vbar.getValue() + amount);
            }
        });

        return btn;
    }

    private DefaultTableCellRenderer createSelectedRoomRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, selected, hasFocus, row, column);
                setHorizontalAlignment(column > 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                if (!selected) {
                    String roomName = Objects.toString(table.getValueAt(row, 0), "");
                    boolean highlighted = selectedRoomName != null && selectedRoomName.equalsIgnoreCase(roomName);
                    component.setForeground(highlighted ? new Color(210, 35, 35) : new Color(25, 30, 40));
                    component.setFont(highlighted ? AppFonts.SMALL_BOLD : AppFonts.SMALL);
                    component.setBackground(Color.WHITE);
                }
                return component;
            }
        };
    }
}
