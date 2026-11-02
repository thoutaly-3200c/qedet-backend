CREATE TABLE receiving_requests (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    request_number VARCHAR(64) NOT NULL,
    purchase_order_id BINARY(16) NOT NULL,
    warehouse_id BINARY(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    version BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_receiving_request_org_number UNIQUE (organization_id, request_number),
    CONSTRAINT uk_receiving_request_order_warehouse UNIQUE (purchase_order_id, warehouse_id),
    CONSTRAINT fk_receiving_request_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_receiving_request_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id),
    INDEX idx_receiving_request_org_warehouse_status (organization_id, warehouse_id, status),
    INDEX idx_receiving_request_purchase_order (purchase_order_id)
);

CREATE TABLE receiving_request_items (
    id BINARY(16) NOT NULL,
    receiving_request_id BINARY(16) NOT NULL,
    purchase_order_item_id BINARY(16) NOT NULL,
    expected_quantity DECIMAL(19,4) NOT NULL,
    version BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_receiving_request_item_po_item UNIQUE (receiving_request_id, purchase_order_item_id),
    CONSTRAINT fk_receiving_request_item_request FOREIGN KEY (receiving_request_id) REFERENCES receiving_requests (id),
    CONSTRAINT fk_receiving_request_item_po_item FOREIGN KEY (purchase_order_item_id) REFERENCES purchase_order_items (id),
    INDEX idx_receiving_request_item_po_item (purchase_order_item_id)
);

CREATE TABLE receiving_operations (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    receiving_request_id BINARY(16) NOT NULL,
    warehouse_employee_id BINARY(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    notes VARCHAR(4000) NULL,
    version BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_receiving_operation_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_receiving_operation_request FOREIGN KEY (receiving_request_id) REFERENCES receiving_requests (id),
    CONSTRAINT fk_receiving_operation_employee FOREIGN KEY (warehouse_employee_id) REFERENCES employees (id),
    INDEX idx_receiving_operation_request_status (receiving_request_id, status),
    INDEX idx_receiving_operation_org_employee (organization_id, warehouse_employee_id)
);

CREATE TABLE receiving_operation_items (
    id BINARY(16) NOT NULL,
    receiving_operation_id BINARY(16) NOT NULL,
    receiving_request_item_id BINARY(16) NOT NULL,
    received_quantity DECIMAL(19,4) NOT NULL,
    accepted_quantity DECIMAL(19,4) NOT NULL,
    rejected_quantity DECIMAL(19,4) NOT NULL,
    notes VARCHAR(4000) NULL,
    version BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_receiving_operation_request_item UNIQUE (receiving_operation_id, receiving_request_item_id),
    CONSTRAINT fk_receiving_operation_item_operation FOREIGN KEY (receiving_operation_id) REFERENCES receiving_operations (id),
    CONSTRAINT fk_receiving_operation_item_request_item FOREIGN KEY (receiving_request_item_id) REFERENCES receiving_request_items (id),
    INDEX idx_receiving_operation_item_request_item (receiving_request_item_id)
);

CREATE TABLE receiving_operation_serials (
    id BINARY(16) NOT NULL,
    receiving_operation_item_id BINARY(16) NOT NULL,
    product_id BINARY(16) NOT NULL,
    serial_number VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_receiving_serial_product_number UNIQUE (product_id, serial_number),
    CONSTRAINT fk_receiving_serial_operation_item FOREIGN KEY (receiving_operation_item_id) REFERENCES receiving_operation_items (id),
    CONSTRAINT fk_receiving_serial_product FOREIGN KEY (product_id) REFERENCES procurement_products (id)
);
