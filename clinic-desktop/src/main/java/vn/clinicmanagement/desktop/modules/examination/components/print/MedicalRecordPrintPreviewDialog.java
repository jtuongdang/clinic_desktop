package vn.clinicmanagement.desktop.modules.examination.components.print;

import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ xem và in bệnh án khám bệnh.
 */
public class MedicalRecordPrintPreviewDialog extends JDialog {

    private final MedicalRecordPrintPreviewPanel previewPanel;

    public MedicalRecordPrintPreviewDialog(Window owner) {
        this(owner, null);
    }

    public MedicalRecordPrintPreviewDialog(Window owner, MedicalRecordPrintPreviewPanel.MedicalRecordData data) {
        super(owner, "Bệnh án khám bệnh - Bản xem trước", ModalityType.MODELESS);
        setSize(1200, 840);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(owner);
        previewPanel = new MedicalRecordPrintPreviewPanel(data);
        setContentPane(previewPanel);
    }

    public void bindData(MedicalRecordPrintPreviewPanel.MedicalRecordData data) {
        previewPanel.bindData(data);
    }
}
