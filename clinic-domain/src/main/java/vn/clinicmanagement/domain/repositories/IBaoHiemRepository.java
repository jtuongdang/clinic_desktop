package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.patient.BaoHiem;

import java.util.List;
import java.util.Optional;

public interface IBaoHiemRepository {
    Optional<BaoHiem> getById(int id);
    Optional<BaoHiem> getLatestByPatientId(int patientId);
    List<BaoHiem> getByPatientId(int patientId);
    void add(BaoHiem insurance);
    void update(BaoHiem insurance);
}
