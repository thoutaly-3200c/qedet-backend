CREATE TABLE supplier_return_operations (
 id BINARY(16) NOT NULL PRIMARY KEY, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 organization_id BINARY(16) NOT NULL, reference VARCHAR(64) NOT NULL, warehouse_return_id BINARY(16) NOT NULL,
 supplier_id BINARY(16) NOT NULL, status VARCHAR(32) NOT NULL, settlement_type VARCHAR(24), settlement_reference VARCHAR(160), settlement_notes VARCHAR(2000), financial_request_id BINARY(16),
 approved_at DATETIME(6), dispatched_at DATETIME(6), supplier_responded_at DATETIME(6), settlement_requested_at DATETIME(6), closed_at DATETIME(6), notes VARCHAR(2000), version BIGINT,
 CONSTRAINT uk_supplier_return_reference UNIQUE (organization_id,reference),
 CONSTRAINT fk_supplier_return_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
 CONSTRAINT fk_supplier_return_request FOREIGN KEY (warehouse_return_id) REFERENCES warehouse_returns(id),
 CONSTRAINT fk_supplier_return_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);
CREATE TABLE supplier_return_operation_items (
 id BINARY(16) NOT NULL PRIMARY KEY, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 operation_id BINARY(16) NOT NULL, warehouse_return_item_id BINARY(16) NOT NULL,
 purchase_order_item_id BINARY(16) NOT NULL, receiving_operation_item_id BINARY(16) NOT NULL,
 approved_quantity DECIMAL(19,4) NOT NULL, dispatched_quantity DECIMAL(19,4) NOT NULL,
 supplier_accepted_quantity DECIMAL(19,4) NOT NULL, supplier_rejected_quantity DECIMAL(19,4) NOT NULL,
 unit_cost DECIMAL(19,4) NOT NULL, currency VARCHAR(3) NOT NULL,
 CONSTRAINT uk_supplier_return_request_item UNIQUE (warehouse_return_item_id),
 CONSTRAINT fk_supplier_return_item_operation FOREIGN KEY (operation_id) REFERENCES supplier_return_operations(id),
 CONSTRAINT fk_supplier_return_item_request FOREIGN KEY (warehouse_return_item_id) REFERENCES warehouse_return_items(id),
 CONSTRAINT fk_supplier_return_item_po FOREIGN KEY (purchase_order_item_id) REFERENCES purchase_order_items(id),
 CONSTRAINT fk_supplier_return_item_receiving FOREIGN KEY (receiving_operation_item_id) REFERENCES receiving_operation_items(id)
);
CREATE TABLE supplier_return_dispatches (
 id BINARY(16) NOT NULL PRIMARY KEY, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 operation_id BINARY(16) NOT NULL, reference VARCHAR(80) NOT NULL, tracking_reference VARCHAR(160), employee_id BINARY(16) NOT NULL, dispatched_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_supplier_return_dispatch_ref UNIQUE (operation_id,reference),
 CONSTRAINT fk_supplier_return_dispatch_operation FOREIGN KEY (operation_id) REFERENCES supplier_return_operations(id)
);
CREATE TABLE supplier_return_dispatch_items (
 id BINARY(16) NOT NULL PRIMARY KEY, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 dispatch_id BINARY(16) NOT NULL, operation_item_id BINARY(16) NOT NULL, source_zone_id BINARY(16) NOT NULL, quantity DECIMAL(19,4) NOT NULL,
 CONSTRAINT fk_supplier_return_dispatch_item_dispatch FOREIGN KEY (dispatch_id) REFERENCES supplier_return_dispatches(id),
 CONSTRAINT fk_supplier_return_dispatch_item_operation_item FOREIGN KEY (operation_item_id) REFERENCES supplier_return_operation_items(id),
 CONSTRAINT fk_supplier_return_dispatch_item_zone FOREIGN KEY (source_zone_id) REFERENCES site_zones(id)
);
