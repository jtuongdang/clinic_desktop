package vn.clinicmanagement.application.attendance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.TrangThaiChamCong;
import vn.clinicmanagement.domain.identity.CaTruc;
import vn.clinicmanagement.domain.identity.ChamCong;
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

class GetChamCongTheoKyHandlerTest {

    private InBoNhoChamCongRepository repository;
    private GetChamCongTheoKyHandler handler;

    private final int employeeId = 201;

    @BeforeEach
    void setUp() {
        repository = new InBoNhoChamCongRepository();
        handler = new GetChamCongTheoKyHandler(repository);

        // Tạo 2 ca trực
        CaTruc morningShift = new CaTruc();
        morningShift.setId(1);
        morningShift.setTenCa("Ca Sáng");
        morningShift.setGioBatDau(LocalTime.of(8, 0));
        morningShift.setGioKetThuc(LocalTime.of(12, 0));
        repository.saveShift(morningShift);

        CaTruc afternoonShift = new CaTruc();
        afternoonShift.setId(2);
        afternoonShift.setTenCa("Ca Chiều");
        afternoonShift.setGioBatDau(LocalTime.of(13, 0));
        afternoonShift.setGioKetThuc(LocalTime.of(17, 0));
        repository.saveShift(afternoonShift);

        // Ngày 1: 2026-11-01 - Ca sáng - Đúng giờ (4h)
        LichLamViec sch1 = new LichLamViec();
        sch1.setId(11);
        sch1.setMaNhanVien(employeeId);
        sch1.setMaCaTruc(1);
        sch1.setNgayLamViec(LocalDate.of(2026, 11, 1));
        repository.saveSchedule(sch1);

        ChamCong cc1 = new ChamCong();
        cc1.setId(101);
        cc1.setMaLichLamViec(11);
        cc1.setThoiGianCheckIn(LocalDateTime.of(2026, 11, 1, 8, 0));
        cc1.setThoiGianCheckOut(LocalDateTime.of(2026, 11, 1, 12, 0));
        cc1.setSoGioLam(new BigDecimal("4.00"));
        cc1.setSoGioDiMuon(BigDecimal.ZERO);
        cc1.setSoGioVeSom(BigDecimal.ZERO);
        cc1.setTrangThai(TrangThaiChamCong.DI_DUNG_GIO);
        repository.saveAttendance(cc1);

        // Ngày 2: 2026-11-02 - Ca chiều - Đi muộn 0.5h, về sớm 0.5h (3h làm)
        LichLamViec sch2 = new LichLamViec();
        sch2.setId(12);
        sch2.setMaNhanVien(employeeId);
        sch2.setMaCaTruc(2);
        sch2.setNgayLamViec(LocalDate.of(2026, 11, 2));
        repository.saveSchedule(sch2);

        ChamCong cc2 = new ChamCong();
        cc2.setId(102);
        cc2.setMaLichLamViec(12);
        cc2.setThoiGianCheckIn(LocalDateTime.of(2026, 11, 2, 13, 30));
        cc2.setThoiGianCheckOut(LocalDateTime.of(2026, 11, 2, 16, 30));
        cc2.setSoGioLam(new BigDecimal("3.00"));
        cc2.setSoGioDiMuon(new BigDecimal("0.50"));
        cc2.setSoGioVeSom(new BigDecimal("0.50"));
        cc2.setTrangThai(TrangThaiChamCong.DI_MUON_VE_SOM);
        repository.saveAttendance(cc2);

        // Ngày 3: 2026-11-03 - Ca sáng - Có lịch nhưng chưa chấm công (Vắng / Chưa check-in)
        LichLamViec sch3 = new LichLamViec();
        sch3.setId(13);
        sch3.setMaNhanVien(employeeId);
        sch3.setMaCaTruc(1);
        sch3.setNgayLamViec(LocalDate.of(2026, 11, 3));
        repository.saveSchedule(sch3);
    }

    @Test
    @DisplayName("Tra cứu chấm công theo khoảng thời gian: tính đúng số ca, số giờ làm và chi tiết")
    void getAttendanceSummary_success() {
        GetChamCongTheoKyQuery query = new GetChamCongTheoKyQuery(
                employeeId,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 5)
        );

