-- Reset the retired organization_locations / warehouse_locations topology.
--
-- Scope: organizations which still own an organization_locations row.  Before any
-- legacy column is removed, every retained business record is repointed to a
-- canonical site or zone.  The procedure aborts if a reference cannot be moved;
-- it never disables foreign-key checks and it never deletes a business record.

DROP PROCEDURE IF EXISTS qedet_drop_foreign_keys_for_column;
DELIMITER //
CREATE PROCEDURE qedet_drop_foreign_keys_for_column(IN table_name_in VARCHAR(64), IN column_name_in VARCHAR(64))
BEGIN
    DECLARE done BOOLEAN DEFAULT FALSE;
    DECLARE fk_name VARCHAR(64);
    DECLARE fk_cursor CURSOR FOR
        SELECT DISTINCT k.constraint_name
        FROM information_schema.key_column_usage k
        WHERE k.constraint_schema = DATABASE()
          AND k.table_name = table_name_in
          AND k.column_name = column_name_in
          AND k.referenced_table_name IS NOT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN fk_cursor;
    fk_loop: LOOP
        FETCH fk_cursor INTO fk_name;
        IF done THEN LEAVE fk_loop; END IF;
        SET @drop_fk = CONCAT('ALTER TABLE `', table_name_in, '` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE drop_fk_statement FROM @drop_fk;
        EXECUTE drop_fk_statement;
        DEALLOCATE PREPARE drop_fk_statement;
    END LOOP;
    CLOSE fk_cursor;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS qedet_reset_disposable_legacy_locations;
DELIMITER //
CREATE PROCEDURE qedet_reset_disposable_legacy_locations()
BEGIN
START TRANSACTION;

-- Canonical topology for every organization that has disposable legacy locations.
INSERT INTO organization_sites (id, organization_id, code, name, type, status, is_default, version, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), legacy.organization_id, 'SALES_HALL', 'Sales Hall', 'SALES_HALL', 'ACTIVE', TRUE, 0, NOW(6), NOW(6)
FROM (SELECT DISTINCT organization_id FROM organization_locations) legacy
WHERE NOT EXISTS (
    SELECT 1 FROM organization_sites site
    WHERE site.organization_id = legacy.organization_id AND site.code = 'SALES_HALL'
);

INSERT INTO organization_sites (id, organization_id, code, name, type, status, is_default, version, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), legacy.organization_id, 'WAREHOUSE', 'Warehouse', 'WAREHOUSE', 'ACTIVE', FALSE, 0, NOW(6), NOW(6)
FROM (SELECT DISTINCT organization_id FROM organization_locations) legacy
WHERE NOT EXISTS (
    SELECT 1 FROM organization_sites site
    WHERE site.organization_id = legacy.organization_id AND site.code = 'WAREHOUSE'
);

INSERT INTO site_zones (id, organization_site_id, code, name, active, is_default, version, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), site.id, 'DEFAULT', 'Default', TRUE, TRUE, 0, NOW(6), NOW(6)
FROM organization_sites site
JOIN (SELECT DISTINCT organization_id FROM organization_locations) legacy
  ON legacy.organization_id = site.organization_id
WHERE NOT EXISTS (
    SELECT 1 FROM site_zones zone
    WHERE zone.organization_site_id = site.id AND zone.active = TRUE AND zone.is_default = TRUE
);

UPDATE site_zones duplicate_zone
JOIN (
    SELECT organization_site_id, MIN(id) AS kept_id
    FROM site_zones
    WHERE active = TRUE AND is_default = TRUE
    GROUP BY organization_site_id
    HAVING COUNT(*) > 1
) duplicates ON duplicates.organization_site_id = duplicate_zone.organization_site_id
SET duplicate_zone.is_default = FALSE
WHERE duplicate_zone.active = TRUE
  AND duplicate_zone.is_default = TRUE
  AND duplicate_zone.id <> duplicates.kept_id;

-- The warehouse records remain a supported aggregate.  Create the single row
-- required by its unique organization constraint and its one default staging zone.
INSERT INTO warehouses (id, organization_id, code, name, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), legacy.organization_id, 'WAREHOUSE', 'Warehouse', TRUE, NOW(6), NOW(6)
FROM (SELECT DISTINCT organization_id FROM organization_locations) legacy
WHERE NOT EXISTS (SELECT 1 FROM warehouses warehouse WHERE warehouse.organization_id = legacy.organization_id);

INSERT INTO warehouse_zones (id, organization_id, warehouse_id, code, name, active, is_default, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), warehouse.organization_id, warehouse.id, 'DEFAULT', 'Default staging zone', TRUE, TRUE, NOW(6), NOW(6)
FROM warehouses warehouse
JOIN (SELECT DISTINCT organization_id FROM organization_locations) legacy
  ON legacy.organization_id = warehouse.organization_id
WHERE NOT EXISTS (
    SELECT 1 FROM warehouse_zones zone
    WHERE zone.warehouse_id = warehouse.id AND zone.active = TRUE AND zone.is_default = TRUE
);

UPDATE warehouse_zones duplicate_zone
JOIN (
    SELECT warehouse_id, MIN(id) AS kept_id
    FROM warehouse_zones
    WHERE active = TRUE AND is_default = TRUE
    GROUP BY warehouse_id
    HAVING COUNT(*) > 1
) duplicates ON duplicates.warehouse_id = duplicate_zone.warehouse_id
SET duplicate_zone.is_default = FALSE
WHERE duplicate_zone.active = TRUE
  AND duplicate_zone.is_default = TRUE
  AND duplicate_zone.id <> duplicates.kept_id;

-- Add canonical columns before moving foreign keys.  Existing columns are kept
-- through the checks below so an interrupted migration remains recoverable.
ALTER TABLE receiving_requests ADD COLUMN destination_site_id BINARY(16) NULL;
ALTER TABLE receiving_operation_zone_assignments ADD COLUMN organization_zone_id BINARY(16) NULL;
ALTER TABLE inventory_balances ADD COLUMN organization_zone_id BINARY(16) NULL;
ALTER TABLE stock_movements ADD COLUMN from_organization_zone_id BINARY(16) NULL;
ALTER TABLE stock_movements ADD COLUMN to_organization_zone_id BINARY(16) NULL;

-- Site-level references retain their business rows and move to the default sales site.
UPDATE employees employee
JOIN organization_locations legacy ON legacy.id = employee.location_id
JOIN organization_sites site ON site.organization_id = legacy.organization_id AND site.code = 'SALES_HALL'
SET employee.site_id = site.id;

UPDATE positions position_row
JOIN organization_locations legacy ON legacy.id = COALESCE(position_row.default_location_id, position_row.location_id)
JOIN organization_sites site ON site.organization_id = legacy.organization_id AND site.code = 'SALES_HALL'
SET position_row.default_site_id = site.id;

UPDATE finance_cash_boxes cash_box
JOIN organization_locations legacy ON legacy.id = cash_box.location_id
JOIN organization_sites site ON site.organization_id = legacy.organization_id AND site.code = 'SALES_HALL'
SET cash_box.site_id = site.id;

UPDATE hr_attendance_events event_row
JOIN organization_locations legacy ON legacy.id = event_row.location_id
JOIN organization_sites site ON site.organization_id = legacy.organization_id AND site.code = 'SALES_HALL'
SET event_row.site_id = site.id;

UPDATE hr_attendance_qrs attendance_qr
JOIN organization_locations legacy ON legacy.id = attendance_qr.location_id
JOIN organization_sites site ON site.organization_id = legacy.organization_id AND site.code = 'SALES_HALL'
SET attendance_qr.site_id = site.id;

UPDATE hr_attendance_records attendance_record
JOIN organization_locations legacy ON legacy.id = attendance_record.location_id
JOIN organization_sites site ON site.organization_id = legacy.organization_id AND site.code = 'SALES_HALL'
SET attendance_record.site_id = site.id;

-- Receiving destinations and stock topology use the warehouse site's default zone.
UPDATE receiving_requests request_row
JOIN organization_locations legacy ON legacy.id = request_row.destination_location_id
JOIN organization_sites site ON site.organization_id = legacy.organization_id AND site.code = 'WAREHOUSE'
SET request_row.destination_site_id = site.id;

UPDATE receiving_operation_zone_assignments assignment_row
JOIN warehouse_locations legacy_zone ON legacy_zone.id = assignment_row.zone_id
JOIN organization_sites site ON site.organization_id = legacy_zone.organization_id AND site.code = 'WAREHOUSE'
JOIN site_zones zone ON zone.organization_site_id = site.id AND zone.active = TRUE AND zone.is_default = TRUE
SET assignment_row.organization_zone_id = zone.id;

UPDATE inventory_balances balance
JOIN warehouse_locations legacy_zone ON legacy_zone.id = balance.zone_id
JOIN organization_sites site ON site.organization_id = legacy_zone.organization_id AND site.code = 'WAREHOUSE'
JOIN site_zones zone ON zone.organization_site_id = site.id AND zone.active = TRUE AND zone.is_default = TRUE
SET balance.organization_zone_id = zone.id;

UPDATE stock_movements movement
JOIN warehouse_locations legacy_zone ON legacy_zone.id = movement.from_zone_id
JOIN organization_sites site ON site.organization_id = legacy_zone.organization_id AND site.code = 'WAREHOUSE'
JOIN site_zones zone ON zone.organization_site_id = site.id AND zone.active = TRUE AND zone.is_default = TRUE
SET movement.from_organization_zone_id = zone.id;

UPDATE stock_movements movement
JOIN warehouse_locations legacy_zone ON legacy_zone.id = movement.to_zone_id
JOIN organization_sites site ON site.organization_id = legacy_zone.organization_id AND site.code = 'WAREHOUSE'
JOIN site_zones zone ON zone.organization_site_id = site.id AND zone.active = TRUE AND zone.is_default = TRUE
SET movement.to_organization_zone_id = zone.id;

-- Safety gate: no business row is dropped.  A future database with a legacy
-- purchase-order delivery reference must be migrated explicitly before this reset.
IF EXISTS (SELECT 1 FROM purchase_order_items WHERE delivery_location_id IS NOT NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Legacy purchase-order delivery locations require explicit migration';
END IF;
IF EXISTS (SELECT 1 FROM receiving_requests WHERE destination_location_id IS NOT NULL AND destination_site_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A receiving destination could not be moved to a canonical site';
END IF;
IF EXISTS (SELECT 1 FROM receiving_operation_zone_assignments WHERE zone_id IS NOT NULL AND organization_zone_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A receiving zone assignment could not be moved to a canonical zone';
END IF;
IF EXISTS (SELECT 1 FROM inventory_balances WHERE zone_id IS NOT NULL AND organization_zone_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'An inventory balance could not be moved to a canonical zone';
END IF;
IF EXISTS (SELECT 1 FROM stock_movements WHERE from_zone_id IS NOT NULL AND from_organization_zone_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A stock movement source zone could not be moved to a canonical zone';
END IF;
IF EXISTS (SELECT 1 FROM stock_movements WHERE to_zone_id IS NOT NULL AND to_organization_zone_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A stock movement destination zone could not be moved to a canonical zone';
END IF;

-- Replace all foreign keys only after their values are valid.
ALTER TABLE receiving_requests ADD CONSTRAINT fk_receiving_request_destination_site FOREIGN KEY (destination_site_id) REFERENCES organization_sites(id);
ALTER TABLE receiving_operation_zone_assignments ADD CONSTRAINT fk_receiving_assignment_organization_zone FOREIGN KEY (organization_zone_id) REFERENCES site_zones(id);
ALTER TABLE inventory_balances ADD CONSTRAINT fk_inventory_balance_organization_zone FOREIGN KEY (organization_zone_id) REFERENCES site_zones(id);
ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movement_from_organization_zone FOREIGN KEY (from_organization_zone_id) REFERENCES site_zones(id);
ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movement_to_organization_zone FOREIGN KEY (to_organization_zone_id) REFERENCES site_zones(id);
ALTER TABLE receiving_operation_zone_assignments MODIFY organization_zone_id BINARY(16) NOT NULL;
ALTER TABLE inventory_balances MODIFY organization_zone_id BINARY(16) NOT NULL;

CALL qedet_drop_foreign_keys_for_column('employees', 'location_id');
CALL qedet_drop_foreign_keys_for_column('positions', 'location_id');
CALL qedet_drop_foreign_keys_for_column('positions', 'default_location_id');
CALL qedet_drop_foreign_keys_for_column('finance_cash_boxes', 'location_id');
CALL qedet_drop_foreign_keys_for_column('hr_attendance_events', 'location_id');
CALL qedet_drop_foreign_keys_for_column('hr_attendance_qrs', 'location_id');
CALL qedet_drop_foreign_keys_for_column('hr_attendance_records', 'location_id');
CALL qedet_drop_foreign_keys_for_column('purchase_order_items', 'delivery_location_id');
CALL qedet_drop_foreign_keys_for_column('receiving_requests', 'delivery_location_id');
CALL qedet_drop_foreign_keys_for_column('receiving_requests', 'destination_location_id');
CALL qedet_drop_foreign_keys_for_column('receiving_operation_zone_assignments', 'zone_id');
CALL qedet_drop_foreign_keys_for_column('receiving_operation_zone_assignments', 'legacy_warehouse_zone_id');
CALL qedet_drop_foreign_keys_for_column('inventory_balances', 'zone_id');
CALL qedet_drop_foreign_keys_for_column('stock_movements', 'from_zone_id');
CALL qedet_drop_foreign_keys_for_column('stock_movements', 'to_zone_id');

-- The old composite unique key also supports the item foreign key, so retain a
-- narrow supporting index while its zone half is replaced.
ALTER TABLE receiving_operation_zone_assignments ADD INDEX idx_receiving_assignment_item (receiving_operation_item_id);
ALTER TABLE receiving_operation_zone_assignments DROP INDEX uk_receiving_operation_item_zone;
ALTER TABLE receiving_operation_zone_assignments ADD UNIQUE INDEX uk_receiving_operation_item_zone (receiving_operation_item_id, organization_zone_id);

ALTER TABLE employees DROP COLUMN location_id;
ALTER TABLE positions DROP COLUMN location_id, DROP COLUMN default_location_id;
ALTER TABLE finance_cash_boxes DROP COLUMN location_id;
ALTER TABLE hr_attendance_events DROP COLUMN location_id;
ALTER TABLE hr_attendance_qrs DROP COLUMN location_id;
ALTER TABLE hr_attendance_records DROP COLUMN location_id;
ALTER TABLE purchase_order_items DROP COLUMN delivery_location_id;
ALTER TABLE receiving_requests DROP COLUMN delivery_location_id, DROP COLUMN destination_location_id;
ALTER TABLE receiving_operation_zone_assignments DROP COLUMN zone_id, DROP COLUMN legacy_warehouse_zone_id;
ALTER TABLE inventory_balances DROP COLUMN zone_id;
-- The former composite unique key may be the supporting index for the
-- organization foreign key in Hibernate-created schemas.
ALTER TABLE inventory_balances ADD INDEX idx_inventory_balance_organization (organization_id);
ALTER TABLE inventory_balances DROP INDEX idx_inventory_org_product_zone;
ALTER TABLE inventory_balances ADD UNIQUE INDEX idx_inventory_org_product_zone (organization_id, product_id, organization_zone_id);
ALTER TABLE stock_movements DROP COLUMN from_zone_id, DROP COLUMN to_zone_id;

-- warehouse_locations contains only the retired hierarchy.  Its dependent rows
-- have already been moved above, so this scoped delete removes only rows owned by
-- legacy organization locations before the retired table is removed.
DELETE legacy_zone
FROM warehouse_locations legacy_zone
JOIN organization_locations legacy_site ON legacy_site.id = legacy_zone.organization_location_id;

IF EXISTS (SELECT 1 FROM warehouse_locations) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected non-legacy warehouse location prevents table removal';
END IF;
DROP TABLE warehouse_locations;

CALL qedet_drop_foreign_keys_for_column('organization_locations', 'parent_location_id');
DELETE legacy_site
FROM organization_locations legacy_site
JOIN organization_sites site ON site.organization_id = legacy_site.organization_id
WHERE site.code IN ('SALES_HALL', 'WAREHOUSE');

IF EXISTS (SELECT 1 FROM organization_locations) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected organization location prevents table removal';
END IF;
DROP TABLE organization_locations;

COMMIT;
END //
DELIMITER ;

CALL qedet_reset_disposable_legacy_locations();
DROP PROCEDURE qedet_reset_disposable_legacy_locations;
DROP PROCEDURE qedet_drop_foreign_keys_for_column;
