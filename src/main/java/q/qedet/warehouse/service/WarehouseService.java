package q.qedet.warehouse.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.model.LocationStatus;
import q.qedet.common.model.LocationType;
import q.qedet.organization.entity.*;
import q.qedet.organization.repository.*;
import q.qedet.product.entity.Product;
import q.qedet.warehouse.dto.WarehouseDtos;
import q.qedet.warehouse.entity.*;
import q.qedet.warehouse.repository.InventoryBalanceRepository;
import q.qedet.warehouse.repository.StockMovementRepository;
import q.qedet.warehouse.repository.ReceivingOperationZoneAssignmentRepository;
import java.math.BigDecimal;
import java.util.*;

/** Canonical inventory topology: Organization -> OrganizationSite -> OrganizationZone. */
@Service
public class WarehouseService {
 private final ActorContext actor; private final OrganizationRepository organizations; private final OrganizationSiteRepository sites; private final OrganizationZoneRepository zones; private final ReceivingOperationZoneAssignmentRepository assignments; private final InventoryBalanceRepository balances; private final StockMovementRepository movements;
 @Autowired public WarehouseService(ActorContext actor, OrganizationRepository organizations, OrganizationSiteRepository sites, OrganizationZoneRepository zones, ReceivingOperationZoneAssignmentRepository assignments, InventoryBalanceRepository balances, StockMovementRepository movements){this.actor=actor;this.organizations=organizations;this.sites=sites;this.zones=zones;this.assignments=assignments;this.balances=balances;this.movements=movements;}

 /** The former warehouseId API field is now an OrganizationSite id. */

 @Transactional(readOnly=true) public List<WarehouseDtos.InventoryResponse> inventory(){return balances.findInventoryResponsesByOrganizationId(actor.organizationId());}
 @Transactional public void applyCompletedReceivingOperation(ReceivingOperation operation){
  UUID orgId=actor.organizationId(); if(operation.isInventoryApplied())return;
  if(operation.getOrganization()==null||!orgId.equals(operation.getOrganization().getId()))throw new ConflictException("RECEIVING_ORGANIZATION_MISMATCH","Receiving operation does not belong to the authenticated organization");
  UUID destinationSiteId=operation.getReceivingRequest().getDestinationLocation()==null?null:operation.getReceivingRequest().getDestinationLocation().getId();
  OrganizationSite site=destinationSiteId==null?null:sites.findByIdAndOrganizationId(destinationSiteId,orgId).filter(candidate->candidate.getType()==LocationType.WAREHOUSE&&candidate.getStatus()==LocationStatus.ACTIVE).orElse(null);
  if(site==null)throw new ConflictException("RECEIVING_WAREHOUSE_SITE_REQUIRED","An active Warehouse site belonging to this receiving request is required");
  for(ReceivingOperationItem item:operation.getItems()){
   BigDecimal accepted=zero(item.getAcceptedQuantity());if(accepted.signum()==0)continue;Product product=item.getProduct();if(product==null)throw new ConflictException("RECEIVING_PRODUCT_REQUIRED","Receiving operation item has no canonical product");
   List<ReceivingOperationZoneAssignment> rows=assignments.findAllByReceivingOperationItemIdOrderByCreatedAtAsc(item.getId());validateAssignments(orgId,site,rows,accepted);
   for(ReceivingOperationZoneAssignment row:rows){OrganizationZone zone=row.getZone();BigDecimal quantity=row.getAcceptedQuantity();InventoryBalance balance=balances.findForUpdate(orgId,product.getId(),zone.getId()).orElseGet(()->newBalance(operation.getOrganization(),product.getId(),zone));balance.setOnHand(zero(balance.getOnHand()).add(quantity));balances.save(balance);StockMovement movement=new StockMovement();movement.setOrganization(operation.getOrganization());movement.setProductId(product.getId());movement.setToZone(zone);movement.setQuantity(quantity);movement.setMovementType("RECEIVING_STAGING");movement.setReference("RECEIVING_OPERATION:"+operation.getId());movements.save(movement);}
  } operation.setInventoryApplied(true);
 }
 private void validateAssignments(UUID orgId,OrganizationSite site,List<ReceivingOperationZoneAssignment> rows,BigDecimal accepted){
  if(rows.isEmpty())throw new ConflictException("RECEIVING_ZONE_ASSIGNMENT_REQUIRED","Accepted quantity requires destination zone assignments");Set<UUID> ids=new HashSet<>();BigDecimal total=BigDecimal.ZERO;
  for(ReceivingOperationZoneAssignment row:rows){OrganizationZone persisted=row.getZone()==null?null:zones.findByIdAndSiteId(row.getZone().getId(),site.getId()).filter(OrganizationZone::isActive).orElse(null);if(persisted==null||!persisted.getSite().getOrganization().getId().equals(orgId)||!ids.add(persisted.getId()))throw new ConflictException("RECEIVING_INVALID_DESTINATION_ZONE","Receiving assignment has an invalid destination zone");if(zero(row.getAcceptedQuantity()).signum()<=0)throw new ConflictException("RECEIVING_INVALID_ZONE_QUANTITY","Receiving assignment quantity must be greater than zero");row.setZone(persisted);total=total.add(row.getAcceptedQuantity());}
  if(total.compareTo(accepted)!=0)throw new ConflictException("RECEIVING_ZONE_ASSIGNMENT_MISMATCH","Zone assignment quantities must equal accepted quantity");
 }
 private OrganizationZone ensureDefaultZone(OrganizationSite site){return zones.findBySiteIdAndActiveTrueAndIsDefaultTrue(site.getId()).orElseGet(()->{OrganizationZone zone=zones.findBySiteIdAndCodeIgnoreCase(site.getId(),"DEFAULT").orElseGet(()->new OrganizationZone(site,"DEFAULT","Default"));zone.setActive(true);zone.setDefault(true);return zones.save(zone);});}
 private InventoryBalance newBalance(Organization org,UUID productId,OrganizationZone zone){InventoryBalance value=new InventoryBalance();value.setOrganization(org);value.setProductId(productId);value.setZone(zone);value.setOnHand(BigDecimal.ZERO);value.setReserved(BigDecimal.ZERO);return value;}
 private String required(String value,String label){if(value==null||value.isBlank())throw new ConflictException("WAREHOUSE_VALIDATION",label+" is required");return value.trim();} private BigDecimal zero(BigDecimal value){return value==null?BigDecimal.ZERO:value;}
}
