package vn.clinicmanagement.application.session;

import vn.clinicmanagement.domain.repositories.IHeThongRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.logging.Logger;

/** Các thời hạn phiên hữu hạn, đã kiểm tra và được tải từ cấu hình hệ thống. */
public record PhienHetGioChinhSach(Duration idleTimeout, Duration absoluteTimeout) {
    private static final Logger LOGGER = Logger.getLogger(PhienHetGioChinhSach.class.getName());
    public static final String IDLE_TIMEOUT_KEY = "SESSION_IDLE_TIMEOUT_MINUTES";
    public static final String ABSOLUTE_TIMEOUT_KEY = "SESSION_ABSOLUTE_TIMEOUT_HOURS";
    private static final long DEFAULT_IDLE_MINUTES = 15;
    private static final long DEFAULT_ABSOLUTE_HOURS = 12;
    private static final long MAX_IDLE_MINUTES = 24 * 60;
    private static final long MAX_ABSOLUTE_HOURS = 24 * 30;

    public PhienHetGioChinhSach {
        if (idleTimeout == null || idleTimeout.isZero() || idleTimeout.isNegative()
                || absoluteTimeout == null || absoluteTimeout.isZero() || absoluteTimeout.isNegative()) {
            throw new IllegalArgumentException("Session timeouts must be finite and positive.");
        }
    }

    public static PhienHetGioChinhSach from(IHeThongRepository repository) {
        Optional<String> idle = Optional.empty();
        Optional<String> absolute = Optional.empty();
        try {
            if (repository != null) {
                idle = repository.getSystemParameterValue(IDLE_TIMEOUT_KEY);
                absolute = repository.getSystemParameterValue(ABSOLUTE_TIMEOUT_KEY);
            }
        } catch (RuntimeException ignored) {
            // Giá trị mặc định được phê duyệt và có giới hạn, tránh để lỗi cấu hình tạo ra phiên vô thời hạn.
            LOGGER.warning("Unable to read session timeout settings; bounded approved defaults are in use.");
        }
        return new PhienHetGioChinhSach(
                Duration.ofMinutes(parseBounded(IDLE_TIMEOUT_KEY, idle, DEFAULT_IDLE_MINUTES, MAX_IDLE_MINUTES)),
                Duration.ofHours(parseBounded(ABSOLUTE_TIMEOUT_KEY, absolute, DEFAULT_ABSOLUTE_HOURS, MAX_ABSOLUTE_HOURS)));
    }

    private static long parseBounded(String key, Optional<String> configured, long fallback, long maximum) {
        if (configured == null || configured.isEmpty()) {
            LOGGER.warning("Missing session timeout setting " + key + "; bounded approved default is in use.");
            return fallback;
        }
        try {
            long value = Long.parseLong(configured.get().trim());
            if (value > 0 && value <= maximum) return value;
        } catch (RuntimeException ignored) {
            // Dùng giá trị mặc định an toàn có giới hạn, không ghi nội dung cấu hình vào nhật ký.
        }
        LOGGER.warning("Invalid session timeout setting " + key + "; bounded approved default is in use.");
        return fallback;
    }
}
