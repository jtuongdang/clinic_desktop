package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.domain.examination.ICD;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Cửa sổ Dialog tra cứu danh mục bệnh ICD-10 (Bệnh lý / Bệnh truyền nhiễm).
 */
public class ICDDanhMucLuaChonHopThoai extends JDialog {

    private final ICDDanhMucManHinh panel;

    public ICDDanhMucLuaChonHopThoai(Window owner, String initialSearch, BiConsumer<String, String> onSelectedCallback) {
        this(owner, initialSearch, null, onSelectedCallback);
    }

    public ICDDanhMucLuaChonHopThoai(
            Window owner,
            String initialSearch,
            List<ICD> icdCatalog,
            BiConsumer<String, String> onSelectedCallback) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        panel = new ICDDanhMucManHinh(initialSearch, icdCatalog, onSelectedCallback, this::dispose);
        setContentPane(panel);

        pack();
        setLocationRelativeTo(owner);
    }

    public ICDDanhMucManHinh getPanel() {
        return panel;
    }
}
