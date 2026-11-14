package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;

/** Trường `purchaseOrderId`: muahang yêu cầu mã. */
public record CreateMuaHangYeuCauBienNhan(int purchaseOrderId, String soDonDatHang, BigDecimal tongTien) {
}
