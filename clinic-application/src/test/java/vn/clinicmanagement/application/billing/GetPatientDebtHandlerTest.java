package vn.clinicmanagement.application.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetPatientDebtHandlerTest {

    private CreateInvoiceHandlerTest.InMemoryInvoiceRepo invoiceRepo;
    private CreateInvoiceHandlerTest.RecordingAuditPort auditPort;
    private GetPatientDebtHandler handler;

    @BeforeEach
    void setUp() {
        invoiceRepo = new CreateInvoiceHandlerTest.InMemoryInvoiceRepo();
        auditPort = new CreateInvoiceHandlerTest.RecordingAuditPort();
        handler = new GetPatientDebtHandler(invoiceRepo, auditPort);
    }

    @Test
    @DisplayName("Returns patient debt records with remaining debt and invoice number")
    void returnsPatientDebts() {
        HoaDon invoice = new HoaDon();
        invoice.setId(20);
        invoice.setSoHoaDon("HD261107-1020");
        invoiceRepo.save(invoice);

        CongNoBenhNhan debt = new CongNoBenhNhan();
        debt.setId(1);
        debt.setMaHoaDon(20);
        debt.setMaBenhNhan(501);
        debt.setSoTienNo(new BigDecimal("400000"));
        debt.setSoTienDaTra(new BigDecimal("150000"));
        debt.setHanThanhToan(LocalDate.now().plusDays(10));
        debt.setTrangThai(TrangThaiCongNo.DANG_NO);
        invoiceRepo.saveCongNo(debt);

        AuthorizationContext auth = new AuthorizationContext(
                40, 5, "CASHIER", 1, 1, UUID.randomUUID());
        GetPatientDebtQuery query = new GetPatientDebtQuery(501, auth, null);

        Result<List<PatientDebtDto>> result = handler.handle(query);
        assertTrue(result.isSuccess());

        List<PatientDebtDto> dtos = result.getValue().orElseThrow();
        assertEquals(1, dtos.size());
        PatientDebtDto d = dtos.get(0);
        assertEquals(20, d.invoiceId());
        assertEquals("HD261107-1020", d.invoiceNumber());
        assertEquals(new BigDecimal("400000"), d.totalDebtAmount());
        assertEquals(new BigDecimal("150000"), d.paidAmount());
        assertEquals(new BigDecimal("250000"), d.remainingDebt());
        assertEquals(TrangThaiCongNo.DANG_NO, d.status());
    }

    @Test
    @DisplayName("Empty debts returns empty list")
    void emptyDebtsReturnsEmptyList() {
        AuthorizationContext auth = new AuthorizationContext(
                40, 5, "CASHIER", 1, 1, UUID.randomUUID());
        GetPatientDebtQuery query = new GetPatientDebtQuery(999, auth, null);

        Result<List<PatientDebtDto>> result = handler.handle(query);
        assertTrue(result.isSuccess());
        assertTrue(result.getValue().orElseThrow().isEmpty());
    }

    @Test
    @DisplayName("Unauthorized role is denied")
    void unauthorizedRoleIsDenied() {
        AuthorizationContext auth = new AuthorizationContext(
                50, 6, "PHARMACIST", 1, 1, UUID.randomUUID());
        GetPatientDebtQuery query = new GetPatientDebtQuery(501, auth, null);

        Result<List<PatientDebtDto>> result = handler.handle(query);
        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElse(""));
    }
}
