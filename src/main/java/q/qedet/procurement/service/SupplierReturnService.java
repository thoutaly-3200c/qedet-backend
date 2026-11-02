package q.qedet.procurement.service;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.procurement.dto.SupplierReturnDtos.*;
import q.qedet.procurement.entity.*;
import q.qedet.procurement.repository.*;
import q.qedet.warehouse.entity.*;
import q.qedet.warehouse.repository.*;
import q.qedet.organization.repository.OrganizationZoneRepository;
import q.qedet.finance.service.FinanceWorkflowService;
import q.qedet.finance.repository.FinanceRequestRepository;
import java.math.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.*;

@Service
public class SupplierReturnService {
 private final ActorContext actor; private final WarehouseReturnRepository requests; private final WarehouseReturnItemRepository requestItems;
 private final ReceivingOperationItemRepository receiving; private final SupplierReturnOperationRepository operations; private final SupplierReturnOperationItemRepository operationItems;
 private final SupplierReturnDispatchRepository dispatches; private final InventoryBalanceRepository balances; private final StockMovementRepository movements; private final OrganizationZoneRepository zones; private final FinanceWorkflowService finance; private final FinanceRequestRepository financeRequests;
 public SupplierReturnService(ActorContext actor,WarehouseReturnRepository requests,WarehouseReturnItemRepository requestItems,ReceivingOperationItemRepository receiving,SupplierReturnOperationRepository operations,SupplierReturnOperationItemRepository operationItems,SupplierReturnDispatchRepository dispatches,InventoryBalanceRepository balances,StockMovementRepository movements,OrganizationZoneRepository zones,FinanceWorkflowService finance,FinanceRequestRepository financeRequests){
  this.actor=actor;this.requests=requests;this.requestItems=requestItems;this.receiving=receiving;this.operations=operations;this.operationItems=operationItems;this.dispatches=dispatches;this.balances=balances;this.movements=movements;this.zones=zones;this.finance=finance;this.financeRequests=financeRequests;
 }
 private WarehouseReturn request(UUID id){ WarehouseReturn r=requests.findByIdAndOrganizationId(id,actor.organizationId()).orElseThrow(()->new NotFoundException("SUPPLIER_RETURN_NOT_FOUND"));if(r.getType()!=WarehouseReturn.Type.SUPPLIER)throw new NotFoundException("SUPPLIER_RETURN_NOT_FOUND");return r; }
 private WarehouseReturnItem item(WarehouseReturn r,UUID id){return requestItems.findByIdAndWarehouseReturnOrganizationId(id,actor.organizationId()).filter(i->i.getWarehouseReturn().getId().equals(r.getId())).orElseThrow(()->new NotFoundException("SUPPLIER_RETURN_ITEM_NOT_FOUND"));}
 @Transactional(readOnly=true) public Page<Request> requests(int page,int size){actor.requirePermission(Permission.PROCUREMENT_VIEW);return requests.findAllByOrganizationIdAndType(actor.organizationId(),WarehouseReturn.Type.SUPPLIER,PageRequest.of(Math.max(0,page),Math.max(1,size))).map(this::requestView);}
 @Transactional(readOnly=true) public Request requestDetail(UUID id){actor.requirePermission(Permission.PROCUREMENT_VIEW);return requestView(request(id));}
 private Request requestView(WarehouseReturn r){return new Request(r.getId(),r.getReference(),r.getStatus().name(),r.getReason(),r.getItems().stream().map(i->new RequestItem(i.getId(),i.getProductId(),i.getRequestedQuantity(),i.getStatus())).toList());}
 @Transactional(readOnly=true) public List<History> history(UUID requestId,UUID itemId){
  actor.requirePermission(Permission.PROCUREMENT_VIEW);WarehouseReturnItem requested=item(request(requestId),itemId);
  return receiving.eligibleHistory(actor.organizationId(),requested.getProductId()).stream().map(i->{PurchaseOrderItem p=i.getReceivingRequestItem().getPurchaseOrderItem();return new History(p.getSupplier().getId(),p.getSupplier().getName(),p.getPurchaseOrder().getId(),p.getPurchaseOrder().getNumber(),p.getId(),i.getReceivingOperation().getId(),i.getReceivingRequestItem().getReceivingRequest().getRequestNumber(),i.getId(),p.getQuantity(),i.getReceivedQuantity(),i.getAcceptedQuantity(),i.getRejectedQuantity(),p.getCurrency(),p.getUnit(),p.getTotalPrice().divide(p.getQuantity(),4,RoundingMode.HALF_UP),"Supplier-level remaining stock is not tracked; accepted receiving quantity is an upper bound, not available stock.");}).toList();
 }
 @Transactional public List<Operation> review(UUID requestId,Review input){
  actor.requirePermission(Permission.PROCUREMENT_MANAGE);WarehouseReturn r=request(requestId);
  if(r.getStatus()!=WarehouseReturn.Status.SUPPLIER_OPEN)throw new ConflictException("SUPPLIER_RETURN_NOT_OPEN","Return request is not open for review");
  if(input==null||input.items()==null||input.items().isEmpty()||input.items().size()!=r.getItems().size())throw new BadRequestException("Review must select a purchase source for every request item");
  Map<UUID,SupplierReturnOperation> grouped=new LinkedHashMap<>();Map<UUID,BigDecimal> batchAllocated=new HashMap<>();Set<UUID> seen=new HashSet<>();
  for(ReviewLine line:input.items()){
   if(line==null||line.requestItemId()==null||!seen.add(line.requestItemId())||line.receivingOperationItemId()==null||line.purchaseOrderItemId()==null||line.approvedQuantity()==null||line.approvedQuantity().signum()<=0)throw new BadRequestException("Invalid review line");
   WarehouseReturnItem requested=item(r,line.requestItemId());if(operationItems.existsByWarehouseReturnItemId(requested.getId()))throw new ConflictException("SUPPLIER_RETURN_ALREADY_REVIEWED","Request item already allocated");
   if(line.approvedQuantity().compareTo(requested.getRequestedQuantity())>0)throw new ConflictException("SUPPLIER_RETURN_QUANTITY_EXCEEDED","Approved quantity exceeds requested quantity");
   ReceivingOperationItem received=receiving.findForUpdate(line.receivingOperationItemId()).orElseThrow(()->new NotFoundException("RECEIVING_ITEM_NOT_FOUND"));PurchaseOrderItem p=received.getReceivingRequestItem().getPurchaseOrderItem();
   if(!p.getId().equals(line.purchaseOrderItemId())||!p.getPurchaseOrder().getOrganization().getId().equals(actor.organizationId())||!p.getSupplier().getOrganization().getId().equals(actor.organizationId())||!received.getReceivingOperation().getOrganization().getId().equals(actor.organizationId())||!received.getReceivingRequestItem().getReceivingRequest().getOrganization().getId().equals(actor.organizationId())||!received.getReceivingRequestItem().getReceivingRequest().getPurchaseOrder().getId().equals(p.getPurchaseOrder().getId())||!received.getReceivingOperation().getReceivingRequest().getId().equals(received.getReceivingRequestItem().getReceivingRequest().getId())||p.getPurchaseRequestItem().getProduct()==null||!p.getPurchaseRequestItem().getProduct().getId().equals(requested.getProductId())||(received.getProduct()!=null&&!received.getProduct().getId().equals(requested.getProductId())))throw new ConflictException("SUPPLIER_RETURN_SOURCE_INVALID","Purchase and receiving source does not match request");
   if(received.getReceivingOperation().getStatus()!=ReceivingOperation.Status.COMPLETED||p.getSupplier().getStatus()!=Supplier.SupplierStatus.Active||line.approvedQuantity().compareTo(received.getAcceptedQuantity())>0||p.getQuantity().signum()<=0)throw new ConflictException("SUPPLIER_RETURN_SOURCE_INELIGIBLE","Purchase source has insufficient accepted receiving quantity");
   BigDecimal prior=operationItems.approvedForReceivingItem(received.getId());if(prior==null)prior=BigDecimal.ZERO;
   BigDecimal allocated=batchAllocated.merge(received.getId(),line.approvedQuantity(),BigDecimal::add);
   if(prior.add(allocated).compareTo(received.getAcceptedQuantity())>0)throw new ConflictException("SUPPLIER_RETURN_SOURCE_EXHAUSTED","Approved returns exceed accepted receiving history");
   SupplierReturnOperation op=grouped.computeIfAbsent(p.getSupplier().getId(),k->{SupplierReturnOperation o=new SupplierReturnOperation();o.setOrganization(r.getOrganization());o.setWarehouseReturn(r);o.setSupplier(p.getSupplier());o.setReference("SRET-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(Locale.ROOT));o.setApprovedAt(Instant.now());return o;});
   SupplierReturnOperationItem oi=new SupplierReturnOperationItem();oi.setOperation(op);oi.setWarehouseReturnItem(requested);oi.setPurchaseOrderItem(p);oi.setReceivingOperationItem(received);oi.setApprovedQuantity(line.approvedQuantity());oi.setUnitCost(p.getTotalPrice().divide(p.getQuantity(),4,RoundingMode.HALF_UP));oi.setCurrency(p.getCurrency());op.getItems().add(oi);
  }
  r.setStatus(WarehouseReturn.Status.PROCUREMENT_APPROVED);return operations.saveAll(grouped.values()).stream().map(this::view).toList();
 }
 @Transactional(readOnly=true) public Page<Operation> operations(int page,int size){actor.requirePermission(Permission.PROCUREMENT_VIEW);return operations.findAllByOrganizationId(actor.organizationId(),PageRequest.of(Math.max(0,page),Math.max(1,size))).map(this::view);}
 @Transactional(readOnly=true) public Operation operation(UUID id){actor.requirePermission(Permission.PROCUREMENT_VIEW);return view(owned(id));}
 @Transactional(readOnly=true) public Detail detail(UUID id){
  actor.requirePermission(Permission.PROCUREMENT_VIEW);SupplierReturnOperation op=owned(id);
  List<Timeline> timeline=new ArrayList<>();timeline.add(new Timeline("APPROVED","APPROVED",op.getApprovedAt(),null));
  if(op.getDispatchedAt()!=null)timeline.add(new Timeline("DISPATCHED",op.getStatus().name(),op.getDispatchedAt(),null));
  if(op.getSupplierRespondedAt()!=null)timeline.add(new Timeline("SUPPLIER_RESPONSE",op.getStatus().name(),op.getSupplierRespondedAt(),null));
  if(op.getSettlementRequestedAt()!=null)timeline.add(new Timeline("SETTLEMENT_REQUESTED",op.getStatus().name(),op.getSettlementRequestedAt(),null));
  List<DispatchEvent> events=dispatches.findAllByOperationIdOrderByDispatchedAtAsc(id).stream().flatMap(d->d.getItems().stream().map(i->new DispatchEvent(d.getReference(),d.getTrackingReference(),d.getEmployeeId(),d.getDispatchedAt(),i.getOperationItem().getId(),i.getSourceZone().getSite().getId(),i.getSourceZone().getId(),i.getQuantity()))).toList();
  BigDecimal approved=value(op,SupplierReturnOperationItem::getApprovedQuantity),dispatched=value(op,SupplierReturnOperationItem::getDispatchedQuantity),accepted=value(op,SupplierReturnOperationItem::getSupplierAcceptedQuantity),rejected=value(op,SupplierReturnOperationItem::getSupplierRejectedQuantity);
  String currency=op.getItems().stream().map(SupplierReturnOperationItem::getCurrency).distinct().count()==1?op.getItems().get(0).getCurrency():"MIXED";
  String settlementStatus=op.getFinancialRequestId()==null?op.getStatus().name():financeRequests.findByIdAndOrganizationId(op.getFinancialRequestId(),actor.organizationId()).map(f->f.getStatus().name()+"/"+f.getFulfillmentStatus().name()).orElse("FINANCE_REQUEST_MISSING");
  return new Detail(view(op),timeline,events,new FinancialSummary(currency,approved,dispatched,accepted,rejected,op.getSettlementType()==SupplierReturnOperation.SettlementType.REFUND?accepted:BigDecimal.ZERO,op.getSettlementType()==SupplierReturnOperation.SettlementType.CREDIT_NOTE?accepted:BigDecimal.ZERO,op.getSettlementType()==SupplierReturnOperation.SettlementType.REPLACEMENT?accepted:BigDecimal.ZERO,settlementStatus));
 }
 private BigDecimal value(SupplierReturnOperation op,java.util.function.Function<SupplierReturnOperationItem,BigDecimal> quantity){return op.getItems().stream().map(i->quantity.apply(i).multiply(i.getUnitCost())).reduce(BigDecimal.ZERO,BigDecimal::add);}
 @Transactional public Operation dispatch(UUID id,Dispatch input){
  actor.requirePermission(Permission.RETURN_PROCESS);SupplierReturnOperation op=operations.findForUpdate(id,actor.organizationId()).orElseThrow(()->new NotFoundException("SUPPLIER_RETURN_OPERATION_NOT_FOUND"));
  if(op.getStatus()!=SupplierReturnOperation.Status.APPROVED&&op.getStatus()!=SupplierReturnOperation.Status.PARTIALLY_DISPATCHED)throw new ConflictException("SUPPLIER_RETURN_DISPATCH_STATUS","Operation cannot be dispatched");
  if(input==null||input.reference()==null||input.reference().isBlank()||input.items()==null||input.items().isEmpty()||dispatches.existsByOperationIdAndReference(id,input.reference()))throw new ConflictException("SUPPLIER_RETURN_DUPLICATE_DISPATCH","Invalid or duplicate dispatch reference");
  SupplierReturnDispatch d=new SupplierReturnDispatch();d.setOperation(op);d.setReference(input.reference().trim());d.setTrackingReference(input.trackingReference());d.setEmployeeId(actor.employeeId());d.setDispatchedAt(Instant.now());Set<UUID> seen=new HashSet<>();
  for(DispatchLine line:input.items()){
   if(line==null||line.operationItemId()==null||line.sourceZoneId()==null||line.quantity()==null||line.quantity().signum()<=0||!seen.add(line.operationItemId()))throw new BadRequestException("Invalid dispatch line");
   SupplierReturnOperationItem i=op.getItems().stream().filter(x->x.getId().equals(line.operationItemId())).findFirst().orElseThrow(()->new NotFoundException("SUPPLIER_RETURN_OPERATION_ITEM_NOT_FOUND"));
   if(i.getDispatchedQuantity().add(line.quantity()).compareTo(i.getApprovedQuantity())>0)throw new ConflictException("SUPPLIER_RETURN_DISPATCH_QUANTITY","Dispatch exceeds approval");
   var zone=zones.findByIdAndSiteOrganizationIdAndActiveTrue(line.sourceZoneId(),actor.organizationId()).orElseThrow(()->new NotFoundException("SOURCE_ZONE_NOT_FOUND"));
   InventoryBalance balance=balances.findForUpdate(actor.organizationId(),i.getWarehouseReturnItem().getProductId(),zone.getId()).orElseThrow(()->new ConflictException("SUPPLIER_RETURN_STOCK_UNAVAILABLE","No stock in source zone"));
   if(balance.getOnHand().subtract(balance.getReserved()).compareTo(line.quantity())<0)throw new ConflictException("SUPPLIER_RETURN_STOCK_UNAVAILABLE","Insufficient available stock");
   balance.setOnHand(balance.getOnHand().subtract(line.quantity()));i.setDispatchedQuantity(i.getDispatchedQuantity().add(line.quantity()));i.getWarehouseReturnItem().setReturnedQuantity(i.getWarehouseReturnItem().getReturnedQuantity().add(line.quantity()));
   StockMovement m=new StockMovement();m.setOrganization(op.getOrganization());m.setProductId(i.getWarehouseReturnItem().getProductId());m.setFromZone(zone);m.setQuantity(line.quantity());m.setMovementType("SUPPLIER_RETURN_DISPATCH");m.setReference(op.getReference()+"/"+d.getReference());m.setNotes("Supplier return item "+i.getId());movements.save(m);
   SupplierReturnDispatchItem di=new SupplierReturnDispatchItem();di.setDispatch(d);di.setOperationItem(i);di.setSourceZone(zone);di.setQuantity(line.quantity());d.getItems().add(di);
  }
  dispatches.save(d);op.getWarehouseReturn().setStatus(WarehouseReturn.Status.PARTIALLY_RETURNED);op.setDispatchedAt(d.getDispatchedAt());op.setStatus(op.getItems().stream().allMatch(i->i.getDispatchedQuantity().compareTo(i.getApprovedQuantity())==0)?SupplierReturnOperation.Status.DISPATCHED:SupplierReturnOperation.Status.PARTIALLY_DISPATCHED);return view(op);
 }
 @Transactional public Operation acceptance(UUID id,Acceptance input){
  actor.requirePermission(Permission.PROCUREMENT_MANAGE);SupplierReturnOperation op=operations.findForUpdate(id,actor.organizationId()).orElseThrow(()->new NotFoundException("SUPPLIER_RETURN_OPERATION_NOT_FOUND"));
  if(op.getStatus()!=SupplierReturnOperation.Status.DISPATCHED&&op.getStatus()!=SupplierReturnOperation.Status.PARTIALLY_DISPATCHED&&op.getStatus()!=SupplierReturnOperation.Status.PARTIALLY_ACCEPTED)throw new ConflictException("SUPPLIER_RETURN_ACCEPTANCE_STATUS","Operation is not dispatched");
  if(input==null||input.items()==null||input.items().isEmpty())throw new BadRequestException("Acceptance items required");Set<UUID> seen=new HashSet<>();
  for(AcceptanceLine line:input.items()){
   if(line==null||line.operationItemId()==null||!seen.add(line.operationItemId())||line.acceptedQuantity()==null||line.rejectedQuantity()==null||line.acceptedQuantity().signum()<0||line.rejectedQuantity().signum()<0)throw new BadRequestException("Invalid supplier quantities");
   SupplierReturnOperationItem i=op.getItems().stream().filter(x->x.getId().equals(line.operationItemId())).findFirst().orElseThrow(()->new NotFoundException("SUPPLIER_RETURN_OPERATION_ITEM_NOT_FOUND"));
   if(line.acceptedQuantity().add(line.rejectedQuantity()).compareTo(i.getDispatchedQuantity())>0)throw new ConflictException("SUPPLIER_RETURN_ACCEPTANCE_QUANTITY","Supplier quantities exceed dispatch");
   i.setSupplierAcceptedQuantity(line.acceptedQuantity());i.setSupplierRejectedQuantity(line.rejectedQuantity());
  }
  op.setSupplierRespondedAt(Instant.now());op.setStatus(op.getItems().stream().allMatch(i->i.getSupplierAcceptedQuantity().add(i.getSupplierRejectedQuantity()).compareTo(i.getDispatchedQuantity())==0)?SupplierReturnOperation.Status.ACCEPTED:SupplierReturnOperation.Status.PARTIALLY_ACCEPTED);return view(op);
 }
 @Transactional public Operation settlement(UUID id,Settlement input){
  actor.requirePermission(Permission.PROCUREMENT_MANAGE);SupplierReturnOperation op=operations.findForUpdate(id,actor.organizationId()).orElseThrow(()->new NotFoundException("SUPPLIER_RETURN_OPERATION_NOT_FOUND"));
  if(op.getSettlementType()!=null)return view(op);
  if(op.getStatus()!=SupplierReturnOperation.Status.ACCEPTED)throw new ConflictException("SUPPLIER_RETURN_SETTLEMENT_STATUS","Resolve all dispatched quantities before settlement");
  if(input==null||input.type()==null)throw new BadRequestException("Settlement type required");
  SupplierReturnOperation.SettlementType type;try{type=SupplierReturnOperation.SettlementType.valueOf(input.type().toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){throw new BadRequestException("Invalid settlement type");}
  BigDecimal amount=op.getItems().stream().map(i->i.getSupplierAcceptedQuantity().multiply(i.getUnitCost())).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
  if(amount.signum()<=0)throw new ConflictException("SUPPLIER_RETURN_NO_ACCEPTANCE","No accepted quantity to settle");
  if(op.getItems().stream().map(SupplierReturnOperationItem::getCurrency).distinct().count()!=1)throw new ConflictException("SUPPLIER_RETURN_MIXED_CURRENCY","Split mixed-currency items before settlement");
  if(type==SupplierReturnOperation.SettlementType.REFUND){
   Map<String,Object> result=finance.submitSupplierReturnRefund(op.getId(),amount,op.getItems().get(0).getCurrency(),op.getReference(),input.notes(),input.reference());
   op.setFinancialRequestId((UUID)result.get("id"));
  }
  op.setSettlementType(type);op.setSettlementReference(input.reference());op.setSettlementNotes(input.notes());op.setSettlementRequestedAt(Instant.now());op.setStatus(SupplierReturnOperation.Status.SETTLEMENT_PENDING);return view(op);
 }
 private SupplierReturnOperation owned(UUID id){return operations.findByIdAndOrganizationId(id,actor.organizationId()).orElseThrow(()->new NotFoundException("SUPPLIER_RETURN_OPERATION_NOT_FOUND"));}
 private Operation view(SupplierReturnOperation o){return new Operation(o.getId(),o.getReference(),o.getWarehouseReturn().getId(),o.getWarehouseReturn().getReference(),o.getWarehouseReturn().getReason(),o.getSupplier().getId(),o.getSupplier().getName(),o.getStatus().name(),o.getSettlementType()==null?null:o.getSettlementType().name(),o.getSettlementReference(),o.getFinancialRequestId(),o.getItems().stream().map(i->new Line(i.getId(),i.getWarehouseReturnItem().getId(),i.getWarehouseReturnItem().getProductId(),i.getPurchaseOrderItem().getPurchaseRequestItem().getSku(),i.getPurchaseOrderItem().getId(),i.getPurchaseOrderItem().getPurchaseOrder().getNumber(),i.getReceivingOperationItem().getReceivingOperation().getId(),i.getReceivingOperationItem().getReceivingRequestItem().getReceivingRequest().getRequestNumber(),i.getReceivingOperationItem().getId(),i.getApprovedQuantity(),i.getDispatchedQuantity(),i.getSupplierAcceptedQuantity(),i.getSupplierRejectedQuantity(),i.getUnitCost(),i.getCurrency())).toList());}
}
