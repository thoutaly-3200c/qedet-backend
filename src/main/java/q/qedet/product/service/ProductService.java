package q.qedet.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.product.dto.ProductBarcodeLookupResponse;
import q.qedet.product.dto.ProductRequest;
import q.qedet.product.dto.ProductResponse;
import q.qedet.product.entity.Product;
import q.qedet.product.entity.ProductBarcode;
import q.qedet.product.entity.ProductBarcodeType;
import q.qedet.product.entity.ProductBrand;
import q.qedet.product.entity.ProductCategory;
import q.qedet.product.entity.ProductStatus;
import q.qedet.product.entity.ProductType;
import q.qedet.product.entity.ProductUnit;
import q.qedet.product.mapper.ProductMapper;
import q.qedet.product.repository.ProductBarcodeRepository;
import q.qedet.product.repository.ProductBrandRepository;
import q.qedet.product.repository.ProductCategoryRepository;
import q.qedet.product.repository.ProductRepository;
import q.qedet.product.repository.ProductUnitRepository;

import java.util.List;
import java.util.UUID;

import static q.qedet.common.model.Permission.PRODUCT_MANAGE;
import static q.qedet.common.model.Permission.PRODUCT_VIEW;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductBrandRepository productBrandRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductUnitRepository productUnitRepository;
    private final ProductBarcodeRepository productBarcodeRepository;
    private final OrganizationRepository organizationRepository;
    private final ActorContext actorContext;

    public ProductService(ProductRepository productRepository,
                          ProductBrandRepository productBrandRepository,
                          ProductCategoryRepository productCategoryRepository,
                          ProductUnitRepository productUnitRepository,
                          ProductBarcodeRepository productBarcodeRepository,
                          OrganizationRepository organizationRepository,
                          ActorContext actorContext) {
        this.productRepository = productRepository;
        this.productBrandRepository = productBrandRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productUnitRepository = productUnitRepository;
        this.productBarcodeRepository = productBarcodeRepository;
        this.organizationRepository = organizationRepository;
        this.actorContext = actorContext;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        actorContext.requirePermission(PRODUCT_VIEW);
        return productRepository.findAll().stream()
                .map(ProductMapper::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        actorContext.requirePermission(PRODUCT_VIEW);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return ProductMapper.toProductResponse(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        actorContext.requirePermission(PRODUCT_MANAGE);
        Product product = new Product();
        applyProductData(product, request);
        Product saved = productRepository.save(product);
        return ProductMapper.toProductResponse(saved);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        actorContext.requirePermission(PRODUCT_MANAGE);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        applyProductData(product, request);
        Product saved = productRepository.save(product);
        return ProductMapper.toProductResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductBarcodeLookupResponse findProductByBarcode(String barcode) {
        actorContext.requirePermission(PRODUCT_VIEW);
        String normalized = BarcodeNormalizer.normalize(barcode);
        if (normalized.isBlank()) {
            throw new BadRequestException("Barcode is required");
        }
        ProductBarcode barcodeRecord = productBarcodeRepository.findByBarcode(normalized)
                .orElseThrow(() -> new NotFoundException("Product not found for barcode"));
        Product product = barcodeRecord.getProduct();
        if (product == null && barcodeRecord.getOrganizationProduct() != null) {
            product = barcodeRecord.getOrganizationProduct().getProduct();
        }
        if (product == null) {
            throw new NotFoundException("Product not found for barcode");
        }
        return ProductMapper.toProductBarcodeLookupResponse(barcodeRecord);
    }

    public Product findOrCreateGlobalProductForBarcode(String barcode, String name, String description,
                                                      UUID brandId, UUID categoryId, UUID unitId,
                                                      ProductType requestedType, ProductStatus requestedStatus,
                                                      ProductBarcodeType requestedBarcodeType) {
        String normalized = BarcodeNormalizer.normalize(barcode);
        if (normalized.isBlank()) {
            throw new BadRequestException("Barcode is required");
        }
        BarcodeNormalizer.validate(normalized, requestedBarcodeType == null ? ProductBarcodeType.OTHER : requestedBarcodeType);

        Product product = productBarcodeRepository.findByBarcode(normalized)
                .map(ProductBarcode::getProduct)
                .orElse(null);
        if (product != null) {
            return product;
        }

        ProductRequest request = new ProductRequest(
                name == null || name.isBlank() ? "Unknown Product" : name,
                description,
                brandId,
                categoryId,
                unitId,
                requestedType == null ? ProductType.STANDARD : requestedType,
                requestedStatus == null ? ProductStatus.ACTIVE : requestedStatus
        );
        Product newProduct = new Product();
        applyProductData(newProduct, request);
        Product saved = productRepository.save(newProduct);

        ProductBarcode newBarcode = new ProductBarcode();
        newBarcode.setProduct(saved);
        newBarcode.setBarcode(normalized);
        newBarcode.setBarcodeType(requestedBarcodeType == null ? ProductBarcodeType.OTHER : requestedBarcodeType);
        newBarcode.setPrimary(true);
        productBarcodeRepository.save(newBarcode);
        return saved;
    }

    private void applyProductData(Product product, ProductRequest request) {
        if (request == null) {
            throw new BadRequestException("Product request is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("Product name is required");
        }
        if (request.productType() == null) {
            throw new BadRequestException("Product type is required");
        }
        if (request.status() == null) {
            throw new BadRequestException("Product status is required");
        }

        product.setName(request.name().trim());
        product.setDescription(request.description() == null ? null : request.description().trim());
        product.setBrand(resolveBrand(request.brandId()));
        product.setCategory(resolveCategory(request.categoryId()));
        product.setUnit(resolveUnit(request.unitId()));
        product.setProductType(request.productType());
        product.setStatus(request.status());
    }

    private ProductBrand resolveBrand(UUID brandId) {
        if (brandId == null) {
            return null;
        }
        return productBrandRepository.findById(brandId)
                .orElseThrow(() -> new NotFoundException("Brand not found"));
    }

    private ProductCategory resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private ProductUnit resolveUnit(UUID unitId) {
        if (unitId == null) {
            return null;
        }
        return productUnitRepository.findById(unitId)
                .orElseThrow(() -> new NotFoundException("Unit not found"));
    }

    public Organization findOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }
}
