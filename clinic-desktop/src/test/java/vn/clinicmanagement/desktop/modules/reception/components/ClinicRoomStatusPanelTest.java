package vn.clinicmanagement.desktop.modules.reception.components;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.reception.ClinicRoomQueueSummary;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.*;

class ClinicRoomStatusPanelTest {

    @Test
    void setQueueReplacesTheQueueTableRows() throws Exception {
        onEdt(() -> {
            ClinicRoomStatusPanel panel = new ClinicRoomStatusPanel();
            panel.setQueue(List.of(
                    new ReceptionQueueItem(100, 200, "PAT-200", "Synthetic Patient",
                            7, "PK Nhi 1", 12, "012", "Chờ khám",
                            LocalDateTime.of(2026, 11, 6, 9, 15))
            ));

            TableModel model = panel.getQueueTable().getModel();
            assertEquals(1, model.getRowCount());
            assertEquals("09:15 06/11/2026", model.getValueAt(0, 0));
            assertEquals("012", model.getValueAt(0, 1));
            assertEquals("Chờ khám", model.getValueAt(0, 2));

            panel.setQueue(List.of());
            assertEquals(0, model.getRowCount());
            return null;
        });
    }

    @Test
    void roomSummariesPopulateBothColumnsAndHighlightTheSelectedRoom() throws Exception {
        onEdt(() -> {
            ClinicRoomStatusPanel panel = new ClinicRoomStatusPanel();
            panel.setRoomSummaries(List.of(
                    new ClinicRoomQueueSummary(1, "PK Nhi 1", "Nhi", 8, 3, 2),
                    new ClinicRoomQueueSummary(2, "PK Nhi 2", "Nhi", 5, 1, 3),
                    new ClinicRoomQueueSummary(3, "PK Mắt", "Mắt", 4, 2, 1)
            ));

            TableModel first = panel.getFirstRoomTable().getModel();
            TableModel second = panel.getSecondRoomTable().getModel();
            assertEquals(2, first.getRowCount());
            assertEquals("PK Nhi 1", first.getValueAt(0, 0));
            assertEquals(8, first.getValueAt(0, 1));
            assertEquals(3, first.getValueAt(0, 2));
            assertEquals("PK Mắt", second.getValueAt(0, 0));
            assertEquals(4, second.getValueAt(0, 1));
            assertEquals(1, second.getValueAt(0, 3));

            panel.setSelectedClinicRoom("PK Mắt");
            JTable table = panel.getSecondRoomTable();
            Component renderer = table.getCellRenderer(0, 0)
                    .getTableCellRendererComponent(table, table.getValueAt(0, 0), false, false, 0, 0);
            assertEquals(new Color(210, 35, 35), renderer.getForeground());

            panel.setRoomSummaries(List.of());
            assertEquals(0, first.getRowCount());
            assertEquals(0, second.getRowCount());
            return null;
        });
    }

    private static <T> T onEdt(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
