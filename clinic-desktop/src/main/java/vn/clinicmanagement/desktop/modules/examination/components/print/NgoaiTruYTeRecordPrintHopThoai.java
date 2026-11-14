package vn.clinicmanagement.desktop.modules.examination.components.print;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Cửa sổ Dialog Xem và In Bệnh Án Ngoại Trú (EHC EMR Print Station).
 * Tiêu đề: "EHC - Bệnh án điện tử  --- Người đăng nhập: bennv --  --- Version: 2022.3.11.1 ---  -- BC11-MVJ17690 - Bệnh án khám khám bệnh viện"
 * Kích hoạt khi bác sĩ nhấn [🖨 In Bệnh Án] trên tab Bệnh Án.
 * Khớp 100% media_1793381212647_7bcb9750.jpg.
 */
public class NgoaiTruYTeRecordPrintHopThoai extends JDialog {

    /** Trường `previewPanel`: xem trước màn hình. */
    private final NgoaiTruYTeRecordPrintManHinh previewPanel;

    public NgoaiTruYTeRecordPrintHopThoai(Window owner) {
        this(owner, null);
    }

    public NgoaiTruYTeRecordPrintHopThoai(
            Window owner,
            YTeRecordPrintPreviewManHinh.MedicalRecordData data) {
        super(owner, "Bệnh án ngoại trú - Bản xem trước", ModalityType.MODELESS);

        setIconImage(createCrossIconImage());
        setSize(1200, 840);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(owner);

        previewPanel = new NgoaiTruYTeRecordPrintManHinh(data);
        setContentPane(previewPanel);
    }

    /** Hàm `bindData`: gắn kết dữ liệu. */
    public void bindData(YTeRecordPrintPreviewManHinh.MedicalRecordData data) {
        previewPanel.bindData(data);
    }

    /** Hàm `createCrossIconImage`: tạo `Cross` `Icon` `Image`. */
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
