package vn.clinicmanagement.desktop.modules.examination;

import vn.clinicmanagement.application.examination.add.AddChanDoanCommand;
import vn.clinicmanagement.application.examination.complete.CompleteKhamCommand;
import vn.clinicmanagement.application.examination.update.UpdateEMRLamSangRecordCommand;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.prescription.DonThuocMatHangDauVao;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocBienNhan;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocCommand;
import vn.clinicmanagement.desktop.modules.billing.VienPhiGhepNoiGoc;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.prescription.LamSangThuocAnToanDichVu;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KhamBenhGhepNoiGocTest {

    @Test
    @DisplayName("CreateDefault must assemble all 11 examination handlers cleanly")
    void testCreateDefaultAssemblesAllHandlers() {
        KhamBenhGhepNoiGoc.Services services = KhamBenhGhepNoiGoc.createDefault();

        assertNotNull(services, "Services bundle must not be null");
        assertNotNull(services.startExam(), "StartExamHandler must be present");
        assertNotNull(services.completeExam(), "CompleteExamHandler must be present");
        assertNotNull(services.addDiagnosis(), "AddDiagnosisHandler must be present");
        assertNotNull(services.prescribeMedication(), "PrescribeMedicationHandler must be present");
        assertNotNull(services.getPrescriptionByVisit(), "GetPrescriptionByVisitHandler must be present");
        assertNotNull(services.cancelPrescription(), "CancelPrescriptionHandler must be present");
        assertNotNull(services.orderService(), "OrderServiceHandler must be present");
        assertNotNull(services.getServicesByVisit(), "GetServicesByVisitHandler must be present");
        assertNotNull(services.cancelServiceOrder(), "CancelServiceOrderHandler must be present");
        assertNotNull(services.updateEmrClinicalRecord(), "UpdateEmrClinicalRecordHandler must be present");
        assertNotNull(services.transferVisit(), "TransferVisitHandler must be present");
    }

    @Test
    @DisplayName("ADMIN can prescribe on CHO_KHAM visit via prescribeWithAutoStart")
    void testPrescribeWithAutoStartAllowsAdminOnWaitingVisit() {
        var visitRepo = new VienPhiGhepNoiGoc.InMemoryVisitRepository();
        LuotKham visit = new LuotKham();
        visit.setId(2020);
        visit.setMaBenhNhan(2);
        visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);
        visitRepo.update(visit);

        var patientRepo = new KhamBenhGhepNoiGoc.InMemoryPatientRepository();
        BenhNhan patient = new BenhNhan();
        patient.setId(2);
        patientRepo.add(patient);

        var inventoryRepo = new KhamBenhGhepNoiGoc.InMemoryInventoryRepository();
        MatHang drug = new MatHang();
        drug.setId(3);
        drug.setMaItem("THUOC003");
        drug.setTenItem("Paracetamol 500mg");
        drug.setLoaiItem(LoaiMatHang.THUOC);
        inventoryRepo.saveItem(drug);

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
                patientRepo,
                new KhamBenhGhepNoiGoc.InMemoryPrescriptionRepository(),
                new KhamBenhGhepNoiGoc.InMemoryServiceOrderRepository(),
                inventoryRepo,
                new LamSangThuocAnToanDichVu(),
                directTx,
                noopAudit);

        PhanQuyenNguCanh adminAuth = new PhanQuyenNguCanh(1, 1, "ADMIN", 1, 1, UUID.randomUUID());
        PrescribeThuocCommand cmd = new PrescribeThuocCommand(
                2020, 1, 1, 1, "Kê đơn", "Uống sau ăn", true,
                List.of(new DonThuocMatHangDauVao(
                        3, BigDecimal.valueOf(4), BigDecimal.valueOf(1000),
                        "Sáng 1", "Uống", "5 ngày", 5,
                        BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "Sau ăn")),
                adminAuth);

        KetQua<PrescribeThuocBienNhan> result = KhamBenhGhepNoiGoc.prescribeWithAutoStart(services, cmd);

        assertTrue(result.isSuccess());
        assertEquals(TrangThaiLuotKham.DANG_KHAM, visitRepo.getById(2020).orElseThrow().getTrangThai());
        assertEquals(1, visitRepo.getById(2020).orElseThrow().getMaBacSi());
    }

    @Test
    @DisplayName("prescribeWithAutoStart saves selected ICD diagnosis alongside prescription")
    void testPrescribeWithAutoStartPersistsIcdDiagnosis() {
        var visitRepo = new VienPhiGhepNoiGoc.InMemoryVisitRepository();
        LuotKham visit = new LuotKham();
        visit.setId(2021);
        visit.setMaBenhNhan(2);
        visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);
        visitRepo.update(visit);

        var patientRepo = new KhamBenhGhepNoiGoc.InMemoryPatientRepository();
        BenhNhan patient = new BenhNhan();
        patient.setId(2);
        patientRepo.add(patient);

        var inventoryRepo = new KhamBenhGhepNoiGoc.InMemoryInventoryRepository();
        MatHang drug = new MatHang();
        drug.setId(3);
        drug.setMaItem("THUOC003");
        drug.setTenItem("Paracetamol 500mg");
        drug.setLoaiItem(LoaiMatHang.THUOC);
        inventoryRepo.saveItem(drug);

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
                patientRepo,
                new KhamBenhGhepNoiGoc.InMemoryPrescriptionRepository(),
                new KhamBenhGhepNoiGoc.InMemoryServiceOrderRepository(),
                inventoryRepo,
                new LamSangThuocAnToanDichVu(),
                directTx,
                noopAudit);

        PhanQuyenNguCanh adminAuth = new PhanQuyenNguCanh(1, 1, "ADMIN", 1, 1, UUID.randomUUID());
        PrescribeThuocCommand cmd = new PrescribeThuocCommand(
                2021, 1, 1, 1, "Kê đơn", "Uống sau ăn", true,
                List.of(new DonThuocMatHangDauVao(
                        3, BigDecimal.valueOf(4), BigDecimal.valueOf(1000),
                        "Sáng 1", "Uống", "5 ngày", 5,
                        BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "Sau ăn")),
                adminAuth);

        KetQua<PrescribeThuocBienNhan> result = KhamBenhGhepNoiGoc.prescribeWithAutoStart(
                services, cmd, 4, "Viêm họng cấp", "J02 - Viêm họng cấp", "");

        assertTrue(result.isSuccess());
        LuotKham updated = visitRepo.getById(2021).orElseThrow();
        assertEquals(1, updated.getChanDoans().size());
        assertEquals(4, updated.getChanDoans().get(0).getMaICD());
        assertEquals("Viêm họng cấp", updated.getChanDoans().get(0).getChanDoanText());
    }

    @Test
    @DisplayName("completeDispositionWithAutoStart auto-starts CHO_KHAM visit and persists custom ngayHenTaiKham")
    void testCompleteDispositionWithAutoStartPersistsReexamDate() {
        var visitRepo = new VienPhiGhepNoiGoc.InMemoryVisitRepository();
        LuotKham visit = new LuotKham();
        visit.setId(2022);
        visit.setMaBenhNhan(2);
        visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);
        visitRepo.update(visit);

        var patientRepo = new KhamBenhGhepNoiGoc.InMemoryPatientRepository();
        BenhNhan patient = new BenhNhan();
        patient.setId(2);
        patientRepo.add(patient);

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
                patientRepo,
                new KhamBenhGhepNoiGoc.InMemoryPrescriptionRepository(),
                new KhamBenhGhepNoiGoc.InMemoryServiceOrderRepository(),
                new KhamBenhGhepNoiGoc.InMemoryInventoryRepository(),
                new LamSangThuocAnToanDichVu(),
                directTx,
                noopAudit);

        PhanQuyenNguCanh adminAuth = new PhanQuyenNguCanh(1, 1, "ADMIN", 1, 1, UUID.randomUUID());
        java.time.LocalDate followUp = java.time.LocalDate.now().plusDays(10);
        var diagCmd = new vn.clinicmanagement.application.examination.add.AddChanDoanCommand(
                2022, 1, 4, "Viêm họng cấp", true, "XAC_DINH", adminAuth);
        var completeCmd = new vn.clinicmanagement.application.examination.complete.CompleteKhamCommand(
                2022, 1,
                vn.clinicmanagement.domain.common.enums.KetQuaDieuTri.KHOI,
                vn.clinicmanagement.domain.common.enums.HuongXuTri.CHO_VE_TOA,
                "Uống thuốc đủ liều",
                followUp,
                adminAuth);

        String err = KhamBenhGhepNoiGoc.completeDispositionWithAutoStart(services, diagCmd, completeCmd, true);

        assertNull(err);
        LuotKham completed = visitRepo.getById(2022).orElseThrow();
        assertEquals(TrangThaiLuotKham.HOAN_THANH, completed.getTrangThai());
        assertEquals(followUp, completed.getNgayHenTaiKham());
        assertEquals(1, completed.getChanDoans().size());
    }

    @Test
    @DisplayName("completeDispositionWithAutoStart persists secondary diagnoses, clinical notes, NOI_TRU and custom timestamp")
    void testCompleteDispositionPersistsSecondaryDiagnosesAndClinicalFields() {
        var visitRepo = new VienPhiGhepNoiGoc.InMemoryVisitRepository();
        LuotKham visit = new LuotKham();
        visit.setId(2023);
        visit.setMaBenhNhan(2);
        visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);
        visitRepo.update(visit);

        var patientRepo = new KhamBenhGhepNoiGoc.InMemoryPatientRepository();
        BenhNhan patient = new BenhNhan();
        patient.setId(2);
        patientRepo.add(patient);

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
                patientRepo,
                new KhamBenhGhepNoiGoc.InMemoryPrescriptionRepository(),
                new KhamBenhGhepNoiGoc.InMemoryServiceOrderRepository(),
                new KhamBenhGhepNoiGoc.InMemoryInventoryRepository(),
                new LamSangThuocAnToanDichVu(),
                directTx,
                noopAudit);

        PhanQuyenNguCanh adminAuth = new PhanQuyenNguCanh(1, 1, "ADMIN", 1, 1, UUID.randomUUID());
        java.time.LocalDateTime customTime = java.time.LocalDateTime.now().withNano(0);
        var mainDiag = new vn.clinicmanagement.application.examination.add.AddChanDoanCommand(
                2023, 1, 4, "Viêm họng cấp", true, "XAC_DINH", adminAuth);
        var secDiag = new vn.clinicmanagement.application.examination.add.AddChanDoanCommand(
                2023, 1, 7, "Tiêu chảy nhiễm khuẩn", false, "KEM_THEO", adminAuth);
        var clinicalCmd = new vn.clinicmanagement.application.examination.update.UpdateEMRLamSangRecordCommand(
                2023, 1, "Sốt 38.5 độ, ho khan", null, null, null, "Họng đỏ sung huyết", null, null, null, adminAuth);
        var completeCmd = new vn.clinicmanagement.application.examination.complete.CompleteKhamCommand(
                2023, 1,
                vn.clinicmanagement.domain.common.enums.KetQuaDieuTri.DO_GIAM,
                vn.clinicmanagement.domain.common.enums.HuongXuTri.NHAP_VIEN_NOI_TRU,
                "Theo dõi nội trú",
                null,
                customTime,
                vn.clinicmanagement.domain.common.enums.HinhThucDieuTri.NOI_TRU,
                "Nhập viện - Khoa nội nhi tổng hợp",
                null,
                adminAuth);

        String err = KhamBenhGhepNoiGoc.completeDispositionWithAutoStart(
                services, mainDiag, List.of(secDiag, secDiag), clinicalCmd, completeCmd, null, true);

        assertNull(err);
        LuotKham saved = visitRepo.getById(2023).orElseThrow();
        assertEquals(TrangThaiLuotKham.HOAN_THANH, saved.getTrangThai());
        assertEquals(2, saved.getChanDoans().size());
        assertEquals("KEM_THEO", saved.getChanDoans().get(1).getLoaiChanDoan());
        assertEquals("Sốt 38.5 độ, ho khan", saved.getQuaTrinhBenhLy());
        assertEquals("Họng đỏ sung huyết", saved.getKhamBoPhan());
        assertEquals(vn.clinicmanagement.domain.common.enums.HinhThucDieuTri.NOI_TRU, saved.getHinhThucDieuTri());
        assertEquals("Nhập viện - Khoa nội nhi tổng hợp", saved.getHuongDieuTri());
        assertEquals(customTime, saved.getThoiGianKetThucKham());
    }
}
