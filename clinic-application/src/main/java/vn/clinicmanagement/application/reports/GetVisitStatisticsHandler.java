package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Xử lý truy vấn thống kê lượt khám bệnh và phân bố khoa/phòng (RPT-01).
 * Thẩm quyền yêu cầu: ADMIN, DIRECTOR, DOCTOR.
 * Thống kê tổng lượt khám, phân bố khoa/phòng, phân bố trạng thái và tỷ lệ hoàn tất.
 */
public final class GetVisitStatisticsHandler implements QueryHandler<GetVisitStatisticsQuery, VisitStatisticsDto> {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "DIRECTOR", "DOCTOR");

    private final IVisitStatisticsDataSource dataSource;
    private final AuditWritePort auditWritePort;

    public GetVisitStatisticsHandler(IVisitStatisticsDataSource dataSource, AuditWritePort auditWritePort) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.auditWritePort = auditWritePort;
    }

    public GetVisitStatisticsHandler(IVisitStatisticsDataSource dataSource) {
        this(dataSource, null);
    }

    public GetVisitStatisticsHandler(ILuotKhamRepository visitRepository, AuditWritePort auditWritePort) {
        Objects.requireNonNull(visitRepository, "visitRepository");
        this.dataSource = (from, to, facId, deptId) -> {
            List<LuotKham> all = visitRepository.getAll();
            if (all == null) return List.of();
            return all.stream()
                    .filter(Objects::nonNull)
                    .filter(v -> !v.isDeleted())
                    .filter(v -> {
                        LocalDate d = resolveVisitDate(v);
                        if (d == null) return false;
                        return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
                    })
                    .filter(v -> facId == null || facId == 0 || v.getMaCoSo() == facId)
                    .filter(v -> deptId == null || deptId == 0 || v.getMaKhoa() == deptId)
                    .toList();
        };
        this.auditWritePort = auditWritePort;
    }

    public GetVisitStatisticsHandler(ILuotKhamRepository visitRepository) {
        this(visitRepository, (AuditWritePort) null);
    }

    public GetVisitStatisticsHandler(ILuotKhamRepository visitRepository, IVisitStatisticsDataSource dataSource) {
        this(dataSource != null ? dataSource : fromRepository(visitRepository), null);
    }

    public GetVisitStatisticsHandler(ILuotKhamRepository visitRepository, IVisitStatisticsDataSource dataSource, AuditWritePort auditWritePort) {
        this(dataSource != null ? dataSource : fromRepository(visitRepository), auditWritePort);
    }

    private static IVisitStatisticsDataSource fromRepository(ILuotKhamRepository visitRepository) {
        Objects.requireNonNull(visitRepository, "visitRepository");
        return (from, to, facId, deptId) -> {
            List<LuotKham> all = visitRepository.getAll();
            if (all == null) return List.of();
            return all.stream()
                    .filter(Objects::nonNull)
                    .filter(v -> !v.isDeleted())
                    .filter(v -> {
                        LocalDate d = resolveVisitDate(v);
                        if (d == null) return false;
                        return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
                    })
                    .filter(v -> facId == null || facId == 0 || v.getMaCoSo() == facId)
                    .filter(v -> deptId == null || deptId == 0 || v.getMaKhoa() == deptId)
                    .toList();
        };
    }

    private static LocalDate resolveVisitDate(LuotKham v) {
        if (v.getNgayKham() != null) return v.getNgayKham();
        if (v.getThoiGianTiepNhan() != null) return v.getThoiGianTiepNhan().toLocalDate();
        if (v.getCreatedAt() != null) return v.getCreatedAt().toLocalDate();
        return null;
    }

    @Override
    public Result<VisitStatisticsDto> handle(GetVisitStatisticsQuery query) {
        if (query == null) {
            return Result.failure("QUERY_INVALID", "Truy vấn thống kê lượt khám không được để trống.");
        }

        // 1. Kiểm tra thẩm quyền nghiêm ngặt (ADMIN, DIRECTOR, DOCTOR)
        String role = query.getEffectiveRole();
        if (role == null || role.isBlank() || !ALLOWED_ROLES.contains(role)) {
            logAudit(query.getAuditActorContext(), AuditOutcome.DENIED, AuditReasonCode.AUTH_FORBIDDEN,
                    "Từ chối quyền xem thống kê lượt khám cho vai trò: " + role);
            return Result.failure("AUTHZ_DENIED",
                    "Chỉ Quản trị viên (ADMIN), Giám đốc (DIRECTOR) hoặc Bác sĩ (DOCTOR) mới có quyền xem thống kê lượt khám.");
        }

        // 2. Kiểm tra khoảng thời gian
        LocalDate fromDate = query.getFromDate() != null ? query.getFromDate() : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = query.getToDate() != null ? query.getToDate() : LocalDate.now();

        if (fromDate.isAfter(toDate)) {
            return Result.failure("INVALID_DATE_RANGE", "Từ ngày không được lớn hơn đến ngày.");
        }

        // 3. Tra cứu lượt khám trong kỳ
        List<LuotKham> visits;
        try {
            visits = dataSource.getVisits(fromDate, toDate, query.getFacilityId(), query.getDepartmentId());
            if (visits == null) {
                visits = List.of();
            }
        } catch (Exception ex) {
            return Result.failure("DATA_FETCH_FAILED", "Không thể lấy dữ liệu lượt khám: " + ex.getMessage());
        }

        // 4. Tính toán tổng hợp
        long totalVisits = visits.size();
        long completedVisits = 0;
        long waitingVisits = 0;
        long inProgressVisits = 0;
        long cancelledVisits = 0;

        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        Map<String, Long> departmentBreakdown = new TreeMap<>();
        Map<Integer, DeptStatsAccumulator> deptStats = new LinkedHashMap<>();

        for (LuotKham v : visits) {
            if (v == null) continue;

            TrangThaiLuotKham st = v.getTrangThai();
            String stKey = st != null ? st.name() : "KHONG_XAC_DINH";
            statusBreakdown.put(stKey, statusBreakdown.getOrDefault(stKey, 0L) + 1);

            boolean isCompleted = isCompletedStatus(st);
            boolean isWaiting = (st == TrangThaiLuotKham.CHO_KHAM);
            boolean isInProgress = (st == TrangThaiLuotKham.DANG_KHAM || st == TrangThaiLuotKham.CHO_CLS);
            boolean isCancelled = (st == TrangThaiLuotKham.DA_HUY);

            if (isCompleted) completedVisits++;
            else if (isWaiting) waitingVisits++;
            else if (isInProgress) inProgressVisits++;
            else if (isCancelled) cancelledVisits++;

            int deptId = v.getMaKhoa();
            String deptName = (v.getKhoa() != null && v.getKhoa().getTenKhoa() != null && !v.getKhoa().getTenKhoa().isBlank())
                    ? v.getKhoa().getTenKhoa()
                    : "Khoa " + (deptId > 0 ? deptId : "Chung");

            departmentBreakdown.put(deptName, departmentBreakdown.getOrDefault(deptName, 0L) + 1);

            DeptStatsAccumulator acc = deptStats.computeIfAbsent(deptId, k -> new DeptStatsAccumulator(deptId, deptName));
            acc.addVisit(isCompleted, isWaiting, isInProgress, isCancelled);

            Integer roomId = v.getMaPhongKham();
            if (roomId != null && roomId > 0) {
                String roomName = (v.getPhongKham() != null && v.getPhongKham().getTenPhong() != null)
                        ? v.getPhongKham().getTenPhong()
                        : "Phòng " + roomId;
                acc.addRoomVisit(roomId, roomName, isCompleted, isWaiting, isInProgress, isCancelled);
            }
        }

        double completionRate = totalVisits > 0
                ? BigDecimal.valueOf(completedVisits * 100.0 / totalVisits).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        // 5. Tạo danh sách VisitStatisticsItemDto
        List<VisitStatisticsItemDto> items = new ArrayList<>();
        int stt = 1;

        // Thêm tổng quan toàn viện
        items.add(new VisitStatisticsItemDto(stt++, 0, "Toàn viện / Tổng hợp tất cả khoa",
                totalVisits, completedVisits, waitingVisits, inProgressVisits, cancelledVisits,
                completionRate, "Tỷ lệ hoàn tất toàn viện"));

        // Thêm theo từng khoa phòng
        for (DeptStatsAccumulator acc : deptStats.values()) {
            items.add(acc.toItemDto(stt++));

            // Thêm các phòng thuộc khoa nếu có
            for (RoomStatsAccumulator roomAcc : acc.rooms.values()) {
                items.add(roomAcc.toItemDto(stt++, acc.departmentId, acc.departmentName));
            }
        }

        VisitStatisticsDto dto = new VisitStatisticsDto(
                fromDate, toDate,
                totalVisits,
                completedVisits,
                waitingVisits,
                inProgressVisits,
                cancelledVisits,
                completionRate,
                departmentBreakdown,
                statusBreakdown,
                items
        );

        // 6. Ghi nhật ký kiểm toán nếu có context
        logAudit(query.getAuditActorContext(), AuditOutcome.SUCCESS, null, "Xem thống kê lượt khám bệnh");

        return Result.success(dto);
    }

    private boolean isCompletedStatus(TrangThaiLuotKham st) {
        if (st == null) return false;
        return st == TrangThaiLuotKham.HOAN_THANH || st == TrangThaiLuotKham.DA_KHAM || st == TrangThaiLuotKham.KET_LUAN;
    }

    private void logAudit(AuditActorContext ctx, AuditOutcome outcome, AuditReasonCode reasonCode, String message) {
        if (auditWritePort == null || ctx == null) {
            return;
        }
        try {
            SystemAuditEntry entry = new SystemAuditEntry(
                    ctx.employeeId(),
                    ctx.roleId(),
                    ctx.roleCode(),
                    ctx.facilityId(),
                    ctx.departmentId(),
                    outcome == AuditOutcome.SUCCESS ? AuditActionCode.CATALOG_VIEW : AuditActionCode.AUTH_ACCESS_DENIED,
                    AuditResourceType.VISIT,
                    1,
                    outcome,
                    reasonCode,
                    Instant.now(),
                    ctx.workstationId(),
                    ctx.correlationId()
            );
            auditWritePort.appendIndependently(entry);
        } catch (Exception ignored) {
            // Read-only query does not fail on audit exception
        }
    }

    private static class DeptStatsAccumulator {
        final int departmentId;
        final String departmentName;
        long total;
        long completed;
        long waiting;
        long inProgress;
        long cancelled;
        final Map<Integer, RoomStatsAccumulator> rooms = new LinkedHashMap<>();

        DeptStatsAccumulator(int departmentId, String departmentName) {
            this.departmentId = departmentId;
            this.departmentName = departmentName;
        }

        void addVisit(boolean isCompleted, boolean isWaiting, boolean isInProgress, boolean isCancelled) {
            total++;
            if (isCompleted) completed++;
            else if (isWaiting) waiting++;
            else if (isInProgress) inProgress++;
            else if (isCancelled) cancelled++;
        }

        void addRoomVisit(int roomId, String roomName, boolean isCompleted, boolean isWaiting, boolean isInProgress, boolean isCancelled) {
            RoomStatsAccumulator r = rooms.computeIfAbsent(roomId, k -> new RoomStatsAccumulator(roomId, roomName));
            r.addVisit(isCompleted, isWaiting, isInProgress, isCancelled);
        }

        VisitStatisticsItemDto toItemDto(int stt) {
            double rate = total > 0 ? BigDecimal.valueOf(completed * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0;
            return new VisitStatisticsItemDto(stt, departmentId, departmentName, total, completed, waiting, inProgress, cancelled, rate, "Khoa chuyên môn");
        }
    }

    private static class RoomStatsAccumulator {
        final int roomId;
        final String roomName;
        long total;
        long completed;
        long waiting;
        long inProgress;
        long cancelled;

        RoomStatsAccumulator(int roomId, String roomName) {
            this.roomId = roomId;
            this.roomName = roomName;
        }

        void addVisit(boolean isCompleted, boolean isWaiting, boolean isInProgress, boolean isCancelled) {
            total++;
            if (isCompleted) completed++;
            else if (isWaiting) waiting++;
            else if (isInProgress) inProgress++;
            else if (isCancelled) cancelled++;
        }

        VisitStatisticsItemDto toItemDto(int stt, int deptId, String deptName) {
            double rate = total > 0 ? BigDecimal.valueOf(completed * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0;
            return new VisitStatisticsItemDto(stt, deptId, deptName, roomId, roomName, total, completed, waiting, inProgress, cancelled, rate, 0L, null, "Buồng khám chuyên khoa");
        }
    }
}
