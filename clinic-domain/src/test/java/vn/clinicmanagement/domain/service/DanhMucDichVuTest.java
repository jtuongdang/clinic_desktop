package vn.clinicmanagement.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.common.events.AbstractDomainEvent;
import vn.clinicmanagement.domain.common.events.IDomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DanhMucDichVuTest {

    @Test
    @DisplayName("Should initialize service catalog with valid domain defaults and implement IAggregateRoot")
    void initializesWithSensibleDefaults() {
        DanhMucDichVu service = new DanhMucDichVu();

        assertInstanceOf(IAggregateRoot.class, service);
        assertNull(service.getId());
        assertEquals(0L, service.getVersion());
        assertNotNull(service.getCreatedAt());
        assertNull(service.getCreatedBy());
        assertNull(service.getUpdatedAt());
        assertNull(service.getUpdatedBy());
        assertFalse(service.isDeleted());
        assertNull(service.getDeletedAt());
        assertNull(service.getDeletedBy());
        assertTrue(service.getDomainEvents().isEmpty());

        assertEquals("", service.getMaDichVu());
        assertEquals("", service.getTenDichVu());
        assertEquals(LoaiDichVu.KHAM, service.getLoaiDichVu());
        assertEquals(BigDecimal.ZERO, service.getDonGia());
        assertEquals(BigDecimal.ZERO, service.getGiaBHYT());
        assertTrue(service.isHoatDong());
        assertNull(service.getMoTa());
    }

    @Test
    @DisplayName("Should mutate and retrieve all service catalog attributes")
    void exposesAndMutatesAllServiceCatalogFields() {
        DanhMucDichVu service = new DanhMucDichVu();

        service.setId(42);
        service.setMaDichVu("CLS-XN-001");
        service.setTenDichVu("Tổng phân tích tế bào máu ngoại vi bằng laser (24 thông số)");
        service.setLoaiDichVu(LoaiDichVu.XET_NGHIEM);
        service.setDonGia(new BigDecimal("120000.00"));
        service.setGiaBHYT(new BigDecimal("48600.00"));
        service.setHoatDong(false);
        service.setMoTa("Xét nghiệm huyết học thường quy");

        assertEquals(42, service.getId());
        assertEquals("CLS-XN-001", service.getMaDichVu());
        assertEquals("Tổng phân tích tế bào máu ngoại vi bằng laser (24 thông số)", service.getTenDichVu());
        assertEquals(LoaiDichVu.XET_NGHIEM, service.getLoaiDichVu());
        assertEquals(new BigDecimal("120000.00"), service.getDonGia());
        assertEquals(new BigDecimal("48600.00"), service.getGiaBHYT());
        assertFalse(service.isHoatDong());
        assertEquals("Xét nghiệm huyết học thường quy", service.getMoTa());

        service.setHoatDong(true);
        assertTrue(service.isHoatDong());

        service.setMoTa(null);
        assertNull(service.getMoTa());
    }

    @ParameterizedTest(name = "LoaiDichVu {0} should be set and validated accurately")
    @EnumSource(LoaiDichVu.class)
    @DisplayName("Should accept all defined LoaiDichVu enum classifications")
    void supportsAllServiceTypes(LoaiDichVu serviceType) {
        DanhMucDichVu service = new DanhMucDichVu();
        service.setLoaiDichVu(serviceType);

        assertEquals(serviceType, service.getLoaiDichVu());
        assertTrue(serviceType.getValue() >= 1 && serviceType.getValue() <= 4);
        assertNotNull(serviceType.getDescription());
        assertEquals(serviceType, LoaiDichVu.fromValue(serviceType.getValue()));
    }

    @Test
    @DisplayName("Should handle BaseEntity auditing, soft delete, and restore lifecycles")
    void supportsBaseEntityLifecycleAndActorValidation() {
        DanhMucDichVu service = new DanhMucDichVu();

        service.setVersion(1L);
        assertEquals(1L, service.getVersion());

        service.setCreatedBy(5);
        assertEquals(5, service.getCreatedBy());
        assertThrows(IllegalArgumentException.class, () -> service.setCreatedBy(0));
        assertThrows(IllegalArgumentException.class, () -> service.setCreatedBy(-10));

        service.markAsUpdated(12);
        assertEquals(12, service.getUpdatedBy());
        assertNotNull(service.getUpdatedAt());
        assertThrows(IllegalArgumentException.class, () -> service.markAsUpdated(0));
        assertThrows(IllegalArgumentException.class, () -> service.markAsUpdated(-1));

        service.softDelete(16);
        assertTrue(service.isDeleted());
        assertEquals(16, service.getDeletedBy());
        assertNotNull(service.getDeletedAt());
        assertThrows(IllegalArgumentException.class, () -> service.softDelete(0));
        assertThrows(IllegalArgumentException.class, () -> service.softDelete(-5));

        // Soft-deleting when already deleted does nothing
        service.softDelete(99);
        assertEquals(16, service.getDeletedBy());

        service.restore();
        assertFalse(service.isDeleted());
        assertNull(service.getDeletedAt());
        assertNull(service.getDeletedBy());
    }

    @Test
    @DisplayName("Should manage domain events through the BaseEntity domain event hub")
    void managesDomainEventsHub() {
        DanhMucDichVu service = new DanhMucDichVu();
        IDomainEvent event = new TestDomainEvent();

        service.registerDomainEvent(event);
        List<IDomainEvent> events = service.getDomainEvents();
        assertEquals(1, events.size());

        List<IDomainEvent> pulled = service.pullDomainEvents();
        assertEquals(1, pulled.size());
        assertTrue(service.getDomainEvents().isEmpty());
    }

    private static class TestDomainEvent extends AbstractDomainEvent {}

    @Test
    @DisplayName("Should respect identity-based equality and hash code contracts")
    void respectsEqualsAndHashCodeContracts() {
        DanhMucDichVu service1 = new DanhMucDichVu();
        DanhMucDichVu service2 = new DanhMucDichVu();

        // Null IDs are not considered equal
        assertNotEquals(service1, service2);

        service1.setId(10);
        service2.setId(10);
        assertEquals(service1, service2);
        assertEquals(service1.hashCode(), service2.hashCode());

        service2.setId(20);
        assertNotEquals(service1, service2);

        assertEquals(service1, service1);
        assertNotEquals(service1, null);
        assertNotEquals(service1, "a string");
    }
}
