package q.qedet.product;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import q.qedet.auth.security.AuthenticatedPrincipal;
import q.qedet.auth.security.PermissionCatalog;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.model.BusinessType;
import q.qedet.common.model.Permission;
import q.qedet.common.model.RoleType;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.product.dto.OrganizationProductRequest;
import q.qedet.product.entity.Product;
import q.qedet.product.entity.ProductBarcode;
import q.qedet.product.entity.ProductBarcodeType;
import q.qedet.product.entity.ProductBrand;
import q.qedet.product.entity.ProductCategory;
import q.qedet.product.entity.ProductStatus;
import q.qedet.product.entity.ProductType;
import q.qedet.product.entity.ProductUnit;
import q.qedet.product.repository.ProductBarcodeRepository;
import q.qedet.product.repository.ProductBrandRepository;
import q.qedet.product.repository.ProductCategoryRepository;
import q.qedet.product.repository.ProductRepository;
import q.qedet.product.repository.ProductUnitRepository;
import q.qedet.product.service.OrganizationProductService;
import q.qedet.product.service.ProductService;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ProductBusinessRulesTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductBrandRepository productBrandRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductUnitRepository productUnitRepository;

    @Autowired
    private ProductBarcodeRepository productBarcodeRepository;

    @Autowired
    private OrganizationProductService organizationProductService;

    @Autowired
    private ProductService productService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findProductByBarcodeUsesNormalizedValue() {
        Organization organization = saveOrganization("Acme Trading");
        setAuthenticatedOrganization(organization);

        ProductBrand brand = saveBrand("Samsung-" + UUID.randomUUID());
        ProductCategory category = saveCategory("Chargers-" + UUID.randomUUID());
        ProductUnit unit = saveUnit("Piece-" + UUID.randomUUID(), "pcs");

        Product product = createProduct("Samsung 25W Charger", brand, category, unit);

        ProductBarcode barcode = new ProductBarcode();
        barcode.setProduct(product);
        barcode.setBarcode("629 1234-567890");
        barcode.setBarcodeType(ProductBarcodeType.EAN_13);
        productBarcodeRepository.saveAndFlush(barcode);

        var lookup = productService.findProductByBarcode("629-1234-567890");
        assertThat(lookup.productId()).isEqualTo(product.getId());
        assertThat(lookup.productName()).isEqualTo("Samsung 25W Charger");
    }

    @Test
    void duplicateOrganizationProductInSameOrganizationIsRejected() {
        Organization organization = saveOrganization("Delta Retail");
        setAuthenticatedOrganization(organization);

        ProductBrand brand = saveBrand("Anker-" + UUID.randomUUID());
        ProductCategory category = saveCategory("Mobile Accessories-" + UUID.randomUUID());
        ProductUnit unit = saveUnit("Piece-" + UUID.randomUUID(), "pcs");

        Product product = createProduct("Anker 20W Cable", brand, category, unit);

        organizationProductService.create(organization.getId(), new OrganizationProductRequest(
                product.getId(),
                "ANK-20W-001",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThatThrownBy(() -> organizationProductService.create(organization.getId(), new OrganizationProductRequest(
                product.getId(),
                "ANK-20W-002",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void duplicateGlobalBarcodeIsRejected() {
        Organization organization = saveOrganization("Nova Supply");
        setAuthenticatedOrganization(organization);

        ProductBrand brand = saveBrand("Sony-" + UUID.randomUUID());
        ProductCategory category = saveCategory("Audio-" + UUID.randomUUID());
        ProductUnit unit = saveUnit("Piece-" + UUID.randomUUID(), "pcs");

        Product firstProduct = createProduct("Sony Earbuds", brand, category, unit);
        ProductBarcode firstBarcode = new ProductBarcode();
        firstBarcode.setProduct(firstProduct);
        firstBarcode.setBarcode("1234567890128");
        firstBarcode.setBarcodeType(ProductBarcodeType.EAN_13);
        productBarcodeRepository.saveAndFlush(firstBarcode);

        Product secondProduct = createProduct("Sony Earbuds Pro", brand, category, unit);
        ProductBarcode secondBarcode = new ProductBarcode();
        secondBarcode.setProduct(secondProduct);
        secondBarcode.setBarcode("1234567890128");
        secondBarcode.setBarcodeType(ProductBarcodeType.EAN_13);

        assertThatThrownBy(() -> productBarcodeRepository.saveAndFlush(secondBarcode))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Product createProduct(String name, ProductBrand brand, ProductCategory category, ProductUnit unit) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Test product");
        product.setBrand(brand);
        product.setCategory(category);
        product.setUnit(unit);
        product.setProductType(ProductType.STANDARD);
        product.setStatus(ProductStatus.ACTIVE);
        return productRepository.saveAndFlush(product);
    }

    private Organization saveOrganization(String name) {
        Organization organization = new Organization(name + "-" + UUID.randomUUID(), BusinessType.GENERAL_TRADE);
        return organizationRepository.save(organization);
    }

    private void setAuthenticatedOrganization(Organization organization) {
        Set<Permission> permissions = PermissionCatalog.permissionsFor(RoleType.OWNER);
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                null,
                null,
                organization.getId(),
                RoleType.OWNER,
                permissions,
                "owner@example.com"
        );
        var authorities = permissions.stream().map(permission -> new SimpleGrantedAuthority(permission.name())).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "token", authorities)
        );
    }

    private ProductBrand saveBrand(String name) {
        ProductBrand brand = new ProductBrand();
        brand.setName(name);
        return productBrandRepository.save(brand);
    }

    private ProductCategory saveCategory(String name) {
        ProductCategory category = new ProductCategory();
        category.setName(name);
        return productCategoryRepository.save(category);
    }

    private ProductUnit saveUnit(String name, String symbol) {
        ProductUnit unit = new ProductUnit();
        unit.setName(name);
        unit.setSymbol(symbol + "-" + UUID.randomUUID().toString().substring(0, 4));
        return productUnitRepository.save(unit);
    }
}
