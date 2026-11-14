package vn.clinicmanagement.desktop.modules.examination.components.print;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Cửa sổ Dialog In / Xem trước: "In - K3_015_A1_PhuerienTaiKham.doc"
 * Kích hoạt tự động khi bác sĩ nhấn [💾 Lưu] trên màn hình Xử Trí.
 * Khớp 100% media_1793380172130_9595cffb.jpg.
 */
public class TheoTiepLichHenPrintHopThoai extends JDialog {

    public TheoTiepLichHenPrintHopThoai(Window owner) {
        this(owner, "22001770", "NGUYỄN THỊ LIÊN", "38", "Nữ",
                "Xã Mường Lai, Huyện Lục Yên, Tỉnh Yên Bái",
                "15 giờ 57 ngày 19 tháng 08 năm 2022", "",
                "Ngày 12 tháng 08 năm 2022", "bieuCBT");
    }

    public TheoTiepLichHenPrintHopThoai(Window owner, String code, String name, String age,
                                         String gender, String address, String appointTime,
                                         String instructions, String date, String doctor) {
        super(owner, "In - K3_015_A1_PhuerienTaiKham.doc", ModalityType.MODELESS);

        setIconImage(createCrossIconImage());
        setSize(1100, 720);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(owner);

        setContentPane(new TheoTiepLichHenPrintManHinh(code, name, age, gender, address,
                appointTime, instructions, date, doctor));
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
