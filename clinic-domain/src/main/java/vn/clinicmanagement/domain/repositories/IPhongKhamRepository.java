package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.facility.PhongKham;

import java.util.List;
import java.util.Optional;

public interface IPhongKhamRepository {
    Optional<PhongKham> getById(int id);
    List<PhongKham> getActiveRooms();
}
