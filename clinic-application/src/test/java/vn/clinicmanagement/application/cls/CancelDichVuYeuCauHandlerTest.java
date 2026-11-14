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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancelDichVuYeuCauHandlerTest {

    private InMemoryServiceRepo serviceRepo;
    private RecordingAuditPort auditPort;
    private CancelDichVuYeuCauHandler handler;

    @BeforeEach
    void setUp() {
        serviceRepo = new InMemoryServiceRepo();
        auditPort = new RecordingAuditPort();
        handler = new CancelDichVuYeuCauHandler(serviceRepo, auditPort);
    }

    @Test
    @DisplayName("Doctor successfully cancels pending service order")
    void doctorCancelsPendingOrderSuccessfully() {
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(300);
        order.setMaLuotKham(100);
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
        serviceRepo.save(order);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                10, 1, "DOCTOR", 1, 1, UUID.randomUUID());
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                10, 1, "DOCTOR", 1, 1, "WS-01", UUID.randomUUID());

        CancelDichVuYeuCauCommand command = new CancelDichVuYeuCauCommand(
                300, "Bệnh nhân từ chối thực hiện", auth, actor);

        KetQua<CancelDichVuYeuCauBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess());
        CancelDichVuYeuCauBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(300, receipt.orderId());
        assertEquals(TrangThaiChiDinhDichVu.DA_HUY.name(), receipt.status());
        assertEquals("Bệnh nhân từ chối thực hiện", receipt.reason());

        ChiDinhDichVu updated = serviceRepo.getById(300).orElseThrow();
        assertEquals(TrangThaiChiDinhDichVu.DA_HUY, updated.getTrangThai());

        assertEquals(1, auditPort.independentEntries.size());
        HeThongKiemToanNhapLieu entry = auditPort.independentEntries.get(0);
        assertEquals(KiemToanHanhDongMa.SERVICE_CANCELLED, entry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.SERVICE_ORDER, entry.resourceType());
        assertEquals(KiemToanKetQua.SUCCESS, entry.outcome());
    }

    @Test
    @DisplayName("Cannot cancel order that already has results")
    void cannotCancelCompletedOrder() {
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(301);
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CO_KET_QUA);
        serviceRepo.save(order);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                10, 1, "DOCTOR", 1, 1, UUID.randomUUID());

        CancelDichVuYeuCauCommand command = new CancelDichVuYeuCauCommand(
                301, "Hủy", auth, null);

        KetQua<CancelDichVuYeuCauBienNhan> result = handler.handle(command);
        assertFalse(result.isSuccess());
        assertEquals("ORDER_ALREADY_COMPLETED", result.getErrorCode().orElse(""));
    }

    @Test
    @DisplayName("Cannot cancel order that is already cancelled")
    void cannotCancelAlreadyCancelledOrder() {
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(302);
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_HUY);
        serviceRepo.save(order);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                10, 1, "ADMIN", 1, 1, UUID.randomUUID());

        CancelDichVuYeuCauCommand command = new CancelDichVuYeuCauCommand(
                302, "Hủy tiếp", auth, null);

        KetQua<CancelDichVuYeuCauBienNhan> result = handler.handle(command);
        assertFalse(result.isSuccess());
        assertEquals("ORDER_ALREADY_CANCELLED", result.getErrorCode().orElse(""));
    }
}
