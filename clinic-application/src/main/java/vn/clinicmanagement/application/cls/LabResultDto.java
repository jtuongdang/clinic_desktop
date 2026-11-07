package vn.clinicmanagement.application.cls;

public record LabResultDto(
        int id,
        int orderId,
        String indicatorName,
        String value,
        String unit,
        String referenceRange,
        String evaluation
) {}
