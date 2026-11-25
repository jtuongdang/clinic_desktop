package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.domain.common.enums.LoaiMatHang;

import java.math.BigDecimal;

public record MatHangTongHopDuLieu(
        int id,
        String maItem,
        String tenItem,
        LoaiMatHang loaiItem,
        String hoatChat,
        String hamLuong,
        String donViCoBan,
        BigDecimal giaBanMacDinh,
        boolean hoatDong) {
}
