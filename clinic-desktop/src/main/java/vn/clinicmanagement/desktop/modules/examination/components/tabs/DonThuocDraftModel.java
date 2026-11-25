package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.prescription.DonThuocMatHangDauVao;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocCommand;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.examination.components.common.ExaminationUiFactory;
import vn.clinicmanagement.desktop.modules.examination.components.print.DonThuocPrintPreviewManHinh;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.DonThuocYeuCauManHinh.PrescribedDraftItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý danh sách thuốc đang kê nháp, dựng `PrescribeThuocCommand` và dữ liệu in nháp.
 */
final class DonThuocDraftModel {

    private final List<PrescribedDraftItem> items = new ArrayList<>();

    void addOrReplace(PrescribedDraftItem draft) {
        if (draft == null) return;
        items.add(draft);
    }

    void removeAt(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    void clear() {
        items.clear();
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    int size() {
        return items.size();
    }

    String buildStatusSummaryText() {
        if (items.isEmpty()) {
            return "";
        }
        PrescribedDraftItem last = items.get(items.size() - 1);
        return " " + last.name + " (" + last.quantity.stripTrailingZeros().toPlainString()
                + " " + last.unit + ") | Đã kê: " + items.size() + " thuốc";
    }

    BigDecimal calculateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (PrescribedDraftItem d : items) {
            total = total.add(d.unitPrice.multiply(d.quantity));
        }
        return total;
    }

    List<Object[]> toTableRows() {
        List<Object[]> rows = new ArrayList<>(items.size());
        int stt = 1;
        for (PrescribedDraftItem d : items) {
            BigDecimal lineTotal = d.unitPrice.multiply(d.quantity);
            rows.add(new Object[]{
                    String.valueOf(stt++),
                    d.code,
                    d.name,
                    d.unit,
                    d.quantity.stripTrailingZeros().toPlainString(),
                    ExaminationUiFactory.formatMoney(d.unitPrice),
                    ExaminationUiFactory.formatMoney(lineTotal),
                    d.route,
                    d.hdsd
            });
        }
        return rows;
    }

    PrescribeThuocCommand buildPrescribeCommand(
            int visitId,
            int doctorId,
            int facilityId,
            String doctorAdvice,
            PhanQuyenNguCanh auth) {
        List<DonThuocMatHangDauVao> cmdItems = new ArrayList<>(items.size());
        for (PrescribedDraftItem draft : items) {
            cmdItems.add(new DonThuocMatHangDauVao(
                    draft.itemId,
                    draft.quantity,
                    draft.unitPrice,
                    draft.dosageSummary,
                    draft.route,
                    draft.frequency,
                    draft.days,
                    draft.morning,
                    draft.noon,
                    draft.afternoon,
                    draft.night,
                    draft.hdsd));
        }
        String advice = doctorAdvice != null && !doctorAdvice.isBlank()
                ? doctorAdvice.trim()
                : "Uống thuốc theo đơn và tái khám khi hết thuốc";
        return new PrescribeThuocCommand(
                visitId,
                doctorId > 0 ? doctorId : (auth != null ? auth.employeeId() : 1),
                facilityId > 0 ? facilityId : (auth != null ? auth.facilityId() : 1),
                1,
                "Kê đơn từ bàn khám",
                advice,
                true,
                cmdItems,
                auth);
    }

    DonThuocPrintPreviewManHinh.PrescriptionData buildPrintData(
            AdminPatientItem patient,
            String diagnosis,
            String doctorName) {
        List<DonThuocPrintPreviewManHinh.PrescriptionItem> printItems = new ArrayList<>(items.size());
        for (PrescribedDraftItem d : items) {
            printItems.add(new DonThuocPrintPreviewManHinh.PrescriptionItem(
                    d.name,
                    d.quantity.stripTrailingZeros().toPlainString(),
                    d.unit,
                    d.hdsd));
        }
        return new DonThuocPrintPreviewManHinh.PrescriptionData(
                patient != null ? ExaminationUiFactory.safeText(patient.name, "") : "",
                patient != null ? ExaminationUiFactory.safeText(patient.code, "") : "",
                patient != null ? ExaminationUiFactory.safeText(patient.birthYear, "") : "",
                patient != null ? ExaminationUiFactory.safeText(patient.gender, "") : "",
                patient != null ? ExaminationUiFactory.safeText(patient.requestType, "Viện phí") : "Viện phí",
                diagnosis == null ? "" : diagnosis,
                printItems,
                doctorName);
    }
}
