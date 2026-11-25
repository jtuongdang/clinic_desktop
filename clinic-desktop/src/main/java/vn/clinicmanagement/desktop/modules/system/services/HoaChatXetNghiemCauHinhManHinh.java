package vn.clinicmanagement.desktop.modules.system.services;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh;

/** Cùng một định mức với mục Vật tư hao phí trên tab Thông tin dịch vụ. */
final class HoaChatXetNghiemCauHinhManHinh extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Mã", "Hóa chất / vật tư", "Kho", "Đơn vị", "Quy cách", "Lượng định mức"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JLabel status = new JLabel("Đang tải định mức…");
    private final JButton configure = new JButton("Chọn hóa chất và vật tư");

    HoaChatXetNghiemCauHinhManHinh(Runnable openEditor) {
        super(new BorderLayout(0, 8));
        setName("labReagentConfiguration");
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        var actions = new JPanel(new BorderLayout(8, 0));
        var controls = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        controls.add(configure);
        actions.add(status, BorderLayout.CENTER);
        actions.add(controls, BorderLayout.EAST);
        configure.addActionListener(event -> openEditor.run());
        add(actions, BorderLayout.NORTH);

        var table = new JTable(model);
        table.setName("labReagentRows");
        table.getAccessibleContext().setAccessibleName("Định mức hóa chất và vật tư xét nghiệm");
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(190);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    void showDefinition(String definition, String legacy) {
        model.setRowCount(0);
        try {
            var value = VatTuHaoPhiCauHinh.decode(definition);
            for (var line : value.lines()) {
                model.addRow(new Object[]{line.code(), line.name(), line.warehouseName(), line.unit(),
                        line.packing(), line.quantity().stripTrailingZeros()});
            }
            if (!value.lines().isEmpty()) {
                status.setText(value.lines().size() + " dòng định mức · " + value.stage().title()
                        + (value.rounding() ? " · Làm tròn số lượng" : "")
                        + (value.stage() == VatTuHaoPhiCauHinh.Stage.RESULT ? "" : " · Chưa tự động ghi tiêu hao ở bước đã chọn"));
            } else {
                status.setText(definition.isBlank() && !legacy.isBlank()
                        ? "Có cấu hình cũ. Mở bộ chọn để cập nhật định mức."
                        : "Dịch vụ chưa có định mức hóa chất hoặc vật tư.");
            }
        } catch (IllegalArgumentException ex) {
            status.setText(ex.getMessage());
        }
    }

    void setEditable(boolean editable) {
        configure.setEnabled(editable);
    }
}
