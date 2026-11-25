package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.IUnitOfWork;

import java.util.List;
import java.util.Optional;

public interface IRepository<T extends CoSoThucThe<Integer> & ITongHopGoc> {
    Optional<T> getById(int id);
    List<T> getAll();
    PhanTrangKetQua<T> getPaged(int pageIndex, int pageSize, ITieuChuan<T> specification);
    List<T> find(ITieuChuan<T> specification);
    boolean exists(ITieuChuan<T> specification);
    int count(ITieuChuan<T> specification);
    void add(T entity);
    void update(T entity);
    void delete(T entity);
    IUnitOfWork getUnitOfWork();
}
