package vn.clinicmanagement.application.reception;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.PatientIdentityConflictException;
import vn.clinicmanagement.domain.repositories.IBaoHiemRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterPatientHandlerTest {
    private static final int ROOM_ID = 31;
    private static final int FACILITY_ID = 11;
    private static final int DEPARTMENT_ID = 21;
    private static final String SYNTHETIC_CCCD = "synthetic-cccd-001";

    @Test
    void invalidCommandIsRejectedBeforeTransactionOrPersistence() {
        Fixture fixture = new Fixture();

        Result<RegistrationReceipt> result = fixture.handler.handle(null);

        assertFailure(result, "PATIENT_VALIDATION");
        assertEquals(0, fixture.transactions.calls);
        assertNoWritesOrVisit(fixture);
    }

    @Test
    void newPatientRegistrationPersistsPatientInsuranceAndVisitReceipt() {
        Fixture fixture = new Fixture();
        BenhNhan patient = validPatient();
        BaoHiem insurance = validInsurance();
        fixture.patientInsertId = 41;
        fixture.visit = visit(91, 7, "Q-007");

        Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, insurance));

        assertTrue(result.isSuccess());
        RegistrationReceipt receipt = result.valueOrThrow();
        assertEquals(41, receipt.patientId());
        assertEquals("SYNTH-PATIENT-41", receipt.patientCode());
        assertEquals(91, receipt.visitId());
        assertEquals(7, receipt.queueNumber());
        assertEquals("Q-007", receipt.queueNumberDisplay());
        assertEquals(1, fixture.patientAdds.get());
        assertEquals(0, fixture.patientUpdates.get());
        assertEquals(1, fixture.insuranceAdds.get());
        assertEquals(0, fixture.insuranceUpdates.get());
        assertEquals(41, insurance.getMaBenhNhan());
        assertEquals(1, fixture.visitCalls.get());
        assertEquals(List.of(41, FACILITY_ID, DEPARTMENT_ID, ROOM_ID, "Khám bệnh",
                "Synthetic reason", DoiTuongKham.BHYT, true, 5), fixture.visitArguments);
        assertEquals(1, fixture.transactions.calls);
        assertEquals(0, fixture.transactions.failures);
    }

    @Test
    void existingPatientIsUpdatedAndNotInsertedAgain() {
        Fixture fixture = new Fixture();
        BenhNhan patient = validPatient();
        patient.setId(52);
        patient.setMaBenhNhanFormat("SYNTH-EXISTING-52");
        BaoHiem insurance = validInsurance();
        BaoHiem currentInsurance = new BaoHiem();
        currentInsurance.setId(8);
        currentInsurance.setVersion(3L);
        fixture.currentInsurance = currentInsurance;
        fixture.visit = visit(92, 8, "Q-008");

        Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, insurance));

        assertTrue(result.isSuccess());
        assertEquals(52, result.valueOrThrow().patientId());
        assertEquals("SYNTH-EXISTING-52", result.valueOrThrow().patientCode());
        assertEquals(0, fixture.patientAdds.get());
        assertEquals(1, fixture.patientUpdates.get());
        assertEquals(0, fixture.insuranceAdds.get());
        assertEquals(1, fixture.insuranceUpdates.get());
        assertEquals(52, insurance.getMaBenhNhan());
        assertEquals(8, insurance.getId());
        assertEquals(3L, insurance.getVersion());
        assertEquals(1, fixture.visitCalls.get());
        assertEquals(1, fixture.transactions.calls);
        assertEquals(0, fixture.transactions.failures);
    }

    @Test
    void inactiveOrUnlinkedRoomIsRejectedWithoutWrites() {
        List<PhongKham> unavailableRooms = new ArrayList<>();
        PhongKham inactive = activeRoom();
        inactive.setHoatDong(false);
        unavailableRooms.add(inactive);
        PhongKham withoutDepartment = activeRoom();
        withoutDepartment.setKhoa(null);
        unavailableRooms.add(withoutDepartment);
        PhongKham withoutFacility = activeRoom();
        withoutFacility.getKhoa().setCoSo(null);
        unavailableRooms.add(withoutFacility);

        for (PhongKham room : unavailableRooms) {
            Fixture fixture = new Fixture();
            fixture.room = room;

            Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

            assertFailure(result, "ROOM_UNAVAILABLE");
            assertNoWritesOrVisit(fixture);
            assertEquals(1, fixture.transactions.failures);
        }
    }

    @Test
    void duplicateCccdPrecheckReturnsStableErrorWithoutInsuranceOrVisit() {
        Fixture fixture = new Fixture();
        fixture.duplicatePatient = validPatient();

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

        assertFailure(result, "PATIENT_DUPLICATE_CCCD");
        assertEquals(0, fixture.patientAdds.get());
        assertEquals(0, fixture.patientUpdates.get());
        assertEquals(0, fixture.insuranceAdds.get());
        assertEquals(0, fixture.insuranceUpdates.get());
        assertEquals(0, fixture.visitCalls.get());
        assertEquals(1, fixture.transactions.failures);
    }

    @Test
    void databaseCccdConflictReturnsStableErrorWithoutInsuranceOrVisit() {
        Fixture fixture = new Fixture();
        fixture.identityConflictOnInsert = true;

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

        assertFailure(result, "PATIENT_DUPLICATE_CCCD");
        assertEquals(1, fixture.patientAdds.get());
        assertEquals(0, fixture.insuranceAdds.get());
        assertEquals(0, fixture.insuranceUpdates.get());
        assertEquals(0, fixture.visitCalls.get());
        assertEquals(1, fixture.transactions.failures);
    }

    @Test
    void insuranceFailureReturnsSafeErrorAndDoesNotCreateVisit() {
        Fixture fixture = new Fixture();
        fixture.insuranceFailure = new IllegalStateException("failure containing " + SYNTHETIC_CCCD);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

        assertSafePersistenceFailure(result);
        assertEquals(1, fixture.patientAdds.get());
        assertEquals(1, fixture.insuranceAdds.get());
        assertEquals(0, fixture.visitCalls.get());
        assertEquals(1, fixture.transactions.failures);
    }

    @Test
    void queueFailureReturnsSafeErrorWithoutFurtherPersistenceCalls() {
        Fixture fixture = new Fixture();
        fixture.queueFailure = new IllegalStateException("failure containing " + SYNTHETIC_CCCD);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

        assertSafePersistenceFailure(result);
        assertEquals(1, fixture.patientAdds.get());
        assertEquals(1, fixture.insuranceAdds.get());
        assertEquals(1, fixture.visitCalls.get());
        assertEquals(1, fixture.transactions.failures);
    }

    private static void assertSafePersistenceFailure(Result<RegistrationReceipt> result) {
        assertFailure(result, "PATIENT_REGISTRATION_FAILED");
        String message = result.getError().orElseThrow().message();
        assertFalse(message.contains("failure containing"));
        assertFalse(message.contains(SYNTHETIC_CCCD));
    }

    private static void assertFailure(Result<RegistrationReceipt> result, String code) {
        assertTrue(result.isFailure());
        assertEquals(code, result.getError().orElseThrow().code());
    }

    private static void assertNoWritesOrVisit(Fixture fixture) {
        assertEquals(0, fixture.patientAdds.get());
        assertEquals(0, fixture.patientUpdates.get());
        assertEquals(0, fixture.insuranceAdds.get());
        assertEquals(0, fixture.insuranceUpdates.get());
        assertEquals(0, fixture.visitCalls.get());
    }

    private static RegisterPatientCommand command(BenhNhan patient, BaoHiem insurance) {
        return new RegisterPatientCommand(patient, insurance, ROOM_ID, "Khám bệnh", "Synthetic reason",
                DoiTuongKham.BHYT, true, 5);
    }

    private static BenhNhan validPatient() {
        BenhNhan patient = new BenhNhan();
        patient.setHo("Synthetic");
        patient.setTen("Patient");
        patient.setNgaySinh(LocalDate.of(1990, 1, 1));
        patient.setSoCCCD(SYNTHETIC_CCCD);
        return patient;
    }

    private static BaoHiem validInsurance() {
        BaoHiem insurance = new BaoHiem();
        insurance.setSoBaoHiem("synthetic-insurance-001");
        insurance.setNgayHetHan(LocalDate.of(2030, 1, 1));
        insurance.setMucHuong(80);
        return insurance;
    }

    private static PhongKham activeRoom() {
        CoSo facility = new CoSo();
        facility.setId(FACILITY_ID);
        Khoa department = new Khoa();
        department.setId(DEPARTMENT_ID);
        department.setCoSo(facility);
        PhongKham room = new PhongKham();
        room.setId(ROOM_ID);
        room.setMaKhoa(DEPARTMENT_ID);
        room.setKhoa(department);
        return room;
    }

    private static LuotKham visit(int id, int queueNumber, String displayNumber) {
        LuotKham visit = new LuotKham();
        visit.setId(id);
        visit.setSoThuTu(queueNumber);
        visit.setSoThuTuDisplay(displayNumber);
        return visit;
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> instance == args[0];
                    case "hashCode" -> System.identityHashCode(instance);
                    case "toString" -> type.getSimpleName() + " test proxy";
                    default -> null;
                };
            }
            return invocation.invoke(method.getName(), args);
        });
        return type.cast(proxy);
    }

    private static Object defaultValue(String method) {
        return switch (method) {
            case "getById", "getLatestByPatientId", "getByCCCD", "getByPhone" -> Optional.empty();
            case "getAll", "search", "getByPatientId", "getActiveRooms" -> List.of();
            default -> null;
        };
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args);
    }

    private static final class Fixture {
        private final AtomicInteger patientAdds = new AtomicInteger();
        private final AtomicInteger patientUpdates = new AtomicInteger();
        private final AtomicInteger insuranceAdds = new AtomicInteger();
        private final AtomicInteger insuranceUpdates = new AtomicInteger();
        private final AtomicInteger visitCalls = new AtomicInteger();
        private final List<Object> visitArguments = new ArrayList<>();
        private final TransactionProbe transactions = new TransactionProbe();
        private final RegisterPatientHandler handler;
        private PhongKham room = activeRoom();
        private BenhNhan duplicatePatient;
        private BaoHiem currentInsurance;
        private int patientInsertId = 41;
        private boolean identityConflictOnInsert;
        private RuntimeException insuranceFailure;
        private RuntimeException queueFailure;
        private LuotKham visit = visit(93, 9, "Q-009");

        private Fixture() {
            IBenhNhanRepository patients = proxy(IBenhNhanRepository.class, (method, args) -> {
                return switch (method) {
                    case "getByCCCD" -> Optional.ofNullable(duplicatePatient);
                    case "add" -> {
                        patientAdds.incrementAndGet();
                        if (identityConflictOnInsert) throw new PatientIdentityConflictException();
                        BenhNhan patient = (BenhNhan) args[0];
                        patient.setId(patientInsertId);
                        patient.setMaBenhNhanFormat("SYNTH-PATIENT-" + patientInsertId);
                        yield null;
                    }
                    case "update" -> {
                        patientUpdates.incrementAndGet();
                        yield null;
                    }
                    default -> defaultValue(method);
                };
            });
            IBaoHiemRepository insuranceCards = proxy(IBaoHiemRepository.class, (method, args) -> {
                return switch (method) {
                    case "getLatestByPatientId" -> Optional.ofNullable(currentInsurance);
                    case "add" -> {
                        insuranceAdds.incrementAndGet();
                        if (insuranceFailure != null) throw insuranceFailure;
                        yield null;
                    }
                    case "update" -> {
                        insuranceUpdates.incrementAndGet();
                        if (insuranceFailure != null) throw insuranceFailure;
                        yield null;
                    }
                    default -> defaultValue(method);
                };
            });
            IPhongKhamRepository rooms = proxy(IPhongKhamRepository.class, (method, args) ->
                    method.equals("getById") ? Optional.ofNullable(room) : defaultValue(method));
            ReceptionQueuePort queue = proxy(ReceptionQueuePort.class, (method, args) -> {
                if (!method.equals("registerVisit")) return defaultValue(method);
                visitCalls.incrementAndGet();
                visitArguments.clear();
                visitArguments.addAll(List.of(args));
                if (queueFailure != null) throw queueFailure;
                return visit;
            });
            handler = new RegisterPatientHandler(patients, insuranceCards, rooms, queue, transactions);
        }
    }

    private static final class TransactionProbe implements TransactionBoundary {
        private int calls;
        private int failures;

        @Override
        public <T> Result<T> inTransaction(java.util.function.Supplier<Result<T>> operation) {
            calls++;
            Result<T> result = operation.get();
            if (result.isFailure()) failures++;
            return result;
        }
    }
}
