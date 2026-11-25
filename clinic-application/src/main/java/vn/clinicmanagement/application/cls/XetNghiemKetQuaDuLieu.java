package vn.clinicmanagement.application.cls;

public record XetNghiemKetQuaDuLieu(
        int id,
        int orderId,
        String indicatorName,
        String value,
        String unit,
        String referenceRange,
        String evaluation
) {}
