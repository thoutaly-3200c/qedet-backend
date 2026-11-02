package q.qedet.sales.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import q.qedet.auth.security.AuthenticatedPrincipal;
import q.qedet.common.model.*;
import q.qedet.employee.entity.Employee;
import q.qedet.finance.entity.CashBox;
import q.qedet.finance.model.*;
import q.qedet.finance.repository.CashTransactionRepository;
import q.qedet.organization.entity.*;
import q.qedet.product.entity.*;
import q.qedet.sales.dto.SalesDtos;
import q.qedet.sales.entity.*;
import q.qedet.sales.repository.PosSaleRepository;
import q.qedet.warehouse.entity.*;
import q.qedet.warehouse.repository.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class PosCheckoutIntegrationTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired PosCheckoutService checkout;
    @Autowired PosSaleRepository sales;
    @Autowired CashTransactionRepository cashTransactions;
    @Autowired InventoryBalanceRepository balances;
    @Autowired StockMovementRepository movements;

    private record Fixture(UUID organizationId, UUID cashierId, UUID hallId, UUID productId, UUID boxId, UUID balanceId) {}

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    private Fixture setup() {
        return new TransactionTemplate(transactions).execute(status -> {
            Organization org = new Organization("POS test " + UUID.randomUUID(), BusinessType.GENERAL_TRADE);
            em.persist(org);
            OrganizationConfiguration config = new OrganizationConfiguration(); config.setOrganization(org);
            config.setCurrency("SAR"); config.setSalesTaxRate(new BigDecimal("10.0000")); em.persist(config);
            OrganizationSite hall = new OrganizationSite(org, "HALL", "Test Hall", LocationType.SALES_HALL); em.persist(hall);
            OrganizationZone zone = new OrganizationZone(hall, "MAIN", "Main"); em.persist(zone);
            Employee cashier = new Employee(org, "CASH-1", "Test", "Cashier", "cashier@example.test", null, RoleType.CASHIER);
            cashier.setStatus(EmployeeStatus.ACTIVE); em.persist(cashier);
            PosAssignment assignment = new PosAssignment(); assignment.setOrganization(org); assignment.setCashier(cashier);
            assignment.setSalesHall(hall); assignment.setCode("POS-1"); em.persist(assignment);
            CashBox box = new CashBox(); box.setOrganization(org); box.setOwnerEmployee(cashier); box.setName("Cashier box");
            box.setCurrency("SAR"); box.setType(CashBoxType.EMPLOYEE); box.setProvisioningKey("EMPLOYEE-" + cashier.getId().toString().replace("-", "").toUpperCase());
            em.persist(box);
            Product product = new Product(); product.setName("Rice"); product.setProductType(ProductType.WEIGHTED); em.persist(product);
            OrganizationProduct op = new OrganizationProduct(); op.setOrganization(org); op.setProduct(product); op.setOrganizationSku("RICE"); em.persist(op);
            SalesPrice price = new SalesPrice(); price.setOrganization(org); price.setProductId(product.getId());
            price.setCurrentPrice(new BigDecimal("80.00")); price.setUpdatedBy(cashier.getId()); em.persist(price);
            InventoryBalance balance = new InventoryBalance(); balance.setOrganization(org); balance.setZone(zone);
            balance.setProductId(product.getId()); balance.setOnHand(new BigDecimal("10.00")); em.persist(balance);
            em.flush();
            return new Fixture(org.getId(), cashier.getId(), hall.getId(), product.getId(), box.getId(), balance.getId());
        });
    }

    private void authenticate(Fixture fixture) {
        Set<Permission> permissions = EnumSet.of(Permission.SALE_CREATE, Permission.SALE_VIEW, Permission.PAYMENT_RECEIVE, Permission.PRODUCT_SCAN);
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(UUID.randomUUID(), fixture.cashierId(), fixture.organizationId(),
                RoleType.CASHIER, permissions, "cashier@example.test");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "test", List.of()));
    }

    @Test void cashSalePersistsReceiptInventoryAndTwoCashTransactionsOnce() {
        Fixture fixture = setup(); authenticate(fixture);
        var quote = checkout.quote(new SalesDtos.PosQuoteRequest(fixture.hallId(),
                List.of(new SalesDtos.PosQuoteRequestLine(fixture.productId(), new BigDecimal("0.750"), null, null))));
        var request = new SalesDtos.PosSaleRequest(quote.quoteToken(),
                new SalesDtos.PosPaymentRequest("CASH", fixture.boxId(), new BigDecimal("70.00"), null));
        var first = checkout.sale("integration-key", request);
        var retry = checkout.sale("integration-key", request);
        assertThat(retry.saleId()).isEqualTo(first.saleId());
        assertThat(first.total()).isEqualByComparingTo("66.00");
        assertThat(first.change()).isEqualByComparingTo("4.00");
        assertThat(sales.findByIdAndOrganizationId(first.saleId(), fixture.organizationId())).isPresent();
        assertThat(balances.findById(fixture.balanceId()).orElseThrow().getOnHand()).isEqualByComparingTo("9.25");
        assertThat(cashTransactions.findAllByCashBoxIdAndOrganizationIdOrderByPostedAtDesc(fixture.boxId(), fixture.organizationId())).hasSize(2);
        assertThat(movements.findAll()).filteredOn(m -> "POS_SALE".equals(m.getMovementType())
                && first.saleId().toString().equals(m.getReference())).hasSize(1);
        var receipt = checkout.receipt(first.saleId());
        assertThat(receipt.items()).hasSize(1);
        assertThat(receipt.total()).isEqualByComparingTo("66.00");
    }

    @Test void concurrentCheckoutsCannotOversellTheSameHallBalance() throws Exception {
        Fixture fixture = setup(); authenticate(fixture);
        var cart = new SalesDtos.PosQuoteRequest(fixture.hallId(),
                List.of(new SalesDtos.PosQuoteRequestLine(fixture.productId(), new BigDecimal("6.00"), null, null)));
        var first = checkout.quote(cart);
        var second = checkout.quote(cart);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Boolean> attemptOne = () -> attemptCheckout(fixture, first.quoteToken(), "concurrent-1", start);
            Callable<Boolean> attemptTwo = () -> attemptCheckout(fixture, second.quoteToken(), "concurrent-2", start);
            Future<Boolean> one = pool.submit(attemptOne), two = pool.submit(attemptTwo);
            start.countDown();
            assertThat((one.get(15, TimeUnit.SECONDS) ? 1 : 0) + (two.get(15, TimeUnit.SECONDS) ? 1 : 0)).isEqualTo(1);
            assertThat(balances.findById(fixture.balanceId()).orElseThrow().getOnHand()).isEqualByComparingTo("4.00");
            assertThat(cashTransactions.findAllByCashBoxIdAndOrganizationIdOrderByPostedAtDesc(fixture.boxId(), fixture.organizationId())).hasSize(2);
        } finally { pool.shutdownNow(); }
    }

    @Test void lateMovementFailureRollsBackSaleStockAndCash() {
        Fixture fixture = setup(); authenticate(fixture);
        var quote = checkout.quote(new SalesDtos.PosQuoteRequest(fixture.hallId(),
                List.of(new SalesDtos.PosQuoteRequestLine(fixture.productId(), new BigDecimal("1.00"), null, null))));
        new TransactionTemplate(transactions).execute(status -> {
            em.createNativeQuery("ALTER TABLE stock_movements ADD CONSTRAINT fail_pos_movement CHECK (movement_type <> 'POS_SALE')")
                    .executeUpdate(); return null;
        });
        try {
            assertThatThrownBy(() -> checkout.sale("rollback-key", new SalesDtos.PosSaleRequest(quote.quoteToken(),
                    new SalesDtos.PosPaymentRequest("CASH", fixture.boxId(), new BigDecimal("100.00"), null))))
                    .isInstanceOf(RuntimeException.class);
            assertThat(sales.findByOrganizationIdAndCashierIdAndIdempotencyKey(
                    fixture.organizationId(), fixture.cashierId(), "rollback-key")).isEmpty();
            assertThat(balances.findById(fixture.balanceId()).orElseThrow().getOnHand()).isEqualByComparingTo("10.00");
            assertThat(cashTransactions.findAllByCashBoxIdAndOrganizationIdOrderByPostedAtDesc(fixture.boxId(), fixture.organizationId())).isEmpty();
        } finally {
            new TransactionTemplate(transactions).execute(status -> {
                em.createNativeQuery("ALTER TABLE stock_movements DROP CONSTRAINT fail_pos_movement").executeUpdate();
                return null;
            });
        }
    }

    private boolean attemptCheckout(Fixture fixture, String token, String key, CountDownLatch start) throws Exception {
        authenticate(fixture);
        start.await();
        try {
            checkout.sale(key, new SalesDtos.PosSaleRequest(token,
                    new SalesDtos.PosPaymentRequest("CASH", fixture.boxId(), new BigDecimal("600.00"), null)));
            return true;
        } catch (q.qedet.common.exception.ConflictException expected) {
            return false;
        } finally { SecurityContextHolder.clearContext(); }
    }
}
