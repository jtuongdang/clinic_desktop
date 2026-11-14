package vn.clinicmanagement.application.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.common.enums.TrangThaiKiemKe;
import vn.clinicmanagement.domain.inventory.KiemKe;
import vn.clinicmanagement.domain.inventory.KiemKeDem;
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

class ApproveKiemKeHandlerTest {
    private static final int STOCKTAKE_ID = 401;
    private static final int APPROVER_ID = 10;
    private static final int FACILITY_ID = 1;
    private static final int DEPARTMENT_ID = 5;
    private static final int BATCH_ID_1 = 101;
    private static final int BATCH_ID_2 = 102;
    private static final String WORKSTATION_ID = "WS-TEST-01";
    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final PhanQuyenNguCanh ADMIN_CONTEXT =
            new PhanQuyenNguCanh(APPROVER_ID, 1, "ADMIN", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    private static final PhanQuyenNguCanh PHARMACIST_CONTEXT =
            new PhanQuyenNguCanh(APPROVER_ID, 2, "PHARMACIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    private static final PhanQuyenNguCanh DOCTOR_CONTEXT =
            new PhanQuyenNguCanh(APPROVER_ID, 3, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

    private final List<HeThongKiemToanNhapLieu> independentAudits = new ArrayList<>();
    private final List<HeThongKiemToanNhapLieu> transactionalAudits = new ArrayList<>();
    private final AtomicInteger transactionCalls = new AtomicInteger();
    private final Map<Integer, TonKho> batches = new HashMap<>();
    private final List<TonKho> savedBatches = new ArrayList<>();

    private KiemKe stocktake;
    private KiemKe savedStocktake;
    private ApproveKiemKeHandler handler;

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

        stocktake = new KiemKe();
        stocktake.setId(STOCKTAKE_ID);
        stocktake.setMaKho(1);
        stocktake.setTrangThai(TrangThaiKiemKe.DA_NOP);

        KiemKeDem count1 = new KiemKeDem();
        count1.setId(1);
        count1.setMaStocktake(STOCKTAKE_ID);
        count1.setMaTonKho(BATCH_ID_1);
        count1.setSoLuongSoSach(new BigDecimal("100.00"));
        count1.setSoLuongThucTe(new BigDecimal("95.00"));
        count1.setChenhLech(new BigDecimal("-5.00"));

        KiemKeDem count2 = new KiemKeDem();
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
                savedStocktake = (KiemKe) args[0];
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

        GiaoDichRanhGioi transactions = new GiaoDichRanhGioi() {
            @Override
            public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation) {
                transactionCalls.incrementAndGet();
                return operation.get();
            }
        };

        KiemToanWriteCongKetNoi audit = new KiemToanWriteCongKetNoi() {
            @Override
            public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {
                transactionalAudits.add(entry);
            }

            @Override
            public void appendIndependently(HeThongKiemToanNhapLieu entry) {
                independentAudits.add(entry);
            }

            @Override
            public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {
            }

            @Override
            public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {
            }
        };

        handler = new ApproveKiemKeHandler(repository, transactions, audit);
    }

    @Test
    void approvesStocktakeByAdminAndAdjustsBatchBalances() {
        ApproveKiemKeCommand command = command(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        KetQua<ApproveKiemKeBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess());
        ApproveKiemKeBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(STOCKTAKE_ID, receipt.stocktakeId());
        assertEquals(2, receipt.totalBatchesAdjusted());
        assertEquals(2, receipt.adjustments().size());

        KiemKeDieuChinhDongDuLieu line1 = receipt.adjustments().get(0);
        assertEquals(BATCH_ID_1, line1.tonKhoId());
        assertEquals(new BigDecimal("100.00"), line1.qtyBefore());
        assertEquals(new BigDecimal("95.00"), line1.qtyAfter());
        assertEquals(new BigDecimal("-5.00"), line1.chenhLech());

        KiemKeDieuChinhDongDuLieu line2 = receipt.adjustments().get(1);
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
        ApproveKiemKeCommand command = command(STOCKTAKE_ID, APPROVER_ID, PHARMACIST_CONTEXT);

        KetQua<ApproveKiemKeBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess());
        assertEquals(TrangThaiKiemKe.DA_DUYET, stocktake.getTrangThai());
        assertEquals(2, result.getValue().orElseThrow().totalBatchesAdjusted());
        assertEquals(1, transactionCalls.get());
        assertEquals(1, independentAudits.size());
        assertSuccessAudit(independentAudits.get(0), PHARMACIST_CONTEXT);
    }

