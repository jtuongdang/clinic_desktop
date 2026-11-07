package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.domain.common.enums.LoaiItem;

import java.math.BigDecimal;

public record ItemSummaryDto(
        int id,
        String maItem,
        String tenItem,
        LoaiItem loaiItem,
        String hoatChat,
        String hamLuong,
        String donViCoBan,
        BigDecimal giaBanMacDinh,
        boolean hoatDong) {
}
