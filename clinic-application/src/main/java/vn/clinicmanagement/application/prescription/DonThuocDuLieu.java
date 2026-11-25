package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DonThuocDuLieu(
        int id,
        int visitId,
        int doctorId,
        String doctorName,
        int facilityId,
        Integer warehouseId,
        TrangThaiDonThuoc trangThai,
        LocalDateTime ngayKe,
        String ghiChu,
        String loiDanDonThuoc,
        BigDecimal tongTien,
        List<DonThuocMatHangDuLieu> items) {

    /**
     * Phân biệt phiếu vật tư y tế (`VAT_TU`) với đơn thuốc (`THUOC`) khi lưu chung bảng `DonThuoc`.
     */
    public boolean isSupplyOrder() {
        if (ghiChu != null && ghiChu.startsWith("[VAT_TU]")) {
            return true;
        }
        return items != null && !items.isEmpty()
                && items.stream().allMatch(item -> item.loaiItem() == LoaiMatHang.VAT_TU);
    }
}
