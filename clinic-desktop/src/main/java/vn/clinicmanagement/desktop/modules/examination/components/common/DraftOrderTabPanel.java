package vn.clinicmanagement.desktop.modules.examination.components.common;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Tab bảng hiển thị danh sách nháp ("Đơn Đang Kê" / "Phiếu Đang Kê") dùng chung nút xóa dòng.
 */
public class DraftOrderTabPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;

    public DraftOrderTabPanel(String removeButtonText, String[] columns, IntConsumer onRemoveRow) {
        super(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(new Color(245, 248, 252));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 218, 228)));

        JButton btnRemove = new JButton(removeButtonText);
        btnRemove.setFont(UngDungPhongChu.SMALL_BOLD);
        btnRemove.setForeground(new Color(185, 25, 25));
        btnRemove.setBackground(Color.WHITE);
        btnRemove.setFocusPainted(false);
        bar.add(btnRemove);
        add(bar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setFont(UngDungPhongChu.BODY);
        table.setRowHeight(22);
        table.setShowGrid(true);
        table.setGridColor(new Color(225, 230, 238));
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnRemove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && onRemoveRow != null) {
                onRemoveRow.accept(row);
            }
        });
    }

    public void setRows(List<Object[]> rows) {
        tableModel.setRowCount(0);
        if (rows != null) {
            for (Object[] row : rows) {
                tableModel.addRow(row);
            }
        }
    }
}
