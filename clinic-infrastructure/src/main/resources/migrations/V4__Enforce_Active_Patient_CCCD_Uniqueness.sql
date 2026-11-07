-- Dừng trước khi thay đổi schema nếu các hồ sơ đang hoạt động đã có CCCD trùng.
-- Thông báo không chứa CCCD hoặc dữ liệu định danh của hồ sơ.
IF EXISTS (
    SELECT 1
    FROM BenhNhan
    WHERE isDeleted = 0
      AND NULLIF(LTRIM(RTRIM(soCCCD)), '') IS NOT NULL
    GROUP BY UPPER(LTRIM(RTRIM(soCCCD)))
    HAVING COUNT(*) > 1
)
BEGIN
    ;THROW 51000, 'V4 aborted: duplicate active CCCD records exist; resolve them manually before retrying.', 1;
END;
GO

-- Active, nonblank CCCDs share a normalized key. Deleted or blank records get
-- per-row keys so they do not consume the unique key space.
ALTER TABLE BenhNhan
ADD activeCccdUniqueKey AS (
    CASE
        WHEN isDeleted = 0 AND NULLIF(LTRIM(RTRIM(soCCCD)), '') IS NOT NULL
            THEN 'CCCD:' + UPPER(LTRIM(RTRIM(soCCCD)))
        ELSE 'ROW:' + CONVERT(VARCHAR(20), id)
    END
) PERSISTED;
GO

CREATE UNIQUE NONCLUSTERED INDEX UX_BenhNhan_Active_CCCD
    ON BenhNhan(activeCccdUniqueKey);
