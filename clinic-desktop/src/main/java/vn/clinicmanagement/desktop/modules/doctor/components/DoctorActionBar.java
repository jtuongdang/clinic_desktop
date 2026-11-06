package vn.clinicmanagement.desktop.modules.doctor.components;

import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/** Read-only patient and visit summary with queue refresh. */
public class DoctorActionBar extends JPanel {
    private final JLabel selectionSummary = new JLabel("Chưa chọn người bệnh.");
    private final JButton refreshButton = new JButton("Làm mới hàng đợi");

    public DoctorActionBar() {
        setLayout(new BorderLayout(8, 0));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 212, 222)),
                new EmptyBorder(4, 8, 4, 8)));

        selectionSummary.setFont(AppFonts.BODY_BOLD);
        add(selectionSummary, BorderLayout.CENTER);

        refreshButton.setFont(AppFonts.SMALL);
        refreshButton.setFocusPainted(false);
        add(refreshButton, BorderLayout.EAST);
    }

    public void updatePatient(ReceptionQueueItem item) {
        if (item == null) {
            selectionSummary.setText("Chưa chọn người bệnh.");
            return;
        }
        selectionSummary.setText(String.format(
                "%s  ·  %s  ·  Lượt khám %d  ·  %s  ·  %s",
                safe(item.patientName()), safe(item.patientCode()), item.visitId(),
                safe(item.queueNumberDisplay()), safe(item.status())));
    }

    public void setOnRefresh(ActionListener listener) {
        for (ActionListener existing : refreshButton.getActionListeners()) {
            refreshButton.removeActionListener(existing);
        }
        refreshButton.addActionListener(listener);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
