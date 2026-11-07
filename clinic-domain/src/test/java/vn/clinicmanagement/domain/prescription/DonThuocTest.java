package vn.clinicmanagement.domain.prescription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.common.events.DonThuocApprovedEvent;
import vn.clinicmanagement.domain.common.events.DonThuocDispensedEvent;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonThuocTest {

    @Test
    void defaultsToTemporaryPrescriptionWithEmptyDetails() {
        DonThuoc prescription = new DonThuoc();

        assertEquals(TrangThaiDonThuoc.TAM, prescription.getTrangThai());
        assertNotNull(prescription.getNgayKe());
        assertEquals("", prescription.getGhiChu());
        assertEquals("", prescription.getLoiDanDonThuoc());
        assertNull(prescription.getNgayCapThuoc());
        assertNull(prescription.getMaNguoiCapThuoc());
        assertNull(prescription.getMaKhoXuat());
        assertTrue(prescription.getChiTietDonThuocs().isEmpty());
    }

    @Test
    void approvesTemporaryPrescriptionAndRegistersApprovalEvent() {
        DonThuoc prescription = prescriptionInState(TrangThaiDonThuoc.TAM);

        prescription.approve();

        assertEquals(TrangThaiDonThuoc.DA_DUYET, prescription.getTrangThai());
        assertEquals(1, prescription.getDomainEvents().size());
        DonThuocApprovedEvent event = assertInstanceOf(
                DonThuocApprovedEvent.class, prescription.getDomainEvents().get(0));
        assertEquals(42, event.getPrescriptionId());
        assertEquals(7, event.getVisitId());
        assertEquals(11, event.getDoctorId());
    }

    @ParameterizedTest
    @EnumSource(value = TrangThaiDonThuoc.class, mode = EnumSource.Mode.EXCLUDE, names = "TAM")
    void approvalRejectsNonTemporaryPrescriptionWithoutChangingStateOrEvents(TrangThaiDonThuoc initialState) {
        DonThuoc prescription = prescriptionInState(initialState);
        List<?> eventsBefore = List.copyOf(prescription.getDomainEvents());

        assertThrows(IllegalStateException.class, prescription::approve);

        assertEquals(initialState, prescription.getTrangThai());
        assertEquals(eventsBefore, prescription.getDomainEvents());
    }

    @Test
    void completesDispenseAfterApprovalAndRegistersDispenseEvent() {
        DonThuoc prescription = prescriptionInState(TrangThaiDonThuoc.DA_DUYET);

        prescription.completeDispense(19);

        assertEquals(TrangThaiDonThuoc.DA_XUAT_HET, prescription.getTrangThai());
        assertEquals(19, prescription.getMaNguoiCapThuoc());
        assertNotNull(prescription.getNgayCapThuoc());
        assertEquals(1, prescription.getDomainEvents().size());
        DonThuocDispensedEvent event = assertInstanceOf(
                DonThuocDispensedEvent.class, prescription.getDomainEvents().get(0));
        assertEquals(42, event.getPrescriptionId());
        assertEquals(19, event.getDispenserId());
    }

    @ParameterizedTest
    @EnumSource(value = TrangThaiDonThuoc.class, mode = EnumSource.Mode.EXCLUDE, names = "DA_DUYET")
    void dispensingRejectsUnapprovedPrescriptionWithoutChangingStateOrEvents(TrangThaiDonThuoc initialState) {
        DonThuoc prescription = prescriptionInState(initialState);
        LocalDateTime priorDispenseTime = LocalDateTime.of(2026, 10, 20, 9, 15);
        prescription.setNgayCapThuoc(priorDispenseTime);
        prescription.setMaNguoiCapThuoc(18);
        List<?> eventsBefore = List.copyOf(prescription.getDomainEvents());

        assertThrows(IllegalStateException.class, () -> prescription.completeDispense(19));

        assertEquals(initialState, prescription.getTrangThai());
        assertEquals(priorDispenseTime, prescription.getNgayCapThuoc());
        assertEquals(18, prescription.getMaNguoiCapThuoc());
        assertEquals(eventsBefore, prescription.getDomainEvents());
    }

    @Test
    void cancel_cancelsPrescriptionInTam_andRegistersCancelEvent() {
        DonThuoc prescription = prescriptionInState(TrangThaiDonThuoc.TAM);

        prescription.cancel("Doi phac do khang sinh khac");

        assertEquals(TrangThaiDonThuoc.HUY, prescription.getTrangThai());
        assertTrue(prescription.getGhiChu().contains("Doi phac do khang sinh khac"));
        assertEquals(1, prescription.getDomainEvents().size());
        vn.clinicmanagement.domain.common.events.DonThuocCancelledEvent event = assertInstanceOf(
                vn.clinicmanagement.domain.common.events.DonThuocCancelledEvent.class, prescription.getDomainEvents().get(0));
        assertEquals(42, event.getPrescriptionId());
        assertEquals("Doi phac do khang sinh khac", event.getReason());
    }

    @Test
    void cancel_rejectsDispensedPrescription() {
        DonThuoc prescription = prescriptionInState(TrangThaiDonThuoc.DA_XUAT_HET);
        assertThrows(IllegalStateException.class, () -> prescription.cancel("Ly do"));
    }

    @Test
    void cancel_rejectsAlreadyCancelledPrescription() {
        DonThuoc prescription = prescriptionInState(TrangThaiDonThuoc.HUY);
        assertThrows(IllegalStateException.class, () -> prescription.cancel("Ly do"));
    }

    @Test
    void cancel_rejectsBlankReason() {
        DonThuoc prescription = prescriptionInState(TrangThaiDonThuoc.TAM);
        assertThrows(IllegalArgumentException.class, () -> prescription.cancel(""));
        assertThrows(IllegalArgumentException.class, () -> prescription.cancel("   "));
        assertThrows(IllegalArgumentException.class, () -> prescription.cancel(null));
    }

    private static DonThuoc prescriptionInState(TrangThaiDonThuoc state) {
        DonThuoc prescription = new DonThuoc();
        prescription.setId(42);
        prescription.setMaLuotKham(7);
        prescription.setMaBacSi(11);
        prescription.setTrangThai(state);
        return prescription;
    }
}
