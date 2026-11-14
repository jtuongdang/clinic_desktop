package vn.clinicmanagement.desktop.modules.examination.components.print;

import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ Xem và In: "BẢNG KÊ CHI PHÍ KHÁM BỆNH" (Mẫu 01/KCCB chuẩn Bộ Y Tế).
 * Kích hoạt khi nhấn vào mục "Đăng Ký Khám Bệnh, Chữa Bệnh" từ Menu In.
 * Khớp 100% chi tiết trong ảnh chụp mẫu với cỡ chữ nâng cấp to rõ.
 */
public class YTeVienPhiPhieuHopThoai extends JDialog {

    /** Trường `billingSlipPanel`: vienphi phiếu màn hình. */
    private final YTeVienPhiPhieuManHinh billingSlipPanel;

    public YTeVienPhiPhieuHopThoai(Window owner) {
        this(owner, null);
    }

    public YTeVienPhiPhieuHopThoai(Window owner, YTeVienPhiPhieuManHinh.BillingSlipData data) {
        super(owner, "DHC - Bệnh Án Điện Tử  --- Người đăng nhập: sennv -- ... --- Version: 2022.6.10.2 --- BCS21-VP214332-0 - Bảng kê chi phí khám bệnh, chữa bệnh (CV6156)", ModalityType.MODELESS);
        setSize(1180, 840);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(owner);
        this.billingSlipPanel = new YTeVienPhiPhieuManHinh(data);
        setContentPane(billingSlipPanel);
    }

    /** Hàm `getBillingSlipPanel`: lấy vienphi phiếu màn hình. */
    public YTeVienPhiPhieuManHinh getBillingSlipPanel() {
        return billingSlipPanel;
    }
}
