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
        Path moduleSourcePath = Path.of("src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java");
        Path sourcePath = Files.exists(moduleSourcePath)
                ? moduleSourcePath
                : Path.of("clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java");
        String source = Files.readString(sourcePath);

        assertFalse(source.contains("services.rooms().getActiveRooms()"));
        assertFalse(source.contains("services.patients().search("));
        assertFalse(source.contains("services.queue().getQueue("));
        assertFalse(source.contains("services.queue().getRoomSummaries("));
        assertTrue(source.contains("roomsHandler.handle("));
        assertTrue(source.contains("new GetActiveReceptionRoomsQuery("));
        assertTrue(source.contains("queueHandler.handle("));
        assertTrue(source.contains("new GetReceptionQueueQuery("));
        assertTrue(source.contains("searchPatientsHandler.handle("));
    }
}
