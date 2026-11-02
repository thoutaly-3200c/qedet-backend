package q.qedet.product.mapper;

import q.qedet.product.dto.OrganizationProductResponse;
import q.qedet.product.dto.ProductBarcodeLookupResponse;
import q.qedet.product.dto.ProductResponse;
import q.qedet.product.entity.OrganizationProduct;
import q.qedet.product.entity.Product;
import q.qedet.product.entity.ProductBarcode;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toProductResponse(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand() == null ? null : product.getBrand().getId(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                product.getUnit() == null ? null : product.getUnit().getId(),
                product.getProductType(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public static OrganizationProductResponse toOrganizationProductResponse(OrganizationProduct organizationProduct) {
        if (organizationProduct == null) {
            return null;
        }
        Product product = organizationProduct.getProduct();
        return new OrganizationProductResponse(
                organizationProduct.getId(),
                organizationProduct.getOrganization() == null ? null : organizationProduct.getOrganization().getId(),
                product == null ? null : product.getId(),
                product == null ? null : product.getName(),
                organizationProduct.getOrganizationSku(),
                organizationProduct.getStatus(),
                organizationProduct.getCreatedAt(),
                organizationProduct.getUpdatedAt()
        );
    }

    public static ProductBarcodeLookupResponse toProductBarcodeLookupResponse(ProductBarcode barcode) {
        if (barcode == null) {
            return null;
        }
        Product product = barcode.getProduct();
        OrganizationProduct organizationProduct = barcode.getOrganizationProduct();
        Product selectedProduct = product;
        if (selectedProduct == null && organizationProduct != null) {
            selectedProduct = organizationProduct.getProduct();
        }
        return new ProductBarcodeLookupResponse(
                selectedProduct == null ? null : selectedProduct.getId(),
                selectedProduct == null ? null : selectedProduct.getName(),
                selectedProduct == null ? null : selectedProduct.getDescription(),
                selectedProduct == null || selectedProduct.getBrand() == null ? null : selectedProduct.getBrand().getId(),
                selectedProduct == null || selectedProduct.getCategory() == null ? null : selectedProduct.getCategory().getId(),
                selectedProduct == null || selectedProduct.getUnit() == null ? null : selectedProduct.getUnit().getId(),
                selectedProduct == null ? null : selectedProduct.getProductType(),
                selectedProduct == null ? null : selectedProduct.getStatus(),
                organizationProduct == null || organizationProduct.getOrganization() == null ? null : organizationProduct.getOrganization().getId(),
                organizationProduct == null ? null : organizationProduct.getId(),
                organizationProduct == null ? null : organizationProduct.getOrganizationSku(),
                organizationProduct == null ? null : organizationProduct.getStatus(),
                barcode.getBarcode(),
                barcode.getBarcodeType()
        );
    }
}
