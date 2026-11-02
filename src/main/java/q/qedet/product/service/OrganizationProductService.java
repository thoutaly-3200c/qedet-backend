package q.qedet.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.ForbiddenException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.product.dto.OrganizationProductRequest;
import q.qedet.product.dto.OrganizationProductResponse;
import q.qedet.product.entity.OrganizationProduct;
import q.qedet.product.entity.OrganizationProductStatus;
import q.qedet.product.entity.Product;
import q.qedet.product.entity.ProductBarcode;
import q.qedet.product.entity.ProductBarcodeType;
import q.qedet.product.entity.ProductStatus;
import q.qedet.product.entity.ProductType;
import q.qedet.product.mapper.ProductMapper;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.product.repository.ProductBarcodeRepository;
import q.qedet.product.repository.ProductBrandRepository;
import q.qedet.product.repository.ProductCategoryRepository;
import q.qedet.product.repository.ProductRepository;
import q.qedet.product.repository.ProductUnitRepository;

import java.util.List;
import java.util.UUID;

import static q.qedet.common.model.Permission.PRODUCT_MANAGE;
import static q.qedet.common.model.Permission.PRODUCT_VIEW;
import static q.qedet.common.model.Permission.GOODS_RECEIVE;

@Service
public class OrganizationProductService {

    private final OrganizationProductRepository organizationProductRepository;
    private final OrganizationRepository organizationRepository;
    private final ProductRepository productRepository;
    private final ProductBarcodeRepository productBarcodeRepository;
    private final ProductBrandRepository productBrandRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductUnitRepository productUnitRepository;
    private final ActorContext actorContext;

    public OrganizationProductService(OrganizationProductRepository organizationProductRepository,
                                     OrganizationRepository organizationRepository,
                                     ProductRepository productRepository,
                                     ProductBarcodeRepository productBarcodeRepository,
                                     ProductBrandRepository productBrandRepository,
                                     ProductCategoryRepository productCategoryRepository,
                                     ProductUnitRepository productUnitRepository,
                                     ActorContext actorContext) {
        this.organizationProductRepository = organizationProductRepository;
        this.organizationRepository = organizationRepository;
        this.productRepository = productRepository;
        this.productBarcodeRepository = productBarcodeRepository;
        this.productBrandRepository = productBrandRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productUnitRepository = productUnitRepository;
        this.actorContext = actorContext;
    }

