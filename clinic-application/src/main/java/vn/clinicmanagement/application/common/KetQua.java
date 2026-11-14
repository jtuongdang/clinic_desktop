package vn.clinicmanagement.application.common;

import java.util.Objects;
import java.util.Optional;

/** Kết quả chuẩn cho use case, gồm giá trị thành công hoặc lỗi có mã ổn định. */
public final class KetQua<T> {
    /** Trường `value`: giá trị. */
    private final T value;
    /** Trường `error`: lỗi. */
    private final ApplicationError error;

    private KetQua(T value, ApplicationError error) {
        this.value = value;
        this.error = error;
    }

    /** Hàm `success`: thành công. */
    public static <T> KetQua<T> success(T value) {
        return new KetQua<>(value, null);
    }

    /** Hàm `failure`: thất bại. */
    public static <T> KetQua<T> failure(String code, String message) {
        return new KetQua<>(null, new ApplicationError(code, message));
    }

    /** Hàm `isSuccess`: là thành công. */
    public boolean isSuccess() { return error == null; }
    /** Hàm `isFailure`: là thất bại. */
    public boolean isFailure() { return !isSuccess(); }
    /** Hàm `getValue`: lấy giá trị. */
    public Optional<T> getValue() { return Optional.ofNullable(value); }
    /** Hàm `getError`: lấy lỗi. */
    public Optional<ApplicationError> getError() { return Optional.ofNullable(error); }
    /** Hàm `getErrorCode`: lấy lỗi mã. */
    public Optional<String> getErrorCode() { return getError().map(ApplicationError::code); }

    /** Hàm `valueOrThrow`: giá trị `Or` `Throw`. */
    public T valueOrThrow() {
        if (isFailure()) throw new IllegalStateException(error.code() + ": " + error.message());
        return value;
    }

    /** Trường `code`: mã. */
    /** Trường `message`: thông báo. */
    public record ApplicationError(String code, String message) {
        public ApplicationError {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
        }
    }
}
