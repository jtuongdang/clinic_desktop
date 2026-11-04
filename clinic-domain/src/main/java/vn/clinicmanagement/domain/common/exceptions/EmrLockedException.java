package vn.clinicmanagement.domain.common.exceptions;

public class EmrLockedException extends DomainException {
    public EmrLockedException(int visitId) {
        super("Hồ sơ bệnh án EMR của lượt khám #" + visitId + " đã được ký số và khóa an toàn.");
    }
}
