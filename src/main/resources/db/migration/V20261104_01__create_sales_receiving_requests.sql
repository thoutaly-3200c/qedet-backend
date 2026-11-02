ALTER TABLE sales_stock_requests MODIFY sales_hall_zone_id BINARY(16) NULL;

CREATE TABLE sales_receiving_requests (
    id BINARY(16) NOT NULL PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    sales_stock_request_id BINARY(16) NOT NULL,
    fulfillment_id BINARY(16) NOT NULL,
    sales_hall_id BINARY(16) NOT NULL,
    destination_zone_id BINARY(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    accepted_by BINARY(16) NULL,
    notes VARCHAR(1000) NULL,
    CONSTRAINT uk_sales_receiving_fulfillment UNIQUE (fulfillment_id),
    CONSTRAINT fk_sales_receiving_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_sales_receiving_request FOREIGN KEY (sales_stock_request_id) REFERENCES sales_stock_requests(id),
    CONSTRAINT fk_sales_receiving_fulfillment FOREIGN KEY (fulfillment_id) REFERENCES sales_stock_fulfillments(id),
    CONSTRAINT fk_sales_receiving_hall FOREIGN KEY (sales_hall_id) REFERENCES organization_sites(id),
    CONSTRAINT fk_sales_receiving_zone FOREIGN KEY (destination_zone_id) REFERENCES site_zones(id),
    INDEX idx_sales_receiving_org_status (organization_id, status)
);

CREATE TABLE sales_receiving_items (
    id BINARY(16) NOT NULL PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    receiving_request_id BINARY(16) NOT NULL,
    product_id BINARY(16) NOT NULL,
    dispatched_quantity DECIMAL(14,2) NOT NULL,
    actual_received_quantity DECIMAL(14,2) NULL,
    discrepancy_quantity DECIMAL(14,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_sales_receiving_item_request FOREIGN KEY (receiving_request_id) REFERENCES sales_receiving_requests(id),
    CONSTRAINT uk_sales_receiving_item_product UNIQUE (receiving_request_id, product_id)
);
