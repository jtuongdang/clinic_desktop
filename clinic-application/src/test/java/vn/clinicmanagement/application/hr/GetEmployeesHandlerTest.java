package vn.clinicmanagement.application.hr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetEmployeesHandlerTest extends HrHandlerTestSupport {
    @Test
    void returnsOnlyEmployeesMatchingRequestedStatus() {
        employees.add(employee(1, "NV-1", "active", true));
        employees.add(employee(2, "NV-2", "inactive", false));
        employees.add(employee(3, "NV-3", "deleted", true));
        employees.getById(3).orElseThrow().setDeleted(true);

        var result = new GetEmployeesHandler(employees).handle(new GetEmployeesQuery(false));

        assertTrue(result.isSuccess());
        assertEquals(1, result.valueOrThrow().size());
        assertEquals("NV-2", result.valueOrThrow().get(0).maNhanVien());
    }

    @Test
    void nullStatusReturnsAllNonDeletedEmployees() {
        employees.add(employee(1, "NV-1", "active", true));
        employees.add(employee(2, "NV-2", "inactive", false));

        var result = new GetEmployeesHandler(employees).handle(new GetEmployeesQuery(null));

        assertEquals(2, result.valueOrThrow().size());
    }
}
