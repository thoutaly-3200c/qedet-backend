UPDATE hr_department_schedule_assignments
SET location_id = NULL
WHERE location_id = UNHEX('00000000000000000000000000000000');

ALTER TABLE hr_department_schedule_assignments
    DROP FOREIGN KEY fk_hr_department_schedule_assignment_location;

ALTER TABLE hr_department_schedule_assignments
    DROP COLUMN location_id;
