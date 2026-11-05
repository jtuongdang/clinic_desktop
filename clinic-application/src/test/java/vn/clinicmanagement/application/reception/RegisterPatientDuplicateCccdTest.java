package vn.clinicmanagement.application.reception;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterPatientDuplicateCccdTest {
    @Test
    void duplicateDuringPatientInsertReturnsGuidanceAndRollsBackBeforeInsuranceOrVisit() {
        AtomicInteger insuranceCalls = new AtomicInteger();
        AtomicInteger visitCalls = new AtomicInteger();
        AtomicInteger patientInsertCalls = new AtomicInteger();

        IBenhNhanRepository patients = proxy(IBenhNhanRepository.class, (method, args) -> {
            if (method.equals("getByCCCD")) return Optional.empty();
            if (method.equals("add")) {
                patientInsertCalls.incrementAndGet();
                throw new PatientIdentityConflictException();
            }
            return defaultValue(method);
        });
        IBaoHiemRepository insurance = proxy(IBaoHiemRepository.class, (method, args) -> {
            insuranceCalls.incrementAndGet();
            return defaultValue(method);
        });
        IPhongKhamRepository rooms = proxy(IPhongKhamRepository.class, (method, args) -> {
            if (method.equals("getById")) return Optional.of(activeRoom());
            return defaultValue(method);
        });
        ReceptionQueuePort queue = proxy(ReceptionQueuePort.class, (method, args) -> {
            if (method.equals("registerVisit")) visitCalls.incrementAndGet();
            return method.equals("getQueue") || method.equals("getRoomSummaries") ? List.of() : defaultValue(method);
        });
        AtomicInteger rollbackCalls = new AtomicInteger();
        TransactionBoundary transactions = new TransactionBoundary() {
            @Override
            public <T> Result<T> inTransaction(java.util.function.Supplier<Result<T>> operation) {
                Result<T> result = operation.get();
                if (result.isFailure()) rollbackCalls.incrementAndGet();
                return result;
            }
        };
        RegisterPatientHandler handler = new RegisterPatientHandler(patients, insurance, rooms, queue, transactions,
                new AuditWritePort() {
                    @Override public void appendInCurrentTransaction(vn.clinicmanagement.application.audit.SystemAuditEntry entry) {}
                    @Override public void appendIndependently(vn.clinicmanagement.application.audit.SystemAuditEntry entry) {}
                    @Override public void appendMedicalRecordInCurrentTransaction(
                            vn.clinicmanagement.application.audit.MedicalRecordAuditEntry entry) {}
                    @Override public void appendMedicalRecordIndependently(
                            vn.clinicmanagement.application.audit.MedicalRecordAuditEntry entry) {}
                });

        BenhNhan patient = new BenhNhan();
        patient.setHo("Nguyen");
        patient.setTen("Synthetic");
        patient.setNgaySinh(LocalDate.of(1990, 1, 1));
        patient.setSoCCCD("synthetic-cccd");
        BaoHiem card = new BaoHiem();
        card.setSoBaoHiem("synthetic-insurance");
        card.setNgayHetHan(LocalDate.of(2030, 1, 1));
        RegisterPatientCommand command = new RegisterPatientCommand(patient, card, 3,
                "Khám bệnh", "Synthetic test", DoiTuongKham.BHYT, false, 1,
                new AuditActorContext(1, 31, "RECEPTION", 1, 2, null,
                        UUID.fromString("123e4567-e89b-12d3-a456-426614174000")));

        Result<RegistrationReceipt> result = handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("PATIENT_DUPLICATE_CCCD", result.getError().orElseThrow().code());
        assertTrue(result.getError().orElseThrow().message().contains("tìm người bệnh"));
        assertEquals(1, patientInsertCalls.get());
        assertEquals(1, rollbackCalls.get());
        assertEquals(0, insuranceCalls.get());
        assertEquals(0, visitCalls.get());
        assertFalse(result.getError().orElseThrow().message().contains("synthetic-cccd"));
    }

    private static PhongKham activeRoom() {
        CoSo facility = new CoSo();
        facility.setId(1);
        Khoa department = new Khoa();
        department.setId(2);
        department.setCoSo(facility);
        PhongKham room = new PhongKham();
        room.setId(3);
        room.setMaKhoa(2);
        room.setKhoa(department);
        return room;
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
            default -> null;
        };
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args);
    }
}
