package vn.clinicmanagement.domain.common.events;

public class ThanhToanDaGhiNhanSuKien extends TruuTuongMienNghiepVuSuKien {
    /** Trường `invoiceId`: hóa đơn mã. */
    private final int invoiceId;
    private final java.math.BigDecimal amountPaid;
    /** Trường `paymentMethod`: thanhtoan `Method`. */
    private final String paymentMethod;

    public ThanhToanDaGhiNhanSuKien(int invoiceId, java.math.BigDecimal amountPaid, String paymentMethod) {
        super();
        this.invoiceId = invoiceId;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
    }

    /** Hàm `getInvoiceId`: lấy hóa đơn mã. */
    public int getInvoiceId() { return invoiceId; }
    /** Hàm `getAmountPaid`: lấy `Amount` `Paid`. */
    public java.math.BigDecimal getAmountPaid() { return amountPaid; }
    /** Hàm `getPaymentMethod`: lấy thanhtoan `Method`. */
    public String getPaymentMethod() { return paymentMethod; }
}
