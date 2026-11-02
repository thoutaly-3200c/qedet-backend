ALTER TABLE departments
    ADD COLUMN schedule_id BINARY(16) NULL;

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_schedule
        FOREIGN KEY (schedule_id) REFERENCES hr_work_schedules (id);
