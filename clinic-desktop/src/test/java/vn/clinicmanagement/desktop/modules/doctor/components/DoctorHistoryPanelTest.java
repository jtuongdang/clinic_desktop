package vn.clinicmanagement.desktop.modules.doctor.components;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.doctor.model.PatientExamDto;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DoctorHistoryPanelTest {
    private static final String NO_VERIFIED_HISTORY_MESSAGE =
            "Chưa có dữ liệu lịch sử khám đã xác thực.";

    @Test
    void patientDtoDoesNotCreateHistoryAndPatientChangesDoNotRetainPriorData() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DoctorHistoryPanel panel = new DoctorHistoryPanel();
            PatientExamDto firstPatient = patient("11/11/2026", "Synthetic Department One", "Synthetic Diagnosis One");
            PatientExamDto secondPatient = patient("12/11/2026", "Synthetic Department Two", "Synthetic Diagnosis Two");

            panel.updateHistory(firstPatient);
            assertNoVerifiedHistory(panel, firstPatient);

            JLabel status = findStatusLabel(panel);
            status.setText(firstPatient.getDiagnosis());
            panel.updateHistory(secondPatient);
            assertNoVerifiedHistory(panel, firstPatient);
            assertNoVerifiedHistory(panel, secondPatient);

            status.setText(secondPatient.getDiagnosis());
            panel.updateHistory(null);
            assertNoVerifiedHistory(panel, firstPatient);
            assertNoVerifiedHistory(panel, secondPatient);
        });
    }

    private static PatientExamDto patient(String admissionDate, String department, String diagnosis) {
        return new PatientExamDto(
                "Synthetic Patient",
                "SYN-001",
                "Nữ",
                "1990",
                "36 tuổi",
                "Synthetic Address",
                "SYN-BHYT",
                "80%",
                "31/12/2026",
                admissionDate,
                "Synthetic Doctor",
                department,
                diagnosis,
                "SYN-ICD"
        );
    }

    private static void assertNoVerifiedHistory(DoctorHistoryPanel panel, PatientExamDto patient) {
        JLabel status = findStatusLabel(panel);
        assertNotNull(status, "Verified-history status label was not found");
        assertEquals(NO_VERIFIED_HISTORY_MESSAGE, status.getText());
        assertFalse(containsComponent(panel, JTable.class));
        assertFalse(containsText(panel, patient.getAdmissionDate()));
        assertFalse(containsText(panel, patient.getDepartment()));
        assertFalse(containsText(panel, patient.getDiagnosis()));
    }

    private static JLabel findStatusLabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label
                    && NO_VERIFIED_HISTORY_MESSAGE.equals(label.getText())) {
                return label;
            }
            if (component instanceof Container child) {
                JLabel label = findStatusLabel(child);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    private static boolean containsText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && label.getText() != null
                    && label.getText().contains(text)) {
                return true;
            }
            if (component instanceof Container child && containsText(child, text)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsComponent(Container container, Class<? extends Component> type) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                return true;
            }
            if (component instanceof Container child && containsComponent(child, type)) {
                return true;
            }
        }
        return false;
    }
}
