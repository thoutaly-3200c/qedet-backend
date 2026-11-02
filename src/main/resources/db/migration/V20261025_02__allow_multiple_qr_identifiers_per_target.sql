-- A product may have a Qedet-generated QR plus supplier/manufacturer QR identifiers.
-- Identifier ownership remains unique inside an organization; target lookup stays indexed.
-- This migration is non-destructive and is safe to rerun manually if the index was
-- created by an earlier Hibernate schema update.

SET @exists := (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'qr_codes'
      AND INDEX_NAME = 'uk_qr_codes_target_identity'
);
SET @sql := IF(@exists = 0, 'SELECT 1',
    'ALTER TABLE qr_codes DROP INDEX uk_qr_codes_target_identity');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'qr_codes'
      AND INDEX_NAME = 'idx_qr_codes_target_identity'
);
SET @sql := IF(@exists > 0, 'SELECT 1',
    'ALTER TABLE qr_codes ADD INDEX idx_qr_codes_target_identity '
    '(organization_id, type, target_kind, reference_id)');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
