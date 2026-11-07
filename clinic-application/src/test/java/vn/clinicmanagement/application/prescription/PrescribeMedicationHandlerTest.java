package vn.clinicmanagement.application.prescription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.ClinicalDrugSafetyService;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.IKhoRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrescribeMedicationHandlerTest {
    private static final int VISIT_ID = 501;
    private static final int DOCTOR_ID = 20;
    private static final int FACILITY_ID = 10;
    private static final int PATIENT_ID = 700;
    private static final int DEPARTMENT_ID = 30;
    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final AuthorizationContext DOCTOR_CONTEXT =
            new AuthorizationContext(2, 20, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

    private final Map<Integer, Item> inventoryItems = new HashMap<>();
    private final List<SystemAuditEntry> transactionalAudits = new ArrayList<>();
    private final List<SystemAuditEntry> independentAudits = new ArrayList<>();
    private final AtomicInteger transactionCalls = new AtomicInteger();
    private LuotKham visit;
    private BenhNhan patient;
    private DonThuoc addedPrescription;
    private boolean assignIdOnAdd;
    private int visitReads;
    private PrescribeMedicationHandler handler;

    @BeforeEach
    void setUp() {
        visit = new LuotKham();
        visit.setId(VISIT_ID);
        visit.setMaBenhNhan(PATIENT_ID);
        visit.setMaBacSi(DOCTOR_ID);
        visit.setTrangThai(TrangThaiLuotKham.DANG_KHAM);
        patient = new BenhNhan();
        inventoryItems.clear();
        transactionalAudits.clear();
        independentAudits.clear();
        transactionCalls.set(0);
        visitReads = 0;
        addedPrescription = null;
        assignIdOnAdd = true;

        IDonThuocRepository prescriptionRepository = proxy(IDonThuocRepository.class, (method, args) -> {
            if ("add".equals(method)) {
                addedPrescription = (DonThuoc) args[0];
                if (assignIdOnAdd) {
                    addedPrescription.setId(9001);
                }
            }
            return null;
        });
        ILuotKhamRepository visitRepository = proxy(ILuotKhamRepository.class, (method, args) -> {
            if ("getById".equals(method)) {
                visitReads++;
                return visit != null && visit.getId() == (int) args[0]
                        ? Optional.of(visit)
                        : Optional.empty();
            }
            return null;
        });
        IBenhNhanRepository patientRepository = proxy(IBenhNhanRepository.class, (method, args) ->
                "getById".equals(method) && patient != null && patient.getId() != null
                        && patient.getId() == (int) args[0] ? Optional.of(patient) : Optional.empty());
        IKhoRepository inventoryRepository = proxy(IKhoRepository.class, (method, args) ->
                "getItemById".equals(method)
                        ? Optional.ofNullable(inventoryItems.get((int) args[0]))
                        : null);
        TransactionBoundary transactions = new TransactionBoundary() {
            @Override
            public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
                transactionCalls.incrementAndGet();
                return operation.get();
            }
        };
        AuditWritePort audit = new AuditWritePort() {
            @Override
            public void appendInCurrentTransaction(SystemAuditEntry entry) {
                transactionalAudits.add(entry);
            }

            @Override
            public void appendIndependently(SystemAuditEntry entry) {
                independentAudits.add(entry);
            }

            @Override
            public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {
            }

            @Override
            public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {
            }
        };
        handler = new PrescribeMedicationHandler(
                prescriptionRepository,
                visitRepository,
                patientRepository,
                inventoryRepository,
                new ClinicalDrugSafetyService(),
                transactions,
                audit);
    }

    @Test
    void createsPrescriptionWithMultipleItemsAndAuditsSuccess() {
        patient.setId(PATIENT_ID);
        Item firstItem = item(101, "Synthetic medication A", "active-a");
        Item secondItem = item(102, "Synthetic medication B", "active-b");
        inventoryItems.put(firstItem.getId(), firstItem);
        inventoryItems.put(secondItem.getId(), secondItem);
        PrescriptionItemInput first = itemInput(101, "2", "10.50");
        PrescriptionItemInput second = itemInput(102, "3", "4.25");

        Result<PrescribeMedicationReceipt> result =
                handler.handle(command(DOCTOR_CONTEXT, false, List.of(first, second)));

        assertTrue(result.isSuccess());
        PrescribeMedicationReceipt receipt = result.getValue().orElseThrow();
        assertEquals(9001, receipt.prescriptionId());
        assertEquals(VISIT_ID, receipt.visitId());
        assertEquals(2, receipt.itemCount());
        assertEquals(new BigDecimal("33.75"), receipt.totalAmount());
        assertEquals(TrangThaiDonThuoc.TAM, receipt.trangThai());
        assertNotNull(receipt.prescribedAt());

        assertNotNull(addedPrescription);
        assertEquals(VISIT_ID, addedPrescription.getMaLuotKham());
        assertEquals(DOCTOR_ID, addedPrescription.getMaBacSi());
        assertEquals(FACILITY_ID, addedPrescription.getMaCoSo());
        assertEquals(Integer.valueOf(4), addedPrescription.getMaKhoXuat());
        assertEquals("synthetic note", addedPrescription.getGhiChu());
        assertEquals("synthetic instructions", addedPrescription.getLoiDanDonThuoc());
        assertEquals(2, addedPrescription.getChiTietDonThuocs().size());
        ChiTietDonThuoc savedFirst = addedPrescription.getChiTietDonThuocs().get(0);
        assertEquals(101, savedFirst.getMaItem());
        assertEquals(new BigDecimal("2"), savedFirst.getSoLuongKe());
        assertEquals(new BigDecimal("10.50"), savedFirst.getDonGiaKe());
        assertEquals("morning", savedFirst.getLieuDung());
        assertEquals("oral", savedFirst.getDuongDung());
        assertEquals("twice daily", savedFirst.getTanSuat());
        assertEquals(5, savedFirst.getSoNgay());
        assertEquals(new BigDecimal("1"), savedFirst.getSang());
        assertEquals(new BigDecimal("0"), savedFirst.getTrua());
        assertEquals(new BigDecimal("1"), savedFirst.getChieu());
        assertEquals(new BigDecimal("0"), savedFirst.getToi());
        assertEquals("after meals", savedFirst.getHuongDan());
        assertSame(addedPrescription, savedFirst.getDonThuoc());
        assertSame(firstItem, savedFirst.getItem());
        assertEquals(1, transactionCalls.get());
        assertEquals(1, transactionalAudits.size());
        assertTrue(independentAudits.isEmpty());

        SystemAuditEntry audit = transactionalAudits.get(0);
        assertEquals(AuditActionCode.RX_CREATED, audit.actionCode());
        assertEquals(AuditResourceType.PRESCRIPTION, audit.resourceType());
        assertEquals(9001, audit.resourceId());
        assertEquals(AuditOutcome.SUCCESS, audit.outcome());
    }

    @Test
    void blocksAllergyWarningWithoutOverride() {
        patient.setId(PATIENT_ID);
        patient.setTienSuBenhs(List.of(history(LoaiTienSu.DI_UNG, "active-a")));
        inventoryItems.put(101, item(101, "Synthetic medication", "ACTIVE-A"));

        Result<PrescribeMedicationReceipt> result =
                handler.handle(command(DOCTOR_CONTEXT, false, List.of(itemInput(101, "1", "5"))));

        assertFailure(result, "ALLERGY_WARNING");
        assertTrue(result.getError().orElseThrow().message().contains("ACTIVE-A"));
        assertEquals(0, transactionCalls.get());
        assertNull(addedPrescription);
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void allowsAllergyOverrideAndStillAuditsPrescription() {
        patient.setId(PATIENT_ID);
        patient.setTienSuBenhs(List.of(history(LoaiTienSu.DI_UNG, "active-a")));
        inventoryItems.put(101, item(101, "Synthetic medication", "active-a"));

        Result<PrescribeMedicationReceipt> result =
                handler.handle(command(DOCTOR_CONTEXT, true, List.of(itemInput(101, "1", "5"))));

        assertTrue(result.isSuccess());
        assertEquals(1, transactionCalls.get());
        assertEquals(AuditActionCode.RX_CREATED, transactionalAudits.get(0).actionCode());
    }

    @Test
    void rejectsMissingVisit() {
        visit = null;

        Result<PrescribeMedicationReceipt> result =
                handler.handle(command(DOCTOR_CONTEXT, false, List.of(itemInput(101, "1", "5"))));

        assertFailure(result, "NOT_FOUND");
        assertEquals("Không tìm thấy lượt khám.", result.getError().orElseThrow().message());
        assertEquals(1, visitReads);
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void rejectsNullOrEmptyItemList() {
        Result<PrescribeMedicationReceipt> empty =
                handler.handle(command(DOCTOR_CONTEXT, false, List.of()));
        Result<PrescribeMedicationReceipt> missing =
                handler.handle(command(DOCTOR_CONTEXT, false, null));

        assertFailure(empty, "VALIDATION_ERROR");
        assertFailure(missing, "VALIDATION_ERROR");
        assertEquals("Đơn thuốc phải có ít nhất một loại thuốc.",
                empty.getError().orElseThrow().message());
        assertEquals(0, visitReads);
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void rejectsInvalidItemIdentifierOrNonpositiveQuantity() {
        Result<PrescribeMedicationReceipt> invalidId =
                handler.handle(command(DOCTOR_CONTEXT, false, List.of(itemInput(0, "1", "5"))));
        Result<PrescribeMedicationReceipt> zeroQuantity =
                handler.handle(command(DOCTOR_CONTEXT, false, List.of(itemInput(101, "0", "5"))));
        Result<PrescribeMedicationReceipt> negativeQuantity =
                handler.handle(command(DOCTOR_CONTEXT, false, List.of(itemInput(101, "-1", "5"))));

        assertFailure(invalidId, "VALIDATION_ERROR");
        assertFailure(zeroQuantity, "VALIDATION_ERROR");
        assertFailure(negativeQuantity, "VALIDATION_ERROR");
        assertEquals("Chi tiết thuốc kê đơn không hợp lệ.",
                zeroQuantity.getError().orElseThrow().message());
        assertEquals(0, visitReads);
    }

    @Test
    void rejectsDoctorMismatch() {
        visit.setMaBacSi(DOCTOR_ID + 1);

        Result<PrescribeMedicationReceipt> result =
                handler.handle(command(DOCTOR_CONTEXT, false, List.of(itemInput(101, "1", "5"))));

        assertFailure(result, "DOCTOR_MISMATCH");
        assertEquals(0, transactionCalls.get());
        assertNull(addedPrescription);
    }

    @Test
    void rejectsLockedEmr() {
        visit.setEmrLocked(true);

        Result<PrescribeMedicationReceipt> result =
                handler.handle(command(DOCTOR_CONTEXT, false, List.of(itemInput(101, "1", "5"))));

        assertFailure(result, "EMR_LOCKED");
        assertEquals(0, transactionCalls.get());
        assertNull(addedPrescription);
    }

    @Test
    void rejectsVisitNotInProgress() {
        visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);

        Result<PrescribeMedicationReceipt> result =
                handler.handle(command(DOCTOR_CONTEXT, false, List.of(itemInput(101, "1", "5"))));

        assertFailure(result, "INVALID_STATE");
        assertEquals(0, transactionCalls.get());
        assertNull(addedPrescription);
    }

    @Test
    void deniesReceptionistAndOtherRoleWithForbiddenAudit() {
        AuthorizationContext receptionist =
                new AuthorizationContext(1, 10, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
        AuthorizationContext otherRole =
                new AuthorizationContext(3, 11, "PHARMACIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

        Result<PrescribeMedicationReceipt> receptionistResult =
                handler.handle(command(receptionist, false, List.of(itemInput(101, "1", "5"))));
        Result<PrescribeMedicationReceipt> otherRoleResult =
                handler.handle(command(otherRole, false, List.of(itemInput(101, "1", "5"))));

        assertFailure(receptionistResult, "AUTH_ACCESS_DENIED");
        assertFailure(otherRoleResult, "AUTH_ACCESS_DENIED");
        assertEquals(2, independentAudits.size());
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitReads);
        assertTrue(transactionalAudits.isEmpty());
        for (SystemAuditEntry entry : independentAudits) {
            assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, entry.actionCode());
            assertEquals(AuditOutcome.DENIED, entry.outcome());
            assertEquals(AuditReasonCode.AUTH_FORBIDDEN, entry.reasonCode());
            assertNull(entry.resourceType());
            assertNull(entry.resourceId());
        }
    }

    @Test
    void assignsGeneratedIdAndUsesItInAuditWhenRepositoryDoesNotAssignOne() {
        patient.setId(PATIENT_ID);
        inventoryItems.put(101, item(101, "Synthetic medication", "active-a"));
        assignIdOnAdd = false;

        Result<PrescribeMedicationReceipt> result =
                handler.handle(command(DOCTOR_CONTEXT, false, List.of(itemInput(101, "1", "5"))));

        assertTrue(result.isSuccess());
        int generatedId = result.getValue().orElseThrow().prescriptionId();
        assertTrue(generatedId > 0);
        assertEquals(generatedId, addedPrescription.getId());
        assertEquals(generatedId, transactionalAudits.get(0).resourceId());
    }

    private static PrescribeMedicationCommand command(
            AuthorizationContext context, boolean override, List<PrescriptionItemInput> items) {
        return new PrescribeMedicationCommand(
                VISIT_ID,
                DOCTOR_ID,
                FACILITY_ID,
                4,
                "synthetic note",
                "synthetic instructions",
                override,
                items,
                context,
                actor(context));
    }

    private static AuditActorContext actor(AuthorizationContext context) {
        return context != null && context.isValid()
                ? new AuditActorContext(context.employeeId(), context.roleId(), context.roleCode(),
                        context.facilityId(), context.departmentId(), "WS-SYN-01", CORRELATION_ID)
                : null;
    }

    private static PrescriptionItemInput itemInput(int itemId, String quantity, String unitPrice) {
        return new PrescriptionItemInput(
                itemId,
                new BigDecimal(quantity),
                new BigDecimal(unitPrice),
                "morning",
                "oral",
                "twice daily",
                5,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                "after meals");
    }

    private static Item item(int id, String name, String activeSubstance) {
        Item item = new Item();
        item.setId(id);
        item.setTenItem(name);
        item.setHoatChat(activeSubstance);
        return item;
    }

    private static TienSuBenh history(LoaiTienSu type, String description) {
        TienSuBenh history = new TienSuBenh();
        history.setLoaiTienSu(type);
        history.setMoTa(description);
        return history;
    }

    private static void assertFailure(Result<PrescribeMedicationReceipt> result, String code) {
        assertFalse(result.isSuccess());
        assertEquals(code, result.getErrorCode().orElse(null));
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        Object instance = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + " synthetic test proxy";
                    default -> null;
                };
            }
            return invocation.invoke(method.getName(), args);
        });
        return type.cast(instance);
    }
}
