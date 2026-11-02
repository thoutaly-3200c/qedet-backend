ALTER TABLE hr_department_schedule_assignments
    ADD COLUMN location_id BINARY(16) NULL;

UPDATE hr_department_schedule_assignments assignment_row
JOIN organization_locations location_row
    ON location_row.organization_id = assignment_row.organization_id
   AND location_row.is_default = TRUE
SET assignment_row.location_id = location_row.id
WHERE assignment_row.location_id IS NULL;

ALTER TABLE hr_department_schedule_assignments
    ADD CONSTRAINT fk_hr_department_schedule_assignment_location
        FOREIGN KEY (location_id) REFERENCES organization_locations(id);
