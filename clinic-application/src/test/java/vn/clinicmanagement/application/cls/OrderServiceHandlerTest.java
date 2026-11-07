package vn.clinicmanagement.application.cls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.repositories.ISpecification;
import vn.clinicmanagement.domain.repositories.PagedResult;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderServiceHandlerTest {

    private InMemoryServiceRepo serviceRepo;
    private InMemoryVisitRepo visitRepo;
    private RecordingAuditPort auditPort;
    private OrderServiceHandler handler;

    @BeforeEach
    void setUp() {
        serviceRepo = new InMemoryServiceRepo();
        visitRepo = new InMemoryVisitRepo();
        auditPort = new RecordingAuditPort();
        handler = new OrderServiceHandler(serviceRepo, visitRepo, auditPort);
    }

    @Test
    @DisplayName("Doctor successfully orders active lab service for an active visit")
    void doctorOrdersActiveServiceSuccessfully() {
        LuotKham visit = new LuotKham();
        visit.setId(100);
        visit.setTrangThai(TrangThaiLuotKham.DANG_KHAM);
        visitRepo.save(visit);

        DanhMucDichVu catalog = new DanhMucDichVu();
        catalog.setId(50);
        catalog.setMaDichVu("XN-MAU-01");
        catalog.setTenDichVu("Tổng phân tích tế bào máu");
        catalog.setLoaiDichVu(LoaiDichVu.XET_NGHIEM);
        catalog.setDonGia(new BigDecimal("120000"));
        catalog.setHoatDong(true);
        serviceRepo.catalogs.put(50, catalog);

        AuthorizationContext auth = new AuthorizationContext(
                10, 1, "DOCTOR", 1, 1, UUID.randomUUID());
        AuditActorContext actor = new AuditActorContext(
                10, 1, "DOCTOR", 1, 1, "WS-01", UUID.randomUUID());

        OrderServiceCommand command = new OrderServiceCommand(
                100, 50, BigDecimal.ONE, 5, "Xét nghiệm trước khi kê đơn", auth, actor);

        Result<OrderServiceReceipt> result = handler.handle(command);

        assertTrue(result.isSuccess());
        OrderServiceReceipt receipt = result.getValue().orElseThrow();
        assertEquals(100, receipt.visitId());
        assertEquals(50, receipt.serviceId());
        assertEquals("Tổng phân tích tế bào máu", receipt.serviceName());
        assertEquals(new BigDecimal("120000"), receipt.unitPrice());
        assertEquals(TrangThaiChiDinhDichVu.DA_CHI_DINH.name(), receipt.status());

        assertEquals(1, auditPort.independentEntries.size());
        SystemAuditEntry entry = auditPort.independentEntries.get(0);
        assertEquals(AuditActionCode.SERVICE_ORDERED, entry.actionCode());
        assertEquals(AuditResourceType.SERVICE_ORDER, entry.resourceType());
        assertEquals(AuditOutcome.SUCCESS, entry.outcome());
    }

    @Test
    @DisplayName("Rejects order if user is not DOCTOR or ADMIN")
    void rejectsUnauthorizedRole() {
        AuthorizationContext auth = new AuthorizationContext(
                20, 2, "RECEPTIONIST", 1, 1, UUID.randomUUID());
        AuditActorContext actor = new AuditActorContext(
                20, 2, "RECEPTIONIST", 1, 1, "WS-01", UUID.randomUUID());

        OrderServiceCommand command = new OrderServiceCommand(
                100, 50, BigDecimal.ONE, 5, "Note", auth, actor);

        Result<OrderServiceReceipt> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("AUTH_ACCESS_DENIED", result.getErrorCode().orElse(""));
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, auditPort.independentEntries.get(0).actionCode());
    }

    @Test
    @DisplayName("Rejects order if visit is cancelled")
    void rejectsOrderForCancelledVisit() {
        LuotKham visit = new LuotKham();
        visit.setId(100);
        visit.setTrangThai(TrangThaiLuotKham.DA_HUY);
        visitRepo.save(visit);

        AuthorizationContext auth = new AuthorizationContext(
                10, 1, "DOCTOR", 1, 1, UUID.randomUUID());

        OrderServiceCommand command = new OrderServiceCommand(
                100, 50, BigDecimal.ONE, null, null, auth, null);

        Result<OrderServiceReceipt> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("VISIT_CANCELLED", result.getErrorCode().orElse(""));
    }

    @Test
    @DisplayName("Rejects order if service is inactive")
    void rejectsInactiveService() {
        LuotKham visit = new LuotKham();
        visit.setId(100);
        visit.setTrangThai(TrangThaiLuotKham.DANG_KHAM);
        visitRepo.save(visit);

        DanhMucDichVu catalog = new DanhMucDichVu();
        catalog.setId(50);
        catalog.setTenDichVu("Dịch vụ ngưng");
        catalog.setHoatDong(false);
        serviceRepo.catalogs.put(50, catalog);

        AuthorizationContext auth = new AuthorizationContext(
                10, 1, "ADMIN", 1, 1, UUID.randomUUID());

        OrderServiceCommand command = new OrderServiceCommand(
                100, 50, BigDecimal.ONE, null, null, auth, null);

        Result<OrderServiceReceipt> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("SERVICE_INACTIVE", result.getErrorCode().orElse(""));
    }

    @Test
    @DisplayName("Validates input constraints")
    void validatesInputConstraints() {
        AuthorizationContext auth = new AuthorizationContext(
                10, 1, "DOCTOR", 1, 1, UUID.randomUUID());

        assertFalse(handler.handle(null).isSuccess());
        assertFalse(handler.handle(new OrderServiceCommand(0, 50, BigDecimal.ONE, null, null, auth, null)).isSuccess());
        assertFalse(handler.handle(new OrderServiceCommand(100, 0, BigDecimal.ONE, null, null, auth, null)).isSuccess());
        assertFalse(handler.handle(new OrderServiceCommand(100, 50, BigDecimal.ZERO, null, null, auth, null)).isSuccess());
        assertFalse(handler.handle(new OrderServiceCommand(100, 50, new BigDecimal("-1"), null, null, auth, null)).isSuccess());
    }

    static class InMemoryServiceRepo implements IChiDinhDichVuRepository {
        Map<Integer, ChiDinhDichVu> orders = new HashMap<>();
        Map<Integer, DanhMucDichVu> catalogs = new HashMap<>();
        List<ChiTietKetQuaXetNghiem> results = new ArrayList<>();
        private int seq = 1;

        @Override public List<ChiDinhDichVu> getByLuotKham(int maLuotKham) {
            List<ChiDinhDichVu> list = new ArrayList<>();
            for (ChiDinhDichVu o : orders.values()) {
                if (o.getMaLuotKham() == maLuotKham) list.add(o);
            }
            return list;
        }

        @Override public List<ChiTietKetQuaXetNghiem> getKetQuaByChiDinh(int maChiDinh) {
            List<ChiTietKetQuaXetNghiem> list = new ArrayList<>();
            for (ChiTietKetQuaXetNghiem r : results) {
                if (r.getMaChiDinhDichVu() == maChiDinh) list.add(r);
            }
            return list;
        }

        @Override public void saveKetQua(ChiTietKetQuaXetNghiem ketQua) {
            if (ketQua.getId() == null) ketQua.setId(seq++);
            results.add(ketQua);
        }

        @Override public Optional<DanhMucDichVu> getDichVuById(int maDichVu) {
            return Optional.ofNullable(catalogs.get(maDichVu));
        }

        @Override public List<DanhMucDichVu> searchDichVu(String keyword, LoaiDichVu loaiDichVu) {
            return new ArrayList<>(catalogs.values());
        }

        @Override public Optional<ChiDinhDichVu> getById(int id) {
            return Optional.ofNullable(orders.get(id));
        }

        @Override public void save(ChiDinhDichVu entity) {
            if (entity.getId() == null) entity.setId(seq++);
            orders.put(entity.getId(), entity);
        }

        @Override public void delete(ChiDinhDichVu entity) {
            if (entity.getId() != null) orders.remove(entity.getId());
        }

        @Override public List<ChiDinhDichVu> getAll() { return new ArrayList<>(orders.values()); }
        @Override public List<ChiDinhDichVu> find(ISpecification<ChiDinhDichVu> spec) { return List.of(); }
        @Override public PagedResult<ChiDinhDichVu> getPaged(int pageIndex, int pageSize, ISpecification<ChiDinhDichVu> specification) { return null; }
        @Override public boolean exists(ISpecification<ChiDinhDichVu> specification) { return false; }
        @Override public int count(ISpecification<ChiDinhDichVu> specification) { return orders.size(); }
        @Override public void add(ChiDinhDichVu entity) { save(entity); }
        @Override public void update(ChiDinhDichVu entity) { save(entity); }
        @Override public vn.clinicmanagement.domain.common.IUnitOfWork getUnitOfWork() { return null; }
    }

    static class InMemoryVisitRepo implements ILuotKhamRepository {
        Map<Integer, LuotKham> storage = new HashMap<>();
        @Override public Optional<LuotKham> getById(int id) { return Optional.ofNullable(storage.get(id)); }
        @Override public void add(LuotKham entity) { storage.put(entity.getId(), entity); }
        @Override public void update(LuotKham entity) { storage.put(entity.getId(), entity); }
        @Override public void delete(LuotKham entity) { storage.remove(entity.getId()); }
        @Override public List<LuotKham> getAll() { return new ArrayList<>(storage.values()); }
        @Override public List<LuotKham> find(ISpecification<LuotKham> spec) { return List.of(); }
        @Override public PagedResult<LuotKham> getPaged(int pageIndex, int pageSize, ISpecification<LuotKham> spec) { return null; }
        @Override public boolean exists(ISpecification<LuotKham> spec) { return false; }
        @Override public int count(ISpecification<LuotKham> spec) { return storage.size(); }
        @Override public vn.clinicmanagement.domain.common.IUnitOfWork getUnitOfWork() { return null; }
        @Override public List<LuotKham> getVisitsByDoctor(int maBacSi, java.time.LocalDate date) { return List.of(); }
        @Override public int getNextQueueNumber(int maCoSo, int maKhoa, java.time.LocalDate date) { return 1; }
        public void save(LuotKham entity) { storage.put(entity.getId(), entity); }
    }

    static class RecordingAuditPort implements AuditWritePort {
        List<SystemAuditEntry> independentEntries = new ArrayList<>();
        @Override public void appendInCurrentTransaction(SystemAuditEntry entry) {}
        @Override public void appendIndependently(SystemAuditEntry entry) { independentEntries.add(entry); }
        @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}
        @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
    }
}
