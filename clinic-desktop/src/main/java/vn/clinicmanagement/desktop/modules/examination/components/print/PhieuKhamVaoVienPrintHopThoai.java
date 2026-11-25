package vn.clinicmanagement.desktop.modules.examination.components.print;

import vn.clinicmanagement.desktop.modules.examination.components.print.PhieuKhamVaoVienPrintManHinh.AdmissionFormData;

import javax.swing.*;
import java.awt.*;

/**
 * Hộp thoại modal hiển thị "EHC - Bệnh Án Điện Tử: Phiếu khám bệnh vào viện"
 * khi xử trí Nhập viện.
 */
public class PhieuKhamVaoVienPrintHopThoai extends JDialog {

    private final PhieuKhamVaoVienPrintManHinh previewPanel;

    public PhieuKhamVaoVienPrintHopThoai(Window owner, AdmissionFormData data) {
        super(owner, "EHC - Bệnh Án Điện Tử --- Phiếu khám bệnh vào viện", ModalityType.APPLICATION_MODAL);
        this.previewPanel = new PhieuKhamVaoVienPrintManHinh(data);
        setContentPane(previewPanel);
        setSize(1020, 740);
        setLocationRelativeTo(owner);
    }

    public PhieuKhamVaoVienPrintManHinh getPreviewPanel() {
        return previewPanel;
    }
}
