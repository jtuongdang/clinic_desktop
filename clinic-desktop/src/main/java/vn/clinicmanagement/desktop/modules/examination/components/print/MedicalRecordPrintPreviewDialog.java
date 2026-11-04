package vn.clinicmanagement.desktop.modules.examination.components.print;

import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ Dialog Xem và In Bệnh Án Khám Bệnh (Chuyên khoa Tai - Mũi - Họng).
 * Khớp 100% tiêu đề và giao diện trong ảnh chụp thực tế:
 * "EHC - Bệnh Án Điện Tử  --- Người đăng nhập: tiennv --  --- Version: 2022.8.17.1 ---  -- BC23-MR317707 - Bệnh an Tai Mũi Họng"
 */
public class MedicalRecordPrintPreviewDialog extends JDialog {

    public MedicalRecordPrintPreviewDialog(Window owner) {
        super(owner, "EHC - Bệnh Án Điện Tử  --- Người đăng nhập: tiennv --  --- Version: 2022.8.17.1 ---  -- BC23-MR317707 - Bệnh an Tai Mũi Họng", ModalityType.MODELESS);
        setSize(1200, 840);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(owner);
        setContentPane(new MedicalRecordPrintPreviewPanel());
    }
}
