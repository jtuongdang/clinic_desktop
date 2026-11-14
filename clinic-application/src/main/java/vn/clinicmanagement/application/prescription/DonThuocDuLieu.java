package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DonThuocDuLieu(
        /** Trường `id`: mã. */
        int id,
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `doctorId`: bacsi mã. */
        int doctorId,
        /** Trường `doctorName`: bacsi tên. */
        String doctorName,
        /** Trường `facilityId`: cơ sở mã. */
        int facilityId,
        /** Trường `warehouseId`: kho mã. */
        Integer warehouseId,
        TrangThaiDonThuoc trangThai,
        LocalDateTime ngayKe,
        String ghiChu,
        String loiDanDonThuoc,
        BigDecimal tongTien,
        List<DonThuocMatHangDuLieu> items) {
}
