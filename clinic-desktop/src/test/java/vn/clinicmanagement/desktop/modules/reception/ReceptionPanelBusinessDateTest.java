package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.reception.GetReceptionQueueQuery;
import vn.clinicmanagement.application.reception.ReceptionQueueSnapshot;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Synthetic handlers only: no JDBC composition, patient data, or display server. */
class ReceptionPanelBusinessDateTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void queueQueryUsesVietnamBusinessDateAtMidnightWhenJvmDefaultIsUtc() throws Exception {
        TimeZone originalZone = TimeZone.getDefault();
        AtomicReference<ReceptionPanel> panel = new AtomicReference<>();
        UserSession.getInstance().clearSession();
        Instant instant = Instant.parse("2026-11-05T17:00:00Z");
        Clock businessClock = Clock.fixed(instant, ZoneId.of("Asia/Ho_Chi_Minh"));
        SessionLifecycleService lifecycle = new SessionLifecycleService(UserSession.getInstance(),
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)),
                Clock.fixed(instant, ZoneOffset.UTC));
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertTrue(GraphicsEnvironment.isHeadless());
            assertEquals(ZoneId.of("UTC"), ZoneId.systemDefault());
            assertEquals(LocalDate.of(2026, 11, 5), LocalDate.ofInstant(instant, ZoneId.systemDefault()));
            lifecycle.startSession(701, "synthetic-reception", "Synthetic Reception",
                    new RoleChoice(42, "RECEPTIONIST", "Synthetic Reception"),
                    new WorkScopeChoice(11, "Synthetic Facility", 21, "Synthetic Department"),
                    UUID.randomUUID().toString());
            AtomicReference<GetReceptionQueueQuery> capturedQuery = new AtomicReference<>();
            CountDownLatch queryReceived = new CountDownLatch(1);
            var services = new ReceptionCompositionRoot.Services(null,
                    command -> { throw new AssertionError("Registration must not run during a queue read"); },
                    query -> { throw new AssertionError("Patient search must not run during a queue read"); },
                    query -> {
                        capturedQuery.set(query);
                        queryReceived.countDown();
                        return Result.success(new ReceptionQueueSnapshot(List.of(), List.of()));
                    },
                    query -> Result.success(List.of()));

            SwingUtilities.invokeAndWait(() -> {
                panel.set(new ReceptionPanel(lifecycle, services, businessClock));
                panel.get().onShow();
            });

            assertTrue(queryReceived.await(3, TimeUnit.SECONDS), "Expected the fake queue handler to receive a query");
            GetReceptionQueueQuery query = capturedQuery.get();
            assertEquals(LocalDate.of(2026, 11, 6), query.date());
            assertEquals(lifecycle.currentAuthorizationContext().orElseThrow(), query.authorizationContext());
            assertEquals(701, query.auditActorContext().employeeId());
            assertEquals(11, query.auditActorContext().facilityId());
            assertEquals(21, query.auditActorContext().departmentId());
        } finally {
            try {
                if (panel.get() != null) SwingUtilities.invokeAndWait(panel.get()::onHide);
                lifecycle.clear();
            } finally {
                TimeZone.setDefault(originalZone);
            }
        }
    }
}
