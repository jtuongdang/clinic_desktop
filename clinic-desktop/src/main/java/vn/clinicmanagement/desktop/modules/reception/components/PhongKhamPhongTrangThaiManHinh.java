package vn.clinicmanagement.desktop.modules.reception.components;

import vn.clinicmanagement.application.reception.PhongKhamPhongHangDoiTongHop;
import vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang;
import vn.clinicmanagement.application.reception.TiepDonLichSuKhamMatHang;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBieuTuong;

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
public class PhongKhamPhongTrangThaiManHinh extends JPanel {

    private int activeTabIndex = 2; // Mặc định hiển thị Tab 2: "Trạng Thái Phòng Khám" (Khớp 100% ảnh sau khi chọn dịch vụ)
    private String selectedRoomName = "Phòng Tiêm Chủng"; // Phòng đang được chọn trên form

    private final CardLayout cardLayout;
    private final JPanel cardsContainer;
    private final JPanel tabStrip;
    private DefaultTableModel queueModel;
    private DefaultTableModel historyModel;
    private DefaultTableModel firstRoomModel;
    private DefaultTableModel secondRoomModel;
    private JTable queueTable;
    private JTable historyTable;
    private JTable firstRoomTable;
    private JTable secondRoomTable;

    private List<TiepDonHangDoiMatHang> printableQueue = List.of();

    /** Lượt được chọn trên bảng danh sách, không suy đoán từ tên người bệnh. */
    public Integer getSelectedVisitId() {
        int row = queueTable.getSelectedRow();
        if (row < 0) return null;
        int index = queueTable.convertRowIndexToModel(row);
        return index < printableQueue.size() ? printableQueue.get(index).visitId() : null;
    }

