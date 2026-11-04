package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Cửa sổ Dialog: "EHC - Cập Nhật Thông Tin Ra Viện - Phiếu tóm tắt ra viện".
 * Hiển thị khi người dùng chọn hình thức xử trí "Cấp toa cho về" trong tab Xử Trí.
 */
public class DischargeSummaryDialog extends JDialog {

    private final DischargeSummaryPanel panel;

    public DischargeSummaryDialog(Window owner, Consumer<DischargeSummaryPanel.DischargeSummaryData> onSaveCallback) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        panel = new DischargeSummaryPanel(onSaveCallback, this::dispose);
        setContentPane(panel);

        pack();
        setLocationRelativeTo(owner);
    }

    public DischargeSummaryPanel getPanel() {
        return panel;
    }
}
