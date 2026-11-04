package vn.clinicmanagement.domain.repositories;

import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.IUnitOfWork;

import java.util.List;
import java.util.Optional;

public interface IRepository<T extends BaseEntity<Integer> & IAggregateRoot> {
    Optional<T> getById(int id);
    List<T> getAll();
    PagedResult<T> getPaged(int pageIndex, int pageSize, ISpecification<T> specification);
    List<T> find(ISpecification<T> specification);
    boolean exists(ISpecification<T> specification);
    int count(ISpecification<T> specification);
    void add(T entity);
    void update(T entity);
    void delete(T entity);
    IUnitOfWork getUnitOfWork();
}
