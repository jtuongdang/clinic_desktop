package vn.clinicmanagement.desktop.modules.dashboard;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.auth.*;
import vn.clinicmanagement.application.session.PhienHetGioChinhSach;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.components.ThanhBangNut;
import vn.clinicmanagement.desktop.core.session.*;
import vn.clinicmanagement.desktop.modules.home.TrangChuChaoMungManHinh;

import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import static org.junit.jupiter.api.Assertions.*;

class BangDieuKhienDieuHuongChinhSachTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @AfterEach void clear() { NguoiDungPhien.getInstance().clearSession(); }

    @Test void routesAreAuthorizedAccordingToRoleGrantsAndDeniedWithoutGrants() {
        var adminPolicy = new BangDieuKhienDieuHuongChinhSach(Set.of("HOME", "SYSTEM", "CATALOG"));
        assertTrue(adminPolicy.canOpen("home"));
        for (String key : new String[]{"reception", "treatment", "doctor", "examination", "billing", "paraclinical",
                "pharmacy", "reports", "admin", "system", "catalog", "user", "help", "exit"}) {
            assertTrue(adminPolicy.canOpen(key), key);
        }
        assertFalse(adminPolicy.canOpen(null));
        assertFalse(adminPolicy.canOpen("unknown_route"));

        var emptyPolicy = new BangDieuKhienDieuHuongChinhSach(Set.of());
        assertFalse(emptyPolicy.canOpen("home"));
        assertFalse(emptyPolicy.canOpen("reception"));
        assertFalse(emptyPolicy.canOpen("treatment"));
        assertFalse(emptyPolicy.canOpen("examination"));
        assertFalse(emptyPolicy.canOpen("billing"));
        assertFalse(emptyPolicy.canOpen("pharmacy"));
        assertTrue(emptyPolicy.canOpen("user"));
        assertTrue(emptyPolicy.canOpen("help"));
        assertTrue(emptyPolicy.canOpen("exit"));

        var receptionPolicy = new BangDieuKhienDieuHuongChinhSach(Set.of("HOME", "RECEPTION"));
        assertTrue(receptionPolicy.canOpen("home"));
        assertTrue(receptionPolicy.canOpen("reception"));
        assertFalse(receptionPolicy.canOpen("treatment"));
        assertFalse(receptionPolicy.canOpen("billing"));
        assertFalse(receptionPolicy.canOpen("pharmacy"));
        assertFalse(receptionPolicy.canOpen("system"));
    }

    @Test void directOpenRetainedTabAndImpersonatedHomePanelAreGuarded() throws Exception {
        // Preload the welcome image outside Swing, as the real login worker does.
        Class.forName(TrangChuChaoMungManHinh.class.getName());
        var lifecycle = lifecycle();
        SwingUtilities.invokeAndWait(() -> {
            CardLayout cards = new CardLayout();
            JPanel center = new JPanel(cards);
            JPanel tabs = new JPanel();
            var workspace = new MainBangDieuKhienCuaSo.GuardedWorkspace(center, cards, tabs, lifecycle,
                    new BangDieuKhienDieuHuongChinhSach(Set.of("HOME", "EMR", "RECEPTION")));
            workspace.openView(new CoSoManHinhManHinh("HOME", "Impersonated", "") {
                @Override public void refreshData() { fail("No foreign panel should activate"); }
            });
            assertEquals(0, center.getComponentCount());
            TrangChuChaoMungManHinh home = new TrangChuChaoMungManHinh();
            workspace.openView(home);
            assertEquals(1, center.getComponentCount());
            assertEquals(1, tabs.getComponentCount());
            lifecycle.clear();
            workspace.openView(new TrangChuChaoMungManHinh());
            workspace.activateView("HOME");
            assertEquals(1, center.getComponentCount());
            workspace.disposeViews();
            assertEquals(0, center.getComponentCount());
            assertEquals(0, tabs.getComponentCount());
            workspace.openView(home);
            assertEquals(0, center.getComponentCount());
        });
    }

    @Test void dashboardHasNoFabricatedIdentityOrDisplayLabelConstructor() {
        var constructors = MainBangDieuKhienCuaSo.class.getConstructors();
        assertEquals(1, constructors.length);
        assertArrayEquals(new Class<?>[]{DaHoanTatDangNhapPhien.class, Set.class, PhienVongDoiDichVu.class},
                constructors[0].getParameterTypes());
    }

    @Test void userDialogFormattingReadsVerifiedCompletedLoginSessionEvenIfSingletonCleared() {
        var loginSession = new DaHoanTatDangNhapPhien(701, "synthetic.701", "Synthetic employee",
                new DaXacThucNguoiDung.RoleChoice(42, "DOCTOR", "Bác sĩ"),
                new DaXacThucNguoiDung.WorkScopeChoice(11, "Cơ sở tổng hợp", 21, "Khoa tổng hợp"),
                UUID.randomUUID(), Instant.parse("2026-01-01T00:00:00Z"));
        NguoiDungPhien.getInstance().clearSession();

        assertEquals("Bác sĩ đang khám: Synthetic employee\n" +
                        "Cơ sở: Cơ sở tổng hợp\n" +
                        "Khoa phòng: Khoa tổng hợp\n" +
                        "Vai trò: Bác sĩ",
                MainBangDieuKhienCuaSo.formatDoctorUserInfo(loginSession));
        assertEquals("Thông tin người dùng: Synthetic employee\n" +
                        "Cơ sở: Cơ sở tổng hợp\n" +
                        "Khoa phòng: Khoa tổng hợp\n" +
                        "Vai trò: Bác sĩ",
                MainBangDieuKhienCuaSo.formatStaffUserInfo(loginSession));
    }

    @Test void ribbonButtonsDisableOnIdleLockAndReenableOnlyAllowedRoutesAfterReauthentication() throws Exception {
        var clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        var role = new DaXacThucNguoiDung.RoleChoice(42, "DOCTOR", "Bác sĩ");
        var scope = new DaXacThucNguoiDung.WorkScopeChoice(11, "Cơ sở tổng hợp", 21, "Khoa tổng hợp");
        var lifecycle = new PhienVongDoiDichVu(NguoiDungPhien.getInstance(),
                new PhienHetGioChinhSach(Duration.ofMinutes(15), Duration.ofHours(12)), clock,
                new KiemToanWriteCongKetNoi() {
                    @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) { fail("Unexpected transactional audit"); }
                    @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
                    @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) { fail("Unexpected medical audit"); }
                    @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) { fail("Unexpected medical audit"); }
                }, Runnable::run);
        lifecycle.startSession(new DaHoanTatDangNhapPhien(701, "synthetic.701", "Synthetic employee",
                role, scope, UUID.randomUUID(), clock.instant()));
        var navigation = new BangDieuKhienDieuHuongChinhSach(Set.of("HOME"));
        Map<String, ThanhBangNut> ribbonButtons = Map.of(
                "home", new ThanhBangNut("Trang Chủ", "", "home", null),
                "reception", new ThanhBangNut("Đón Tiếp", "", "reception", null),
                "examination", new ThanhBangNut("Khám Bệnh", "", "examination", null),
                "user", new ThanhBangNut("Người Dùng", "", "user", null),
                "exit", new ThanhBangNut("Kết Thúc", "", "exit", null));

        MainBangDieuKhienCuaSo.syncRibbonButtonsForSessionState(lifecycle, navigation, ribbonButtons);
        assertRibbonButtons(ribbonButtons, Set.of("home", "user", "exit"));

        clock.advance(Duration.ofMinutes(15));
        assertEquals(PhienVongDoiDichVu.State.LOCKED_IDLE, lifecycle.evaluate());
        MainBangDieuKhienCuaSo.syncRibbonButtonsForSessionState(lifecycle, navigation, ribbonButtons);
        assertRibbonButtons(ribbonButtons, Set.of("exit"));

        var authenticated = new DaXacThucNguoiDung(701, "synthetic.701", "Synthetic employee",
                java.util.List.of(role), java.util.List.of(scope));
        FutureTask<Boolean> reauthentication = new FutureTask<>(() -> {
            assertFalse(SwingUtilities.isEventDispatchThread());
            return lifecycle.reauthenticate(authenticated);
        });
        Thread worker = new Thread(reauthentication);
        worker.start();
        assertTrue(reauthentication.get());
        MainBangDieuKhienCuaSo.syncRibbonButtonsForSessionState(lifecycle, navigation, ribbonButtons);
        assertRibbonButtons(ribbonButtons, Set.of("home", "user", "exit"));
    }

    @Test void lockOverlayVisibilityAndInputBlockingTrackSessionLifecycleTransitions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var lockOverlay = new PhienKhoaLopPhu(credentials -> {}, () -> {});

            MainBangDieuKhienCuaSo.syncLockOverlayForSessionState(PhienVongDoiDichVu.State.ACTIVE, lockOverlay);
            assertFalse(lockOverlay.isVisible());
            assertFalse(lockOverlay.isInputBlocked());

            MainBangDieuKhienCuaSo.syncLockOverlayForSessionState(PhienVongDoiDichVu.State.LOCKED_IDLE, lockOverlay);
            assertTrue(lockOverlay.isVisible());
            assertTrue(lockOverlay.isInputBlocked());

            MainBangDieuKhienCuaSo.syncLockOverlayForSessionState(PhienVongDoiDichVu.State.ACTIVE, lockOverlay);
            assertFalse(lockOverlay.isVisible());
            assertFalse(lockOverlay.isInputBlocked());

            MainBangDieuKhienCuaSo.syncLockOverlayForSessionState(PhienVongDoiDichVu.State.LOCKED_ABSOLUTE, lockOverlay);
            assertTrue(lockOverlay.isVisible());
            assertTrue(lockOverlay.isInputBlocked());
        });
    }

    private void assertRibbonButtons(Map<String, ThanhBangNut> ribbonButtons, Set<String> enabledKeys) {
        for (var entry : ribbonButtons.entrySet()) {
            assertEquals(enabledKeys.contains(entry.getKey()), entry.getValue().isEnabled(), entry.getKey());
        }
    }

    private PhienVongDoiDichVu lifecycle() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        var lifecycle = new PhienVongDoiDichVu(NguoiDungPhien.getInstance(),
                new PhienHetGioChinhSach(Duration.ofMinutes(15), Duration.ofHours(12)), Clock.fixed(now, ZoneOffset.UTC),
                new KiemToanWriteCongKetNoi() {
                    @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) { fail("No transactional session audit"); }
                    @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
                    @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) { fail("No medical audit"); }
                    @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) { fail("No medical audit"); }
                });
        lifecycle.startSession(new DaHoanTatDangNhapPhien(701, "synthetic.701", "Synthetic employee",
                new DaXacThucNguoiDung.RoleChoice(42, "DOCTOR", "Synthetic doctor"),
                new DaXacThucNguoiDung.WorkScopeChoice(11, "Synthetic facility", 21, "Synthetic department"), UUID.randomUUID(), now));
        return lifecycle;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
