package vn.clinicmanagement.domain.appointment;
import java.math.BigDecimal;
public interface IThanhToanCongKetNoiDichVu {
    PaymentResult createPaymentUrl(int appointmentId, BigDecimal amount, String description, String returnUrl);
    PaymentCallbackResult processCallback(String queryParams);
    record PaymentResult(boolean success, String paymentUrl, String transactionId, String message) {}
    record PaymentCallbackResult(boolean success, int appointmentId, BigDecimal amount, String transactionId, String responseCode, String message) {}
}
