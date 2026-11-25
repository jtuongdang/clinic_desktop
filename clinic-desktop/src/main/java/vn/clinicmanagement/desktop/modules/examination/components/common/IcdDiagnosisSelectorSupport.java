package vn.clinicmanagement.desktop.modules.examination.components.common;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ICDDanhMucLuaChonHopThoai;
import vn.clinicmanagement.domain.examination.ICD;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Hỗ trợ chọn/tìm kiếm Chẩn đoán & mã bệnh ICD-10 (liên kết CSDL `ICD`)
 * dùng chung cho ô mã ICD, ô tên bệnh và nút `[...]` trên màn Chỉ Định Thuốc & Vật Tư.
 */
public final class IcdDiagnosisSelectorSupport {

    private final JComboBox<String> cboCode;
    private final JComboBox<String> cboName;
    private final Component ownerAnchor;
    private final List<ICD> icdCatalog = new ArrayList<>();
    private boolean updatingProgrammatically = false;

    public IcdDiagnosisSelectorSupport(
            JComboBox<String> cboCode,
            JButton btnBrowseCode,
            JComboBox<String> cboName,
            JButton btnBrowseName,
            Supplier<List<ICD>> catalogSupplier,
            Component ownerAnchor) {
        this.cboCode = cboCode;
        this.cboName = cboName;
        this.ownerAnchor = ownerAnchor;

        this.cboCode.setEditable(true);
        this.cboName.setEditable(true);

        if (catalogSupplier != null) {
            try {
                loadCatalog(catalogSupplier.get());
            } catch (Exception ignored) {
            }
        }
        wireComboInteractions(btnBrowseCode, btnBrowseName);
    }

    public void loadCatalog(List<ICD> items) {
        if (items != icdCatalog) {
            List<ICD> next = new ArrayList<>();
            if (items != null) {
                for (ICD item : items) {
                    if (item != null) next.add(item);
                }
            }
            icdCatalog.clear();
            icdCatalog.addAll(next);
        }
        updatingProgrammatically = true;
        try {
            String curCode = getEditorText(cboCode);
            String curName = getEditorText(cboName);
            rebuildComboItems();
            setEditorText(cboCode, curCode);
            setEditorText(cboName, curName);
        } finally {
            updatingProgrammatically = false;
        }
    }

    private void rebuildComboItems() {
        cboCode.removeAllItems();
        cboName.removeAllItems();
        cboCode.addItem("");
        cboName.addItem("");
        for (ICD icd : icdCatalog) {
            cboCode.addItem(safe(icd.getMaICD()));
            cboName.addItem(safe(icd.getTenBenh()));
        }
    }

    private void wireComboInteractions(JButton btnBrowseCode, JButton btnBrowseName) {
        cboCode.addActionListener(e -> {
            if (updatingProgrammatically || !"comboBoxChanged".equals(e.getActionCommand())) return;
            String code = getEditorText(cboCode);
            ICD found = findByCode(code);
            if (found != null) {
                applyIcd(found.getMaICD(), found.getTenBenh());
            }
        });

        cboName.addActionListener(e -> {
            if (updatingProgrammatically || !"comboBoxChanged".equals(e.getActionCommand())) return;
            String name = getEditorText(cboName);
            ICD found = findByName(name);
            if (found != null) {
                applyIcd(found.getMaICD(), found.getTenBenh());
            }
        });

        attachSearchFilter(cboCode, true);
        attachSearchFilter(cboName, false);

        if (btnBrowseCode != null) {
            btnBrowseCode.addActionListener(e -> openLookupDialog(getEditorText(cboCode)));
        }
        if (btnBrowseName != null) {
            btnBrowseName.addActionListener(e -> openLookupDialog(getEditorText(cboName)));
        }
    }

    private void attachSearchFilter(JComboBox<String> combo, boolean byCode) {
        Component editorComp = combo.getEditor().getEditorComponent();
        if (!(editorComp instanceof JTextField tf)) return;
        tf.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onType(); }
            @Override public void removeUpdate(DocumentEvent e) { onType(); }
            @Override public void changedUpdate(DocumentEvent e) { onType(); }

