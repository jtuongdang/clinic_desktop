package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.identity.CaTruc;
import vn.clinicmanagement.domain.identity.ChamCong;
import vn.clinicmanagement.domain.identity.LichLamViec;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Xử lý truy vấn tổng hợp và chi tiết chấm công của nhân viên theo khoảng thời gian hoặc kỳ làm việc.
 */
public final class GetChamCongTheoKyHandler implements QueryHandler<GetChamCongTheoKyQuery, ChamCongTongHopDuLieu> {

    private static final Set<String> ALLOWED_MANAGEMENT_ROLES = Set.of(
            "ADMIN", "HR_ADMIN", "MANAGER", "ACCOUNTANT", "DIRECTOR"
    );

    /** Trường `repository`: kho dữ liệu. */
    private final ChamCongRepository repository;

    public GetChamCongTheoKyHandler(ChamCongRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public KetQua<ChamCongTongHopDuLieu> handle(GetChamCongTheoKyQuery query) {
        if (query == null) {
            return KetQua.failure("VALIDATION_ERROR", "Truy vấn chấm công không được để trống.");
        }

        if (query.employeeId() <= 0) {
            return KetQua.failure("INVALID_EMPLOYEE", "Mã nhân viên không hợp lệ.");
        }

        // 1. Phân quyền: Nhân viên được xem chấm công của chính mình, quản lý được xem toàn bộ
        PhanQuyenNguCanh auth = query.authorizationContext();
        if (auth != null) {
            boolean isSelf = auth.employeeId() == query.employeeId();
            boolean isManager = auth.roleCode() != null
                    && ALLOWED_MANAGEMENT_ROLES.contains(auth.roleCode().trim().toUpperCase(Locale.ROOT));
            if (!isSelf && !isManager) {
                return KetQua.failure("AUTHORIZATION_DENIED", "Không có quyền tra cứu chấm công của nhân viên khác.");
            }
        }

        // 2. Xác thực khoảng ngày
        LocalDate fromDate = query.fromDate() != null ? query.fromDate() : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = query.toDate() != null ? query.toDate() : LocalDate.now();
        if (toDate.isBefore(fromDate)) {
            return KetQua.failure("INVALID_DATE_RANGE", "Ngày kết thúc không thể trước ngày bắt đầu.");
        }

        // 3. Tải danh sách lịch làm việc và danh sách chấm công
        List<LichLamViec> schedules = repository.getSchedulesByEmployeeAndDateRange(query.employeeId(), fromDate, toDate);
        List<ChamCong> attendances = repository.getAttendanceByEmployeeAndDateRange(query.employeeId(), fromDate, toDate);

        Map<Integer, LichLamViec> scheduleMap = schedules.stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(LichLamViec::getId, Function.identity(), (a, b) -> a));

        int totalScheduledShifts = schedules.size();
        int attendedShifts = 0;
        BigDecimal totalWorkHours = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalLateHours = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalEarlyLeaveHours = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        int lateCount = 0;
        int earlyLeaveCount = 0;

        List<ChamCongRecordDuLieu> records = new ArrayList<>();

        for (ChamCong att : attendances) {
            if (att.getThoiGianCheckIn() != null) {
                attendedShifts++;
            }

            if (att.getSoGioLam() != null) {
                totalWorkHours = totalWorkHours.add(att.getSoGioLam());
            }

            if (att.getSoGioDiMuon() != null && att.getSoGioDiMuon().compareTo(BigDecimal.ZERO) > 0) {
                totalLateHours = totalLateHours.add(att.getSoGioDiMuon());
                lateCount++;
            }

            if (att.getSoGioVeSom() != null && att.getSoGioVeSom().compareTo(BigDecimal.ZERO) > 0) {
                totalEarlyLeaveHours = totalEarlyLeaveHours.add(att.getSoGioVeSom());
                earlyLeaveCount++;
            }

            LichLamViec sch = scheduleMap.get(att.getMaLichLamViec());
            if (sch == null) {
                sch = repository.getScheduleById(att.getMaLichLamViec()).orElse(null);
            }

            int shiftId = 0;
            String shiftName = "";
            LocalDate workDate = null;

            if (sch != null) {
                shiftId = sch.getMaCaTruc();
                workDate = sch.getNgayLamViec();
                Optional<CaTruc> shiftOpt = repository.getShiftById(sch.getMaCaTruc());
                if (shiftOpt.isPresent()) {
                    shiftName = shiftOpt.get().getTenCa();
                }
            }

            records.add(new ChamCongRecordDuLieu(
                    att.getId() != null ? att.getId() : 0,
                    att.getMaLichLamViec(),
                    shiftId,
                    shiftName,
                    workDate,
                    att.getThoiGianCheckIn(),
                    att.getThoiGianCheckOut(),
                    att.getSoGioLam() != null ? att.getSoGioLam() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    att.getSoGioDiMuon() != null ? att.getSoGioDiMuon() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    att.getSoGioVeSom() != null ? att.getSoGioVeSom() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    att.getTrangThai()
            ));
        }

        // Sắp xếp các bản ghi theo ngày làm việc hoặc thời gian check-in
        records.sort(Comparator.comparing(
                ChamCongRecordDuLieu::checkInTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        ChamCongTongHopDuLieu summary = new ChamCongTongHopDuLieu(
                query.employeeId(),
                fromDate,
                toDate,
                totalScheduledShifts,
                attendedShifts,
                totalWorkHours,
                totalLateHours,
                totalEarlyLeaveHours,
                lateCount,
                earlyLeaveCount,
                records
        );

        return KetQua.success(summary);
    }
}
