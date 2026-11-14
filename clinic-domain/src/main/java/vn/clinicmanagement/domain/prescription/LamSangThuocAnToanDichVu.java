package vn.clinicmanagement.domain.prescription;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.common.exceptions.NghiepVuQuyTacViPhamNgoaiLe;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import java.util.ArrayList;
import java.util.List;

public class LamSangThuocAnToanDichVu {
    /** Trường `itemCode`: mathang mã. */
    /** Trường `itemName`: mathang tên. */
    /** Trường `warningMessage`: canhbao thông báo. */
    /** Trường `isCritical`: là `Critical`. */
    public record SafetyWarning(String itemCode, String itemName, String warningMessage, boolean isCritical) {}

    /** Hàm `evaluatePrescriptionSafety`: `evaluate` đơn thuốc antoan. */
    public List<SafetyWarning> evaluatePrescriptionSafety(BenhNhan patient, List<MatHang> prescribedItems) {
        List<SafetyWarning> warnings = new ArrayList<>();
        if (patient == null || prescribedItems == null) return warnings;
        List<TienSuBenh> historyList = patient.getTienSuBenhs();
        if (historyList == null || historyList.isEmpty()) return warnings;

        for (MatHang item : prescribedItems) {
            String active = item.getHoatChat();
            if (active == null || active.isBlank()) continue;
            for (TienSuBenh history : historyList) {
                if (history.getLoaiTienSu() == LoaiTienSu.DI_UNG) {
                    if (history.getMoTa().toLowerCase().contains(active.toLowerCase()) || active.toLowerCase().contains(history.getMoTa().toLowerCase())) {
                        warnings.add(new SafetyWarning(item.getMaItem(), item.getTenItem(), "Bệnh nhân có tiền sử DỊ ỨNG với hoạt chất: " + active, true));
                    }
                }
            }
        }
        return warnings;
    }
}
