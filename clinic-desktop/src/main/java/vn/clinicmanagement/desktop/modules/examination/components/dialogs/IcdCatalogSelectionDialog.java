package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

/**
 * Cửa sổ Dialog tra cứu danh mục bệnh ICD-10 (Bệnh lý / Bệnh truyền nhiễm).
 * Kích hoạt khi bấm nút [...] tại ô mã ICD A02 trong tab Xử Trí.
 */
public class IcdCatalogSelectionDialog extends JDialog {

    private final IcdCatalogPanel panel;

    public IcdCatalogSelectionDialog(Window owner, String initialSearch, BiConsumer<String, String> onSelectedCallback) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        panel = new IcdCatalogPanel(initialSearch, onSelectedCallback, this::dispose);
        setContentPane(panel);

        pack();
        setLocationRelativeTo(owner);
    }

    public IcdCatalogPanel getPanel() {
        return panel;
    }
}
