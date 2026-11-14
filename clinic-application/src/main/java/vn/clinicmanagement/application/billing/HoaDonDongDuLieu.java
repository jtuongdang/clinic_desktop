package vn.clinicmanagement.application.billing;

import java.math.BigDecimal;

public record HoaDonDongDuLieu(
        /** Trường `id`: mã. */
        int id,
        /** Trường `itemType`: mathang loai. */
        String itemType,
        /** Trường `sourceId`: nguồn mã. */
        int sourceId,
        String description,
        BigDecimal quantity,
        /** Trường `unitPrice`: đơn vị `Price`. */
        BigDecimal unitPrice,
        /** Trường `lineTotal`: đóng `Total`. */
        BigDecimal lineTotal
) {}
