package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.ActiveSessionPort;
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
import vn.clinicmanagement.application.reception.RegistrationReceipt;
import vn.clinicmanagement.application.reception.SearchPatientsHandler;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ReceptionPanelActiveRoomBindingTest {

    private static final int TEST_FACILITY_ID = 1;
    private static final int TEST_DEPARTMENT_ID = 1;
    private static final int TEST_USER_ID = 1;
    private static final int TEST_ROLE_ID = 1;
    private static final String TEST_ROLE_CODE = "RECEPTIONIST";

    private final AtomicInteger capturedRoomId = new AtomicInteger(-1);
    private final AtomicInteger visitCallCount = new AtomicInteger(0);
    private final List<PhongKham> availableRooms = new ArrayList<>();

    private ReceptionQueuePort queuePort;
    private AuditWritePort auditPort;
    private AuthorizationService authzService;
    private IPhongKhamRepository roomsRepo;
    private IBenhNhanRepository patientRepo;
    private IBenhNhanCoSoRepository patientFacilityRepo;
    private IBaoHiemRepository insuranceRepo;
    private TransactionBoundary txBoundary;

    private GetReceptionQueueHandler queueHandler;
    private GetActiveReceptionRoomsHandler roomsHandler;
    private RegisterPatientHandler registerPatientHandler;
    private SearchPatientsHandler searchPatientsHandler;

    private ReceptionPanel panel;

    @BeforeAll
    static void configureHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void setUp() throws Exception {
        UserSession.getInstance().clearSession();
        capturedRoomId.set(-1);
        visitCallCount.set(0);
        availableRooms.clear();

        queuePort = new ReceptionQueuePort() {
            @Override
            public LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                                          String registrationType, String reason, DoiTuongKham payerType,
                                          boolean priority, int createdBy) {
                visitCallCount.incrementAndGet();
                capturedRoomId.set(roomId);
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

        auditPort = new AuditWritePort() {
            @Override public void appendInCurrentTransaction(SystemAuditEntry entry) {}
            @Override public void appendIndependently(SystemAuditEntry entry) {}
            @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}
            @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
        };

        ActiveSessionPort sessionPort = sessionId -> Optional.of(new ActiveSessionPort.ActiveSession(
                sessionId, TEST_USER_ID, TEST_ROLE_ID, TEST_ROLE_CODE,
                TEST_FACILITY_ID, TEST_DEPARTMENT_ID, true, true, true));

        NhanVien employee = new NhanVien();
        employee.setId(TEST_USER_ID);
        employee.setHoatDong(true);
        VaiTro role = new VaiTro();
        role.setId(TEST_ROLE_ID);
        role.setTenVaiTro(TEST_ROLE_CODE);

        INhanVienRepository employeeRepo = mockInterface(INhanVienRepository.class, (proxy, method, args) -> switch (method.getName()) {
            case "getById" -> Optional.of(employee);
            case "getAssignedRoles" -> List.of(role);
            case "getAssignedWorkScopes" -> List.of(new NhanVienWorkScope(
                    TEST_FACILITY_ID, "Facility", TEST_DEPARTMENT_ID, "Department"));
            default -> defaultValue(method);
        });

        List<MaTranPhanQuyen> grants = List.of(
                createGrant(TEST_ROLE_ID, "PATIENT", HanhDongQuyen.TAO),
                createGrant(TEST_ROLE_ID, "PATIENT", HanhDongQuyen.SUA),
                createGrant(TEST_ROLE_ID, "PATIENT", HanhDongQuyen.XEM),
                createGrant(TEST_ROLE_ID, "RECEPTION", HanhDongQuyen.TAO),
                createGrant(TEST_ROLE_ID, "RECEPTION", HanhDongQuyen.XEM),
                createGrant(TEST_ROLE_ID, "CATALOG", HanhDongQuyen.XEM)
        );

        ISystemRepository systemRepo = new ISystemRepository() {
            @Override public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) { return grants; }
            @Override public Optional<String> getSystemParameterValue(String key) { return Optional.empty(); }
        };

        authzService = new AuthorizationService(sessionPort, employeeRepo, systemRepo);

        roomsRepo = new IPhongKhamRepository() {
            @Override
            public Optional<PhongKham> getById(int id) {
                return availableRooms.stream().filter(r -> r.getId() != null && r.getId() == id).findFirst();
            }

            @Override
            public List<PhongKham> getActiveRooms() {
                return new ArrayList<>(availableRooms);
            }

            @Override
            public List<PhongKham> getActiveRoomsInScope(int facilityId, int departmentId) {
                return new ArrayList<>(availableRooms);
            }
        };

        patientRepo = mockInterface(IBenhNhanRepository.class, (proxy, method, args) -> switch (method.getName()) {
            case "getByIdInScope" -> Optional.of(createSyntheticPatient((Integer) args[0], "Nguyen Van Test", "0901234567", "001090000001"));
            case "getByCCCD", "getByCCCDInScope", "getByPhone" -> Optional.empty();
            case "search", "searchInScope" -> Collections.emptyList();
            case "add" -> {
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

        // Explicit concrete handler instantiation - no proxy
        queueHandler = new GetReceptionQueueHandler(queuePort, authzService, auditPort);
        roomsHandler = new GetActiveReceptionRoomsHandler(roomsRepo, authzService, auditPort);
        registerPatientHandler = new RegisterPatientHandler(
                patientRepo, patientFacilityRepo, insuranceRepo, roomsRepo, queuePort,
                txBoundary, auditPort, authzService);
        searchPatientsHandler = new SearchPatientsHandler(patientRepo, insuranceRepo, authzService, auditPort);

        SwingUtilities.invokeAndWait(() -> {
            panel = new ReceptionPanel(queueHandler, roomsHandler, registerPatientHandler, searchPatientsHandler, null);
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        if (panel != null) {
            SwingUtilities.invokeAndWait(panel::destroy);
        }
    }

    @Test
    void concreteGetReceptionQueueHandlerIsInstantiatedDirectlyWithoutProxy() {
        assertNotNull(queueHandler);
        assertEquals(GetReceptionQueueHandler.class, queueHandler.getClass());
        assertFalse(Proxy.isProxyClass(queueHandler.getClass()),
                "GetReceptionQueueHandler must be instantiated directly as a concrete class, not proxied");
    }

    @Test
    void selectedRealRoomIdPropagatesToRegisterPatientCommandAndHandler() throws Exception {
        PhongKham room = createRoom(77, "Phòng 102 - Khám Nhi");
        availableRooms.add(room);

        AtomicReference<RegisterPatientCommand> capturedCommand = new AtomicReference<>();
        panel.setCommandConsumerForTest(capturedCommand::set);

        SwingUtilities.invokeAndWait(() -> {
            panel.updateActiveRooms(List.of(room));
            BenhNhan patient = createSyntheticPatient(10, "Nguyen Van Test", "0901234567", "001090000001");
            panel.getFormPanel().selectPatient(patient, null);
            panel.saveRegistration();
        });

        waitForWorkers(panel);

        assertNotNull(capturedCommand.get(), "RegisterPatientCommand should have been created");
        assertEquals(77, capturedCommand.get().roomId(),
                "Selected real room ID 77 must reach RegisterPatientCommand");
        assertEquals(77, capturedRoomId.get(),
                "Selected real room ID 77 must reach RegisterPatientHandler / ReceptionQueuePort");
        assertEquals(1, visitCallCount.get(),
                "Exactly one visit registration should have been performed");
    }

    @Test
    void successfulEmptyRoomsResultClearsSelectionAndStaleIdentity() throws Exception {
        PhongKham initialRoom = createRoom(88, "Phòng 105 - Cấp Cứu");
        availableRooms.add(initialRoom);

        SwingUtilities.invokeAndWait(() -> {
            panel.updateActiveRooms(List.of(initialRoom));
            BenhNhan patient = createSyntheticPatient(11, "Tran Thi Test", "0912345678", "001091000002");
            panel.getFormPanel().selectPatient(patient, null);
        });

        // Verify room is currently selected and command can be built
        AuthorizationContext authzContext = new AuthorizationContext(
                TEST_USER_ID, TEST_ROLE_ID, TEST_ROLE_CODE, TEST_FACILITY_ID, TEST_DEPARTMENT_ID, UUID.randomUUID());
        AuditActorContext auditContext = new AuditActorContext(
                TEST_USER_ID, TEST_ROLE_ID, TEST_ROLE_CODE, TEST_FACILITY_ID, TEST_DEPARTMENT_ID, "RECEPTION-01", UUID.randomUUID());

        SwingUtilities.invokeAndWait(() -> {
            RegisterPatientCommand cmd = panel.getFormPanel().toRegisterCommand(authzContext, auditContext);
            assertEquals(88, cmd.roomId());
        });

        // Now pass empty active rooms list
        SwingUtilities.invokeAndWait(() -> {
            panel.updateActiveRooms(List.of());
        });

        // Verify that stale room identity is cleared and toRegisterCommand throws IllegalStateException
        SwingUtilities.invokeAndWait(() -> {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> panel.getFormPanel().toRegisterCommand(authzContext, auditContext));
            assertTrue(ex.getMessage().contains("clinic room"),
                    "Stale room identity must be cleared, producing missing room IllegalStateException");
        });
    }

    @Test
    void saveWithoutSelectedActiveRoomProducesSafeValidationResultWithoutUncaughtException() throws Exception {
        // Form has no active rooms
        SwingUtilities.invokeAndWait(() -> {
            panel.updateActiveRooms(List.of());
            BenhNhan patient = createSyntheticPatient(12, "Le Van Test", "0987654321", "001092000003");
            panel.getFormPanel().selectPatient(patient, null);
            panel.clearLastMessagesForTest();
        });

        AtomicReference<String> errorReceived = new AtomicReference<>();
        panel.setErrorListenerForTest(errorReceived::set);

        // Save on EDT
        SwingUtilities.invokeAndWait(() -> {
            assertDoesNotThrow(() -> panel.saveRegistration(),
                    "saveRegistration without an active room must not throw uncaught EDT exception");
        });

        waitForWorkers(panel);

        assertEquals(ReceptionPanelMessagePolicy.toUserMessage("PATIENT_VALIDATION"), panel.getLastErrorMessage(),
                "Safe validation message should be presented when saving without an active room");
        assertEquals(ReceptionPanelMessagePolicy.toUserMessage("PATIENT_VALIDATION"), errorReceived.get(),
                "Error listener should receive safe validation message");
        assertEquals(0, visitCallCount.get(),
                "No visit registration must occur when room validation fails");
    }

    @Test
    void refreshDataWithEmptyRoomsQueryClearsStaleRoomIdentity() throws Exception {
        PhongKham initialRoom = createRoom(99, "Phòng 201 - Răng Hàm Mặt");
        availableRooms.add(initialRoom);

        SwingUtilities.invokeAndWait(() -> {
            panel.updateActiveRooms(List.of(initialRoom));
            BenhNhan patient = createSyntheticPatient(13, "Pham Thi Test", "0976543210", "001093000004");
            panel.getFormPanel().selectPatient(patient, null);
        });

        // Clear available rooms so query returns empty list
        availableRooms.clear();

        SwingUtilities.invokeAndWait(panel::refreshData);
        waitForWorkers(panel);

        AuthorizationContext authzContext = new AuthorizationContext(
                TEST_USER_ID, TEST_ROLE_ID, TEST_ROLE_CODE, TEST_FACILITY_ID, TEST_DEPARTMENT_ID, UUID.randomUUID());
        AuditActorContext auditContext = new AuditActorContext(
                TEST_USER_ID, TEST_ROLE_ID, TEST_ROLE_CODE, TEST_FACILITY_ID, TEST_DEPARTMENT_ID, "RECEPTION-01", UUID.randomUUID());

        SwingUtilities.invokeAndWait(() -> {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> panel.getFormPanel().toRegisterCommand(authzContext, auditContext));
            assertTrue(ex.getMessage().contains("clinic room"));
        });
    }

    private static PhongKham createRoom(int id, String name) {
        PhongKham r = new PhongKham();
        r.setId(id);
        r.setTenPhong(name);
        r.setMaKhoa(TEST_DEPARTMENT_ID);
        r.setHoatDong(true);
        CoSo c = new CoSo();
        c.setId(TEST_FACILITY_ID);
        c.setTenCoSo("Cơ sở 1");
        c.setHoatDong(true);
        Khoa k = new Khoa();
        k.setId(TEST_DEPARTMENT_ID);
        k.setMaCoSo(TEST_FACILITY_ID);
        k.setTenKhoa("Khoa Khám Bệnh");
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

    private static void waitForWorkers(ReceptionPanel panel) throws Exception {
        for (int i = 0; i < 50; i++) {
            for (SwingWorker<?, ?> worker : panel.getInFlightWorkers()) {
                try {
                    worker.get(100, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignored) {
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
}
