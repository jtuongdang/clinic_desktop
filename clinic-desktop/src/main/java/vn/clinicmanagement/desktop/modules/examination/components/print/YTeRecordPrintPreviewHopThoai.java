package vn.clinicmanagement.desktop.modules.examination.components.print;

import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ xem và in bệnh án khám bệnh.
 */
public class YTeRecordPrintPreviewHopThoai extends JDialog {

    /** Trường `previewPanel`: xem trước màn hình. */
    private final YTeRecordPrintPreviewManHinh previewPanel;

    public YTeRecordPrintPreviewHopThoai(Window owner) {
        this(owner, null);
    }

    public YTeRecordPrintPreviewHopThoai(Window owner, YTeRecordPrintPreviewManHinh.MedicalRecordData data) {
        super(owner, "Bệnh án khám bệnh - Bản xem trước", ModalityType.MODELESS);
        setSize(1200, 840);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(owner);
        previewPanel = new YTeRecordPrintPreviewManHinh(data);
        setContentPane(previewPanel);
    }

    /** Hàm `bindData`: gắn kết dữ liệu. */
    public void bindData(YTeRecordPrintPreviewManHinh.MedicalRecordData data) {
        previewPanel.bindData(data);
    }
}
