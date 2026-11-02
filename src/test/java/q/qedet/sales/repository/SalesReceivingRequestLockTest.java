package q.qedet.sales.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import q.qedet.common.model.BusinessType;
import q.qedet.common.model.LocationType;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.OrganizationZone;
import q.qedet.sales.entity.SalesReceivingRequest;
import q.qedet.sales.entity.SalesStockRequest;
import q.qedet.warehouse.entity.SalesStockFulfillment;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SalesReceivingRequestLockTest {
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired SalesReceivingRequestRepository receivingRequests;

    @Test
    void secondTransactionWaitsForFirstDecisionAndSeesUpdatedStatus() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        UUID[] ids = transaction.execute(status -> {
            Organization organization = new Organization("Receiving lock test", BusinessType.GENERAL_TRADE);
            entityManager.persist(organization);
            OrganizationSite hall = new OrganizationSite(organization, "SALES", "Sales Hall", LocationType.SALES_HALL);
            OrganizationSite warehouse = new OrganizationSite(organization, "WAREHOUSE", "Warehouse", LocationType.WAREHOUSE);
            entityManager.persist(hall);
            entityManager.persist(warehouse);
            OrganizationZone zone = new OrganizationZone(hall, "DEFAULT", "Default");
            entityManager.persist(zone);
            SalesStockRequest request = new SalesStockRequest();
            request.setOrganization(organization);
            
            
            request.setCreatedBy(UUID.randomUUID());
            entityManager.persist(request);
            SalesStockFulfillment fulfillment = new SalesStockFulfillment();
            fulfillment.setOrganization(organization);
            fulfillment.setSalesStockRequestId(request.getId());
            fulfillment.setWarehouseId(warehouse.getId());
            fulfillment.setSalesHallId(hall.getId());
            fulfillment.setDestinationZone(zone);
            fulfillment.setConfirmedBy(UUID.randomUUID());
            entityManager.persist(fulfillment);
            SalesReceivingRequest receiving = new SalesReceivingRequest();
            receiving.setOrganization(organization);
            receiving.setStockRequest(request);
            receiving.setFulfillment(fulfillment);
            receiving.setSalesHallId(hall.getId());
            receiving.setDestinationZone(zone);
            entityManager.persist(receiving);
            entityManager.flush();
            return new UUID[]{receiving.getId(), organization.getId()};
        });

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> transaction.execute(status -> {
                SalesReceivingRequest receiving = receivingRequests.findForUpdate(ids[0], ids[1]).orElseThrow();
                locked.countDown();
                try { assertThat(release.await(5, TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
                receiving.setStatus("ACCEPTED");
                return null;
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> transaction.execute(status -> {
                secondStarted.countDown();
                return receivingRequests.findForUpdate(ids[0], ids[1]).orElseThrow().getStatus();
            }));
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo("ACCEPTED");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}

