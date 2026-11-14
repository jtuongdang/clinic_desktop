package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cửa sổ Dialog: "ĐH1 - Chẩn Đoán Bệnh" (Bệnh kèm theo).
 * Mở khi bấm vào nút "Bệnh kèm Theo" bên phải danh sách ICD-10 trong tab Xử Trí.
 */
public class ThuPhuChanDoanHopThoai extends JDialog {

    /** Trường `panel`: màn hình. */
    private final ThuPhuChanDoanManHinh panel;

    public ThuPhuChanDoanHopThoai(Window owner, List<String> initialDiagnoses, Consumer<List<String>> onSaveCallback) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        panel = new ThuPhuChanDoanManHinh(initialDiagnoses, onSaveCallback, this::dispose);
        setContentPane(panel);

        pack();
        setLocationRelativeTo(owner);
    }

    /** Hàm `getPanel`: lấy màn hình. */
    public ThuPhuChanDoanManHinh getPanel() {
        return panel;
    }
}
