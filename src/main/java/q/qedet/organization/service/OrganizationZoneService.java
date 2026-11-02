package q.qedet.organization.service;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import q.qedet.auth.security.ActorContext; import q.qedet.common.exception.*; import q.qedet.organization.dto.OrganizationZoneRequest; import q.qedet.organization.entity.*; import q.qedet.organization.repository.*; import java.util.*; import static q.qedet.common.model.Permission.LOCATION_MANAGE;
import static q.qedet.common.model.Permission.LOCATION_VIEW;

@Service public class OrganizationZoneService {
 private final OrganizationSiteRepository sites; private final OrganizationZoneRepository zones; private final ActorContext actor;
 public OrganizationZoneService(OrganizationSiteRepository sites,OrganizationZoneRepository zones,ActorContext actor){this.sites=sites;this.zones=zones;this.actor=actor;}
 @Transactional public OrganizationZone create(UUID organizationId,UUID siteId,OrganizationZoneRequest request){tenant(organizationId);actor.requirePermission(LOCATION_MANAGE);OrganizationSite site=sites.findByIdAndOrganizationId(siteId,organizationId).orElseThrow(()->new NotFoundException("Site not found"));String code=req(request.code());if(zones.findBySiteIdAndCodeIgnoreCase(siteId,code).isPresent())throw new ConflictException("Zone code already exists in this site");OrganizationZone zone=new OrganizationZone(site,code,req(request.name()));apply(zone,request);if(Boolean.TRUE.equals(request.isDefault())) setDefault(site,zone);return zones.save(zone);}

 @Transactional(readOnly = true)
 public List<OrganizationZone> list(UUID organizationId, UUID siteId) {
  tenant(organizationId);
  actor.requirePermission(LOCATION_VIEW);

  OrganizationSite site = sites
          .findByIdAndOrganizationId(siteId, organizationId)
          .orElseThrow(() -> new NotFoundException("Site not found"));

  return zones.findAllBySiteIdOrderByNameAsc(site.getId())
          .stream()
          .filter(OrganizationZone::isActive)
          .toList();
 }
 @Transactional public OrganizationZone update(UUID organizationId,UUID siteId,UUID id,OrganizationZoneRequest request){tenant(organizationId);actor.requirePermission(LOCATION_MANAGE);OrganizationZone zone=zones.findByIdAndSiteId(id,siteId).filter(z->z.getSite().getOrganization().getId().equals(organizationId)).orElseThrow(()->new NotFoundException("Zone not found"));String code=req(request.code());zones.findBySiteIdAndCodeIgnoreCase(siteId,code).filter(z->!z.getId().equals(id)).ifPresent(z->{throw new ConflictException("Zone code already exists in this site");});zone.setCode(code);zone.setName(req(request.name()));apply(zone,request);if(Boolean.TRUE.equals(request.isDefault()))setDefault(zone.getSite(),zone);return zones.save(zone);}
 private void setDefault(OrganizationSite site,OrganizationZone chosen){if(!chosen.isActive())throw new ConflictException("Default zone must be active");zones.findAllBySiteIdOrderByNameAsc(site.getId()).forEach(z->{z.setDefault(z.getId().equals(chosen.getId()));zones.save(z);});chosen.setDefault(true);}
 private void apply(OrganizationZone z,OrganizationZoneRequest r){z.setDescription(blank(r.description()));if(r.active()!=null)z.setActive(r.active());if(r.capacity()!=null&&r.capacity().signum()<0)throw new BadRequestException("Capacity cannot be negative");z.setCapacity(r.capacity());}
 private void tenant(UUID org){if(!org.equals(actor.organizationId()))throw new ForbiddenException("Organization access denied");} private String req(String s){if(s==null||s.isBlank())throw new BadRequestException("Code and name are required");return s.trim();}private String blank(String s){return s==null||s.isBlank()?null:s.trim();}
}
