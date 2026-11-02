package q.qedet.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import q.qedet.product.entity.ProductStatus;
import q.qedet.product.entity.ProductType;

import java.util.UUID;

public record ProductRequest(
        @NotBlank(message = "Product name is required") String name,
        String description,
        UUID brandId,
        UUID categoryId,
        UUID unitId,
        @NotNull(message = "Product type is required") ProductType productType,
        @NotNull(message = "Product status is required") ProductStatus status
) {
}
