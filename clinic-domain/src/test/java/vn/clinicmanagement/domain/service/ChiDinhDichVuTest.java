package vn.clinicmanagement.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.common.events.TruuTuongMienNghiepVuSuKien;
import vn.clinicmanagement.domain.common.events.IMienNghiepVuSuKien;
import vn.clinicmanagement.domain.examination.LuotKham;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChiDinhDichVuTest {

    @Test
    @DisplayName("Should initialize service order with valid business defaults and audit fields")
    void initializesWithSensibleDefaults() {
        ChiDinhDichVu order = new ChiDinhDichVu();

        assertNull(order.getId());
        assertEquals(0L, order.getVersion());
        assertNotNull(order.getCreatedAt());
        assertNull(order.getCreatedBy());
        assertNull(order.getUpdatedAt());
        assertNull(order.getUpdatedBy());
        assertFalse(order.isDeleted());
        assertNull(order.getDeletedAt());
        assertNull(order.getDeletedBy());
        assertTrue(order.getDomainEvents().isEmpty());

        assertEquals(0, order.getMaLuotKham());
        assertEquals(0, order.getMaDichVu());
        assertEquals(0, order.getMaBacSiChiDinh());
        assertEquals(BigDecimal.ONE, order.getSoLuong());
        assertEquals(BigDecimal.ZERO, order.getDonGia());
        assertEquals(TrangThaiChiDinhDichVu.DA_CHI_DINH, order.getTrangThai());
        assertNull(order.getKetQua());
        assertNull(order.getKetLuan());
        assertNotNull(order.getThoiGianChiDinh());
        assertNull(order.getThoiGianThucHien());

        assertNull(order.getMaPhongThucHien());
        assertNull(order.getBacSiThucHien());
        assertEquals("", order.getDanhSachHinhAnhUrl());
        assertNull(order.getThoiGianTraKetQua());

        assertNull(order.getLuotKham());
        assertNull(order.getDanhMucDichVu());
    }

    @Test
    @DisplayName("Should mutate and expose all service order fields through getters and setters")
    void exposesAndMutatesAllServiceOrderFields() {
        ChiDinhDichVu order = new ChiDinhDichVu();
        LocalDateTime orderTime = LocalDateTime.of(2026, 11, 7, 8, 15, 0);
        LocalDateTime performTime = LocalDateTime.of(2026, 11, 7, 9, 30, 0);
        LocalDateTime returnTime = LocalDateTime.of(2026, 11, 7, 10, 0, 0);

        order.setId(101);
        order.setMaLuotKham(202);
        order.setMaDichVu(303);
        order.setMaBacSiChiDinh(404);
        order.setSoLuong(new BigDecimal("2.00"));
        order.setDonGia(new BigDecimal("250000.00"));
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CO_KET_QUA);
        order.setKetQua("Chụp X-quang tim phổi thẳng: Không thấy thâm nhiễm phế trường");
        order.setKetLuan("Tim phổi bình thường");
        order.setThoiGianChiDinh(orderTime);
        order.setThoiGianThucHien(performTime);

        order.setMaPhongThucHien(14);
        order.setBacSiThucHien(88);
        order.setDanhSachHinhAnhUrl("https://pacs.clinic.local/dicom/study-998877");
        order.setThoiGianTraKetQua(returnTime);

        assertEquals(101, order.getId());
        assertEquals(202, order.getMaLuotKham());
        assertEquals(303, order.getMaDichVu());
        assertEquals(404, order.getMaBacSiChiDinh());
        assertEquals(new BigDecimal("2.00"), order.getSoLuong());
        assertEquals(new BigDecimal("250000.00"), order.getDonGia());
        assertEquals(TrangThaiChiDinhDichVu.DA_CO_KET_QUA, order.getTrangThai());
        assertEquals("Chụp X-quang tim phổi thẳng: Không thấy thâm nhiễm phế trường", order.getKetQua());
        assertEquals("Tim phổi bình thường", order.getKetLuan());
        assertEquals(orderTime, order.getThoiGianChiDinh());
        assertEquals(performTime, order.getThoiGianThucHien());

        assertEquals(14, order.getMaPhongThucHien());
        assertEquals(88, order.getBacSiThucHien());
        assertEquals("https://pacs.clinic.local/dicom/study-998877", order.getDanhSachHinhAnhUrl());
        assertEquals(returnTime, order.getThoiGianTraKetQua());
    }

    @Test
    @DisplayName("Should accept nullable values for optional diagnostic reporting fields")
    void allowsNullableOptionalFields() {
        ChiDinhDichVu order = new ChiDinhDichVu();

        order.setKetQua(null);
        order.setKetLuan(null);
        order.setThoiGianThucHien(null);
        order.setMaPhongThucHien(null);
        order.setBacSiThucHien(null);
        order.setDanhSachHinhAnhUrl(null);
        order.setThoiGianTraKetQua(null);

        assertNull(order.getKetQua());
        assertNull(order.getKetLuan());
        assertNull(order.getThoiGianThucHien());
        assertNull(order.getMaPhongThucHien());
        assertNull(order.getBacSiThucHien());
        assertNull(order.getDanhSachHinhAnhUrl());
        assertNull(order.getThoiGianTraKetQua());
    }

    @Test
    @DisplayName("Should maintain navigation references to LuotKham and DanhMucDichVu")
    void associatesWithVisitAndServiceCatalogEntities() {
        ChiDinhDichVu order = new ChiDinhDichVu();
        LuotKham visit = new LuotKham();
        DanhMucDichVu serviceCatalog = new DanhMucDichVu();

        order.setLuotKham(visit);
        order.setDanhMucDichVu(serviceCatalog);

        assertSame(visit, order.getLuotKham());
        assertSame(serviceCatalog, order.getDanhMucDichVu());

        order.setLuotKham(null);
        order.setDanhMucDichVu(null);
        assertNull(order.getLuotKham());
        assertNull(order.getDanhMucDichVu());
    }

    @ParameterizedTest(name = "TrangThaiChiDinhDichVu {0} should be set and retrieved accurately")
    @EnumSource(TrangThaiChiDinhDichVu.class)
    @DisplayName("Should transition cleanly between all allowed order lifecycle states")
    void supportsAllStateTransitions(TrangThaiChiDinhDichVu targetState) {
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setTrangThai(targetState);

        assertEquals(targetState, order.getTrangThai());
        assertTrue(targetState.getValue() >= 1 && targetState.getValue() <= 5);
        assertNotNull(targetState.getDescription());
        assertEquals(targetState, TrangThaiChiDinhDichVu.fromValue(targetState.getValue()));
    }

    @Test
    @DisplayName("Should support BaseEntity update, soft delete, and restore lifecycles with validation")
    void supportsBaseEntityLifecycleAndActorValidation() {
        ChiDinhDichVu order = new ChiDinhDichVu();

        order.setVersion(3L);
        assertEquals(3L, order.getVersion());

        order.setCreatedBy(10);
        assertEquals(10, order.getCreatedBy());
        assertThrows(IllegalArgumentException.class, () -> order.setCreatedBy(0));
        assertThrows(IllegalArgumentException.class, () -> order.setCreatedBy(-5));

        order.markAsUpdated(15);
        assertEquals(15, order.getUpdatedBy());
        assertNotNull(order.getUpdatedAt());
        assertThrows(IllegalArgumentException.class, () -> order.markAsUpdated(0));
        assertThrows(IllegalArgumentException.class, () -> order.markAsUpdated(-1));

        order.setUpdatedBy(20);
        assertEquals(20, order.getUpdatedBy());
        assertThrows(IllegalArgumentException.class, () -> order.setUpdatedBy(-3));

        order.softDelete(25);
        assertTrue(order.isDeleted());
        assertEquals(25, order.getDeletedBy());
        assertNotNull(order.getDeletedAt());
        assertThrows(IllegalArgumentException.class, () -> order.softDelete(0));
        assertThrows(IllegalArgumentException.class, () -> order.softDelete(-2));

        // Soft-deleting an already deleted entity is idempotent
        order.softDelete(99);
        assertEquals(25, order.getDeletedBy());

        order.restore();
        assertFalse(order.isDeleted());
        assertNull(order.getDeletedAt());
        assertNull(order.getDeletedBy());

        // Restoring a non-deleted entity is idempotent
        order.restore();
        assertFalse(order.isDeleted());
    }

    @Test
    @DisplayName("Should manage domain events lifecycle correctly")
    void managesDomainEventsHub() {
        ChiDinhDichVu order = new ChiDinhDichVu();
        IMienNghiepVuSuKien event1 = new TestDomainEvent();
        IMienNghiepVuSuKien event2 = new TestDomainEvent();

        assertThrows(NullPointerException.class, () -> order.registerDomainEvent(null));

        order.registerDomainEvent(event1);
        order.registerDomainEvent(event2);

        List<IMienNghiepVuSuKien> events = order.getDomainEvents();
        assertEquals(2, events.size());
        assertThrows(UnsupportedOperationException.class, () -> events.add(new TestDomainEvent()));

        List<IMienNghiepVuSuKien> pulled = order.pullDomainEvents();
        assertEquals(2, pulled.size());
        assertTrue(order.getDomainEvents().isEmpty());

        order.registerDomainEvent(event1);
        order.clearDomainEvents();
        assertTrue(order.getDomainEvents().isEmpty());
    }

    private static class TestDomainEvent extends TruuTuongMienNghiepVuSuKien {}

    @Test
    @DisplayName("Should satisfy identity-based equality and hash code semantics")
    void respectsEqualsAndHashCodeContracts() {
        ChiDinhDichVu order1 = new ChiDinhDichVu();
        ChiDinhDichVu order2 = new ChiDinhDichVu();

        // Null IDs are not equal
        assertNotEquals(order1, order2);

        order1.setId(500);
        order2.setId(500);
        assertEquals(order1, order2);
        assertEquals(order1.hashCode(), order2.hashCode());

        order2.setId(600);
        assertNotEquals(order1, order2);

        assertEquals(order1, order1);
        assertNotEquals(order1, null);
        assertNotEquals(order1, new Object());
    }
}
