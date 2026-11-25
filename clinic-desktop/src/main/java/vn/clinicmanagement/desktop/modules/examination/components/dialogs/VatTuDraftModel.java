package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.prescription.DonThuocMatHangDauVao;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocCommand;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.examination.components.common.ExaminationUiFactory;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.YTeVatTuYeuCauManHinh.SupplyCatalogItem;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.YTeVatTuYeuCauManHinh.SupplyDraftItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý trạng thái phiếu vật tư đang kê nháp, tính chi phí và dựng `PrescribeThuocCommand`.
 */
final class VatTuDraftModel {

    record FinancialSnapshot(BigDecimal total, BigDecimal bhyt, BigDecimal patient) {}

    private final List<SupplyDraftItem> draftItems = new ArrayList<>();

    void addOrMerge(SupplyCatalogItem item, BigDecimal qty) {
        if (item == null || qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) return;
        for (int i = 0; i < draftItems.size(); i++) {
            SupplyDraftItem existing = draftItems.get(i);
            if (existing.itemId() == item.itemId() && existing.code().equals(item.code())) {
                draftItems.set(i, new SupplyDraftItem(
                        existing.itemId(),
                        existing.code(),
                        existing.name(),
                        existing.unit(),
                        existing.warehouseId(),
                        existing.warehouseName(),
                        existing.quantity().add(qty),
                        existing.unitPrice()));
                return;
            }
        }
        draftItems.add(new SupplyDraftItem(
                item.itemId(),
                item.code(),
                item.name(),
                item.unit(),
                item.warehouseId(),
                item.warehouseName(),
                qty,
                item.price()));
    }

    void removeAt(int index) {
        if (index >= 0 && index < draftItems.size()) {
            draftItems.remove(index);
        }
    }

    void clear() {
        draftItems.clear();
    }

    boolean isEmpty() {
        return draftItems.isEmpty();
    }

    int size() {
        return draftItems.size();
    }

    List<SupplyDraftItem> snapshot() {
        return List.copyOf(draftItems);
    }

    String buildStatusSummaryText() {
        if (draftItems.isEmpty()) {
            return "Chọn vật tư trong bảng và bấm OK (Ctrl+A) để thêm vào phiếu chỉ định.";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < draftItems.size(); i++) {
            SupplyDraftItem d = draftItems.get(i);
            if (i > 0) sb.append(", ");
            sb.append(d.name()).append(" (")
                    .append(d.quantity().stripTrailingZeros().toPlainString())
                    .append(" ").append(d.unit()).append(")");
        }
        return sb.toString();
    }

    FinancialSnapshot calculateFinancials(AdminPatientItem patient) {
        BigDecimal total = BigDecimal.ZERO;
        for (SupplyDraftItem d : draftItems) {
            total = total.add(d.unitPrice().multiply(d.quantity()));
        }
        boolean isBhyt = patient != null && patient.requestType != null
                && (patient.requestType.contains("BHYT") || patient.requestType.contains("hiểm"));
        int rate = isBhyt ? (patient.bhytRate > 0 ? patient.bhytRate : 80) : 0;
        BigDecimal bhytAmount = isBhyt
                ? total.multiply(BigDecimal.valueOf(rate)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new FinancialSnapshot(total, bhytAmount, total.subtract(bhytAmount));
    }

    List<Object[]> toDraftTableRows() {
        List<Object[]> rows = new ArrayList<>(draftItems.size());
        int stt = 1;
        for (SupplyDraftItem d : draftItems) {
            BigDecimal lineTotal = d.unitPrice().multiply(d.quantity());
            rows.add(new Object[]{
                    stt++,
                    d.code(),
                    d.name(),
                    d.unit(),
                    d.warehouseName(),
                    d.quantity().stripTrailingZeros().toPlainString(),
                    ExaminationUiFactory.formatMoney(d.unitPrice()),
                    ExaminationUiFactory.formatMoney(lineTotal)
            });
        }
        return rows;
    }

    List<Object[]> toCallbackRows() {
        List<Object[]> rows = new ArrayList<>(draftItems.size());
        int stt = 1;
        for (SupplyDraftItem d : draftItems) {
            rows.add(new Object[]{
                    String.valueOf(stt++),
                    d.code(),
                    d.name(),
                    d.unit(),
                    d.quantity().stripTrailingZeros().toPlainString()
            });
        }
        return rows;
    }

    PrescribeThuocCommand buildPrescribeCommand(int visitId, int doctorId, int facilityId, PhanQuyenNguCanh auth) {
        List<DonThuocMatHangDauVao> items = new ArrayList<>(draftItems.size());
        int warehouseId = 4;
        for (SupplyDraftItem d : draftItems) {
            if (d.warehouseId() > 0) {
                warehouseId = d.warehouseId();
            }
            items.add(new DonThuocMatHangDauVao(
                    d.itemId(),
                    d.quantity(),
                    d.unitPrice(),
                    "Số lượng " + d.quantity().stripTrailingZeros().toPlainString() + " " + d.unit(),
                    "Vật tư y tế",
                    "",
                    1,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "Xuất từ " + d.warehouseName()));
        }
        return new PrescribeThuocCommand(
                visitId,
                doctorId > 0 ? doctorId : (auth != null ? auth.employeeId() : 1),
                facilityId > 0 ? facilityId : (auth != null ? auth.facilityId() : 1),
                warehouseId,
                "[VAT_TU] Chỉ định vật tư y tế từ bàn khám",
                "Sử dụng theo chỉ định của bác sĩ",
                true,
                items,
                auth);
    }
}
