package vn.clinicmanagement.application.audit;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cổng giao tiếp đọc dữ liệu kiểm toán (AUD-01).
 * Tuân thủ Clean Architecture (Application Port).
 */
public interface KiemToanReadCongKetNoi {

    /**
     * Tra cứu danh sách nhật ký kiểm toán với các tiêu chí lọc và phân trang.
     *
     * @param actor Tên hoặc mã người thực hiện (có thể null)
     * @param resourceType Loại tài nguyên (có thể null)
     * @param actionCode Mã hành động (có thể null)
     * @param fromTime Thời gian bắt đầu (có thể null)
     * @param toTime Thời gian kết thúc (có thể null)
     * @param page Số trang (bắt đầu từ 0)
     * @param size Kích thước trang
     * @return Danh sách DTO nhật ký kiểm toán
     */
    List<KiemToanNhatKyDuLieu> findAuditLogs(
            String actor,
            String resourceType,
            String actionCode,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            int page,
            int size
    );

    /** Hàm `findAuditLogs`: tìm kiểm toán nhatky. */
    default List<KiemToanNhatKyDuLieu> findAuditLogs(
            String actor,
            String resourceType,
            String actionCode,
            LocalDateTime fromTime,
            LocalDateTime toTime
    ) {
        return findAuditLogs(actor, resourceType, actionCode, fromTime, toTime, 0, 50);
    }

    /** Hàm `getAuditLogs`: lấy kiểm toán nhatky. */
    default List<KiemToanNhatKyDuLieu> getAuditLogs(GetKiemToanNhatKyQuery query) {
        if (query == null) {
            return List.of();
        }
        return findAuditLogs(
                query.getActor(),
                query.getResourceType(),
                query.getActionCode(),
                query.getFromTime(),
                query.getToTime(),
                query.getPage(),
                query.getPageSize()
        );
    }
}
