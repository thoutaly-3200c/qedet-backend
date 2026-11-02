package q.qedet.sales.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.model.Permission;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.finance.entity.CashBox;
import q.qedet.finance.entity.CashTransaction;
import q.qedet.finance.model.CashBoxStatus;
import q.qedet.finance.model.CashBoxType;
import q.qedet.finance.model.CashTransactionType;
import q.qedet.finance.repository.CashBoxRepository;
import q.qedet.finance.repository.CashTransactionRepository;
import q.qedet.organization.entity.*;
import q.qedet.organization.repository.*;
import q.qedet.organization.service.OrganizationConfigurationService;
import q.qedet.product.entity.*;
import q.qedet.product.repository.*;
import q.qedet.sales.dto.SalesDtos;
import q.qedet.sales.entity.*;
import q.qedet.sales.repository.*;
import q.qedet.warehouse.entity.*;
import q.qedet.warehouse.repository.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PosCheckoutServiceTest {
    @Mock ActorContext actor;
    @Mock CashierService cashier;
    @Mock OrganizationRepository organizations;
    @Mock OrganizationConfigurationRepository configurations;
    @Mock OrganizationConfigurationService numbering;
    @Mock EmployeeRepository employees;
    @Mock ProductRepository products;
    @Mock OrganizationProductRepository organizationProducts;
    @Mock SalesPriceRepository prices;
    @Mock SalesPackageRepository packages;
    @Mock InventoryBalanceRepository balances;
    @Mock StockMovementRepository movements;
    @Mock PosQuoteRepository quotes;
    @Mock PosSaleRepository sales;
    @Mock CashBoxRepository cashBoxes;
    @Mock CashTransactionRepository cashTransactions;
    @InjectMocks PosCheckoutService service;

    UUID orgId = UUID.randomUUID(), employeeId = UUID.randomUUID(), hallId = UUID.randomUUID(), productId = UUID.randomUUID();
    Organization org;
    Employee employee;
    OrganizationSite hall;
    OrganizationConfiguration config;
    InventoryBalance stock;
    CashBox box;
    PosQuote savedQuote;

    @BeforeEach void setup() {
        org = new Organization(); org.setId(orgId);
        employee = new Employee(); employee.setId(employeeId); employee.setOrganization(org);
        employee.setFirstName("Test"); employee.setLastName("Cashier");
        hall = new OrganizationSite(); hall.setId(hallId); hall.setOrganization(org);
        PosAssignment assignment = new PosAssignment(); assignment.setSalesHall(hall);
        config = new OrganizationConfiguration(); config.setOrganization(org); config.setCurrency("SAR");
        config.setSalesTaxRate(new BigDecimal("10.0000"));
        Product product = new Product(); product.setId(productId); product.setName("Rice"); product.setProductType(ProductType.WEIGHTED);
        OrganizationProduct op = new OrganizationProduct(); op.setOrganization(org); op.setProduct(product); op.setOrganizationSku("RICE");
        SalesPrice price = new SalesPrice(); price.setCurrentPrice(new BigDecimal("80.00"));
        OrganizationZone zone = new OrganizationZone(); zone.setSite(hall);
        stock = new InventoryBalance(); stock.setZone(zone); stock.setProductId(productId);
        stock.setOnHand(new BigDecimal("10.00")); stock.setReserved(BigDecimal.ZERO);
        box = new CashBox(); box.setId(UUID.randomUUID()); box.setOrganization(org); box.setOwnerEmployee(employee);
        box.setType(CashBoxType.EMPLOYEE); box.setStatus(CashBoxStatus.ACTIVE); box.setCurrency("SAR");
        when(actor.organizationId()).thenReturn(orgId); when(actor.employeeId()).thenReturn(employeeId);
        when(actor.permissions()).thenReturn(Set.of(Permission.SALE_VIEW));
        when(cashier.assignmentRecord()).thenReturn(assignment); when(cashier.currency()).thenReturn("SAR");
        when(organizations.findById(orgId)).thenReturn(Optional.of(org)); when(organizations.findByIdForUpdate(orgId)).thenReturn(Optional.of(org));
        when(employees.findByIdAndOrganizationId(employeeId, orgId)).thenReturn(Optional.of(employee));
        when(configurations.findByOrganizationId(orgId)).thenReturn(Optional.of(config));
        when(products.findById(productId)).thenReturn(Optional.of(product));
        when(organizationProducts.findByOrganizationIdAndProductId(orgId, productId)).thenReturn(Optional.of(op));
        when(prices.findByOrganizationIdAndProductId(orgId, productId)).thenReturn(Optional.of(price));
        when(balances.findAllByOrganizationIdAndProductId(orgId, productId)).thenReturn(List.of(stock));
        when(balances.findAllForUpdateByOrganizationIdAndProductId(orgId, productId)).thenReturn(List.of(stock));
        when(cashBoxes.findLockedByIdAndOrganizationId(box.getId(), orgId)).thenReturn(Optional.of(box));
        when(cashTransactions.findAllByCashBoxIdAndOrganizationIdOrderByPostedAtDesc(box.getId(), orgId)).thenReturn(List.of());
        when(quotes.save(any(PosQuote.class))).thenAnswer(inv -> { savedQuote = inv.getArgument(0); savedQuote.setId(UUID.randomUUID()); return savedQuote; });
        when(quotes.findByOrganizationIdAndTokenHash(eq(orgId), anyString())).thenAnswer(inv -> Optional.ofNullable(savedQuote));
        when(numbering.nextNumber(SequenceType.INVOICE)).thenReturn("000001");
        when(sales.saveAndFlush(any(PosSale.class))).thenAnswer(inv -> { PosSale sale = inv.getArgument(0); sale.setId(UUID.randomUUID()); return sale; });
    }

    private SalesDtos.PosQuoteResponse quote() {
        return service.quote(new SalesDtos.PosQuoteRequest(hallId,
                List.of(new SalesDtos.PosQuoteRequestLine(productId, new BigDecimal("0.750"), null, null))));
    }
    private SalesDtos.PosSaleRequest cash(String token, String amount) {
        return new SalesDtos.PosSaleRequest(token,
                new SalesDtos.PosPaymentRequest("CASH", box.getId(), new BigDecimal(amount), null));
    }

    @Test void weightedQuoteUsesConfiguredExclusiveTaxAndDoesNotWriteStock() {
        var result = quote();
        assertThat(result.subtotal()).isEqualByComparingTo("60.00");
        assertThat(result.taxTotal()).isEqualByComparingTo("6.00");
        assertThat(result.total()).isEqualByComparingTo("66.00");
        assertThat(result.lines().get(0).quantity()).isEqualByComparingTo("0.75");
        assertThat(result.quoteToken()).isNotBlank();
        verifyNoInteractions(movements, cashTransactions);
        verify(balances, never()).save(any());
    }

    @Test void inclusiveTaxIsExtractedFromPrice() {
        config.setSalesPricesTaxInclusive(true);
        var result = quote();
        assertThat(result.subtotal()).isEqualByComparingTo("54.55");
        assertThat(result.taxTotal()).isEqualByComparingTo("5.45");
        assertThat(result.total()).isEqualByComparingTo("60.00");
    }

    @Test void preparedPackageUsesStoredTotalAndCannotAppearTwice() {
        SalesPackage pack = new SalesPackage(); pack.setId(UUID.randomUUID()); pack.setProductId(productId);
        pack.setSalesHallId(hallId); pack.setStatus("ACTIVE"); pack.setWeight(new BigDecimal("0.75"));
        pack.setUnitPrice(new BigDecimal("70.00")); pack.setTotalPrice(new BigDecimal("50.00"));
        when(packages.findByIdAndOrganizationId(pack.getId(), orgId)).thenReturn(Optional.of(pack));
        var line = new SalesDtos.PosQuoteRequestLine(null, null, null, pack.getId());
        var result = service.quote(new SalesDtos.PosQuoteRequest(hallId, List.of(line)));
        assertThat(result.lines().get(0).packageId()).isEqualTo(pack.getId());
        assertThat(result.subtotal()).isEqualByComparingTo("50.00");
        assertThat(result.total()).isEqualByComparingTo("55.00");
        assertThatThrownBy(() -> service.quote(new SalesDtos.PosQuoteRequest(hallId, List.of(line, line))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test void quoteRejectsInsufficientStock() {
        stock.setOnHand(new BigDecimal("0.50"));
        assertThatThrownBy(this::quote).isInstanceOf(ConflictException.class).hasMessageContaining("Insufficient");
        verify(quotes, never()).save(any());
    }

    @Test void cashCheckoutPostsReceivedAndChangeAndDecrementsStock() {
        var q = quote();
        var result = service.sale("key-1", cash(q.quoteToken(), "70.00"));
        assertThat(result.transactionCode()).isEqualTo("SALE-000001");
        assertThat(result.change()).isEqualByComparingTo("4.00");
        assertThat(stock.getOnHand()).isEqualByComparingTo("9.25");
        ArgumentCaptor<CashTransaction> cash = ArgumentCaptor.forClass(CashTransaction.class);
        verify(cashTransactions, times(2)).save(cash.capture());
        assertThat(cash.getAllValues()).extracting(CashTransaction::getType)
                .containsExactly(CashTransactionType.CASH_IN, CashTransactionType.CASH_OUT);
        assertThat(cash.getAllValues()).extracting(CashTransaction::getAmount)
                .containsExactly(new BigDecimal("70.00"), new BigDecimal("4.00"));
        verify(movements).save(any(StockMovement.class));
    }

    @Test void insufficientCashDoesNotPersistSale() {
        var q = quote();
        assertThatThrownBy(() -> service.sale("key-2", cash(q.quoteToken(), "65.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Amount received");
        verify(sales, never()).saveAndFlush(any());
        verifyNoInteractions(movements, cashTransactions);
    }

    @Test void externalPaymentIsRejectedWithoutPosting() {
        var q = quote();
        var request = new SalesDtos.PosSaleRequest(q.quoteToken(),
                new SalesDtos.PosPaymentRequest("CARD", box.getId(), new BigDecimal("66"), null));
        assertThatThrownBy(() -> service.sale("key-3", request)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("confirmed payment integration");
        verify(sales, never()).saveAndFlush(any());
        verifyNoInteractions(cashTransactions);
    }

    @Test void expiredQuoteIsRejected() {
        var q = quote(); savedQuote.setExpiresAt(java.time.Instant.EPOCH);
        assertThatThrownBy(() -> service.sale("key-4", cash(q.quoteToken(), "70"))).isInstanceOf(ConflictException.class);
        verify(sales, never()).saveAndFlush(any());
    }

    @Test void wrongCashboxOwnerIsRejected() {
        var q = quote(); box.getOwnerEmployee().setId(UUID.randomUUID());
        assertThatThrownBy(() -> service.sale("key-5", cash(q.quoteToken(), "70"))).isInstanceOf(ConflictException.class);
        verify(sales, never()).saveAndFlush(any());
    }

    @Test void closedOrWrongCurrencyCashboxIsRejected() {
        var q = quote();
        box.setStatus(CashBoxStatus.CLOSED);
        assertThatThrownBy(() -> service.sale("key-closed", cash(q.quoteToken(), "70"))).isInstanceOf(ConflictException.class);
        box.setStatus(CashBoxStatus.ACTIVE); box.setCurrency("USD");
        assertThatThrownBy(() -> service.sale("key-currency", cash(q.quoteToken(), "70"))).isInstanceOf(ConflictException.class);
        verify(sales, never()).saveAndFlush(any());
    }

    @Test void serializedSaleFailsClosedWithoutPerSerialHallLocation() {
        Product serial = new Product(); serial.setId(productId); serial.setName("Phone"); serial.setProductType(ProductType.SERIALIZED);
        when(products.findById(productId)).thenReturn(Optional.of(serial));
        assertThatThrownBy(() -> service.quote(new SalesDtos.PosQuoteRequest(hallId,
                List.of(new SalesDtos.PosQuoteRequestLine(productId, BigDecimal.ONE, "SN123", null)))))
                .isInstanceOf(ConflictException.class).hasMessageContaining("location is not tracked");
        verify(quotes, never()).save(any());
    }

    @Test void changedPriceMakesQuoteStale() {
        var q = quote();
        when(prices.findByOrganizationIdAndProductId(orgId, productId)).thenAnswer(inv -> {
            SalesPrice changed = new SalesPrice(); changed.setCurrentPrice(new BigDecimal("90.00")); return Optional.of(changed);
        });
        assertThatThrownBy(() -> service.sale("key-6", cash(q.quoteToken(), "70")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Price or total changed");
        verify(sales, never()).saveAndFlush(any());
    }

    @Test void idempotentRetryReturnsExistingResult() {
        var q = quote();
        var first = service.sale("key-7", cash(q.quoteToken(), "70"));
        ArgumentCaptor<PosSale> saved = ArgumentCaptor.forClass(PosSale.class);
        verify(sales).saveAndFlush(saved.capture());
        when(sales.findByOrganizationIdAndCashierIdAndIdempotencyKey(orgId, employeeId, "key-7"))
                .thenReturn(Optional.of(saved.getValue()));
        var retry = service.sale("key-7", cash(q.quoteToken(), "70.00"));
        assertThat(retry.saleId()).isEqualTo(first.saleId());
        verify(sales, times(1)).saveAndFlush(any());
        verify(cashTransactions, times(2)).save(any());
    }

    @Test void idempotencyKeyCannotBeReusedForDifferentPayment() {
        var q = quote(); service.sale("key-8", cash(q.quoteToken(), "70"));
        ArgumentCaptor<PosSale> saved = ArgumentCaptor.forClass(PosSale.class);
        verify(sales).saveAndFlush(saved.capture());
        when(sales.findByOrganizationIdAndCashierIdAndIdempotencyKey(orgId, employeeId, "key-8"))
                .thenReturn(Optional.of(saved.getValue()));
        assertThatThrownBy(() -> service.sale("key-8", cash(q.quoteToken(), "71")))
                .isInstanceOf(ConflictException.class);
        verify(sales, times(1)).saveAndFlush(any());
    }

    @Test void receiptUsesPersistedLinesAndPaymentWithoutPosting() {
        var q = quote(); service.sale("key-9", cash(q.quoteToken(), "70"));
        ArgumentCaptor<PosSale> saved = ArgumentCaptor.forClass(PosSale.class);
        verify(sales).saveAndFlush(saved.capture());
        PosSale sale = saved.getValue();
        when(sales.findByIdAndOrganizationId(sale.getId(), orgId)).thenReturn(Optional.of(sale));
        var receipt = service.receipt(sale.getId());
        assertThat(receipt.total()).isEqualByComparingTo("66.00");
        assertThat(receipt.items()).hasSize(1);
        assertThat(receipt.paymentMethod()).isEqualTo("CASH");
        verify(cashTransactions, times(2)).save(any());
    }

    @Test void anotherCashierCannotReadReceipt() {
        var q = quote(); service.sale("key-10", cash(q.quoteToken(), "70"));
        ArgumentCaptor<PosSale> saved = ArgumentCaptor.forClass(PosSale.class);
        verify(sales).saveAndFlush(saved.capture());
        PosSale sale = saved.getValue();
        when(sales.findByIdAndOrganizationId(sale.getId(), orgId)).thenReturn(Optional.of(sale));
        when(actor.employeeId()).thenReturn(UUID.randomUUID());
        assertThatThrownBy(() -> service.receipt(sale.getId())).isInstanceOf(NotFoundException.class);
        verify(cashTransactions, times(2)).save(any());
    }
}
