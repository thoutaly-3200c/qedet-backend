ALTER TABLE receiving_operation_items
    ADD COLUMN global_product_id BINARY(16) NULL,
    ADD INDEX idx_receiving_operation_item_global_product (global_product_id),
    ADD CONSTRAINT fk_receiving_operation_item_global_product
        FOREIGN KEY (global_product_id) REFERENCES products (id);
