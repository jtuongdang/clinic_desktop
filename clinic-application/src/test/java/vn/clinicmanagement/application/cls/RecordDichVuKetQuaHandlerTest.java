package vn.clinicmanagement.application.cls;

import vn.clinicmanagement.application.cls.record.RecordDichVuKetQuaBienNhan;
import vn.clinicmanagement.application.cls.record.RecordDichVuKetQuaCommand;
import vn.clinicmanagement.application.cls.record.RecordDichVuKetQuaHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.cls.YeuCauDichVuHandlerTest.InMemoryServiceRepo;
import vn.clinicmanagement.application.cls.YeuCauDichVuHandlerTest.RecordingAuditPort;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordDichVuKetQuaHandlerTest {

    private InMemoryServiceRepo serviceRepo;
    private RecordingAuditPort auditPort;
    private RecordDichVuKetQuaHandler handler;

    @BeforeEach
    void setUp() {
        serviceRepo = new InMemoryServiceRepo();
        auditPort = new RecordingAuditPort();
        handler = new RecordDichVuKetQuaHandler(serviceRepo, auditPort);
    }

    @Test
    @DisplayName("Lab tech successfully records lab test results")
    void labTechRecordsResultsSuccessfully() {
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(200);
        order.setMaLuotKham(100);
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
        serviceRepo.save(order);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                30, 6, "LAB_TECH", 1, 1, UUID.randomUUID());
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                30, 6, "LAB_TECH", 1, 1, "WS-LAB-01", UUID.randomUUID());

        List<XetNghiemKetQuaMatHangDauVao> items = List.of(
                new XetNghiemKetQuaMatHangDauVao("WBC", "7.5", "G/L", "4.0 - 10.0", "Bình thường"),
                new XetNghiemKetQuaMatHangDauVao("RBC", "4.8", "T/L", "4.2 - 5.4", "Bình thường")
        );

        RecordDichVuKetQuaCommand command = new RecordDichVuKetQuaCommand(
                200, "Mẫu máu đạt tiêu chuẩn", "Các chỉ số trong giới hạn bình thường",
                "https://pacs.clinic.local/img/lab-200.pdf", items, auth, actor);

        KetQua<RecordDichVuKetQuaBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess());
        RecordDichVuKetQuaBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(200, receipt.orderId());
        assertEquals(100, receipt.visitId());
        assertEquals(TrangThaiChiDinhDichVu.DA_CO_KET_QUA.name(), receipt.status());
        assertEquals(30, receipt.performedBy());
        assertEquals(2, receipt.resultItemCount());

        ChiDinhDichVu updated = serviceRepo.getById(200).orElseThrow();
        assertEquals(TrangThaiChiDinhDichVu.DA_CO_KET_QUA, updated.getTrangThai());
        assertEquals("Mẫu máu đạt tiêu chuẩn", updated.getKetQua());
        assertEquals("Các chỉ số trong giới hạn bình thường", updated.getKetLuan());
        assertEquals(Integer.valueOf(30), updated.getBacSiThucHien());

        List<ChiTietKetQuaXetNghiem> results = serviceRepo.getKetQuaByChiDinh(200);
        assertEquals(2, results.size());
        assertEquals("WBC", results.get(0).getTenChiSo());
        assertEquals("7.5", results.get(0).getGiaTri());

        assertEquals(1, auditPort.independentEntries.size());
        HeThongKiemToanNhapLieu entry = auditPort.independentEntries.get(0);
        assertEquals(KiemToanHanhDongMa.SERVICE_RESULT_RECORDED, entry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.SERVICE_ORDER, entry.resourceType());
        assertEquals(KiemToanKetQua.SUCCESS, entry.outcome());
    }

    @Test
    @DisplayName("Doctor can also record service result")
    void doctorCanAlsoRecordResult() {
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(201);
        order.setMaLuotKham(100);
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
        serviceRepo.save(order);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                10, 1, "DOCTOR", 1, 1, UUID.randomUUID());

        RecordDichVuKetQuaCommand command = new RecordDichVuKetQuaCommand(
                201, "Nội soi dạ dày", "Viêm hang vị nhẹ", null, null, auth, null);

        KetQua<RecordDichVuKetQuaBienNhan> result = handler.handle(command);
        assertTrue(result.isSuccess());
    }

    @Test
    void rejectsBlankResultWithoutCompletingTheOrder() {
        var order = new ChiDinhDichVu();
        order.setId(203);
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
        serviceRepo.save(order);
        var auth = new PhanQuyenNguCanh(10, 1, "DOCTOR", 1, 1, UUID.randomUUID());

        var result = handler.handle(new RecordDichVuKetQuaCommand(203, " ", "", null, null, auth, null));

        assertEquals("RESULT_REQUIRED", result.getErrorCode().orElseThrow());
        assertEquals(TrangThaiChiDinhDichVu.DA_CHI_DINH, serviceRepo.getById(203).orElseThrow().getTrangThai());
    }

    @Test
    @DisplayName("Rejects unauthorized role such as RECEPTIONIST")
    void rejectsUnauthorizedRole() {
        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                20, 2, "RECEPTIONIST", 1, 1, UUID.randomUUID());

        RecordDichVuKetQuaCommand command = new RecordDichVuKetQuaCommand(
                200, "Ket qua", "Ket luan", null, null, auth, null);

        KetQua<RecordDichVuKetQuaBienNhan> result = handler.handle(command);
        assertFalse(result.isSuccess());
        assertEquals("AUTH_ACCESS_DENIED", result.getErrorCode().orElse(""));
    }

    @Test
    @DisplayName("Rejects recording result for cancelled order")
    void rejectsCancelledOrder() {
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(202);
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_HUY);
        serviceRepo.save(order);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                30, 6, "LAB_TECH", 1, 1, UUID.randomUUID());

        RecordDichVuKetQuaCommand command = new RecordDichVuKetQuaCommand(
                202, "Ket qua", "Ket luan", null, null, auth, null);

        KetQua<RecordDichVuKetQuaBienNhan> result = handler.handle(command);
        assertFalse(result.isSuccess());
        assertEquals("ORDER_CANCELLED", result.getErrorCode().orElse(""));
    }

    @Test
    void repeatedDraftsDoNotConsumeAndApprovalConsumesOnce() {
        var stock = new RecordingConsumption();
        var subject = new RecordDichVuKetQuaHandler(serviceRepo, null, null, auditPort, stock);
        var order = new ChiDinhDichVu();
        order.setId(210);
        order.setTrangThai(TrangThaiChiDinhDichVu.DANG_THUC_HIEN);
        serviceRepo.save(order);
        var auth = new PhanQuyenNguCanh(30, 6, "LAB_TECH", 1, 1, UUID.randomUUID());
        var operation = UUID.randomUUID();
        var actual = List.of(new vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.SoLuongThucTe(
                1, 2, new java.math.BigDecimal("0.25")));

        for (int i = 0; i < 2; i++) {
            var draft = subject.handle(new RecordDichVuKetQuaCommand(
                    210, "Kết quả nháp", "", null, null, auth, null, actual, operation, false));
            assertTrue(draft.isSuccess());
            assertEquals(0, stock.calls);
            assertEquals(TrangThaiChiDinhDichVu.DANG_THUC_HIEN, order.getTrangThai());
            org.junit.jupiter.api.Assertions.assertNull(order.getThoiGianTraKetQua());
            org.junit.jupiter.api.Assertions.assertNull(draft.getValue().orElseThrow().returnedAt());
        }

        var approval = new RecordDichVuKetQuaCommand(
                210, "Kết quả cuối", "", null, null, auth, null, actual, operation, true);
        assertTrue(subject.handle(approval).isSuccess());
        assertEquals(1, stock.calls);
        assertEquals(actual, stock.actual);
        assertEquals(TrangThaiChiDinhDichVu.DA_CO_KET_QUA, order.getTrangThai());
        assertFalse(subject.handle(approval).isSuccess());
        assertEquals(1, stock.calls);
    }

    @Test
    void shortageAllowsFinalResultButReturnsPendingConsumption() {
        var stock = new RecordingConsumption();
        stock.status = "PENDING";
        var subject = new RecordDichVuKetQuaHandler(serviceRepo, null, null, auditPort, stock);
        var order = new ChiDinhDichVu();
        order.setId(211);
        order.setTrangThai(TrangThaiChiDinhDichVu.DANG_THUC_HIEN);
        serviceRepo.save(order);
        var auth = new PhanQuyenNguCanh(30, 6, "LAB_TECH", 1, 1, UUID.randomUUID());
        var result = subject.handle(new RecordDichVuKetQuaCommand(
                211, "Kết quả", "", null, null, auth, null, List.of(), UUID.randomUUID(), true));
        assertTrue(result.isSuccess());
        assertEquals("PENDING", result.getValue().orElseThrow().consumptionStatus());
        assertEquals(1, stock.calls);
    }

    @Test
    void roundingOnlyChangesSuggestedQuantityAndKeepsOriginalStandard() {
        var standard = new java.math.BigDecimal("0.25");
        var definition = new vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.CauHinh(
                1, 2, "VT", "Vật tư", "Cái", standard, true);
        assertEquals(java.math.BigDecimal.ONE, definition.suggestedQuantity());
        assertEquals(standard, definition.standardQuantity());
        assertEquals(standard, new vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.CauHinh(
                1, 2, "VT", "Vật tư", "ml", standard, false).suggestedQuantity());
    }

    private static final class RecordingConsumption implements vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi {
        int calls;
        String status = "POSTED";
        List<SoLuongThucTe> actual;
        public List<CauHinh> getDefinition(PhanQuyenNguCanh context, int order) { return List.of(); }
        public KetQua<BienNhan> record(PhanQuyenNguCanh context, int order, UUID operation, List<SoLuongThucTe> values, String reason) {
            calls++;
            actual = values;
            return KetQua.success(new BienNhan(1, status, "POSTED".equals(status) ? values.size() : 0));
        }
        public List<ChoXuLy> getPending(PhanQuyenNguCanh context) { return List.of(); }
        public KetQua<BienNhan> retryPending(PhanQuyenNguCanh context, long id) { throw new UnsupportedOperationException(); }
        public List<BaoCaoDong> getReport(PhanQuyenNguCanh context, LocalDateTime from, LocalDateTime to) { return List.of(); }
    }
}