    public PhongKhamPhongTrangThaiManHinh() {
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

        // Card 1: Bảng "Lịch Sử Khám Và Điều Trị" của bệnh nhân từ CSDL
        cardsContainer.add(buildPatientHistoryView(), "TAB_PATIENT_HISTORY");

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

    public void setQueue(List<TiepDonHangDoiMatHang> items) {
        Objects.requireNonNull(items, "items");
        printableQueue = List.copyOf(items);
        queueModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        for (TiepDonHangDoiMatHang item : items) {
            queueModel.addRow(new Object[]{
                    item.registeredAt() == null ? "" : item.registeredAt().format(formatter),
                    item.queueNumberDisplay() == null || item.queueNumberDisplay().isBlank()
                            ? Integer.toString(item.queueNumber()) : item.queueNumberDisplay(),
                    item.status(),
                    item.patientCode(),
                    item.patientName(),
                    item.roomName(),
                    item.registeredAt() == null ? "" : item.registeredAt().toLocalDate().toString()
            });
        }
    }

    public void setRoomSummaries(List<PhongKhamPhongHangDoiTongHop> rooms) {
        Objects.requireNonNull(rooms, "rooms");
        firstRoomModel.setRowCount(0);
        secondRoomModel.setRowCount(0);
        int firstColumnSize = (rooms.size() + 1) / 2;
        for (int i = 0; i < rooms.size(); i++) {
            PhongKhamPhongHangDoiTongHop room = rooms.get(i);
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

    public void setPatientHistory(List<TiepDonLichSuKhamMatHang> items) {
        if (historyModel == null) return;
        historyModel.setRowCount(0);
        if (items == null || items.isEmpty()) {
            historyTable.repaint();
            return;
        }
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (TiepDonLichSuKhamMatHang item : items) {
            String timeStr = item.registeredAt() != null ? item.registeredAt().format(dtFmt) : "";
            String stt = item.queueNumberDisplay() != null && !item.queueNumberDisplay().isBlank()
                    ? item.queueNumberDisplay() : Integer.toString(item.queueNumber());
            String roomPart = item.roomName() != null && !item.roomName().isBlank() ? " - " + item.roomName() : "";
            String reasonPart = item.reason() != null && !item.reason().isBlank() ? " (" + item.reason() + ")" : "";
            String col0 = timeStr + (stt.isBlank() ? "" : " | STT " + stt) + roomPart + reasonPart;
            String statusLabel = switch (item.status() == null ? "" : item.status()) {
                case "HOAN_TAT" -> "Hoàn tất";
                case "CHO_KHAM" -> "Chờ khám";
                case "DANG_KHAM" -> "Đang khám";
                case "CHO_CLS" -> "Chờ CLS";
                case "DA_HUY" -> "Đã hủy";
                default -> item.status();
            };
            String followUpStr = item.followUpDate() != null ? item.followUpDate().format(dFmt) : "";
            String visitDateStr = item.visitDate() != null ? item.visitDate().format(dFmt) : "";
            String doctorStr = item.doctorName() != null ? item.doctorName() : "";
            String completedStr = item.completedAt() != null ? item.completedAt().format(dtFmt) : "";
            historyModel.addRow(new Object[]{
                    col0,
                    stt,
                    statusLabel,
                    followUpStr,
                    visitDateStr,
                    doctorStr,
                    completedStr
            });
        }
        historyTable.repaint();
    }

    public JTable getQueueTable() {
        return queueTable;
    }

    public JTable getHistoryTable() {
        return historyTable;
    }

    JTable getFirstRoomTable() {
        return firstRoomTable;
    }

    JTable getSecondRoomTable() {
        return secondRoomTable;
    }

    public void switchTab(int index) {
        this.activeTabIndex = index;
        if (index == 2) {
            cardLayout.show(cardsContainer, "TAB_ROOM_STATUS");
        } else if (index == 1) {
            cardLayout.show(cardsContainer, "TAB_PATIENT_HISTORY");
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
                "Mã Bệnh Nhân",
                "Họ Và Tên",
                "Phòng Khám",
                "Ngày Khám"
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
     * Giao diện Card 1: "Lịch Sử Khám Và Điều Trị" của bệnh nhân từ CSDL.
     */
    private JPanel buildPatientHistoryView() {
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

        historyModel = model;
        historyTable = createGridTable(model);
        JTable table = historyTable;
        table.getColumnModel().getColumn(0).setPreferredWidth(260);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setPreferredWidth(160);
        table.getColumnModel().getColumn(6).setPreferredWidth(130);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);
        table.getColumnModel().getColumn(3).setCellRenderer(center);
        table.getColumnModel().getColumn(4).setCellRenderer(center);
        table.getColumnModel().getColumn(6).setCellRenderer(center);

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
                @Override public void doLayout() {
                    super.doLayout();
                    for (Component child : getComponents())
                        if (child instanceof JLabel label) TiepDonKieuMau.fitLabel(label);
                }
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth() - 1, h = getHeight() - 1;
                    java.awt.geom.Path2D shape = new java.awt.geom.Path2D.Double();
                    shape.moveTo(1, h);
                    shape.lineTo(index == 0 || index == activeTabIndex ? 18 : 1, 5);
                    shape.quadTo(index == 0 || index == activeTabIndex ? 21 : 1, 1, 26, 1);
                    shape.lineTo(w - 6, 1);
                    shape.quadTo(w, 1, w, 7);
                    shape.lineTo(w, h);
                    shape.closePath();
                    g2.setPaint(new GradientPaint(0, 0, new Color(244, 250, 254), 0, h,
                            index == activeTabIndex ? new Color(230, 241, 250) : new Color(160, 188, 216)));
                    g2.fill(shape);
                    g2.setColor(new Color(95, 134, 163));
                    g2.draw(shape);
                    g2.dispose();
                }
            };
            tabItem.setPreferredSize(new Dimension(new int[]{130, 150, 142, 65, 39, 122, 61, 75, 60}[index], 24));
            tabItem.setOpaque(false);
            tabItem.setBackground(new Color(225, 234, 244));
            tabItem.setBorder(BorderFactory.createEmptyBorder());
            tabItem.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JLabel lbl = new JLabel(name);
            lbl.setFont(UngDungPhongChu.TIEP_DON);
            lbl.setForeground(Color.BLACK);
            lbl.setBorder(new EmptyBorder(3, index == 0 || index == activeTabIndex ? 18 : 6, 3, 6));
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
        table.setFont(UngDungPhongChu.TIEP_DON);
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionBackground(new Color(210, 232, 252));
        table.setSelectionForeground(Color.BLACK);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(100, 22));
        header.setFont(UngDungPhongChu.TIEP_DON);
        header.setBackground(Color.WHITE);
        header.setForeground(new Color(30, 40, 50));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focused, int row, int column) {
                super.getTableCellRendererComponent(t, value, false, false, row, column);
                setFont(UngDungPhongChu.TIEP_DON);
                setBackground(Color.WHITE);
                setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 5, 0, 3));
                return this;
            }
        });

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
                    component.setForeground(highlighted && column == 0 ? new Color(210, 35, 35) : new Color(25, 30, 40));
                    component.setFont(highlighted ? UngDungPhongChu.TIEP_DON : UngDungPhongChu.TIEP_DON);
                    component.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                }
                return component;
            }
        };
    }
}
