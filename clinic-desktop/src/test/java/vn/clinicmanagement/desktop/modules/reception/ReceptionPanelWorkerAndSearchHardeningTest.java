package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.auth.CompletedLoginSession;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.application.reception.ClinicRoomQueueSummary;
import vn.clinicmanagement.application.reception.GetActiveReceptionRoomsHandler;
import vn.clinicmanagement.application.reception.GetReceptionQueueHandler;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.application.reception.ReceptionQueuePort;
import vn.clinicmanagement.application.reception.RegisterPatientCommand;
import vn.clinicmanagement.application.reception.RegisterPatientHandler;
import vn.clinicmanagement.application.reception.SearchPatientsHandler;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.repositories.IBaoHiemRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanCoSoRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;
import vn.clinicmanagement.domain.repositories.ISystemRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless unit tests verifying ReceptionPanel session lifecycle gating,
 * distinct correlation UUIDs for separately audited operations, search error
 * handling without partial results, and viewSessionEpoch stale-worker suppression.
 */
class ReceptionPanelWorkerAndSearchHardeningTest {

    private static final int TEST_FACILITY_ID = 1;
    private static final int TEST_DEPARTMENT_ID = 1;
    private static final int TEST_USER_ID = 101;
    private static final int TEST_ROOM_ID = 71;
    private static final RoleChoice ROLE = new RoleChoice(10, "RECEPTIONIST", "Tiếp đón mẫu");
    private static final WorkScopeChoice SCOPE = new WorkScopeChoice(TEST_FACILITY_ID, "Cơ sở mẫu", TEST_DEPARTMENT_ID, "Khoa mẫu");