    @Test
    void rejectsValidationErrorsForInvalidInputs() {
        KetQua<ApproveKiemKeBienNhan> nullCmd = handler.handle(null);
        KetQua<ApproveKiemKeBienNhan> invalidStocktakeId =
                handler.handle(command(0, APPROVER_ID, ADMIN_CONTEXT));
        KetQua<ApproveKiemKeBienNhan> negativeStocktakeId =
                handler.handle(command(-5, APPROVER_ID, ADMIN_CONTEXT));
        KetQua<ApproveKiemKeBienNhan> invalidApproverId =
                handler.handle(command(STOCKTAKE_ID, 0, ADMIN_CONTEXT));
        KetQua<ApproveKiemKeBienNhan> negativeApproverId =
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
        ApproveKiemKeCommand command = command(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        KetQua<ApproveKiemKeBienNhan> result = handler.handle(command);

        assertFailure(result, "NOT_FOUND", "Không tìm thấy phiếu kiểm kê.");
        assertEquals(0, transactionCalls.get());
        assertTrue(independentAudits.isEmpty());
    }

    @Test
    void rejectsWhenStocktakeAlreadyApproved() {
        stocktake.setTrangThai(TrangThaiKiemKe.DA_DUYET);
        ApproveKiemKeCommand command = command(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        KetQua<ApproveKiemKeBienNhan> result = handler.handle(command);

        assertFailure(result, "INVALID_STATE", "Phiếu kiểm kê đã được duyệt hoặc bị hủy trước đó.");
        assertEquals(0, transactionCalls.get());
        assertTrue(independentAudits.isEmpty());
    }

    @Test
    void rejectsWhenStocktakeAlreadyCancelled() {
        stocktake.setTrangThai(TrangThaiKiemKe.DA_HUY);
        ApproveKiemKeCommand command = command(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        KetQua<ApproveKiemKeBienNhan> result = handler.handle(command);

        assertFailure(result, "INVALID_STATE", "Phiếu kiểm kê đã được duyệt hoặc bị hủy trước đó.");
        assertEquals(0, transactionCalls.get());
        assertTrue(independentAudits.isEmpty());
    }

    @Test
    void deniesUnauthorizedRoleAndLogsForbiddenAudit() {
        ApproveKiemKeCommand command = command(STOCKTAKE_ID, APPROVER_ID, DOCTOR_CONTEXT);

        KetQua<ApproveKiemKeBienNhan> result = handler.handle(command);

        assertFailure(result, "AUTH_ACCESS_DENIED", "Không có quyền phê duyệt kiểm kê kho.");
        assertEquals(0, transactionCalls.get());
        assertEquals(1, independentAudits.size());

        HeThongKiemToanNhapLieu auditEntry = independentAudits.get(0);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, auditEntry.actionCode());
        assertEquals(KiemToanKetQua.DENIED, auditEntry.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, auditEntry.reasonCode());
        assertEquals(APPROVER_ID, auditEntry.actorEmployeeId());
        assertEquals("DOCTOR", auditEntry.roleCode());
        assertNull(auditEntry.resourceType());
        assertNull(auditEntry.resourceId());
    }

    @Test
    void deniesWhenAuthorizationContextIsMissingOrInvalid() {
        ApproveKiemKeCommand nullContext =
                new ApproveKiemKeCommand(STOCKTAKE_ID, APPROVER_ID, null, null);
        PhanQuyenNguCanh invalidContext =
                new PhanQuyenNguCanh(0, 0, "", 0, 0, null);
        ApproveKiemKeCommand invalidCmd =
                new ApproveKiemKeCommand(STOCKTAKE_ID, APPROVER_ID, invalidContext, null);

        KetQua<ApproveKiemKeBienNhan> nullResult = handler.handle(nullContext);
        KetQua<ApproveKiemKeBienNhan> invalidResult = handler.handle(invalidCmd);

        assertFailure(nullResult, "AUTH_ACCESS_DENIED", "Không có quyền phê duyệt kiểm kê kho.");
        assertFailure(invalidResult, "AUTH_ACCESS_DENIED", "Không có quyền phê duyệt kiểm kê kho.");
        assertEquals(0, transactionCalls.get());
        assertEquals(2, independentAudits.size());
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, independentAudits.get(0).actionCode());
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, independentAudits.get(1).actionCode());
        assertEquals(KiemToanKetQua.DENIED, independentAudits.get(0).outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, independentAudits.get(0).reasonCode());
        assertNull(independentAudits.get(0).actorEmployeeId());
    }

