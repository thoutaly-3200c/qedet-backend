CREATE TABLE hr_attendance_qrs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    organization_id BINARY(16) NOT NULL,
    department_id BINARY(16) NOT NULL,
    schedule_id BINARY(16) NOT NULL,
    location_id BINARY(16) NOT NULL,
    active BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_hr_attendance_qr_department UNIQUE (organization_id, department_id),
    CONSTRAINT fk_hr_attendance_qr_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_hr_attendance_qr_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_hr_attendance_qr_schedule FOREIGN KEY (schedule_id) REFERENCES hr_work_schedules(id),
    CONSTRAINT fk_hr_attendance_qr_location FOREIGN KEY (location_id) REFERENCES organization_locations(id)
);

CREATE TABLE hr_attendance_qr_tokens (
    id BINARY(16) NOT NULL PRIMARY KEY,
    attendance_qr_id BINARY(16) NOT NULL,
    work_date DATE NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    token_ciphertext VARCHAR(512) NOT NULL,
    valid_from DATETIME(6) NOT NULL,
    valid_until DATETIME(6) NOT NULL,
    status VARCHAR(16) NOT NULL,
    last_used_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_hr_attendance_qr_work_date UNIQUE (attendance_qr_id, work_date),
    CONSTRAINT fk_hr_attendance_qr_token FOREIGN KEY (attendance_qr_id) REFERENCES hr_attendance_qrs(id),
    INDEX idx_hr_attendance_qr_token_hash (token_hash)
);
