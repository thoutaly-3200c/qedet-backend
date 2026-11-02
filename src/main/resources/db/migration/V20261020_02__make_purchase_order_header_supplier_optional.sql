-- Suppliers are assigned per purchase-order item. Retain legacy header values
-- and their foreign key, but allow new multi-supplier orders to omit the header.
ALTER TABLE purchase_orders
    MODIFY COLUMN supplier_id BINARY(16) NULL;
