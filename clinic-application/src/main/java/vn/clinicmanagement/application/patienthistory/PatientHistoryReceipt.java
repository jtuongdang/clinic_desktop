package vn.clinicmanagement.application.patienthistory;

/** Safe mutation receipt; it deliberately contains no clinical narrative. */
public record PatientHistoryReceipt(int patientId, int historyId, long version) {
}
