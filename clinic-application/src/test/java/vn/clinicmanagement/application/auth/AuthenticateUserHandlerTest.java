package vn.clinicmanagement.application.auth;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticateUserHandlerTest {
    private static final int EMPLOYEE_ID = 701;
    private static final String USERNAME = "synth.user.701";
    private static final String HASH = "synthetic-hash";

    @Test
    void returnsEveryRoleAndScopeOnlyAfterVerifiedCredentialsWithoutChoosingFirst() {
        Fixture fixture = new Fixture();
        fixture.roles = List.of(role(31, "admin"), role(42, "doctor"));
        fixture.scopes = List.of(new NhanVienWorkScope(11, "Cơ sở mẫu A", 21, "Khoa mẫu A"),
                new NhanVienWorkScope(12, "Cơ sở mẫu B", 22, "Khoa mẫu B"));

        Result<AuthenticatedUser> result = fixture.authenticate();

        assertTrue(result.isSuccess());
        AuthenticatedUser user = result.valueOrThrow();
        assertEquals(List.of(new AuthenticatedUser.RoleChoice(31, "ADMIN", "Quản trị viên (Admin)"),
                new AuthenticatedUser.RoleChoice(42, "DOCTOR", "Bác sĩ điều trị (Doctor Station)")), user.roles());
        assertEquals(List.of(new AuthenticatedUser.WorkScopeChoice(11, "Cơ sở mẫu A", 21, "Khoa mẫu A"),
                new AuthenticatedUser.WorkScopeChoice(12, "Cơ sở mẫu B", 22, "Khoa mẫu B")), user.workScopes());
        assertEquals(1, fixture.assignmentLookups.get());
        assertEquals(1, fixture.scopeLookups.get());
        assertEquals(1, fixture.loginRecords.get());
        assertEquals(1, fixture.transactions.calls.get());
        assertPasswordBuffersCleared(fixture.command, fixture.verifierBuffer.get());
        assertThrows(UnsupportedOperationException.class, () -> user.roles().clear());
        assertThrows(UnsupportedOperationException.class, () -> user.workScopes().clear());
    }

    @Test
    void assignmentQueriesDoNotRunBeforeCredentialsAreVerified() {
        Fixture fixture = new Fixture();
        fixture.passwordVerified = false;

        Result<AuthenticatedUser> result = fixture.authenticate();

        assertTrue(result.isFailure());
        assertEquals("AUTH_INVALID", result.getError().orElseThrow().code());
        assertEquals(0, fixture.assignmentLookups.get());
        assertEquals(0, fixture.loginRecords.get());
        assertPasswordBuffersCleared(fixture.command, fixture.verifierBuffer.get());
    }

    @Test
    void missingRoleOrScopeFailsClosedWithoutRecordingSuccessfulLogin() {
        Fixture noRoles = new Fixture();
        noRoles.roles = List.of();
        Result<AuthenticatedUser> noRoleResult = noRoles.authenticate();
        assertEquals("AUTH_NO_ROLE", noRoleResult.getError().orElseThrow().code());
        assertEquals(0, noRoles.loginRecords.get());
        assertEquals(0, noRoles.scopeLookups.get());

        Fixture noScopes = new Fixture();
        noScopes.roles = List.of(role(31, "ADMIN"));
        noScopes.scopes = List.of();
        Result<AuthenticatedUser> noScopeResult = noScopes.authenticate();
        assertEquals("AUTH_NO_SCOPE", noScopeResult.getError().orElseThrow().code());
        assertEquals(1, noScopes.roleLookups.get());
        assertEquals(1, noScopes.scopeLookups.get());
        assertEquals(0, noScopes.loginRecords.get());
        assertEquals(0, noScopes.transactions.calls.get());
    }

    @Test
    void zeroizesPasswordBuffersEvenWhenAssignmentsCannotBeLoaded() {
        Fixture fixture = new Fixture();
        fixture.roleFailure = new IllegalStateException("synthetic repository failure");

        Result<AuthenticatedUser> result = fixture.authenticate();

        assertEquals("AUTH_UNAVAILABLE", result.getError().orElseThrow().code());
        assertPasswordBuffersCleared(fixture.command, fixture.verifierBuffer.get());
        assertEquals(0, fixture.loginRecords.get());
    }

    private static VaiTro role(int id, String code) {
        VaiTro role = new VaiTro();
        role.setId(id);
        role.setTenVaiTro(code);
        return role;
    }

    private static AuthenticateUserCommand command() { return new AuthenticateUserCommand(USERNAME, "synthetic-pass".toCharArray()); }

    private static void assertPasswordBuffersCleared(AuthenticateUserCommand command, char[] verifierBuffer) {
        assertCleared(command.passwordCopy());
        assertNotNull(verifierBuffer);
        assertCleared(verifierBuffer);
    }

    private static void assertCleared(char[] chars) {
        assertTrue(chars.length > 0);
        for (char c : chars) assertEquals('\0', c);
    }

    private static final class Fixture {
        private final AtomicInteger roleLookups = new AtomicInteger();
        private final AtomicInteger scopeLookups = new AtomicInteger();
        private final AtomicInteger assignmentLookups = new AtomicInteger();
        private final AtomicInteger loginRecords = new AtomicInteger();
        private final AtomicReference<char[]> verifierBuffer = new AtomicReference<>();
        private final Probe transactions = new Probe();
        private AuthenticateUserCommand command;
        private final AuthenticateUserHandler handler;
        private List<VaiTro> roles = List.of(role(31, "ADMIN"));
        private List<NhanVienWorkScope> scopes = List.of(new NhanVienWorkScope(11, "Cơ sở mẫu", 21, "Khoa mẫu"));
        private boolean passwordVerified = true;
        private RuntimeException roleFailure;

        private Fixture() {
            NhanVien employee = new NhanVien();
            employee.setId(EMPLOYEE_ID);
            employee.setUsername(USERNAME);
            employee.setPasswordHash(HASH);
            employee.setHo("Nhân viên");
            employee.setTen("Mẫu");
            employee.setHoatDong(true);

            INhanVienRepository repository = (INhanVienRepository) Proxy.newProxyInstance(
                    INhanVienRepository.class.getClassLoader(), new Class<?>[]{INhanVienRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getByUsername" -> Optional.of(employee);
                        case "getAssignedRoles" -> {
                            roleLookups.incrementAndGet(); assignmentLookups.incrementAndGet();
                            if (roleFailure != null) throw roleFailure;
                            yield roles;
                        }
                        case "getAssignedWorkScopes" -> { scopeLookups.incrementAndGet(); yield scopes; }
                        case "recordSuccessfulLogin" -> { loginRecords.incrementAndGet(); yield null; }
                        case "getRoleNames" -> List.of();
                        case "getById" -> Optional.empty();
                        case "getAll", "getActiveDoctors", "find" -> List.of();
                        case "exists" -> false;
                        case "count" -> 0;
                        default -> null;
                    });
            handler = new AuthenticateUserHandler(repository, (password, hash) -> {
                verifierBuffer.set(password);
                return passwordVerified && HASH.equals(hash);
            }, transactions);
        }

        private Result<AuthenticatedUser> authenticate() {
            command = command();
            return handler.handle(command);
        }
    }

    private static final class Probe implements TransactionBoundary {
        private final AtomicInteger calls = new AtomicInteger();
        @Override public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
            calls.incrementAndGet();
            return operation.get();
        }
    }
}
