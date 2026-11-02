ALTER TABLE finance_cash_boxes
    ADD COLUMN cash_box_type VARCHAR(16) NOT NULL DEFAULT 'LEGACY',
    ADD COLUMN owner_employee_id BINARY(16) NULL,
    ADD COLUMN provisioning_key VARCHAR(48) NULL;

UPDATE finance_cash_boxes
SET provisioning_key = CONCAT('LEGACY-', HEX(id))
WHERE provisioning_key IS NULL;

ALTER TABLE finance_cash_boxes
    MODIFY COLUMN provisioning_key VARCHAR(48) NOT NULL,
    ADD CONSTRAINT uk_finance_cash_box_org_provisioning UNIQUE (organization_id, provisioning_key),
    ADD CONSTRAINT uk_finance_cash_box_org_owner UNIQUE (organization_id, owner_employee_id),
    ADD CONSTRAINT fk_finance_cash_box_owner_employee
        FOREIGN KEY (owner_employee_id) REFERENCES employees(id);

INSERT INTO finance_cash_boxes (
    id, organization_id, name, currency, status, cash_box_type, provisioning_key, created_at, updated_at
)
SELECT UUID_TO_BIN(UUID()), organization.id, CONCAT('Main Cashbox ', HEX(organization.id)), 'USD', 'ACTIVE',
       'MAIN', 'MAIN', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM organizations organization
WHERE NOT EXISTS (
    SELECT 1
    FROM finance_cash_boxes cash_box
    WHERE cash_box.organization_id = organization.id
      AND cash_box.provisioning_key = 'MAIN'
);

INSERT INTO finance_cash_boxes (
    id, organization_id, owner_employee_id, name, currency, status, cash_box_type, provisioning_key, created_at, updated_at
)
SELECT UUID_TO_BIN(UUID()), employee.organization_id, employee.id,
       CONCAT('Cashbox ', HEX(employee.id)), 'USD', 'ACTIVE', 'EMPLOYEE',
       CONCAT('EMPLOYEE-', HEX(employee.id)), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM employees employee
WHERE employee.status = 'ACTIVE'
  AND employee.role IN ('FINANCE_MANAGER', 'FINANCE_EMPLOYEE')
  AND NOT EXISTS (
    SELECT 1
    FROM finance_cash_boxes cash_box
    WHERE cash_box.organization_id = employee.organization_id
      AND cash_box.provisioning_key = CONCAT('EMPLOYEE-', HEX(employee.id))
);
