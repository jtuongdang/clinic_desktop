package vn.clinicmanagement.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.clinicmanagement.domain.common.events.AbstractDomainEvent;
import vn.clinicmanagement.domain.common.events.IDomainEvent;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChiTietKetQuaXetNghiemTest {

    @Test
    @DisplayName("Should initialize lab test detail item with valid defaults and audit fields")
    void initializesWithSensibleDefaults() {
        ChiTietKetQuaXetNghiem item = new ChiTietKetQuaXetNghiem();

        assertNull(item.getId());
        assertEquals(0L, item.getVersion());
        assertNotNull(item.getCreatedAt());
        assertNull(item.getCreatedBy());
        assertNull(item.getUpdatedAt());
        assertNull(item.getUpdatedBy());
        assertFalse(item.isDeleted());
        assertNull(item.getDeletedAt());
        assertNull(item.getDeletedBy());
        assertTrue(item.getDomainEvents().isEmpty());

        assertEquals(0, item.getMaChiDinhDichVu());
        assertEquals("", item.getTenChiSo());
        assertEquals("", item.getGiaTri());
        assertNull(item.getDonViDo());
        assertNull(item.getKhoangThamChieu());
        assertNull(item.getDanhGia());
    }

    @Test
    @DisplayName("Should mutate and retrieve all lab test item attributes")
    void exposesAndMutatesAllLabItemFields() {
        ChiTietKetQuaXetNghiem item = new ChiTietKetQuaXetNghiem();

        item.setId(55);
        item.setMaChiDinhDichVu(120);
        item.setTenChiSo("Glucose máu lúc đói");
        item.setGiaTri("7.2");
        item.setDonViDo("mmol/L");
        item.setKhoangThamChieu("3.9 - 6.4");
        item.setDanhGia("Tăng cao");

        assertEquals(55, item.getId());
        assertEquals(120, item.getMaChiDinhDichVu());
        assertEquals("Glucose máu lúc đói", item.getTenChiSo());
        assertEquals("7.2", item.getGiaTri());
        assertEquals("mmol/L", item.getDonViDo());
        assertEquals("3.9 - 6.4", item.getKhoangThamChieu());
        assertEquals("Tăng cao", item.getDanhGia());

        item.setDonViDo(null);
        item.setKhoangThamChieu(null);
        item.setDanhGia(null);
        assertNull(item.getDonViDo());
        assertNull(item.getKhoangThamChieu());
        assertNull(item.getDanhGia());
    }

    @ParameterizedTest(name = "Clinical evaluation \"{0}\" should be retained accurately")
    @ValueSource(strings = {
            "Bình thường",
            "Cao",
            "Thấp",
            "Nguy hiểm",
            "Dương tính (+)",
            "Âm tính (-)"
    })
    @DisplayName("Should support various clinical qualitative evaluation strings")
    void supportsClinicalEvaluationValues(String evaluation) {
        ChiTietKetQuaXetNghiem item = new ChiTietKetQuaXetNghiem();
        item.setDanhGia(evaluation);

        assertEquals(evaluation, item.getDanhGia());
    }

    @Test
    @DisplayName("Should handle BaseEntity auditing, soft delete, and restore lifecycles")
    void supportsBaseEntityLifecycleAndActorValidation() {
        ChiTietKetQuaXetNghiem item = new ChiTietKetQuaXetNghiem();

        item.setVersion(2L);
        assertEquals(2L, item.getVersion());

        item.setCreatedBy(8);
        assertEquals(8, item.getCreatedBy());
        assertThrows(IllegalArgumentException.class, () -> item.setCreatedBy(0));
        assertThrows(IllegalArgumentException.class, () -> item.setCreatedBy(-1));

        item.markAsUpdated(14);
        assertEquals(14, item.getUpdatedBy());
        assertNotNull(item.getUpdatedAt());
        assertThrows(IllegalArgumentException.class, () -> item.markAsUpdated(0));
        assertThrows(IllegalArgumentException.class, () -> item.markAsUpdated(-4));

        item.softDelete(18);
        assertTrue(item.isDeleted());
        assertEquals(18, item.getDeletedBy());
        assertNotNull(item.getDeletedAt());
        assertThrows(IllegalArgumentException.class, () -> item.softDelete(0));
        assertThrows(IllegalArgumentException.class, () -> item.softDelete(-2));

        // Soft-deleting an already deleted entity is idempotent
        item.softDelete(99);
        assertEquals(18, item.getDeletedBy());

        item.restore();
        assertFalse(item.isDeleted());
        assertNull(item.getDeletedAt());
        assertNull(item.getDeletedBy());
    }

    @Test
    @DisplayName("Should manage domain events through the BaseEntity domain event hub")
    void managesDomainEventsHub() {
        ChiTietKetQuaXetNghiem item = new ChiTietKetQuaXetNghiem();
        IDomainEvent event = new TestDomainEvent();

        item.registerDomainEvent(event);
        List<IDomainEvent> events = item.getDomainEvents();
        assertEquals(1, events.size());

        List<IDomainEvent> pulled = item.pullDomainEvents();
        assertEquals(1, pulled.size());
        assertTrue(item.getDomainEvents().isEmpty());
    }

    private static class TestDomainEvent extends AbstractDomainEvent {}

    @Test
    @DisplayName("Should respect identity-based equality and hash code contracts")
    void respectsEqualsAndHashCodeContracts() {
        ChiTietKetQuaXetNghiem item1 = new ChiTietKetQuaXetNghiem();
        ChiTietKetQuaXetNghiem item2 = new ChiTietKetQuaXetNghiem();

        // Null IDs are not considered equal
        assertNotEquals(item1, item2);

        item1.setId(30);
        item2.setId(30);
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());

        item2.setId(40);
        assertNotEquals(item1, item2);

        assertEquals(item1, item1);
        assertNotEquals(item1, null);
        assertNotEquals(item1, "some other object");
    }
}
