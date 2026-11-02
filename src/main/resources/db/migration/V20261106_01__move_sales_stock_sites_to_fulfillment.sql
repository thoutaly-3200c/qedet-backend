-- Preserve site choices for existing dispatches before removing them from requests.
ALTER TABLE sales_stock_fulfillments
    ADD COLUMN warehouse_id BINARY(16) NULL,
    ADD COLUMN sales_hall_id BINARY(16) NULL;

UPDATE sales_stock_fulfillments f
JOIN sales_stock_requests r ON r.id = f.sales_stock_request_id
SET f.warehouse_id = r.warehouse_id, f.sales_hall_id = r.sales_hall_id;

-- Existing accepted requests predate item-level approval tracking.
UPDATE sales_stock_request_items i
JOIN sales_stock_requests r ON r.id = i.sales_stock_request_id
SET i.approved_quantity = i.requested_quantity
WHERE r.status IN ('ACCEPTED', 'PARTIALLY_FULFILLED', 'PENDING_ACCEPTANCE', 'FULFILLED')
  AND i.approved_quantity = 0;

ALTER TABLE sales_stock_fulfillments
    MODIFY warehouse_id BINARY(16) NOT NULL,
    MODIFY sales_hall_id BINARY(16) NOT NULL,
    ADD CONSTRAINT fk_sales_fulfillment_warehouse FOREIGN KEY (warehouse_id) REFERENCES organization_sites(id),
    ADD CONSTRAINT fk_sales_fulfillment_hall FOREIGN KEY (sales_hall_id) REFERENCES organization_sites(id);

ALTER TABLE sales_stock_requests DROP FOREIGN KEY fk_sales_request_hall_zone;
ALTER TABLE sales_stock_requests
    DROP COLUMN sales_hall_zone_id,
    DROP COLUMN sales_hall_id,
    DROP COLUMN warehouse_id;
