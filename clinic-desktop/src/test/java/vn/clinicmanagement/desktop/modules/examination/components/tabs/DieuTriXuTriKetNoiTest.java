package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("TreatmentResolution & MedicalRecord Wiring Tests (Task 1.4 & 1.5 - HD-INTEG-01)")
class DieuTriXuTriKetNoiTest {

    @Test
    @DisplayName("TreatmentResolutionTabPanel initializes cleanly with default constructor and services overload")
    void treatmentResolutionTabPanelInitializesCleanly() {
        DieuTriXuTriTheManHinh defaultPanel = new DieuTriXuTriTheManHinh();
        assertNotNull(defaultPanel, "TreatmentResolutionTabPanel default instance must not be null");
        assertNotNull(defaultPanel.getServices(), "Services must be initialized via fallback");

        KhamBenhGhepNoiGoc.Services customServices = KhamBenhGhepNoiGoc.createDefault();
        DieuTriXuTriTheManHinh panel = new DieuTriXuTriTheManHinh(customServices);
        assertNotNull(panel, "TreatmentResolutionTabPanel custom services instance must not be null");
        assertSame(customServices, panel.getServices(), "Services must match injected instance");

        assertDoesNotThrow(() -> panel.setVisitContext(105, 12));
        assertEquals(105, panel.getCurrentVisitId(), "Visit ID must be updated");
        assertEquals(12, panel.getCurrentDoctorId(), "Doctor ID must be updated");
    }

    @Test
    @DisplayName("MedicalRecordTabPanel initializes cleanly and updates visit context")
    void medicalRecordTabPanelInitializesCleanly() {
        YTeRecordTheManHinh defaultPanel = new YTeRecordTheManHinh();
        assertNotNull(defaultPanel, "MedicalRecordTabPanel default instance must not be null");
        assertNotNull(defaultPanel.getServices(), "Services must be initialized via fallback");

        KhamBenhGhepNoiGoc.Services customServices = KhamBenhGhepNoiGoc.createDefault();
        YTeRecordTheManHinh panel = new YTeRecordTheManHinh(customServices);
        assertNotNull(panel, "MedicalRecordTabPanel custom services instance must not be null");
        assertSame(customServices, panel.getServices(), "Services must match injected instance");

        assertDoesNotThrow(() -> panel.setVisitContext(202, 18));
        assertEquals(202, panel.getCurrentVisitId(), "Visit ID must be updated");
        assertEquals(18, panel.getCurrentDoctorId(), "Doctor ID must be updated");
    }

    @Test
    @DisplayName("ExaminationPanel properly wires services into TreatmentResolution and MedicalRecord tabs")
    void examinationPanelWiresServicesIntoTabs() {
        KhamBenhGhepNoiGoc.Services services = KhamBenhGhepNoiGoc.createDefault();
        KhamBenhManHinh examPanel = new KhamBenhManHinh(services);
        assertNotNull(examPanel, "ExaminationPanel must not be null");

        DieuTriXuTriTheManHinh trPanel = examPanel.getTreatmentResolutionTabPanel();
        assertNotNull(trPanel, "TreatmentResolutionTabPanel in ExaminationPanel must not be null");
        assertSame(services, trPanel.getServices(), "TreatmentResolutionTabPanel must receive examination services");

        YTeRecordTheManHinh mrPanel = examPanel.getMedicalRecordTabPanel();
        assertNotNull(mrPanel, "MedicalRecordTabPanel in ExaminationPanel must not be null");
        assertSame(services, mrPanel.getServices(), "MedicalRecordTabPanel must receive examination services");

        assertDoesNotThrow(() -> examPanel.selectPatientByIndex(0));
        assertNotNull(trPanel.getTxtChanDoanXacDinh());
        assertNotNull(mrPanel.getTxtDienBienLamSang());
    }

