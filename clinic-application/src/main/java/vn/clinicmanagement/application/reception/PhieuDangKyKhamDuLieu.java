package vn.clinicmanagement.application.reception;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Dữ liệu phiếu đọc theo lượt khám đã lưu; giá null nghĩa là chưa có giá được chốt. */
public record PhieuDangKyKhamDuLieu(int visitId, String patientCode, String facilityName,
        String facilityAddress, String facilityPhone, String patientName, LocalDate birthDate,
        String gender, String address, String roomName, LocalDateTime registeredAt,
        LocalDateTime collectedAt, String collectorName, List<DongDichVu> services) {
    public PhieuDangKyKhamDuLieu { services = List.copyOf(services); }
    public boolean hasConfirmedPrice() { return !services.isEmpty() && services.stream().allMatch(s -> s.unitPrice() != null); }
    public BigDecimal total() {
        if (!hasConfirmedPrice()) return null;
        return services.stream().map(s -> s.unitPrice().multiply(s.quantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    /** Dòng dịch vụ và đơn giá chốt trong giao dịch đăng ký, không đọc lại giá danh mục để in lại. */
    public record DongDichVu(String name, BigDecimal quantity, BigDecimal unitPrice) { }
}
