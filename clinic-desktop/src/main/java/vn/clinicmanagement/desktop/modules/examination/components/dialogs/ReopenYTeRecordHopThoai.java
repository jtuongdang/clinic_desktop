package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ReopenYTeRecordManHinh.ReopenRecordRequest;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Cửa sổ Dialog: "EHC - Mở Lại Bệnh Án".
 * Kích hoạt khi người dùng nhấn chuột phải vào bệnh nhân đã hoàn thành khám và chọn "Mở Lại Bệnh Án".
 * Khớp 100% chi tiết trong ảnh chụp thực tế media_1793455095013_78d4d94b.jpg.
 */
public class ReopenYTeRecordHopThoai extends JDialog {

    /** Trường `panel`: màn hình. */
    private final ReopenYTeRecordManHinh panel;

    public ReopenYTeRecordHopThoai(Window owner, AdminPatientItem patient,
                                    Consumer<ReopenRecordRequest> onSaveCallback) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        panel = new ReopenYTeRecordManHinh(patient, onSaveCallback, this::dispose);
        setContentPane(panel);

        pack();
        setLocationRelativeTo(owner);
    }

    /** Hàm `getPanel`: lấy màn hình. */
    public ReopenYTeRecordManHinh getPanel() {
        return panel;
    }
}
