package vn.clinicmanagement.domain.common.events;

public class HoaDonDaTaoSuKien extends TruuTuongMienNghiepVuSuKien {
    /** Trường `invoiceId`: hóa đơn mã. */
    private final int invoiceId;
    /** Trường `visitId`: luotkham mã. */
    private final int visitId;
    private final java.math.BigDecimal totalAmount;

    public HoaDonDaTaoSuKien(int invoiceId, int visitId, java.math.BigDecimal totalAmount) {
        super();
        this.invoiceId = invoiceId;
        this.visitId = visitId;
        this.totalAmount = totalAmount;
    }

    /** Hàm `getInvoiceId`: lấy hóa đơn mã. */
    public int getInvoiceId() { return invoiceId; }
    /** Hàm `getVisitId`: lấy luotkham mã. */
    public int getVisitId() { return visitId; }
    /** Hàm `getTotalAmount`: lấy `Total` `Amount`. */
    public java.math.BigDecimal getTotalAmount() { return totalAmount; }
}
