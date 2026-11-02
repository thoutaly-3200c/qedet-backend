ALTER TABLE receiving_request_items
    ADD COLUMN delivery_location_id BINARY(16) NULL;

UPDATE receiving_request_items rri
JOIN purchase_order_items poi ON poi.id = rri.purchase_order_item_id
SET rri.delivery_location_id = poi.delivery_location_id
WHERE rri.delivery_location_id IS NULL;

ALTER TABLE receiving_request_items
    ADD CONSTRAINT fk_receiving_request_item_delivery_location
        FOREIGN KEY (delivery_location_id) REFERENCES organization_locations (id);
