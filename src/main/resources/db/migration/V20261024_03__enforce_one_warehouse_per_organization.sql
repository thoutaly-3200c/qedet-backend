-- This unique index is only safe when every existing organization has zero or
-- one warehouse.  It deliberately fails if historical duplicate warehouses
-- exist so they can be reviewed instead of silently discarding inventory data.
ALTER TABLE warehouses
    ADD CONSTRAINT uk_warehouse_organization UNIQUE (organization_id);
