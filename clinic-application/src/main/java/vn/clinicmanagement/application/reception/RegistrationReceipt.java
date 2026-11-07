package vn.clinicmanagement.application.reception;

public record RegistrationReceipt(int patientId, String patientCode, int visitId, int queueNumber,
                                  String queueNumberDisplay) {}
