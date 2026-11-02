ALTER TABLE warehouse_return_items ADD COLUMN source_site_zone_id BINARY(16);
ALTER TABLE warehouse_return_items ADD COLUMN last_inspection_decision_key VARCHAR(512);
