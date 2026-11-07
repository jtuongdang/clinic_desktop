package vn.clinicmanagement.application.cls;

public record LabResultItemInput(
        String indicatorName,
        String value,
        String unit,
        String referenceRange,
        String evaluation
) {}
