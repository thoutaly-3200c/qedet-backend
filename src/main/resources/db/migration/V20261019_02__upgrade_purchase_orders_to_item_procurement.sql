-- Move the established line table to the item-level purchase-order model.
-- Legacy orders that cannot be tied to a purchase request/employee remain nullable
-- during this deployment; resolve them before enforcing NOT NULL in a follow-up
-- operational migration. New application writes always provide these values.
RENAME TABLE purchase_order_lines TO purchase_order_items;

ALTER TABLE purchase_orders
    ADD COLUMN purchase_request_id BINARY(16) NULL,
    ADD COLUMN created_by BINARY(16) NULL,
    ADD COLUMN version BIGINT NULL;

UPDATE purchase_orders po
JOIN purchase_requests pr ON pr.id = UNHEX(REPLACE(po.pr_id, '-', ''))
SET po.purchase_request_id = pr.id,
    po.created_by = pr.requested_by_id
WHERE po.purchase_request_id IS NULL
  AND po.pr_id REGEXP '^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$';

UPDATE purchase_orders
SET status = CASE UPPER(status)
    WHEN 'CONFIRMED' THEN 'CONFIRMED'
    WHEN 'COMPLETED' THEN 'CLOSED'
    WHEN 'CANCELLED' THEN 'CANCELLED'
    ELSE 'DRAFT' END;

ALTER TABLE purchase_order_items
    ADD COLUMN purchase_request_item_id BINARY(16) NULL,
    ADD COLUMN supplier_id BINARY(16) NULL,
    ADD COLUMN order_date DATE NULL,
    ADD COLUMN expected_delivery_date DATE NULL,
    ADD COLUMN warehouse_id BINARY(16) NULL,
    ADD COLUMN delivery_location_id BINARY(16) NULL,
    ADD COLUMN currency VARCHAR(3) NULL,
    ADD COLUMN payment_terms VARCHAR(60) NULL,
    ADD COLUMN payment_method VARCHAR(20) NULL,
    ADD COLUMN payment_timing VARCHAR(20) NULL,
    ADD COLUMN quantity DECIMAL(19,4) NULL,
    ADD COLUMN total_price DECIMAL(19,2) NULL,
    ADD COLUMN discount_amount DECIMAL(19,2) NULL DEFAULT 0,
    ADD COLUMN tax_amount DECIMAL(19,2) NULL DEFAULT 0,
    ADD COLUMN shipping_amount DECIMAL(19,2) NULL DEFAULT 0,
    ADD COLUMN order_status VARCHAR(24) NULL,
    ADD COLUMN receiving_status VARCHAR(24) NULL,
    ADD COLUMN created_by BINARY(16) NULL,
    ADD COLUMN version BIGINT NULL;

UPDATE purchase_order_items poi
JOIN purchase_orders po ON po.id = poi.purchase_order_id
LEFT JOIN purchase_request_items pri ON pri.purchase_request_id = po.purchase_request_id AND pri.product_id = poi.product_id
SET poi.purchase_request_item_id = pri.id,
    poi.supplier_id = po.supplier_id,
    poi.order_date = po.date,
    poi.expected_delivery_date = po.expected_date,
    poi.currency = COALESCE((SELECT currency FROM suppliers s WHERE s.id = po.supplier_id), 'SAR'),
    poi.payment_terms = po.payment_terms,
    poi.payment_method = po.payment_method,
    poi.payment_timing = po.payment_timing,
    poi.quantity = poi.order_qty,
    poi.total_price = COALESCE(poi.line_total, 0),
    poi.discount_amount = 0,
    poi.tax_amount = 0,
    poi.shipping_amount = 0,
    poi.order_status = CASE UPPER(po.status)
        WHEN 'CANCELLED' THEN 'CANCELLED'
        WHEN 'COMPLETED' THEN 'CLOSED'
        WHEN 'FULLY_RECEIVED' THEN 'READY_TO_RECEIVE'
        WHEN 'PARTIALLY_RECEIVED' THEN 'READY_TO_RECEIVE'
        WHEN 'CONFIRMED' THEN 'CONFIRMED'
        ELSE 'DRAFT' END,
    poi.receiving_status = CASE UPPER(po.receiving_status)
        WHEN 'FULLY_RECEIVED' THEN 'FULLY_RECEIVED'
        WHEN 'PARTIALLY_RECEIVED' THEN 'PARTIALLY_RECEIVED'
        WHEN 'IN_PROGRESS' THEN 'RECEIVING'
        ELSE 'NOT_STARTED' END,
    poi.created_by = po.created_by,
    poi.version = 0;

ALTER TABLE warehouse_record_lines ADD COLUMN purchase_order_item_id BINARY(16) NULL;
ALTER TABLE finance_payment_requests ADD COLUMN purchase_order_item_id BINARY(16) NULL;

CREATE INDEX idx_purchase_order_org_number ON purchase_orders (organization_id, number);
CREATE INDEX idx_purchase_order_item_request_item ON purchase_order_items (purchase_request_item_id);
CREATE INDEX idx_purchase_order_item_supplier ON purchase_order_items (supplier_id);
CREATE INDEX idx_warehouse_record_line_po_item ON warehouse_record_lines (purchase_order_item_id);
