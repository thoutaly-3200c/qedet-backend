-- A receiving request is created when its PO item becomes ready. The physical
-- destination is selected by the receiving workflow when work actually starts.
ALTER TABLE receiving_requests
    MODIFY COLUMN warehouse_id BINARY(16) NULL;
