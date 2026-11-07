package vn.clinicmanagement.application.prescription;

import java.math.BigDecimal;

public record PrescriptionItemInput(
        int itemId,
        BigDecimal soLuongKe,
        BigDecimal donGiaKe,
        String lieuDung,
        String duongDung,
        String tanSuat,
        Integer soNgay,
        BigDecimal sang,
        BigDecimal trua,
        BigDecimal chieu,
        BigDecimal toi,
        String huongDan) {
}
