package q.qedet.organization.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.ForbiddenException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.organization.dto.PositionRequest;
import q.qedet.organization.dto.PositionResponse;
import q.qedet.organization.entity.Department;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.Position;
import q.qedet.organization.mapper.OrganizationMapper;
import q.qedet.organization.repository.DepartmentRepository;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.organization.repository.PositionRepository;

import java.util.List;
import java.util.UUID;

import static q.qedet.common.model.Permission.*;

@Service
public class PositionService {
    private final PositionRepository positionRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final OrganizationSiteRepository siteRepository;
    private final ActorContext actorContext;

    public PositionService(PositionRepository positionRepository, OrganizationRepository organizationRepository,
                           DepartmentRepository departmentRepository, OrganizationSiteRepository siteRepository,
                           ActorContext actorContext) {
        this.positionRepository = positionRepository;
        this.organizationRepository = organizationRepository;
        this.departmentRepository = departmentRepository;
        this.siteRepository = siteRepository;
        this.actorContext = actorContext;
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> list(UUID organizationId, UUID departmentId, UUID siteId, Boolean active, String search) {
        ensureTenant(organizationId);
        actorContext.requirePermission(POSITION_VIEW);
        String term = search == null ? null : search.trim().toLowerCase();
        return positionRepository.findAllByOrganizationIdOrderByNameAsc(organizationId).stream()
                .filter(position -> departmentId == null || departmentId.equals(position.getDepartment().getId()))
                .filter(position -> siteId == null || (position.getDefaultSite() != null
                        && siteId.equals(position.getDefaultSite().getId())))
                .filter(position -> active == null || active.equals(position.isActive()))
                .filter(position -> term == null || term.isBlank() || position.getName().toLowerCase().contains(term)
                        || position.getCode().toLowerCase().contains(term))
                .map(OrganizationMapper::position)
                .toList();
    }

    @Transactional(readOnly = true)
    public PositionResponse get(UUID organizationId, UUID id) {
        ensureTenant(organizationId);
        actorContext.requirePermission(POSITION_VIEW);
        return OrganizationMapper.position(ownedPosition(id, organizationId));
    }

    @Transactional
    public PositionResponse create(UUID organizationId, PositionRequest request) {
        ensureTenant(organizationId);
        actorContext.requirePermission(POSITION_CREATE);
        String code = request.code().trim().toUpperCase();
        if (positionRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)) {
            throw new ConflictException("Position code already exists");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        Position position = new Position();
        position.setOrganization(organization);
        position.setCode(code);
        position.setName(request.name().trim());
        apply(position, request, organizationId);
        return OrganizationMapper.position(positionRepository.save(position));
    }

    @Transactional
    public PositionResponse update(UUID organizationId, UUID id, PositionRequest request) {
        ensureTenant(organizationId);
        actorContext.requirePermission(POSITION_UPDATE);
        Position position = ownedPosition(id, organizationId);
        String code = request.code().trim().toUpperCase();
        if (!position.getCode().equalsIgnoreCase(code)
                && positionRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)) {
            throw new ConflictException("Position code already exists");
        }
        position.setCode(code);
        position.setName(request.name().trim());
        apply(position, request, organizationId);
        return OrganizationMapper.position(positionRepository.save(position));
    }

    @Transactional
    public void delete(UUID organizationId, UUID id) {
        ensureTenant(organizationId);
        actorContext.requirePermission(POSITION_DELETE);
        Position position = ownedPosition(id, organizationId);
        position.setActive(false);
        positionRepository.save(position);
    }

    private void apply(Position position, PositionRequest request, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(request.departmentId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Department not found"));
        OrganizationSite site = request.defaultSiteId() == null ? null
                : siteRepository.findByIdAndOrganizationId(request.defaultSiteId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Default site not found"));
        position.setDepartment(department);
        position.setDefaultSite(site);
        position.setDescription(trimToNull(request.description()));
        if (request.active() != null) {
            position.setActive(request.active());
        }
    }

    private Position ownedPosition(UUID id, UUID organizationId) {
        return positionRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new NotFoundException("Position not found"));
    }

    private void ensureTenant(UUID organizationId) {
        if (!organizationId.equals(actorContext.organizationId())) {
            throw new ForbiddenException("Organization access denied");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
