-- Finalize the canonical Organization -> OrganizationSite -> OrganizationZone -> OrganizationBin model.
-- V20261027 copied physical sites, zones, and bins with their existing UUIDs, so all
-- foreign-key values remain valid while they are repointed below.

ALTER TABLE positions ADD COLUMN IF NOT EXISTS default_site_id BINARY(16) NULL;
UPDATE positions p JOIN organization_sites s ON s.id = p.default_location_id
SET p.default_site_id = s.id WHERE p.default_site_id IS NULL;
ALTER TABLE positions ADD CONSTRAINT fk_positions_default_site FOREIGN KEY (default_site_id) REFERENCES organization_sites(id);
CREATE INDEX idx_positions_default_site ON positions(default_site_id);

-- Site-owned references preserve the former IDs, now pointing to organization_sites.
ALTER TABLE employees ADD COLUMN IF NOT EXISTS site_id BINARY(16) NULL;
UPDATE employees e JOIN organization_sites s ON s.id = e.location_id SET e.site_id = s.id WHERE e.site_id IS NULL;
ALTER TABLE employees ADD CONSTRAINT fk_employees_site FOREIGN KEY (site_id) REFERENCES organization_sites(id);

ALTER TABLE finance_cash_boxes ADD COLUMN IF NOT EXISTS site_id BINARY(16) NULL;
UPDATE finance_cash_boxes c JOIN organization_sites s ON s.id = c.location_id SET c.site_id = s.id WHERE c.site_id IS NULL;
ALTER TABLE finance_cash_boxes ADD CONSTRAINT fk_finance_cash_box_site FOREIGN KEY (site_id) REFERENCES organization_sites(id);

-- Attendance records retain their historical physical-site identity.
ALTER TABLE attendance_qrs ADD COLUMN IF NOT EXISTS site_id BINARY(16) NULL;
UPDATE attendance_qrs q JOIN organization_sites s ON s.id = q.location_id SET q.site_id = s.id WHERE q.site_id IS NULL;
ALTER TABLE attendance_qrs ADD CONSTRAINT fk_attendance_qr_site FOREIGN KEY (site_id) REFERENCES organization_sites(id);

-- Drop legacy foreign keys and columns only in the deployment-specific follow-up
-- once every dependent table has been migrated. This migration intentionally does
-- not disable foreign-key checks or delete rows.
