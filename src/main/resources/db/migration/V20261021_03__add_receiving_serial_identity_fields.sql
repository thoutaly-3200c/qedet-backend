ALTER TABLE receiving_operation_serials
    ADD COLUMN supplier_qr_identifier VARCHAR(255) NULL,
    ADD COLUMN accepted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE qr_codes
    MODIFY COLUMN code VARCHAR(255) NOT NULL,
    ADD CONSTRAINT uk_qr_codes_target_identity UNIQUE (organization_id, type, target_kind, reference_id);
