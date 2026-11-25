package vn.clinicmanagement.domain.common.exceptions;

public class EMRDaKhoaNgoaiLe extends MienNghiepVuNgoaiLe {
    public EMRDaKhoaNgoaiLe(int visitId) {
        super("Hồ sơ bệnh án EMR của lượt khám #" + visitId + " đã được ký số và khóa an toàn.");
    }
}
