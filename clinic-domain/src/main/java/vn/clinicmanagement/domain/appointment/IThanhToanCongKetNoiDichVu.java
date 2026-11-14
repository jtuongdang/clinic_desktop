package vn.clinicmanagement.domain.appointment;
import java.math.BigDecimal;
public interface IThanhToanCongKetNoiDichVu {
    /** Hàm `createPaymentUrl`: tạo thanhtoan `Url`. */
    PaymentResult createPaymentUrl(int appointmentId, BigDecimal amount, String description, String returnUrl);
    /** Hàm `processCallback`: xử lý `Callback`. */
    PaymentCallbackResult processCallback(String queryParams);
    /** Trường `success`: thành công. */
    /** Trường `paymentUrl`: thanhtoan `Url`. */
    /** Trường `transactionId`: giao dịch mã. */
    /** Trường `message`: thông báo. */
    record PaymentResult(boolean success, String paymentUrl, String transactionId, String message) {}
    /** Trường `success`: thành công. */
    /** Trường `appointmentId`: lịch hẹn mã. */
    /** Trường `transactionId`: giao dịch mã. */
    /** Trường `responseCode`: `response` mã. */
    /** Trường `message`: thông báo. */
    record PaymentCallbackResult(boolean success, int appointmentId, BigDecimal amount, String transactionId, String responseCode, String message) {}
}
