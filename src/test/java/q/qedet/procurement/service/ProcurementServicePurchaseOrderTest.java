package q.qedet.procurement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.ConflictException;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.procurement.dto.ProcurementDtos.PurchaseOrderItemRequest;
import q.qedet.procurement.dto.ProcurementDtos.PurchaseOrderRequest;
import q.qedet.product.entity.Product;
import q.qedet.product.entity.OrganizationProduct;
import q.qedet.procurement.entity.PurchaseOrder;
import q.qedet.procurement.entity.PurchaseOrderItem;
import q.qedet.procurement.entity.PurchaseRequest;
import q.qedet.procurement.entity.PurchaseRequestItem;
import q.qedet.procurement.entity.Supplier;
import q.qedet.product.repository.ProductRepository;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.procurement.repository.PurchaseOrderItemRepository;
import q.qedet.procurement.repository.PurchaseOrderRepository;
import q.qedet.procurement.repository.PurchaseRequestRepository;
import q.qedet.procurement.repository.ReceivingRecordRepository;
import q.qedet.procurement.repository.SupplierRepository;
import q.qedet.warehouse.service.ReceivingService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcurementServicePurchaseOrderTest {
    @Mock private ActorContext actor;
    @Mock private OrganizationRepository organizations;
    @Mock private EmployeeRepository employees;
    @Mock private ProductRepository products;
    @Mock private OrganizationProductRepository organizationProducts;
    @Mock private SupplierRepository suppliers;
    @Mock private PurchaseRequestRepository requests;
    @Mock private PurchaseOrderRepository orders;
    @Mock private PurchaseOrderItemRepository orderItems;
    @Mock private ReceivingRecordRepository receiving;
    @Mock private ReceivingService receivingService;

    private ProcurementService service;
    private UUID organizationId;
    private PurchaseRequest sourceRequest;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        service = new ProcurementService(actor, organizations, employees, products, organizationProducts, suppliers, requests, orders,
                orderItems, receiving, receivingService);
        organizationId = UUID.randomUUID();
        sourceRequest = approvedRequest();
        supplier = new Supplier();
        supplier.setId(UUID.randomUUID());
        supplier.setName("Supplier");
        supplier.setStatus(Supplier.SupplierStatus.Active);

        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        when(actor.organizationId()).thenReturn(organizationId);
        when(actor.employeeId()).thenReturn(employee.getId());
        when(requests.findLockedByIdAndOrganizationId(eq(sourceRequest.getId()), eq(organizationId)))
                .thenReturn(Optional.of(sourceRequest));
        when(employees.findByIdAndOrganizationId(employee.getId(), organizationId)).thenReturn(Optional.of(employee));
        when(organizations.findById(organizationId)).thenReturn(Optional.of(new Organization()));
        when(suppliers.findByIdAndOrganizationId(eq(supplier.getId()), eq(organizationId))).thenReturn(Optional.of(supplier));
        when(orders.findAllByOrganizationId(organizationId)).thenReturn(List.of());
        when(orders.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsOrdersForCatalogAndNonCatalogItemsWithSourceItemDetails() {
        PurchaseRequestItem catalog = requestItem(true, new BigDecimal("3"));
        PurchaseRequestItem nonCatalog = requestItem(false, new BigDecimal("2"));
        sourceRequest.getItems().addAll(List.of(catalog, nonCatalog));
        when(orderItems.findAllByPurchaseRequestItemId(catalog.getId())).thenReturn(List.of());
        when(orderItems.findAllByPurchaseRequestItemId(nonCatalog.getId())).thenReturn(List.of());

        var response = service.orderCreate(order(catalog.getId(), new BigDecimal("1"), nonCatalog.getId(), new BigDecimal("2")));

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).product().catalogProduct()).isTrue();
        assertThat(response.items().get(0).product().id()).isNotNull();
        assertThat(response.items().get(1).product().catalogProduct()).isFalse();
        assertThat(response.items().get(1).product().id()).isNull();
        assertThat(response.items().get(1).product().name()).isEqualTo("Non-catalog item");
        assertThat(response.items().get(1).product().description()).isEqualTo("Source description");
        assertThat(response.items().get(1).unit()).isEqualTo("box");
    }

    @Test
    void createsOrderContainingOnlyCatalogItems() {
        PurchaseRequestItem catalog = requestItem(true, BigDecimal.ONE);
        sourceRequest.getItems().add(catalog);
        when(orderItems.findAllByPurchaseRequestItemId(catalog.getId())).thenReturn(List.of());

        var response = service.orderCreate(order(catalog.getId(), BigDecimal.ONE));

        assertThat(response.items()).singleElement()
                .satisfies(item -> assertThat(item.product().catalogProduct()).isTrue());
    }

    @Test
    void createsOrderContainingOnlyNonCatalogItems() {
        PurchaseRequestItem nonCatalog = requestItem(false, BigDecimal.ONE);
        sourceRequest.getItems().add(nonCatalog);
        when(orderItems.findAllByPurchaseRequestItemId(nonCatalog.getId())).thenReturn(List.of());

        var response = service.orderCreate(new PurchaseOrderRequest(sourceRequest.getId(), null, List.of(
                new PurchaseOrderItemRequest(nonCatalog.getId(), supplier.getId(), null, null, "USD", null,
                        "BANK_TRANSFER", "AFTER_DELIVERY", "STANDARD_UNIT", BigDecimal.ONE, "box", new BigDecimal("100.00"),
                        null, null, null, null))));

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.product().catalogProduct()).isFalse();
            assertThat(item.product().name()).isEqualTo("Non-catalog item");
            assertThat(item.product().description()).isEqualTo("Source description");
            assertThat(item.unit()).isEqualTo("box");
        });
    }

    @Test
    void createsOrderWithDifferentSuppliersForEachItem() {
        PurchaseRequestItem catalog = requestItem(true, BigDecimal.ONE);
        PurchaseRequestItem nonCatalog = requestItem(false, BigDecimal.ONE);
        sourceRequest.getItems().addAll(List.of(catalog, nonCatalog));
        Supplier alternateSupplier = new Supplier();
        alternateSupplier.setId(UUID.randomUUID());
        alternateSupplier.setName("Alternate supplier");
        alternateSupplier.setStatus(Supplier.SupplierStatus.Active);
        when(orderItems.findAllByPurchaseRequestItemId(catalog.getId())).thenReturn(List.of());
        when(orderItems.findAllByPurchaseRequestItemId(nonCatalog.getId())).thenReturn(List.of());
        when(suppliers.findByIdAndOrganizationId(alternateSupplier.getId(), organizationId))
                .thenReturn(Optional.of(alternateSupplier));

        var response = service.orderCreate(new PurchaseOrderRequest(sourceRequest.getId(), null, List.of(
                orderItem(catalog.getId(), BigDecimal.ONE),
                new PurchaseOrderItemRequest(nonCatalog.getId(), alternateSupplier.getId(), null, null,
                        "USD", null, "BANK_TRANSFER", "AFTER_DELIVERY", "STANDARD_UNIT", BigDecimal.ONE, "box",
                        new BigDecimal("100.00"), null, null, null, null))));

        assertThat(response.items()).extracting(item -> item.supplierId())
                .containsExactly(supplier.getId(), alternateSupplier.getId());
    }

    @Test
    void rejectsItemsOutsideTheSourceRequestAndQuantitiesBeyondTheRemainingAmount() {
        PurchaseRequestItem source = requestItem(false, new BigDecimal("2"));
        sourceRequest.getItems().add(source);
        when(orderItems.findAllByPurchaseRequestItemId(source.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.orderCreate(order(UUID.randomUUID(), BigDecimal.ONE)))
                .isInstanceOf(BadRequestException.class);

        PurchaseOrderItem allocated = new PurchaseOrderItem();
        allocated.setOrderStatus(PurchaseOrderItem.OrderStatus.DRAFT);
        allocated.setQuantity(new BigDecimal("1.5"));
        when(orderItems.findAllByPurchaseRequestItemId(source.getId())).thenReturn(List.of(allocated));

        assertThatThrownBy(() -> service.orderCreate(order(source.getId(), BigDecimal.ONE)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Purchase order quantity exceeds the approved request item remaining quantity");
    }

    @Test
    void marksNonCatalogItemReadyToReceiveAndReconcilesIt() {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        PurchaseRequestItem sourceItem = requestItem(false, BigDecimal.ONE);
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(UUID.randomUUID());
        item.setPurchaseRequestItem(sourceItem);
        item.setSupplier(supplier);
        item.setCreatedBy(employee);
        item.setCurrency("USD");
        item.setQuantity(BigDecimal.ONE);
        item.setTotalPrice(BigDecimal.TEN);
        item.setOrderStatus(PurchaseOrderItem.OrderStatus.SUPPLIER_CONFIRMED);
        PurchaseOrder order = new PurchaseOrder();
        order.setId(UUID.randomUUID());
        order.setOrganization(new Organization());
        order.setNumber("PO-1");
        order.setPurchaseRequest(sourceRequest);
        order.setCreatedBy(employee);
        order.getItems().add(item);
        when(orders.findLockedByIdAndOrganizationId(order.getId(), organizationId)).thenReturn(Optional.of(order));

        var response = service.itemAction(order.getId(), item.getId(), "READY_TO_RECEIVE");

        assertThat(response.items()).singleElement().satisfies(responseItem ->
                assertThat(responseItem.orderStatus()).isEqualTo("READY_TO_RECEIVE"));
        verify(receivingService).reconcileReadyItem(order, item);
    }

    private PurchaseRequest approvedRequest() {
        PurchaseRequest request = new PurchaseRequest();
        request.setId(UUID.randomUUID());
        request.setOrganization(new Organization());
        request.setNumber("PR-1");
        request.setOriginatingRequestStatus(PurchaseRequest.OriginatingRequestStatus.OPEN);
        request.setProcurementStatus(PurchaseRequest.ProcurementStatus.APPROVED);
        return request;
    }

    private PurchaseRequestItem requestItem(boolean catalog, BigDecimal quantity) {
        PurchaseRequestItem item = new PurchaseRequestItem();
        item.setId(UUID.randomUUID());
        item.setPurchaseRequest(sourceRequest);
        item.setProductName(catalog ? "Catalog item" : "Non-catalog item");
        item.setDescription("Source description");
        item.setUnit(catalog ? "EA" : "box");
        item.setQty(quantity);
        if (catalog) {
            Product product = new Product();
            product.setId(UUID.randomUUID());
            product.setName("Catalog item");
            q.qedet.product.entity.ProductUnit unit = new q.qedet.product.entity.ProductUnit();
            unit.setSymbol("EA"); product.setUnit(unit);
            item.setProduct(product);
            OrganizationProduct owned = new OrganizationProduct();
            owned.setProduct(product); owned.setOrganizationSku("SKU-1");
            when(organizationProducts.findByOrganizationIdAndProductId(organizationId, product.getId()))
                    .thenReturn(Optional.of(owned));
        }
        return item;
    }

    private PurchaseOrderRequest order(UUID requestItemId, BigDecimal quantity) {
        return order(requestItemId, quantity, null, null);
    }

    private PurchaseOrderRequest order(UUID firstItemId, BigDecimal firstQuantity, UUID secondItemId, BigDecimal secondQuantity) {
        List<PurchaseOrderItemRequest> items = new java.util.ArrayList<>();
        items.add(orderItem(firstItemId, firstQuantity));
        if (secondItemId != null) {
            items.add(orderItem(secondItemId, secondQuantity));
        }
        return new PurchaseOrderRequest(sourceRequest.getId(), null, items);
    }

    private PurchaseOrderItemRequest orderItem(UUID requestItemId, BigDecimal quantity) {
        String unit = sourceRequest.getItems().stream().filter(item -> item.getId().equals(requestItemId))
                .findFirst().map(PurchaseRequestItem::getUnit).orElse("EA");
        return new PurchaseOrderItemRequest(requestItemId, supplier.getId(), null, null, "USD", null,
                "BANK_TRANSFER", "AFTER_DELIVERY", "STANDARD_UNIT", quantity, unit, new BigDecimal("100.00"), null, null, null, null);
    }
}
