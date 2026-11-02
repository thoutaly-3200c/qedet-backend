ALTER TABLE finance_requests
    ADD COLUMN fulfillment_status VARCHAR(20) NOT NULL DEFAULT 'NOT_PAID';
