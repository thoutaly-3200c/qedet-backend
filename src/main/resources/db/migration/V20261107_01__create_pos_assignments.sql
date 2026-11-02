CREATE TABLE sales_pos_assignments (
    id BINARY(16) NOT NULL PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    cashier_id BINARY(16) NOT NULL,
    sales_hall_id BINARY(16) NOT NULL,
    code VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_pos_assignment_cashier UNIQUE (organization_id, cashier_id),
    CONSTRAINT uk_pos_assignment_code UNIQUE (organization_id, code),
    CONSTRAINT fk_pos_assignment_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_pos_assignment_cashier FOREIGN KEY (cashier_id) REFERENCES employees(id),
    CONSTRAINT fk_pos_assignment_hall FOREIGN KEY (sales_hall_id) REFERENCES organization_sites(id)
);