    @Test
    @DisplayName("TreatmentResolutionTabPanel syncs Sau N ngay <-> Ngay hen tai kham cleanly")
    void reexamDaysAndDateSyncBidirectionally() {
        KhamBenhGhepNoiGoc.Services services = KhamBenhGhepNoiGoc.createDefault();
        DieuTriXuTriTheManHinh panel = new DieuTriXuTriTheManHinh(services);

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        java.time.LocalDate in5Days = java.time.LocalDate.now().plusDays(5);
        panel.getTxtSoNgayTaiKham().setText("5");
        assertEquals(in5Days.format(fmt), panel.getTxtNgayHenTaiKham().getText());
        assertEquals(in5Days, panel.resolveReexamDateForSave());

        java.time.LocalDate in14Days = java.time.LocalDate.now().plusDays(14);
        panel.getTxtNgayHenTaiKham().setText(in14Days.format(fmt));
        assertEquals("14", panel.getTxtSoNgayTaiKham().getText());
        assertEquals(in14Days, panel.resolveReexamDateForSave());

        panel.setReexamDate(null);
        assertEquals("", panel.getTxtNgayHenTaiKham().getText());
        assertEquals("0", panel.getTxtSoNgayTaiKham().getText());
        org.junit.jupiter.api.Assertions.assertNull(panel.resolveReexamDateForSave());
    }

    @Test
    @DisplayName("TreatmentResolutionTabPanel clears sample A00.9 default and syncs patient diagnosis from DB")
    void syncsPatientDiagnosisIntoTreatmentResolutionTab() {
        KhamBenhGhepNoiGoc.Services services = KhamBenhGhepNoiGoc.createDefault();
        DieuTriXuTriTheManHinh panel = new DieuTriXuTriTheManHinh(services);

        assertEquals("", panel.getTxtMaIcdChinh().getText());
        assertEquals("", panel.getTxtTenIcdChinh().getText());

        panel.setPatientInfo("BN260002", "TRẦN MINH KHÔI", "4", "Nam", "TP Bắc Ninh", "ADMIN #1", "J02 - Viêm họng cấp");
        assertEquals("J02", panel.getTxtMaIcdChinh().getText());
        assertEquals("Viêm họng cấp", panel.getTxtTenIcdChinh().getText());
        assertEquals("Viêm họng cấp (J02)", panel.getTxtChanDoanXacDinh().getText());

        vn.clinicmanagement.domain.examination.LuotKham visit = new vn.clinicmanagement.domain.examination.LuotKham();
        visit.setQuaTrinhBenhLy("Sốt cao, ho khan");
        visit.setKhamBoPhan("Amidan sưng đỏ");
        visit.setKetQuaDieuTri(vn.clinicmanagement.domain.common.enums.KetQuaDieuTri.DO_GIAM);
        visit.setHuongXuTri(vn.clinicmanagement.domain.common.enums.HuongXuTri.NHAP_VIEN_NOI_TRU);
        visit.setHuongDieuTri("Nhập viện - Khoa Nhi");
        vn.clinicmanagement.domain.examination.ChanDoan sec = new vn.clinicmanagement.domain.examination.ChanDoan();
        sec.setLaChinh(false);
        sec.setLoaiChanDoan("KEM_THEO");
        sec.setChanDoanText("Rối loạn tiêu hóa");
        visit.setChanDoans(java.util.List.of(sec));

        panel.applyVisitDetail(visit);
        assertEquals("Sốt cao, ho khan", panel.getTxtTrieuChung().getText());
        assertEquals("Amidan sưng đỏ", panel.getTxtKhamLamSang().getText());
        assertEquals("Nhập viện", panel.getCboHinhThucXuTri().getSelectedItem());

        panel.getCboHinhThucXuTri().setSelectedItem("Tử vong");
        org.junit.jupiter.api.Assertions.assertFalse(panel.getTxtNgayHenTaiKham().isEnabled());
        org.junit.jupiter.api.Assertions.assertNull(panel.resolveReexamDateForSave());
    }
}
