package vn.clinicmanagement.application.common;

import java.util.Objects;
import java.util.Optional;

/** Kết quả chuẩn cho use case, gồm giá trị thành công hoặc lỗi có mã ổn định. */
public final class KetQua<T> {
    private final T value;
    private final ApplicationError error;

    private KetQua(T value, ApplicationError error) {
        this.value = value;
        this.error = error;
    }

    public static <T> KetQua<T> success(T value) {
        return new KetQua<>(value, null);
    }

    public static <T> KetQua<T> failure(String code, String message) {
        return new KetQua<>(null, new ApplicationError(code, message));
    }

    public boolean isSuccess() { return error == null; }
    public boolean isFailure() { return !isSuccess(); }
    public Optional<T> getValue() { return Optional.ofNullable(value); }
    public Optional<ApplicationError> getError() { return Optional.ofNullable(error); }
    public Optional<String> getErrorCode() { return getError().map(ApplicationError::code); }

    public T valueOrThrow() {
        if (isFailure()) throw new IllegalStateException(error.code() + ": " + error.message());
        return value;
    }

    public record ApplicationError(String code, String message) {
        public ApplicationError {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
        }
    }
}
