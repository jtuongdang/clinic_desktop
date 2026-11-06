package vn.clinicmanagement.desktop.modules.doctor.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.modules.doctor.model.PatientExamDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cột trái: Hàng đợi bệnh nhân chờ khám & chỉ số tiếp nhận
 */
public class DoctorQueueSidebar extends JPanel {

    private final JTable patientQueueTable;
    private final DefaultTableModel queueTableModel;
    private final List<PatientExamDto> patients;
    private Consumer<PatientExamDto> onPatientSelected;

    public DoctorQueueSidebar(List<PatientExamDto> patients) {
        this.patients = patients;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(210, 218, 226)));

        // 1. Bộ lọc ngày & đối tượng tiếp đón phía trên
        JPanel filterHeader = new JPanel(new BorderLayout(4, 0));
        filterHeader.setBackground(new Color(255, 255, 215)); // Light yellow
        filterHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 208, 218)),
                new EmptyBorder(3, 6, 3, 6)
        ));

        JLabel lblFilter = new JLabel("13/11/18 >> 13/11/18 - BH(115...");
        lblFilter.setFont(AppFonts.BODY);
        JLabel lblFilterSearch = new JLabel("🔍");
        lblFilterSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));

        filterHeader.add(lblFilter, BorderLayout.CENTER);
        filterHeader.add(lblFilterSearch, BorderLayout.EAST);
        add(filterHeader, BorderLayout.NORTH);

        // 2. Bảng hàng đợi bệnh nhân
        String[] qCols = {"Tên Bệnh Nhân", "STT"};
        queueTableModel = new DefaultTableModel(qCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        for (PatientExamDto p : patients) {
            queueTableModel.addRow(new Object[]{p, "1"});
        }

        patientQueueTable = new JTable(queueTableModel);
        patientQueueTable.setRowHeight(38);
        patientQueueTable.setShowGrid(false);
        patientQueueTable.setIntercellSpacing(new Dimension(0, 1));
        patientQueueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        patientQueueTable.getTableHeader().setFont(AppFonts.BODY_BOLD);
        patientQueueTable.getTableHeader().setPreferredSize(new Dimension(100, 24));
        patientQueueTable.getTableHeader().setBackground(new Color(238, 242, 246));

        // Renderer 2 dòng cho bệnh nhân (vẽ trực tiếp bằng Graphics2D sắc nét)
        patientQueueTable.getColumnModel().getColumn(0).setCellRenderer(new javax.swing.table.TableCellRenderer() {
            private PatientExamDto item;
            private boolean selected;
            private final JComponent comp = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(selected ? new Color(0, 120, 215) : Color.WHITE);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    if (item != null) {
                        g2.setFont(AppFonts.SMALL_BOLD);
                        g2.setColor(selected ? Color.WHITE : new Color(20, 20, 20));
                        g2.drawString(item.getName(), 8, 16);
                        g2.setFont(AppFonts.SMALL);
                        g2.setColor(selected ? new Color(225, 238, 255) : new Color(80, 80, 80));
                        g2.drawString(item.getCode(), 8, 31);
                    }
                    g2.dispose();
                }
            };
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                this.item = value instanceof PatientExamDto ? (PatientExamDto) value : null;
                this.selected = isSelected;
                return comp;
            }
        });

        // Cột số lượng / badge
        patientQueueTable.getColumnModel().getColumn(1).setMaxWidth(30);
        patientQueueTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(AppFonts.SMALL);
                if (isSelected) {
                    lbl.setBackground(new Color(0, 120, 215));
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(Color.GRAY);
                }
                return lbl;
            }
        });

        // Sự kiện chọn bệnh nhân
        patientQueueTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = patientQueueTable.getSelectedRow();
                if (row >= 0 && row < patients.size()) {
                    if (onPatientSelected != null) {
                        onPatientSelected.accept(patients.get(row));
                    }
                }
            }
        });

        JScrollPane queueScroll = new JScrollPane(patientQueueTable);
        queueScroll.setColumnHeaderView(patientQueueTable.getTableHeader());
        queueScroll.getViewport().setBackground(Color.WHITE);
        queueScroll.setBorder(BorderFactory.createEmptyBorder());
        add(queueScroll, BorderLayout.CENTER);

        // 3. Footer bộ lọc dưới (2 hộp chọn có viền và mũi tên ▾ bên phải)
        JPanel leftFooter = new JPanel(new GridLayout(2, 1, 0, 2));
        leftFooter.setBackground(new Color(240, 243, 248));
        leftFooter.setBorder(new EmptyBorder(2, 0, 2, 0));

        leftFooter.add(createDropdownBox("(Tất cả bệnh nhân) - 1176", new Color(25, 45, 110), true));
        leftFooter.add(createDropdownBox("(Tất cả khoa)", new Color(145, 35, 35), false));
        add(leftFooter, BorderLayout.SOUTH);
    }

    private JPanel createDropdownBox(String text, Color textColor, boolean bold) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createLineBorder(new Color(185, 192, 202), 1));
        box.setPreferredSize(new Dimension(100, 24));

        JLabel lbl = new JLabel(text);
        lbl.setFont(bold ? AppFonts.BODY_BOLD : AppFonts.BODY);
        lbl.setForeground(textColor);
        lbl.setBorder(new EmptyBorder(0, 6, 0, 4));

        JLabel arrow = new JLabel("▾", SwingConstants.CENTER);
        arrow.setFont(AppFonts.SMALL);
        arrow.setForeground(new Color(90, 90, 90));
        arrow.setOpaque(true);
        arrow.setBackground(new Color(238, 240, 244));
        arrow.setPreferredSize(new Dimension(18, 22));
        arrow.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(195, 200, 208)));

        box.add(lbl, BorderLayout.CENTER);
        box.add(arrow, BorderLayout.EAST);
        return box;
    }

    public void setOnPatientSelectedListener(Consumer<PatientExamDto> listener) {
        this.onPatientSelected = listener;
    }

    public void selectPatient(int index) {
        if (index >= 0 && index < patients.size()) {
            patientQueueTable.setRowSelectionInterval(index, index);
            if (onPatientSelected != null) {
                onPatientSelected.accept(patients.get(index));
            }
        }
    }
}
