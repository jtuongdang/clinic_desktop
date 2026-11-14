package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Cửa sổ Dialog chuyển loại bệnh án (EHC Medical Record Type Dialog).
 * Kích hoạt khi bấm vào mục "Chuyển Loại Bệnh Án" từ menu Khám Bệnh.
 */
public class YTeRecordLoaiHopThoai extends JDialog {

    /** Trường `panel`: màn hình. */
    private final YTeRecordLoaiManHinh panel;

    public YTeRecordLoaiHopThoai(Window owner, Consumer<String> onSelectCallback) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        panel = new YTeRecordLoaiManHinh(onSelectCallback, this::dispose);
        setContentPane(panel);

        pack();
        setLocationRelativeTo(owner);
    }

    /** Hàm `getSelectedType`: lấy duocchon loai. */
    public String getSelectedType() {
        return panel.getSelectedType();
    }
}
