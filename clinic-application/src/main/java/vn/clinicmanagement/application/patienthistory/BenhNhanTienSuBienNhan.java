package vn.clinicmanagement.application.patienthistory;

/**
 * Biên nhận thay đổi an toàn, chủ ý không chứa nội dung tường thuật lâm sàng.
 */
public record BenhNhanTienSuBienNhan(int patientId, int historyId, long version) {
}
