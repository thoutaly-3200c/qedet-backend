package q.qedet.qr.dto;

import java.util.UUID;

/**
 * Product identity exposed for an authorized product QR scan.
 */
public record QrProductResponse(
        UUID productId,
        String sku,
        String name,
        String barcode,
        String unit
) {}
