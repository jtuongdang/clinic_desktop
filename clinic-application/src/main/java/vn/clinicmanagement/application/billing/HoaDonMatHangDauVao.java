package vn.clinicmanagement.application.billing;

import java.math.BigDecimal;

public record HoaDonMatHangDauVao(
        /** Trường `itemType`: mathang loai. */
        String itemType,
        /** Trường `sourceId`: nguồn mã. */
        int sourceId,
        String description,
        BigDecimal quantity,
        /** Trường `unitPrice`: đơn vị `Price`. */
        BigDecimal unitPrice
) {}
