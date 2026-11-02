-- Sales fulfillment is an explicit workflow, not a generic warehouse record.
-- Existing inventory tables and history are deliberately untouched.
ALTER TABLE sales_stock_requests ADD COLUMN sales_hall_zone_id BINARY(16) NULL;
UPDATE sales_stock_requests r
JOIN site_zones z ON z.organization_site_id = r.sales_hall_id AND z.active = TRUE AND z.is_default = TRUE
SET r.sales_hall_zone_id = z.id
WHERE r.sales_hall_zone_id IS NULL;
-- Deployments must provision a default Sales Hall zone before this migration.
ALTER TABLE sales_stock_requests MODIFY sales_hall_zone_id BINARY(16) NOT NULL;
ALTER TABLE sales_stock_requests ADD CONSTRAINT fk_sales_request_hall_zone FOREIGN KEY (sales_hall_zone_id) REFERENCES site_zones(id);

ALTER TABLE sales_stock_request_items
    ADD COLUMN fulfilled_quantity DECIMAL(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN pending_acceptance_quantity DECIMAL(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN accepted_quantity DECIMAL(14,2) NOT NULL DEFAULT 0;

CREATE TABLE sales_stock_fulfillments (
    id BINARY(16) NOT NULL PRIMARY KEY, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    organization_id BINARY(16) NOT NULL, sales_stock_request_id BINARY(16) NOT NULL,
    destination_zone_id BINARY(16) NOT NULL, status VARCHAR(32) NOT NULL,
    confirmed_by BINARY(16) NOT NULL, decided_by BINARY(16) NULL, decision_notes VARCHAR(1000) NULL,
    CONSTRAINT fk_sales_fulfillment_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_sales_fulfillment_request FOREIGN KEY (sales_stock_request_id) REFERENCES sales_stock_requests(id),
    CONSTRAINT fk_sales_fulfillment_destination FOREIGN KEY (destination_zone_id) REFERENCES site_zones(id),
    INDEX idx_sales_fulfillment_request (sales_stock_request_id)
);
CREATE TABLE sales_stock_fulfillment_lines (
    id BINARY(16) NOT NULL PRIMARY KEY, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    fulfillment_id BINARY(16) NOT NULL, product_id BINARY(16) NOT NULL, quantity DECIMAL(14,2) NOT NULL,
    CONSTRAINT fk_sales_fulfillment_line FOREIGN KEY (fulfillment_id) REFERENCES sales_stock_fulfillments(id)
);
CREATE TABLE sales_stock_fulfillment_allocations (
    id BINARY(16) NOT NULL PRIMARY KEY, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    fulfillment_line_id BINARY(16) NOT NULL, source_zone_id BINARY(16) NOT NULL, quantity DECIMAL(14,2) NOT NULL,
    CONSTRAINT fk_sales_fulfillment_allocation_line FOREIGN KEY (fulfillment_line_id) REFERENCES sales_stock_fulfillment_lines(id),
    CONSTRAINT fk_sales_fulfillment_allocation_zone FOREIGN KEY (source_zone_id) REFERENCES site_zones(id)
);

-- Preserve old generic record rows for audit. They are renamed so no runtime
-- workflow or JPA mapping can continue using them. A later retention migration
-- may export and drop these audit tables under the organization's retention policy.
RENAME TABLE warehouse_record_lines TO warehouse_record_line_audit_legacy,
             warehouse_records TO warehouse_record_audit_legacy,
             sales_receipt_items TO sales_receipt_item_audit_legacy,
             sales_receipts TO sales_receipt_audit_legacy;
