package vn.clinicmanagement.domain.common;

public interface IUnitOfWork {
    int saveChanges();
    boolean saveEntities();
}
