-- Run only against the intended development/test organization after replacing @org_id.
-- Do not run on production. It deletes no users, employees, products, purchase orders,
-- or organization_locations.
SET @org_id = UUID_TO_BIN('00000000-0000-0000-0000-000000000000');

DELETE sm FROM stock_movements sm WHERE sm.organization_id = @org_id;
DELETE ib FROM inventory_balances ib WHERE ib.organization_id = @org_id;
DELETE roza FROM receiving_operation_zone_assignments roza
  JOIN receiving_operation_items roi ON roi.id = roza.receiving_operation_item_id
  JOIN receiving_operations ro ON ro.id = roi.receiving_operation_id
 WHERE ro.organization_id = @org_id;

-- Delete zones/bins only when they belong to the test organization. Physical organization
-- locations are deliberately retained.
DELETE wl FROM warehouse_locations wl WHERE wl.organization_id = @org_id;
