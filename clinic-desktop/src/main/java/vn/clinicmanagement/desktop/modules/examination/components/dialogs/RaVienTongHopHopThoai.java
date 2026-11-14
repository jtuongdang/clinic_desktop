package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Cửa sổ Dialog: "EHC - Cập Nhật Thông Tin Ra Viện - Phiếu tóm tắt ra viện".
 * Hiển thị khi người dùng chọn hình thức xử trí "Cấp toa cho về" trong tab Xử Trí.
 */
public class RaVienTongHopHopThoai extends JDialog {

    /** Trường `panel`: màn hình. */
    private final RaVienTongHopManHinh panel;

    public RaVienTongHopHopThoai(Window owner, Consumer<RaVienTongHopManHinh.DischargeSummaryData> onSaveCallback) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        panel = new RaVienTongHopManHinh(onSaveCallback, this::dispose);
        setContentPane(panel);

        pack();
        setLocationRelativeTo(owner);
    }

    /** Hàm `getPanel`: lấy màn hình. */
    public RaVienTongHopManHinh getPanel() {
        return panel;
    }
}
