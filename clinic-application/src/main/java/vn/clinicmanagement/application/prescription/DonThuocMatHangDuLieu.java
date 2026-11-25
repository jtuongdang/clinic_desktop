package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.domain.common.enums.LoaiMatHang;

import java.math.BigDecimal;

public record DonThuocMatHangDuLieu(
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
        String huongDan,
        LoaiMatHang loaiItem) {

    public DonThuocMatHangDuLieu(
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
        this(id, itemId, itemCode, itemName, donViTinh, soLuongKe, soLuongDaXuat, donGiaKe, thanhTien,
                duongDung, lieuDung, tanSuat, soNgay, sang, trua, chieu, toi, huongDan, LoaiMatHang.THUOC);
    }
}
