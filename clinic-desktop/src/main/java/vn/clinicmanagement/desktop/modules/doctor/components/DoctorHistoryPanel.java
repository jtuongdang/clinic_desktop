package vn.clinicmanagement.desktop.modules.doctor.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.domain.patient.TienSuBenh;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Read-only patient-level history returned by the separately authorized history query. */
public class DoctorHistoryPanel extends JPanel {
    private static final String EMPTY_MESSAGE = "Chưa có dữ liệu tiền sử bệnh đã xác thực.";

    private final JLabel historyStatus = new JLabel(EMPTY_MESSAGE, SwingConstants.CENTER);
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new Object[]{"Loại tiền sử", "Nội dung"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable historyTable = new JTable(historyModel);

    public DoctorHistoryPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 218, 226)));

        JLabel title = new JLabel("Tiền sử bệnh");
        title.setFont(AppFonts.BODY_BOLD);
        title.setForeground(new Color(24, 75, 120));
        title.setBorder(new EmptyBorder(6, 4, 4, 4));
        add(title, BorderLayout.NORTH);

        historyStatus.setFont(AppFonts.BODY);
        historyStatus.setForeground(new Color(100, 110, 120));
        historyStatus.setBorder(new EmptyBorder(12, 8, 12, 8));
        add(historyStatus, BorderLayout.CENTER);

        historyTable.setFont(AppFonts.BODY);
        historyTable.setRowHeight(28);
        historyTable.setFillsViewportHeight(true);
        historyTable.getTableHeader().setFont(AppFonts.BODY_BOLD);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public void showLoading() {
        clearRows();
        historyStatus.setText("Đang tải tiền sử bệnh...");
    }

    public void showHistory(List<TienSuBenh> histories) {
        clearRows();
        if (histories.isEmpty()) {
            historyStatus.setText(EMPTY_MESSAGE);
            return;
        }
        for (TienSuBenh history : histories) {
            historyModel.addRow(new Object[]{
                    history.getLoaiTienSu().name(),
                    history.getMoTa() == null ? "" : history.getMoTa()
            });
        }
        remove(historyStatus);
        add(new JScrollPane(historyTable), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showUnavailable() {
        clearRows();
        historyStatus.setText("Không thể tải tiền sử bệnh.");
    }

    public void clearHistory() {
        clearRows();
        historyStatus.setText(EMPTY_MESSAGE);
    }

    private void clearRows() {
        historyModel.setRowCount(0);
        removeAllExceptTitle();
        add(historyStatus, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void removeAllExceptTitle() {
        Component title = getComponent(0);
        removeAll();
        add(title, BorderLayout.NORTH);
    }
}
