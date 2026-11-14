package vn.clinicmanagement.application.reception;

import java.time.LocalDateTime;

/** Trường `visitId`: luotkham mã. */
/** Trường `patientId`: bệnh nhân mã. */
/** Trường `patientCode`: bệnh nhân mã. */
/** Trường `patientName`: bệnh nhân tên. */
public record TiepDonHangDoiMatHang(int visitId, int patientId, String patientCode, String patientName,
                                 /** Trường `roomId`: phòng mã. */
                                 /** Trường `roomName`: phòng tên. */
                                 /** Trường `queueNumber`: hangdoi so. */
                                 int roomId, String roomName, int queueNumber,
                                 /** Trường `queueNumberDisplay`: hangdoi so `Display`. */
                                 /** Trường `status`: trạng thái. */
                                 /** Trường `registeredAt`: dadangky luc. */
                                 String queueNumberDisplay, String status, LocalDateTime registeredAt) {}
