package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.DonThuocYeuCauManHinh.MedicineSearchItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bộ gợi ý tự động (Auto-complete) cho ô tìm kiếm thuốc trên màn hình Chỉ Định Thuốc.
 */
final class DonThuocAutocompleteSupport {

    private final JTextField txtDrugName;
    private final List<MedicineSearchItem> allMedicines;
    private final Consumer<MedicineSearchItem> onSelected;

    private final JPopupMenu suggestPopup;
    private final DefaultListModel<MedicineSearchItem> suggestListModel;
    private final JList<MedicineSearchItem> suggestList;
    private boolean settingTextProgrammatically = false;

    DonThuocAutocompleteSupport(
            JTextField txtDrugName,
            JButton btnBrowseDrug,
            List<MedicineSearchItem> allMedicines,
            Consumer<MedicineSearchItem> onSelected) {
        this.txtDrugName = txtDrugName;
        this.allMedicines = allMedicines;
        this.onSelected = onSelected;

        suggestPopup = new JPopupMenu();
        suggestPopup.setBorder(BorderFactory.createLineBorder(new Color(135, 148, 165), 1));
        suggestPopup.setFocusable(false);

        suggestListModel = new DefaultListModel<>();
        suggestList = new JList<>(suggestListModel);
        suggestList.setFont(UngDungPhongChu.SMALL);
        suggestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(new EmptyBorder(2, 6, 2, 6));
                l.setFont(UngDungPhongChu.SMALL);
                if (isSelected) {
                    l.setBackground(new Color(0, 120, 215));
                    l.setForeground(Color.WHITE);
                } else {
                    l.setBackground(Color.WHITE);
                    l.setForeground(new Color(20, 25, 30));
                }
                return l;
            }
        });

        JScrollPane scroll = new JScrollPane(suggestList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(475, 340));
        suggestPopup.add(scroll);

        attachListeners(btnBrowseDrug);
    }

    private void attachListeners(JButton btnBrowseDrug) {
        txtDrugName.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onQueryChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onQueryChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onQueryChanged(); }
        });

        txtDrugName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!suggestPopup.isShowing()) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        onQueryChanged();
                    }
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    int next = suggestList.getSelectedIndex() + 1;
                    if (next < suggestListModel.size()) {
                        suggestList.setSelectedIndex(next);
                        suggestList.ensureIndexIsVisible(next);
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int prev = suggestList.getSelectedIndex() - 1;
                    if (prev >= 0) {
                        suggestList.setSelectedIndex(prev);
                        suggestList.ensureIndexIsVisible(prev);
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    MedicineSearchItem item = suggestList.getSelectedValue();
                    if (item != null && onSelected != null) {
                        onSelected.accept(item);
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    suggestPopup.setVisible(false);
                    e.consume();
                }
            }
        });

        suggestList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                MedicineSearchItem item = suggestList.getSelectedValue();
                if (item != null && onSelected != null) {
                    onSelected.accept(item);
                }
            }
        });

        if (btnBrowseDrug != null) {
            btnBrowseDrug.addActionListener(e -> showAll());
        }
    }

    void showAll() {
        suggestListModel.clear();
        for (MedicineSearchItem item : allMedicines) {
            suggestListModel.addElement(item);
        }
        if (!suggestListModel.isEmpty()) {
            suggestList.setSelectedIndex(0);
        }
        if (txtDrugName.isShowing()) {
            suggestPopup.show(txtDrugName, 0, txtDrugName.getHeight());
        }
    }

    void setDrugTextProgrammatically(String text) {
        settingTextProgrammatically = true;
        try {
            txtDrugName.setText(text);
        } finally {
            settingTextProgrammatically = false;
        }
    }

    void hidePopup() {
        if (suggestPopup.isShowing()) {
            suggestPopup.setVisible(false);
        }
    }

    void populateForSearchDemo(String query) {
        settingTextProgrammatically = true;
        try {
            txtDrugName.setText(query);
        } finally {
            settingTextProgrammatically = false;
        }
        List<MedicineSearchItem> matches = filterMedicines(query);
        suggestListModel.clear();
        for (MedicineSearchItem item : matches) {
            suggestListModel.addElement(item);
        }
        if (!suggestListModel.isEmpty()) {
            suggestList.setSelectedIndex(0);
        }
        if (txtDrugName.isShowing()) {
            suggestPopup.show(txtDrugName, 0, txtDrugName.getHeight());
        }
    }

    JPopupMenu getSuggestPopup() {
        return suggestPopup;
    }

    private void onQueryChanged() {
        if (settingTextProgrammatically) return;
        SwingUtilities.invokeLater(() -> {
            String q = txtDrugName.getText().trim();
            if (q.isEmpty()) {
                suggestPopup.setVisible(false);
                return;
            }
            List<MedicineSearchItem> matches = filterMedicines(q);
            if (matches.isEmpty()) {
                suggestPopup.setVisible(false);
                return;
            }
            suggestListModel.clear();
            for (MedicineSearchItem item : matches) {
                suggestListModel.addElement(item);
            }
            suggestList.setSelectedIndex(0);
            suggestList.ensureIndexIsVisible(0);

            if (txtDrugName.isShowing()) {
                if (!suggestPopup.isShowing()) {
                    suggestPopup.show(txtDrugName, 0, txtDrugName.getHeight());
                } else {
                    suggestPopup.pack();
                }
                txtDrugName.requestFocusInWindow();
            }
        });
    }

    private List<MedicineSearchItem> filterMedicines(String query) {
        String cleanQuery = removeAccents(query.trim().toLowerCase());
        List<MedicineSearchItem> result = new ArrayList<>();
        for (MedicineSearchItem item : allMedicines) {
            String raw = item.code + " " + item.name;
            String normalized = removeAccents(raw.toLowerCase());
            if (raw.toLowerCase().contains(query.toLowerCase()) || normalized.contains(cleanQuery)) {
                result.add(item);
            }
        }
        return result;
    }

    private static String removeAccents(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replace('đ', 'd').replace('Đ', 'D');
    }
}
