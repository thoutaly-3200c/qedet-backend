package q.qedet.product.dto;

import q.qedet.product.entity.OrganizationProductStatus;
import q.qedet.product.entity.ProductBarcodeType;
import q.qedet.product.entity.ProductStatus;
import q.qedet.product.entity.ProductType;

import java.util.UUID;

public record ProductBarcodeLookupResponse(
        UUID productId,
        String productName,
        String description,
        UUID brandId,
        UUID categoryId,
        UUID unitId,
        ProductType productType,
        ProductStatus status,
        UUID organizationId,
        UUID organizationProductId,
        String organizationSku,
        OrganizationProductStatus organizationProductStatus,
        String barcode,
        ProductBarcodeType barcodeType
) {
}
