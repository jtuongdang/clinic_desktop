package vn.clinicmanagement.application.prescription;

import java.math.BigDecimal;

public record PrescriptionItemDto(
        int id,
        int itemId,
        String itemCode,
        String itemName,
        String donViTinh,
        BigDecimal soLuongKe,
        BigDecimal soLuongDaXuat,
        BigDecimal donGiaKe,
        BigDecimal thanhTien,
        String duongDung,
        String lieuDung,
        String tanSuat,
        Integer soNgay,
        BigDecimal sang,
        BigDecimal trua,
        BigDecimal chieu,
        BigDecimal toi,
        String huongDan) {
}
