package vn.clinicmanagement.application.auth;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.identity.NhanVien;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticateUserHandlerTest {
    private static final int SYNTHETIC_EMPLOYEE_ID = 701;
    private static final String SYNTHETIC_USERNAME = "synth.user.701";
    private static final String SYNTHETIC_HASH = "pbkdf2_sha256$100000$c3ludGhldGljLXNhbHQtMTZi$c3ludGhldGljLWhhc2gtMzItYnl0ZXMtMDAwMDAwMDA=";
    private static final String EXPECTED_INVALID_MESSAGE = "Tên đăng nhập hoặc mật khẩu không đúng.";

    @Test
    void authenticatesActiveEmployeeNormalizesAdminRoleAndRecordsLoginInsideTransactionBoundary() {
        Fixture fixture = new Fixture();
        fixture.employee = activeSyntheticEmployee();
        fixture.roles = Arrays.asList("   ", "admin", "doctor");
        fixture.passwordVerified = true;

        AuthenticateUserCommand command = new AuthenticateUserCommand(
                "  " + SYNTHETIC_USERNAME + "  ",
                "synthetic-pass-only".toCharArray()
        );

        Result<AuthenticatedUser> result = fixture.handler.handle(command);

        assertTrue(result.isSuccess());
        AuthenticatedUser user = result.valueOrThrow();
        assertEquals(SYNTHETIC_EMPLOYEE_ID, user.userId());
        assertEquals(SYNTHETIC_USERNAME, user.username());
        assertEquals("Họ Mẫu Nhân Viên 701", user.fullName());
        assertEquals("ADMIN", user.roleCode());
        assertEquals("Quản trị viên (Admin)", user.roleName());

        assertEquals(1, fixture.transactions.calls.get());
        assertEquals(1, fixture.recordLoginCalls.get());
        assertTrue(fixture.recordedInsideTransaction.get());
        assertEquals(SYNTHETIC_EMPLOYEE_ID, fixture.recordedEmployeeId.get());
        assertNotNull(fixture.recordedLoginAt.get());
        assertCommandBufferZeroized(command);
        assertPasswordVerifierBufferZeroized(fixture.lastPasswordPassedToVerifier.get());
    }

    @Test
    void returnsIdenticalGenericErrorForMissingInactiveDeletedAndWrongCredentialAccountsWithoutRecordingLogin() {
        // 1. Missing account
        Fixture missingFixture = new Fixture();
        missingFixture.employee = null;
        AuthenticateUserCommand missingCommand = commandWithSyntheticSecret("missing.synth.user");
        Result<AuthenticatedUser> missingResult = missingFixture.handler.handle(missingCommand);

        // 2. Inactive account
        Fixture inactiveFixture = new Fixture();
        NhanVien inactiveEmployee = activeSyntheticEmployee();
        inactiveEmployee.setHoatDong(false);
        inactiveFixture.employee = inactiveEmployee;
        inactiveFixture.passwordVerified = true;
        AuthenticateUserCommand inactiveCommand = commandWithSyntheticSecret(SYNTHETIC_USERNAME);
        Result<AuthenticatedUser> inactiveResult = inactiveFixture.handler.handle(inactiveCommand);

        // 3. Soft-deleted account
        Fixture deletedFixture = new Fixture();
        NhanVien deletedEmployee = activeSyntheticEmployee();
        deletedEmployee.softDelete(999);
        deletedFixture.employee = deletedEmployee;
        deletedFixture.passwordVerified = true;
        AuthenticateUserCommand deletedCommand = commandWithSyntheticSecret(SYNTHETIC_USERNAME);
        Result<AuthenticatedUser> deletedResult = deletedFixture.handler.handle(deletedCommand);

        // 4. Wrong password on active account
        Fixture wrongPasswordFixture = new Fixture();
        wrongPasswordFixture.employee = activeSyntheticEmployee();
        wrongPasswordFixture.passwordVerified = false;
        AuthenticateUserCommand wrongPasswordCommand = commandWithSyntheticSecret(SYNTHETIC_USERNAME);
        Result<AuthenticatedUser> wrongPasswordResult = wrongPasswordFixture.handler.handle(wrongPasswordCommand);

        Result.ApplicationError expectedError = new Result.ApplicationError("AUTH_INVALID", EXPECTED_INVALID_MESSAGE);
        for (Result<AuthenticatedUser> result : List.of(missingResult, inactiveResult, deletedResult, wrongPasswordResult)) {
            assertTrue(result.isFailure());
            Result.ApplicationError error = result.getError().orElseThrow();
            assertEquals(expectedError, error);
            assertFalse(error.message().contains(SYNTHETIC_USERNAME));
            assertFalse(error.message().contains("missing.synth.user"));
        }

        for (Fixture fixture : List.of(missingFixture, inactiveFixture, deletedFixture, wrongPasswordFixture)) {
            assertEquals(0, fixture.transactions.calls.get());
            assertEquals(0, fixture.recordLoginCalls.get());
            assertEquals(0, fixture.roleLookupCalls.get());
        }

        assertEquals(0, missingFixture.verifierCalls.get());
        assertEquals(0, inactiveFixture.verifierCalls.get());
        assertEquals(0, deletedFixture.verifierCalls.get());
        assertEquals(1, wrongPasswordFixture.verifierCalls.get());
        assertPasswordVerifierBufferZeroized(wrongPasswordFixture.lastPasswordPassedToVerifier.get());

        assertCommandBufferZeroized(missingCommand);
        assertCommandBufferZeroized(inactiveCommand);
        assertCommandBufferZeroized(deletedCommand);
        assertCommandBufferZeroized(wrongPasswordCommand);
    }

    @Test
    void rejectsNullOrBlankUsernameWithoutQueryingRepositoryAndZeroizesCommandBuffer() {
        Fixture fixture = new Fixture();

        Result<AuthenticatedUser> nullCommandResult = fixture.handler.handle(null);
        AuthenticateUserCommand blankCommand = new AuthenticateUserCommand("   \t  ", "synthetic-secret".toCharArray());
        Result<AuthenticatedUser> blankCommandResult = fixture.handler.handle(blankCommand);

        assertEquals(new Result.ApplicationError("AUTH_INVALID", EXPECTED_INVALID_MESSAGE),
                nullCommandResult.getError().orElseThrow());
        assertEquals(new Result.ApplicationError("AUTH_INVALID", EXPECTED_INVALID_MESSAGE),
                blankCommandResult.getError().orElseThrow());
        assertEquals(0, fixture.usernameLookupCalls.get());
        assertEquals(0, fixture.transactions.calls.get());
        assertEquals(0, fixture.recordLoginCalls.get());
        assertCommandBufferZeroized(blankCommand);
    }

    @Test
    void rejectsAccountWithNoActiveRoleWithoutRecordingLoginAndZeroizesCommandBuffer() {
        List<List<String>> emptyOrBlankRoleLists = List.of(
                List.of(),
                Arrays.asList(null, "", "   ")
        );

        for (List<String> roles : emptyOrBlankRoleLists) {
            Fixture fixture = new Fixture();
            fixture.employee = activeSyntheticEmployee();
            fixture.passwordVerified = true;
            fixture.roles = roles;
            AuthenticateUserCommand command = commandWithSyntheticSecret(SYNTHETIC_USERNAME);

            Result<AuthenticatedUser> result = fixture.handler.handle(command);

            assertTrue(result.isFailure());
            Result.ApplicationError error = result.getError().orElseThrow();
            assertEquals("AUTH_NO_ROLE", error.code());
            assertEquals("Tài khoản chưa được gán vai trò hoạt động.", error.message());
            assertEquals(1, fixture.roleLookupCalls.get());
            assertEquals(0, fixture.transactions.calls.get());
            assertEquals(0, fixture.recordLoginCalls.get());
            assertCommandBufferZeroized(command);
            assertPasswordVerifierBufferZeroized(fixture.lastPasswordPassedToVerifier.get());
        }
    }

    @Test
    void returnsSafeUnavailableErrorOnRepositoryOrTransactionOutageAndZeroizesCommandBuffer() {
        String sensitiveDiagnostic = "jdbc:sqlserver://internal-db-host;user=" + SYNTHETIC_USERNAME;

        // 1. Outage during getByUsername
        Fixture lookupOutage = new Fixture();
        lookupOutage.lookupFailure = new IllegalStateException(sensitiveDiagnostic);
        AuthenticateUserCommand lookupCommand = commandWithSyntheticSecret(SYNTHETIC_USERNAME);
        Result<AuthenticatedUser> lookupResult = lookupOutage.handler.handle(lookupCommand);

        // 2. Outage during getRoleNames
        Fixture roleOutage = new Fixture();
        roleOutage.employee = activeSyntheticEmployee();
        roleOutage.passwordVerified = true;
        roleOutage.roleFailure = new IllegalStateException(sensitiveDiagnostic);
        AuthenticateUserCommand roleCommand = commandWithSyntheticSecret(SYNTHETIC_USERNAME);
        Result<AuthenticatedUser> roleResult = roleOutage.handler.handle(roleCommand);

        // 3. Outage during recordSuccessfulLogin inside transaction
        Fixture recordOutage = new Fixture();
        recordOutage.employee = activeSyntheticEmployee();
        recordOutage.passwordVerified = true;
        recordOutage.roles = List.of("ADMIN");
        recordOutage.recordLoginFailure = new IllegalStateException(sensitiveDiagnostic);
        AuthenticateUserCommand recordCommand = commandWithSyntheticSecret(SYNTHETIC_USERNAME);
        Result<AuthenticatedUser> recordResult = recordOutage.handler.handle(recordCommand);

        for (Result<AuthenticatedUser> result : List.of(lookupResult, roleResult, recordResult)) {
            assertTrue(result.isFailure());
            Result.ApplicationError error = result.getError().orElseThrow();
            assertEquals("AUTH_UNAVAILABLE", error.code());
            assertEquals("Không thể xác thực lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.", error.message());
            assertFalse(error.message().contains(sensitiveDiagnostic));
            assertFalse(error.message().contains(SYNTHETIC_USERNAME));
        }

        assertEquals(0, lookupOutage.transactions.calls.get());
        assertEquals(0, lookupOutage.recordLoginCalls.get());
        assertEquals(0, roleOutage.transactions.calls.get());
        assertEquals(0, roleOutage.recordLoginCalls.get());
        assertEquals(1, recordOutage.transactions.calls.get());
        assertEquals(1, recordOutage.recordLoginCalls.get());

        assertCommandBufferZeroized(lookupCommand);
        assertCommandBufferZeroized(roleCommand);
        assertCommandBufferZeroized(recordCommand);
    }

    @Test
    void commandCopiesAndZeroizesOwnedSecretBufferIndependentlyOfCallerArrays() {
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

    private static AuthenticateUserCommand commandWithSyntheticSecret(String username) {
        return new AuthenticateUserCommand(username, "synthetic-secret-value-123".toCharArray());
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

    private static void assertCommandBufferZeroized(AuthenticateUserCommand command) {
        char[] remaining = command.passwordCopy();
        assertTrue(remaining.length > 0, "Expected non-empty secret buffer before zeroization check");
        for (char c : remaining) {
            assertEquals('\0', c, "Expected AuthenticateUserCommand secret buffer to be cleared to '\\0'");
        }
    }

    private static void assertPasswordVerifierBufferZeroized(char[] verifierBuffer) {
        assertNotNull(verifierBuffer);
        assertTrue(verifierBuffer.length > 0);
        for (char c : verifierBuffer) {
            assertEquals('\0', c, "Expected handler working password buffer to be cleared to '\\0'");
        }
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        Object instance = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxyObj, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxyObj == args[0];
                    case "hashCode" -> System.identityHashCode(proxyObj);
                    case "toString" -> type.getSimpleName() + " test proxy";
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
        private final AtomicInteger recordLoginCalls = new AtomicInteger();
        private final AtomicInteger verifierCalls = new AtomicInteger();
        private final AtomicBoolean recordedInsideTransaction = new AtomicBoolean(false);
        private final AtomicReference<Integer> recordedEmployeeId = new AtomicReference<>();
        private final AtomicReference<LocalDateTime> recordedLoginAt = new AtomicReference<>();
        private final AtomicReference<char[]> lastPasswordPassedToVerifier = new AtomicReference<>();
        private final TransactionProbe transactions = new TransactionProbe();
        private final AuthenticateUserHandler handler;

        private NhanVien employee;
        private List<String> roles = new ArrayList<>();
        private boolean passwordVerified;
        private RuntimeException lookupFailure;
        private RuntimeException roleFailure;
        private RuntimeException recordLoginFailure;

        private Fixture() {
            INhanVienRepository employees = proxy(INhanVienRepository.class, (method, args) -> switch (method) {
                case "getByUsername" -> {
                    usernameLookupCalls.incrementAndGet();
                    if (lookupFailure != null) {
                        throw lookupFailure;
                    }
                    yield Optional.ofNullable(employee);
                }
                case "getRoleNames" -> {
                    roleLookupCalls.incrementAndGet();
                    if (roleFailure != null) {
                        throw roleFailure;
                    }
                    yield roles;
                }
                case "recordSuccessfulLogin" -> {
                    recordLoginCalls.incrementAndGet();
                    recordedInsideTransaction.set(transactions.active.get());
                    recordedEmployeeId.set((Integer) args[0]);
                    recordedLoginAt.set((LocalDateTime) args[1]);
                    if (recordLoginFailure != null) {
                        throw recordLoginFailure;
                    }
                    yield null;
                }
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
