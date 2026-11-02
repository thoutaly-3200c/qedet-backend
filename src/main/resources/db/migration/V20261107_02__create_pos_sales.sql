ALTER TABLE organization_configurations
    ADD COLUMN sales_tax_rate DECIMAL(7,4) NOT NULL DEFAULT 0.0000,
    ADD COLUMN sales_prices_tax_inclusive BOOLEAN NOT NULL DEFAULT FALSE;

-- Align unused employee cashboxes with the organization's configured currency.
-- Boxes with transactions retain their ledger currency and require manual reconciliation.
UPDATE finance_cash_boxes box
JOIN organization_configurations config ON config.organization_id = box.organization_id
SET box.currency = config.currency
WHERE box.cash_box_type = 'EMPLOYEE'
  AND config.currency IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM finance_cash_transactions tx WHERE tx.cash_box_id = box.id);

CREATE TABLE sales_pos_quotes (
    id BINARY(16) PRIMARY KEY,
    organization_id BINARY(16) NOT NULL,
    cashier_id BINARY(16) NOT NULL,
    sales_hall_id BINARY(16) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    tax_rate DECIMAL(7,4) NOT NULL,
    prices_tax_inclusive BOOLEAN NOT NULL,
    subtotal DECIMAL(19,2) NOT NULL,
    discount_total DECIMAL(19,2) NOT NULL,
    tax_total DECIMAL(19,2) NOT NULL,
    total DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_pos_quote_token_hash (token_hash),
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (cashier_id) REFERENCES employees(id),
    FOREIGN KEY (sales_hall_id) REFERENCES organization_sites(id)
);

CREATE TABLE sales_pos_quote_lines (
    id BINARY(16) PRIMARY KEY,
    quote_id BINARY(16) NOT NULL,
    sort_order INT NOT NULL,
    product_id BINARY(16) NOT NULL,
    package_id BINARY(16) NULL,
    serial_number VARCHAR(160) NULL,
    product_name VARCHAR(200) NOT NULL,
    sku VARCHAR(100) NULL,
    quantity DECIMAL(14,2) NOT NULL,
    unit_price DECIMAL(19,2) NOT NULL,
    line_total DECIMAL(19,2) NOT NULL,
    tax_amount DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    FOREIGN KEY (quote_id) REFERENCES sales_pos_quotes(id)
);

CREATE TABLE sales_pos_sales (
    id BINARY(16) PRIMARY KEY,
    organization_id BINARY(16) NOT NULL,
    cashier_id BINARY(16) NOT NULL,
    sales_hall_id BINARY(16) NOT NULL,
    quote_id BINARY(16) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    transaction_code VARCHAR(64) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    subtotal DECIMAL(19,2) NOT NULL,
    discount_total DECIMAL(19,2) NOT NULL,
    tax_total DECIMAL(19,2) NOT NULL,
    total DECIMAL(19,2) NOT NULL,
    completed_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_pos_sale_idempotency (organization_id, cashier_id, idempotency_key),
    UNIQUE KEY uk_pos_sale_transaction_code (organization_id, transaction_code),
    UNIQUE KEY uk_pos_sale_quote (quote_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (cashier_id) REFERENCES employees(id),
    FOREIGN KEY (sales_hall_id) REFERENCES organization_sites(id),
    FOREIGN KEY (quote_id) REFERENCES sales_pos_quotes(id)
);

CREATE TABLE sales_pos_sale_lines (
    id BINARY(16) PRIMARY KEY,
    sale_id BINARY(16) NOT NULL,
    product_id BINARY(16) NOT NULL,
    package_id BINARY(16) NULL,
    serial_number VARCHAR(160) NULL,
    product_name VARCHAR(200) NOT NULL,
    sku VARCHAR(100) NULL,
    quantity DECIMAL(14,2) NOT NULL,
    unit_price DECIMAL(19,2) NOT NULL,
    line_total DECIMAL(19,2) NOT NULL,
    tax_amount DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales_pos_sales(id)
);

CREATE TABLE sales_pos_sale_payments (
    id BINARY(16) PRIMARY KEY,
    sale_id BINARY(16) NOT NULL,
    cash_box_id BINARY(16) NOT NULL,
    method VARCHAR(16) NOT NULL,
    amount_received DECIMAL(19,2) NOT NULL,
    change_amount DECIMAL(19,2) NOT NULL,
    reference VARCHAR(160) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_pos_payment_sale (sale_id),
    FOREIGN KEY (sale_id) REFERENCES sales_pos_sales(id),
    FOREIGN KEY (cash_box_id) REFERENCES finance_cash_boxes(id)
);
