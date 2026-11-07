package vn.clinicmanagement.application.reception;

import java.util.List;

public record ReceptionQueueSnapshot(List<ReceptionQueueItem> queue,
                                     List<ClinicRoomQueueSummary> rooms) {
    public ReceptionQueueSnapshot {
        queue = queue == null ? List.of() : List.copyOf(queue);
        rooms = rooms == null ? List.of() : List.copyOf(rooms);
    }
}
