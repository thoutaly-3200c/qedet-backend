-- Physical topology replacement. This migration is additive until all foreign-key
-- consumers have been moved, so a failed deployment never discards stock history.
-- It preserves UUIDs from organization_locations / warehouse_locations whenever a
-- one-to-one mapping exists.

CREATE TABLE IF NOT EXISTS organization_sites (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(30) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    is_default BIT NOT NULL DEFAULT FALSE,
    address_line1 VARCHAR(240) NULL, address_line2 VARCHAR(240) NULL,
    city VARCHAR(100) NULL, state VARCHAR(100) NULL, postal_code VARCHAR(30) NULL,
    country VARCHAR(100) NULL, phone VARCHAR(40) NULL, email VARCHAR(254) NULL,
    version BIGINT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_organization_site_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT uk_organization_site_org_code UNIQUE (organization_id, code),
    INDEX idx_organization_site_org_status (organization_id, status)
);

-- organization_locations has an exact physical-site interpretation in the old model.
INSERT IGNORE INTO organization_sites
    (id, organization_id, code, name, type, description, status, is_default,
     address_line1, address_line2, city, state, postal_code, country, phone, email,
     version, created_at, updated_at)
SELECT id, organization_id, code, name,
       CASE WHEN type = 'RECEIVING_AREA' THEN 'WAREHOUSE' ELSE type END,
       description, status, is_default, address_line1, address_line2, city, state,
       postal_code, country, phone, email, 0, created_at, updated_at
FROM organization_locations;

CREATE TABLE IF NOT EXISTS site_zones (
    id BINARY(16) NOT NULL,
    organization_site_id BINARY(16) NOT NULL,
    code VARCHAR(64) NOT NULL, name VARCHAR(160) NOT NULL, description VARCHAR(500) NULL,
    active BIT NOT NULL, is_default BIT NOT NULL DEFAULT FALSE,
    capacity DECIMAL(14,2) NULL, version BIGINT NULL,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_site_zone_site FOREIGN KEY (organization_site_id) REFERENCES organization_sites(id),
    CONSTRAINT uk_site_zone_code UNIQUE (organization_site_id, code),
    INDEX idx_site_zone_site_active (organization_site_id, active)
);

-- A ZONE row is unambiguous only when its parent physical location was copied.
INSERT IGNORE INTO site_zones
    (id, organization_site_id, code, name, description, active, is_default, capacity, version, created_at, updated_at)
SELECT wl.id, wl.organization_location_id, wl.code, wl.name, wl.description, wl.active,
       wl.is_default, wl.capacity, wl.version, wl.created_at, wl.updated_at
FROM warehouse_locations wl
JOIN organization_sites s ON s.id = wl.organization_location_id
WHERE wl.type = 'ZONE';

CREATE TABLE IF NOT EXISTS site_bins (
    id BINARY(16) NOT NULL,
    zone_id BINARY(16) NOT NULL,
    code VARCHAR(64) NOT NULL, name VARCHAR(160) NOT NULL, description VARCHAR(500) NULL,
    active BIT NOT NULL, capacity DECIMAL(14,2) NULL, version BIGINT NULL,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_site_bin_zone FOREIGN KEY (zone_id) REFERENCES site_zones(id),
    CONSTRAINT uk_site_bin_code UNIQUE (zone_id, code),
    INDEX idx_site_bin_zone_active (zone_id, active)
);

INSERT IGNORE INTO site_bins
    (id, zone_id, code, name, description, active, capacity, version, created_at, updated_at)
SELECT wl.id, wl.parent_id, wl.code, wl.name, wl.description, wl.active, wl.capacity,
       wl.version, wl.created_at, wl.updated_at
FROM warehouse_locations wl
JOIN site_zones z ON z.id = wl.parent_id
WHERE wl.type = 'BIN';

-- A normalized destination has exactly one concrete target. The generated key
-- prevents duplicate product/destination balances once consumers use this table.
CREATE TABLE IF NOT EXISTS inventory_destinations (
    id BINARY(16) NOT NULL,
    zone_id BINARY(16) NULL,
    bin_id BINARY(16) NULL,
    destination_kind VARCHAR(8) GENERATED ALWAYS AS
        (CASE WHEN zone_id IS NOT NULL THEN 'ZONE' ELSE 'BIN' END) STORED,
    destination_key BINARY(16) GENERATED ALWAYS AS
        (COALESCE(zone_id, bin_id)) STORED,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_inventory_destination_target CHECK ((zone_id IS NULL) <> (bin_id IS NULL)),
    CONSTRAINT fk_inventory_destination_zone FOREIGN KEY (zone_id) REFERENCES site_zones(id),
    CONSTRAINT fk_inventory_destination_bin FOREIGN KEY (bin_id) REFERENCES site_bins(id),
    UNIQUE INDEX uk_inventory_destination_target (destination_kind, destination_key)
);

-- These result sets are release preflight evidence. They must be empty before
-- the follow-up migration can repoint and drop legacy foreign keys/tables.
SELECT wl.id AS unresolved_legacy_zone
FROM warehouse_locations wl
LEFT JOIN organization_sites s ON s.id = wl.organization_location_id
WHERE wl.type = 'ZONE' AND s.id IS NULL;
SELECT wl.id AS unresolved_legacy_bin
FROM warehouse_locations wl
LEFT JOIN site_zones z ON z.id = wl.parent_id
WHERE wl.type = 'BIN' AND z.id IS NULL;

-- MySQL has no portable assertion for “exactly one active default row”. A
-- generated unique key enforces *at most* one; the transactional provisioning
-- service creates a default before an active site is exposed.
ALTER TABLE site_zones
    ADD COLUMN default_active_site_id BINARY(16) GENERATED ALWAYS AS
        (CASE WHEN active = TRUE AND is_default = TRUE THEN organization_site_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_site_zone_active_default (default_active_site_id);
