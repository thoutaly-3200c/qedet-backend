-- Replace the legacy organization-scoped procurement catalog with canonical
-- products. This migration deliberately creates a new canonical UUID for an
-- unmapped legacy product: a legacy ID is never assumed to be a global ID.
--
-- Run this migration in a maintenance window. It is data-preserving: every
-- legacy product obtains a mapping before any reference or legacy table is
-- removed. Existing reviewed mappings in procurement_product_global_mappings
-- are retained; an exact organization SKU is reused only when it points to an
-- otherwise unmapped canonical product.

INSERT INTO procurement_product_global_mappings
    (legacy_product_id, global_product_id, organization_id, created_at)
SELECT legacy.id, organization_product.product_id, legacy.organization_id, CURRENT_TIMESTAMP(6)
FROM procurement_products legacy
JOIN organization_products organization_product
  ON organization_product.organization_id = legacy.organization_id
 AND organization_product.organization_sku = legacy.sku
LEFT JOIN procurement_product_global_mappings existing_legacy
  ON existing_legacy.legacy_product_id = legacy.id
LEFT JOIN procurement_product_global_mappings existing_global
  ON existing_global.global_product_id = organization_product.product_id
WHERE existing_legacy.legacy_product_id IS NULL
  AND existing_global.global_product_id IS NULL;

CREATE TEMPORARY TABLE procurement_product_identity_stage (
    legacy_product_id BINARY(16) NOT NULL PRIMARY KEY,
    global_product_id BINARY(16) NOT NULL UNIQUE,
    organization_id BINARY(16) NOT NULL
);

INSERT INTO procurement_product_identity_stage
    (legacy_product_id, global_product_id, organization_id)
SELECT legacy.id, UUID_TO_BIN(UUID()), legacy.organization_id
FROM procurement_products legacy
LEFT JOIN procurement_product_global_mappings mapping
  ON mapping.legacy_product_id = legacy.id
WHERE mapping.legacy_product_id IS NULL;

INSERT INTO products
    (id, name, description, brand_id, category_id, unit_id, product_type, status, created_at, updated_at)
SELECT stage.global_product_id,
       legacy.name,
       NULL,
       NULL,
       NULL,
       NULL,
       CASE legacy.type
           WHEN 'Serialized' THEN 'SERIALIZED'
           WHEN 'Measured' THEN 'WEIGHTED'
           WHEN 'Package' THEN 'BUNDLE'
           ELSE 'STANDARD'
       END,
       CASE legacy.status WHEN 'Inactive' THEN 'INACTIVE' ELSE 'ACTIVE' END,
       COALESCE(legacy.created_at, CURRENT_TIMESTAMP(6)),
       COALESCE(legacy.updated_at, CURRENT_TIMESTAMP(6))
FROM procurement_product_identity_stage stage
JOIN procurement_products legacy ON legacy.id = stage.legacy_product_id;

INSERT INTO procurement_product_global_mappings
    (legacy_product_id, global_product_id, organization_id, created_at)
SELECT stage.legacy_product_id, stage.global_product_id, stage.organization_id, CURRENT_TIMESTAMP(6)
FROM procurement_product_identity_stage stage;

-- Every canonical product used by procurement must be associated with the
-- source organization, including products supplied by a reviewed mapping.
INSERT INTO organization_products
    (id, organization_id, product_id, organization_sku, status, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), mapping.organization_id, mapping.global_product_id,
       legacy.sku,
       CASE legacy.status WHEN 'Inactive' THEN 'INACTIVE' ELSE 'ACTIVE' END,
       COALESCE(legacy.created_at, CURRENT_TIMESTAMP(6)),
       COALESCE(legacy.updated_at, CURRENT_TIMESTAMP(6))
FROM procurement_product_global_mappings mapping
JOIN procurement_products legacy ON legacy.id = mapping.legacy_product_id
LEFT JOIN organization_products organization_product
  ON organization_product.organization_id = mapping.organization_id
 AND organization_product.product_id = mapping.global_product_id
WHERE organization_product.id IS NULL;

-- Keep legacy barcode data as an organization-level identifier when it is not
-- already owned by a canonical barcode record. Conflicts are intentionally
-- left untouched for an operator to resolve before the final table drop.
INSERT INTO product_barcodes
    (id, product_id, organization_product_id, organization_id, barcode, barcode_type, is_primary, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), NULL, organization_product.id, legacy.organization_id,
       UPPER(TRIM(legacy.barcode)), 'OTHER', TRUE,
       COALESCE(legacy.created_at, CURRENT_TIMESTAMP(6)),
       COALESCE(legacy.updated_at, CURRENT_TIMESTAMP(6))
FROM procurement_products legacy
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = legacy.id
JOIN organization_products organization_product
  ON organization_product.organization_id = mapping.organization_id
 AND organization_product.product_id = mapping.global_product_id
LEFT JOIN product_barcodes barcode ON barcode.barcode = UPPER(TRIM(legacy.barcode))
WHERE legacy.barcode IS NOT NULL
  AND TRIM(legacy.barcode) <> ''
  AND barcode.id IS NULL;

