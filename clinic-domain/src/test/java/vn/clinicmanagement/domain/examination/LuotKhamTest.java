package vn.clinicmanagement.domain.examination;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.common.events.EmrLockedEvent;
import vn.clinicmanagement.domain.common.events.LuotKhamCompletedEvent;
import vn.clinicmanagement.domain.common.events.LuotKhamStartedEvent;
import vn.clinicmanagement.domain.common.exceptions.EmrLockedException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuotKhamTest {

    @Test
    void startExamTransitionsWaitingVisitAndRegistersStartEvent() {
        LuotKham visit = visitInState(TrangThaiLuotKham.CHO_KHAM);

        visit.startExam();

        assertEquals(TrangThaiLuotKham.DANG_KHAM, visit.getTrangThai());
        assertNotNull(visit.getThoiGianBatDauKham());
        assertEquals(1, visit.getDomainEvents().size());
        LuotKhamStartedEvent event = assertInstanceOf(
                LuotKhamStartedEvent.class, visit.getDomainEvents().get(0));
        assertEquals(42, event.getVisitId());
        assertEquals(7, event.getPatientId());
        assertEquals(11, event.getDoctorId());
    }

    @ParameterizedTest
    @EnumSource(value = TrangThaiLuotKham.class, mode = EnumSource.Mode.EXCLUDE, names = "CHO_KHAM")
    void startExamRejectsNonWaitingStateWithoutChangingVisitOrEvents(TrangThaiLuotKham initialState) {
        LuotKham visit = visitInState(initialState);
        LocalDateTime startTime = LocalDateTime.of(2026, 10, 20, 9, 15);
        visit.setThoiGianBatDauKham(startTime);
        visit.registerDomainEvent(new LuotKhamStartedEvent(42, 7, 11));
        List<?> eventsBefore = visit.getDomainEvents();

        assertThrows(IllegalStateException.class, visit::startExam);

        assertEquals(initialState, visit.getTrangThai());
        assertEquals(startTime, visit.getThoiGianBatDauKham());
        assertEquals(eventsBefore, visit.getDomainEvents());
    }

    @Test
    void completeExamTransitionsActiveVisitAndRegistersCompletionEvent() {
        LuotKham visit = visitInState(TrangThaiLuotKham.DANG_KHAM);

        visit.completeExam();

        assertEquals(TrangThaiLuotKham.HOAN_THANH, visit.getTrangThai());
        assertNotNull(visit.getThoiGianKetThucKham());
        assertEquals(1, visit.getDomainEvents().size());
        LuotKhamCompletedEvent event = assertInstanceOf(
                LuotKhamCompletedEvent.class, visit.getDomainEvents().get(0));
        assertEquals(42, event.getVisitId());
        assertEquals(7, event.getPatientId());
        assertEquals(11, event.getDoctorId());
    }

    @ParameterizedTest
    @EnumSource(value = TrangThaiLuotKham.class, mode = EnumSource.Mode.EXCLUDE, names = "DANG_KHAM")
    void completeExamRejectsNonActiveStateWithoutChangingVisit(TrangThaiLuotKham initialState) {
        LuotKham visit = visitInState(initialState);
        LocalDateTime completionTime = LocalDateTime.of(2026, 10, 20, 9, 15);
        visit.setThoiGianKetThucKham(completionTime);
        visit.registerDomainEvent(new LuotKhamStartedEvent(42, 7, 11));
        List<?> eventsBefore = visit.getDomainEvents();

        assertThrows(IllegalStateException.class, visit::completeExam);

        assertEquals(initialState, visit.getTrangThai());
        assertEquals(completionTime, visit.getThoiGianKetThucKham());
        assertEquals(eventsBefore, visit.getDomainEvents());
    }

    @Test
    void completeExamRejectsLockedEmrWithoutChangingVisitOrEvents() {
        LuotKham visit = visitInState(TrangThaiLuotKham.DANG_KHAM);
        LocalDateTime completionTime = LocalDateTime.of(2026, 10, 20, 9, 15);
        visit.setThoiGianKetThucKham(completionTime);
        visit.lockEmr("synthetic-signature", 11);
        List<?> eventsBefore = visit.getDomainEvents();

        assertThrows(EmrLockedException.class, visit::completeExam);

        assertEquals(TrangThaiLuotKham.DANG_KHAM, visit.getTrangThai());
        assertEquals(completionTime, visit.getThoiGianKetThucKham());
        assertEquals(eventsBefore, visit.getDomainEvents());
    }

    @Test
    void lockEmrAcceptsValidSignatureAndDoctorAndRegistersOneEvent() {
        LuotKham visit = visitInState(TrangThaiLuotKham.DANG_KHAM);
        String signature = "  synthetic-signature  ";

        visit.lockEmr(signature, 11);

        assertTrue(visit.isEmrLocked());
        assertEquals(signature, visit.getEmrSignature());
        assertNotNull(visit.getEmrLockedAt());
        assertEquals(11, visit.getEmrLockedBy());
        assertEquals(1, visit.getDomainEvents().size());
        EmrLockedEvent event = assertInstanceOf(EmrLockedEvent.class, visit.getDomainEvents().get(0));
        assertEquals(42, event.getVisitId());
        assertEquals(11, event.getDoctorId());
        assertEquals(signature, event.getSignature());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n"})
    void lockEmrRejectsNullOrBlankSignatureWithoutChangingLockFieldsOrEvents(String signature) {
        assertInvalidLockArgumentsLeaveVisitUnchanged(signature, 11);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void lockEmrRejectsNonpositiveDoctorIdWithoutChangingLockFieldsOrEvents(int doctorId) {
        assertInvalidLockArgumentsLeaveVisitUnchanged("synthetic-signature", doctorId);
    }

    @Test
    void lockEmrRejectsRepeatedLockWithoutChangingFieldsOrEvents() {
        LuotKham visit = visitInState(TrangThaiLuotKham.DANG_KHAM);
        visit.lockEmr("synthetic-signature", 11);
        boolean lockedBefore = visit.isEmrLocked();
        String signatureBefore = visit.getEmrSignature();
        LocalDateTime lockedAtBefore = visit.getEmrLockedAt();
        Integer lockedByBefore = visit.getEmrLockedBy();
        List<?> eventsBefore = List.copyOf(visit.getDomainEvents());

        assertThrows(EmrLockedException.class, () -> visit.lockEmr("replacement-signature", 12));

        assertEquals(lockedBefore, visit.isEmrLocked());
        assertEquals(signatureBefore, visit.getEmrSignature());
        assertEquals(lockedAtBefore, visit.getEmrLockedAt());
        assertEquals(lockedByBefore, visit.getEmrLockedBy());
        assertEquals(eventsBefore, visit.getDomainEvents());
    }

    private static void assertInvalidLockArgumentsLeaveVisitUnchanged(String signature, int doctorId) {
        LuotKham visit = visitInState(TrangThaiLuotKham.DANG_KHAM);
        visit.registerDomainEvent(new LuotKhamStartedEvent(42, 7, 11));
        List<?> eventsBefore = List.copyOf(visit.getDomainEvents());

        assertThrows(IllegalArgumentException.class, () -> visit.lockEmr(signature, doctorId));

        assertFalse(visit.isEmrLocked());
        assertNull(visit.getEmrSignature());
        assertNull(visit.getEmrLockedAt());
        assertNull(visit.getEmrLockedBy());
        assertEquals(eventsBefore, visit.getDomainEvents());
    }

    @Test
    void reopenEmr_acceptsValidApproverAndReason_andResetsLock() {
        LuotKham visit = visitInState(TrangThaiLuotKham.DANG_KHAM);
        visit.completeExam();
        visit.lockEmr("synthetic-sig", 11);
        assertTrue(visit.isEmrLocked());
        assertEquals(TrangThaiLuotKham.HOAN_THANH, visit.getTrangThai());

        visit.reopenEmr(99, "Bo sung ket qua can lam sang theo hoi chan");

        assertFalse(visit.isEmrLocked());
        assertNull(visit.getEmrSignature());
        assertNull(visit.getEmrLockedAt());
        assertNull(visit.getEmrLockedBy());
        assertEquals(TrangThaiLuotKham.DANG_KHAM, visit.getTrangThai());

        boolean hasReopenedEvent = visit.getDomainEvents().stream()
                .anyMatch(e -> e instanceof vn.clinicmanagement.domain.common.events.EmrReopenedEvent);
        assertTrue(hasReopenedEvent);
    }

    @Test
    void reopenEmr_rejectsWhenNotLocked() {
        LuotKham visit = visitInState(TrangThaiLuotKham.HOAN_THANH);
        assertThrows(IllegalStateException.class, () -> visit.reopenEmr(99, "Reason"));
    }

    @Test
    void reopenEmr_rejectsBlankReasonOrNonpositiveApprover() {
        LuotKham visit = visitInState(TrangThaiLuotKham.DANG_KHAM);
        visit.completeExam();
        visit.lockEmr("synthetic-sig", 11);

        assertThrows(IllegalArgumentException.class, () -> visit.reopenEmr(0, "Reason"));
        assertThrows(IllegalArgumentException.class, () -> visit.reopenEmr(99, ""));
        assertThrows(IllegalArgumentException.class, () -> visit.reopenEmr(99, "   "));
        assertThrows(IllegalArgumentException.class, () -> visit.reopenEmr(99, null));
    }

    private static LuotKham visitInState(TrangThaiLuotKham state) {
        LuotKham visit = new LuotKham();
        visit.setId(42);
        visit.setMaBenhNhan(7);
        visit.setMaBacSi(11);
        visit.setTrangThai(state);
        return visit;
    }
}
