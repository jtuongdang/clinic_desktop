package vn.clinicmanagement.desktop.modules.doctor.components;

import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/** Team queue for the authenticated facility and department. */
public class DoctorQueueSidebar extends JPanel {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DefaultTableModel queueTableModel;
    private final JTable patientQueueTable;
    private final JLabel queueStatus;
    private Consumer<ReceptionQueueItem> onPatientSelected;
    private boolean replacingRows;

    public DoctorQueueSidebar() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(210, 218, 226)));

        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setBackground(new Color(245, 247, 250));
        header.setBorder(new EmptyBorder(6, 8, 6, 8));
        JLabel title = new JLabel("Hàng đợi · " + LocalDate.now(BUSINESS_ZONE).format(DATE_FORMAT));
        title.setFont(AppFonts.BODY_BOLD);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        queueTableModel = new DefaultTableModel(new Object[]{"Người bệnh", "STT", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        patientQueueTable = new JTable(queueTableModel);
        patientQueueTable.setRowHeight(42);
        patientQueueTable.setShowGrid(false);
        patientQueueTable.setIntercellSpacing(new Dimension(0, 1));
        patientQueueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        patientQueueTable.getTableHeader().setFont(AppFonts.BODY_BOLD);
        patientQueueTable.getTableHeader().setPreferredSize(new Dimension(100, 24));
        patientQueueTable.getTableHeader().setBackground(new Color(238, 242, 246));
        patientQueueTable.getColumnModel().getColumn(1).setMaxWidth(54);
        patientQueueTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        patientQueueTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, selected, focused, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(AppFonts.SMALL);
                return label;
            }
        });
        patientQueueTable.getColumnModel().getColumn(0).setCellRenderer(new javax.swing.table.TableCellRenderer() {
            private ReceptionQueueItem item;
            private boolean selected;
            private final JComponent component = new JComponent() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setColor(selected ? new Color(0, 120, 215) : Color.WHITE);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    if (item != null) {
                        g.setFont(AppFonts.SMALL_BOLD);
                        g.setColor(selected ? Color.WHITE : new Color(20, 20, 20));
                        g.drawString(safe(item.patientName()), 8, 17);
                        g.setFont(AppFonts.SMALL);
                        g.setColor(selected ? new Color(225, 238, 255) : new Color(80, 80, 80));
                        g.drawString(safe(item.patientCode()), 8, 34);
                    }
                    g.dispose();
                }
            };

            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                item = value instanceof ReceptionQueueItem queueItem ? queueItem : null;
                this.selected = selected;
                return component;
            }
        });
        patientQueueTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || replacingRows) return;
            int viewRow = patientQueueTable.getSelectedRow();
            ReceptionQueueItem selected = viewRow < 0
                    ? null
                    : (ReceptionQueueItem) queueTableModel.getValueAt(
                            patientQueueTable.convertRowIndexToModel(viewRow), 0);
            if (onPatientSelected != null) onPatientSelected.accept(selected);
        });

        JScrollPane queueScroll = new JScrollPane(patientQueueTable);
        queueScroll.getViewport().setBackground(Color.WHITE);
        queueScroll.setBorder(BorderFactory.createEmptyBorder());
        add(queueScroll, BorderLayout.CENTER);

        queueStatus = new JLabel("Chưa tải hàng đợi.", SwingConstants.CENTER);
        queueStatus.setFont(AppFonts.SMALL);
        queueStatus.setForeground(new Color(100, 110, 120));
        queueStatus.setBorder(new EmptyBorder(4, 4, 4, 4));
        add(queueStatus, BorderLayout.SOUTH);
    }

    public void setOnPatientSelectedListener(Consumer<ReceptionQueueItem> listener) {
        onPatientSelected = listener;
    }

    public void showLoading(LocalDate date) {
        setDate(date);
        clearRows();
        queueStatus.setText("Đang tải hàng đợi...");
    }

    public void setQueue(List<ReceptionQueueItem> queue, LocalDate date) {
        setDate(date);
        clearRows();
        for (ReceptionQueueItem item : queue) {
            queueTableModel.addRow(new Object[]{item, safe(item.queueNumberDisplay()), safe(item.status())});
        }
        queueStatus.setText(queue.isEmpty() ? "Không có lượt khám đang hoạt động." : queue.size() + " lượt đang hoạt động");
    }

    public void showUnavailable() {
        clearRows();
        queueStatus.setText("Không thể tải hàng đợi.");
    }

    public void clearQueue() {
        clearRows();
        queueStatus.setText("Hàng đợi đã được làm mới.");
    }

    private void setDate(LocalDate date) {
        Component header = getComponent(0);
        if (header instanceof JPanel panel && panel.getComponent(0) instanceof JLabel title) {
            title.setText("Hàng đợi · " + date.format(DATE_FORMAT));
        }
    }

    private void clearRows() {
        replacingRows = true;
        try {
            patientQueueTable.clearSelection();
            queueTableModel.setRowCount(0);
        } finally {
            replacingRows = false;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

}
