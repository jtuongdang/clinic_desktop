package vn.clinicmanagement.desktop.modules.doctor.components;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.patient.TienSuBenh;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DoctorHistoryPanelTest {
    @Test
    void rendersOnlyQueryRowsInNonEditableHistoryTableAndClearsThem() throws Exception {
        TienSuBenh history = history(501, "Synthetic history detail");
        SwingUtilities.invokeAndWait(() -> {
            DoctorHistoryPanel panel = new DoctorHistoryPanel();
            panel.showHistory(List.of(history));

            JTable table = findTable(panel);
            assertNotNull(table);
            assertEquals(1, table.getRowCount());
            assertEquals(LoaiTienSu.DI_UNG.name(), table.getValueAt(0, 0));
            assertEquals("Synthetic history detail", table.getValueAt(0, 1));
            assertFalse(table.getModel().isCellEditable(0, 0));
            assertFalse(table.getModel().isCellEditable(0, 1));
            assertFalse(containsMutationButton(panel));

            panel.clearHistory();
            assertEquals(0, table.getRowCount());
            assertFalse(containsText(panel, "Synthetic history detail"));
            assertEquals("Chưa có dữ liệu tiền sử bệnh đã xác thực.",
                    findLabel(panel, "Chưa có dữ liệu tiền sử bệnh đã xác thực.").getText());

            panel.showHistory(List.of());
            assertEquals(0, table.getRowCount());
            assertEquals("Chưa có dữ liệu tiền sử bệnh đã xác thực.",
                    findLabel(panel, "Chưa có dữ liệu tiền sử bệnh đã xác thực.").getText());
        });
    }

    private static TienSuBenh history(int patientId, String detail) {
        TienSuBenh history = new TienSuBenh();
        history.setMaBenhNhan(patientId);
        history.setLoaiTienSu(LoaiTienSu.DI_UNG);
        history.setMoTa(detail);
        history.setOwnershipScope(11, 21);
        return history;
    }

    private static JTable findTable(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTable table) return table;
            if (component instanceof Container child) {
                JTable table = findTable(child);
                if (table != null) return table;
            }
        }
        return null;
    }

    private static JLabel findLabel(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) return label;
            if (component instanceof Container child) {
                JLabel label = findLabel(child, text);
                if (label != null) return label;
            }
        }
        return null;
    }

    private static boolean containsText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && label.getText() != null
                    && label.getText().contains(text)) return true;
            if (component instanceof Container child && containsText(child, text)) return true;
        }
        return false;
    }

    private static boolean containsMutationButton(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof AbstractButton button
                    && button.getText() != null
                    && button.getText().matches("(?i).*(lưu|sửa|xóa|mở khóa|unlock|edit|delete).*")) return true;
            if (component instanceof Container child && containsMutationButton(child)) return true;
        }
        return false;
    }
}
