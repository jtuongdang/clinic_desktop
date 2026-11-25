package vn.clinicmanagement.domain.identity;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.enums.GioiTinh;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NhanVienTest {

    @Test
    void initializesEmployeeDefaultsAndBaseEntityMetadata() {
        NhanVien employee = new NhanVien();

        assertEquals("", employee.getMaNhanVien());
        assertEquals("", employee.getHo());
        assertEquals("", employee.getTen());
        assertEquals("", employee.getHoTen());
        assertEquals("", employee.getSoDienThoai());
        assertNull(employee.getEmail());
        assertNull(employee.getDiaChi());
        assertEquals(GioiTinh.M, employee.getGioiTinh());
        assertNull(employee.getNgaySinh());
        assertNull(employee.getMaChucVu());
        assertEquals("", employee.getUsername());
        assertEquals("", employee.getPasswordHash());
        assertTrue(employee.isHoatDong());
        assertNull(employee.getDangNhapCuoi());
        assertNull(employee.getChungChiHanhNghe());
        assertNull(employee.getNgayVaoLam());
        assertNull(employee.getNgayNghiViec());
        assertNull(employee.getLyDoNghiViec());
        assertEquals(BigDecimal.ZERO, employee.getLuongCoBan());
        assertEquals(BigDecimal.ONE, employee.getHeSoLuong());
        assertEquals(BigDecimal.ZERO, employee.getPhuCap());
        assertEquals(0L, employee.getVersion());
        assertFalse(employee.isDeleted());
        assertNotNull(employee.getCreatedAt());
    }

    @Test
    void exposesEmployeePropertiesThroughGettersAndSetters() {
        NhanVien employee = new NhanVien();
        LocalDate date = LocalDate.of(1990, 5, 20);
        LocalDateTime loginTime = LocalDateTime.of(2024, 3, 10, 9, 15);
        BigDecimal baseSalary = new BigDecimal("12500000.00");
        BigDecimal salaryFactor = new BigDecimal("1.25");
        BigDecimal allowance = new BigDecimal("750000.00");

        employee.setId(42);
        employee.setMaNhanVien("NV-SYN-001");
        employee.setHo("Nguyen");
        employee.setTen("An");
        employee.setSoDienThoai("0900000000");
        employee.setEmail("an@example.test");
        employee.setDiaChi("Synthetic address");
        employee.setGioiTinh(GioiTinh.F);
        employee.setNgaySinh(date);
        employee.setMaChucVu(7);
        employee.setUsername("synthetic-user");
        employee.setPasswordHash("synthetic-hash");
        employee.setHoatDong(false);
        employee.setDangNhapCuoi(loginTime);
        employee.setChungChiHanhNghe("Synthetic certificate");
        employee.setNgayVaoLam(date.plusYears(20));
        employee.setNgayNghiViec(date.plusYears(30));
        employee.setLyDoNghiViec("Synthetic reason");
        employee.setLuongCoBan(baseSalary);
        employee.setHeSoLuong(salaryFactor);
        employee.setPhuCap(allowance);

        assertEquals(42, employee.getId());
        assertEquals("NV-SYN-001", employee.getMaNhanVien());
        assertEquals("Nguyen", employee.getHo());
        assertEquals("An", employee.getTen());
        assertEquals("Nguyen An", employee.getHoTen());
        assertEquals("0900000000", employee.getSoDienThoai());
        assertEquals("an@example.test", employee.getEmail());
        assertEquals("Synthetic address", employee.getDiaChi());
        assertEquals(GioiTinh.F, employee.getGioiTinh());
        assertEquals(date, employee.getNgaySinh());
        assertEquals(7, employee.getMaChucVu());
        assertEquals("synthetic-user", employee.getUsername());
        assertEquals("synthetic-hash", employee.getPasswordHash());
        assertFalse(employee.isHoatDong());
        assertEquals(loginTime, employee.getDangNhapCuoi());
        assertEquals("Synthetic certificate", employee.getChungChiHanhNghe());
        assertEquals(date.plusYears(20), employee.getNgayVaoLam());
        assertEquals(date.plusYears(30), employee.getNgayNghiViec());
        assertEquals("Synthetic reason", employee.getLyDoNghiViec());
        assertEquals(baseSalary, employee.getLuongCoBan());
        assertEquals(salaryFactor, employee.getHeSoLuong());
        assertEquals(allowance, employee.getPhuCap());
    }

    @Test
    void composesFullNameFromAvailableNameParts() {
        NhanVien employee = new NhanVien();

        assertEquals("", employee.getHoTen());
        employee.setHo(" Nguyen ");
        assertEquals("Nguyen", employee.getHoTen());
        employee.setTen(" An ");
        assertEquals("Nguyen   An", employee.getHoTen());
        employee.setHo("");
        assertEquals("An", employee.getHoTen());
    }

    @Test
    void supportsUpdateSoftDeleteAndRestoreLifecycle() {
        NhanVien employee = new NhanVien();

        employee.markAsUpdated(21);
        assertEquals(21, employee.getUpdatedBy());
        assertNotNull(employee.getUpdatedAt());

        employee.softDelete(22);
        assertTrue(employee.isDeleted());
        assertEquals(22, employee.getDeletedBy());
        assertNotNull(employee.getDeletedAt());

        employee.restore();
        assertFalse(employee.isDeleted());
        assertNull(employee.getDeletedAt());
        assertNull(employee.getDeletedBy());
    }

    @Test
    void rejectsInvalidLifecycleActors() {
        NhanVien employee = new NhanVien();

        assertThrows(IllegalArgumentException.class, () -> employee.markAsUpdated(0));
        assertThrows(IllegalArgumentException.class, () -> employee.markAsUpdated(-1));
        assertThrows(IllegalArgumentException.class, () -> employee.softDelete(0));
        assertThrows(IllegalArgumentException.class, () -> employee.softDelete(-1));
    }
}
