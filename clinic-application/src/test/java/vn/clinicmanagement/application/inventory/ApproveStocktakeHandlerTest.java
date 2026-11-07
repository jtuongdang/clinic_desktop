package vn.clinicmanagement.application.inventory;

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
import vn.clinicmanagement.domain.common.enums.TrangThaiKiemKe;
import vn.clinicmanagement.domain.inventory.Stocktake;
import vn.clinicmanagement.domain.inventory.StocktakeCount;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

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

class ApproveStocktakeHandlerTest {
    private static final int STOCKTAKE_ID = 401;
    private static final int APPROVER_ID = 10;
    private static final int FACILITY_ID = 1;
    private static final int DEPARTMENT_ID = 5;
    private static final int BATCH_ID_1 = 101;
    private static final int BATCH_ID_2 = 102;
    private static final String WORKSTATION_ID = "WS-TEST-01";
    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final AuthorizationContext ADMIN_CONTEXT =
            new AuthorizationContext(APPROVER_ID, 1, "ADMIN", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    private static final AuthorizationContext PHARMACIST_CONTEXT =
            new AuthorizationContext(APPROVER_ID, 2, "PHARMACIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    private static final AuthorizationContext DOCTOR_CONTEXT =
            new AuthorizationContext(APPROVER_ID, 3, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

    private final List<SystemAuditEntry> independentAudits = new ArrayList<>();
    private final List<SystemAuditEntry> transactionalAudits = new ArrayList<>();
    private final AtomicInteger transactionCalls = new AtomicInteger();
    private final Map<Integer, TonKho> batches = new HashMap<>();
    private final List<TonKho> savedBatches = new ArrayList<>();

    private Stocktake stocktake;
    private Stocktake savedStocktake;
    private ApproveStocktakeHandler handler;

    @BeforeEach
    void setUp() {
        independentAudits.clear();
        transactionalAudits.clear();
        transactionCalls.set(0);
        batches.clear();
        savedBatches.clear();
        savedStocktake = null;

        TonKho batch1 = new TonKho();
        batch1.setId(BATCH_ID_1);
        batch1.setMaKho(1);
        batch1.setMaItem(201);
        batch1.setSoLo("BATCH-A");
        batch1.setSoLuongTon(new BigDecimal("100.00"));
        batches.put(BATCH_ID_1, batch1);

        TonKho batch2 = new TonKho();
        batch2.setId(BATCH_ID_2);
        batch2.setMaKho(1);
        batch2.setMaItem(202);
        batch2.setSoLo("BATCH-B");
        batch2.setSoLuongTon(new BigDecimal("50.00"));
        batches.put(BATCH_ID_2, batch2);

        stocktake = new Stocktake();
        stocktake.setId(STOCKTAKE_ID);
        stocktake.setMaKho(1);
        stocktake.setTrangThai(TrangThaiKiemKe.DA_NOP);

        StocktakeCount count1 = new StocktakeCount();
        count1.setId(1);
        count1.setMaStocktake(STOCKTAKE_ID);
        count1.setMaTonKho(BATCH_ID_1);
        count1.setSoLuongSoSach(new BigDecimal("100.00"));
        count1.setSoLuongThucTe(new BigDecimal("95.00"));
        count1.setChenhLech(new BigDecimal("-5.00"));

        StocktakeCount count2 = new StocktakeCount();
        count2.setId(2);
        count2.setMaStocktake(STOCKTAKE_ID);
        count2.setMaTonKho(BATCH_ID_2);
        count2.setSoLuongSoSach(new BigDecimal("50.00"));
        count2.setSoLuongThucTe(new BigDecimal("52.00"));
        count2.setChenhLech(new BigDecimal("2.00"));

        stocktake.setCounts(new ArrayList<>(List.of(count1, count2)));

        IKhoRepository repository = proxy(IKhoRepository.class, (method, args) -> switch (method) {
            case "getStocktakeById" -> stocktake != null && stocktake.getId() == (int) args[0]
                    ? Optional.of(stocktake)
                    : Optional.empty();
            case "saveStocktake" -> {
                savedStocktake = (Stocktake) args[0];
                yield savedStocktake;
            }
            case "getBatchById" -> Optional.ofNullable(batches.get((int) args[0]));
            case "saveTonKho" -> {
                TonKho t = (TonKho) args[0];
                savedBatches.add(t);
                yield t;
            }
            default -> null;
        });

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

        handler = new ApproveStocktakeHandler(repository, transactions, audit);
    }

    @Test
    void approvesStocktakeByAdminAndAdjustsBatchBalances() {
        ApproveStocktakeCommand command = command(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        Result<ApproveStocktakeReceipt> result = handler.handle(command);

        assertTrue(result.isSuccess());
        ApproveStocktakeReceipt receipt = result.getValue().orElseThrow();
        assertEquals(STOCKTAKE_ID, receipt.stocktakeId());
        assertEquals(2, receipt.totalBatchesAdjusted());
        assertEquals(2, receipt.adjustments().size());

        StocktakeAdjustmentLineDto line1 = receipt.adjustments().get(0);
        assertEquals(BATCH_ID_1, line1.tonKhoId());
        assertEquals(new BigDecimal("100.00"), line1.qtyBefore());
        assertEquals(new BigDecimal("95.00"), line1.qtyAfter());
        assertEquals(new BigDecimal("-5.00"), line1.chenhLech());

        StocktakeAdjustmentLineDto line2 = receipt.adjustments().get(1);
        assertEquals(BATCH_ID_2, line2.tonKhoId());
        assertEquals(new BigDecimal("50.00"), line2.qtyBefore());
        assertEquals(new BigDecimal("52.00"), line2.qtyAfter());
        assertEquals(new BigDecimal("2.00"), line2.chenhLech());

        assertEquals(TrangThaiKiemKe.DA_DUYET, stocktake.getTrangThai());
        assertSame(stocktake, savedStocktake);
        assertEquals(2, savedBatches.size());
        assertEquals(new BigDecimal("95.00"), batches.get(BATCH_ID_1).getSoLuongTon());
        assertEquals(new BigDecimal("52.00"), batches.get(BATCH_ID_2).getSoLuongTon());

        assertEquals(1, transactionCalls.get());
        assertEquals(1, independentAudits.size());
        assertSuccessAudit(independentAudits.get(0), ADMIN_CONTEXT);
    }

    @Test
    void approvesStocktakeByPharmacistWhenStateIsMoiTao() {
        stocktake.setTrangThai(TrangThaiKiemKe.MOI_TAO);
        ApproveStocktakeCommand command = command(STOCKTAKE_ID, APPROVER_ID, PHARMACIST_CONTEXT);

        Result<ApproveStocktakeReceipt> result = handler.handle(command);

        assertTrue(result.isSuccess());
        assertEquals(TrangThaiKiemKe.DA_DUYET, stocktake.getTrangThai());
        assertEquals(2, result.getValue().orElseThrow().totalBatchesAdjusted());
        assertEquals(1, transactionCalls.get());
        assertEquals(1, independentAudits.size());
        assertSuccessAudit(independentAudits.get(0), PHARMACIST_CONTEXT);
    }

    @Test
    void rejectsValidationErrorsForInvalidInputs() {
        Result<ApproveStocktakeReceipt> nullCmd = handler.handle(null);
        Result<ApproveStocktakeReceipt> invalidStocktakeId =
                handler.handle(command(0, APPROVER_ID, ADMIN_CONTEXT));
        Result<ApproveStocktakeReceipt> negativeStocktakeId =
                handler.handle(command(-5, APPROVER_ID, ADMIN_CONTEXT));
        Result<ApproveStocktakeReceipt> invalidApproverId =
                handler.handle(command(STOCKTAKE_ID, 0, ADMIN_CONTEXT));
        Result<ApproveStocktakeReceipt> negativeApproverId =
                handler.handle(command(STOCKTAKE_ID, -1, ADMIN_CONTEXT));

        assertFailure(nullCmd, "VALIDATION_ERROR", "Thông tin phê duyệt kiểm kê không hợp lệ.");
        assertFailure(invalidStocktakeId, "VALIDATION_ERROR", "Thông tin phê duyệt kiểm kê không hợp lệ.");
        assertFailure(negativeStocktakeId, "VALIDATION_ERROR", "Thông tin phê duyệt kiểm kê không hợp lệ.");
        assertFailure(invalidApproverId, "VALIDATION_ERROR", "Thông tin phê duyệt kiểm kê không hợp lệ.");
        assertFailure(negativeApproverId, "VALIDATION_ERROR", "Thông tin phê duyệt kiểm kê không hợp lệ.");

        assertEquals(0, transactionCalls.get());
        assertTrue(independentAudits.isEmpty());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsWhenStocktakeNotFound() {
        stocktake = null;
        ApproveStocktakeCommand command = command(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        Result<ApproveStocktakeReceipt> result = handler.handle(command);

        assertFailure(result, "NOT_FOUND", "Không tìm thấy phiếu kiểm kê.");
        assertEquals(0, transactionCalls.get());
        assertTrue(independentAudits.isEmpty());
    }

    @Test
    void rejectsWhenStocktakeAlreadyApproved() {
        stocktake.setTrangThai(TrangThaiKiemKe.DA_DUYET);
        ApproveStocktakeCommand command = command(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        Result<ApproveStocktakeReceipt> result = handler.handle(command);

        assertFailure(result, "INVALID_STATE", "Phiếu kiểm kê đã được duyệt hoặc bị hủy trước đó.");
        assertEquals(0, transactionCalls.get());
        assertTrue(independentAudits.isEmpty());
    }

    @Test
    void rejectsWhenStocktakeAlreadyCancelled() {
        stocktake.setTrangThai(TrangThaiKiemKe.DA_HUY);
        ApproveStocktakeCommand command = command(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        Result<ApproveStocktakeReceipt> result = handler.handle(command);

        assertFailure(result, "INVALID_STATE", "Phiếu kiểm kê đã được duyệt hoặc bị hủy trước đó.");
        assertEquals(0, transactionCalls.get());
        assertTrue(independentAudits.isEmpty());
    }

    @Test
    void deniesUnauthorizedRoleAndLogsForbiddenAudit() {
        ApproveStocktakeCommand command = command(STOCKTAKE_ID, APPROVER_ID, DOCTOR_CONTEXT);

        Result<ApproveStocktakeReceipt> result = handler.handle(command);

        assertFailure(result, "AUTH_ACCESS_DENIED", "Không có quyền phê duyệt kiểm kê kho.");
        assertEquals(0, transactionCalls.get());
        assertEquals(1, independentAudits.size());

        SystemAuditEntry auditEntry = independentAudits.get(0);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, auditEntry.actionCode());
        assertEquals(AuditOutcome.DENIED, auditEntry.outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, auditEntry.reasonCode());
        assertEquals(APPROVER_ID, auditEntry.actorEmployeeId());
        assertEquals("DOCTOR", auditEntry.roleCode());
        assertNull(auditEntry.resourceType());
        assertNull(auditEntry.resourceId());
    }

    @Test
    void deniesWhenAuthorizationContextIsMissingOrInvalid() {
        ApproveStocktakeCommand nullContext =
                new ApproveStocktakeCommand(STOCKTAKE_ID, APPROVER_ID, null, null);
        AuthorizationContext invalidContext =
                new AuthorizationContext(0, 0, "", 0, 0, null);
        ApproveStocktakeCommand invalidCmd =
                new ApproveStocktakeCommand(STOCKTAKE_ID, APPROVER_ID, invalidContext, null);

        Result<ApproveStocktakeReceipt> nullResult = handler.handle(nullContext);
        Result<ApproveStocktakeReceipt> invalidResult = handler.handle(invalidCmd);

        assertFailure(nullResult, "AUTH_ACCESS_DENIED", "Không có quyền phê duyệt kiểm kê kho.");
        assertFailure(invalidResult, "AUTH_ACCESS_DENIED", "Không có quyền phê duyệt kiểm kê kho.");
        assertEquals(0, transactionCalls.get());
        assertEquals(2, independentAudits.size());
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, independentAudits.get(0).actionCode());
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, independentAudits.get(1).actionCode());
        assertEquals(AuditOutcome.DENIED, independentAudits.get(0).outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, independentAudits.get(0).reasonCode());
        assertNull(independentAudits.get(0).actorEmployeeId());
    }

    @Test
    void auditLoggingVerificationContainsAllRequiredMetadata() {
        ApproveStocktakeCommand command = command(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        Result<ApproveStocktakeReceipt> result = handler.handle(command);

        assertTrue(result.isSuccess());
        assertEquals(1, independentAudits.size());
        SystemAuditEntry entry = independentAudits.get(0);

        assertEquals(APPROVER_ID, entry.actorEmployeeId());
        assertEquals(1, entry.roleId());
        assertEquals("ADMIN", entry.roleCode());
        assertEquals(FACILITY_ID, entry.facilityId());
        assertEquals(DEPARTMENT_ID, entry.departmentId());
        assertEquals(AuditActionCode.INV_STOCKTAKE_APPROVED, entry.actionCode());
        assertEquals(AuditResourceType.STOCKTAKE, entry.resourceType());
        assertEquals(STOCKTAKE_ID, entry.resourceId());
        assertEquals(AuditOutcome.SUCCESS, entry.outcome());
        assertNull(entry.reasonCode());
        assertNotNull(entry.occurredAtUtc());
        assertEquals(WORKSTATION_ID, entry.workstationId());
        assertEquals(CORRELATION_ID, entry.correlationId());
    }

    @Test
    void skipsMissingBatchDuringAdjustment() {
        batches.remove(BATCH_ID_2);
        ApproveStocktakeCommand command = command(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        Result<ApproveStocktakeReceipt> result = handler.handle(command);

        assertTrue(result.isSuccess());
        ApproveStocktakeReceipt receipt = result.getValue().orElseThrow();
        assertEquals(1, receipt.totalBatchesAdjusted());
        assertEquals(1, receipt.adjustments().size());
        assertEquals(BATCH_ID_1, receipt.adjustments().get(0).tonKhoId());
        assertEquals(1, savedBatches.size());
    }

    @Test
    void convenienceConstructorDerivesAuditActorContext() {
        ApproveStocktakeCommand command =
                new ApproveStocktakeCommand(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        assertNotNull(command.auditActorContext());
        assertEquals(APPROVER_ID, command.auditActorContext().employeeId());
        assertEquals(1, command.auditActorContext().roleId());
        assertEquals("ADMIN", command.auditActorContext().roleCode());
        assertEquals(FACILITY_ID, command.auditActorContext().facilityId());
        assertEquals(DEPARTMENT_ID, command.auditActorContext().departmentId());
        assertNotNull(command.auditActorContext().correlationId());
        assertNull(command.auditActorContext().workstationId());

        ApproveStocktakeCommand nullContextCmd =
                new ApproveStocktakeCommand(STOCKTAKE_ID, APPROVER_ID, null);
        assertNull(nullContextCmd.auditActorContext());
    }

    private static ApproveStocktakeCommand command(
            int stocktakeId, int approverId, AuthorizationContext context) {
        return new ApproveStocktakeCommand(
                stocktakeId,
                approverId,
                context,
                new AuditActorContext(
                        context.employeeId(),
                        context.roleId(),
                        context.roleCode(),
                        context.facilityId(),
                        context.departmentId(),
                        WORKSTATION_ID,
                        CORRELATION_ID));
    }

    private static void assertSuccessAudit(SystemAuditEntry audit, AuthorizationContext context) {
        assertEquals(context.employeeId(), audit.actorEmployeeId());
        assertEquals(context.roleId(), audit.roleId());
        assertEquals(context.roleCode(), audit.roleCode());
        assertEquals(AuditActionCode.INV_STOCKTAKE_APPROVED, audit.actionCode());
        assertEquals(AuditResourceType.STOCKTAKE, audit.resourceType());
        assertEquals(STOCKTAKE_ID, audit.resourceId());
        assertEquals(AuditOutcome.SUCCESS, audit.outcome());
        assertNull(audit.reasonCode());
    }

    private static void assertFailure(Result<ApproveStocktakeReceipt> result, String code, String message) {
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertEquals(code, result.getErrorCode().orElse(null));
        assertEquals(message, result.getError().map(Result.ApplicationError::message).orElse(null));
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
                    case "toString" -> type.getSimpleName() + " synthetic proxy";
                    default -> null;
                };
            }
            return invocation.invoke(method.getName(), args);
        });
        return type.cast(instance);
    }
}