    @Test
    void auditLoggingVerificationContainsAllRequiredMetadata() {
        ApproveKiemKeCommand command = command(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        KetQua<ApproveKiemKeBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess());
        assertEquals(1, independentAudits.size());
        HeThongKiemToanNhapLieu entry = independentAudits.get(0);

        assertEquals(APPROVER_ID, entry.actorEmployeeId());
        assertEquals(1, entry.roleId());
        assertEquals("ADMIN", entry.roleCode());
        assertEquals(FACILITY_ID, entry.facilityId());
        assertEquals(DEPARTMENT_ID, entry.departmentId());
        assertEquals(KiemToanHanhDongMa.INV_STOCKTAKE_APPROVED, entry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.STOCKTAKE, entry.resourceType());
        assertEquals(STOCKTAKE_ID, entry.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, entry.outcome());
        assertNull(entry.reasonCode());
        assertNotNull(entry.occurredAtUtc());
        assertEquals(WORKSTATION_ID, entry.workstationId());
        assertEquals(CORRELATION_ID, entry.correlationId());
    }

    @Test
    void skipsMissingBatchDuringAdjustment() {
        batches.remove(BATCH_ID_2);
        ApproveKiemKeCommand command = command(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        KetQua<ApproveKiemKeBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess());
        ApproveKiemKeBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(1, receipt.totalBatchesAdjusted());
        assertEquals(1, receipt.adjustments().size());
        assertEquals(BATCH_ID_1, receipt.adjustments().get(0).tonKhoId());
        assertEquals(1, savedBatches.size());
    }

    @Test
    void convenienceConstructorDerivesAuditActorContext() {
        ApproveKiemKeCommand command =
                new ApproveKiemKeCommand(STOCKTAKE_ID, APPROVER_ID, ADMIN_CONTEXT);

        assertNotNull(command.auditActorContext());
        assertEquals(APPROVER_ID, command.auditActorContext().employeeId());
        assertEquals(1, command.auditActorContext().roleId());
        assertEquals("ADMIN", command.auditActorContext().roleCode());
        assertEquals(FACILITY_ID, command.auditActorContext().facilityId());
        assertEquals(DEPARTMENT_ID, command.auditActorContext().departmentId());
        assertNotNull(command.auditActorContext().correlationId());
        assertNull(command.auditActorContext().workstationId());

        ApproveKiemKeCommand nullContextCmd =
                new ApproveKiemKeCommand(STOCKTAKE_ID, APPROVER_ID, null);
        assertNull(nullContextCmd.auditActorContext());
    }

    private static ApproveKiemKeCommand command(
            int stocktakeId, int approverId, PhanQuyenNguCanh context) {
        return new ApproveKiemKeCommand(
                stocktakeId,
                approverId,
                context,
                new KiemToanTacNhanNguCanh(
                        context.employeeId(),
                        context.roleId(),
                        context.roleCode(),
                        context.facilityId(),
                        context.departmentId(),
                        WORKSTATION_ID,
                        CORRELATION_ID));
    }

    private static void assertSuccessAudit(HeThongKiemToanNhapLieu audit, PhanQuyenNguCanh context) {
        assertEquals(context.employeeId(), audit.actorEmployeeId());
        assertEquals(context.roleId(), audit.roleId());
        assertEquals(context.roleCode(), audit.roleCode());
        assertEquals(KiemToanHanhDongMa.INV_STOCKTAKE_APPROVED, audit.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.STOCKTAKE, audit.resourceType());
        assertEquals(STOCKTAKE_ID, audit.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, audit.outcome());
        assertNull(audit.reasonCode());
    }

    private static void assertFailure(KetQua<ApproveKiemKeBienNhan> result, String code, String message) {
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertEquals(code, result.getErrorCode().orElse(null));
        assertEquals(message, result.getError().map(KetQua.ApplicationError::message).orElse(null));
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
