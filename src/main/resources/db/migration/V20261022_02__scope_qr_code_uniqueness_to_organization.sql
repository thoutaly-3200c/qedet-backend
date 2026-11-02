-- Allow the same external supplier QR identifier to exist in different organizations.
-- Within an organization the identifier remains unique.
--
-- Apply manually against MySQL (Flyway is not wired in this project; Hibernate ddl-auto=update
-- may also pick up the matching JPA unique constraint on restart).

-- Preflight: this must return no rows.  Do not remove or merge records if it
-- does return rows; resolve the data inconsistency before adding the unique
-- index below.  A legacy globally-unique code index normally guarantees this.
SELECT organization_id, code, COUNT(*) AS duplicate_count
FROM qr_codes
GROUP BY organization_id, code
HAVING COUNT(*) > 1;

-- Drop the legacy global unique index on qr_codes.code (name varies by how it was created).
SET @idx := (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'qr_codes'
      AND COLUMN_NAME = 'code'
      AND NON_UNIQUE = 0
      AND INDEX_NAME <> 'PRIMARY'
      AND SEQ_IN_INDEX = 1
    ORDER BY CASE WHEN INDEX_NAME = 'idx_qr_codes_code' THEN 0 ELSE 1 END
    LIMIT 1
);
SET @sql := IF(@idx IS NULL, 'SELECT 1', CONCAT('ALTER TABLE qr_codes DROP INDEX `', @idx, '`'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Composite uniqueness: one identifier per organization.
SET @exists := (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'qr_codes'
      AND INDEX_NAME = 'uk_qr_codes_organization_code'
);
SET @sql := IF(@exists > 0, 'SELECT 1',
    'ALTER TABLE qr_codes ADD UNIQUE INDEX uk_qr_codes_organization_code (organization_id, code)');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Keep a non-unique lookup index on code for rare public/ambiguous resolutions.
SET @exists := (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'qr_codes'
      AND INDEX_NAME = 'idx_qr_codes_code'
);
SET @sql := IF(@exists > 0, 'SELECT 1',
    'ALTER TABLE qr_codes ADD INDEX idx_qr_codes_code (code)');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
