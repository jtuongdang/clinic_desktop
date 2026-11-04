package vn.clinicmanagement.desktop.core.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Bảng dữ liệu chuẩn cho phần mềm phòng khám / bệnh viện.
 */
public class StyledTable extends JTable {

    public StyledTable(DefaultTableModel model) {
        super(model);
        initStyle();
    }

    public static JTable create(DefaultTableModel model) {
        return new StyledTable(model);
    }

    private void initStyle() {
        setRowHeight(30);
        setFont(AppFonts.BODY);
        setShowGrid(true);
        setGridColor(new Color(225, 230, 235));
        setSelectionBackground(new Color(210, 230, 250));
        setSelectionForeground(Color.BLACK);

        JTableHeader header = getTableHeader();
        header.setPreferredSize(new Dimension(100, 32));
        header.setFont(AppFonts.BODY_BOLD);
        header.setBackground(new Color(236, 240, 245));
        header.setForeground(new Color(40, 50, 60));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        if (getColumnCount() > 0) {
            getColumnModel().getColumn(0).setCellRenderer(center);
        }
    }
}
