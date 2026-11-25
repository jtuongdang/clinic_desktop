package vn.clinicmanagement.application.reception;

import java.math.BigDecimal;

/** Giá dịch vụ đã được chốt cho lượt khám lúc đăng ký. */
public record TiepDonKhamGia(int visitId, int serviceId, String serviceName, BigDecimal unitPrice) {
}
