package vn.clinicmanagement.application.attendance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.common.enums.TrangThaiChamCong;
import vn.clinicmanagement.domain.common.enums.TrangThaiDieuChinhChamCong;
import vn.clinicmanagement.domain.identity.CaTruc;
import vn.clinicmanagement.domain.identity.ChamCong;
import vn.clinicmanagement.domain.identity.DieuChinhChamCong;
import vn.clinicmanagement.domain.identity.LichLamViec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdjustAttendanceHandlerTest {

    private InMemoryAttendanceRepository repository;
    private RecordingAttendanceAuditPort auditPort;
    private AdjustAttendanceHandler handler;

    private ChamCong existingAttendance;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAttendanceRepository();
        auditPort = new RecordingAttendanceAuditPort();
        handler = new AdjustAttendanceHandler(repository, auditPort);

        // Tạo ca và lịch
        CaTruc shift = new CaTruc();
        shift.setId(1);
        shift.setTenCa("Ca Sáng");
        shift.setGioBatDau(LocalTime.of(8, 0));
        shift.setGioKetThuc(LocalTime.of(12, 0));
        repository.saveShift(shift);

        LichLamViec schedule = new LichLamViec();
        schedule.setId(10);
        schedule.setMaNhanVien(101);
        schedule.setMaCaTruc(1);
        schedule.setNgayLamViec(LocalDate.of(2026, 11, 7));
        repository.saveSchedule(schedule);

        // Tạo bản ghi chấm công ban đầu: check-in 08:30 (muộn 30p), check-out 11:30 (về sớm 30p)
        existingAttendance = new ChamCong();
        existingAttendance.setId(50);
        existingAttendance.setMaLichLamViec(10);
        existingAttendance.setThoiGianCheckIn(LocalDateTime.of(2026, 11, 7, 8, 30));
        existingAttendance.setThoiGianCheckOut(LocalDateTime.of(2026, 11, 7, 11, 30));
        existingAttendance.setSoGioLam(new BigDecimal("3.00"));
        existingAttendance.setSoGioDiMuon(new BigDecimal("0.50"));
        existingAttendance.setSoGioVeSom(new BigDecimal("0.50"));
        existingAttendance.setTrangThai(TrangThaiChamCong.DI_MUON_VE_SOM);
        repository.saveAttendance(existingAttendance);
    }

    @Test
    @DisplayName("ADMIN điều chỉnh giờ check-in/out thành công và ghi nhật ký ATTENDANCE_ADJUSTED")
    void adminAdjustsAttendanceTimes_successAndAuditRecorded() {
        AuthorizationContext adminAuth = new AuthorizationContext(
                1, 1, "ADMIN", 1, 1, UUID.randomUUID()
        );

        // Điều chỉnh thành đúng giờ: check-in 08:00, check-out 12:00
        LocalDateTime newIn = LocalDateTime.of(2026, 11, 7, 8, 0);
        LocalDateTime newOut = LocalDateTime.of(2026, 11, 7, 12, 0);

        AdjustAttendanceCommand command = new AdjustAttendanceCommand(
                50,
                newIn,
                newOut,
                null,
                "Nhân viên quên quẹt thẻ do cấp cứu đột xuất",
                adminAuth
        );

        Result<AdjustAttendanceReceipt> result = handler.handle(command);
        assertTrue(result.isSuccess());

        AdjustAttendanceReceipt receipt = result.getValue().orElseThrow();
        assertEquals(50, receipt.attendanceId());
        assertEquals(newIn, receipt.checkInTime());
        assertEquals(newOut, receipt.checkOutTime());
        assertEquals(new BigDecimal("4.00"), receipt.soGioLam());
        assertEquals(new BigDecimal("0.00"), receipt.soGioDiMuon());
        assertEquals(new BigDecimal("0.00"), receipt.soGioVeSom());
        assertEquals(TrangThaiChamCong.DI_DUNG_GIO, receipt.trangThai());

        // Kiểm tra nhật ký kiểm toán ATTENDANCE_ADJUSTED
        assertEquals(1, auditPort.entries.size());
        AttendanceAuditEntry audit = auditPort.entries.get(0);
        assertEquals("ATTENDANCE_ADJUSTED", audit.action());
        assertEquals(1, audit.actorEmployeeId());
        assertEquals(50, audit.attendanceId());
        assertEquals("Nhân viên quên quẹt thẻ do cấp cứu đột xuất", audit.reason());

        // Kiểm tra bản ghi phiếu điều chỉnh được lưu trong CSDL
        List<DieuChinhChamCong> adjustments = repository.getAdjustmentsByAttendanceId(50);
        assertEquals(1, adjustments.size());
        DieuChinhChamCong adj = adjustments.get(0);
        assertEquals(newIn, adj.getGioCheckInDeXuat());
        assertEquals(newOut, adj.getGioCheckOutDeXuat());
        assertEquals("Nhân viên quên quẹt thẻ do cấp cứu đột xuất", adj.getLyDo());
        assertEquals(TrangThaiDieuChinhChamCong.DA_DUYET, adj.getTrangThai());
    }

    @Test
    @DisplayName("HR_ADMIN điều chỉnh số giờ công làm việc trực tiếp thành công")
    void hrAdminAdjustsWorkHours_success() {
        AuthorizationContext hrAuth = new AuthorizationContext(
                2, 2, "HR_ADMIN", 1, 1, UUID.randomUUID()
        );

        AdjustAttendanceCommand command = new AdjustAttendanceCommand(
                50,
                null,
                null,
                new BigDecimal("5.50"),
                "Bổ sung 2.5 giờ công làm thêm ngoài ca",
                hrAuth
        );

        Result<AdjustAttendanceReceipt> result = handler.handle(command);
        assertTrue(result.isSuccess());

        AdjustAttendanceReceipt receipt = result.getValue().orElseThrow();
        assertEquals(new BigDecimal("5.50"), receipt.soGioLam());

        // Verify audit recorded
        assertEquals(1, auditPort.entries.size());
        assertEquals("ATTENDANCE_ADJUSTED", auditPort.entries.get(0).action());
        assertEquals(2, auditPort.entries.get(0).actorEmployeeId());
    }

    @Test
    @DisplayName("Từ chối người dùng không có vai trò ADMIN hoặc HR_ADMIN")
    void nonPrivilegedUser_denied() {
        AuthorizationContext doctorAuth = new AuthorizationContext(
                101, 5, "DOCTOR", 1, 1, UUID.randomUUID()
        );

        AdjustAttendanceCommand command = new AdjustAttendanceCommand(
                50,
                LocalDateTime.of(2026, 11, 7, 8, 0),
                LocalDateTime.of(2026, 11, 7, 12, 0),
                null,
                "Tự sửa giờ chấm công",
                doctorAuth
        );

        Result<AdjustAttendanceReceipt> result = handler.handle(command);
        assertFalse(result.isSuccess());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElseThrow());
        assertTrue(auditPort.entries.isEmpty());
    }

    @Test
    @DisplayName("Báo lỗi khi lý do điều chỉnh để trống")
    void missingReason_validationError() {
        AuthorizationContext adminAuth = new AuthorizationContext(
                1, 1, "ADMIN", 1, 1, UUID.randomUUID()
        );

        AdjustAttendanceCommand command = new AdjustAttendanceCommand(
                50,
                LocalDateTime.of(2026, 11, 7, 8, 0),
                LocalDateTime.of(2026, 11, 7, 12, 0),
                null,
                "   ",
                adminAuth
        );

        Result<AdjustAttendanceReceipt> result = handler.handle(command);
        assertFalse(result.isSuccess());
        assertEquals("VALIDATION_ERROR", result.getErrorCode().orElseThrow());
    }

    @Test
    @DisplayName("Báo lỗi khi mã chấm công không tồn tại")
    void attendanceNotFound_error() {
        AuthorizationContext adminAuth = new AuthorizationContext(
                1, 1, "ADMIN", 1, 1, UUID.randomUUID()
        );

        AdjustAttendanceCommand command = new AdjustAttendanceCommand(
                9999,
                LocalDateTime.of(2026, 11, 7, 8, 0),
                LocalDateTime.of(2026, 11, 7, 12, 0),
                null,
                "Điều chỉnh chấm công",
                adminAuth
        );

        Result<AdjustAttendanceReceipt> result = handler.handle(command);
        assertFalse(result.isSuccess());
        assertEquals("ATTENDANCE_NOT_FOUND", result.getErrorCode().orElseThrow());
    }
}