-- Repoint every persisted legacy product reference before removing old columns.
UPDATE purchase_request_items request_item
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = request_item.product_id
SET request_item.global_product_id = mapping.global_product_id
WHERE request_item.global_product_id IS NULL;

UPDATE receiving_operation_items operation_item
JOIN receiving_request_items receiving_item ON receiving_item.id = operation_item.receiving_request_item_id
JOIN purchase_order_items order_item ON order_item.id = receiving_item.purchase_order_item_id
JOIN purchase_request_items request_item ON request_item.id = order_item.purchase_request_item_id
SET operation_item.global_product_id = request_item.global_product_id
WHERE operation_item.global_product_id IS NULL;

UPDATE receiving_operation_serials serial
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = serial.product_id
SET serial.global_product_id = mapping.global_product_id
WHERE serial.global_product_id IS NULL;

UPDATE inventory_balances balance
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = balance.product_id
SET balance.product_id = mapping.global_product_id;

UPDATE stock_movements movement
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = movement.product_id
SET movement.product_id = mapping.global_product_id;

UPDATE warehouse_records record_value
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = record_value.product_id
SET record_value.product_id = mapping.global_product_id;

UPDATE warehouse_record_lines line
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = line.product_id
SET line.product_id = mapping.global_product_id;

UPDATE sales_stock_request_items item
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = item.product_id
SET item.product_id = mapping.global_product_id;

UPDATE sales_receipt_items item
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = item.product_id
SET item.product_id = mapping.global_product_id;

UPDATE sales_prices price
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = price.product_id
SET price.product_id = mapping.global_product_id;

UPDATE sales_price_history history
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = history.product_id
SET history.product_id = mapping.global_product_id;

UPDATE sales_packages package_value
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = package_value.product_id
SET package_value.product_id = mapping.global_product_id;

UPDATE sales_return_items item
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = item.product_id
SET item.product_id = mapping.global_product_id;

-- QR records are polymorphic, so only PRODUCT targets are migrated.
UPDATE qr_codes qr
JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = qr.reference_id
SET qr.reference_id = mapping.global_product_id
WHERE qr.type = 'PRODUCT' AND qr.target_kind = 'PRODUCT';

-- The following result sets are mandatory preflight checks. Each must be
-- empty before executing the destructive final statements below.
SELECT request_item.id AS unmapped_purchase_request_item
FROM purchase_request_items request_item
WHERE request_item.product_id IS NOT NULL AND request_item.global_product_id IS NULL;

SELECT serial.id AS unmapped_receiving_serial
FROM receiving_operation_serials serial
WHERE serial.product_id IS NOT NULL AND serial.global_product_id IS NULL;

SELECT legacy.id AS unmapped_legacy_product
FROM procurement_products legacy
LEFT JOIN procurement_product_global_mappings mapping ON mapping.legacy_product_id = legacy.id
WHERE mapping.legacy_product_id IS NULL;

-- These legacy columns are no longer used by application code. Their foreign
-- keys are removed dynamically because Hibernate generated their names in
-- earlier deployments.
SET @request_product_fk := (
    SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_request_items'
      AND COLUMN_NAME = 'product_id' AND REFERENCED_TABLE_NAME = 'procurement_products'
    LIMIT 1
);
SET @sql := IF(@request_product_fk IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE purchase_request_items DROP FOREIGN KEY `', @request_product_fk, '`'));
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;
ALTER TABLE purchase_request_items DROP COLUMN product_id;

SET @serial_product_fk := (
    SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'receiving_operation_serials'
      AND COLUMN_NAME = 'product_id' AND REFERENCED_TABLE_NAME = 'procurement_products'
    LIMIT 1
);
SET @sql := IF(@serial_product_fk IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE receiving_operation_serials DROP FOREIGN KEY `', @serial_product_fk, '`'));
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;
ALTER TABLE receiving_operation_serials DROP COLUMN product_id;

-- The legacy entity stored supplier references as strings. Retain those
-- associations against the canonical product even though they are not part of
-- the current procurement API.
CREATE TABLE IF NOT EXISTS global_product_supplier_references (
    product_id BINARY(16) NOT NULL,
    supplier_reference VARCHAR(255) NOT NULL,
    PRIMARY KEY (product_id, supplier_reference),
    CONSTRAINT fk_global_product_supplier_reference_product
        FOREIGN KEY (product_id) REFERENCES products(id)
);

INSERT IGNORE INTO global_product_supplier_references (product_id, supplier_reference)
SELECT mapping.global_product_id, legacy_supplier.supplier_id
FROM product_suppliers legacy_supplier
JOIN procurement_product_global_mappings mapping
  ON mapping.legacy_product_id = legacy_supplier.product_id;

DROP TABLE IF EXISTS product_suppliers;
DROP TABLE procurement_product_global_mappings;
DROP TABLE procurement_products;
DROP TEMPORARY TABLE IF EXISTS procurement_product_identity_stage;
