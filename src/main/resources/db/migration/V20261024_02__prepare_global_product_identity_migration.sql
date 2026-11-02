-- Product identity migration is deliberately mapping-gated.  Legacy product IDs
-- are not equal to global product IDs and must never be copied by value.

CREATE TABLE IF NOT EXISTS procurement_product_global_mappings (
    legacy_product_id BINARY(16) NOT NULL,
    global_product_id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (legacy_product_id),
    UNIQUE KEY uk_procurement_product_global_mapping_global (global_product_id),
    CONSTRAINT fk_procurement_product_mapping_legacy
        FOREIGN KEY (legacy_product_id) REFERENCES procurement_products (id),
    CONSTRAINT fk_procurement_product_mapping_global
        FOREIGN KEY (global_product_id) REFERENCES products (id),
    CONSTRAINT fk_procurement_product_mapping_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

ALTER TABLE purchase_request_items
    ADD COLUMN IF NOT EXISTS global_product_id BINARY(16) NULL,
    ADD INDEX IF NOT EXISTS idx_purchase_request_item_global_product (global_product_id),
    ADD CONSTRAINT fk_purchase_request_item_global_product
        FOREIGN KEY (global_product_id) REFERENCES products (id);

ALTER TABLE receiving_operation_serials
    ADD COLUMN IF NOT EXISTS global_product_id BINARY(16) NULL,
    ADD INDEX IF NOT EXISTS idx_receiving_serial_global_product (global_product_id),
    ADD CONSTRAINT fk_receiving_serial_global_product
        FOREIGN KEY (global_product_id) REFERENCES products (id);

-- Backfill only explicit, organization-matched mappings.  Rows that do not
-- have a reviewed mapping remain NULL and are rejected by the receiving flow.
UPDATE purchase_request_items pri
JOIN purchase_requests pr ON pr.id = pri.purchase_request_id
JOIN procurement_product_global_mappings map
  ON map.legacy_product_id = pri.product_id
 AND map.organization_id = pr.organization_id
SET pri.global_product_id = map.global_product_id
WHERE pri.global_product_id IS NULL;

UPDATE receiving_operation_serials serial
JOIN receiving_operation_items operation_item
  ON operation_item.id = serial.receiving_operation_item_id
JOIN receiving_request_items request_item
  ON request_item.id = operation_item.receiving_request_item_id
JOIN purchase_order_items purchase_order_item
  ON purchase_order_item.id = request_item.purchase_order_item_id
JOIN purchase_request_items pri
  ON pri.id = purchase_order_item.purchase_request_item_id
SET serial.global_product_id = pri.global_product_id
WHERE serial.global_product_id IS NULL
  AND pri.global_product_id IS NOT NULL;
