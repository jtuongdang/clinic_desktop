package vn.clinicmanagement.application.reception;

import java.time.LocalDateTime;

public record ReceptionQueueItem(int visitId, int patientId, String patientCode, String patientName,
                                 int roomId, String roomName, int queueNumber,
                                 String queueNumberDisplay, String status, LocalDateTime registeredAt) {}
