package q.qedet.product.dto;

import q.qedet.product.entity.OrganizationProductStatus;
import q.qedet.product.entity.ProductBarcodeType;
import q.qedet.product.entity.ProductStatus;
import q.qedet.product.entity.ProductType;

import java.util.UUID;

public record OrganizationProductRequest(
        UUID productId,
        String organizationSku,
        String barcode,
        String name,
        String description,
        UUID brandId,
        UUID categoryId,
        UUID unitId,
        ProductType productType,
        ProductStatus status,
        ProductBarcodeType barcodeType,
        OrganizationProductStatus organizationProductStatus
) {
}
