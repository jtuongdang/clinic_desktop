package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionDto(
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
        List<PrescriptionItemDto> items) {
}
