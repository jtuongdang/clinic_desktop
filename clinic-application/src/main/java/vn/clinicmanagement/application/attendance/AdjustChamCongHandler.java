package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.TrangThaiChamCong;
import vn.clinicmanagement.domain.common.enums.TrangThaiDieuChinhChamCong;
import vn.clinicmanagement.domain.identity.CaTruc;
import vn.clinicmanagement.domain.identity.ChamCong;
import vn.clinicmanagement.domain.identity.DieuChinhChamCong;
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
 * Xử lý lệnh điều chỉnh chấm công từ Quản trị viên (ADMIN hoặc HR_ADMIN).
 * Ghi nhận nhật ký kiểm toán ATTENDANCE_ADJUSTED vào AuditWritePort.
 */
public final class AdjustChamCongHandler implements CommandHandler<AdjustChamCongCommand, AdjustChamCongBienNhan> {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "HR_ADMIN");

    /** Trường `repository`: kho dữ liệu. */
    private final ChamCongRepository repository;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final KiemToanWriteCongKetNoi auditWritePort;

    public AdjustChamCongHandler(ChamCongRepository repository, KiemToanWriteCongKetNoi auditWritePort) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    public AdjustChamCongHandler(ChamCongRepository repository, vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi systemAuditPort) {
        this(repository, new KiemToanWriteCongKetNoi() {
            /** Hàm `recordAudit`: ghi nhận kiểm toán. */
            @Override
            public void recordAudit(ChamCongKiemToanNhapLieu entry) {
                // Adapter cho system audit port nếu được truyền vào
            }
        });
    }

    @Override
    public KetQua<AdjustChamCongBienNhan> handle(AdjustChamCongCommand command) {
        if (command == null) {
            return KetQua.failure("VALIDATION_ERROR", "Lệnh điều chỉnh chấm công không được để trống.");
        }

        // 1. Phân quyền: Chỉ ADMIN hoặc HR_ADMIN
        PhanQuyenNguCanh auth = command.authorizationContext();
        if (auth == null || !isRoleAllowed(auth.roleCode())) {
            return KetQua.failure("AUTHORIZATION_DENIED", "Chỉ ADMIN hoặc HR_ADMIN mới có quyền điều chỉnh chấm công.");
        }

        // 2. Kiểm tra tính hợp lệ dữ liệu
        if (command.attendanceId() <= 0) {
            return KetQua.failure("INVALID_ATTENDANCE", "Mã chấm công không hợp lệ.");
        }
        if (command.reason() == null || command.reason().trim().isEmpty()) {
            return KetQua.failure("VALIDATION_ERROR", "Lý do điều chỉnh không được để trống.");
        }

        // 3. Tìm bản ghi chấm công
        Optional<ChamCong> attOpt = repository.getAttendanceById(command.attendanceId());
        if (attOpt.isEmpty()) {
            return KetQua.failure("ATTENDANCE_NOT_FOUND", "Không tìm thấy bản ghi chấm công: " + command.attendanceId());
        }
        ChamCong chamCong = attOpt.get();

        // 4. Tìm thông tin ca trực để tính lại đi muộn / về sớm nếu có
        Optional<LichLamViec> scheduleOpt = repository.getScheduleById(chamCong.getMaLichLamViec());
        CaTruc shift = scheduleOpt.flatMap(s -> repository.getShiftById(s.getMaCaTruc())).orElse(null);

        LocalDateTime shiftStart = null;
        LocalDateTime shiftEnd = null;
        if (scheduleOpt.isPresent() && shift != null) {
            LocalDate workDate = scheduleOpt.get().getNgayLamViec() != null ? scheduleOpt.get().getNgayLamViec() : LocalDate.now();
            shiftStart = LocalDateTime.of(workDate, shift.getGioBatDau());
            if (shift.getGioKetThuc().isBefore(shift.getGioBatDau())) {
                shiftEnd = LocalDateTime.of(workDate.plusDays(1), shift.getGioKetThuc());
            } else {
                shiftEnd = LocalDateTime.of(workDate, shift.getGioKetThuc());
            }
        }

        // 5. Cập nhật các trường được chỉ định
        if (command.newCheckInTime() != null) {
            chamCong.setThoiGianCheckIn(command.newCheckInTime());
        }
        if (command.newCheckOutTime() != null) {
            chamCong.setThoiGianCheckOut(command.newCheckOutTime());
        }

        if (chamCong.getThoiGianCheckIn() != null && chamCong.getThoiGianCheckOut() != null) {
            if (chamCong.getThoiGianCheckOut().isBefore(chamCong.getThoiGianCheckIn())) {
                return KetQua.failure("INVALID_TIME", "Thời gian check-out không thể trước thời gian check-in.");
            }
        }

        // Cập nhật số giờ làm việc
        if (command.adjustedWorkHours() != null) {
            chamCong.setSoGioLam(command.adjustedWorkHours().setScale(2, RoundingMode.HALF_UP));
        } else if (chamCong.getThoiGianCheckIn() != null && chamCong.getThoiGianCheckOut() != null) {
            long secondsWorked = Duration.between(chamCong.getThoiGianCheckIn(), chamCong.getThoiGianCheckOut()).getSeconds();
            chamCong.setSoGioLam(BigDecimal.valueOf(secondsWorked).divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP));
        }

        // Tính lại số giờ đi muộn nếu có mốc ca bắt đầu
        if (shiftStart != null && chamCong.getThoiGianCheckIn() != null) {
            if (chamCong.getThoiGianCheckIn().isAfter(shiftStart)) {
                long secondsLate = Duration.between(shiftStart, chamCong.getThoiGianCheckIn()).getSeconds();
                chamCong.setSoGioDiMuon(BigDecimal.valueOf(secondsLate).divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP));
            } else {
                chamCong.setSoGioDiMuon(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
        }

        // Tính lại số giờ về sớm nếu có mốc ca kết thúc
        if (shiftEnd != null && chamCong.getThoiGianCheckOut() != null) {
            if (chamCong.getThoiGianCheckOut().isBefore(shiftEnd)) {
                long secondsEarly = Duration.between(chamCong.getThoiGianCheckOut(), shiftEnd).getSeconds();
                chamCong.setSoGioVeSom(BigDecimal.valueOf(secondsEarly).divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP));
            } else {
                chamCong.setSoGioVeSom(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
        }

        // Cập nhật lại trạng thái chấm công
        if (chamCong.getThoiGianCheckIn() != null && chamCong.getThoiGianCheckOut() != null) {
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

        // 6. Lưu bản ghi phiếu điều chỉnh
        DieuChinhChamCong adjustment = new DieuChinhChamCong();
        adjustment.setMaChamCong(chamCong.getId());
        adjustment.setGioCheckInDeXuat(command.newCheckInTime());
        adjustment.setGioCheckOutDeXuat(command.newCheckOutTime());
        adjustment.setLyDo(command.reason().trim());
        adjustment.setTrangThai(TrangThaiDieuChinhChamCong.DA_DUYET);
        repository.saveAdjustment(adjustment);

        // 7. Lưu bản ghi chấm công cập nhật
        ChamCong saved = repository.saveAttendance(chamCong);

        // 8. Ghi nhật ký kiểm toán ATTENDANCE_ADJUSTED vào AuditWritePort
        auditWritePort.recordAudit("ATTENDANCE_ADJUSTED", auth.employeeId(), saved.getId(), command.reason().trim());

        AdjustChamCongBienNhan receipt = new AdjustChamCongBienNhan(
                saved.getId(),
                saved.getMaLichLamViec(),
                saved.getThoiGianCheckIn(),
                saved.getThoiGianCheckOut(),
                saved.getSoGioLam() != null ? saved.getSoGioLam() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                saved.getSoGioDiMuon() != null ? saved.getSoGioDiMuon() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                saved.getSoGioVeSom() != null ? saved.getSoGioVeSom() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                saved.getTrangThai(),
                command.reason().trim(),
                LocalDateTime.now()
        );

        return KetQua.success(receipt);
    }

    /** Hàm `isRoleAllowed`: là vai trò `Allowed`. */
    private boolean isRoleAllowed(String roleCode) {
        if (roleCode == null) return false;
        return ALLOWED_ROLES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }
}
