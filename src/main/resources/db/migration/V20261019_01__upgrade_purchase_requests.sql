-- Preserve request-item identifiers and historical rows while moving to the
-- explicit request-item table and split workflow statuses.
RENAME TABLE purchase_request_lines TO purchase_request_items;

UPDATE purchase_requests
SET priority = 'NORMAL'
WHERE UPPER(priority) IN ('MEDIUM', 'NORMAL');
UPDATE purchase_requests
SET priority = 'LOW'
WHERE UPPER(priority) = 'LOW';
UPDATE purchase_requests
SET priority = 'HIGH'
WHERE UPPER(priority) = 'HIGH';

ALTER TABLE purchase_requests
    ADD COLUMN request_type VARCHAR(32) NULL,
    ADD COLUMN title VARCHAR(255) NULL,
    ADD COLUMN reason VARCHAR(2000) NULL,
    ADD COLUMN requested_by_id BINARY(16) NULL,
    ADD COLUMN source_domain VARCHAR(60) NULL,
    ADD COLUMN source_reference VARCHAR(160) NULL,
    ADD COLUMN procurement_status VARCHAR(20) NULL,
    ADD COLUMN originating_request_status VARCHAR(16) NULL,
    ADD COLUMN attachment_reference VARCHAR(1000) NULL,
    ADD COLUMN currency VARCHAR(3) NULL,
    ADD COLUMN version BIGINT NULL;

UPDATE purchase_requests
SET request_type = 'STANDARD',
    title = COALESCE(NULLIF(number, ''), 'Legacy purchase request'),
    reason = COALESCE(NULLIF(notes, ''), 'Migrated legacy purchase request'),
    procurement_status = CASE UPPER(status)
        WHEN 'APPROVED' THEN 'APPROVED'
        WHEN 'REJECTED' THEN 'REJECTED'
        WHEN 'COMPLETED' THEN 'COMPLETED'
        ELSE 'SUBMITTED'
    END,
    originating_request_status = 'OPEN',
    version = 0
WHERE request_type IS NULL;

-- Only valid legacy UUID requester values are copied; rows without a usable
-- identity must be assigned by the deployment data migration before NOT NULL
-- is enforced for requested_by_id.
UPDATE purchase_requests
SET requested_by_id = UNHEX(REPLACE(requested_by, '-', ''))
WHERE requested_by REGEXP '^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$';

ALTER TABLE purchase_request_items
    ADD COLUMN item_category VARCHAR(120) NULL,
    ADD COLUMN description VARCHAR(2000) NULL,
    ADD COLUMN currency VARCHAR(3) NULL,
    ADD COLUMN notes VARCHAR(2000) NULL;

-- Hibernate's former @Column(unique = true) used a generated index name.
-- Remove that single-column unique index before creating the organization scope.
SET @legacy_number_unique_index = (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'purchase_requests'
      AND column_name = 'number'
      AND non_unique = 0
    GROUP BY index_name
    HAVING COUNT(*) = 1
    LIMIT 1
);
SET @drop_legacy_number_unique_index = IF(
    @legacy_number_unique_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE purchase_requests DROP INDEX `', @legacy_number_unique_index, '`')
);
PREPARE drop_legacy_number_unique_index FROM @drop_legacy_number_unique_index;
EXECUTE drop_legacy_number_unique_index;
DEALLOCATE PREPARE drop_legacy_number_unique_index;

CREATE UNIQUE INDEX uk_purchase_request_org_number
    ON purchase_requests (organization_id, number);
