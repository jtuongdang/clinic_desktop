package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cửa sổ Dialog: "ĐH1 - Chẩn Đoán Bệnh" (Bệnh kèm theo).
 * Mở khi bấm vào nút "Bệnh kèm Theo" bên phải danh sách ICD-10 trong tab Xử Trí.
 */
public class SecondaryDiagnosisDialog extends JDialog {

    private final SecondaryDiagnosisPanel panel;

    public SecondaryDiagnosisDialog(Window owner, List<String> initialDiagnoses, Consumer<List<String>> onSaveCallback) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        panel = new SecondaryDiagnosisPanel(initialDiagnoses, onSaveCallback, this::dispose);
        setContentPane(panel);

        pack();
        setLocationRelativeTo(owner);
    }

    public SecondaryDiagnosisPanel getPanel() {
        return panel;
    }
}
