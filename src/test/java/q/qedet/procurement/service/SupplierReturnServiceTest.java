package q.qedet.procurement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.finance.service.FinanceWorkflowService;
import q.qedet.organization.entity.*;
import q.qedet.organization.repository.OrganizationZoneRepository;
import q.qedet.procurement.dto.SupplierReturnDtos.*;
import q.qedet.procurement.entity.*;
import q.qedet.procurement.repository.*;
import q.qedet.product.entity.Product;
import q.qedet.warehouse.entity.*;
import q.qedet.warehouse.repository.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierReturnServiceTest {
 @Mock ActorContext actor; @Mock WarehouseReturnRepository requests; @Mock WarehouseReturnItemRepository requestItems;
 @Mock ReceivingOperationItemRepository receiving; @Mock SupplierReturnOperationRepository operations; @Mock SupplierReturnOperationItemRepository operationItems;
 @Mock SupplierReturnDispatchRepository dispatches; @Mock InventoryBalanceRepository balances; @Mock StockMovementRepository movements;
 @Mock OrganizationZoneRepository zones; @Mock FinanceWorkflowService finance; @Mock q.qedet.finance.repository.FinanceRequestRepository financeRequests;
 private SupplierReturnService service(){return new SupplierReturnService(actor,requests,requestItems,receiving,operations,operationItems,dispatches,balances,movements,zones,finance,financeRequests);}
 private UUID org=UUID.randomUUID();
 private <T extends q.qedet.common.model.TimestampedEntity> T id(T e){e.setId(UUID.randomUUID());return e;}
 private Organization organization(){return id(new Organization());}
 private WarehouseReturn request(){WarehouseReturn r=id(new WarehouseReturn());Organization o=organization();o.setId(org);r.setOrganization(o);r.setType(WarehouseReturn.Type.SUPPLIER);r.setStatus(WarehouseReturn.Status.SUPPLIER_OPEN);r.setReference("RET-1");return r;}
 private WarehouseReturnItem requestItem(WarehouseReturn r){WarehouseReturnItem i=id(new WarehouseReturnItem());i.setWarehouseReturn(r);i.setProductId(UUID.randomUUID());i.setRequestedQuantity(new BigDecimal("3"));r.getItems().add(i);return i;}
 private ReceivingOperationItem source(WarehouseReturnItem item,Supplier supplier){
  Product product=id(new Product());product.setId(item.getProductId());PurchaseRequestItem pri=id(new PurchaseRequestItem());pri.setProduct(product);
  PurchaseOrder po=id(new PurchaseOrder());po.setOrganization(item.getWarehouseReturn().getOrganization());po.setNumber("PO-1");
  PurchaseOrderItem p=id(new PurchaseOrderItem());p.setPurchaseOrder(po);p.setPurchaseRequestItem(pri);p.setSupplier(supplier);p.setQuantity(new BigDecimal("10"));p.setTotalPrice(new BigDecimal("100"));p.setCurrency("USD");
  ReceivingRequest rr=id(new ReceivingRequest());rr.setOrganization(item.getWarehouseReturn().getOrganization());rr.setPurchaseOrder(po);ReceivingRequestItem ri=id(new ReceivingRequestItem());ri.setReceivingRequest(rr);ri.setPurchaseOrderItem(p);
  ReceivingOperation ro=id(new ReceivingOperation());ro.setOrganization(item.getWarehouseReturn().getOrganization());ro.setReceivingRequest(rr);ro.setStatus(ReceivingOperation.Status.COMPLETED);
  ReceivingOperationItem received=id(new ReceivingOperationItem());received.setReceivingRequestItem(ri);received.setReceivingOperation(ro);received.setReceivedQuantity(new BigDecimal("5"));received.setAcceptedQuantity(new BigDecimal("4"));return received;
 }
 private Supplier supplier(WarehouseReturn r){Supplier s=id(new Supplier());s.setOrganization(r.getOrganization());s.setName("Supplier");return s;}
 private void stub(WarehouseReturn r){when(actor.organizationId()).thenReturn(org);when(requests.findByIdAndOrganizationId(r.getId(),org)).thenReturn(Optional.of(r));for(var i:r.getItems())when(requestItems.findByIdAndWarehouseReturnOrganizationId(i.getId(),org)).thenReturn(Optional.of(i));}
 @Test void historyUsesActualReceivingQuantities(){WarehouseReturn r=request();WarehouseReturnItem i=requestItem(r);ReceivingOperationItem received=source(i,supplier(r));stub(r);when(receiving.eligibleHistory(org,i.getProductId())).thenReturn(List.of(received));History h=service().history(r.getId(),i.getId()).get(0);assertThat(h.receivedQuantity()).isEqualByComparingTo("5");assertThat(h.acceptedQuantity()).isEqualByComparingTo("4");assertThat(h.unitCost()).isEqualByComparingTo("10");}
 @Test void groupsTwoItemsForOneSupplier(){WarehouseReturn r=request();WarehouseReturnItem a=requestItem(r),b=requestItem(r);Supplier s=supplier(r);ReceivingOperationItem ra=source(a,s),rb=source(b,s);stub(r);when(receiving.findForUpdate(ra.getId())).thenReturn(Optional.of(ra));when(receiving.findForUpdate(rb.getId())).thenReturn(Optional.of(rb));when(operations.saveAll(any())).thenAnswer(x->new ArrayList<>((Collection<SupplierReturnOperation>)x.getArgument(0)));List<Operation> result=service().review(r.getId(),new Review(List.of(new ReviewLine(a.getId(),ra.getReceivingRequestItem().getPurchaseOrderItem().getId(),ra.getId(),BigDecimal.ONE),new ReviewLine(b.getId(),rb.getReceivingRequestItem().getPurchaseOrderItem().getId(),rb.getId(),BigDecimal.ONE))));assertThat(result).hasSize(1);assertThat(result.get(0).items()).hasSize(2);verifyNoInteractions(balances,movements);}
 @Test void rejectsApprovalAboveRequest(){WarehouseReturn r=request();WarehouseReturnItem i=requestItem(r);stub(r);assertThatThrownBy(()->service().review(r.getId(),new Review(List.of(new ReviewLine(i.getId(),UUID.randomUUID(),UUID.randomUUID(),new BigDecimal("4")))))).isInstanceOf(ConflictException.class);verifyNoInteractions(operations);}
 @Test void separatesSuppliersAndRejectsForeignPurchase(){WarehouseReturn r=request();WarehouseReturnItem a=requestItem(r),b=requestItem(r);ReceivingOperationItem ra=source(a,supplier(r)),rb=source(b,supplier(r));stub(r);when(receiving.findForUpdate(ra.getId())).thenReturn(Optional.of(ra));when(receiving.findForUpdate(rb.getId())).thenReturn(Optional.of(rb));when(operations.saveAll(any())).thenAnswer(x->new ArrayList<>((Collection<SupplierReturnOperation>)x.getArgument(0)));Review review=new Review(List.of(new ReviewLine(a.getId(),ra.getReceivingRequestItem().getPurchaseOrderItem().getId(),ra.getId(),BigDecimal.ONE),new ReviewLine(b.getId(),rb.getReceivingRequestItem().getPurchaseOrderItem().getId(),rb.getId(),BigDecimal.ONE)));assertThat(service().review(r.getId(),review)).hasSize(2);r.setStatus(WarehouseReturn.Status.SUPPLIER_OPEN);ra.getReceivingRequestItem().getPurchaseOrderItem().getPurchaseOrder().setOrganization(organization());assertThatThrownBy(()->service().review(r.getId(),review)).isInstanceOf(ConflictException.class);}
 @Test void dispatchDeductsOnceAndPartialAcceptanceIsExplicit(){WarehouseReturn r=request();WarehouseReturnItem requested=requestItem(r);SupplierReturnOperation op=id(new SupplierReturnOperation());op.setOrganization(r.getOrganization());op.setWarehouseReturn(r);op.setSupplier(supplier(r));op.setReference("SRET-1");SupplierReturnOperationItem i=id(new SupplierReturnOperationItem());i.setOperation(op);i.setWarehouseReturnItem(requested);i.setApprovedQuantity(new BigDecimal("3"));i.setDispatchedQuantity(BigDecimal.ZERO);i.setSupplierAcceptedQuantity(BigDecimal.ZERO);i.setSupplierRejectedQuantity(BigDecimal.ZERO);i.setUnitCost(BigDecimal.TEN);i.setCurrency("USD");ReceivingOperationItem received=source(requested,op.getSupplier());i.setPurchaseOrderItem(received.getReceivingRequestItem().getPurchaseOrderItem());i.setReceivingOperationItem(received);op.getItems().add(i);OrganizationZone zone=id(new OrganizationZone());InventoryBalance balance=id(new InventoryBalance());balance.setOnHand(new BigDecimal("5"));balance.setReserved(BigDecimal.ZERO);when(actor.organizationId()).thenReturn(org);when(actor.employeeId()).thenReturn(UUID.randomUUID());when(operations.findForUpdate(op.getId(),org)).thenReturn(Optional.of(op));when(zones.findByIdAndSiteOrganizationIdAndActiveTrue(zone.getId(),org)).thenReturn(Optional.of(zone));when(balances.findForUpdate(org,requested.getProductId(),zone.getId())).thenReturn(Optional.of(balance));Operation dispatched=service().dispatch(op.getId(),new Dispatch("D-1",null,List.of(new DispatchLine(i.getId(),zone.getId(),new BigDecimal("2")))));assertThat(balance.getOnHand()).isEqualByComparingTo("3");assertThat(dispatched.items().get(0).supplierAcceptedQuantity()).isEqualByComparingTo("0");verify(movements,times(1)).save(any());Operation accepted=service().acceptance(op.getId(),new Acceptance(List.of(new AcceptanceLine(i.getId(),BigDecimal.ONE,BigDecimal.ZERO))));assertThat(accepted.items().get(0).supplierAcceptedQuantity()).isEqualByComparingTo("1");}
 @Test void rejectsSupplierQuantitiesAboveDispatch(){WarehouseReturn r=request();WarehouseReturnItem requested=requestItem(r);SupplierReturnOperation op=id(new SupplierReturnOperation());op.setStatus(SupplierReturnOperation.Status.DISPATCHED);SupplierReturnOperationItem i=id(new SupplierReturnOperationItem());i.setDispatchedQuantity(BigDecimal.ONE);op.getItems().add(i);when(actor.organizationId()).thenReturn(org);when(operations.findForUpdate(op.getId(),org)).thenReturn(Optional.of(op));assertThatThrownBy(()->service().acceptance(op.getId(),new Acceptance(List.of(new AcceptanceLine(i.getId(),BigDecimal.ONE,BigDecimal.ONE))))).isInstanceOf(ConflictException.class);}
}
