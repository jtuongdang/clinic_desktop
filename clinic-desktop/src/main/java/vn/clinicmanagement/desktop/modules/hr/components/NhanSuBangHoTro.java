package vn.clinicmanagement.desktop.modules.hr.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tiện ích dùng chung cho các panel con trong phân hệ Quản Lý Nhân Sự.
 */
public final class NhanSuBangHoTro {
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    public static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");

    public enum ViewSection {
        ALL_EMPLOYEES("Danh Sách Nhân Viên"),
        ACTIVE_EMPLOYEES("Đang Hoạt Động"),
        LOCKED_EMPLOYEES("Đã Khóa"),
        SCHEDULES("Lịch Làm Việc / Ca Trực"),
        ATTENDANCE("Bảng Chấm Công Tháng"),
        PAYROLL("Kỳ Lương & Bảng Lương");

        public final String label;

        ViewSection(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private NhanSuBangHoTro() {}

    public static void addMenuItem(JPopupMenu menu, String title, Runnable action) {
        JMenuItem item = new JMenuItem(title);
        item.setFont(UngDungPhongChu.BODY);
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    public static JScrollPane scrollOf(JTable tbl) {
        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    public static DefaultTableModel readOnlyModel(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    public static JTable createStyledTable(DefaultTableModel model) {
        JTable tbl = new JTable(model);
        tbl.setRowHeight(25);
        tbl.setFont(UngDungPhongChu.BODY);
        tbl.setGridColor(new Color(235, 238, 242));
        tbl.setShowVerticalLines(false);
        tbl.setSelectionBackground(new Color(214, 232, 250));
        tbl.setSelectionForeground(Color.BLACK);
        tbl.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        tbl.getTableHeader().setBackground(new Color(240, 244, 248));
        tbl.getTableHeader().setReorderingAllowed(false);
        return tbl;
    }

    public static void exportTableCsv(Component parent, JTable target, String fileName, Consumer<String> onStatus) {
        if (GraphicsEnvironment.isHeadless() || target == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(fileName));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        try (var w = Files.newBufferedWriter(fc.getSelectedFile().toPath(), StandardCharsets.UTF_8)) {
            w.write('\ufeff');
            for (int r = -1; r < target.getRowCount(); r++) {
                for (int c = 0; c < target.getColumnCount(); c++) {
                    if (c > 0) w.write(',');
                    String v = Objects.toString(r < 0 ? target.getColumnModel().getColumn(c).getHeaderValue() : target.getValueAt(r, c), "");
                    if (v.startsWith("=") || v.startsWith("+") || v.startsWith("-") || v.startsWith("@")) v = "'" + v;
                    w.write("\"" + v.replace("\"", "\"\"") + "\"");
                }
                w.newLine();
            }
            if (onStatus != null) onStatus.accept("Đã xuất CSV thành công.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Không thể xuất CSV: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
