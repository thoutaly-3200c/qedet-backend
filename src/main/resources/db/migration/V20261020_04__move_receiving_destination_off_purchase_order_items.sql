-- Receiving destinations are request-level workflow state.  Preserve the
-- transitional line-level value before removing obsolete PO-item ownership.
ALTER TABLE receiving_requests
    ADD COLUMN delivery_location_id BINARY(16) NULL;

UPDATE receiving_requests rr
JOIN (
    SELECT receiving_request_id, MIN(delivery_location_id) AS delivery_location_id
    FROM receiving_request_items
    WHERE delivery_location_id IS NOT NULL
    GROUP BY receiving_request_id
) destination ON destination.receiving_request_id = rr.id
SET rr.delivery_location_id = destination.delivery_location_id
WHERE rr.delivery_location_id IS NULL;

ALTER TABLE receiving_requests
    ADD CONSTRAINT fk_receiving_request_delivery_location
        FOREIGN KEY (delivery_location_id) REFERENCES organization_locations (id);

ALTER TABLE receiving_request_items
    DROP FOREIGN KEY fk_receiving_request_item_delivery_location,
    DROP COLUMN delivery_location_id;

ALTER TABLE purchase_order_items
    DROP COLUMN warehouse_id,
    DROP COLUMN delivery_location_id;
