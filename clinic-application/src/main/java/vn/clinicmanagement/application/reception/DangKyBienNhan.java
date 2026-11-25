package vn.clinicmanagement.application.reception;

public record DangKyBienNhan(int patientId, String patientCode, int visitId, int queueNumber,
                                  String queueNumberDisplay) {}
