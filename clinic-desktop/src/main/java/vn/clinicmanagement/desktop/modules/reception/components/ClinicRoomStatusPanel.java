package vn.clinicmanagement.desktop.modules.reception.components;

import vn.clinicmanagement.application.reception.ClinicRoomQueueSummary;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Reception queue and live room status, populated from the reception query. */
public class ClinicRoomStatusPanel extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private final DefaultTableModel queueModel = model("Số tiếp đón", "Mã BN", "Họ tên", "Phòng khám", "Giờ đăng ký", "Trạng thái");
    private final DefaultTableModel[] roomModels = {
            model("Tên phòng", "ĐK", "Chờ", "Khám"), model("Tên phòng", "ĐK", "Chờ", "Khám"),
            model("Tên phòng", "ĐK", "Chờ", "Khám")};
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private String selectedRoomName;

    public ClinicRoomStatusPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        tabs.setBackground(new Color(230, 238, 246));
        JButton queue = new JButton("DS Đăng Ký Khám");
        JButton rooms = new JButton("Trạng Thái Phòng Khám");
        queue.addActionListener(e -> cards.show(content, "queue"));
        rooms.addActionListener(e -> cards.show(content, "rooms"));
        tabs.add(queue); tabs.add(rooms);
        add(tabs, BorderLayout.NORTH);

        content.add(new JScrollPane(new JTable(queueModel)), "queue");
        JPanel roomGrid = new JPanel(new GridLayout(1, 3, 2, 0));
        for (int i = 0; i < roomModels.length; i++) {
            JTable table = new JTable(roomModels[i]);
            table.setRowHeight(21);
            table.setAutoCreateRowSorter(true);
            table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                         boolean focus, int row, int column) {
                    Component cell = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                    String roomName = String.valueOf(t.getValueAt(row, 0));
                    if (!selected && roomName.equalsIgnoreCase(String.valueOf(selectedRoomName))) cell.setForeground(new Color(190, 35, 35));
                    else if (!selected) cell.setForeground(new Color(30, 40, 50));
                    setHorizontalAlignment(column > 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                    return cell;
                }
            });
            roomGrid.add(new JScrollPane(table));
        }
        content.add(roomGrid, "rooms");
        add(content, BorderLayout.CENTER);
        cards.show(content, "rooms");
    }

    public void setSelectedClinicRoom(String roomName) { selectedRoomName = roomName; repaint(); }

    public void setQueue(List<ReceptionQueueItem> items) {
        queueModel.setRowCount(0);
        if (items == null) return;
        for (ReceptionQueueItem item : items) {
            queueModel.addRow(new Object[]{item.queueNumberDisplay(), item.patientCode(), item.patientName(),
                    item.roomName(), item.registeredAt() == null ? "" : item.registeredAt().format(TIME), displayStatus(item.status())});
        }
    }

    public void setRoomSummaries(List<ClinicRoomQueueSummary> rooms) {
        for (DefaultTableModel model : roomModels) model.setRowCount(0);
        if (rooms == null) return;
        int index = 0;
        for (ClinicRoomQueueSummary room : rooms) {
            roomModels[index++ % roomModels.length].addRow(new Object[]{room.roomName(), room.registeredCount(),
                    room.waitingCount(), room.examiningCount()});
        }
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    }

    private static String displayStatus(String value) {
        if (value == null) return "";
        return switch (value) {
            case "CHO_KHAM" -> "Chờ khám";
            case "DANG_KHAM" -> "Đang khám";
            case "DA_KHAM" -> "Đã khám";
            case "DA_HUY" -> "Đã hủy";
            default -> value;
        };
    }
}
