package vn.clinicmanagement.application.common;

import java.util.function.Supplier;

/** Ranh giới transaction do Infrastructure hiện thực; lỗi Result phải rollback. */
public interface TransactionBoundary {
    <T> Result<T> inTransaction(Supplier<Result<T>> operation);
}
