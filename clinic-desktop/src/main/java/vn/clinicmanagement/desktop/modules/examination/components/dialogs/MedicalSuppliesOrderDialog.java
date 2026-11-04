package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ Dialog: "ĐHC - Chỉ Định Vật Tư"
 * Mở khi người dùng nhấn chọn "Chỉ Định Vật Tư" từ Menu Thuốc/VT trên màn hình Khám Bệnh.
 */
public class MedicalSuppliesOrderDialog extends JDialog {

    public MedicalSuppliesOrderDialog(Window owner) {
        super(owner, "ĐHC - Chỉ Định Vật Tư --- Người đăng nhập: sennv --- --- Version: 2022.6.17.1 ---", ModalityType.MODELESS);
        setSize(1200, 720);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(owner);
        setContentPane(new MedicalSuppliesOrderPanel(this));
    }
}
