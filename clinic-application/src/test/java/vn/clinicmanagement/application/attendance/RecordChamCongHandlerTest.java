package vn.clinicmanagement.application.attendance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.TrangThaiChamCong;
import vn.clinicmanagement.domain.identity.CaTruc;
import vn.clinicmanagement.domain.identity.LichLamViec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordChamCongHandlerTest {

    private InBoNhoChamCongRepository repository;
    private RecordChamCongHandler handler;

    private LichLamViec daySchedule;
    private CaTruc dayShift;

    @BeforeEach
    void setUp() {
        repository = new InBoNhoChamCongRepository();
        handler = new RecordChamCongHandler(repository);

        // Tạo ca ngày: 08:00 - 17:00
        dayShift = new CaTruc();
        dayShift.setId(1);
        dayShift.setTenCa("Ca Hành Chính Sáng Chiều");
        dayShift.setGioBatDau(LocalTime.of(8, 0));
        dayShift.setGioKetThuc(LocalTime.of(17, 0));
        repository.saveShift(dayShift);

        // Tạo lịch làm việc cho nhân viên 101 ngày 2026-11-07
        daySchedule = new LichLamViec();
        daySchedule.setId(10);
        daySchedule.setMaNhanVien(101);
        daySchedule.setMaCaTruc(1);
        daySchedule.setNgayLamViec(LocalDate.of(2026, 11, 7));
        repository.saveSchedule(daySchedule);
    }

    @Test
    @DisplayName("Check-in đúng giờ: trạng thái DI_DUNG_GIO và số giờ đi muộn bằng 0")
    void checkInOnTime_setsDungGioAndZeroLateHours() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 11, 7, 7, 55);
        RecordChamCongCommand command = RecordChamCongCommand.checkIn(10, checkInTime);

        KetQua<RecordChamCongBienNhan> result = handler.handle(command);
        assertTrue(result.isSuccess());

        RecordChamCongBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(10, receipt.maLichLamViec());
        assertEquals(checkInTime, receipt.checkInTime());
        assertEquals(new BigDecimal("0.00"), receipt.soGioDiMuon());
        assertEquals(TrangThaiChamCong.DI_DUNG_GIO, receipt.trangThai());
    }

    @Test
    @DisplayName("Check-in muộn 30 phút: tự động tính số giờ đi muộn 0.50 và trạng thái DI_MUON")
    void checkInLate_calculatesLateHoursAndSetsDiMuon() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 11, 7, 8, 30);
        RecordChamCongCommand command = RecordChamCongCommand.checkIn(10, checkInTime);

        KetQua<RecordChamCongBienNhan> result = handler.handle(command);
        assertTrue(result.isSuccess());

        RecordChamCongBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(new BigDecimal("0.50"), receipt.soGioDiMuon());
        assertEquals(TrangThaiChamCong.DI_MUON, receipt.trangThai());
    }

    @Test
    @DisplayName("Check-out đúng giờ: tính đủ số giờ làm việc và trạng thái DI_DUNG_GIO")
    void checkOutOnTime_calculatesWorkHoursAndSetsDungGio() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 11, 7, 8, 0);
        handler.handle(RecordChamCongCommand.checkIn(10, checkInTime));

        LocalDateTime checkOutTime = LocalDateTime.of(2026, 11, 7, 17, 0);
        KetQua<RecordChamCongBienNhan> result = handler.handle(RecordChamCongCommand.checkOut(10, checkOutTime));
        assertTrue(result.isSuccess());

        RecordChamCongBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(new BigDecimal("9.00"), receipt.soGioLam());
        assertEquals(new BigDecimal("0.00"), receipt.soGioVeSom());
        assertEquals(new BigDecimal("0.00"), receipt.soGioDiMuon());
        assertEquals(TrangThaiChamCong.DI_DUNG_GIO, receipt.trangThai());
    }

    @Test
    @DisplayName("Check-out về sớm 30 phút: tính số giờ về sớm 0.50 và trạng thái VE_SOM")
    void checkOutEarly_calculatesEarlyLeaveHoursAndSetsVeSom() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 11, 7, 8, 0);
        handler.handle(RecordChamCongCommand.checkIn(10, checkInTime));

        LocalDateTime checkOutTime = LocalDateTime.of(2026, 11, 7, 16, 30);
        KetQua<RecordChamCongBienNhan> result = handler.handle(RecordChamCongCommand.checkOut(10, checkOutTime));
        assertTrue(result.isSuccess());

        RecordChamCongBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(new BigDecimal("8.50"), receipt.soGioLam());
        assertEquals(new BigDecimal("0.50"), receipt.soGioVeSom());
        assertEquals(new BigDecimal("0.00"), receipt.soGioDiMuon());
        assertEquals(TrangThaiChamCong.VE_SOM, receipt.trangThai());
    }

    @Test
    @DisplayName("Vừa đi muộn 30 phút vừa về sớm 30 phút: trạng thái DI_MUON_VE_SOM")
    void checkInLateAndCheckOutEarly_setsDiMuonVeSom() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 11, 7, 8, 30);
        handler.handle(RecordChamCongCommand.checkIn(10, checkInTime));

        LocalDateTime checkOutTime = LocalDateTime.of(2026, 11, 7, 16, 30);
        KetQua<RecordChamCongBienNhan> result = handler.handle(RecordChamCongCommand.checkOut(10, checkOutTime));
        assertTrue(result.isSuccess());

        RecordChamCongBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(new BigDecimal("8.00"), receipt.soGioLam());
        assertEquals(new BigDecimal("0.50"), receipt.soGioDiMuon());
        assertEquals(new BigDecimal("0.50"), receipt.soGioVeSom());
        assertEquals(TrangThaiChamCong.DI_MUON_VE_SOM, receipt.trangThai());
    }

    @Test
    @DisplayName("Ghi nhận đồng thời check-in và check-out (recordBoth)")
    void recordBothAtOnce_calculatesAllFieldsCorrectly() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 11, 7, 8, 15); // Muộn 15p = 0.25h
        LocalDateTime checkOutTime = LocalDateTime.of(2026, 11, 7, 16, 45); // Về sớm 15p = 0.25h

        RecordChamCongCommand command = RecordChamCongCommand.recordBoth(10, checkInTime, checkOutTime);
        KetQua<RecordChamCongBienNhan> result = handler.handle(command);
        assertTrue(result.isSuccess());

        RecordChamCongBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(new BigDecimal("8.50"), receipt.soGioLam());
        assertEquals(new BigDecimal("0.25"), receipt.soGioDiMuon());
        assertEquals(new BigDecimal("0.25"), receipt.soGioVeSom());
        assertEquals(TrangThaiChamCong.DI_MUON_VE_SOM, receipt.trangThai());
    }

    @Test
    @DisplayName("Ca đêm qua ngày hôm sau: tính chính xác số giờ về sớm và giờ làm việc")
    void overnightShift_calculatesEarlyLeaveCorrectlyAcrossMidnight() {
        CaTruc nightShift = new CaTruc();
        nightShift.setId(2);
        nightShift.setTenCa("Ca Đêm Cấp Cứu");
        nightShift.setGioBatDau(LocalTime.of(22, 0));
        nightShift.setGioKetThuc(LocalTime.of(6, 0));
        repository.saveShift(nightShift);

        LichLamViec nightSchedule = new LichLamViec();
        nightSchedule.setId(20);
        nightSchedule.setMaNhanVien(101);
        nightSchedule.setMaCaTruc(2);
        nightSchedule.setNgayLamViec(LocalDate.of(2026, 11, 7));
        repository.saveSchedule(nightSchedule);

        LocalDateTime checkInTime = LocalDateTime.of(2026, 11, 7, 22, 0);
        handler.handle(RecordChamCongCommand.checkIn(20, checkInTime));

        // Về sớm 30 phút: 05:30 sáng hôm sau thay vì 06:00
        LocalDateTime checkOutTime = LocalDateTime.of(2026, 11, 8, 5, 30);
        KetQua<RecordChamCongBienNhan> result = handler.handle(RecordChamCongCommand.checkOut(20, checkOutTime));
        assertTrue(result.isSuccess());

        RecordChamCongBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(new BigDecimal("7.50"), receipt.soGioLam());
        assertEquals(new BigDecimal("0.50"), receipt.soGioVeSom());
        assertEquals(TrangThaiChamCong.VE_SOM, receipt.trangThai());
    }

    @Test
    @DisplayName("Từ chối phân quyền khi tài khoản khác không có quyền quản lý chấm công hộ")
    void authorizationDenied_whenNonOwnerAndNonPrivilegedAttemptsCheckIn() {
        PhanQuyenNguCanh unauthorizedAuth = new PhanQuyenNguCanh(
                999, 10, "DOCTOR", 1, 1, UUID.randomUUID()
        );

        RecordChamCongCommand command = RecordChamCongCommand.checkIn(
                10,
                LocalDateTime.of(2026, 11, 7, 8, 0),
                unauthorizedAuth,
                null
        );

        KetQua<RecordChamCongBienNhan> result = handler.handle(command);
        assertFalse(result.isSuccess());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElseThrow());
    }

    @Test
    @DisplayName("Cho phép người dùng tự chấm công cho chính lịch làm việc của mình")
    void authorizationAllowed_whenOwnerChecksIn() {
        PhanQuyenNguCanh ownerAuth = new PhanQuyenNguCanh(
                101, 10, "DOCTOR", 1, 1, UUID.randomUUID()
        );

        RecordChamCongCommand command = RecordChamCongCommand.checkIn(
                10,
                LocalDateTime.of(2026, 11, 7, 8, 0),
                ownerAuth,
                null
        );

        KetQua<RecordChamCongBienNhan> result = handler.handle(command);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Báo lỗi khi mã lịch làm việc không tồn tại")
    void validationError_whenScheduleNotFound() {
        RecordChamCongCommand command = RecordChamCongCommand.checkIn(
                999,
                LocalDateTime.of(2026, 11, 7, 8, 0)
        );

        KetQua<RecordChamCongBienNhan> result = handler.handle(command);
        assertFalse(result.isSuccess());
        assertEquals("SCHEDULE_NOT_FOUND", result.getErrorCode().orElseThrow());
    }

    @Test
    @DisplayName("Báo lỗi khi check-out trước check-in")
    void validationError_whenCheckOutBeforeCheckIn() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 11, 7, 8, 0);
        handler.handle(RecordChamCongCommand.checkIn(10, checkInTime));

        LocalDateTime invalidCheckOut = LocalDateTime.of(2026, 11, 7, 7, 30);
        KetQua<RecordChamCongBienNhan> result = handler.handle(RecordChamCongCommand.checkOut(10, invalidCheckOut));
        assertFalse(result.isSuccess());
        assertEquals("INVALID_TIME", result.getErrorCode().orElseThrow());
    }

    @Test
    @DisplayName("Báo lỗi khi check-out nhưng chưa từng check-in")
    void validationError_whenCheckOutWithoutCheckIn() {
        KetQua<RecordChamCongBienNhan> result = handler.handle(
                RecordChamCongCommand.checkOut(10, LocalDateTime.of(2026, 11, 7, 17, 0))
        );
        assertFalse(result.isSuccess());
        assertEquals("CHECKIN_REQUIRED", result.getErrorCode().orElseThrow());
    }
}
