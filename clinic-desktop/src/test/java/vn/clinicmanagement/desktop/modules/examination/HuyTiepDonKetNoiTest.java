package vn.clinicmanagement.desktop.modules.examination;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.core.session.PhienThuNganKiemThu;
import vn.clinicmanagement.desktop.modules.billing.VienPhiGhepNoiGoc;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.prescription.LamSangThuocAnToanDichVu;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class HuyTiepDonKetNoiTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @AfterEach
    void logout() {
        NguoiDungPhien.getInstance().clearSession();
    }

    @Test
    void wiresCancelVisitAndCancelsWaitingVisitForReceptionist() throws Exception {
        var visitRepo = new VienPhiGhepNoiGoc.InMemoryVisitRepository();
        LuotKham visit = new LuotKham();
        visit.setId(2025);
        visit.setMaBenhNhan(1);
        visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);
        visitRepo.update(visit);

        GiaoDichRanhGioi directTx = new GiaoDichRanhGioi() {
            @Override
            public <T> KetQua<T> inTransaction(java.util.function.Supplier<KetQua<T>> operation) {
                return operation.get();
            }
        };
        KiemToanWriteCongKetNoi noopAudit = new KiemToanWriteCongKetNoi() {
            @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
            @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
            @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
            @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
        };
        var services = KhamBenhGhepNoiGoc.create(
                visitRepo,
                new KhamBenhGhepNoiGoc.InMemoryRoomRepository(),
                new KhamBenhGhepNoiGoc.InMemoryPatientRepository(),
                new KhamBenhGhepNoiGoc.InMemoryPrescriptionRepository(),
                new KhamBenhGhepNoiGoc.InMemoryServiceOrderRepository(),
                new KhamBenhGhepNoiGoc.InMemoryInventoryRepository(),
                new LamSangThuocAnToanDichVu(),
                directTx,
                noopAudit);
        assertNotNull(services.cancelVisit());

        KhamBenhManHinh[] screen = new KhamBenhManHinh[1];
        SwingUtilities.invokeAndWait(() -> screen[0] = new KhamBenhManHinh(services));

        PhienThuNganKiemThu.login("DOCTOR");
        String doctorErr = screen[0].executeCancelReception(2025, "Bệnh nhân xin về");
        assertNotNull(doctorErr);
        assertTrue(doctorErr.contains("Không được phép"));

        PhienThuNganKiemThu.login("RECEPTIONIST");
        String receptionistErr = screen[0].executeCancelReception(2025, "Bệnh nhân xin về");
        assertNull(receptionistErr);
        assertEquals(TrangThaiLuotKham.DA_HUY, visitRepo.getById(2025).orElseThrow().getTrangThai());
    }
}
