package vn.clinicmanagement.application.hr;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.auth.Pbkdf2PasswordHasher;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.identity.NhanVien;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CreateEmployeeHandlerTest extends HrHandlerTestSupport {
    private CreateEmployeeHandler handler() {
        return new CreateEmployeeHandler(employees, password -> "synthetic-hash",
                authorization, auditLog, transactions);
    }

    @Test
    void createsEmployeeAndAuditsWithoutPersistingPlaintextPassword() {
        char[] originalPassword = "synthetic-password".toCharArray();
        CreateEmployeeCommand command = new CreateEmployeeCommand(7, "NV-NEW", "Tran", "Synthetic",
                "0000000000", "synthetic@example.test", 3, "New.User", originalPassword);

        Result<CreateEmployeeReceipt> result = handler().handle(command);

        assertTrue(result.isSuccess());
        assertEquals(1, employees.getAll().size());
        NhanVien saved = employees.getAll().get(0);
        assertEquals("new.user", saved.getUsername());
        assertEquals("synthetic-hash", saved.getPasswordHash());
        assertFalse(Arrays.equals(command.passwordCopy(), "synthetic-password".toCharArray()));
        assertEquals(HrAuditAction.USER_CREATED, audits.get(0).action());
        assertEquals(saved.getId(), result.valueOrThrow().employeeId());
    }

    @Test
    void rejectsDuplicateEmployeeCodeAndUsername() {
        employees.add(employee(1, "NV-1", "existing", true));
        CreateEmployeeCommand duplicateCode = new CreateEmployeeCommand(7, "NV-1", "Tran", "A",
                "", null, null, "different", "pass".toCharArray());
        CreateEmployeeCommand duplicateUsername = new CreateEmployeeCommand(7, "NV-2", "Tran", "B",
                "", null, null, "EXISTING", "pass".toCharArray());

        assertEquals("HR_DUPLICATE_EMPLOYEE_CODE", handler().handle(duplicateCode)
                .getError().orElseThrow().code());
        assertEquals("HR_DUPLICATE_USERNAME", handler().handle(duplicateUsername)
                .getError().orElseThrow().code());
        assertEquals(1, employees.getAll().size());
        assertTrue(audits.isEmpty());
    }

    @Test
    void deniesUnauthorisedCreate() {
        authorized = false;
        CreateEmployeeCommand command = new CreateEmployeeCommand(7, "NV-NEW", "Tran", "A",
                "", null, null, "new", "pass".toCharArray());

        assertEquals("HR_FORBIDDEN", handler().handle(command).getError().orElseThrow().code());
        assertTrue(employees.getAll().isEmpty());
        assertTrue(audits.isEmpty());
    }

    @Test
    void createsPasswordHashCompatibleWithAuthenticationVerifier() {
        char[] password = "synthetic-password".toCharArray();
        String encoded = new Pbkdf2EmployeePasswordHasher().hash(password);

        assertTrue(new Pbkdf2PasswordHasher().verify(password, encoded));
    }
}
