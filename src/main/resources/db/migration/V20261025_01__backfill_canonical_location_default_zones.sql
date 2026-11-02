-- Non-destructive canonical-location repair. Legacy warehouses, zones and bins
-- are intentionally retained; application workflows use warehouse_locations.

-- A location may have stale duplicate defaults from pre-canonical deployments.
-- Keep the lowest UUID deterministically and deactivate only the duplicate
-- default flag; the zone rows and inventory remain intact.
UPDATE warehouse_locations duplicate_zone
JOIN (
    SELECT organization_location_id, MIN(id) AS kept_id
    FROM warehouse_locations
    WHERE type = 'ZONE' AND active = TRUE AND is_default = TRUE
    GROUP BY organization_location_id
    HAVING COUNT(*) > 1
) duplicates ON duplicates.organization_location_id = duplicate_zone.organization_location_id
SET duplicate_zone.is_default = FALSE
WHERE duplicate_zone.type = 'ZONE'
  AND duplicate_zone.active = TRUE
  AND duplicate_zone.is_default = TRUE
  AND duplicate_zone.id <> duplicates.kept_id;

-- Every active physical location receives one canonical receiving zone. The
-- UUID-derived code cannot collide with another location's zone code.
INSERT INTO warehouse_locations (
    id, organization_id, organization_location_id, parent_id, code, name,
    description, type, active, is_default, capacity, version, created_at, updated_at
)
SELECT UUID_TO_BIN(UUID()), location.organization_id, location.id, NULL,
       CONCAT('DEFAULT-', REPLACE(UUID(), '-', '')), 'Default',
       'Default receiving zone', 'ZONE', TRUE, TRUE, 0, 0, NOW(6), NOW(6)
FROM organization_locations location
WHERE location.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM warehouse_locations zone
      WHERE zone.organization_location_id = location.id
        AND zone.type = 'ZONE'
        AND zone.active = TRUE
        AND zone.is_default = TRUE
  );

-- At most one active default zone may exist at a physical location. NULL for
-- all other rows permits any number of ordinary zones and bins.
ALTER TABLE warehouse_locations
    ADD COLUMN default_active_zone_location_id BINARY(16)
        GENERATED ALWAYS AS (
            CASE WHEN type = 'ZONE' AND active = TRUE AND is_default = TRUE
                 THEN organization_location_id ELSE NULL END
        ) STORED,
    ADD UNIQUE INDEX uk_warehouse_location_active_default_zone (default_active_zone_location_id);

-- receiving_operation_zone_assignments.zone_id is already mapped by the
-- canonical migration to warehouse_locations(id); no legacy table is dropped.
