package vn.clinicmanagement.domain.common.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sự kiện miền có định danh duy nhất và thời điểm phát sinh.
 */
public interface IMienNghiepVuSuKien {
    UUID getEventId();

    /**
     * Trả về thời điểm phát sinh sự kiện dưới dạng {@link LocalDateTime} không có múi giờ, trong đó các trường thời gian biểu diễn UTC. @return thời điểm sự kiện theo UTC.
     */
    LocalDateTime getOccurredOn();
}
