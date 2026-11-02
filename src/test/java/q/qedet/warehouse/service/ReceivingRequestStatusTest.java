package q.qedet.warehouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.security.ActorContext;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.organization.repository.OrganizationZoneRepository;
import q.qedet.procurement.repository.PurchaseOrderItemRepository;
import q.qedet.procurement.repository.PurchaseOrderRepository;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.product.repository.ProductRepository;
import q.qedet.product.service.OrganizationProductService;
import q.qedet.qr.service.QrCodeService;
import q.qedet.warehouse.entity.ReceivingOperation;
import q.qedet.warehouse.entity.ReceivingRequest;
import q.qedet.warehouse.entity.ReceivingRequestItem;
import q.qedet.warehouse.repository.ReceivingOperationItemRepository;
import q.qedet.warehouse.repository.ReceivingOperationRepository;
import q.qedet.warehouse.repository.ReceivingOperationSerialRepository;
import q.qedet.warehouse.repository.ReceivingOperationZoneAssignmentRepository;
import q.qedet.warehouse.repository.ReceivingRequestItemRepository;
import q.qedet.warehouse.repository.ReceivingRequestRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceivingRequestStatusTest {
    @Mock private ActorContext actor;
    @Mock private EmployeeRepository employees;
    @Mock private PurchaseOrderRepository purchaseOrders;
    @Mock private PurchaseOrderItemRepository purchaseOrderItems;
    @Mock private ReceivingRequestRepository requests;
    @Mock private ReceivingRequestItemRepository requestItems;
    @Mock private ReceivingOperationRepository operations;
    @Mock private ReceivingOperationItemRepository operationItems;
    @Mock private ReceivingOperationSerialRepository serials;
    @Mock private ReceivingOperationZoneAssignmentRepository assignments;
    @Mock private WarehouseService warehouseService;
    @Mock private QrCodeService qrCodes;
    @Mock private ProductRepository products;
    @Mock private OrganizationProductRepository organizationProducts;
    @Mock private OrganizationProductService organizationProductService;
    @Mock private OrganizationSiteRepository sites;
    @Mock private OrganizationZoneRepository zones;

    private ReceivingService service;

    @BeforeEach
    void setUp() {
        service = new ReceivingService(actor, employees, purchaseOrders, purchaseOrderItems, requests, requestItems,
                operations, operationItems, serials, assignments, warehouseService, qrCodes, products,
                organizationProducts, organizationProductService, sites, zones);
    }

    @Test
    void activeOperationKeepsPartiallyRecordedRequestInProgress() {
        ReceivingRequest request = request(ReceivingRequest.Status.PENDING);
        ReceivingRequestItem item = item(request, "100");
        when(operations.existsByReceivingRequestIdAndStatus(request.getId(), ReceivingOperation.Status.IN_PROGRESS)).thenReturn(true);

        service.recalculateRequestStatus(request);

        assertThat(request.getStatus()).isEqualTo(ReceivingRequest.Status.IN_PROGRESS);
        verifyNoInteractions(requestItems, operationItems);
    }

    @Test
    void completedOperationWithOutstandingQuantityIsPartiallyReceived() {
        ReceivingRequest request = request(ReceivingRequest.Status.IN_PROGRESS);
        ReceivingRequestItem item = item(request, "100");
        noActiveOperation(request);
        when(requestItems.findAllByReceivingRequestId(request.getId())).thenReturn(List.of(item));
        when(operationItems.receivedQuantityForRequestItem(item.getId())).thenReturn(new BigDecimal("43"));

        service.recalculateRequestStatus(request);

        assertThat(request.getStatus()).isEqualTo(ReceivingRequest.Status.PARTIALLY_RECEIVED);
    }

    @Test
    void resumingAPartiallyReceivedRequestMovesItBackToInProgress() {
        ReceivingRequest request = request(ReceivingRequest.Status.PARTIALLY_RECEIVED);
        when(operations.existsByReceivingRequestIdAndStatus(request.getId(), ReceivingOperation.Status.IN_PROGRESS)).thenReturn(true);

        service.recalculateRequestStatus(request);

        assertThat(request.getStatus()).isEqualTo(ReceivingRequest.Status.IN_PROGRESS);
    }

    @Test
    void multipleCompletedOperationsCompleteRequestOnlyAfterEveryItemIsReceived() {
        ReceivingRequest request = request(ReceivingRequest.Status.PARTIALLY_RECEIVED);
        ReceivingRequestItem first = item(request, "43");
        ReceivingRequestItem second = item(request, "57");
        noActiveOperation(request);
        when(requestItems.findAllByReceivingRequestId(request.getId())).thenReturn(List.of(first, second));
        when(operationItems.receivedQuantityForRequestItem(first.getId())).thenReturn(new BigDecimal("43"));
        when(operationItems.receivedQuantityForRequestItem(second.getId())).thenReturn(new BigDecimal("57"));

        service.recalculateRequestStatus(request);

        assertThat(request.getStatus()).isEqualTo(ReceivingRequest.Status.COMPLETED);
    }

    @Test
    void cancelledOperationContributionIsExcludedAndRequestReturnsToPending() {
        ReceivingRequest request = request(ReceivingRequest.Status.IN_PROGRESS);
        ReceivingRequestItem item = item(request, "100");
        noActiveOperation(request);
        when(requestItems.findAllByReceivingRequestId(request.getId())).thenReturn(List.of(item));
        when(operationItems.receivedQuantityForRequestItem(item.getId())).thenReturn(BigDecimal.ZERO);

        service.recalculateRequestStatus(request);

        assertThat(request.getStatus()).isEqualTo(ReceivingRequest.Status.PENDING);
    }

    @Test
    void terminalRequestStatusesArePreserved() {
        ReceivingRequest cancelled = request(ReceivingRequest.Status.CANCELLED);
        ReceivingRequest completed = request(ReceivingRequest.Status.COMPLETED);

        service.recalculateRequestStatus(cancelled);
        service.recalculateRequestStatus(completed);

        assertThat(cancelled.getStatus()).isEqualTo(ReceivingRequest.Status.CANCELLED);
        assertThat(completed.getStatus()).isEqualTo(ReceivingRequest.Status.COMPLETED);
        verifyNoInteractions(operations, requestItems, operationItems);
    }

    @Test
    void emptyAndZeroQuantityRequestsRemainPending() {
        ReceivingRequest empty = request(ReceivingRequest.Status.IN_PROGRESS);
        ReceivingRequest zeroQuantity = request(ReceivingRequest.Status.IN_PROGRESS);
        ReceivingRequestItem item = item(zeroQuantity, "0");
        noActiveOperation(empty);
        noActiveOperation(zeroQuantity);
        when(requestItems.findAllByReceivingRequestId(empty.getId())).thenReturn(List.of());
        when(requestItems.findAllByReceivingRequestId(zeroQuantity.getId())).thenReturn(List.of(item));
        when(operationItems.receivedQuantityForRequestItem(item.getId())).thenReturn(BigDecimal.ZERO);

        service.recalculateRequestStatus(empty);
        service.recalculateRequestStatus(zeroQuantity);

        assertThat(empty.getStatus()).isEqualTo(ReceivingRequest.Status.PENDING);
        assertThat(zeroQuantity.getStatus()).isEqualTo(ReceivingRequest.Status.PENDING);
    }

    private void noActiveOperation(ReceivingRequest request) {
        when(operations.existsByReceivingRequestIdAndStatus(request.getId(), ReceivingOperation.Status.IN_PROGRESS)).thenReturn(false);
    }

    private ReceivingRequest request(ReceivingRequest.Status status) {
        ReceivingRequest request = new ReceivingRequest();
        request.setId(UUID.randomUUID());
        request.setStatus(status);
        return request;
    }

    private ReceivingRequestItem item(ReceivingRequest request, String expectedQuantity) {
        ReceivingRequestItem item = new ReceivingRequestItem();
        item.setId(UUID.randomUUID());
        item.setReceivingRequest(request);
        item.setExpectedQuantity(new BigDecimal(expectedQuantity));
        return item;
    }
}
