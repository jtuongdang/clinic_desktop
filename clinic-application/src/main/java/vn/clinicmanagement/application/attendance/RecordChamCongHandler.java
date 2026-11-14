package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.TrangThaiChamCong;
import vn.clinicmanagement.domain.identity.CaTruc;
import vn.clinicmanagement.domain.identity.ChamCong;
import vn.clinicmanagement.domain.identity.LichLamViec;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Xử lý lệnh ghi nhận Check-in và Check-out ca trực của nhân viên.
 */
public final class RecordChamCongHandler implements CommandHandler<RecordChamCongCommand, RecordChamCongBienNhan> {

    private static final Set<String> PRIVILEGED_ROLES = Set.of("ADMIN", "HR_ADMIN", "MANAGER");

    /** Trường `repository`: kho dữ liệu. */
    private final ChamCongRepository repository;

    public RecordChamCongHandler(ChamCongRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public KetQua<RecordChamCongBienNhan> handle(RecordChamCongCommand command) {
        if (command == null) {
            return KetQua.failure("VALIDATION_ERROR", "Lệnh chấm công không được để trống.");
        }

        if (command.maLichLamViec() <= 0) {
            return KetQua.failure("INVALID_SCHEDULE", "Mã lịch làm việc không hợp lệ.");
        }

        // 1. Tìm thông tin lịch làm việc
        Optional<LichLamViec> scheduleOpt = repository.getScheduleById(command.maLichLamViec());
        if (scheduleOpt.isEmpty()) {
            return KetQua.failure("SCHEDULE_NOT_FOUND", "Không tìm thấy lịch làm việc: " + command.maLichLamViec());
        }
        LichLamViec schedule = scheduleOpt.get();

        // 2. Kiểm tra phân quyền thực hiện
        PhanQuyenNguCanh auth = command.authorizationContext();
        if (auth != null) {
            boolean isSelf = auth.employeeId() == schedule.getMaNhanVien();
            boolean isPrivileged = auth.roleCode() != null && PRIVILEGED_ROLES.contains(auth.roleCode().trim().toUpperCase(Locale.ROOT));
            if (!isSelf && !isPrivileged) {
                return KetQua.failure("AUTHORIZATION_DENIED", "Không có quyền chấm công cho nhân viên này.");
            }
        }

        // 3. Tìm thông tin ca trực
        Optional<CaTruc> shiftOpt = repository.getShiftById(schedule.getMaCaTruc());
        if (shiftOpt.isEmpty()) {
            return KetQua.failure("SHIFT_NOT_FOUND", "Không tìm thấy ca trực của lịch làm việc: " + schedule.getMaCaTruc());
        }
        CaTruc shift = shiftOpt.get();

        LocalDate workDate = schedule.getNgayLamViec() != null ? schedule.getNgayLamViec() : LocalDate.now();
        LocalDateTime shiftStart = LocalDateTime.of(workDate, shift.getGioBatDau());
        LocalDateTime shiftEnd;
        if (shift.getGioKetThuc().isBefore(shift.getGioBatDau())) {
            shiftEnd = LocalDateTime.of(workDate.plusDays(1), shift.getGioKetThuc());
        } else {
            shiftEnd = LocalDateTime.of(workDate, shift.getGioKetThuc());
        }

        // 4. Lấy hoặc khởi tạo bản ghi ChamCong
        ChamCong chamCong = repository.getAttendanceByScheduleId(schedule.getId())
                .orElseGet(() -> {
                    ChamCong cc = new ChamCong();
                    cc.setMaLichLamViec(schedule.getId());
                    return cc;
                });

        ChamCongLoai type = command.type() != null ? command.type() : determineType(command);

        if (type == ChamCongLoai.CHECK_IN) {
            LocalDateTime inTime = command.checkInTime() != null ? command.checkInTime() : command.timestamp();
            if (inTime == null) {
                return KetQua.failure("VALIDATION_ERROR", "Thời gian check-in không được để trống.");
            }
            chamCong.setThoiGianCheckIn(inTime);

            // Tính số giờ đi muộn nếu check-in sau giờ bắt đầu ca
            if (inTime.isAfter(shiftStart)) {
                long secondsLate = Duration.between(shiftStart, inTime).getSeconds();
                BigDecimal lateHours = BigDecimal.valueOf(secondsLate)
                        .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
                chamCong.setSoGioDiMuon(lateHours);
                chamCong.setTrangThai(TrangThaiChamCong.DI_MUON);
            } else {
                chamCong.setSoGioDiMuon(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                chamCong.setTrangThai(TrangThaiChamCong.DI_DUNG_GIO);
            }

        } else if (type == ChamCongLoai.CHECK_OUT) {
            LocalDateTime outTime = command.checkOutTime() != null ? command.checkOutTime() : command.timestamp();
            if (outTime == null) {
                return KetQua.failure("VALIDATION_ERROR", "Thời gian check-out không được để trống.");
            }
            if (chamCong.getThoiGianCheckIn() == null) {
                return KetQua.failure("CHECKIN_REQUIRED", "Chưa có thông tin check-in, không thể thực hiện check-out.");
            }
            if (outTime.isBefore(chamCong.getThoiGianCheckIn())) {
                return KetQua.failure("INVALID_TIME", "Thời gian check-out không thể trước thời gian check-in.");
            }
            chamCong.setThoiGianCheckOut(outTime);

            // Tính số giờ về sớm nếu check-out trước giờ kết thúc ca
            if (outTime.isBefore(shiftEnd)) {
                long secondsEarly = Duration.between(outTime, shiftEnd).getSeconds();
                BigDecimal earlyHours = BigDecimal.valueOf(secondsEarly)
                        .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
                chamCong.setSoGioVeSom(earlyHours);
            } else {
                chamCong.setSoGioVeSom(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }

            // Tính tổng số giờ làm việc thực tế
            long secondsWorked = Duration.between(chamCong.getThoiGianCheckIn(), outTime).getSeconds();
            BigDecimal workHours = BigDecimal.valueOf(secondsWorked)
                    .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
            chamCong.setSoGioLam(workHours);

            // Cập nhật trạng thái tổng hợp
            updateAttendanceStatus(chamCong);

        } else { // BOTH
            LocalDateTime inTime = command.checkInTime();
            LocalDateTime outTime = command.checkOutTime();
            if (inTime == null || outTime == null) {
                return KetQua.failure("VALIDATION_ERROR", "Cả thời gian check-in và check-out phải được cung cấp.");
            }
            if (outTime.isBefore(inTime)) {
                return KetQua.failure("INVALID_TIME", "Thời gian check-out không thể trước thời gian check-in.");
            }
            chamCong.setThoiGianCheckIn(inTime);
            chamCong.setThoiGianCheckOut(outTime);

            // Đi muộn
            if (inTime.isAfter(shiftStart)) {
                long secondsLate = Duration.between(shiftStart, inTime).getSeconds();
                chamCong.setSoGioDiMuon(BigDecimal.valueOf(secondsLate)
                        .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP));
            } else {
                chamCong.setSoGioDiMuon(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }

            // Về sớm
            if (outTime.isBefore(shiftEnd)) {
                long secondsEarly = Duration.between(outTime, shiftEnd).getSeconds();
                chamCong.setSoGioVeSom(BigDecimal.valueOf(secondsEarly)
                        .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP));
            } else {
                chamCong.setSoGioVeSom(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }

            // Giờ làm việc
            long secondsWorked = Duration.between(inTime, outTime).getSeconds();
            chamCong.setSoGioLam(BigDecimal.valueOf(secondsWorked)
                    .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP));

            updateAttendanceStatus(chamCong);
        }

        ChamCong saved = repository.saveAttendance(chamCong);

        RecordChamCongBienNhan receipt = new RecordChamCongBienNhan(
                saved.getId() != null ? saved.getId() : 0,
                schedule.getId(),
                saved.getThoiGianCheckIn(),
                saved.getThoiGianCheckOut(),
                saved.getSoGioLam() != null ? saved.getSoGioLam() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                saved.getSoGioDiMuon() != null ? saved.getSoGioDiMuon() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                saved.getSoGioVeSom() != null ? saved.getSoGioVeSom() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                saved.getTrangThai()
        );

        return KetQua.success(receipt);
    }

    /** Hàm `determineType`: `determine` loai. */
    private ChamCongLoai determineType(RecordChamCongCommand command) {
        if (command.checkInTime() != null && command.checkOutTime() != null) {
            return ChamCongLoai.BOTH;
        }
        if (command.checkOutTime() != null) {
            return ChamCongLoai.CHECK_OUT;
        }
        return ChamCongLoai.CHECK_IN;
    }

    /** Hàm `updateAttendanceStatus`: cập nhật chamcong trạng thái. */
    private void updateAttendanceStatus(ChamCong chamCong) {
        boolean isLate = chamCong.getSoGioDiMuon() != null && chamCong.getSoGioDiMuon().compareTo(BigDecimal.ZERO) > 0;
        boolean isEarly = chamCong.getSoGioVeSom() != null && chamCong.getSoGioVeSom().compareTo(BigDecimal.ZERO) > 0;

        if (isLate && isEarly) {
            chamCong.setTrangThai(TrangThaiChamCong.DI_MUON_VE_SOM);
        } else if (isLate) {
            chamCong.setTrangThai(TrangThaiChamCong.DI_MUON);
        } else if (isEarly) {
            chamCong.setTrangThai(TrangThaiChamCong.VE_SOM);
        } else {
            chamCong.setTrangThai(TrangThaiChamCong.DI_DUNG_GIO);
        }
    }
}
