CREATE TABLE warehouses (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500) NULL,
    active BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_warehouse_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_warehouse_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    INDEX idx_warehouse_org_active (organization_id, active)
);

-- Legacy warehouse location IDs are retained as warehouse IDs so existing
-- receiving requests continue to resolve without changing unrelated locations.
INSERT INTO warehouses (id, organization_id, code, name, description, active, created_at, updated_at)
SELECT id, organization_id, code, name, description, status = 'ACTIVE', created_at, updated_at
FROM organization_locations
WHERE type IN ('WAREHOUSE', 'DISTRIBUTION_CENTER');

INSERT INTO warehouses (id, organization_id, code, name, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), z.organization_id, CONCAT('LEGACY-', HEX(MIN(z.id))), 'Migrated warehouse',
       'Created while migrating organization-scoped warehouse zones', TRUE, NOW(6), NOW(6)
FROM warehouse_zones z
LEFT JOIN warehouses w ON w.organization_id = z.organization_id
WHERE w.id IS NULL
GROUP BY z.organization_id;

ALTER TABLE warehouse_zones
    ADD COLUMN warehouse_id BINARY(16) NULL,
    ADD COLUMN is_default BIT NOT NULL DEFAULT FALSE;

-- Legacy zones and bins were organization-scoped. They are attached to that
-- organization's first migrated warehouse; deployments with multiple legacy
-- warehouses must review this deterministic backfill before applying it.
UPDATE warehouse_zones z
JOIN (
    SELECT organization_id, MIN(id) AS warehouse_id
    FROM warehouses
    GROUP BY organization_id
) w ON w.organization_id = z.organization_id
SET z.warehouse_id = w.warehouse_id;

UPDATE warehouse_zones z
JOIN (
    SELECT warehouse_id, MIN(id) AS zone_id
    FROM warehouse_zones
    GROUP BY warehouse_id
) first_zone ON first_zone.zone_id = z.id
SET z.is_default = TRUE, z.active = TRUE;

INSERT INTO warehouse_zones (id, organization_id, warehouse_id, code, name, description, active, is_default, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), w.organization_id, w.id, 'DEFAULT', 'Default', 'Default receiving zone', TRUE, TRUE, NOW(6), NOW(6)
FROM warehouses w
LEFT JOIN warehouse_zones z ON z.warehouse_id = w.id
WHERE z.id IS NULL;

ALTER TABLE warehouse_zones
    MODIFY COLUMN warehouse_id BINARY(16) NOT NULL,
    DROP INDEX idx_warehouse_zone_org_code,
    ADD CONSTRAINT fk_warehouse_zone_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    ADD INDEX idx_warehouse_zone_org_warehouse (organization_id, warehouse_id),
    ADD UNIQUE INDEX idx_warehouse_zone_warehouse_code (warehouse_id, code);

UPDATE warehouse_bins b
JOIN warehouse_zones z ON z.organization_id = b.organization_id AND z.is_default = TRUE
SET b.zone_id = z.id
WHERE b.zone_id IS NULL;

ALTER TABLE warehouse_bins
    MODIFY COLUMN zone_id BINARY(16) NOT NULL,
    DROP INDEX idx_warehouse_bin_org_code,
    ADD CONSTRAINT fk_warehouse_bin_zone FOREIGN KEY (zone_id) REFERENCES warehouse_zones (id),
    ADD UNIQUE INDEX idx_warehouse_bin_zone_code (zone_id, code);

ALTER TABLE receiving_operations
    ADD COLUMN inventory_applied BIT NOT NULL DEFAULT FALSE;

ALTER TABLE receiving_requests
    ADD CONSTRAINT fk_receiving_request_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id);

-- This unused organization-wide predecessor is retained as an archival table;
-- warehouse_zones and warehouse_bins are now the sole active location model.
RENAME TABLE warehouse_locations TO warehouse_locations_legacy;

CREATE TABLE receiving_operation_zone_assignments (
    id BINARY(16) NOT NULL,
    receiving_operation_item_id BINARY(16) NOT NULL,
    zone_id BINARY(16) NOT NULL,
    accepted_quantity DECIMAL(19,4) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_receiving_operation_item_zone UNIQUE (receiving_operation_item_id, zone_id),
    CONSTRAINT fk_receiving_zone_assignment_item FOREIGN KEY (receiving_operation_item_id)
        REFERENCES receiving_operation_items (id),
    CONSTRAINT fk_receiving_zone_assignment_zone FOREIGN KEY (zone_id) REFERENCES warehouse_zones (id),
    INDEX idx_receiving_zone_assignment_zone (zone_id)
);
