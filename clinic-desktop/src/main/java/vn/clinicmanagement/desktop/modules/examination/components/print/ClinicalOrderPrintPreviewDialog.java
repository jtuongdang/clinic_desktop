package vn.clinicmanagement.desktop.modules.examination.components.print;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Cửa sổ Dialog Xem và In "PHIẾU CHỈ ĐỊNH CẬN LÂM SÀNG"
 * Khớp 100% media_1793380792703_8babf04f.jpg.
 * Tiêu đề: "EHC - Bệnh Án Điện Tử  --- Người đăng nhập: bennv --  --- Version: 2022.3.11.1 ---  -- BC11-CDCLSC2223834 - Phiếu chỉ định cận lâm sàng"
 */
public class ClinicalOrderPrintPreviewDialog extends JDialog {

    public ClinicalOrderPrintPreviewDialog(Window owner) {
        super(owner, "EHC - Bệnh Án Điện Tử  --- Người đăng nhập: bennv --  --- Version: 2022.3.11.1 ---  -- BC11-CDCLSC2223834 - Phiếu chỉ định cận lâm sàng", ModalityType.MODELESS);

        setIconImage(createCrossIconImage());
        setSize(1200, 840);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(owner);

        setContentPane(new ClinicalOrderPrintPreviewPanel());
    }

    private static Image createCrossIconImage() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(215, 45, 45));
        g2.fillRect(6, 2, 4, 12);
        g2.fillRect(2, 6, 12, 4);
        g2.dispose();
        return img;
    }
}