    @Transactional(readOnly = true)
    public List<OrganizationProductResponse> list(UUID organizationId) {
        UUID effectiveOrganizationId = ensureOrganizationAccess(organizationId);
        actorContext.requirePermission(PRODUCT_VIEW);
        return organizationProductRepository.findAllByOrganizationId(effectiveOrganizationId).stream()
                .map(ProductMapper::toOrganizationProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationProductResponse get(UUID organizationId, UUID id) {
        UUID effectiveOrganizationId = ensureOrganizationAccess(organizationId);
        actorContext.requirePermission(PRODUCT_VIEW);
        OrganizationProduct organizationProduct = organizationProductRepository.findByIdAndOrganizationId(id, effectiveOrganizationId)
                .orElseThrow(() -> new NotFoundException("Organization product not found"));
        return ProductMapper.toOrganizationProductResponse(organizationProduct);
    }

    @Transactional
    public OrganizationProductResponse create(UUID organizationId, OrganizationProductRequest request) {
        return create(organizationId, request, true, PRODUCT_MANAGE);
    }

    /**
     * Receiving uses the same canonical product and organization-product workflow.
     * A QR may be generated after the product has a persistent identity, so a
     * supplier barcode is intentionally optional here.
     */
    @Transactional
    public OrganizationProductResponse createForReceiving(UUID organizationId, OrganizationProductRequest request) {
        return create(organizationId, request, false, GOODS_RECEIVE);
    }

    private OrganizationProductResponse create(UUID organizationId, OrganizationProductRequest request,
                                               boolean barcodeRequired, q.qedet.common.model.Permission permission) {
        UUID effectiveOrganizationId = ensureOrganizationAccess(organizationId);
        actorContext.requirePermission(permission);
        if (request == null) {
            throw new BadRequestException("Organization product request is required");
        }

        Product product = resolveProduct(request, effectiveOrganizationId, barcodeRequired);
        Organization organization = organizationRepository.findById(effectiveOrganizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (organizationProductRepository.existsByOrganizationIdAndProductId(effectiveOrganizationId, product.getId())) {
            throw new ConflictException("Product is already linked to this organization");
        }

        OrganizationProduct organizationProduct = new OrganizationProduct();
        organizationProduct.setOrganization(organization);
        organizationProduct.setProduct(product);
        organizationProduct.setOrganizationSku(request.organizationSku() == null || request.organizationSku().isBlank() ? null : request.organizationSku().trim());
        organizationProduct.setStatus(request.organizationProductStatus() == null ? OrganizationProductStatus.ACTIVE : request.organizationProductStatus());
        OrganizationProduct saved = organizationProductRepository.save(organizationProduct);

        createBarcodeIfProvided(saved, request.barcode(), request.barcodeType());
        return ProductMapper.toOrganizationProductResponse(saved);
    }

    private Product resolveProduct(OrganizationProductRequest request, UUID organizationId, boolean barcodeRequired) {
        if (request.productId() != null) {
            return productRepository.findById(request.productId())
                    .orElseThrow(() -> new NotFoundException("Product not found"));
        }

        if (barcodeRequired && (request.barcode() == null || request.barcode().isBlank())) {
            throw new BadRequestException("Barcode is required when creating a new organization product");
        }
        if (request.barcode() != null && !request.barcode().isBlank()) {
            String normalizedBarcode = BarcodeNormalizer.normalize(request.barcode());
            if (normalizedBarcode.isBlank()) {
                throw new BadRequestException("Barcode is required when creating a new organization product");
            }
            ProductBarcode existingBarcode = productBarcodeRepository.findByBarcode(normalizedBarcode).orElse(null);
            if (existingBarcode != null && existingBarcode.getProduct() != null) {
                return existingBarcode.getProduct();
            }
            if (existingBarcode != null && existingBarcode.getOrganizationProduct() != null) {
                throw new ConflictException("Barcode is already assigned to another organization product");
            }
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("Product name is required when creating a new global product");
        }
        Product product = new Product();
        product.setName(request.name().trim());
        product.setDescription(request.description() == null ? null : request.description().trim());
        product.setBrand(resolveBrand(request.brandId()));
        product.setCategory(resolveCategory(request.categoryId()));
        product.setUnit(resolveUnit(request.unitId()));
        product.setProductType(request.productType() == null ? ProductType.STANDARD : request.productType());
        product.setStatus(request.status() == null ? ProductStatus.ACTIVE : request.status());
        return productRepository.save(product);
    }

    private void createBarcodeIfProvided(OrganizationProduct organizationProduct, String rawBarcode, ProductBarcodeType barcodeType) {
        if (rawBarcode == null || rawBarcode.isBlank()) {
            return;
        }
        ProductBarcodeType resolvedType = barcodeType == null ? inferBarcodeType(rawBarcode) : barcodeType;
        String normalizedBarcode = BarcodeNormalizer.normalize(rawBarcode);
        BarcodeNormalizer.validate(normalizedBarcode, resolvedType);
        if (productBarcodeRepository.findByBarcode(normalizedBarcode).isPresent()) {
            throw new ConflictException("Barcode already exists for another product");
        }

        ProductBarcode barcode = new ProductBarcode();
        barcode.setOrganizationProduct(organizationProduct);
        barcode.setBarcode(normalizedBarcode);
        barcode.setBarcodeType(resolvedType);
        barcode.setPrimary(true);
        productBarcodeRepository.save(barcode);
    }

    private ProductBarcodeType inferBarcodeType(String rawBarcode) {
        String normalized = BarcodeNormalizer.normalize(rawBarcode);
        if (normalized.matches("^[0-9]+$")) {
            int len = normalized.length();
            if (len == 13) {
                return ProductBarcodeType.EAN_13;
            }
            if (len == 8) {
                return ProductBarcodeType.EAN_8;
            }
            if (len == 12) {
                return ProductBarcodeType.UPC_A;
            }
            if (len == 7) {
                return ProductBarcodeType.UPC_E;
            }
            if (len == 14) {
                return ProductBarcodeType.GTIN;
            }
        }
        return ProductBarcodeType.INTERNAL;
    }

    private UUID ensureOrganizationAccess(UUID organizationId) {
        UUID effectiveOrganizationId = actorContext.organizationId();
        if (organizationId == null || !organizationId.equals(effectiveOrganizationId)) {
            throw new ForbiddenException("You can only access your organization data");
        }
        return effectiveOrganizationId;
    }

    private q.qedet.product.entity.ProductBrand resolveBrand(UUID brandId) {
        if (brandId == null) {
            return null;
        }
        return productBrandRepository.findById(brandId)
                .orElseThrow(() -> new NotFoundException("Brand not found"));
    }

    private q.qedet.product.entity.ProductCategory resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private q.qedet.product.entity.ProductUnit resolveUnit(UUID unitId) {
        if (unitId == null) {
            return null;
        }
        return productUnitRepository.findById(unitId)
                .orElseThrow(() -> new NotFoundException("Unit not found"));
    }
}
