package vn.clinicmanagement.application.cls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.cls.OrderServiceHandlerTest.InMemoryServiceRepo;
import vn.clinicmanagement.application.cls.OrderServiceHandlerTest.RecordingAuditPort;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetServicesByVisitHandlerTest {

    private InMemoryServiceRepo serviceRepo;
    private RecordingAuditPort auditPort;
    private GetServicesByVisitHandler handler;

    @BeforeEach
    void setUp() {
        serviceRepo = new InMemoryServiceRepo();
        auditPort = new RecordingAuditPort();
        handler = new GetServicesByVisitHandler(serviceRepo, auditPort);
    }

    @Test
    @DisplayName("Doctor successfully queries services and lab results for a visit")
    void doctorQueriesServicesForVisit() {
        DanhMucDichVu catalog = new DanhMucDichVu();
        catalog.setId(50);
        catalog.setMaDichVu("XN-MAU-01");
        catalog.setTenDichVu("Tổng phân tích máu");
        catalog.setLoaiDichVu(LoaiDichVu.XET_NGHIEM);
        serviceRepo.catalogs.put(50, catalog);

        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(400);
        order.setMaLuotKham(100);
        order.setMaDichVu(50);
        order.setSoLuong(BigDecimal.ONE);
        order.setDonGia(new BigDecimal("120000"));
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CO_KET_QUA);
        serviceRepo.save(order);

        ChiTietKetQuaXetNghiem line = new ChiTietKetQuaXetNghiem();
        line.setMaChiDinhDichVu(400);
        line.setTenChiSo("WBC");
        line.setGiaTri("6.8");
        serviceRepo.saveKetQua(line);

        AuthorizationContext auth = new AuthorizationContext(
                10, 1, "DOCTOR", 1, 1, UUID.randomUUID());
        AuditActorContext actor = new AuditActorContext(
                10, 1, "DOCTOR", 1, 1, "WS-01", UUID.randomUUID());

        GetServicesByVisitQuery query = new GetServicesByVisitQuery(100, auth, actor);

        Result<List<ServiceOrderDto>> result = handler.handle(query);

        assertTrue(result.isSuccess());
        List<ServiceOrderDto> list = result.getValue().orElseThrow();
        assertEquals(1, list.size());
        ServiceOrderDto dto = list.get(0);
        assertEquals(400, dto.orderId());
        assertEquals("XN-MAU-01", dto.serviceCode());
        assertEquals("Tổng phân tích máu", dto.serviceName());
        assertEquals(1, dto.testResults().size());
        assertEquals("WBC", dto.testResults().get(0).indicatorName());

        assertEquals(1, auditPort.independentEntries.size());
        SystemAuditEntry entry = auditPort.independentEntries.get(0);
        assertEquals(AuditActionCode.PATIENT_VIEW, entry.actionCode());
        assertEquals(AuditResourceType.SERVICE_ORDER, entry.resourceType());
        assertEquals(AuditOutcome.SUCCESS, entry.outcome());
    }

    @Test
    @DisplayName("Receptionist can also view services for billing / patient coordination")
    void receptionistCanViewServices() {
        AuthorizationContext auth = new AuthorizationContext(
                20, 2, "RECEPTIONIST", 1, 1, UUID.randomUUID());

        GetServicesByVisitQuery query = new GetServicesByVisitQuery(100, auth, null);
        Result<List<ServiceOrderDto>> result = handler.handle(query);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Rejects unauthorized role or invalid visit ID")
    void rejectsInvalidRequests() {
        AuthorizationContext unauth = new AuthorizationContext(
                40, 5, "CASHIER", 1, 1, UUID.randomUUID());
        assertFalse(handler.handle(new GetServicesByVisitQuery(100, unauth, null)).isSuccess());

        AuthorizationContext auth = new AuthorizationContext(
                10, 1, "DOCTOR", 1, 1, UUID.randomUUID());
        assertFalse(handler.handle(new GetServicesByVisitQuery(0, auth, null)).isSuccess());
        assertFalse(handler.handle(null).isSuccess());
    }
}
