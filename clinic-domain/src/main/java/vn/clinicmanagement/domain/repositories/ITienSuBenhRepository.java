package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.patient.TienSuBenh;

import java.util.List;
import java.util.Optional;

/**
 * Cổng lưu tiền sử bệnh nhân, thuộc đúng cơ sở và khoa.
 */
public interface ITienSuBenhRepository {
    List<TienSuBenh> getHistoryByPatientAndScope(int patientId, int facilityId, int departmentId);

    Optional<TienSuBenh> getActiveByIdPatientAndScope(int historyId, int patientId, int facilityId, int departmentId);

    TienSuBenh add(TienSuBenh history);

    /**
     * Chỉ cập nhật dòng đang hoạt động trong đúng phạm vi hiện tại và đúng phiên bản kỳ vọng.
     */
    boolean update(TienSuBenh history, long expectedVersion);
}
