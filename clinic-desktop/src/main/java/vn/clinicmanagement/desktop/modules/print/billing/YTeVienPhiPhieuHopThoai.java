package vn.clinicmanagement.desktop.modules.print.billing;


import vn.clinicmanagement.desktop.modules.print.examination.*;
import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ Xem và In: "BẢNG KÊ CHI PHÍ KHÁM BỆNH" (Mẫu 01/KCCB chuẩn Bộ Y Tế).
 * Kích hoạt khi nhấn vào mục "Đăng Ký Khám Bệnh, Chữa Bệnh" từ Menu In.
 * Khớp 100% chi tiết trong ảnh chụp mẫu với cỡ chữ nâng cấp to rõ.
 */
public class YTeVienPhiPhieuHopThoai extends JDialog {

    private final YTeVienPhiPhieuManHinh billingSlipPanel;

    public YTeVienPhiPhieuHopThoai(Window owner) {
        this(owner, null);
    }

    public YTeVienPhiPhieuHopThoai(Window owner, YTeVienPhiPhieuManHinh.BillingSlipData data) {
        super(owner,
                data != null && data.patientName() != null && !data.patientName().isBlank()
                        ? "Bảng kê chi phí khám bệnh, chữa bệnh (01/KCCB) — " + data.patientName() + " | Mã VP: " + data.feeCode()
                        : "Bảng kê chi phí khám bệnh, chữa bệnh (Mẫu 01/KCCB)",
                ModalityType.MODELESS);
        setSize(1180, 840);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(owner);
        this.billingSlipPanel = new YTeVienPhiPhieuManHinh(data);
        setContentPane(billingSlipPanel);
    }

    public YTeVienPhiPhieuManHinh getBillingSlipPanel() {
        return billingSlipPanel;
    }
}
