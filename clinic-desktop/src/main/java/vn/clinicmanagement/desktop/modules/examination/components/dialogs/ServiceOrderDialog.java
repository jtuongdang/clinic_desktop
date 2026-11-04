package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ Dialog: "ĐHC - Chỉ Định Dịch Vụ"
 */
public class ServiceOrderDialog extends JDialog {

    private final ServiceOrderPanel serviceOrderPanel;

    public ServiceOrderDialog(Window owner) {
        super(owner, "ĐHC - Chỉ Định Dịch Vụ --- Người đăng nhập: sennv --- --- Version: 2022.7.15.3 ---", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(owner);

        serviceOrderPanel = new ServiceOrderPanel(this);
        setContentPane(serviceOrderPanel);
    }

    public ServiceOrderPanel getServiceOrderPanel() {
        return serviceOrderPanel;
    }
}
