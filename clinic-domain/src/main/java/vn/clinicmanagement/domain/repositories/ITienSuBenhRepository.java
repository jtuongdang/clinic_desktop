package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.patient.TienSuBenh;

import java.util.List;

/** Read port for patient history owned by an exact facility and department pair. */
public interface ITienSuBenhRepository {
    List<TienSuBenh> getHistoryByPatientAndScope(int patientId, int facilityId, int departmentId);
}
