package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.facility.PhongKham;

import java.util.List;
import java.util.Optional;

/**
 * Khế ước lưu trữ dữ liệu phòng khám / buồng kỹ thuật (PhongKham).
 */
public interface IPhongKhamRepository {
    Optional<PhongKham> getById(int id);
    default List<PhongKham> getActiveRooms() { return List.of(); }
    default List<PhongKham> getActiveRoomsInScope(int facilityId, int departmentId) { return List.of(); }
    default Optional<PhongKham> getByMaPhong(String maPhong) { return Optional.empty(); }
    default boolean existsByMaPhong(String maPhong) { return false; }
    default List<PhongKham> getAll() { return List.of(); }
    default List<PhongKham> getByMaKhoa(int maKhoa) { return List.of(); }
    default List<PhongKham> getByMaCoSo(int maCoSo) { return List.of(); }
    default void add(PhongKham phongKham) {}
    default void update(PhongKham phongKham) {}
    default void delete(PhongKham phongKham) {}

    default Optional<PhongKham> findById(int id) {
        return getById(id);
    }

    default Optional<PhongKham> findByMaPhong(String maPhong) {
        return getByMaPhong(maPhong);
    }

    default boolean existsById(int id) {
        return getById(id).isPresent();
    }

    default PhongKham save(PhongKham phongKham) {
        if (phongKham.getId() == null || phongKham.getId() == 0) {
            add(phongKham);
        } else {
            update(phongKham);
        }
        return phongKham;
    }
}
