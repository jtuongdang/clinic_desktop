package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;

/** Trường `itemId`: mathang mã. */
public record MuaHangYeuCauMatHangDauVao(int itemId, BigDecimal soLuong, BigDecimal donGia) {
}
