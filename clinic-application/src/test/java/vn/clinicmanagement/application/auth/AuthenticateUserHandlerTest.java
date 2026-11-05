package vn.clinicmanagement.application.auth;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticateUserHandlerTest {
    private static final int SYNTHETIC_EMPLOYEE_ID = 701;
    private static final String SYNTHETIC_USERNAME = "synth.user.701";
    private static final String SYNTHETIC_HASH = "synthetic-password-hash-701";
    private static final String EXPECTED_INVALID_MESSAGE = "Tên đăng nhập hoặc mật khẩu không đúng.";

    @Test
    void returnsEveryAssignedRoleAndScopeAndRecordsLoginInsideTransaction() {
        Fixture fixture = new Fixture();
        fixture.roles = List.of(role(31, "admin"), role(42, "doctor"));
        fixture.scopes = List.of(scope(11, "Cơ sở mẫu A", 21, "Khoa mẫu A"),
                scope(12, "Cơ sở mẫu B", 22, "Khoa mẫu B"));
        AuthenticateUserCommand command = commandWithSyntheticSecret("  " + SYNTHETIC_USERNAME + "  ");

        Result<AuthenticatedUser> result = fixture.handler.handle(command);

        assertTrue(result.isSuccess());
        AuthenticatedUser user = result.valueOrThrow();
        assertEquals(SYNTHETIC_EMPLOYEE_ID, user.userId());
        assertEquals(SYNTHETIC_USERNAME, user.username());
        assertEquals("Họ Mẫu Nhân Viên 701", user.fullName());
        assertEquals(List.of(new AuthenticatedUser.RoleChoice(31, "ADMIN", "Quản trị viên (Admin)"),
                new AuthenticatedUser.RoleChoice(42, "DOCTOR", "Bác sĩ điều trị (Doctor Station)")), user.roles());
        assertEquals(List.of(new AuthenticatedUser.WorkScopeChoice(11, "Cơ sở mẫu A", 21, "Khoa mẫu A"),
                new AuthenticatedUser.WorkScopeChoice(12, "Cơ sở mẫu B", 22, "Khoa mẫu B")), user.workScopes());
        assertEquals(1, fixture.usernameLookupCalls.get());
        assertEquals(1, fixture.roleLookupCalls.get());
        assertEquals(1, fixture.scopeLookupCalls.get());
        assertEquals(1, fixture.transactions.calls.get());
        assertEquals(1, fixture.recordLoginCalls.get());
        assertTrue(fixture.recordedInsideTransaction.get());
        assertEquals(SYNTHETIC_EMPLOYEE_ID, fixture.recordedEmployeeId.get());
        assertNotNull(fixture.recordedLoginAt.get());
        assertThrows(UnsupportedOperationException.class, () -> user.roles().clear());
        assertThrows(UnsupportedOperationException.class, () -> user.workScopes().clear());
        assertPasswordBuffersCleared(command, fixture.lastPasswordPassedToVerifier.get());
    }

    @Test
    void returnsIdenticalGenericErrorForMissingInactiveDeletedAndWrongCredentialAccounts() {
        Fixture missing = new Fixture();
        missing.employee = null;
        AuthenticateUserCommand missingCommand = commandWithSyntheticSecret("missing.synth.user");
        Result<AuthenticatedUser> missingResult = missing.handler.handle(missingCommand);

        Fixture inactive = new Fixture();
        inactive.employee.setHoatDong(false);
        AuthenticateUserCommand inactiveCommand = commandWithSyntheticSecret(SYNTHETIC_USERNAME);
        Result<AuthenticatedUser> inactiveResult = inactive.handler.handle(inactiveCommand);

        Fixture deleted = new Fixture();
        deleted.employee.softDelete(999);
        AuthenticateUserCommand deletedCommand = commandWithSyntheticSecret(SYNTHETIC_USERNAME);
        Result<AuthenticatedUser> deletedResult = deleted.handler.handle(deletedCommand);

        Fixture wrongPassword = new Fixture();
        wrongPassword.passwordVerified = false;
        AuthenticateUserCommand wrongPasswordCommand = commandWithSyntheticSecret(SYNTHETIC_USERNAME);
        Result<AuthenticatedUser> wrongPasswordResult = wrongPassword.handler.handle(wrongPasswordCommand);

        Result.ApplicationError expected = new Result.ApplicationError("AUTH_INVALID", EXPECTED_INVALID_MESSAGE);
        for (Result<AuthenticatedUser> result : List.of(missingResult, inactiveResult, deletedResult, wrongPasswordResult)) {
            assertTrue(result.isFailure());
            Result.ApplicationError error = result.getError().orElseThrow();
            assertEquals(expected, error);
            assertFalse(error.message().contains(SYNTHETIC_USERNAME));
            assertFalse(error.message().contains("missing.synth.user"));
        }

        for (Fixture fixture : List.of(missing, inactive, deleted, wrongPassword)) {
            assertEquals(0, fixture.transactions.calls.get());
            assertEquals(0, fixture.recordLoginCalls.get());
            assertEquals(0, fixture.roleLookupCalls.get());
            assertEquals(0, fixture.scopeLookupCalls.get());
        }
        assertEquals(0, missing.verifierCalls.get());
        assertEquals(0, inactive.verifierCalls.get());
        assertEquals(0, deleted.verifierCalls.get());
        assertEquals(1, wrongPassword.verifierCalls.get());
        assertCommandBufferZeroized(missingCommand);
        assertCommandBufferZeroized(inactiveCommand);
        assertCommandBufferZeroized(deletedCommand);
        assertPasswordBuffersCleared(wrongPasswordCommand, wrongPassword.lastPasswordPassedToVerifier.get());
    }

    @Test
    void rejectsNullOrBlankUsernameBeforeRepositoryCalls() {
        Fixture fixture = new Fixture();

        Result<AuthenticatedUser> nullCommandResult = fixture.handler.handle(null);
        AuthenticateUserCommand nullUsernameCommand = new AuthenticateUserCommand(null, "synthetic-secret".toCharArray());
        Result<AuthenticatedUser> nullUsernameResult = fixture.handler.handle(nullUsernameCommand);
        AuthenticateUserCommand blankCommand = new AuthenticateUserCommand("   \t  ", "synthetic-secret".toCharArray());
        Result<AuthenticatedUser> blankCommandResult = fixture.handler.handle(blankCommand);

        Result.ApplicationError expected = new Result.ApplicationError("AUTH_INVALID", EXPECTED_INVALID_MESSAGE);
        assertEquals(expected, nullCommandResult.getError().orElseThrow());
        assertEquals(expected, nullUsernameResult.getError().orElseThrow());
        assertEquals(expected, blankCommandResult.getError().orElseThrow());
        assertEquals(0, fixture.usernameLookupCalls.get());
        assertEquals(0, fixture.roleLookupCalls.get());
        assertEquals(0, fixture.scopeLookupCalls.get());
        assertEquals(0, fixture.transactions.calls.get());
        assertEquals(0, fixture.recordLoginCalls.get());
        assertCommandBufferZeroized(nullUsernameCommand);
        assertCommandBufferZeroized(blankCommand);
    }

    @Test
    void rejectsAccountsWithoutActiveRoleOrValidScopeWithoutRecordingLogin() {
        for (List<VaiTro> roles : List.of(List.<VaiTro>of(), Arrays.asList(null, role(0, "ADMIN"), role(32, "  ")))) {
            Fixture noRoles = new Fixture();
            noRoles.roles = roles;
            AuthenticateUserCommand command = commandWithSyntheticSecret(SYNTHETIC_USERNAME);

            Result<AuthenticatedUser> result = noRoles.handler.handle(command);

            assertTrue(result.isFailure());
            assertEquals("AUTH_NO_ROLE", result.getError().orElseThrow().code());
            assertEquals("Tài khoản chưa được gán vai trò hoạt động.", result.getError().orElseThrow().message());
            assertEquals(1, noRoles.roleLookupCalls.get());
            assertEquals(0, noRoles.scopeLookupCalls.get());
            assertEquals(0, noRoles.transactions.calls.get());
            assertEquals(0, noRoles.recordLoginCalls.get());
            assertPasswordBuffersCleared(command, noRoles.lastPasswordPassedToVerifier.get());
        }

        Fixture noScopes = new Fixture();
        noScopes.scopes = List.of();
        AuthenticateUserCommand noScopeCommand = commandWithSyntheticSecret(SYNTHETIC_USERNAME);
        Result<AuthenticatedUser> noScopeResult = noScopes.handler.handle(noScopeCommand);
        assertTrue(noScopeResult.isFailure());
        assertEquals("AUTH_NO_SCOPE", noScopeResult.getError().orElseThrow().code());
        assertEquals(1, noScopes.roleLookupCalls.get());
        assertEquals(1, noScopes.scopeLookupCalls.get());
        assertEquals(0, noScopes.transactions.calls.get());
        assertEquals(0, noScopes.recordLoginCalls.get());
        assertPasswordBuffersCleared(noScopeCommand, noScopes.lastPasswordPassedToVerifier.get());
    }

    @Test
    void assignmentLookupsHappenOnlyAfterCredentialsAreVerified() {
        Fixture fixture = new Fixture();
        fixture.passwordVerified = false;
        AuthenticateUserCommand command = commandWithSyntheticSecret(SYNTHETIC_USERNAME);

        Result<AuthenticatedUser> result = fixture.handler.handle(command);

        assertEquals(new Result.ApplicationError("AUTH_INVALID", EXPECTED_INVALID_MESSAGE), result.getError().orElseThrow());
        assertEquals(1, fixture.verifierCalls.get());
        assertEquals(0, fixture.roleLookupCalls.get());
        assertEquals(0, fixture.scopeLookupCalls.get());
        assertEquals(0, fixture.recordLoginCalls.get());
        assertPasswordBuffersCleared(command, fixture.lastPasswordPassedToVerifier.get());
    }

    @Test
    void returnsSafeGenericUnavailableErrorForRepositoryAndTransactionFailures() {
        String syntheticDiagnostic = "synthetic repository diagnostic for " + SYNTHETIC_USERNAME;

        Fixture lookupFailure = new Fixture();
        lookupFailure.lookupFailure = new IllegalStateException(syntheticDiagnostic);
        Fixture roleFailure = new Fixture();
        roleFailure.roleFailure = new IllegalStateException(syntheticDiagnostic);
        Fixture scopeFailure = new Fixture();
        scopeFailure.scopeFailure = new IllegalStateException(syntheticDiagnostic);
        Fixture loginFailure = new Fixture();
        loginFailure.loginFailure = new IllegalStateException(syntheticDiagnostic);

        List<Fixture> fixtures = List.of(lookupFailure, roleFailure, scopeFailure, loginFailure);
        List<AuthenticateUserCommand> commands = new ArrayList<>();
        List<Result<AuthenticatedUser>> results = new ArrayList<>();
        for (Fixture fixture : fixtures) {
            AuthenticateUserCommand command = commandWithSyntheticSecret(SYNTHETIC_USERNAME);
            commands.add(command);
            results.add(fixture.handler.handle(command));
        }

        for (Result<AuthenticatedUser> result : results) {
            assertTrue(result.isFailure());
            Result.ApplicationError error = result.getError().orElseThrow();
            assertEquals("AUTH_UNAVAILABLE", error.code());
            assertEquals("Không thể xác thực lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.", error.message());
            assertFalse(error.message().contains(syntheticDiagnostic));
            assertFalse(error.message().contains(SYNTHETIC_USERNAME));
        }
        assertEquals(0, lookupFailure.transactions.calls.get());
        assertEquals(0, lookupFailure.recordLoginCalls.get());
        assertEquals(0, roleFailure.transactions.calls.get());
        assertEquals(0, scopeFailure.transactions.calls.get());
        assertEquals(1, loginFailure.transactions.calls.get());
        assertEquals(1, loginFailure.recordLoginCalls.get());
        for (int index = 0; index < fixtures.size(); index++) {
            assertCommandBufferZeroized(commands.get(index));
            char[] verifierBuffer = fixtures.get(index).lastPasswordPassedToVerifier.get();
            if (verifierBuffer != null) assertCleared(verifierBuffer);
        }
    }

    @Test
    void commandOwnsAnIndependentPasswordCopyAndZeroizesItWhenClosed() {
        char[] callerInput = "synthetic-command-secret".toCharArray();
        AuthenticateUserCommand command = new AuthenticateUserCommand("  synth.actor  ", callerInput);

        Arrays.fill(callerInput, 'X');
        assertEquals("synth.actor", command.username());
        char[] firstCopy = command.passwordCopy();
        assertArrayEquals("synthetic-command-secret".toCharArray(), firstCopy);
        assertNotSame(firstCopy, command.passwordCopy());
        Arrays.fill(firstCopy, 'Z');
        assertArrayEquals("synthetic-command-secret".toCharArray(), command.passwordCopy());

        command.close();
        assertCommandBufferZeroized(command);

        AuthenticateUserCommand nullSafeCommand = new AuthenticateUserCommand(null, null);
        assertEquals("", nullSafeCommand.username());
        assertEquals(0, nullSafeCommand.passwordCopy().length);
        nullSafeCommand.close();
        assertEquals(0, nullSafeCommand.passwordCopy().length);
    }

    private static VaiTro role(int id, String code) {
        VaiTro role = new VaiTro();
        role.setId(id);
        role.setTenVaiTro(code);
        return role;
    }

    private static NhanVienWorkScope scope(int facilityId, String facilityName, int departmentId, String departmentName) {
        return new NhanVienWorkScope(facilityId, facilityName, departmentId, departmentName);
    }

    private static AuthenticateUserCommand commandWithSyntheticSecret(String username) {
        return new AuthenticateUserCommand(username, "synthetic-secret-value-123".toCharArray());
    }

    private static void assertPasswordBuffersCleared(AuthenticateUserCommand command, char[] verifierBuffer) {
        assertCommandBufferZeroized(command);
        assertNotNull(verifierBuffer);
        assertCleared(verifierBuffer);
    }

    private static void assertCommandBufferZeroized(AuthenticateUserCommand command) {
        char[] remaining = command.passwordCopy();
        assertTrue(remaining.length > 0, "Expected non-empty secret buffer before zeroization check");
        assertCleared(remaining);
    }

    private static void assertCleared(char[] chars) {
        for (char value : chars) assertEquals('\0', value, "Expected secret buffer to be cleared to '\\0'");
    }

    private static NhanVien activeSyntheticEmployee() {
        NhanVien employee = new NhanVien();
        employee.setId(SYNTHETIC_EMPLOYEE_ID);
        employee.setMaNhanVien("NV-SYN-701");
        employee.setHo("Họ Mẫu");
        employee.setTen("Nhân Viên 701");
        employee.setUsername(SYNTHETIC_USERNAME);
        employee.setPasswordHash(SYNTHETIC_HASH);
        employee.setHoatDong(true);
        return employee;
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        Object instance = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxyObj, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxyObj == args[0];
                    case "hashCode" -> System.identityHashCode(proxyObj);
                    case "toString" -> type.getSimpleName() + " synthetic test proxy";
                    default -> null;
                };
            }
            return invocation.invoke(method.getName(), args);
        });
        return type.cast(instance);
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args);
    }

    private static final class Fixture {
        private final AtomicInteger usernameLookupCalls = new AtomicInteger();
        private final AtomicInteger roleLookupCalls = new AtomicInteger();
        private final AtomicInteger scopeLookupCalls = new AtomicInteger();
        private final AtomicInteger recordLoginCalls = new AtomicInteger();
        private final AtomicInteger verifierCalls = new AtomicInteger();
        private final AtomicBoolean recordedInsideTransaction = new AtomicBoolean(false);
        private final AtomicReference<Integer> recordedEmployeeId = new AtomicReference<>();
        private final AtomicReference<LocalDateTime> recordedLoginAt = new AtomicReference<>();
        private final AtomicReference<char[]> lastPasswordPassedToVerifier = new AtomicReference<>();
        private final TransactionProbe transactions = new TransactionProbe();
        private final AuthenticateUserHandler handler;

        private NhanVien employee = activeSyntheticEmployee();
        private List<VaiTro> roles = List.of(role(31, "ADMIN"));
        private List<NhanVienWorkScope> scopes = List.of(scope(11, "Cơ sở mẫu", 21, "Khoa mẫu"));
        private boolean passwordVerified = true;
        private RuntimeException lookupFailure;
        private RuntimeException roleFailure;
        private RuntimeException scopeFailure;
        private RuntimeException loginFailure;

        private Fixture() {
            INhanVienRepository employees = proxy(INhanVienRepository.class, (method, args) -> switch (method) {
                case "getByUsername" -> {
                    usernameLookupCalls.incrementAndGet();
                    if (lookupFailure != null) throw lookupFailure;
                    yield Optional.ofNullable(employee);
                }
                case "getAssignedRoles" -> {
                    roleLookupCalls.incrementAndGet();
                    if (roleFailure != null) throw roleFailure;
                    yield roles;
                }
                case "getAssignedWorkScopes" -> {
                    scopeLookupCalls.incrementAndGet();
                    if (scopeFailure != null) throw scopeFailure;
                    yield scopes;
                }
                case "recordSuccessfulLogin" -> {
                    recordLoginCalls.incrementAndGet();
                    recordedInsideTransaction.set(transactions.active.get());
                    recordedEmployeeId.set((Integer) args[0]);
                    recordedLoginAt.set((LocalDateTime) args[1]);
                    if (loginFailure != null) throw loginFailure;
                    yield null;
                }
                case "getRoleNames" -> List.of();
                case "getById" -> Optional.empty();
                case "getAll", "getActiveDoctors", "find" -> List.of();
                case "exists" -> false;
                case "count" -> 0;
                default -> null;
            });

            PasswordHasher passwordHasher = (password, encodedHash) -> {
                verifierCalls.incrementAndGet();
                lastPasswordPassedToVerifier.set(password);
                return passwordVerified && SYNTHETIC_HASH.equals(encodedHash);
            };
            handler = new AuthenticateUserHandler(employees, passwordHasher, transactions);
        }
    }

    private static final class TransactionProbe implements TransactionBoundary {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicBoolean active = new AtomicBoolean(false);

        @Override
        public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
            calls.incrementAndGet();
            active.set(true);
            try {
                return operation.get();
            } finally {
                active.set(false);
            }
        }
    }
}
