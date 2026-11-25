package vn.clinicmanagement.application.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/** Bổ sung thông tin nguồn của từng dòng hóa đơn và thẻ BHYT của bệnh nhân. */
public interface ThanhToanDichVuReadCongKetNoi {
    Map<Integer,NguonDong> getByInvoice(int invoiceId,int visitId,int facilityId);

    default Optional<ThongTinBaoHiemBenhNhan> getActiveInsuranceByPatient(int patientId) {
        return Optional.empty();
    }

    record NguonDong(LocalDateTime orderedAt,String equivalentCode,String category,String unit,
            String department,String doctor,BigDecimal catalogInsurancePrice,BigDecimal catalogServicePrice) {
        public NguonDong(LocalDateTime orderedAt,String equivalentCode,String category,String unit,
                String department,String doctor,BigDecimal catalogInsurancePrice) {
            this(orderedAt,equivalentCode,category,unit,department,doctor,catalogInsurancePrice,null);
        }
    }

    record ThongTinBaoHiemBenhNhan(String soBaoHiem,LocalDate ngayBatDau,LocalDate ngayHetHan,int mucHuong) {
        public boolean isValidOn(LocalDate date) {
            if (soBaoHiem == null || soBaoHiem.isBlank() || ngayHetHan == null) return false;
            LocalDate ref = date != null ? date : LocalDate.now();
            if (ngayBatDau != null && ref.isBefore(ngayBatDau)) return false;
            return !ref.isAfter(ngayHetHan);
        }
    }
}