    private final AtomicReference<Instant> testClockInstant = new AtomicReference<>(Instant.parse("2026-11-07T08:00:00Z"));
    private final Clock testClock = new Clock() {
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return testClockInstant.get(); }
    };

    private final UserSession session = UserSession.getInstance();
    private final UUID sessionId = UUID.randomUUID();

    private final AtomicInteger visitCallCount = new AtomicInteger(0);
    private final AtomicInteger patientSearchCallCount = new AtomicInteger(0);
    private final AtomicInteger patientAddCallCount = new AtomicInteger(0);
    private final RecordingAuditWritePort auditPort = new RecordingAuditWritePort();

    private volatile Supplier<List<PhongKham>> roomsSupplier;
    private volatile Function<String, List<BenhNhan>> searchBehavior = s -> Collections.emptyList();

    private ReceptionQueuePort queuePort;
    private IPhongKhamRepository roomsRepo;
    private IBenhNhanRepository patientRepo;
    private IBenhNhanCoSoRepository patientFacilityRepo;
    private IBaoHiemRepository insuranceRepo;
    private TransactionBoundary txBoundary;
    private AuthorizationService authzService;
    private SessionLifecycleService lifecycle;

    private GetReceptionQueueHandler queueHandler;
    private GetActiveReceptionRoomsHandler roomsHandler;
    private RegisterPatientHandler registerPatientHandler;
    private SearchPatientsHandler searchPatientsHandler;

    private ReceptionPanel panel;
    private PhongKham defaultRoom;

    @BeforeAll
    static void configureHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void setUp() throws Exception {
        session.clearSession();
        testClockInstant.set(Instant.parse("2026-11-07T08:00:00Z"));
        visitCallCount.set(0);
        patientSearchCallCount.set(0);
        patientAddCallCount.set(0);
        auditPort.clear();
        searchBehavior = s -> Collections.emptyList();

        defaultRoom = createRoom(TEST_ROOM_ID, "Phòng 101 - Khám Tổng Quát");
        roomsSupplier = () -> List.of(defaultRoom);

        queuePort = new ReceptionQueuePort() {
            @Override
            public LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                                          String registrationType, String reason, DoiTuongKham payerType,
                                          boolean priority, int createdBy) {
                visitCallCount.incrementAndGet();
                LuotKham visit = new LuotKham();
                visit.setId(901);
                visit.setSoThuTuDisplay("STT-01");
                return visit;
            }

            @Override
            public List<ReceptionQueueItem> getQueue(LocalDate date, int facilityId, int departmentId) {
                return Collections.emptyList();
            }

            @Override
            public List<ClinicRoomQueueSummary> getRoomSummaries(LocalDate date, int facilityId, int departmentId) {
                return Collections.emptyList();
            }
        };

        roomsRepo = new IPhongKhamRepository() {
            @Override
            public Optional<PhongKham> getById(int id) {
                return roomsSupplier.get().stream().filter(r -> r.getId() != null && r.getId() == id).findFirst();
            }

            @Override
            public List<PhongKham> getActiveRooms() {
                return new ArrayList<>(roomsSupplier.get());
            }

            @Override
            public List<PhongKham> getActiveRoomsInScope(int facilityId, int departmentId) {
                return new ArrayList<>(roomsSupplier.get());
            }
        };

        patientRepo = mockInterface(IBenhNhanRepository.class, (proxy, method, args) -> switch (method.getName()) {
            case "searchInScope" -> {
                patientSearchCallCount.incrementAndGet();
                yield searchBehavior.apply((String) args[0]);
            }
            case "getByIdInScope" -> Optional.of(createSyntheticPatient((Integer) args[0], "Nguyen Van Test", "0901234567", "001090000001"));
            case "getByCCCD", "getByCCCDInScope", "getByPhone" -> Optional.empty();
            case "search" -> Collections.emptyList();
            case "add" -> {
                patientAddCallCount.incrementAndGet();
                BenhNhan p = (BenhNhan) args[0];
                if (p.getId() == null) p.setId(42);
                yield null;
            }
            case "update" -> null;
            default -> defaultValue(method);
        });

        patientFacilityRepo = mockInterface(IBenhNhanCoSoRepository.class, (proxy, method, args) ->
                method.getName().equals("ensureActiveLink") ? true : defaultValue(method));

        insuranceRepo = mockInterface(IBaoHiemRepository.class, (proxy, method, args) -> switch (method.getName()) {
            case "getLatestByPatientId" -> Optional.empty();
            case "add", "update" -> null;
            default -> defaultValue(method);
        });

        txBoundary = new TransactionBoundary() {
            @Override
            public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
                return operation.get();
            }
        };

        lifecycle = new SessionLifecycleService(
                session,
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)),
                testClock,
                auditPort,
                Runnable::run);

        lifecycle.startSession(new CompletedLoginSession(
                TEST_USER_ID, "reception.test", "Nhân viên mẫu",
                ROLE, SCOPE, sessionId, testClock.instant()));

        authzService = createSyntheticAuthorization(lifecycle);

        // Concrete handler instantiations without proxies
        queueHandler = new GetReceptionQueueHandler(queuePort, authzService, auditPort);
        roomsHandler = new GetActiveReceptionRoomsHandler(roomsRepo, authzService, auditPort);
        registerPatientHandler = new RegisterPatientHandler(
                patientRepo, patientFacilityRepo, insuranceRepo, roomsRepo, queuePort,
                txBoundary, auditPort, authzService);
        searchPatientsHandler = new SearchPatientsHandler(patientRepo, insuranceRepo, authzService, auditPort);

        SwingUtilities.invokeAndWait(() -> {
            panel = new ReceptionPanel(queueHandler, roomsHandler, registerPatientHandler, searchPatientsHandler, lifecycle);
            panel.updateActiveRooms(List.of(defaultRoom));
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (panel != null) {
                SwingUtilities.invokeAndWait(panel::destroy);
            }
        } finally {
            session.clearSession();
        }
    }

    // =========================================================================
    // 1. Inactive Session Blocking: Locked or Logged-out SessionLifecycleService
    // =========================================================================

    @Test
    void lockedSessionBlocksSaveRegistrationWithSessionExpiredMessageAndDoesNotInvokeHandler() throws Exception {
        selectValidPatientAndRoom();
        auditPort.clear();

        // Advance clock past 15-minute idle timeout to lock session
        testClockInstant.set(Instant.parse("2026-11-07T08:20:00Z"));
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());

        SwingUtilities.invokeAndWait(() -> {
            panel.clearLastMessagesForTest();
            panel.saveRegistration();
        });
        waitForWorkers(panel);

        assertEquals("Phiên làm việc không hợp lệ hoặc đã hết hạn.", panel.getLastErrorMessage(),
                "Locked session must block saveRegistration with expired session message");
        assertEquals(0, visitCallCount.get(),
                "RegisterPatientHandler must not be invoked when session is locked");
        assertEquals(0, patientAddCallCount.get(),
                "Patient repository must not be invoked when session is locked");
    }

    @Test
    void lockedSessionBlocksSearchPatientsWithSessionExpiredMessageAndDoesNotInvokeHandler() throws Exception {
        auditPort.clear();

        // Advance clock past 15-minute idle timeout to lock session
        testClockInstant.set(Instant.parse("2026-11-07T08:20:00Z"));
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());

        SwingUtilities.invokeAndWait(() -> {
            panel.clearLastMessagesForTest();
            panel.searchPatients("0901234567", "001090000001");
        });
        waitForWorkers(panel);

        assertEquals("Phiên làm việc không hợp lệ hoặc đã hết hạn.", panel.getLastErrorMessage(),
                "Locked session must block searchPatients with expired session message");
        assertEquals(0, patientSearchCallCount.get(),
                "SearchPatientsHandler must not be invoked when session is locked");
    }

    @Test
    void loggedOutSessionBlocksSaveRegistrationWithSessionExpiredMessageAndDoesNotInvokeHandler() throws Exception {
        selectValidPatientAndRoom();
        auditPort.clear();

        lifecycle.logout();
        assertEquals(SessionLifecycleService.State.LOGGED_OUT, lifecycle.evaluate());

        SwingUtilities.invokeAndWait(() -> {
            panel.clearLastMessagesForTest();
            panel.saveRegistration();
        });
        waitForWorkers(panel);

        assertEquals("Phiên làm việc không hợp lệ hoặc đã hết hạn.", panel.getLastErrorMessage(),
                "Logged out session must block saveRegistration with expired session message");
        assertEquals(0, visitCallCount.get(),
                "RegisterPatientHandler must not be invoked when session is logged out");
        assertEquals(0, patientAddCallCount.get(),
                "Patient repository must not be invoked when session is logged out");
    }

    @Test
    void loggedOutSessionBlocksSearchPatientsWithSessionExpiredMessageAndDoesNotInvokeHandler() throws Exception {
        auditPort.clear();

        lifecycle.logout();
        assertEquals(SessionLifecycleService.State.LOGGED_OUT, lifecycle.evaluate());

        SwingUtilities.invokeAndWait(() -> {
            panel.clearLastMessagesForTest();
            panel.searchPatients("0901234567", "001090000001");
        });
        waitForWorkers(panel);

        assertEquals("Phiên làm việc không hợp lệ hoặc đã hết hạn.", panel.getLastErrorMessage(),
                "Logged out session must block searchPatients with expired session message");
        assertEquals(0, patientSearchCallCount.get(),
                "SearchPatientsHandler must not be invoked when session is logged out");
    }

    // =========================================================================
    // 2. Distinct Correlation UUIDs: refreshData() and searchPatients()
    // =========================================================================

    @Test
    void refreshDataPassesDistinctCorrelationUuidsToQueueAndRoomsAudits() throws Exception {
        auditPort.clear();

        SwingUtilities.invokeAndWait(panel::refreshData);
        waitForWorkers(panel);

        SystemAuditEntry queueEntry = auditPort.getEntries().stream()
                .filter(e -> e.actionCode() == AuditActionCode.PATIENT_VIEW)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected PATIENT_VIEW audit entry from queueHandler"));

        SystemAuditEntry roomsEntry = auditPort.getEntries().stream()
                .filter(e -> e.actionCode() == AuditActionCode.CATALOG_VIEW)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected CATALOG_VIEW audit entry from roomsHandler"));

        assertNotNull(queueEntry.correlationId(), "Queue audit entry must have correlationId");
        assertNotNull(roomsEntry.correlationId(), "Rooms audit entry must have correlationId");
        assertNotEquals(queueEntry.correlationId(), roomsEntry.correlationId(),
                "refreshData() must pass distinct correlation UUIDs to queue and rooms handlers");
    }

    @Test
    void searchPatientsWithPhoneAndCccdPassesDistinctCorrelationUuidsToEachQuery() throws Exception {
        auditPort.clear();

        SwingUtilities.invokeAndWait(() -> panel.searchPatients("0901234567", "001090000001"));
        waitForWorkers(panel);

        List<SystemAuditEntry> searchEntries = auditPort.getEntries().stream()
                .filter(e -> e.actionCode() == AuditActionCode.PATIENT_SEARCH)
                .toList();

        assertEquals(2, searchEntries.size(),
                "searchPatients with phone and CCCD must execute 2 audited searches");

        UUID phoneCorrelation = searchEntries.get(0).correlationId();
        UUID cccdCorrelation = searchEntries.get(1).correlationId();

        assertNotNull(phoneCorrelation, "Phone search must have correlationId");
        assertNotNull(cccdCorrelation, "CCCD search must have correlationId");
        assertNotEquals(phoneCorrelation, cccdCorrelation,
                "searchPatients() must pass distinct correlation UUIDs to phone and CCCD queries");
    }

    // =========================================================================
    // 3. Search Failure Surfacing: ReceptionPanelMessagePolicy error
    // =========================================================================

    @Test
    void searchPatientsSurfacesMessagePolicyErrorOnSingleQueryFailure() throws Exception {
        searchBehavior = keyword -> {
            throw new RuntimeException("Simulated synthetic database connection error");
        };

        SwingUtilities.invokeAndWait(() -> {
            panel.clearLastMessagesForTest();
            panel.searchPatients("0901234567", "");
        });
        waitForWorkers(panel);

        assertEquals(ReceptionPanelMessagePolicy.MSG_PATIENT_SEARCH_FAILED, panel.getLastErrorMessage(),
                "searchPatients must surface ReceptionPanelMessagePolicy.MSG_PATIENT_SEARCH_FAILED on failure");
        assertNull(panel.getLastInfoMessage(),
                "Failure should not display 'Không tìm thấy hồ sơ phù hợp'");
    }

    @Test
    void searchPatientsSurfacesMessagePolicyErrorWithoutPartialResultsWhenSecondQueryFails() throws Exception {
        BenhNhan match = createSyntheticPatient(51, "Nguyen Van Test", "0901234567", "001090000001");
        searchBehavior = keyword -> {
            if ("0901234567".equals(keyword)) {
                return List.of(match);
            }
            throw new RuntimeException("Simulated second query DB error on CCCD");
        };

        SwingUtilities.invokeAndWait(() -> {
            panel.clearLastMessagesForTest();
            panel.searchPatients("0901234567", "001090000001");
        });
        waitForWorkers(panel);

        assertEquals(ReceptionPanelMessagePolicy.MSG_PATIENT_SEARCH_FAILED, panel.getLastErrorMessage(),
                "Error must be surfaced when the secondary CCCD query fails");
        assertNull(panel.getLastInfoMessage(),
                "Failure should not display 'Không tìm thấy hồ sơ phù hợp'");

        SwingUtilities.invokeAndWait(() -> {
            RegisterPatientCommand cmd = panel.getFormPanel().toRegisterCommand(createAuthzContext(), createAuditContext());
            assertNull(cmd.patient().getId(),
                    "Partial results from phone search must not be selected when CCCD query fails");
            assertEquals("", cmd.patient().getHoTen(),
                    "Form patient name must remain unpopulated when overall search fails");
        });
    }

    // =========================================================================
    // 4. Stale Worker Guard: viewSessionEpoch suppression on clearForm / onHide
    // =========================================================================

    @Test
    void clearFormIncrementsEpochAndSuppressesInFlightSearchCompletion() throws Exception {
        CountDownLatch searchStarted = new CountDownLatch(1);
        CountDownLatch searchRelease = new CountDownLatch(1);
        BenhNhan match = createSyntheticPatient(99, "Stale Search Patient", "0901234567", "001090000099");

        searchBehavior = keyword -> {
            searchStarted.countDown();
            try {
                assertTrue(searchRelease.await(5, TimeUnit.SECONDS), "Release latch timed out");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return List.of(match);
        };

        SwingUtilities.invokeAndWait(() -> panel.searchPatients("0901234567", ""));
        assertTrue(searchStarted.await(5, TimeUnit.SECONDS), "Search query should have started in worker");

        // User clears form while search is in-flight
        SwingUtilities.invokeAndWait(panel::clearForm);

        // Allow background worker to complete
        searchRelease.countDown();
        waitForWorkers(panel);

        SwingUtilities.invokeAndWait(() -> {
            RegisterPatientCommand cmd = panel.getFormPanel().toRegisterCommand(createAuthzContext(), createAuditContext());
            assertNull(cmd.patient().getId(),
                    "Stale search worker must not select patient after clearForm");
            assertEquals("", cmd.patient().getHoTen(),
                    "Patient name must remain empty on cleared form");
        });
    }

    @Test
    void onHideIncrementsEpochAndSuppressesInFlightSearchCompletion() throws Exception {
        CountDownLatch searchStarted = new CountDownLatch(1);
        CountDownLatch searchRelease = new CountDownLatch(1);
        BenhNhan match = createSyntheticPatient(99, "Stale Search Patient", "0901234567", "001090000099");

        searchBehavior = keyword -> {
            searchStarted.countDown();
            try {
                assertTrue(searchRelease.await(5, TimeUnit.SECONDS), "Release latch timed out");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return List.of(match);
        };

        SwingUtilities.invokeAndWait(() -> panel.searchPatients("0901234567", ""));
        assertTrue(searchStarted.await(5, TimeUnit.SECONDS), "Search query should have started in worker");

        // Tab is hidden while search is in-flight
        SwingUtilities.invokeAndWait(panel::onHide);

        // Allow background worker to complete
        searchRelease.countDown();
        waitForWorkers(panel);

        SwingUtilities.invokeAndWait(() -> {
            RegisterPatientCommand cmd = panel.getFormPanel().toRegisterCommand(createAuthzContext(), createAuditContext());
            assertNull(cmd.patient().getId(),
                    "Stale search worker must not select patient after onHide");
            assertEquals("", cmd.patient().getHoTen(),
                    "Patient name must remain empty when search completes after onHide");
        });
    }

    @Test
    void clearFormIncrementsEpochAndSuppressesInFlightRefreshCompletion() throws Exception {
        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch refreshRelease = new CountDownLatch(1);
        PhongKham staleRoom = createRoom(999, "Phòng 999 - Stale Overwrite");

        roomsSupplier = () -> {
            refreshStarted.countDown();
            try {
                assertTrue(refreshRelease.await(5, TimeUnit.SECONDS), "Release latch timed out");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return List.of(staleRoom);
        };

        SwingUtilities.invokeAndWait(panel::refreshData);
        assertTrue(refreshStarted.await(5, TimeUnit.SECONDS), "Refresh query should have started in worker");

        // Clear form while refresh is in-flight
        SwingUtilities.invokeAndWait(panel::clearForm);

        refreshRelease.countDown();
        waitForWorkers(panel);

        SwingUtilities.invokeAndWait(() -> {
            RegisterPatientCommand cmd = panel.getFormPanel().toRegisterCommand(createAuthzContext(), createAuditContext());
            assertNotEquals(999, cmd.roomId(),
                    "Stale refresh worker must not update room selection after clearForm");
            assertEquals(TEST_ROOM_ID, cmd.roomId(),
                    "Form should retain its active room binding rather than stale worker room");
        });
    }

    @Test
    void onHideIncrementsEpochAndSuppressesInFlightRefreshCompletion() throws Exception {
        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch refreshRelease = new CountDownLatch(1);
        PhongKham staleRoom = createRoom(999, "Phòng 999 - Stale Overwrite");

        roomsSupplier = () -> {
            refreshStarted.countDown();
            try {
                assertTrue(refreshRelease.await(5, TimeUnit.SECONDS), "Release latch timed out");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return List.of(staleRoom);
        };

        SwingUtilities.invokeAndWait(panel::refreshData);
        assertTrue(refreshStarted.await(5, TimeUnit.SECONDS), "Refresh query should have started in worker");

        // Tab is hidden while refresh is in-flight
        SwingUtilities.invokeAndWait(panel::onHide);

        refreshRelease.countDown();
        waitForWorkers(panel);

        SwingUtilities.invokeAndWait(() -> {
            RegisterPatientCommand cmd = panel.getFormPanel().toRegisterCommand(createAuthzContext(), createAuditContext());
            assertNotEquals(999, cmd.roomId(),
                    "Stale refresh worker must not update room selection after onHide");
            assertEquals(TEST_ROOM_ID, cmd.roomId(),
                    "Form should retain its active room binding rather than stale worker room");
        });
    }

    // =========================================================================
    // Helpers and Synthetic In-Memory Implementations
    // =========================================================================

    private void selectValidPatientAndRoom() {
        BenhNhan patient = createSyntheticPatient(51, "Nguyen Van Test", "0901234567", "001090000001");
        panel.getFormPanel().selectPatient(patient, null);
    }

    private static PhongKham createRoom(int id, String name) {
        PhongKham r = new PhongKham();
        r.setId(id);
        r.setTenPhong(name);
        r.setMaKhoa(TEST_DEPARTMENT_ID);
        r.setHoatDong(true);

        CoSo c = new CoSo();
        c.setId(TEST_FACILITY_ID);
        c.setTenCoSo("Cơ sở mẫu");
        c.setHoatDong(true);

        Khoa k = new Khoa();
        k.setId(TEST_DEPARTMENT_ID);
        k.setMaCoSo(TEST_FACILITY_ID);
        k.setTenKhoa("Khoa mẫu");
        k.setHoatDong(true);
        k.setCoSo(c);

        r.setKhoa(k);
        return r;
    }

    private static BenhNhan createSyntheticPatient(int id, String fullName, String phone, String cccd) {
        BenhNhan p = new BenhNhan();
        p.setId(id);
        int lastSpace = fullName.lastIndexOf(' ');
        p.setHo(lastSpace < 0 ? "" : fullName.substring(0, lastSpace));
        p.setTen(lastSpace < 0 ? fullName : fullName.substring(lastSpace + 1));
        p.setNgaySinh(LocalDate.of(1990, 5, 15));
        p.setGioiTinh(GioiTinh.M);
        p.setDienThoai(phone);
        p.setSoCCCD(cccd);
        p.setMaBenhNhanFormat("BN" + id);
        return p;
    }

    private static MaTranPhanQuyen createGrant(int roleId, String module, HanhDongQuyen action) {
        MaTranPhanQuyen g = new MaTranPhanQuyen();
        g.setId(1);
        g.setMaVaiTro(roleId);
        g.setPhanHe(module);
        g.setHanhDong(action);
        g.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);
        g.setDuocPhep(true);
        return g;
    }

    private static AuthorizationService createSyntheticAuthorization(SessionLifecycleService lifecycle) {
        NhanVien employee = new NhanVien();
        employee.setId(TEST_USER_ID);
        employee.setHoatDong(true);

        VaiTro role = new VaiTro();
        role.setId(ROLE.id());
        role.setTenVaiTro(ROLE.code());

        NhanVienWorkScope scope = new NhanVienWorkScope(
                SCOPE.facilityId(), SCOPE.facilityName(), SCOPE.departmentId(), SCOPE.departmentName());

        INhanVienRepository employeeRepo = mockInterface(INhanVienRepository.class, (proxy, method, args) -> switch (method.getName()) {
            case "getById" -> Optional.of(employee);
            case "getAssignedRoles" -> List.of(role);
            case "getAssignedWorkScopes" -> List.of(scope);
            default -> defaultValue(method);
        });

        List<MaTranPhanQuyen> grants = List.of(
                createGrant(ROLE.id(), "PATIENT", HanhDongQuyen.TAO),
                createGrant(ROLE.id(), "PATIENT", HanhDongQuyen.SUA),
                createGrant(ROLE.id(), "PATIENT", HanhDongQuyen.XEM),
                createGrant(ROLE.id(), "RECEPTION", HanhDongQuyen.TAO),
                createGrant(ROLE.id(), "RECEPTION", HanhDongQuyen.XEM),
                createGrant(ROLE.id(), "CATALOG", HanhDongQuyen.XEM)
        );

        ISystemRepository systemRepo = new ISystemRepository() {
            @Override public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) { return grants; }
            @Override public Optional<String> getSystemParameterValue(String key) { return Optional.empty(); }
        };

        return new AuthorizationService(lifecycle, employeeRepo, systemRepo);
    }

    private AuthorizationContext createAuthzContext() {
        return new AuthorizationContext(TEST_USER_ID, ROLE.id(), ROLE.code(),
                SCOPE.facilityId(), SCOPE.departmentId(), sessionId);
    }

    private AuditActorContext createAuditContext() {
        return new AuditActorContext(TEST_USER_ID, ROLE.id(), ROLE.code(),
                SCOPE.facilityId(), SCOPE.departmentId(), "RECEPTION-01", UUID.randomUUID());
    }

    private static void waitForWorkers(ReceptionPanel panel) throws Exception {
        for (int i = 0; i < 50; i++) {
            for (SwingWorker<?, ?> worker : panel.getInFlightWorkers()) {
                try {
                    worker.get(100, TimeUnit.MILLISECONDS);
                } catch (TimeoutException | CancellationException | ExecutionException ignored) {
                }
            }
            if (panel.getInFlightWorkersCount() == 0) {
                break;
            }
            Thread.sleep(20);
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeAndWait(() -> {});
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T mockInterface(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    private static Object defaultValue(java.lang.reflect.Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(Optional.class)) return Optional.empty();
        if (returnType.equals(List.class)) return Collections.emptyList();
        if (returnType.equals(boolean.class) || returnType.equals(Boolean.class)) return true;
        if (returnType.equals(int.class) || returnType.equals(Integer.class)) return 0;
        return null;
    }

    private static final class RecordingAuditWritePort implements AuditWritePort {
        private final List<SystemAuditEntry> entries = new CopyOnWriteArrayList<>();

        @Override
        public void appendInCurrentTransaction(SystemAuditEntry entry) {
            entries.add(entry);
        }

        @Override
        public void appendIndependently(SystemAuditEntry entry) {
            entries.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}

        @Override
        public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}

        public List<SystemAuditEntry> getEntries() {
            return List.copyOf(entries);
        }

        public void clear() {
            entries.clear();
        }
    }
}
