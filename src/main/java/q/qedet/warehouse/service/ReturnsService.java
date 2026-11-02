package q.qedet.warehouse.service;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.finance.entity.FinanceRequest;
import q.qedet.finance.repository.FinanceRequestRepository;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.organization.repository.OrganizationZoneRepository;
import q.qedet.organization.entity.OrganizationZone;
import q.qedet.product.entity.*;
import q.qedet.product.repository.*;
import q.qedet.qr.dto.QrCodeResponse;
import q.qedet.qr.entity.QrTargetKind;
import q.qedet.qr.service.QrCodeService;
import q.qedet.warehouse.dto.ReturnDtos;
import q.qedet.warehouse.entity.*;
import q.qedet.warehouse.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class ReturnsService {
 private final ActorContext actor; private final WarehouseReturnRepository returns; private final WarehouseReturnItemRepository items;
 private final OrganizationRepository organizations; private final ProductRepository products; private final OrganizationProductRepository organizationProducts;
 private final InventoryBalanceRepository balances; private final StockMovementRepository movements; private final OrganizationZoneRepository zones; private final QrCodeService qrCodes; private final FinanceRequestRepository financeRequests; private final EmployeeRepository employees;
 public ReturnsService(ActorContext actor, WarehouseReturnRepository returns, WarehouseReturnItemRepository items, OrganizationRepository organizations, ProductRepository products, OrganizationProductRepository organizationProducts, InventoryBalanceRepository balances, StockMovementRepository movements, OrganizationZoneRepository zones, QrCodeService qrCodes, FinanceRequestRepository financeRequests, EmployeeRepository employees) {
  this.actor=actor; this.returns=returns; this.items=items; this.organizations=organizations; this.products=products; this.organizationProducts=organizationProducts; this.balances=balances; this.movements=movements; this.zones=zones; this.qrCodes=qrCodes; this.financeRequests=financeRequests; this.employees=employees;
 }
 @Transactional(readOnly=true) public ReturnDtos.PageResponse list(String type,String status,int page,int size) {
  if(page<0||size<1||size>100) throw new BadRequestException("RETURN_INVALID_PAGE", "page must be non-negative and size must be between 1 and 100");
  WarehouseReturn.Type parsedType=enumValue(type,WarehouseReturn.Type.class,"RETURN_INVALID_TYPE"); WarehouseReturn.Status parsedStatus=enumValue(status,WarehouseReturn.Status.class,"RETURN_INVALID_STATUS");
  Pageable pageable=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt")); UUID org=actor.organizationId();
  Page<WarehouseReturn> result=parsedType!=null&&parsedStatus!=null?returns.findAllByOrganizationIdAndTypeAndStatus(org,parsedType,parsedStatus,pageable):parsedType!=null?returns.findAllByOrganizationIdAndType(org,parsedType,pageable):parsedStatus!=null?returns.findAllByOrganizationIdAndStatus(org,parsedStatus,pageable):returns.findAllByOrganizationId(org,pageable);
  return new ReturnDtos.PageResponse(result.map(this::response).toList(),result.getTotalElements(),page,size);
 }
 @Transactional(readOnly=true) public ReturnDtos.Summary summary(){ UUID o=actor.organizationId(); return new ReturnDtos.Summary(returns.countByOrganizationIdAndStatus(o,WarehouseReturn.Status.PENDING_RECEIPT),returns.countByOrganizationIdAndStatus(o,WarehouseReturn.Status.PENDING_INSPECTION),returns.countByOrganizationIdAndStatus(o,WarehouseReturn.Status.ACCEPTED),returns.countByOrganizationIdAndType(o,WarehouseReturn.Type.SUPPLIER),returns.countByOrganizationIdAndStatus(o,WarehouseReturn.Status.QUARANTINE)); }
 @Transactional(readOnly=true) public ReturnDtos.ReturnResponse get(UUID id){ return response(owned(id)); }
 @Transactional public ReturnDtos.ReturnResponse customer(ReturnDtos.CustomerCreate request){ return create(WarehouseReturn.Type.CUSTOMER,request.reason(),request.salesOrderReference(),request.receiptReference(),request.items(),null); }
 @Transactional public ReturnDtos.ReturnResponse supplier(ReturnDtos.SupplierCreate request){ return create(WarehouseReturn.Type.SUPPLIER,request.reason(),null,null,request.items(),null); }
 @Transactional public ReturnDtos.ReturnResponse damage(ReturnDtos.DamageCreate request){ return create(WarehouseReturn.Type.DAMAGE,request.reason(),null,null,request.items(),request.siteZoneId()); }
 @Transactional(readOnly=true) public ReturnDtos.ScanResponse scan(ReturnDtos.ScanRequest request) {
  Product product=productForQr(request.qrReference()); WarehouseReturn current=request.returnId()==null?null:owned(request.returnId());
  boolean matches=current==null||current.getItems().stream().anyMatch(i->i.getProductId().equals(product.getId()) && (i.getQrReference()==null||i.getQrReference().equals(request.qrReference())));
  OrganizationProduct op=organizationProducts.findByOrganizationIdAndProductId(actor.organizationId(),product.getId()).orElse(null);
  return new ReturnDtos.ScanResponse(true,matches,product.getId(),product.getName(),op==null?null:op.getOrganizationSku(),product.getProductType().name(),product.getUnit()==null?null:product.getUnit().getSymbol(),available(product.getId()),null,null,null);
 }
 @Transactional(readOnly=true) public ReturnDtos.ReceiptValidationResponse validateReceipt(ReturnDtos.ReceiptValidationRequest request) {
  throw new ConflictException("SALES_RECEIPT_VALIDATION_UNAVAILABLE", "Sales receipts and sales-order lines are not persisted by this service; link the sales receipt domain before customer-return validation can be performed");
 }
 @Transactional public ReturnDtos.ReceivingResponse startReceiving(UUID id) { WarehouseReturn r=owned(id); if(r.getType()!=WarehouseReturn.Type.CUSTOMER&&r.getType()!=WarehouseReturn.Type.SALES_HALL) throw new ConflictException("RETURN_RECEIVING_NOT_APPLICABLE","Only customer and Sales Hall returns have inbound receiving"); if(r.getStatus()!=WarehouseReturn.Status.PENDING_RECEIPT&&r.getStatus()!=WarehouseReturn.Status.RECEIVED) throw new ConflictException("RETURN_RECEIVING_NOT_ALLOWED","Return is not awaiting receipt"); if(r.getReceivingOperationId()==null) r.setReceivingOperationId(UUID.randomUUID()); returns.save(r); return new ReturnDtos.ReceivingResponse(r.getReceivingOperationId(),r.getStatus().name()); }
 @Transactional public ReturnDtos.ReturnResponse receiveScan(UUID id,ReturnDtos.ReceivingScanRequest request) {
  WarehouseReturn r=owned(id); if(r.getReceivingOperationId()==null) throw new ConflictException("RETURN_RECEIVING_NOT_STARTED","Start receiving before scanning items"); Product p=productForQr(request.qrReference()); WarehouseReturnItem item=r.getItems().stream().filter(i->i.getProductId().equals(p.getId())&&(i.getQrReference()==null||i.getQrReference().equals(request.qrReference()))).findFirst().orElseThrow(()->new ConflictException("RETURN_UNEXPECTED_ITEM","Scanned item is not expected by this return"));
  if(item.getReceivedQuantity().add(request.quantity()).compareTo(item.getRequestedQuantity())>0) throw new ConflictException("RETURN_RECEIVED_QUANTITY_EXCEEDED","Received quantity exceeds requested quantity");
  item.setReceivedQuantity(item.getReceivedQuantity().add(request.quantity())); if(request.notes()!=null)item.setHandlingNotes(request.notes()); item.setStatus("RECEIVED"); r.setStatus(WarehouseReturn.Status.RECEIVED); returns.save(r); return response(r);
 }
 @Transactional public ReturnDtos.ReturnResponse startInspection(UUID id) { WarehouseReturn r=owned(id); if(r.getStatus()!=WarehouseReturn.Status.RECEIVED&&r.getType()!=WarehouseReturn.Type.DAMAGE) throw new ConflictException("RETURN_INSPECTION_NOT_ALLOWED","Return must be received before inspection"); r.setStatus(WarehouseReturn.Status.PENDING_INSPECTION); r.getItems().forEach(i->{if(i.getReceivedQuantity().signum()>0||r.getType()==WarehouseReturn.Type.DAMAGE)i.setStatus("PENDING_INSPECTION");}); return response(returns.save(r)); }
 @Transactional public ReturnDtos.ReturnResponse decision(UUID id,ReturnDtos.DecisionRequest request) {
  WarehouseReturn r=owned(id); if(r.getStatus()!=WarehouseReturn.Status.PENDING_INSPECTION) throw new ConflictException("RETURN_INSPECTION_NOT_STARTED","Start inspection before submitting a decision"); WarehouseReturnItem item=items.findForUpdateByIdAndOrganizationId(request.itemId(),actor.organizationId()).filter(i->i.getWarehouseReturn().getId().equals(id)).orElseThrow(()->new NotFoundException("RETURN_ITEM_NOT_FOUND"));
  OrganizationZone inspectionZone=activeZone(request.siteZoneId()); if(!inspectionZone.getId().equals(item.getInspectionLocationId())) throw new ConflictException("RETURN_INSPECTION_ZONE_MISMATCH","Inspection decisions must use the item's inspection SiteZone");
  String decision=request.decision().trim().toUpperCase(Locale.ROOT); if(!Set.of("ACCEPT","REJECT","QUARANTINE").contains(decision))throw new BadRequestException("RETURN_INVALID_DECISION","decision must be ACCEPT, REJECT, or QUARANTINE"); String decisionKey=decisionKey(decision,request.quantity(),inspectionZone.getId(),request.notes());
  if(decisionKey.equals(item.getLastInspectionDecisionKey())) return response(r);
  BigDecimal maximum=r.getType()==WarehouseReturn.Type.DAMAGE?item.getRequestedQuantity():item.getReceivedQuantity(); if(request.quantity().compareTo(maximum.subtract(item.getInspectionQuantity()))>0) throw new ConflictException("RETURN_INSPECTION_QUANTITY_EXCEEDED","Decision quantity exceeds the outstanding inspection quantity");
  if(r.getType()==WarehouseReturn.Type.DAMAGE) applyDamageDecision(r,item,inspectionZone,decision,request.quantity(),request.notes());
  item.setInspectionQuantity(item.getInspectionQuantity().add(request.quantity())); item.setHandlingNotes(request.notes()); item.setStatus(decision); item.setLastInspectionDecisionKey(decisionKey); if("ACCEPT".equals(decision))item.setAcceptedQuantity(item.getAcceptedQuantity().add(request.quantity()));
  updateInspectionStatus(r); return response(returns.save(r));
 }
 @Transactional public ReturnDtos.FinancialResponse financial(UUID id,ReturnDtos.FinancialRequest input) {
  WarehouseReturn r=owned(id); if(r.getType()!=WarehouseReturn.Type.CUSTOMER||r.getStatus()!=WarehouseReturn.Status.ACCEPTED)throw new ConflictException("RETURN_REFUND_NOT_ALLOWED","A customer return must be accepted before requesting a refund"); if(r.getFinancialRequestId()!=null){FinanceRequest existing=financeRequests.findByIdAndOrganizationId(r.getFinancialRequestId(),actor.organizationId()).orElseThrow(()->new NotFoundException("FINANCIAL_REQUEST_NOT_FOUND"));return financialResponse(existing);}
  throw new ConflictException("RETURN_REFUND_AMOUNT_UNAVAILABLE","Refund amounts must be calculated from a persisted sales receipt before a financial request can be created");
 }
 @Transactional public ReturnDtos.ReturnResponse supplierInspection(UUID returnId,UUID itemId,ReturnDtos.SupplierInspectionRequest request) { WarehouseReturn r=owned(returnId); if(r.getType()!=WarehouseReturn.Type.SUPPLIER)throw new ConflictException("RETURN_NOT_SUPPLIER","Inspection requests after procurement closure apply only to supplier returns"); OrganizationZone zone=activeZone(request.siteZoneId()); WarehouseReturnItem item=items.findByIdAndWarehouseReturnOrganizationId(itemId,actor.organizationId()).filter(i->i.getWarehouseReturn().getId().equals(returnId)).orElseThrow(()->new NotFoundException("RETURN_ITEM_NOT_FOUND")); BigDecimal outstanding=item.getRequestedQuantity().subtract(item.getReturnedQuantity()).subtract(item.getInspectionQuantity());if(request.quantity().compareTo(outstanding)>0)throw new ConflictException("RETURN_INSPECTION_QUANTITY_EXCEEDED","Inspection quantity exceeds the outstanding supplier-return quantity");item.setInspectionQuantity(item.getInspectionQuantity().add(request.quantity()));item.setInspectionLocationId(zone.getId());item.setStatus("PENDING_INSPECTION");r.setStatus(WarehouseReturn.Status.PENDING_INSPECTION);return response(returns.save(r)); }
 private ReturnDtos.ReturnResponse create(WarehouseReturn.Type type,String reason,String order,String receipt,List<ReturnDtos.ItemInput> inputs,UUID inspectionLocation){ WarehouseReturn r=new WarehouseReturn();r.setOrganization(organizations.findById(actor.organizationId()).orElseThrow(()->new NotFoundException("ORGANIZATION_NOT_FOUND")));r.setReference("RET-"+LocalDate.now(ZoneOffset.UTC).toString().replace("-","")+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT));r.setType(type);r.setStatus(type==WarehouseReturn.Type.DAMAGE?WarehouseReturn.Status.PENDING_INSPECTION:type==WarehouseReturn.Type.SUPPLIER?WarehouseReturn.Status.SUPPLIER_OPEN:WarehouseReturn.Status.PENDING_RECEIPT);r.setReason(reason.trim());r.setSalesOrderReference(order);r.setReceiptReference(receipt);for(ReturnDtos.ItemInput input:inputs){Product p=product(input);WarehouseReturnItem item=new WarehouseReturnItem();item.setWarehouseReturn(r);item.setProductId(p.getId());item.setQrReference(input.qrReference());item.setRequestedQuantity(input.quantity());item.setConditionCode(input.condition());item.setHandlingNotes(input.handlingNotes());item.setInspectionLocationId(inspectionLocation);r.getItems().add(item);} WarehouseReturn saved=returns.save(r); if(type==WarehouseReturn.Type.DAMAGE) reserveDamageHold(saved,inspectionLocation); return response(saved); }
 private void reserveDamageHold(WarehouseReturn r, UUID siteZoneId) {
  OrganizationZone inspectionZone=activeZone(siteZoneId);
  for(WarehouseReturnItem item:r.getItems()) {
   List<InventoryBalance> locked=balances.findAllForUpdateByOrganizationIdAndProductId(actor.organizationId(),item.getProductId());
   List<InventoryBalance> eligible=locked.stream().filter(b->zero(b.getOnHand()).subtract(zero(b.getReserved())).compareTo(item.getRequestedQuantity())>=0).toList();
   if(eligible.isEmpty()) { if(locked.isEmpty()) throw new ConflictException("RETURN_INVENTORY_NOT_FOUND","No eligible inventory exists for the damaged product"); throw new ConflictException("RETURN_INSUFFICIENT_AVAILABLE_QUANTITY","Damage quantity exceeds available inventory"); }
   if(eligible.size()>1) throw new ConflictException("RETURN_SOURCE_INVENTORY_AMBIGUOUS","More than one SiteZone can supply this damage request; select the source SiteZone before retrying");
   InventoryBalance source=eligible.get(0); item.setSourceSiteZoneId(source.getZone().getId());
   if(source.getZone().getId().equals(inspectionZone.getId())) { source.setReserved(zero(source.getReserved()).add(item.getRequestedQuantity())); balances.save(source); continue; }
   source.setOnHand(zero(source.getOnHand()).subtract(item.getRequestedQuantity())); balances.save(source);
   InventoryBalance destination=locked.stream().filter(b->b.getZone().getId().equals(inspectionZone.getId())).findFirst().orElseGet(()->newBalance(r.getOrganization(),item.getProductId(),inspectionZone));
   destination.setOnHand(zero(destination.getOnHand()).add(item.getRequestedQuantity())); destination.setReserved(zero(destination.getReserved()).add(item.getRequestedQuantity())); balances.save(destination);
   movement(r,item.getProductId(),source.getZone(),inspectionZone,item.getRequestedQuantity(),"DAMAGE_TO_INSPECTION",null);
  }
 }
 private void applyDamageDecision(WarehouseReturn r, WarehouseReturnItem item, OrganizationZone inspectionZone, String decision, BigDecimal quantity, String notes) {
  if(item.getSourceSiteZoneId()==null) throw new ConflictException("RETURN_DAMAGE_SOURCE_MISSING","Damage item has no recorded source SiteZone");
  List<InventoryBalance> locked=balances.findAllForUpdateByOrganizationIdAndProductId(actor.organizationId(),item.getProductId());
  InventoryBalance inspection=locked.stream().filter(b->b.getZone().getId().equals(inspectionZone.getId())).findFirst().orElseThrow(()->new ConflictException("RETURN_INSPECTION_INVENTORY_NOT_FOUND","The damage hold is missing from the inspection SiteZone"));
  if(zero(inspection.getReserved()).compareTo(quantity)<0 || zero(inspection.getOnHand()).compareTo(quantity)<0) throw new ConflictException("RETURN_DAMAGE_HOLD_UNAVAILABLE","The requested inspection quantity is no longer held");
  if("ACCEPT".equals(decision)) {
   inspection.setReserved(zero(inspection.getReserved()).subtract(quantity)); inspection.setOnHand(zero(inspection.getOnHand()).subtract(quantity)); balances.save(inspection);
   movement(r,item.getProductId(),inspectionZone,null,quantity,"DAMAGE_WRITEOFF",notes);
  } else if("REJECT".equals(decision)) {
   OrganizationZone sourceZone=ownedZone(item.getSourceSiteZoneId());
   inspection.setReserved(zero(inspection.getReserved()).subtract(quantity)); inspection.setOnHand(zero(inspection.getOnHand()).subtract(quantity)); balances.save(inspection);
   InventoryBalance source=locked.stream().filter(b->b.getZone().getId().equals(sourceZone.getId())).findFirst().orElseGet(()->newBalance(r.getOrganization(),item.getProductId(),sourceZone));
   source.setOnHand(zero(source.getOnHand()).add(quantity)); balances.save(source);
   movement(r,item.getProductId(),inspectionZone,sourceZone,quantity,"DAMAGE_REJECTED_RETURN",notes);
  } else {
   // The reservation stays in the inspection SiteZone, keeping quarantined stock unavailable to normal picking.
   movement(r,item.getProductId(),inspectionZone,inspectionZone,quantity,"DAMAGE_QUARANTINE",notes);
  }
 }
 private void updateInspectionStatus(WarehouseReturn r) {
  boolean complete=r.getItems().stream().allMatch(i->i.getInspectionQuantity().compareTo(r.getType()==WarehouseReturn.Type.DAMAGE?i.getRequestedQuantity():i.getReceivedQuantity())>=0);
  if(!complete) { r.setStatus(WarehouseReturn.Status.PENDING_INSPECTION); return; }
  boolean quarantined=r.getItems().stream().anyMatch(i->"QUARANTINE".equals(i.getStatus()));
  boolean rejected=r.getItems().stream().anyMatch(i->"REJECT".equals(i.getStatus()));
  r.setStatus(quarantined?WarehouseReturn.Status.QUARANTINE:rejected?WarehouseReturn.Status.REJECTED:WarehouseReturn.Status.ACCEPTED);
 }
 private OrganizationZone activeZone(UUID siteZoneId) {
  if(siteZoneId==null) throw new ConflictException("RETURN_INVALID_INSPECTION_SITE_ZONE","An active inspection SiteZone is required");
  return zones.findByIdAndSiteOrganizationIdAndActiveTrue(siteZoneId,actor.organizationId()).orElseThrow(()->new ConflictException("RETURN_INVALID_INSPECTION_SITE_ZONE","Inspection SiteZone must be active and belong to the authenticated organization"));
 }
 private OrganizationZone ownedZone(UUID siteZoneId) {
  return zones.findByIdAndSiteOrganizationId(siteZoneId,actor.organizationId()).orElseThrow(()->new ConflictException("RETURN_DAMAGE_SOURCE_INVALID","Recorded damage source SiteZone no longer belongs to the authenticated organization"));
 }
 private InventoryBalance newBalance(q.qedet.organization.entity.Organization organization, UUID productId, OrganizationZone zone) {
  InventoryBalance balance=new InventoryBalance(); balance.setOrganization(organization); balance.setProductId(productId); balance.setZone(zone); balance.setOnHand(BigDecimal.ZERO); balance.setReserved(BigDecimal.ZERO); return balance;
 }
 private void movement(WarehouseReturn r, UUID productId, OrganizationZone from, OrganizationZone to, BigDecimal quantity, String type, String notes) {
  StockMovement movement=new StockMovement(); movement.setOrganization(r.getOrganization()); movement.setProductId(productId); movement.setFromZone(from); movement.setToZone(to); movement.setQuantity(quantity); movement.setMovementType(type); movement.setReference("WAREHOUSE_RETURN:"+r.getId()); movement.setNotes(notes); movements.save(movement);
 }
 private String decisionKey(String decision, BigDecimal quantity, UUID siteZoneId, String notes) { return decision+"|"+quantity.stripTrailingZeros().toPlainString()+"|"+siteZoneId+"|"+(notes==null?"":notes.trim()); }
 private Product product(ReturnDtos.ItemInput input){if(input.productId()!=null)return products.findByIdAndOrganizationId(input.productId(),actor.organizationId()).orElseThrow(()->new NotFoundException("PRODUCT_NOT_FOUND"));if(input.qrReference()!=null&&!input.qrReference().isBlank())return productForQr(input.qrReference());throw new BadRequestException("RETURN_PRODUCT_REQUIRED","Each return item requires productId or qrReference");}
 private Product productForQr(String code){QrCodeResponse qr=qrCodes.resolveInOrganization(actor.organizationId(),code);if(qr.targetKind()!=QrTargetKind.PRODUCT||qr.referenceId()==null)throw new ConflictException("RETURN_QR_NOT_PRODUCT","QR code does not identify a product");return products.findByIdAndOrganizationId(qr.referenceId(),actor.organizationId()).orElseThrow(()->new NotFoundException("PRODUCT_NOT_FOUND"));}
 private WarehouseReturn owned(UUID id){return returns.findByIdAndOrganizationId(id,actor.organizationId()).orElseThrow(()->new NotFoundException("RETURN_NOT_FOUND"));}
 private BigDecimal available(UUID productId){return balances.findAllByOrganizationIdAndProductId(actor.organizationId(),productId).stream().map(b->zero(b.getOnHand()).subtract(zero(b.getReserved()))).reduce(BigDecimal.ZERO,BigDecimal::add);}
 private ReturnDtos.ReturnResponse response(WarehouseReturn r){return new ReturnDtos.ReturnResponse(r.getId(),r.getReference(),r.getType().name(),r.getStatus().name(),r.getReason(),r.getSalesOrderReference(),r.getReceiptReference(),r.getReceivingOperationId(),r.getFinancialRequestId(),r.getCreatedAt(),r.getItems().stream().map(i->new ReturnDtos.ItemResponse(i.getId(),i.getProductId(),i.getQrReference(),i.getRequestedQuantity(),i.getReceivedQuantity(),i.getReturnedQuantity(),i.getInspectionQuantity(),i.getAcceptedQuantity(),i.getStatus(),i.getInspectionLocationId(),i.getConditionCode(),i.getHandlingNotes())).toList());}
 private ReturnDtos.FinancialResponse financialResponse(FinanceRequest request){return new ReturnDtos.FinancialResponse(request.getId(),request.getRequestNumber(),request.getAmount(),request.getCurrency(),request.getStatus().name());}
 private BigDecimal zero(BigDecimal value){return value==null?BigDecimal.ZERO:value;}
 private <T extends Enum<T>> T enumValue(String raw,Class<T> type,String code){if(raw==null||raw.isBlank())return null;try{return Enum.valueOf(type,raw.trim().toUpperCase(Locale.ROOT));}catch(IllegalArgumentException e){throw new BadRequestException(code,"Unsupported "+type.getSimpleName()+": "+raw);}}
}
