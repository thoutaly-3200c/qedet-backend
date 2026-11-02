package q.qedet.procurement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.ForbiddenException;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.procurement.dto.ProcurementDtos.PurchaseRequestRequest;
import q.qedet.procurement.dto.ProcurementDtos.RequestItem;
import q.qedet.procurement.entity.PurchaseRequest;
import q.qedet.product.repository.ProductRepository;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.procurement.repository.PurchaseOrderRepository;
import q.qedet.procurement.repository.PurchaseOrderItemRepository;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcurementServicePurchaseRequestTest {
    @Mock private ActorContext actor;
    @Mock private OrganizationRepository organizations;
    @Mock private EmployeeRepository employees;
    @Mock private OrganizationProductRepository organizationProducts;
    @Mock private ProductRepository products;
    @Mock private SupplierRepository suppliers;
    @Mock private PurchaseRequestRepository requests;
    @Mock private PurchaseOrderRepository orders;
    @Mock private PurchaseOrderItemRepository orderItems;
    @Mock private ReceivingRecordRepository receiving;
    @Mock private ReceivingService receivingService;

    private ProcurementService service;
    private UUID organizationId;
    private UUID requesterId;

    @BeforeEach
    void setUp() {
        service = new ProcurementService(actor, organizations, employees, products, organizationProducts, suppliers, requests, orders, orderItems, receiving, receivingService);
        organizationId = UUID.randomUUID();
        requesterId = UUID.randomUUID();
        Organization organization = new Organization();
        when(actor.organizationId()).thenReturn(organizationId);
        when(actor.employeeId()).thenReturn(requesterId);
        when(organizations.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizations.findByIdForUpdate(organizationId)).thenReturn(Optional.of(organization));
        when(requests.findAllByOrganizationId(organizationId)).thenReturn(List.of());
        when(requests.save(any(PurchaseRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsMultipleNonCatalogItemsAndCalculatesSingleCurrencyEstimate() {
        var response = service.requestCreate(request(
                item("Laptop stand", new BigDecimal("2"), new BigDecimal("15.50"), null),
                item("USB hub", new BigDecimal("3"), new BigDecimal("10.00"), "Accessories")));

        assertThat(response.requestedBy()).isEqualTo(requesterId);
        assertThat(response.status()).isEqualTo("SUBMITTED");
        assertThat(response.procurementStatus()).isEqualTo("SUBMITTED");
        assertThat(response.totalEstimate()).isEqualByComparingTo("61.00");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).productName()).isEqualTo("Laptop stand");
        assertThat(response.items().get(1).itemCategory()).isEqualTo("Accessories");
    }

    @Test
    void rejectsEmptyItemsInvalidQuantitiesAndMixedCurrencies() {
        assertThatThrownBy(() -> service.requestCreate(new PurchaseRequestRequest(
                "STANDARD", "Office supplies", "Required", null, null, null, null, null, null, null, "USD", List.of())))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.requestCreate(request(
                item("Paper", BigDecimal.ZERO, BigDecimal.ONE, null))))
                .isInstanceOf(BadRequestException.class);

        PurchaseRequestRequest mixedCurrencies = new PurchaseRequestRequest(
                "STANDARD", "Office supplies", "Required", null, null, null, null, null, null, null, null,
                List.of(item("Paper", BigDecimal.ONE, BigDecimal.ONE, null),
                        new RequestItem(null, "Pens", null, null, null, null, BigDecimal.ONE, null, "pcs",
                                BigDecimal.ONE, "EUR", null)));
        assertThatThrownBy(() -> service.requestCreate(mixedCurrencies)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void preventsCancellationAfterApprovalAndUnauthorizedCancellation() {
        PurchaseRequest request = new PurchaseRequest();
        request.setRequestedBy(UUID.randomUUID());
        request.setOrganization(new Organization());
        request.setRequestType(PurchaseRequest.RequestType.STANDARD);
        request.setProcurementStatus(PurchaseRequest.ProcurementStatus.SUBMITTED);
        request.setOriginatingRequestStatus(PurchaseRequest.OriginatingRequestStatus.OPEN);
        UUID id = UUID.randomUUID();
        when(requests.findLockedByIdAndOrganizationId(eq(id), eq(organizationId))).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.requestAction(id, "cancel", null))
                .isInstanceOf(ForbiddenException.class);

        request.setRequestedBy(requesterId);
        service.requestAction(id, "approve", null);
        assertThat(request.getProcurementStatus()).isEqualTo(PurchaseRequest.ProcurementStatus.APPROVED);
        assertThatThrownBy(() -> service.requestAction(id, "cancel", null))
                .isInstanceOf(ConflictException.class);
    }

    private PurchaseRequestRequest request(RequestItem... items) {
        return new PurchaseRequestRequest("STANDARD", "Office supplies", "Required for operations", "Operations",
                null, null, null, "NORMAL", null, null, "USD", List.of(items));
    }

    private RequestItem item(String name, BigDecimal quantity, BigDecimal unitPrice, String category) {
        return new RequestItem(null, null, name, category, null, null, quantity, null,
                "pcs", unitPrice, null, null);
    }
}
