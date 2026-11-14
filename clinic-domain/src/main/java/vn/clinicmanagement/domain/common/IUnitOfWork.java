package vn.clinicmanagement.domain.common;

public interface IUnitOfWork {
    /** Hàm `saveChanges`: lưu thaydoi. */
    int saveChanges();
    /** Hàm `saveEntities`: lưu thực thể. */
    boolean saveEntities();
}
