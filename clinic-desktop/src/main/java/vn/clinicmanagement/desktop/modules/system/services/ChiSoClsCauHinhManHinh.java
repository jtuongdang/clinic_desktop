package vn.clinicmanagement.desktop.modules.system.services;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import vn.clinicmanagement.application.catalog.DichVuCauHinh;

/** Cấu hình chỉ số theo dịch vụ, lưu cùng phiên bản của form cha. */
final class ChiSoClsCauHinhManHinh extends JPanel {
    private final Map<String, JTextComponent> inputs = new LinkedHashMap<>();
    private final Map<String, JCheckBox> options = new LinkedHashMap<>();
    private final Map<String, EditableRows> grids = new LinkedHashMap<>();

    ChiSoClsCauHinhManHinh() {
        super(new BorderLayout());
        setName("labIndexConfiguration");
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        content.add(settings(), BorderLayout.NORTH);

        var tabs = new JTabbedPane();
        addGrid(tabs, "labReferenceRanges", "Khoảng tham chiếu", "Thêm khoảng", "Xóa khoảng",
                "Khoảng giá trị", "Tuổi", "Giới", "Mô tả", "Bất thường", "Thấp", "Cao",
                "Quá thấp", "Quá cao", "In", "In thay kết quả", "Ghi chú");
        addGrid(tabs, "labProcedures", "Quy trình xét nghiệm", "Thêm bước", "Xóa bước",
                "Tên bước", "Hướng dẫn", "Ghi chú");
        addGrid(tabs, "labFormulas", "Công thức", "Thêm công thức", "Xóa công thức",
                "Tên công thức", "Biểu thức", "Ghi chú");
        addGrid(tabs, "labReplacements", "Thay thế kết quả", "Thêm quy tắc", "Xóa quy tắc",
                "Kết quả gốc", "Nội dung thay thế", "Ghi chú");
        content.add(tabs, BorderLayout.CENTER);
        add(new JScrollPane(content), BorderLayout.CENTER);
    }

    private JPanel settings() {
        var settings = new JPanel();
        settings.setLayout(new BoxLayout(settings, BoxLayout.Y_AXIS));
        var metadata = section("Thông số xét nghiệm", 4);
        metadata.add(field("labUnit", "Đơn vị xét nghiệm"));
        metadata.add(field("labSpecimenType", "Loại bệnh phẩm"));
        metadata.add(field("labMethod", "Phương pháp xét nghiệm"));
        metadata.add(field("labAnalyzerGroup", "Nhóm máy xét nghiệm"));
        metadata.add(field("labMolecularGeneType", "Loại gen sinh học phân tử"));
        metadata.add(field("labManualResults", "Kết quả thủ công (phân cách bằng /)"));
        metadata.add(field("labPrintDecimals", "Số chữ số thập phân khi in"));
        metadata.add(field("labPrintIndent", "Số tab lùi phải khi in"));
        metadata.add(field("labBarcodePrefix", "Tiền tố barcode"));
        metadata.add(field("labHenNote", "Ghi chú HEN"));
        metadata.add(field("labTestMethod", "Phương pháp thử"));
        settings.add(metadata);
        settings.add(Box.createVerticalStrut(8));

        var display = section("Nội dung cấu hình", 3);
        display.add(textArea("labNormalText", "Chỉ số xét nghiệm bình thường"));
        display.add(textArea("labFormulaText", "Công thức tính tự động"));
        display.add(textArea("labPrintResultText", "Nội dung in kết quả"));
        settings.add(display);
        settings.add(Box.createVerticalStrut(8));

        var flags = section("Tùy chọn kết quả và in", 3);
        flags.add(check("labPrintBold", "In đậm tên xét nghiệm"));
        flags.add(check("labPrintItalic", "In nghiêng tên xét nghiệm"));
        flags.add(check("labIso", "ISO"));
        flags.add(check("labPrintTubeHen", "In ống HEN"));
        flags.add(check("labSkipTubeHen", "Không in ống HEN"));
        flags.add(check("labPrintSeparateHen", "In riêng HEN"));
        flags.add(check("labSkipEmptyPrint", "Bỏ in khi không có kết quả"));
        flags.add(check("labSkipResultStatusCheck", "Bỏ kiểm tra trạng thái có kết quả"));
        flags.add(check("labClearOnCancellation", "Xóa kết quả khi hủy kết quả"));
        settings.add(flags);
        return settings;
    }

