-- A receiving item can target each canonical site zone once.  This makes a
-- repeated request deterministic and prevents duplicate posting destinations.
ALTER TABLE receiving_operation_zone_assignments
    ADD CONSTRAINT uk_receiving_operation_item_zone
    UNIQUE (receiving_operation_item_id, organization_zone_id);
