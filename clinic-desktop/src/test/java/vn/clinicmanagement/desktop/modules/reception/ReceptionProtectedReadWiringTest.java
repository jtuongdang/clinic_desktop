package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceptionProtectedReadWiringTest {
    @Test
    void receptionPanelRoutesProtectedReadsThroughApplicationHandlers() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java"));

        assertFalse(source.contains("services.rooms().getActiveRooms()"));
        assertFalse(source.contains("services.patients().search("));
        assertFalse(source.contains("services.queue().getQueue("));
        assertFalse(source.contains("services.queue().getRoomSummaries("));
        assertTrue(source.contains("services.getActiveRooms().handle(new GetActiveReceptionRoomsQuery"));
        assertTrue(source.contains("services.getQueue().handle(new GetReceptionQueueQuery"));
        assertTrue(source.contains("services.searchPatients().handle("));
    }
}
