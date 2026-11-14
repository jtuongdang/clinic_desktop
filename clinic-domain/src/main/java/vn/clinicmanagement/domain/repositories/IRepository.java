package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.IUnitOfWork;

import java.util.List;
import java.util.Optional;

public interface IRepository<T extends CoSoThucThe<Integer> & ITongHopGoc> {
    /** Hàm `getById`: lấy theo mã. */
    Optional<T> getById(int id);
    /** Hàm `getAll`: lấy `All`. */
    List<T> getAll();
    /** Hàm `getPaged`: lấy phân trang. */
    PhanTrangKetQua<T> getPaged(int pageIndex, int pageSize, ITieuChuan<T> specification);
    /** Hàm `find`: tìm. */
    List<T> find(ITieuChuan<T> specification);
    boolean exists(ITieuChuan<T> specification);
    /** Hàm `count`: dem. */
    int count(ITieuChuan<T> specification);
    /** Hàm `add`: thêm. */
    void add(T entity);
    /** Hàm `update`: cập nhật. */
    void update(T entity);
    /** Hàm `delete`: xóa. */
    void delete(T entity);
    /** Hàm `getUnitOfWork`: lấy đơn vị của làm việc. */
    IUnitOfWork getUnitOfWork();
}
