ALTER TABLE receiving_operation_items
    ADD COLUMN product_resolution VARCHAR(40) NOT NULL DEFAULT 'PENDING_PRODUCT';
