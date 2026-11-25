package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.prescription.DonThuocDuLieu;
import vn.clinicmanagement.application.prescription.DonThuocMatHangDuLieu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh;
import vn.clinicmanagement.desktop.modules.print.examination.DonThuocPrintPreviewManHinh;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PrescriptionTabPanel Behavior Tests")
class DonThuocYeuCauTheManHinhTest {

    @Test
    @DisplayName("Applies loaded prescriptions, updates tab item count callback, and builds real print data")
    void appliesLoadedPrescriptionsAndBuildsPrintData() {
        KhamBenhGhepNoiGoc.Services services = KhamBenhGhepNoiGoc.createDefault();
        DonThuocYeuCauTheManHinh panel = new DonThuocYeuCauTheManHinh(services);

        AtomicInteger countFromCallback = new AtomicInteger(-1);
        panel.setOnPrescriptionCountChanged(countFromCallback::set);
        panel.setPatientContext(new KhamBenhManHinh.AdminPatientItem(
                "01", "TRẦN MINH KHÔI", "BN260002", "Viện phí", "4", true,
                "Nam", "2022", "4 tuổi", "4 tuổi", "01/01/2022",
                "Phường Võ Cường, TP Bắc Ninh", "TP Bắc Ninh", "0912345002", "BA-15",
                "18/11/2026", "Khoa Khám Bệnh", "BS. Lê Văn An", "Viêm họng cấp", "VP-2020"));

        panel.applyLoadedPrescriptions(List.of());
        assertEquals(0, countFromCallback.get());

        DonThuocMatHangDuLieu itemDto = new DonThuocMatHangDuLieu(
                10, 42, "THUOC004", "Gastropulgite 2.5g", "Gói",
                new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("3500"), new BigDecimal("35000"),
                "Uống", "Sáng 1 Trưa 1", "Ngày 2 lần", 5,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                "Uống trước bữa ăn");
        DonThuocDuLieu rxDto = new DonThuocDuLieu(
                88, 15, 2, "BS. Lê Văn An", 1, 1,
                TrangThaiDonThuoc.DA_DUYET, LocalDateTime.of(2026, 11, 18, 14, 40),
                "Theo dõi thêm", "Tái khám nếu sốt", new BigDecimal("35000"), List.of(itemDto));

        panel.applyLoadedPrescriptions(List.of(rxDto));

        assertEquals(1, countFromCallback.get());
        DonThuocPrintPreviewManHinh.PrescriptionData printData = panel.buildPrescriptionPrintData();
        assertEquals("TRẦN MINH KHÔI", printData.patientName());
        assertEquals("BN260002", printData.patientCode());
        assertEquals("BS. Lê Văn An", printData.doctorName());
        assertEquals("Viêm họng cấp", printData.diagnosis());
        assertEquals(1, printData.medicines().size());
        assertEquals("Gastropulgite 2.5g", printData.medicines().get(0).name());
        assertEquals("10", printData.medicines().get(0).quantity());
        assertEquals("Gói", printData.medicines().get(0).unit());
    }
}
