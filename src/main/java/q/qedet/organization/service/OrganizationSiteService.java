package q.qedet.organization.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.LocationStatus;
import q.qedet.organization.dto.*;
import q.qedet.organization.entity.*;
import q.qedet.organization.repository.*;
import java.util.*;
import static q.qedet.common.model.Permission.*;

@Service
public class OrganizationSiteService {
    private final OrganizationSiteRepository sites; private final OrganizationRepository organizations;
    private final OrganizationSiteProvisioningService provisioning; private final ActorContext actor;
    private final PositionRepository positions;
    private final OrganizationZoneRepository zones;
    private final q.qedet.employee.repository.EmployeeRepository employees;
    public OrganizationSiteService(OrganizationSiteRepository sites, OrganizationRepository organizations,
                                   OrganizationSiteProvisioningService provisioning, ActorContext actor,
                                   PositionRepository positions, OrganizationZoneRepository zones,
                                   q.qedet.employee.repository.EmployeeRepository employees) {
        this.sites=sites; this.organizations=organizations; this.provisioning=provisioning; this.actor=actor;
        this.positions=positions; this.zones=zones; this.employees=employees;
    }
    @Transactional(readOnly = true) public List<OrganizationSiteResponse> list(UUID organizationId) {
        tenant(organizationId); actor.requirePermission(LOCATION_VIEW);
        return sites.findAllByOrganizationIdAndStatusOrderByNameAsc(organizationId, LocationStatus.ACTIVE).stream().map(this::view).toList();
    }
    /** Locations are organization sites in the domain model.  This list intentionally includes inactive sites. */
    @Transactional(readOnly = true) public List<OrganizationSiteResponse> listLocations(UUID organizationId) {
        tenant(organizationId); actor.requirePermission(LOCATION_VIEW);
        return sites.findAllByOrganizationIdOrderByNameAsc(organizationId).stream().map(this::view).toList();
    }
    @Transactional public OrganizationSiteResponse create(UUID organizationId, OrganizationSiteRequest request) {
        tenant(organizationId); actor.requirePermission(LOCATION_MANAGE);
        String code=required(request.code()); if (sites.findByOrganizationIdAndCodeIgnoreCase(organizationId, code).isPresent()) throw new ConflictException("Site code already exists");
        Organization organization=organizations.findByIdForUpdate(organizationId).orElseThrow(() -> new NotFoundException("Organization not found"));
        OrganizationSite site=new OrganizationSite(organization,code,required(request.name()),
                request.type() == null ? q.qedet.common.model.LocationType.OTHER : request.type()); apply(site,request);
        if (Boolean.TRUE.equals(request.isDefault())) { clearDefault(organizationId); site.setDefault(true); }
        sites.save(site); provisioning.ensureExactlyOneActiveDefaultZone(site); return view(site);
    }
    /** Applies only supplied JSON properties; a supplied JSON null clears nullable text fields. */
    @Transactional public OrganizationSiteResponse patch(UUID organizationId, UUID id, JsonNode request) {
        tenant(organizationId); actor.requirePermission(LOCATION_MANAGE);
        if (request == null || !request.isObject()) throw new BadRequestException("Location patch must be an object");
        OrganizationSite site = owned(id, organizationId);
        if (request.has("code")) {
            String code = requiredText(request, "code");
            sites.findByOrganizationIdAndCodeIgnoreCase(organizationId, code).filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> { throw new ConflictException("Site code already exists"); });
            site.setCode(code);
        }
        if (request.has("name")) site.setName(requiredText(request, "name"));
        if (request.has("type")) {
            if (request.get("type").isNull()) throw new BadRequestException("Location type cannot be null");
            try { site.setType(q.qedet.common.model.LocationType.valueOf(request.get("type").asText())); }
            catch (IllegalArgumentException ex) { throw new BadRequestException("Invalid location type"); }
        }
        setTextIfPresent(site::setDescription, request, "description");
        setTextIfPresent(site::setAddressLine1, request, "addressLine1");
        setTextIfPresent(site::setAddressLine2, request, "addressLine2");
        setTextIfPresent(site::setCity, request, "city");
        setTextIfPresent(site::setState, request, "state");
        setTextIfPresent(site::setPostalCode, request, "postalCode");
        setTextIfPresent(site::setCountry, request, "country");
        setTextIfPresent(site::setPhone, request, "phone");
        setTextIfPresent(site::setEmail, request, "email");
        if (request.has("isDefault")) {
            if (!request.get("isDefault").isBoolean()) throw new BadRequestException("isDefault must be a boolean");
            if (request.get("isDefault").booleanValue()) { clearDefault(organizationId); site.setDefault(true); }
            else site.setDefault(false);
        }
        return view(sites.save(site));
    }
    @Transactional public OrganizationSiteResponse update(UUID organizationId, UUID id, OrganizationSiteRequest request) {
        tenant(organizationId); actor.requirePermission(LOCATION_MANAGE); OrganizationSite site=owned(id,organizationId);
        String code=required(request.code()); sites.findByOrganizationIdAndCodeIgnoreCase(organizationId,code).filter(x->!x.getId().equals(id)).ifPresent(x->{throw new ConflictException("Site code already exists");});
        site.setCode(code);site.setName(required(request.name()));
        if (request.type() != null) site.setType(request.type());
        apply(site,request);
        if (Boolean.TRUE.equals(request.isDefault())) {clearDefault(organizationId);site.setDefault(true);} else if(Boolean.FALSE.equals(request.isDefault())) site.setDefault(false);
        return view(sites.save(site));
    }
    @Transactional public OrganizationSiteResponse activate(UUID organizationId,UUID id) { tenant(organizationId);actor.requirePermission(LOCATION_MANAGE);OrganizationSite s=owned(id,organizationId);s.setStatus(LocationStatus.ACTIVE);sites.save(s);provisioning.ensureExactlyOneActiveDefaultZone(s);return view(s); }
    @Transactional public OrganizationSiteResponse deactivate(UUID organizationId,UUID id) { tenant(organizationId);actor.requirePermission(LOCATION_MANAGE);OrganizationSite s=owned(id,organizationId);s.setStatus(LocationStatus.INACTIVE);s.setDefault(false);return view(sites.save(s)); }
    @Transactional public OrganizationSiteResponse activate(UUID id) { return activate(actor.organizationId(), id); }
    @Transactional public OrganizationSiteResponse deactivate(UUID id) { return deactivate(actor.organizationId(), id); }
    @Transactional public void delete(UUID organizationId, UUID id) {
        tenant(organizationId); actor.requirePermission(LOCATION_MANAGE);
        OrganizationSite site = owned(id, organizationId);
        if (site.isDefault()) throw new ConflictException("Default location cannot be deleted");
        if (positions.existsByDefaultSiteId(id) || employees.existsBySiteId(id) || !zones.findAllBySiteIdOrderByNameAsc(id).isEmpty()) {
            throw new ConflictException("Location is referenced and cannot be deleted");
        }
        sites.delete(site);
    }
    private OrganizationSite owned(UUID id,UUID org) {return sites.findByIdAndOrganizationId(id,org).orElseThrow(()->new NotFoundException("Site not found"));}
    private void tenant(UUID id){if(!id.equals(actor.organizationId()))throw new ForbiddenException("Organization access denied");}
    private void clearDefault(UUID org){sites.findAllByOrganizationIdAndStatusOrderByNameAsc(org,LocationStatus.ACTIVE).forEach(s->s.setDefault(false));}
    private String required(String s){if(s==null||s.isBlank())throw new BadRequestException("Code and name are required");return s.trim();}
    private String blank(String s){return s==null||s.isBlank()?null:s.trim();}
    private String requiredText(JsonNode request, String field) {
        JsonNode value = request.get(field);
        if (value == null || value.isNull() || !value.isTextual()) throw new BadRequestException(field + " is required");
        return required(value.asText());
    }
    private void setTextIfPresent(java.util.function.Consumer<String> setter, JsonNode request, String field) {
        if (!request.has(field)) return;
        JsonNode value = request.get(field);
        if (!value.isNull() && !value.isTextual()) throw new BadRequestException(field + " must be a string or null");
        setter.accept(value.isNull() ? null : blank(value.asText()));
    }
    private void apply(OrganizationSite s,OrganizationSiteRequest r){s.setDescription(blank(r.description()));s.setAddressLine1(blank(r.addressLine1()));s.setAddressLine2(blank(r.addressLine2()));s.setCity(blank(r.city()));s.setState(blank(r.state()));s.setPostalCode(blank(r.postalCode()));s.setCountry(blank(r.country()));s.setPhone(blank(r.phone()));s.setEmail(blank(r.email()));}
    private OrganizationSiteResponse view(OrganizationSite s){return new OrganizationSiteResponse(s.getId(),s.getOrganization().getId(),s.getCode(),s.getName(),s.getType(),s.getDescription(),s.getAddressLine1(),s.getAddressLine2(),s.getCity(),s.getState(),s.getPostalCode(),s.getCountry(),s.getPhone(),s.getEmail(),s.isDefault(),s.getStatus(),s.getCreatedAt(),s.getUpdatedAt());}
}
