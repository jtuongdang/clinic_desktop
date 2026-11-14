package vn.clinicmanagement.application.common;

import java.util.function.Supplier;

/** Ranh giới transaction do Infrastructure hiện thực; lỗi Result phải rollback. */
public interface GiaoDichRanhGioi {
    /** Hàm `inTransaction`: trong giao dịch. */
    <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation);
}
