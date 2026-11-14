package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.patient.BaoHiem;

import java.util.List;
import java.util.Optional;

public interface IBaoHiemRepository {
    /** Hàm `getById`: lấy theo mã. */
    Optional<BaoHiem> getById(int id);
    /** Hàm `getLatestByPatientId`: lấy `Latest` theo bệnh nhân mã. */
    Optional<BaoHiem> getLatestByPatientId(int patientId);
    /** Hàm `getByPatientId`: lấy theo bệnh nhân mã. */
    List<BaoHiem> getByPatientId(int patientId);
    /** Hàm `add`: thêm. */
    void add(BaoHiem insurance);
    /** Hàm `update`: cập nhật. */
    void update(BaoHiem insurance);
}
