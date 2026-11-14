package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.facility.PhongKham;

import java.util.List;
import java.util.Optional;

/**
 * Khế ước lưu trữ dữ liệu phòng khám / buồng kỹ thuật (PhongKham).
 */
public interface IPhongKhamRepository {
    /** Hàm `getById`: lấy theo mã. */
    Optional<PhongKham> getById(int id);
    /** Hàm `getActiveRooms`: lấy hoạt động phòng. */
    default List<PhongKham> getActiveRooms() { return List.of(); }
    /** Hàm `getActiveRoomsInScope`: lấy hoạt động phòng trong phamvi. */
    default List<PhongKham> getActiveRoomsInScope(int facilityId, int departmentId) { return List.of(); }
    /** Hàm `getByMaPhong`: lấy theo mã phòng. */
    default Optional<PhongKham> getByMaPhong(String maPhong) { return Optional.empty(); }
    /** Hàm `existsByMaPhong`: `exists` theo mã phòng. */
    default boolean existsByMaPhong(String maPhong) { return false; }
    /** Hàm `getAll`: lấy `All`. */
    default List<PhongKham> getAll() { return List.of(); }
    /** Hàm `getByMaKhoa`: lấy theo mã khoa. */
    default List<PhongKham> getByMaKhoa(int maKhoa) { return List.of(); }
    /** Hàm `getByMaCoSo`: lấy theo mã có so. */
    default List<PhongKham> getByMaCoSo(int maCoSo) { return List.of(); }
    /** Hàm `add`: thêm. */
    default void add(PhongKham phongKham) {}
    /** Hàm `update`: cập nhật. */
    default void update(PhongKham phongKham) {}
    /** Hàm `delete`: xóa. */
    default void delete(PhongKham phongKham) {}

    /** Hàm `findById`: tìm theo mã. */
    default Optional<PhongKham> findById(int id) {
        return getById(id);
    }

    /** Hàm `findByMaPhong`: tìm theo mã phòng. */
    default Optional<PhongKham> findByMaPhong(String maPhong) {
        return getByMaPhong(maPhong);
    }

    /** Hàm `existsById`: `exists` theo mã. */
    default boolean existsById(int id) {
        return getById(id).isPresent();
    }

    /** Hàm `save`: lưu. */
    default PhongKham save(PhongKham phongKham) {
        if (phongKham.getId() == null || phongKham.getId() == 0) {
            add(phongKham);
        } else {
            update(phongKham);
        }
        return phongKham;
    }
}
