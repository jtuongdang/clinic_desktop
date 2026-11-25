package vn.clinicmanagement.desktop.modules.examination.components.print;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Cửa sổ Dialog Xem và In Đơn Thuốc (EHC EMR Print Station).
 * Tiêu đề: "EHC - Bệnh Án Điện Tử  --- Người đăng nhập: henvv --  --- Version: 2022.8.11.1 ---  -- BC505 DONTHUOC2614 - Đơn Thuốc"
 * Kích hoạt khi bác sĩ nhấn [🖨 Đơn Thuốc (F3)] trên tab Thuốc.
 * Khớp 100% media_1793381314310_56e47612.jpg.
 */
public class DonThuocPrintPreviewHopThoai extends JDialog {

    private final DonThuocPrintPreviewManHinh previewPanel;

    public DonThuocPrintPreviewHopThoai(Window owner) {
        this(owner, null);
    }

    public DonThuocPrintPreviewHopThoai(Window owner, DonThuocPrintPreviewManHinh.PrescriptionData data) {
        super(owner, "EHC - Bệnh Án Điện Tử  ---  BC305-DONTHUOCN16 - Đơn Thuốc", ModalityType.MODELESS);

        setIconImage(createCrossIconImage());
        setMinimumSize(new Dimension(900, 620));
        openMaximizedOnOwnerScreen(owner);

        previewPanel = new DonThuocPrintPreviewManHinh(data);
        setContentPane(previewPanel);
    }

    private void openMaximizedOnOwnerScreen(Window owner) {
        GraphicsConfiguration configuration = owner != null
                ? owner.getGraphicsConfiguration()
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle bounds = new Rectangle(configuration.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= insets.left + insets.right;
        bounds.height -= insets.top + insets.bottom;
        setBounds(bounds);
    }

    public void bindData(DonThuocPrintPreviewManHinh.PrescriptionData data) {
        previewPanel.bindData(data);
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
