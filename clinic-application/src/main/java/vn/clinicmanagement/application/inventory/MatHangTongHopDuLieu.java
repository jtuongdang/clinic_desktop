package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.domain.common.enums.LoaiMatHang;

import java.math.BigDecimal;

public record MatHangTongHopDuLieu(
        /** Trường `id`: mã. */
        int id,
        /** Trường `maItem`: `ma` mathang. */
        String maItem,
        /** Trường `tenItem`: `ten` mathang. */
        String tenItem,
        /** Trường `loaiItem`: `loai` mathang. */
        LoaiMatHang loaiItem,
        String hoatChat,
        String hamLuong,
        String donViCoBan,
        BigDecimal giaBanMacDinh,
        boolean hoatDong) {
}
