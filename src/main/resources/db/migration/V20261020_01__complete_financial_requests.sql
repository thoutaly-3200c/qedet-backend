ALTER TABLE finance_requests
    ADD COLUMN request_number VARCHAR(64) NULL,
    ADD COLUMN approved_amount DECIMAL(19,2) NULL,
    ADD COLUMN cancelled_by BINARY(16) NULL,
    ADD COLUMN cancelled_at TIMESTAMP NULL,
    ADD COLUMN cancellation_reason VARCHAR(2000) NULL;
UPDATE finance_requests SET fulfillment_status = 'UNPAID' WHERE fulfillment_status = 'NOT_PAID';
UPDATE finance_requests SET fulfillment_status = 'FULFILLED' WHERE fulfillment_status = 'PAID';
ALTER TABLE finance_operations
    ADD COLUMN evidence_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN evidence_verified_by BINARY(16) NULL,
    ADD COLUMN evidence_verified_at TIMESTAMP NULL,
    ADD COLUMN evidence_rejection_reason VARCHAR(2000) NULL;
CREATE TABLE finance_request_details (
    id BINARY(16) PRIMARY KEY, finance_request_id BINARY(16) NOT NULL,
    supplier_id BINARY(16), purchase_order_id BINARY(16), employee_id BINARY(16), payroll_id BINARY(16),
    department_id BINARY(16), cash_box_id BINARY(16), recipient_employee_id BINARY(16), original_operation_id BINARY(16),
    payee_name VARCHAR(255), purpose VARCHAR(2000), invoice_reference VARCHAR(255), expense_category VARCHAR(255),
    variance_reason VARCHAR(2000), expected_amount DECIMAL(19,2), counted_amount DECIMAL(19,2), settlement_date DATE,
    created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_finance_request_detail_request (finance_request_id),
    FOREIGN KEY (finance_request_id) REFERENCES finance_requests(id));
