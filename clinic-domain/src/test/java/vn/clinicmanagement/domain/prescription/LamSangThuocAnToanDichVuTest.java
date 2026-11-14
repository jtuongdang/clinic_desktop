package vn.clinicmanagement.domain.prescription;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.TienSuBenh;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LamSangThuocAnToanDichVuTest {

    private final LamSangThuocAnToanDichVu service = new LamSangThuocAnToanDichVu();

    @Test
    void warnsForCaseInsensitiveAllergySubstanceMatch() {
        BenhNhan patient = patientWithHistory(LoaiTienSu.DI_UNG, "Penicillin allergy");
        MatHang item = item("DRUG-SYN-01", "Synthetic antibiotic", "PENICILLIN");

        List<LamSangThuocAnToanDichVu.SafetyWarning> warnings =
                service.evaluatePrescriptionSafety(patient, List.of(item));

        assertEquals(1, warnings.size());
        LamSangThuocAnToanDichVu.SafetyWarning warning = warnings.get(0);
        assertEquals("DRUG-SYN-01", warning.itemCode());
        assertEquals("Synthetic antibiotic", warning.itemName());
        assertEquals("Bệnh nhân có tiền sử DỊ ỨNG với hoạt chất: PENICILLIN", warning.warningMessage());
        assertTrue(warning.isCritical());
    }

    @Test
    void warnsWhenAllergyDescriptionIsContainedInTheActiveSubstance() {
        BenhNhan patient = patientWithHistory(LoaiTienSu.DI_UNG, "amox");
        MatHang item = item("DRUG-SYN-02", "Synthetic medicine", "Amoxicillin trihydrate");

        List<LamSangThuocAnToanDichVu.SafetyWarning> warnings =
                service.evaluatePrescriptionSafety(patient, List.of(item));

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).isCritical());
    }

    @Test
    void ignoresNonAllergyHistoryAndUnmatchedOrBlankActiveSubstances() {
        BenhNhan patient = new BenhNhan();
        TienSuBenh chronicCondition = history(LoaiTienSu.BENH_NEN, "penicillin");
        TienSuBenh unrelatedAllergy = history(LoaiTienSu.DI_UNG, "latex");
        patient.setTienSuBenhs(List.of(chronicCondition, unrelatedAllergy));

        List<MatHang> items = List.of(
                item("DRUG-SYN-03", "Synthetic medicine", "penicillin"),
                item("DRUG-SYN-04", "Synthetic medicine", "unmatched-substance"),
                item("DRUG-SYN-05", "Synthetic medicine", " "),
                item("DRUG-SYN-06", "Synthetic medicine", null));

        assertTrue(service.evaluatePrescriptionSafety(patient, items).isEmpty());
    }

    @Test
    void returnsNoWarningsWhenPatientItemsOrHistoryAreAbsent() {
        BenhNhan patientWithoutHistory = new BenhNhan();
        BenhNhan patientWithNullHistory = new BenhNhan();
        patientWithNullHistory.setTienSuBenhs(null);
        MatHang item = item("DRUG-SYN-07", "Synthetic medicine", "active");

        assertTrue(service.evaluatePrescriptionSafety(null, List.of(item)).isEmpty());
        assertTrue(service.evaluatePrescriptionSafety(patientWithoutHistory, List.of(item)).isEmpty());
        assertTrue(service.evaluatePrescriptionSafety(patientWithNullHistory, List.of(item)).isEmpty());
        assertTrue(service.evaluatePrescriptionSafety(patientWithNullHistory, null).isEmpty());
        assertTrue(service.evaluatePrescriptionSafety(patientWithoutHistory, List.of()).isEmpty());
    }

    private static BenhNhan patientWithHistory(LoaiTienSu type, String description) {
        BenhNhan patient = new BenhNhan();
        patient.setTienSuBenhs(List.of(history(type, description)));
        return patient;
    }

    private static TienSuBenh history(LoaiTienSu type, String description) {
        TienSuBenh history = new TienSuBenh();
        history.setLoaiTienSu(type);
        history.setMoTa(description);
        return history;
    }

    private static MatHang item(String code, String name, String active) {
        MatHang item = new MatHang();
        item.setMaItem(code);
        item.setTenItem(name);
        item.setHoatChat(active);
        return item;
    }
}
