package vn.clinicmanagement.desktop.modules.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.auth.*;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.RoleChoice;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.WorkScopeChoice;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.session.PhienHetGioChinhSach;
import vn.clinicmanagement.desktop.core.session.*;

import javax.swing.SwingUtilities;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DangNhapPhienLuongTest {
    private final NguoiDungPhien session = NguoiDungPhien.getInstance();
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final RoleChoice role = new RoleChoice(42, "DOCTOR", "Synthetic role");
    private final WorkScopeChoice scope = new WorkScopeChoice(11, "Synthetic facility", 21, "Synthetic department");
    private final DaXacThucNguoiDung user = new DaXacThucNguoiDung(701, "synthetic.701", "Synthetic employee", List.of(role), List.of(scope));
    private final CompleteDangNhapPhienCommand command = new CompleteDangNhapPhienCommand(user, role, scope);
    private final DaHoanTatDangNhapPhien completed = new DaHoanTatDangNhapPhien(701, user.username(), user.fullName(), role, scope, UUID.randomUUID(), now);

    @AfterEach void clear() { session.clearSession(); }

    @Test void validateAndScrubIfInvalidRejectsBlankUsernameOrEmptyPasswordAndZerosSecret() {
        char[] blankUsernamePassword = "secret".toCharArray();
        assertFalse(DangNhapBieuMau.validateAndScrubIfInvalid(" \t ", blankUsernamePassword));
        assertArrayEquals(new char[blankUsernamePassword.length], blankUsernamePassword);

        char[] nullUsernamePassword = "secret".toCharArray();
        assertFalse(DangNhapBieuMau.validateAndScrubIfInvalid(null, nullUsernamePassword));
        assertArrayEquals(new char[nullUsernamePassword.length], nullUsernamePassword);

        assertFalse(DangNhapBieuMau.validateAndScrubIfInvalid("synthetic.user", new char[0]));
        assertFalse(DangNhapBieuMau.validateAndScrubIfInvalid("synthetic.user", null));

        char[] validPassword = "secret".toCharArray();
        assertTrue(DangNhapBieuMau.validateAndScrubIfInvalid(" synthetic.user ", validPassword));
        assertArrayEquals("secret".toCharArray(), validPassword);
    }

    @Test void toLoginFailureMessageUsesApplicationErrorMessageOrFallback() {
        assertEquals("Custom message",
                DangNhapBieuMau.toLoginFailureMessage(KetQua.failure("AUTH_FAILED", "Custom message"), "Fallback"));
        assertEquals("Fallback", DangNhapBieuMau.toLoginFailureMessage(null, "Fallback"));
        assertEquals("Fallback", DangNhapBieuMau.toLoginFailureMessage(KetQua.success("synthetic"), "Fallback"));
    }

    @Test void onlySingletonChoicesAreAutomaticAndMultipleChoicesRequireAnAssignedObject() {
        assertSame(role, DangNhapBieuMau.chooseAssigned(List.of(role), ignored -> { fail("No singleton prompt"); return null; }));
        RoleChoice second = new RoleChoice(43, "RECEPTION", "Synthetic second role");
        assertSame(second, DangNhapBieuMau.chooseAssigned(List.of(role, second), choices -> choices[1]));
        assertNull(DangNhapBieuMau.chooseAssigned(List.of(role, second), choices -> null));
        assertNull(DangNhapBieuMau.chooseAssigned(List.of(role, second), choices -> new RoleChoice(role.id(), role.code(), role.name())));
        WorkScopeChoice secondScope = new WorkScopeChoice(12, "Other facility", 22, "Other department");
        assertSame(secondScope, DangNhapBieuMau.chooseAssigned(List.of(scope, secondScope), choices -> choices[1]));
        assertNull(DangNhapBieuMau.chooseAssigned(List.of(scope, secondScope), choices -> null));
        assertNull(DangNhapBieuMau.chooseAssigned(List.of(scope, secondScope), choices -> new WorkScopeChoice(11, "Synthetic facility", 22, "Other department")));
        assertFalse(session.isLoggedOn());
    }

    @Test void failedCompletionNeverLoadsGrantsOrCreatesLifecycle() throws Exception {
        var attempt = DangNhapBieuMau.prepareSession(command, ignored -> KetQua.failure("AUDIT_UNAVAILABLE", "Synthetic denial"),
                ignored -> { fail("No grants before completion"); return null; },
                () -> { fail("No lifecycle after failure"); return null; });
        SwingUtilities.invokeAndWait(() -> assertFalse(attempt.activateIfCurrent(() -> true)));
        assertFalse(session.isLoggedOn());
    }

    @Test void grantFailureAfterCompletionCannotActivate() throws Exception {
        var attempt = DangNhapBieuMau.prepareSession(command, ignored -> KetQua.success(completed),
                ignored -> KetQua.failure("NAVIGATION_PERMISSIONS_UNAVAILABLE", "Synthetic denial"),
                () -> { fail("No lifecycle after grant failure"); return null; });
        SwingUtilities.invokeAndWait(() -> assertFalse(attempt.activateIfCurrent(() -> true)));
        assertFalse(session.isLoggedOn());
    }

    @Test void preparationIsOffEdtAndPublicationWaitsForCurrentEdtAttempt() throws Exception {
        List<String> order = new ArrayList<>();
        var attempt = DangNhapBieuMau.prepareSession(command, selected -> {
            assertSame(command, selected);
            assertFalse(SwingUtilities.isEventDispatchThread());
            order.add("complete");
            return KetQua.success(completed);
        }, context -> {
            assertSame(completed, context);
            order.add("grants");
            return KetQua.success(Set.of("HOME"));
        }, () -> { order.add("lifecycle"); return lifecycle(); });
        assertEquals(List.of("complete", "grants", "lifecycle"), order);
        assertFalse(session.isLoggedOn());
        SwingUtilities.invokeAndWait(() -> assertFalse(attempt.activateIfCurrent(() -> false)));
        assertFalse(session.isLoggedOn(), "A disposed/replaced login cannot publish a worker result");
        SwingUtilities.invokeAndWait(() -> assertTrue(attempt.activateIfCurrent(() -> true)));
        assertEquals(completed.correlationId().toString(), session.getSessionId());
        assertEquals(role.id(), session.getRoleId());
        assertEquals(scope.facilityId(), session.getFacilityId());
        assertEquals(scope.departmentId(), session.getDepartmentId());
    }

    @Test void grantOrPolicyExceptionLeavesNoPublishedSession() {
        assertThrows(IllegalStateException.class, () -> DangNhapBieuMau.prepareSession(command,
                ignored -> KetQua.success(completed), ignored -> { throw new IllegalStateException("Synthetic unavailable"); }, this::lifecycle));
        assertThrows(IllegalStateException.class, () -> DangNhapBieuMau.prepareSession(command,
                ignored -> KetQua.success(completed), ignored -> KetQua.success(Set.of("HOME")),
                () -> { throw new IllegalStateException("Synthetic unavailable"); }));
        assertFalse(session.isLoggedOn());
    }

    @Test void ioStageRejectsEdtBeforeInvokingAnyPort() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> assertThrows(IllegalStateException.class, () -> DangNhapBieuMau.prepareSession(command,
                ignored -> { invoked.set(true); return KetQua.success(completed); },
                ignored -> KetQua.success(Set.of("HOME")), this::lifecycle)));
        assertFalse(invoked.get());
    }

    private PhienVongDoiDichVu lifecycle() {
        return new PhienVongDoiDichVu(session, new PhienHetGioChinhSach(Duration.ofMinutes(15), Duration.ofHours(12)),
                Clock.fixed(now, ZoneOffset.UTC), new KiemToanWriteCongKetNoi() {
            @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) { fail("No write on preparation"); }
            @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) { fail("No write on preparation"); }
            @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) { fail("No medical audit"); }
            @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) { fail("No medical audit"); }
        });
    }
}
