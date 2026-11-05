package vn.clinicmanagement.domain.examination;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.common.events.LuotKhamCompletedEvent;
import vn.clinicmanagement.domain.common.events.LuotKhamStartedEvent;
import vn.clinicmanagement.domain.common.exceptions.EmrLockedException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LuotKhamTest {

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

    private static LuotKham visitInState(TrangThaiLuotKham state) {
        LuotKham visit = new LuotKham();
        visit.setId(42);
        visit.setMaBenhNhan(7);
        visit.setMaBacSi(11);
        visit.setTrangThai(state);
        return visit;
    }
}
