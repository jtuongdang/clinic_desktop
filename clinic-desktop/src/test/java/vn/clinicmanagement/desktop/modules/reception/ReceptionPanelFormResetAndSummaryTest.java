package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.auth.CompletedLoginSession;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.reception.ClinicRoomQueueSummary;
import vn.clinicmanagement.application.reception.GetReceptionQueueHandler;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.application.reception.ReceptionQueuePort;
import vn.clinicmanagement.application.reception.RegisterPatientCommand;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.desktop.modules.reception.components.MedicalPopupChooser;
import vn.clinicmanagement.desktop.modules.reception.components.PatientRegistrationFormPanel;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.ISystemRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReceptionPanelFormResetAndSummaryTest {
    private static final RoleChoice ROLE = new RoleChoice(10, "RECEPTIONIST", "Tiếp đón mẫu");
    private static final WorkScopeChoice SCOPE = new WorkScopeChoice(1, "Cơ sở mẫu", 2, "Khoa mẫu");
    private static final int EMPLOYEE_ID = 101;
    private static final Clock SESSION_CLOCK = Clock.fixed(Instant.parse("2026-11-07T08:00:00Z"), ZoneOffset.UTC);

    private final UserSession session = UserSession.getInstance();
    private final UUID sessionId = UUID.randomUUID();
    private final SyntheticQueue queue = new SyntheticQueue();
    private SessionLifecycleService lifecycle;
    private ReceptionPanel panel;

    @BeforeAll
    static void configureHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void setUp() throws Exception {
        session.clearSession();
        AuditWritePort audit = new AuditWritePort() {
            @Override public void appendInCurrentTransaction(SystemAuditEntry entry) {}
            @Override public void appendIndependently(SystemAuditEntry entry) {}
            @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}
            @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
        };
        lifecycle = new SessionLifecycleService(session,
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)),
                SESSION_CLOCK, audit, Runnable::run);
        lifecycle.startSession(new CompletedLoginSession(EMPLOYEE_ID, "reception.test", "Nhân viên mẫu",
                ROLE, SCOPE, sessionId, SESSION_CLOCK.instant()));
        GetReceptionQueueHandler handler = new GetReceptionQueueHandler(queue, syntheticAuthorization(lifecycle), audit);
        SwingUtilities.invokeAndWait(() -> panel = new ReceptionPanel(handler, null, null, null, lifecycle));
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (panel != null) SwingUtilities.invokeAndWait(panel::destroy);
        } finally {
            session.clearSession();
        }
    }

    @Test
    void toolbarNewClearsSelectedPatientAndRestoresDefaults() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectPatientAndChangeVisitDefaults();
            clickNew(panel.getTopToolbar());
            assertFormReset();
        });
    }

    @Test
    void footerNewClearsSelectedPatientAndRestoresDefaults() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectPatientAndChangeVisitDefaults();
            clickNew(((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH));
            assertFormReset();
        });
    }

    @Test
    void logoutClearsSelectedPatientAndRestoresDefaults() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selectPatientAndChangeVisitDefaults();
            assertTrue(panel.isFormEditable());
            lifecycle.logout();
            assertFormReset();
            assertFalse(panel.isFormEditable());
        });
    }

    @Test
    void refreshPopulatesBothEmptyRoomTablesAndClearsThemOnEmptySnapshot() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(2, roomTables().size());
            roomTables().forEach(table -> assertEquals(0, table.getRowCount()));
        });

        queue.rooms = List.of(
                new ClinicRoomQueueSummary(71, "Phòng mẫu A", "Khoa mẫu", 8, 5, 2),
                new ClinicRoomQueueSummary(72, "Phòng mẫu B", "Khoa mẫu", 6, 3, 1));
        refreshAndWait();
        SwingUtilities.invokeAndWait(() -> {
            List<JTable> tables = roomTables();
            assertRoomRow(tables.get(0), "Phòng mẫu A", 8, 5, 2);
            assertRoomRow(tables.get(1), "Phòng mẫu B", 6, 3, 1);
        });

        queue.rooms = List.of();
        refreshAndWait();
        SwingUtilities.invokeAndWait(() -> roomTables().forEach(table -> assertEquals(0, table.getRowCount())));
    }

    @Test
    void refreshQueriesTheClinicDateAcrossMidnightWithUtcJvmAndClock() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertQueryDate("2026-11-07T16:59:59Z", LocalDate.of(2026, 11, 7));
            assertQueryDate("2026-11-07T17:00:00Z", LocalDate.of(2026, 11, 8));
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    private void selectPatientAndChangeVisitDefaults() {
        PhongKham defaultRoom = new PhongKham();
        defaultRoom.setId(71);
        defaultRoom.setTenPhong("Phòng mẫu A");
        PhongKham otherRoom = new PhongKham();
        otherRoom.setId(72);
        otherRoom.setTenPhong("Phòng mẫu B");
        panel.updateActiveRooms(List.of(defaultRoom, otherRoom));

        BenhNhan patient = new BenhNhan();
        patient.setId(51);
        patient.setTen("Người bệnh mẫu");
        patient.setGioiTinh(GioiTinh.M);
        patient.setDanToc("Dân tộc mẫu");
        patient.setQuocTich("Quốc tịch mẫu");
        patient.setDiaChi("Địa chỉ mẫu");
        BaoHiem insurance = new BaoHiem();
        insurance.setMucHuong(80);
        insurance.setDiaChiThe("Địa chỉ thẻ mẫu");

        PatientRegistrationFormPanel form = panel.getFormPanel();
        form.selectPatient(patient, insurance);
        comboStartingWith("Khám bệnh").setSelectedItem("Tái khám");
        comboStartingWith("Viện phí").setSelectedItem("BHYT");
        components(form, MedicalPopupChooser.class).stream()
                .filter(chooser -> defaultRoom.getTenPhong().equals(chooser.getText()))
                .findFirst().orElseThrow().setText(otherRoom.getTenPhong());
        RegisterPatientCommand selected = formCommand();
        assertEquals(51, selected.patient().getId());
        assertEquals(72, selected.roomId());
        assertEquals("Tái khám", selected.registrationType());
        assertEquals(DoiTuongKham.BHYT, selected.payerType());
        assertEquals(80, selected.insurance().getMucHuong());
    }

    private void assertFormReset() {
        RegisterPatientCommand cleared = formCommand();
        assertNull(cleared.patient().getId());
        assertEquals("", cleared.patient().getHoTen());
        assertEquals("", cleared.patient().getDiaChi());
        assertEquals(GioiTinh.F, cleared.patient().getGioiTinh());
        assertEquals("Kinh", cleared.patient().getDanToc());
        assertEquals("Việt nam", cleared.patient().getQuocTich());
        assertEquals(5, comboStartingWith("Kinh").getItemCount());
        assertEquals(2, comboStartingWith("Việt nam").getItemCount());
        assertNull(cleared.insurance());
        assertEquals(71, cleared.roomId());
        assertEquals("Khám bệnh", cleared.registrationType());
        assertEquals(DoiTuongKham.THU_PHI, cleared.payerType());
        assertTrue(components(panel.getFormPanel(), JTextField.class).stream()
                .anyMatch(field -> field.getText().matches("\\d{2}:\\d{2} \\d{2}/\\d{2}/\\d{4}")),
                "The reset should restore the registration timestamp");
    }

    private RegisterPatientCommand formCommand() {
        return panel.getFormPanel().toRegisterCommand(
                new AuthorizationContext(EMPLOYEE_ID, ROLE.id(), ROLE.code(),
                        SCOPE.facilityId(), SCOPE.departmentId(), sessionId),
                new AuditActorContext(EMPLOYEE_ID, ROLE.id(), ROLE.code(),
                        SCOPE.facilityId(), SCOPE.departmentId(), "SYNTHETIC-01", UUID.randomUUID()));
    }

    private JComboBox<?> comboStartingWith(String firstItem) {
        return components(panel.getFormPanel(), JComboBox.class).stream()
                .filter(combo -> firstItem.equals(combo.getItemAt(0)))
                .findFirst().orElseThrow();
    }

    private static void clickNew(Component bar) {
        components(bar, JButton.class).stream()
                .filter(button -> button.getText() != null && button.getText().contains("Bệnh Nhân Mới"))
                .findFirst().orElseThrow().doClick(0);
    }

    private List<JTable> roomTables() {
        // Only the first two room grids display summaries. The third grid is decorative.
        return components(panel.getStatusPanel(), JTable.class).stream()
                .filter(table -> table.getColumnCount() == 4)
                .limit(2)
                .toList();
    }

    private static void assertRoomRow(JTable table, String name, int registered, int waiting, int examining) {
        assertEquals(1, table.getRowCount());
        assertEquals(name, table.getValueAt(0, 0));
        assertEquals(registered, table.getValueAt(0, 1));
        assertEquals(waiting, table.getValueAt(0, 2));
        assertEquals(examining, table.getValueAt(0, 3));
    }

    private void assertQueryDate(String instant, LocalDate expected) throws Exception {
        SwingUtilities.invokeAndWait(() -> panel.setQueueClock(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)));
        refreshAndWait();
        assertEquals(expected, queue.queueDate);
        assertEquals(expected, queue.summaryDate);
    }

    private void refreshAndWait() throws Exception {
        SwingUtilities.invokeAndWait(panel::refreshData);
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (panel.getInFlightWorkersCount() > 0 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(0, panel.getInFlightWorkersCount(), "Refresh worker did not finish");
        SwingUtilities.invokeAndWait(() -> {});
    }

    private static <T extends Component> List<T> components(Component root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        if (type.isInstance(root)) matches.add(type.cast(root));
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                matches.addAll(components(child, type));
            }
        }
        return matches;
    }

    private static AuthorizationService syntheticAuthorization(SessionLifecycleService lifecycle) {
        NhanVien employee = new NhanVien();
        employee.setId(EMPLOYEE_ID);
        employee.setHoatDong(true);
        VaiTro role = new VaiTro();
        role.setId(ROLE.id());
        role.setTenVaiTro(ROLE.code());
        NhanVienWorkScope scope = new NhanVienWorkScope(
                SCOPE.facilityId(), SCOPE.facilityName(), SCOPE.departmentId(), SCOPE.departmentName());
        INhanVienRepository employees = (INhanVienRepository) Proxy.newProxyInstance(
                INhanVienRepository.class.getClassLoader(), new Class<?>[]{INhanVienRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getById" -> Optional.of(employee);
                    case "getAssignedRoles" -> List.of(role);
                    case "getAssignedWorkScopes" -> List.of(scope);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        MaTranPhanQuyen grant = new MaTranPhanQuyen();
        grant.setMaVaiTro(ROLE.id());
        grant.setPhanHe("RECEPTION");
        grant.setHanhDong(HanhDongQuyen.XEM);
        grant.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);
        grant.setDuocPhep(true);
        ISystemRepository system = new ISystemRepository() {
            @Override public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) { return List.of(grant); }
            @Override public Optional<String> getSystemParameterValue(String key) { return Optional.empty(); }
        };
        return new AuthorizationService(lifecycle, employees, system);
    }

    private static final class SyntheticQueue implements ReceptionQueuePort {
        private volatile List<ClinicRoomQueueSummary> rooms = List.of();
        private volatile LocalDate queueDate;
        private volatile LocalDate summaryDate;

        @Override
        public LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                                      String registrationType, String reason, DoiTuongKham payerType,
                                      boolean priority, int createdBy) {
            throw new UnsupportedOperationException("Refresh tests only read the synthetic queue");
        }

        @Override
        public List<ReceptionQueueItem> getQueue(LocalDate date, int facilityId, int departmentId) {
            queueDate = date;
            return List.of();
        }

        @Override
        public List<ClinicRoomQueueSummary> getRoomSummaries(LocalDate date, int facilityId, int departmentId) {
            summaryDate = date;
            return rooms;
        }
    }
}
