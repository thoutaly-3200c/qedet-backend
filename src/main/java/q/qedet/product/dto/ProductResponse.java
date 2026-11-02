package q.qedet.product.dto;

import q.qedet.product.entity.ProductStatus;
import q.qedet.product.entity.ProductType;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        UUID brandId,
        UUID categoryId,
        UUID unitId,
        ProductType productType,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
