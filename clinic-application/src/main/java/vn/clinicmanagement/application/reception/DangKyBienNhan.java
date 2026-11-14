package vn.clinicmanagement.application.reception;

/** Trường `patientId`: bệnh nhân mã. */
/** Trường `patientCode`: bệnh nhân mã. */
/** Trường `visitId`: luotkham mã. */
/** Trường `queueNumber`: hangdoi so. */
public record DangKyBienNhan(int patientId, String patientCode, int visitId, int queueNumber,
                                  /** Trường `queueNumberDisplay`: hangdoi so `Display`. */
                                  String queueNumberDisplay) {}