    private static JPanel section(String title, int columns) {
        var panel = new JPanel(new GridLayout(0, columns, 10, 6));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private JPanel field(String key, String label) {
        var field = new JTextField(10);
        return labeled(key, label, field, field);
    }

    private JPanel textArea(String key, String label) {
        var area = new JTextArea(2, 15);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return labeled(key, label, area, new JScrollPane(area));
    }

    private JPanel labeled(String key, String label, JTextComponent input, JComponent editor) {
        input.setName(key);
        inputs.put(key, input);
        var caption = new JLabel(label);
        caption.setLabelFor(input);
        var panel = new JPanel(new BorderLayout(0, 3));
        panel.add(caption, BorderLayout.NORTH);
        panel.add(editor, BorderLayout.CENTER);
        return panel;
    }

    private JCheckBox check(String key, String label) {
        var option = new JCheckBox(label);
        option.setName(key);
        options.put(key, option);
        return option;
    }

    private void addGrid(JTabbedPane tabs, String key, String title, String add, String remove, String... columns) {
        var grid = new EditableRows(key, title, add, remove, columns);
        grids.put(key, grid);
        tabs.addTab(title, grid);
    }

    void load(DichVuCauHinh value) {
        inputs.forEach((key, input) -> input.setText(value.value(key)));
        options.forEach((key, option) -> option.setSelected(Boolean.parseBoolean(value.value(key))));
        grids.forEach((key, grid) -> grid.load(value.value(key)));
    }

    void copyValuesTo(Map<String, String> values) {
        inputs.forEach((key, input) -> putIfPresent(values, key, input.getText().strip()));
        options.forEach((key, option) -> putIfPresent(values, key, option.isSelected() ? "true" : ""));
        grids.forEach((key, grid) -> putIfPresent(values, key, grid.encode()));
    }

    private static void putIfPresent(Map<String, String> values, String key, String value) {
        if (value.isEmpty() && !values.containsKey(key)) return;
        values.put(key, value);
    }

    void setEditable(boolean editable) {
        inputs.values().forEach(input -> input.setEditable(editable));
        options.values().forEach(option -> option.setEnabled(editable));
        grids.values().forEach(grid -> grid.setEditable(editable));
    }

    boolean finishEditing() {
        return grids.values().stream().allMatch(EditableRows::finishEditing);
    }

    private static final class EditableRows extends JPanel {
        private final DefaultTableModel model;
        private final JTable table;
        private final JButton add;
        private final JButton remove;

        private EditableRows(String key, String title, String addLabel, String removeLabel, String[] columns) {
            super(new BorderLayout(0, 5));
            model = new DefaultTableModel(columns, 0);
            table = new JTable(model);
            table.setName(key);
            table.getAccessibleContext().setAccessibleName(title);
            table.setRowHeight(26);
            table.setFillsViewportHeight(true);
            table.putClientProperty("terminateEditOnFocusLost", true);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            if (columns.length > 8) {
                table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                for (int column = 0; column < columns.length; column++) {
                    table.getColumnModel().getColumn(column).setPreferredWidth(column == 0 || column == 3
                            || column == columns.length - 1 ? 145 : 98);
                }
            }
            var scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(0, 190));
            add(scroll, BorderLayout.CENTER);

            add = new JButton(addLabel);
            remove = new JButton(removeLabel);
            remove.setEnabled(false);
            var actions = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
            actions.add(add);
            actions.add(remove);
            add(actions, BorderLayout.SOUTH);
            add.addActionListener(event -> addRow());
            remove.addActionListener(event -> removeRow());
            table.getSelectionModel().addListSelectionListener(event -> updateRemove());
        }

        private void addRow() {
            if (!finishEditing()) return;
            model.addRow(new String[model.getColumnCount()]);
            int row = model.getRowCount() - 1;
            table.setRowSelectionInterval(row, row);
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
            if (table.editCellAt(row, 0)) table.getEditorComponent().requestFocusInWindow();
        }

        private void removeRow() {
            if (!finishEditing()) return;
            int row = table.getSelectedRow();
            if (row >= 0) model.removeRow(table.convertRowIndexToModel(row));
        }

        private void setEditable(boolean editable) {
            table.setEnabled(editable);
            add.setEnabled(editable);
            updateRemove();
        }

        private void updateRemove() {
            remove.setEnabled(table.isEnabled() && table.getSelectedRow() >= 0);
        }

        private boolean finishEditing() {
            return !table.isEditing() || table.getCellEditor().stopCellEditing();
        }

        // Encode individual cells so pasted line breaks, tabs and Vietnamese text round trip.
        private String encode() {
            List<String> rows = new ArrayList<>();
            var encoder = Base64.getEncoder();
            for (int row = 0; row < model.getRowCount(); row++) {
                var cells = new String[model.getColumnCount()];
                for (int column = 0; column < cells.length; column++) {
                    Object value = model.getValueAt(row, column);
                    String text = value == null ? "" : value.toString().strip();
                    cells[column] = encoder.encodeToString(text.getBytes(StandardCharsets.UTF_8));
                }
                if (Arrays.stream(cells).anyMatch(cell -> !cell.isEmpty())) rows.add(String.join("|", cells));
            }
            return String.join("\n", rows);
        }

        private void load(String encoded) {
            model.setRowCount(0);
            if (encoded.isEmpty()) return;
            var decoder = Base64.getDecoder();
            for (String row : encoded.split("\n")) {
                var cells = row.split("\\|", -1);
                if (cells.length != model.getColumnCount()) throw new IllegalArgumentException("Dữ liệu bảng chỉ số CLS không hợp lệ.");
                for (int column = 0; column < cells.length; column++) {
                    cells[column] = new String(decoder.decode(cells[column]), StandardCharsets.UTF_8);
                }
                model.addRow(cells);
            }
        }
    }
}
