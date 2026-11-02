package q.qedet.product.dto;

import q.qedet.product.entity.OrganizationProductStatus;

import java.time.Instant;
import java.util.UUID;

public record OrganizationProductResponse(
        UUID id,
        UUID organizationId,
        UUID productId,
        String productName,
        String organizationSku,
        OrganizationProductStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
