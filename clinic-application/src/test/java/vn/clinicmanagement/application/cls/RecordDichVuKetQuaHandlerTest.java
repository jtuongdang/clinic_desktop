package vn.clinicmanagement.application.cls;

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
}