            private void onType() {
                if (updatingProgrammatically || icdCatalog.isEmpty()) return;
                SwingUtilities.invokeLater(() -> {
                    if (updatingProgrammatically) return;
                    String query = tf.getText();
                    String normQ = normalize(query);
                    updatingProgrammatically = true;
                    try {
                        combo.removeAllItems();
                        if (normQ.isEmpty()) {
                            combo.addItem("");
                            for (ICD icd : icdCatalog) {
                                combo.addItem(byCode ? safe(icd.getMaICD()) : safe(icd.getTenBenh()));
                            }
                        } else {
                            for (ICD icd : icdCatalog) {
                                String code = safe(icd.getMaICD());
                                String name = safe(icd.getTenBenh());
                                if (normalize(code).contains(normQ) || normalize(name).contains(normQ)) {
                                    combo.addItem(byCode ? code : name);
                                }
                            }
                        }
                        tf.setText(query);
                        tf.setCaretPosition(query.length());
                        if (combo.isShowing() && tf.hasFocus() && combo.getItemCount() > 0) {
                            combo.setPopupVisible(true);
                        }
                    } finally {
                        updatingProgrammatically = false;
                    }
                });
            }
        });
    }

    public void openLookupDialog(String initialQuery) {
        Window owner = ownerAnchor != null ? SwingUtilities.getWindowAncestor(ownerAnchor) : null;
        String seed = initialQuery == null ? "" : initialQuery.trim();
        new ICDDanhMucLuaChonHopThoai(owner, seed, icdCatalog, this::applyIcd).setVisible(true);
    }

    public void applyIcd(String code, String name) {
        updatingProgrammatically = true;
        try {
            rebuildComboItems();
            setEditorText(cboCode, safe(code));
            setEditorText(cboName, safe(name));
        } finally {
            updatingProgrammatically = false;
        }
    }

    public void setDiagnosis(String diagnosisDisplay) {
        String diag = safe(diagnosisDisplay);
        if (diag.isEmpty()) {
            applyIcd("", "");
            return;
        }
        int sep = diag.indexOf(" - ");
        if (sep > 0) {
            applyIcd(diag.substring(0, sep).trim(), diag.substring(sep + 3).trim());
        } else {
            int dash = diag.indexOf('-');
            if (dash > 0 && dash <= 8 && !diag.substring(0, dash).contains(" ")) {
                applyIcd(diag.substring(0, dash).trim(), diag.substring(dash + 1).trim());
            } else {
                ICD byCode = findByCode(diag);
                if (byCode != null) {
                    applyIcd(byCode.getMaICD(), byCode.getTenBenh());
                } else {
                    applyIcd("", diag);
                }
            }
        }
    }

    public Integer getSelectedIcdId() {
        ICD byCode = findByCode(getEditorText(cboCode));
        if (byCode != null) return byCode.getId();
        ICD byName = findByName(getEditorText(cboName));
        return byName != null ? byName.getId() : null;
    }

    public String getDiagnosisTextOnly() {
        String name = getEditorText(cboName);
        if (!name.isEmpty()) return name;
        return getEditorText(cboCode);
    }

    public String getDiagnosisDisplay() {
        String code = getEditorText(cboCode);
        String name = getEditorText(cboName);
        if (!code.isEmpty() && !name.isEmpty()) return code + " - " + name;
        return !name.isEmpty() ? name : code;
    }

    public List<ICD> getIcdCatalog() {
        return List.copyOf(icdCatalog);
    }

    private ICD findByCode(String code) {
        if (code == null || code.isBlank()) return null;
        String q = code.trim();
        for (ICD icd : icdCatalog) {
            if (q.equalsIgnoreCase(safe(icd.getMaICD()))) return icd;
        }
        return null;
    }

    private ICD findByName(String name) {
        if (name == null || name.isBlank()) return null;
        String q = name.trim();
        for (ICD icd : icdCatalog) {
            if (q.equalsIgnoreCase(safe(icd.getTenBenh()))) return icd;
        }
        return null;
    }

    private static String getEditorText(JComboBox<String> combo) {
        if (combo == null) return "";
        Object val = combo.isEditable() ? combo.getEditor().getItem() : combo.getSelectedItem();
        return val == null ? "" : val.toString().trim();
    }

    private static void setEditorText(JComboBox<String> combo, String text) {
        if (combo == null) return;
        combo.setSelectedItem(text);
        if (combo.isEditable() && combo.getEditor() != null) {
            combo.getEditor().setItem(text);
        }
    }

    private static String normalize(String text) {
        if (text == null) return "";
        String nfd = Normalizer.normalize(text.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replace('đ', 'd');
    }

    private static String safe(String val) {
        return val == null ? "" : val.trim();
    }
}
