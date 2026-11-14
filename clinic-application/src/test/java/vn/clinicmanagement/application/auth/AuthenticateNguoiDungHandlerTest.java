package vn.clinicmanagement.application.auth;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticateNguoiDungHandlerTest {
    private static final int EMPLOYEE_ID = 701;
    private static final String USERNAME = "synthetic.user.701";
    private static final String HASH = "synthetic-hash-only";
    private static final String INVALID_MESSAGE = "Tên đăng nhập hoặc mật khẩu không đúng.";
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 15, 8, 0);

    @Test
    void fifthFailureStartsThirtySecondDelayAndFirstFiveFailuresHaveNoDelay() {
        Fixture fixture = new Fixture();

        for (int failure = 1; failure <= 5; failure++) {
            KetQua<DaXacThucNguoiDung> result = fixture.handle("wrong");
            assertInvalid(result);
            assertEquals(failure, fixture.throttle.state.failureCount());
            assertEquals(failure == 5 ? 30 : 0, fixture.throttle.state.delaySeconds());
            assertEquals(START, fixture.throttle.state.windowStartedAt());
        }

        assertEquals(5, fixture.verifierCalls.get());
        assertEquals(5, fixture.audit.entries.size());
        for (HeThongKiemToanNhapLieu entry : fixture.audit.entries) {
            assertEquals(KiemToanHanhDongMa.AUTH_LOGIN_FAILED, entry.actionCode());
            assertEquals(KiemToanLyDoMa.AUTH_INVALID, entry.reasonCode());
            assertEquals(KiemToanKetQua.FAILURE, entry.outcome());
            assertEquals(EMPLOYEE_ID, entry.resourceId());
            assertEquals(KiemToanTaiNguyenLoai.EMPLOYEE, entry.resourceType());
            assertNull(entry.roleCode());
            assertNull(entry.workstationId());
        }
    }

    @Test
    void fixedWindowIsHalfOpenAtExactlyFifteenMinutes() {
        Fixture beforeBoundary = new Fixture();
        beforeBoundary.throttle.state = state(START.plusMinutes(14).plusSeconds(59), START, 4,
                START.plusMinutes(14).plusSeconds(59), 0, null);
        assertInvalid(beforeBoundary.handle("wrong"));
        assertEquals(5, beforeBoundary.throttle.state.failureCount());
        assertEquals(30, beforeBoundary.throttle.state.delaySeconds());

        Fixture atBoundary = new Fixture();
        atBoundary.throttle.now = START.plusMinutes(15);
        atBoundary.throttle.state = state(atBoundary.throttle.now, START, 4,
                atBoundary.throttle.now.minusSeconds(1), 0, null);
        assertInvalid(atBoundary.handle("wrong"));
        assertEquals(1, atBoundary.throttle.state.failureCount());
        assertEquals(atBoundary.throttle.now, atBoundary.throttle.state.windowStartedAt());
        assertEquals(0, atBoundary.throttle.state.delaySeconds());
    }

    @Test
    void activeDelayIsGenericAuditedAndDoesNotVerifyOrMutateState() {
        Fixture fixture = new Fixture();
        fixture.throttle.state = state(START, START, 5, START, 30, START.plusSeconds(30));
        fixture.throttle.now = START.plusSeconds(29);
        TaiKhoanDangNhapGioiHanTanSuatLuuTru.Snapshot before = fixture.throttle.state;

        KetQua<DaXacThucNguoiDung> result = fixture.handle("correct");

        assertInvalid(result);
        assertEquals(0, fixture.verifierCalls.get());
        assertEquals(before, fixture.throttle.state);
        HeThongKiemToanNhapLieu entry = fixture.audit.entries.get(0);
        assertEquals(KiemToanHanhDongMa.AUTH_LOGIN_FAILED, entry.actionCode());
        assertEquals(KiemToanLyDoMa.AUTH_THROTTLED, entry.reasonCode());
        assertEquals(KiemToanKetQua.FAILURE, entry.outcome());
        assertNoCredentialMetadata(entry);
    }

    @Test
    void expiredDelaysEscalateToCapAndRemainThere() {
        Fixture fixture = new Fixture();
        for (int i = 0; i < 5; i++) assertInvalid(fixture.handle("wrong"));
        assertEquals(30, fixture.throttle.state.delaySeconds());

        for (int delay : List.of(60, 120, 240, 480, 900, 900)) {
            LocalDateTime eligibleAt = fixture.throttle.state.nextAllowedAt();
            fixture.throttle.now = eligibleAt.minusSeconds(1);
            TaiKhoanDangNhapGioiHanTanSuatLuuTru.Snapshot before = fixture.throttle.state;
            int verifierCallsBeforeWait = fixture.verifierCalls.get();
            assertInvalid(fixture.handle("correct"));
            assertEquals(before, fixture.throttle.state);
            assertEquals(verifierCallsBeforeWait, fixture.verifierCalls.get());

            fixture.throttle.now = eligibleAt;
            assertInvalid(fixture.handle("wrong"));
            assertEquals(delay, fixture.throttle.state.delaySeconds());
            assertEquals(eligibleAt, fixture.throttle.state.lastFailureAt());
            assertEquals(eligibleAt.plusSeconds(delay), fixture.throttle.state.nextAllowedAt());
        }
    }

    @Test
    void sequenceExpiresAtTwentyFourHoursAndStartsANewWindow() {
        Fixture fixture = new Fixture();
        fixture.throttle.now = START.plusHours(24);
        fixture.throttle.state = state(fixture.throttle.now, START, 5, START, 900,
                fixture.throttle.now.minusSeconds(1));

        assertInvalid(fixture.handle("wrong"));

        assertEquals(1, fixture.throttle.state.failureCount());
        assertEquals(fixture.throttle.now, fixture.throttle.state.windowStartedAt());
        assertEquals(0, fixture.throttle.state.delaySeconds());
        assertNull(fixture.throttle.state.nextAllowedAt());
        assertEquals(1, fixture.verifierCalls.get());
    }

    @Test
    void successfulVerificationResetsThrottleBeforeRoleAndScopeChecks() {
        Fixture noRole = new Fixture();
        noRole.throttle.state = state(START, START, 5, START, 30, START);
        noRole.employeeHasRole = false;
        noRole.assertResetBeforeAssignments = true;

        KetQua<DaXacThucNguoiDung> noRoleResult = noRole.handle("correct");

        assertEquals("AUTH_NO_ROLE", noRoleResult.getError().orElseThrow().code());
        assertEquals(1, noRole.throttle.resetCalls);
        assertEquals(0, noRole.throttle.state.failureCount());
        assertEquals(KiemToanHanhDongMa.AUTH_LOGIN_DENIED_NO_ROLE, noRole.audit.entries.get(0).actionCode());

        Fixture noScope = new Fixture();
        noScope.throttle.state = state(START, START, 5, START, 30, START);
        noScope.employeeHasScope = false;
        noScope.assertResetBeforeAssignments = true;
        KetQua<DaXacThucNguoiDung> noScopeResult = noScope.handle("correct");
        assertEquals("AUTH_NO_SCOPE", noScopeResult.getError().orElseThrow().code());
        assertEquals(1, noScope.throttle.resetCalls);
        assertEquals(KiemToanHanhDongMa.AUTH_LOGIN_DENIED_NO_SCOPE, noScope.audit.entries.get(0).actionCode());
    }

    @Test
    void inactiveOrDeletedAccountNeverVerifiesOrResetsThrottle() {
        for (boolean deleted : List.of(false, true)) {
            Fixture fixture = new Fixture();
            fixture.employee.setHoatDong(false);
            if (deleted) fixture.employee.softDelete(77);
            fixture.throttle.state = state(START, START, 5, START, 30, START);

            assertInvalid(fixture.handle("correct"));

            assertEquals(0, fixture.verifierCalls.get());
            assertEquals(0, fixture.throttle.resetCalls);
            assertEquals(5, fixture.throttle.state.failureCount());
        }
    }

    @Test
    void auditOrThrottlePersistenceFailureFailsClosedAndRollsBackState() {
        Fixture auditFailure = new Fixture();
        TaiKhoanDangNhapGioiHanTanSuatLuuTru.Snapshot initial = auditFailure.throttle.state;
        auditFailure.audit.failure = new IllegalStateException("synthetic audit failure");

        KetQua<DaXacThucNguoiDung> auditResult = auditFailure.handle("wrong");

        assertUnavailable(auditResult);
        assertEquals(initial, auditFailure.throttle.state);
        assertTrue(auditFailure.audit.entries.isEmpty());

        Fixture stateFailure = new Fixture();
        TaiKhoanDangNhapGioiHanTanSuatLuuTru.Snapshot initialState = stateFailure.throttle.state;
        stateFailure.throttle.failure = new IllegalStateException("synthetic persistence failure");

        KetQua<DaXacThucNguoiDung> stateResult = stateFailure.handle("wrong");

        assertUnavailable(stateResult);
        assertEquals(initialState, stateFailure.throttle.state);
        assertTrue(stateFailure.audit.entries.isEmpty());
    }

    @Test
    void unknownAndThrottledAccountsShareGenericPublicFailure() {
        Fixture unknown = new Fixture();
        unknown.employee = null;
        Fixture throttled = new Fixture();
        throttled.throttle.state = state(START, START, 5, START, 30, START.plusSeconds(30));
        throttled.throttle.now = START;

        KetQua<DaXacThucNguoiDung> unknownResult = unknown.handle("synthetic-missing");
        KetQua<DaXacThucNguoiDung> throttledResult = throttled.handle("correct");

        assertEquals(unknownResult.getError().orElseThrow(), throttledResult.getError().orElseThrow());
        assertEquals(INVALID_MESSAGE, throttledResult.getError().orElseThrow().message());
        assertEquals(0, throttled.verifierCalls.get());
        assertEquals(KiemToanHanhDongMa.AUTH_LOGIN_FAILED, unknown.audit.entries.get(0).actionCode());
        assertEquals(KiemToanHanhDongMa.AUTH_LOGIN_FAILED, throttled.audit.entries.get(0).actionCode());
        assertEquals(KiemToanLyDoMa.AUTH_THROTTLED, throttled.audit.entries.get(0).reasonCode());
    }

    @Test
    void blankUsernameUsesAnonymousAuditAndClosesOnlyCommandOwnedPasswordBuffer() {
        Fixture fixture = new Fixture();
        char[] callerPassword = "synthetic-secret".toCharArray();
        AuthenticateNguoiDungCommand blank = new AuthenticateNguoiDungCommand("  \t ", callerPassword);

        KetQua<DaXacThucNguoiDung> result = fixture.handler.handle(blank);

        assertInvalid(result);
        assertEquals(0, fixture.verifierCalls.get());
        assertEquals(0, fixture.throttle.state.failureCount());
        assertEquals(1, fixture.audit.entries.size());
        assertNull(fixture.audit.entries.get(0).actorEmployeeId());
        assertEquals(KiemToanLyDoMa.AUTH_INVALID, fixture.audit.entries.get(0).reasonCode());
        assertArrayEquals(new char["synthetic-secret".length()], blank.passwordCopy());
        assertArrayEquals("synthetic-secret".toCharArray(), callerPassword,
                "The command owns and clears a copy without mutating caller memory");

        KetQua<DaXacThucNguoiDung> nullCommand = fixture.handler.handle(null);
        assertInvalid(nullCommand);
        assertEquals(2, fixture.audit.entries.size());
    }

    @Test
    void wrongPasswordClearsVerifierCopyAndCommandOwnedPassword() {
        Fixture fixture = new Fixture();
        char[] callerPassword = "synthetic-secret".toCharArray();
        AuthenticateNguoiDungCommand command = new AuthenticateNguoiDungCommand(USERNAME, callerPassword);

        assertInvalid(fixture.handler.handle(command));

        assertArrayEquals(new char["synthetic-secret".length()], fixture.lastPasswordPassedToVerifier);
        assertArrayEquals(new char["synthetic-secret".length()], command.passwordCopy());
        assertArrayEquals("synthetic-secret".toCharArray(), callerPassword);
    }

    private static TaiKhoanDangNhapGioiHanTanSuatLuuTru.Snapshot state(
            LocalDateTime now, LocalDateTime windowStart, int count,
            LocalDateTime lastFailure, int delay, LocalDateTime nextAllowed) {
        return new TaiKhoanDangNhapGioiHanTanSuatLuuTru.Snapshot(now, windowStart, count, lastFailure, delay, nextAllowed);
    }

    private static void assertInvalid(KetQua<DaXacThucNguoiDung> result) {
        assertTrue(result.isFailure());
        assertEquals("AUTH_INVALID", result.getError().orElseThrow().code());
        assertEquals(INVALID_MESSAGE, result.getError().orElseThrow().message());
    }

    private static void assertUnavailable(KetQua<DaXacThucNguoiDung> result) {
        assertTrue(result.isFailure());
        assertEquals("AUTH_UNAVAILABLE", result.getError().orElseThrow().code());
        assertFalse(result.getError().orElseThrow().message().contains("synthetic"));
    }

    private static void assertNoCredentialMetadata(HeThongKiemToanNhapLieu entry) {
        assertNull(entry.roleCode());
        assertNull(entry.facilityId());
        assertNull(entry.departmentId());
        assertNull(entry.workstationId());
    }

    private static NhanVien syntheticEmployee() {
        NhanVien employee = new NhanVien();
        employee.setId(EMPLOYEE_ID);
        employee.setUsername(USERNAME);
        employee.setHo("Sample");
        employee.setTen("Employee");
        employee.setPasswordHash(HASH);
        employee.setHoatDong(true);
        return employee;
    }

    private static VaiTro role() {
        VaiTro role = new VaiTro();
        role.setId(31);
        role.setTenVaiTro("ADMIN");
        return role;
    }

    private static NhanVienWorkPhamVi scope() {
        return new NhanVienWorkPhamVi(11, "Synthetic facility", 21, "Synthetic department");
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + " synthetic proxy";
                    default -> null;
                };
            }
            return invocation.invoke(method.getName(), args);
        }));
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args);
    }

    private final class Fixture {
        private NhanVien employee = syntheticEmployee();
        private final AtomicInteger verifierCalls = new AtomicInteger();
        private char[] lastPasswordPassedToVerifier;
        private final FakeThrottleStore throttle = new FakeThrottleStore();
        private final FakeAudit audit = new FakeAudit();
        private final FakeTransaction transaction = new FakeTransaction(throttle, audit);
        private boolean passwordCorrect;
        private boolean employeeHasRole = true;
        private boolean employeeHasScope = true;
        private boolean assertResetBeforeAssignments;
        private final AuthenticateNguoiDungHandler handler;

        private Fixture() {
            INhanVienRepository repository = proxy(INhanVienRepository.class, (method, args) -> switch (method) {
                case "getByUsername" -> employee != null && !employee.isDeleted()
                        ? Optional.of(employee) : Optional.empty();
                case "getById" -> employee != null && !employee.isDeleted()
                        ? Optional.of(employee) : Optional.empty();
                case "getAssignedRoles" -> {
                    if (assertResetBeforeAssignments) {
                        assertEquals(0, throttle.state.failureCount());
                        assertFalse(transaction.active);
                    }
                    yield employeeHasRole ? List.of(role()) : List.of();
                }
                case "getAssignedWorkScopes" -> employeeHasScope ? List.of(scope()) : List.of();
                case "getAll", "getActiveDoctors", "find", "getRoleNames" -> List.of();
                case "count" -> 0;
                case "exists" -> false;
                default -> null;
            });
            MatKhauBoBam hasher = (password, passwordHash) -> {
                verifierCalls.incrementAndGet();
                lastPasswordPassedToVerifier = password;
                return passwordCorrect && HASH.equals(passwordHash)
                        && Arrays.equals(password, "correct".toCharArray());
            };
            handler = new AuthenticateNguoiDungHandler(repository, throttle, hasher, transaction,
                    audit, ClockHolder.CLOCK);
        }

        private KetQua<DaXacThucNguoiDung> handle(String secret) {
            passwordCorrect = "correct".equals(secret);
            char[] chars = secret.toCharArray();
            return handler.handle(new AuthenticateNguoiDungCommand(USERNAME, chars));
        }
    }

    private static final class FakeThrottleStore implements TaiKhoanDangNhapGioiHanTanSuatLuuTru {
        private LocalDateTime now = START;
        private Snapshot state = new Snapshot(START, null, 0, null, 0, null);
        private int resetCalls;
        private RuntimeException failure;

        @Override
        public Optional<Snapshot> lockForUpdate(int employeeId) {
            if (failure != null) throw failure;
            return Optional.of(new Snapshot(now, state.windowStartedAt(), state.failureCount(),
                    state.lastFailureAt(), state.delaySeconds(), state.nextAllowedAt()));
        }

        @Override
        public void recordFailure(int employeeId, LocalDateTime windowStartedAt, int failureCount, int delaySeconds) {
            if (failure != null) throw failure;
            state = new Snapshot(now, windowStartedAt, failureCount, now, delaySeconds,
                    delaySeconds == 0 ? null : now.plusSeconds(delaySeconds));
        }

        @Override
        public void reset(int employeeId) {
            if (failure != null) throw failure;
            resetCalls++;
            state = new Snapshot(now, null, 0, null, 0, null);
        }
    }

    private static final class FakeAudit implements KiemToanWriteCongKetNoi {
        private final List<HeThongKiemToanNhapLieu> entries = new ArrayList<>();
        private RuntimeException failure;

        @Override
        public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {
            if (failure != null) throw failure;
            entries.add(entry);
        }

        @Override
        public void appendIndependently(HeThongKiemToanNhapLieu entry) {
            if (failure != null) throw failure;
            entries.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeTransaction implements GiaoDichRanhGioi {
        private final FakeThrottleStore throttle;
        private final FakeAudit audit;
        private boolean active;

        private FakeTransaction(FakeThrottleStore throttle, FakeAudit audit) {
            this.throttle = throttle;
            this.audit = audit;
        }

        @Override
        public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation) {
            TaiKhoanDangNhapGioiHanTanSuatLuuTru.Snapshot before = throttle.state;
            int resetBefore = throttle.resetCalls;
            List<HeThongKiemToanNhapLieu> auditsBefore = new ArrayList<>(audit.entries);
            active = true;
            try {
                return operation.get();
            } catch (RuntimeException ex) {
                throttle.state = before;
                throttle.resetCalls = resetBefore;
                audit.entries.clear();
                audit.entries.addAll(auditsBefore);
                return KetQua.failure("TRANSACTION_FAILED", "synthetic transaction failure");
            } finally {
                active = false;
            }
        }
    }

    private static final class ClockHolder {
        private static final java.time.Clock CLOCK = java.time.Clock.fixed(
                Instant.parse("2026-01-15T08:00:00Z"), ZoneOffset.UTC);
    }
}
