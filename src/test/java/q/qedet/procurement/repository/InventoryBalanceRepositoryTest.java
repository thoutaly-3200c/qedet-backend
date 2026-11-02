package q.qedet.procurement.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.common.model.BusinessType;
import q.qedet.common.model.LocationType;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.OrganizationZone;
import q.qedet.warehouse.entity.InventoryBalance;
import q.qedet.warehouse.repository.InventoryBalanceRepository;
import q.qedet.product.entity.OrganizationProduct;
import q.qedet.product.entity.Product;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InventoryBalanceRepositoryTest {
    @Autowired private EntityManager entityManager;
    @Autowired private InventoryBalanceRepository balances;

    @Test
    void projectsInventoryBalanceIntoThePublicInventoryDto() {
        Organization organization = new Organization("Projection Org", BusinessType.GENERAL_TRADE);
        entityManager.persist(organization);
        OrganizationSite site = new OrganizationSite(organization, "WAREHOUSE", "Warehouse", LocationType.WAREHOUSE);
        entityManager.persist(site);
        OrganizationZone zone = new OrganizationZone(site, "DEFAULT", "Default");
        entityManager.persist(zone);
        Product product = new Product(); product.setName("Widget");
        entityManager.persist(product);
        OrganizationProduct organizationProduct = new OrganizationProduct();
        organizationProduct.setOrganization(organization); organizationProduct.setProduct(product); organizationProduct.setOrganizationSku("W-1");
        entityManager.persist(organizationProduct);
        InventoryBalance balance = new InventoryBalance();
        balance.setOrganization(organization); balance.setProductId(product.getId()); balance.setZone(zone);
        balance.setOnHand(new BigDecimal("7")); balance.setReserved(new BigDecimal("2"));
        entityManager.persist(balance);
        entityManager.flush();
        entityManager.clear();

        var result = balances.findInventoryResponsesByOrganizationId(organization.getId());

        assertThat(result).singleElement().satisfies(row -> {
            assertThat(row.id()).isEqualTo(balance.getId());
            assertThat(row.productId()).isEqualTo(product.getId());
            assertThat(row.sku()).isEqualTo("W-1");
            assertThat(row.siteCode()).isEqualTo("WAREHOUSE");
            assertThat(row.availableQuantity()).isEqualByComparingTo("5");
        });
    }
}
