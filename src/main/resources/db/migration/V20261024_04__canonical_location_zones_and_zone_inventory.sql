-- Development/test reset migration. It intentionally removes only inventory topology and
-- inventory workflow data. Run the documented scoped reset before this migration if a
-- shared database contains other organizations.

DROP TABLE IF EXISTS receiving_operation_zone_assignments;
DROP TABLE IF EXISTS warehouse_bins;
DROP TABLE IF EXISTS warehouse_zones;
DROP TABLE IF EXISTS warehouse_locations;

CREATE TABLE warehouse_locations (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    organization_location_id BINARY(16) NOT NULL,
    parent_id BINARY(16) NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500) NULL,
    type VARCHAR(12) NOT NULL,
    active BIT NOT NULL,
    is_default BIT NOT NULL DEFAULT FALSE,
    capacity DECIMAL(14,2) NULL,
    version BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_warehouse_location_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_warehouse_location_physical FOREIGN KEY (organization_location_id) REFERENCES organization_locations(id),
    CONSTRAINT fk_warehouse_location_parent FOREIGN KEY (parent_id) REFERENCES warehouse_locations(id),
    CONSTRAINT uk_warehouse_location_zone_code UNIQUE (organization_location_id, code, type),
    CONSTRAINT uk_warehouse_location_bin_code UNIQUE (parent_id, code, type),
    INDEX idx_warehouse_location_org_physical (organization_id, organization_location_id),
    INDEX idx_warehouse_location_parent (parent_id)
);

CREATE TABLE receiving_operation_zone_assignments (
    id BINARY(16) NOT NULL,
    receiving_operation_item_id BINARY(16) NOT NULL,
    zone_id BINARY(16) NOT NULL,
    accepted_quantity DECIMAL(19,4) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_receiving_operation_item_zone UNIQUE (receiving_operation_item_id, zone_id),
    CONSTRAINT fk_receiving_zone_assignment_item FOREIGN KEY (receiving_operation_item_id) REFERENCES receiving_operation_items(id),
    CONSTRAINT fk_receiving_zone_assignment_zone FOREIGN KEY (zone_id) REFERENCES warehouse_locations(id),
    INDEX idx_receiving_zone_assignment_zone (zone_id)
);

-- Existing test balances and movement history use unvalidated strings. They are reset;
-- no old quantity is reallocated to a zone by this migration.
DELETE FROM stock_movements;
DELETE FROM inventory_balances;
ALTER TABLE inventory_balances DROP INDEX idx_inventory_org_product_location;
ALTER TABLE inventory_balances DROP COLUMN location_code;
ALTER TABLE inventory_balances ADD COLUMN zone_id BINARY(16) NOT NULL;
ALTER TABLE inventory_balances ADD COLUMN version BIGINT NULL;
ALTER TABLE inventory_balances ADD CONSTRAINT fk_inventory_balance_zone FOREIGN KEY (zone_id) REFERENCES warehouse_locations(id);
ALTER TABLE inventory_balances ADD CONSTRAINT uk_inventory_org_product_zone UNIQUE (organization_id, product_id, zone_id);
ALTER TABLE inventory_balances ADD INDEX idx_inventory_org_product_zone (organization_id, product_id, zone_id);

ALTER TABLE stock_movements DROP COLUMN from_location;
ALTER TABLE stock_movements DROP COLUMN to_location;
ALTER TABLE stock_movements ADD COLUMN from_zone_id BINARY(16) NULL;
ALTER TABLE stock_movements ADD COLUMN to_zone_id BINARY(16) NULL;
ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movement_from_zone FOREIGN KEY (from_zone_id) REFERENCES warehouse_locations(id);
ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movement_to_zone FOREIGN KEY (to_zone_id) REFERENCES warehouse_locations(id);
ALTER TABLE stock_movements ADD INDEX idx_stock_movement_from_zone (from_zone_id);
ALTER TABLE stock_movements ADD INDEX idx_stock_movement_to_zone (to_zone_id);

ALTER TABLE receiving_requests DROP FOREIGN KEY fk_receiving_request_warehouse;
ALTER TABLE receiving_requests DROP INDEX uk_receiving_request_order_warehouse;
ALTER TABLE receiving_requests DROP INDEX idx_receiving_request_org_warehouse_status;
ALTER TABLE receiving_requests CHANGE COLUMN warehouse_id destination_location_id BINARY(16) NULL;
ALTER TABLE receiving_requests ADD CONSTRAINT fk_receiving_request_destination_location FOREIGN KEY (destination_location_id) REFERENCES organization_locations(id);
ALTER TABLE receiving_requests ADD CONSTRAINT uk_receiving_request_order_destination UNIQUE (purchase_order_id, destination_location_id);
ALTER TABLE receiving_requests ADD INDEX idx_receiving_request_org_destination_status (organization_id, destination_location_id, status);

-- `warehouses` is left in place for now because older deployments may have independent
-- references. Application code no longer uses it for zones, receiving, or inventory.
