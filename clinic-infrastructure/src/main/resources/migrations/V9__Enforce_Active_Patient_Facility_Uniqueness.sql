-- Stop before changing the schema if legacy active links violate the owner-approved rule.
-- Do not include patient identifiers or facility values in the error message.
IF EXISTS (
    SELECT 1
    FROM BenhNhanCoSo
    WHERE isDeleted = 0
    GROUP BY maBenhNhan, maCoSo
    HAVING COUNT(*) > 1
)
BEGIN
    ;THROW 51090, 'V9 aborted: duplicate active patient-facility links exist; resolve them manually before retrying.', 1;
END;
GO

CREATE UNIQUE NONCLUSTERED INDEX UX_BenhNhanCoSo_Active_Patient_Facility
    ON BenhNhanCoSo(maBenhNhan, maCoSo)
    WHERE isDeleted = 0;
