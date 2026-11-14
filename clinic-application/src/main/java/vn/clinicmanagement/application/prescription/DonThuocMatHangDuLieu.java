package vn.clinicmanagement.application.prescription;

import java.math.BigDecimal;

public record DonThuocMatHangDuLieu(
        /** Trường `id`: mã. */
        int id,
        /** Trường `itemId`: mathang mã. */
        int itemId,
        /** Trường `itemCode`: mathang mã. */
        String itemCode,
        /** Trường `itemName`: mathang tên. */
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