        KetQua<ChamCongTongHopDuLieu> result = handler.handle(query);
        assertTrue(result.isSuccess());

        ChamCongTongHopDuLieu summary = result.getValue().orElseThrow();
        assertEquals(employeeId, summary.employeeId());
        assertEquals(LocalDate.of(2026, 11, 1), summary.fromDate());
        assertEquals(LocalDate.of(2026, 11, 5), summary.toDate());

        // Tổng số ca phân công: 3 ca
        assertEquals(3, summary.totalScheduledShifts());
        // Số ca đã chấm công: 2 ca
        assertEquals(2, summary.attendedShifts());
        // Tổng số giờ làm: 4.00 + 3.00 = 7.00
        assertEquals(new BigDecimal("7.00"), summary.totalWorkHours());
        // Tổng số giờ đi muộn: 0.50
        assertEquals(new BigDecimal("0.50"), summary.totalLateHours());
        assertEquals(1, summary.lateCount());
        // Tổng số giờ về sớm: 0.50
        assertEquals(new BigDecimal("0.50"), summary.totalEarlyLeaveHours());
        assertEquals(1, summary.earlyLeaveCount());

        // Kiểm tra danh sách chi tiết bản ghi
        assertEquals(2, summary.records().size());
        assertEquals("Ca Sáng", summary.records().get(0).tenCa());
        assertEquals(new BigDecimal("4.00"), summary.records().get(0).soGioLam());
        assertEquals("Ca Chiều", summary.records().get(1).tenCa());
        assertEquals(new BigDecimal("3.00"), summary.records().get(1).soGioLam());
    }

    @Test
    @DisplayName("Nhân viên được phép tự tra cứu chấm công của chính mình")
    void employeeCanQueryOwnAttendance() {
        PhanQuyenNguCanh selfAuth = new PhanQuyenNguCanh(
                employeeId, 10, "NURSE", 1, 1, UUID.randomUUID()
        );

        GetChamCongTheoKyQuery query = new GetChamCongTheoKyQuery(
                employeeId,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 5),
                selfAuth
        );

        KetQua<ChamCongTongHopDuLieu> result = handler.handle(query);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Quản lý (HR_ADMIN / MANAGER) được phép tra cứu chấm công của nhân viên")
    void managerCanQueryEmployeeAttendance() {
        PhanQuyenNguCanh hrAuth = new PhanQuyenNguCanh(
                5, 2, "HR_ADMIN", 1, 1, UUID.randomUUID()
        );

        GetChamCongTheoKyQuery query = new GetChamCongTheoKyQuery(
                employeeId,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 5),
                hrAuth
        );

        KetQua<ChamCongTongHopDuLieu> result = handler.handle(query);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Từ chối khi nhân viên khác không có quyền quản lý tra cứu chấm công của đồng nghiệp")
    void unauthorizedEmployeeCannotQueryOtherAttendance() {
        PhanQuyenNguCanh otherAuth = new PhanQuyenNguCanh(
                999, 10, "DOCTOR", 1, 1, UUID.randomUUID()
        );

        GetChamCongTheoKyQuery query = new GetChamCongTheoKyQuery(
                employeeId,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 5),
                otherAuth
        );

        KetQua<ChamCongTongHopDuLieu> result = handler.handle(query);
        assertFalse(result.isSuccess());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElseThrow());
    }

    @Test
    @DisplayName("Báo lỗi khi ngày kết thúc trước ngày bắt đầu")
    void invalidDateRange_fails() {
        GetChamCongTheoKyQuery query = new GetChamCongTheoKyQuery(
                employeeId,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 1)
        );

        KetQua<ChamCongTongHopDuLieu> result = handler.handle(query);
        assertFalse(result.isSuccess());
        assertEquals("INVALID_DATE_RANGE", result.getErrorCode().orElseThrow());
    }

    @Test
    @DisplayName("Báo lỗi khi mã nhân viên không hợp lệ")
    void invalidEmployeeId_fails() {
        GetChamCongTheoKyQuery query = new GetChamCongTheoKyQuery(
                0,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 5)
        );

        KetQua<ChamCongTongHopDuLieu> result = handler.handle(query);
        assertFalse(result.isSuccess());
        assertEquals("INVALID_EMPLOYEE", result.getErrorCode().orElseThrow());
    }
}
