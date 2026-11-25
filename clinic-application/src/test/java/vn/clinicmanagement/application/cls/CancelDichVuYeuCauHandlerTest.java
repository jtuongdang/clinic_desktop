package vn.clinicmanagement.application.cls;

import vn.clinicmanagement.application.cls.cancel.CancelDichVuYeuCauBienNhan;
import vn.clinicmanagement.application.cls.cancel.CancelDichVuYeuCauCommand;
import vn.clinicmanagement.application.cls.cancel.CancelDichVuYeuCauHandler;

import vn.clinicmanagement.application.cls.YeuCauDichVuHandlerTest.RecordingAuditPort;

import vn.clinicmanagement.application.cls.YeuCauDichVuHandlerTest.InMemoryServiceRepo;
import vn.clinicmanagement.application.cls.YeuCauDichVuHandlerTest.InMemoryVisitRepo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.DanhMucDichVu;
import vn.clinicmanagement.domain.service.ThietLapThucHienDichVu;

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

        assertEquals(1, auditPort.transactionEntries.size());
        HeThongKiemToanNhapLieu entry = auditPort.transactionEntries.get(0);
        assertEquals(KiemToanHanhDongMa.SERVICE_CANCELLED, entry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.SERVICE_ORDER, entry.resourceType());
        assertEquals(KiemToanKetQua.SUCCESS, entry.outcome());
    }

    @Test
    void rejectsCancellationWhenTheCatalogBlocksIt() {
        var configuredRepo = new InMemoryServiceRepo() {
            @Override public ThietLapThucHienDichVu getExecutionSettings(int orderId) {
                assertEquals(303, orderId);
                return new ThietLapThucHienDichVu(true, false, "", "", "", "", "");
            }
        };
        var order = new ChiDinhDichVu();
        order.setId(303);
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
        configuredRepo.save(order);
        var auth = new PhanQuyenNguCanh(10, 1, "DOCTOR", 1, 1, UUID.randomUUID());

        var result = new CancelDichVuYeuCauHandler(configuredRepo, auditPort)
                .handle(new CancelDichVuYeuCauCommand(303, "Kiểm tra", auth, null));

        assertEquals("SERVICE_CANCELLATION_BLOCKED", result.getErrorCode().orElseThrow());
        assertEquals(TrangThaiChiDinhDichVu.DA_CHI_DINH, configuredRepo.getById(303).orElseThrow().getTrangThai());
        assertTrue(auditPort.transactionEntries.isEmpty());
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

    @Test
    @DisplayName("Restores CHO_CLS visit back to DANG_KHAM only when all pending CLS orders are cancelled")
    void restoresVisitToExaminingWhenAllPendingClsCancelled() {
        InMemoryVisitRepo visitRepo = new InMemoryVisitRepo();
        CancelDichVuYeuCauHandler syncedHandler = new CancelDichVuYeuCauHandler(
                serviceRepo, visitRepo, null, auditPort);

        LuotKham visit = new LuotKham();
        visit.setId(500);
        visit.setTrangThai(TrangThaiLuotKham.CHO_CLS);
        visitRepo.save(visit);

        DanhMucDichVu clsCatalog = new DanhMucDichVu();
        clsCatalog.setId(10);
        clsCatalog.setLoaiDichVu(LoaiDichVu.XET_NGHIEM);
        clsCatalog.setHoatDong(true);
        serviceRepo.catalogs.put(10, clsCatalog);

        ChiDinhDichVu order1 = new ChiDinhDichVu();
        order1.setId(401);
        order1.setMaLuotKham(500);
        order1.setMaDichVu(10);
        order1.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
        serviceRepo.save(order1);

        ChiDinhDichVu order2 = new ChiDinhDichVu();
        order2.setId(402);
        order2.setMaLuotKham(500);
        order2.setMaDichVu(10);
        order2.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
        serviceRepo.save(order2);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                10, 1, "DOCTOR", 1, 1, UUID.randomUUID());

        assertTrue(syncedHandler.handle(new CancelDichVuYeuCauCommand(401, "Hủy 1", auth, null)).isSuccess());
        assertEquals(TrangThaiLuotKham.CHO_CLS, visitRepo.getById(500).orElseThrow().getTrangThai());

        assertTrue(syncedHandler.handle(new CancelDichVuYeuCauCommand(402, "Hủy 2", auth, null)).isSuccess());
        assertEquals(TrangThaiLuotKham.DANG_KHAM, visitRepo.getById(500).orElseThrow().getTrangThai());
    }
}
