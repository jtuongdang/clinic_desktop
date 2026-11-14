package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

/**
 * Cửa sổ Dialog tra cứu danh mục bệnh ICD-10 (Bệnh lý / Bệnh truyền nhiễm).
 * Kích hoạt khi bấm nút [...] tại ô mã ICD A02 trong tab Xử Trí.
 */
public class ICDDanhMucLuaChonHopThoai extends JDialog {

    /** Trường `panel`: màn hình. */
    private final ICDDanhMucManHinh panel;

    public ICDDanhMucLuaChonHopThoai(Window owner, String initialSearch, BiConsumer<String, String> onSelectedCallback) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        panel = new ICDDanhMucManHinh(initialSearch, onSelectedCallback, this::dispose);
        setContentPane(panel);

        pack();
        setLocationRelativeTo(owner);
    }

    /** Hàm `getPanel`: lấy màn hình. */
    public ICDDanhMucManHinh getPanel() {
        return panel;
    }
}
