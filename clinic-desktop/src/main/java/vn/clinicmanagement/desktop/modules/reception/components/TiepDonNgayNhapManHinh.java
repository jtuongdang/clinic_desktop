package vn.clinicmanagement.desktop.modules.reception.components;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/** Ba ô ngày/tháng/năm đồng bộ hai chiều với trường ngày mà nghiệp vụ đang sử dụng. */
final class TiepDonNgayNhapManHinh extends JPanel {
    private final JTextField[] fields = {new JTextField(), new JTextField(), new JTextField()};
    private boolean syncing;

    TiepDonNgayNhapManHinh(JTextField value) {
        setOpaque(false);
        setLayout(new TiepDonBoCucMau(98, 20, new int[]{0, 26}, new int[]{26, 27}, new int[]{53, 45}));
        for (JTextField field : fields) {
            field.setHorizontalAlignment(SwingConstants.CENTER);
            field.setBorder(BorderFactory.createLineBorder(new Color(120, 120, 120)));
            add(field);
            field.getDocument().addDocumentListener(listener(() -> {
                boolean empty = java.util.Arrays.stream(fields).allMatch(f -> f.getText().isBlank());
                value.setText(empty ? "" : fields[0].getText() + "/" + fields[1].getText() + "/" + fields[2].getText());
            }));
        }
        value.getDocument().addDocumentListener(listener(() -> {
            String[] parts = value.getText().split("/", -1);
            for (int i = 0; i < fields.length; i++) fields[i].setText(i < parts.length ? parts[i] : "");
        }));
    }
    private DocumentListener listener(Runnable action) {
        return new DocumentListener() {
            private void update() {
                if (syncing) return;
                syncing = true;
                try { action.run(); } finally { syncing = false; }
            }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        };
    }
}
