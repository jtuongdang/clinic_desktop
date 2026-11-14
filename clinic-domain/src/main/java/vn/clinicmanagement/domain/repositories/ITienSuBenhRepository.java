package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.patient.TienSuBenh;

import java.util.List;
import java.util.Optional;

/** Patient history persistence port with exact facility and department ownership. */
public interface ITienSuBenhRepository {
    /** Hàm `getHistoryByPatientAndScope`: lấy tiensu theo bệnh nhân va phamvi. */
    List<TienSuBenh> getHistoryByPatientAndScope(int patientId, int facilityId, int departmentId);

    /** Hàm `getActiveByIdPatientAndScope`: lấy hoạt động theo mã bệnh nhân va phamvi. */
    Optional<TienSuBenh> getActiveByIdPatientAndScope(int historyId, int patientId, int facilityId, int departmentId);

    /** Hàm `add`: thêm. */
    TienSuBenh add(TienSuBenh history);

    /** Updates only an active row in its existing exact scope and expected version. */
    boolean update(TienSuBenh history, long expectedVersion);
}
