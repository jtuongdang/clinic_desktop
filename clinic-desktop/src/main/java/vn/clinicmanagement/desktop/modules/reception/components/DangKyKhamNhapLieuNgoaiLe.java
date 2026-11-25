package vn.clinicmanagement.desktop.modules.reception.components;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Lỗi nhập liệu an toàn để hiển thị, kèm đúng ô cần sửa trên form tiếp đón. */
public final class DangKyKhamNhapLieuNgoaiLe extends IllegalArgumentException {
    private final JComponent input;

    public DangKyKhamNhapLieuNgoaiLe(String message, JComponent input) {
        super(message);
        this.input = input;
    }

    /** Ô nhận con trỏ sau khi người dùng đóng thông báo. */
    public JComponent getInput() { return input; }

    /** Khôi phục viền gốc khi sửa nội dung hoặc bắt đầu hồ sơ mới. */
    static void clearInput(JComponent component) {
        component.putClientProperty("JComponent.outline", null);
        if (component.getClientProperty("reception.originalValidationBorder") instanceof javax.swing.border.Border[] stored) {
            component.setBorder(stored[0]);
            component.putClientProperty("reception.originalValidationBorder", null);
        }
    }

    /** Đánh dấu lỗi và đưa con trỏ về ô nhập; tự bỏ dấu lỗi khi nội dung được sửa. */
    public void focusInput() {
        input.putClientProperty("JComponent.outline", "error");
        if (input.getClientProperty("reception.originalValidationBorder") == null) {
            javax.swing.border.Border original = input.getBorder();
            input.putClientProperty("reception.originalValidationBorder", new javax.swing.border.Border[]{original});
            input.setBorder(new javax.swing.border.AbstractBorder() {
                @Override public java.awt.Insets getBorderInsets(java.awt.Component component) {
                    return original == null ? new java.awt.Insets(1, 1, 1, 1) : original.getBorderInsets(component);
                }
                @Override public void paintBorder(java.awt.Component component, java.awt.Graphics graphics, int x, int y, int width, int height) {
                    if (original != null) original.paintBorder(component, graphics, x, y, width, height);
                    graphics.setColor(new java.awt.Color(200, 45, 45));
                    graphics.drawRect(x, y, width - 1, height - 1);
                }
            });
        }
        input.scrollRectToVisible(input.getVisibleRect());
        input.requestFocusInWindow();
        if (input instanceof JTextField field) {
            field.selectAll();
            if (field.getClientProperty("reception.validationListener") == null) {
                field.getDocument().addDocumentListener(new DocumentListener() {
                    private void clear() { clearInput(field); }
                    @Override public void insertUpdate(DocumentEvent e) { clear(); }
                    @Override public void removeUpdate(DocumentEvent e) { clear(); }
                    @Override public void changedUpdate(DocumentEvent e) { clear(); }
                });
                field.putClientProperty("reception.validationListener", Boolean.TRUE);
            }
        } else if (input instanceof JComboBox<?> combo && combo.getClientProperty("reception.validationListener") == null) {
            combo.addActionListener(e -> clearInput(combo));
            combo.putClientProperty("reception.validationListener", Boolean.TRUE);
        }
    }
}
