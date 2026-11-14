package vn.clinicmanagement.application.cls;

public record XetNghiemKetQuaDuLieu(
        /** Trường `id`: mã. */
        int id,
        /** Trường `orderId`: yêu cầu mã. */
        int orderId,
        /** Trường `indicatorName`: `indicator` tên. */
        String indicatorName,
        /** Trường `value`: giá trị. */
        String value,
        /** Trường `unit`: đơn vị. */
        String unit,
        /** Trường `referenceRange`: `reference` khoang. */
        String referenceRange,
        String evaluation
) {}
