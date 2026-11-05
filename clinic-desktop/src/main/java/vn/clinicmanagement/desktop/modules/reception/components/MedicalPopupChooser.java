package vn.clinicmanagement.desktop.modules.reception.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

/**
 * Hộp nhập liệu dạng Dropdown Popup chuẩn y tế EHC:
 * - Khi focus hoặc bấm vào: Đổi màu nền vàng nhạt (#FFFF8C)
 * - Xổ danh sách popup với thanh cuộn mượt mà
 * - Khi chọn một mục: Cập nhật text, đổi lại nền trắng và kích hoạt callback
 */
public class MedicalPopupChooser extends JPanel {

    private final JTextField textField;
    private final JButton btnBrowse;
    private final JPopupMenu popupMenu;
    private final JList<String> list;
    private Consumer<String> onSelectListener;

    public MedicalPopupChooser(String[] items, String initialValue, int width, int height) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.WHITE);

        textField = new JTextField(initialValue);
        textField.setFont(AppFonts.BODY);
        textField.setBackground(Color.WHITE);
        textField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(175, 188, 202), 1),
                new EmptyBorder(1, 4, 1, 4)
        ));

        btnBrowse = new JButton("...");
        btnBrowse.setFont(AppFonts.SMALL);
        btnBrowse.setPreferredSize(new Dimension(20, height));
        btnBrowse.setMargin(new Insets(0, 0, 0, 0));
        btnBrowse.setFocusPainted(false);
        btnBrowse.setCursor(new Cursor(Cursor.HAND_CURSOR));

        add(textField, BorderLayout.CENTER);
        add(btnBrowse, BorderLayout.EAST);

        // --- CẤU HÌNH POPUP MENU & DANH SÁCH ---
        popupMenu = new JPopupMenu();
        popupMenu.setBackground(Color.WHITE);
        popupMenu.setBorder(BorderFactory.createLineBorder(new Color(110, 125, 140), 1));

        list = new JList<>(items);
        list.setFont(AppFonts.BODY);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectionBackground(new Color(26, 112, 192)); // Xanh dương đậm chọn hàng
        list.setSelectionForeground(Color.WHITE);

        // Renderer cho từng dòng trong danh sách popup
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
                lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
                lbl.setFont(AppFonts.BODY);
                if (isSelected) {
                    lbl.setBackground(new Color(26, 112, 192));
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(new Color(25, 30, 40));
                }
                return lbl;
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(Math.max(width + 80, 280), 220));

        popupMenu.add(scrollPane);

        // Khi click vào ô hoặc nút browse -> Mở popup
        MouseAdapter openAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopup();
            }
        };
        textField.addMouseListener(openAdapter);
        btnBrowse.addActionListener(e -> showPopup());

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                textField.setBackground(new Color(255, 255, 140)); // Nền vàng nhạt khi focus
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!popupMenu.isVisible()) {
                    textField.setBackground(Color.WHITE);
                }
            }
        });

        // Xử lý chọn mục từ danh sách
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectCurrentItem();
            }
        });

        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    selectCurrentItem();
                }
            }
        });

        popupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                textField.setBackground(new Color(255, 255, 140));
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                textField.setBackground(Color.WHITE);
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                textField.setBackground(Color.WHITE);
            }
        });
    }

    private void showPopup() {
        if (!popupMenu.isVisible()) {
            list.setSelectedValue(textField.getText(), true);
            popupMenu.show(this, 0, getHeight());
        }
    }

    private void selectCurrentItem() {
        String val = list.getSelectedValue();
        if (val != null) {
            textField.setText(val);
            popupMenu.setVisible(false);
            textField.setBackground(Color.WHITE);
            if (onSelectListener != null) {
                onSelectListener.accept(val);
            }
        }
    }

    public String getText() {
        return textField.getText();
    }

    public void setItems(String[] items) {
        list.setListData(items == null ? new String[0] : items);
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public JTextField getTextField() {
        return textField;
    }

    public void setHighlighted(boolean yellow) {
        textField.setBackground(yellow ? new Color(255, 255, 140) : Color.WHITE);
        if (yellow) {
            textField.selectAll();
            textField.setSelectionColor(new Color(160, 205, 250));
        }
    }

    public void requestFocusAndHighlight() {
        setHighlighted(true);
        textField.requestFocusInWindow();
    }

    public void setOnSelectListener(Consumer<String> listener) {
        this.onSelectListener = listener;
    }
}
