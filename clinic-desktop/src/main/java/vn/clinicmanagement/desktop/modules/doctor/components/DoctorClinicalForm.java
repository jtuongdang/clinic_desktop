package vn.clinicmanagement.desktop.modules.doctor.components;

import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Displays only patient and visit values supplied by the protected doctor queue. */
public class DoctorClinicalForm extends JPanel {
    private final JLabel name = valueLabel();
    private final JLabel patientCode = valueLabel();
    private final JLabel visitId = valueLabel();
    private final JLabel queueNumber = valueLabel();
    private final JLabel room = valueLabel();
    private final JLabel status = valueLabel();

    public DoctorClinicalForm() {
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(8, 4, 8, 4));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(3, 4, 3, 8);
        constraints.weightx = 0;
        addField(constraints, 0, "Người bệnh:", name);
        addField(constraints, 1, "Mã người bệnh:", patientCode);
        addField(constraints, 2, "Mã lượt khám:", visitId);
        addField(constraints, 3, "Số thứ tự:", queueNumber);
        addField(constraints, 4, "Phòng:", room);
        addField(constraints, 5, "Trạng thái:", status);
    }

    public void updatePatient(ReceptionQueueItem item) {
        if (item == null) {
            clear();
            return;
        }
        name.setText(safe(item.patientName()));
        patientCode.setText(safe(item.patientCode()));
        visitId.setText(Integer.toString(item.visitId()));
        queueNumber.setText(safe(item.queueNumberDisplay()));
        room.setText(safe(item.roomName()));
        status.setText(safe(item.status()));
    }

    private void clear() {
        name.setText("");
        patientCode.setText("");
        visitId.setText("");
        queueNumber.setText("");
        room.setText("");
        status.setText("");
    }

    private void addField(GridBagConstraints constraints, int row, String title, JLabel value) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.weightx = 0;
        JLabel label = new JLabel(title);
        label.setFont(AppFonts.BODY);
        add(label, constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        add(value, constraints);
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("");
        label.setFont(AppFonts.BODY_BOLD);
        return label;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
