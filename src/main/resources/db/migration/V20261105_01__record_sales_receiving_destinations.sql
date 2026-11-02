ALTER TABLE sales_receiving_items
    ADD COLUMN accepted_zone_id BINARY(16) NULL,
    ADD COLUMN rejected_zone_id BINARY(16) NULL,
    ADD CONSTRAINT fk_sales_receiving_accepted_zone FOREIGN KEY (accepted_zone_id) REFERENCES site_zones(id),
    ADD CONSTRAINT fk_sales_receiving_rejected_zone FOREIGN KEY (rejected_zone_id) REFERENCES site_zones(id);
