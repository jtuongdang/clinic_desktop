package vn.clinicmanagement.domain.billing;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CongNoBenhNhanTest {

    @Test
    void defaultsToOutstandingDebtWithZeroBalance() {
        CongNoBenhNhan debt = new CongNoBenhNhan();

        assertEquals(0, debt.getMaHoaDon());
        assertEquals(0, debt.getMaBenhNhan());
        assertEquals(BigDecimal.ZERO, debt.getSoTienNo());
        assertEquals(BigDecimal.ZERO, debt.getSoTienDaTra());
        assertEquals(BigDecimal.ZERO, debt.getSoTienConLai());
        assertNull(debt.getHanThanhToan());
        assertEquals(TrangThaiCongNo.DANG_NO, debt.getTrangThai());
        assertNull(debt.getHoaDon());
    }

    @Test
    void calculatesRemainingDebtAsAmountOwedMinusAmountPaid() {
        CongNoBenhNhan debt = new CongNoBenhNhan();
        debt.setSoTienNo(new BigDecimal("850.75"));
        debt.setSoTienDaTra(new BigDecimal("300.25"));

        assertEquals(new BigDecimal("550.50"), debt.getSoTienConLai());

        debt.setSoTienDaTra(new BigDecimal("850.75"));

        assertEquals(new BigDecimal("0.00"), debt.getSoTienConLai());
    }

    @Test
    void storesDebtPropertiesAndInvoiceAssociation() {
        CongNoBenhNhan debt = new CongNoBenhNhan();
        HoaDon invoice = new HoaDon();
        LocalDate dueDate = LocalDate.of(2026, 12, 7);

        debt.setMaHoaDon(42);
        debt.setMaBenhNhan(34);
        debt.setSoTienNo(new BigDecimal("1000.00"));
        debt.setSoTienDaTra(new BigDecimal("250.00"));
        debt.setHanThanhToan(dueDate);
        debt.setTrangThai(TrangThaiCongNo.QUA_HAN);
        debt.setHoaDon(invoice);

        assertEquals(42, debt.getMaHoaDon());
        assertEquals(34, debt.getMaBenhNhan());
        assertEquals(new BigDecimal("1000.00"), debt.getSoTienNo());
        assertEquals(new BigDecimal("250.00"), debt.getSoTienDaTra());
        assertEquals(dueDate, debt.getHanThanhToan());
        assertEquals(TrangThaiCongNo.QUA_HAN, debt.getTrangThai());
        assertEquals(invoice, debt.getHoaDon());
        assertEquals(new BigDecimal("750.00"), debt.getSoTienConLai());
    }
}
