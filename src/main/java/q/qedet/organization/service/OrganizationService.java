package q.qedet.organization.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.auth.entity.UserAccount;
import q.qedet.auth.dto.AuthResponse;
import q.qedet.auth.security.TokenService;
import q.qedet.auth.repository.UserAccountRepository;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.ForbiddenException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.model.BusinessType;
import q.qedet.common.model.EmployeeStatus;
import q.qedet.common.model.OrganizationStatus;
import q.qedet.common.model.RoleType;
import q.qedet.common.model.RegistrationStatus;
import q.qedet.common.response.ResponseMapper;
import q.qedet.common.usage.UsageTracker;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.finance.service.CashBoxProvisioningService;
import q.qedet.employee.entity.Role;
import q.qedet.employee.repository.RoleRepository;
import q.qedet.employee.service.DefaultRolePermissionProvisioningService;
import q.qedet.organization.dto.OrganizationRequest;
import q.qedet.organization.dto.RegistrationRequest;
import q.qedet.organization.dto.OrganizationResponse;
import q.qedet.organization.entity.Department;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.mapper.OrganizationMapper;
import q.qedet.organization.repository.DepartmentRepository;
import q.qedet.organization.repository.PositionRepository;
import q.qedet.organization.repository.OrganizationRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static q.qedet.common.model.Permission.ORGANIZATION_UPDATE;
import static q.qedet.common.model.Permission.ORGANIZATION_VIEW;

@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final ActorContext actorContext;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final TokenService tokenService;
    private final UsageTracker usageTracker;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PositionRepository positionRepository;
    private final CashBoxProvisioningService cashBoxProvisioning;
    private final DefaultRolePermissionProvisioningService defaultRolePermissions;
    private final OrganizationSiteProvisioningService organizationSiteProvisioning;

    public OrganizationService(OrganizationRepository organizationRepository, ActorContext actorContext,
                               EmployeeRepository employeeRepository, DepartmentRepository departmentRepository,
                               TokenService tokenService,
                               UsageTracker usageTracker, UserAccountRepository userAccountRepository,
                               RoleRepository roleRepository, PositionRepository positionRepository,
                               CashBoxProvisioningService cashBoxProvisioning,
                               DefaultRolePermissionProvisioningService defaultRolePermissions,
                               OrganizationSiteProvisioningService organizationSiteProvisioning) {
        this.organizationRepository = organizationRepository;
        this.actorContext = actorContext;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.tokenService = tokenService;
        this.usageTracker = usageTracker;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.positionRepository = positionRepository;
        this.cashBoxProvisioning = cashBoxProvisioning;
        this.defaultRolePermissions = defaultRolePermissions;
        this.organizationSiteProvisioning = organizationSiteProvisioning;
    }

    @Transactional
    public OrganizationResponse create(OrganizationRequest request) {
        UserAccount user = userAccountRepository.findById(actorContext.current().userId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.isEmailVerified()) {
            throw new ForbiddenException("Email verification required");
        }
        AuthResponse result = create(user, new RegistrationRequest(
                user.getRegistrationName() == null ? user.getEmail() : user.getRegistrationName(),
                user.getEmail(), null, request.phone(), request.name(), request.country(), request.timezone()));
        return OrganizationMapper.organization(organizationRepository.findById(result.organizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found")));
    }

    @Transactional
    public AuthResponse create(UserAccount user, RegistrationRequest request) {
        String name = request.companyName() == null || request.companyName().isBlank()
                ? request.name().trim() : request.companyName().trim();
        if (organizationRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("Organization name already exists");
        }
        Organization organization = new Organization(name, BusinessType.GENERAL_TRADE);
        organization.setCountry(request.country());
        organization.setTimezone(request.timezone());
        organization.setTrialEndsAt(Instant.now().plus(Duration.ofDays(14)));
        organization = organizationRepository.save(organization);
        java.util.Map<String, OrganizationSite> sites = organizationSiteProvisioning.provisionRegistrationSites(organization);
        cashBoxProvisioning.ensureMainCashBox(organization);

        String[] names = splitName(request.name());
        Employee owner = new Employee(organization, "EMP-" +
                String.format("%05d", employeeRepository.countByOrganizationId(organization.getId()) + 1),
                names[0], names[1], user.getEmail(), request.phone(), RoleType.OWNER);
        owner.setUser(user);
        owner.setStatus(EmployeeStatus.ACTIVE);
        owner.setJoinedAt(Instant.now());
        owner.setActivatedAt(Instant.now());
        owner = employeeRepository.save(owner);

        java.util.Map<String, Department> departments = new java.util.HashMap<>();
        for (DepartmentTemplate template : DepartmentTemplate.values()) {
            departments.put(template.getCode(), departmentRepository.save(
                    new Department(organization, template.getName(), template.getCode())));
        }
        java.util.Map<String, Role> positions = new java.util.HashMap<>();
        for (PositionTemplate template : PositionTemplate.values()) {
            Department department = departments.get(template.getDepartment().getCode());
            OrganizationSite site = sites.get(template.getSite().code());
            positionRepository.save(new q.qedet.organization.entity.Position(
                    organization, department, site, template.getName(), template.getCode(),
                    template.getDefaultRole(), true));
            Role position = new Role();
            position.setOrganization(organization);
            position.setName(template.getName());
            position.setCode(template.getCode());
            position.setSystemRole(true);
            positions.put(template.getCode(), roleRepository.save(position));
        }
        defaultRolePermissions.reconcile(organization);

        TokenService.TokenPair tokens = tokenService.createSession(user, owner, false);
        usageTracker.record(organization, owner, "auth", "register", "organization created");
        return new AuthResponse(tokens.accessToken(), tokens.refreshToken(), user.getId(), owner.getId(),
                organization.getId(), owner.getRole().name(), ResponseMapper.permissionNames(owner.getRole()),
                RegistrationStatus.COMPLETED, null, null, null,
                owner.getFirstName(), owner.getLastName(), user.getEmail());
    }

    private String[] splitName(String name) {
        String[] parts = name == null ? new String[0] : name.trim().split("\\s+", 2);
        return parts.length == 0 ? new String[]{"Owner", "User"}
                : new String[]{parts[0], parts.length > 1 ? parts[1] : ""};
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get() {
        actorContext.requirePermission(ORGANIZATION_VIEW);
        return OrganizationMapper.organization(ownedOrganization());
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listForCurrentUser() {
        UUID userId = actorContext.current().userId();
        return organizationRepository.findAllByUserId(userId).stream()
                .map(OrganizationMapper::organization)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID id) {
        actorContext.requirePermission(ORGANIZATION_VIEW);
        return OrganizationMapper.organization(userOrganization(id));
    }

    @Transactional
    public OrganizationResponse update(OrganizationRequest request) {
        actorContext.requirePermission(ORGANIZATION_UPDATE);
        return update(actorContext.organizationId(), request);
    }

    @Transactional
    public OrganizationResponse update(UUID id, OrganizationRequest request) {
        actorContext.requirePermission(ORGANIZATION_UPDATE);
        requireOwner();
        Organization organization = ownedOrganization(id);
        String name = request.name().trim();
        organizationRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ConflictException("Organization name already exists");
            }
        });
        organization.setName(name);
        if (request.businessType() != null) {
            organization.setBusinessType(request.businessType());
        }
        organization.setCountry(trimToNull(request.country()));
        organization.setTimezone(trimToNull(request.timezone()));
        organization.setEmail(trimToNull(request.email()));
        organization.setPhone(trimToNull(request.phone()));
        organization.setAddress(trimToNull(request.address()));
        organization.setCity(trimToNull(request.city()));
        organization.setState(trimToNull(request.state()));
        organization.setPostalCode(trimToNull(request.postalCode()));
        organization.setWebsite(trimToNull(request.website()));
        organization.setLogoUrl(trimToNull(request.logoUrl()));
        return OrganizationMapper.organization(organizationRepository.save(organization));
    }

    @Transactional
    public OrganizationResponse activate() {
        actorContext.requirePermission(ORGANIZATION_UPDATE);
        requireOwner();
        Organization organization = ownedOrganization();
        organization.setStatus(OrganizationStatus.ACTIVE);
        return OrganizationMapper.organization(organizationRepository.save(organization));
    }

    @Transactional
    public OrganizationResponse suspend() {
        actorContext.requirePermission(ORGANIZATION_UPDATE);
        requireOwner();
        Organization organization = ownedOrganization();
        organization.setStatus(OrganizationStatus.SUSPENDED);
        return OrganizationMapper.organization(organizationRepository.save(organization));
    }

    private Organization ownedOrganization() {
        return ownedOrganization(actorContext.organizationId());
    }

    private Organization ownedOrganization(UUID id) {
        if (!id.equals(actorContext.organizationId())) {
            throw new ForbiddenException("Organization access denied");
        }
        return organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private Organization userOrganization(UUID id) {
        UUID userId = actorContext.current().userId();
        employeeRepository.findByUserIdAndOrganizationId(userId, id)
                .orElseThrow(() -> new ForbiddenException("Organization access denied"));
        return organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private void requireOwner() {
        if (actorContext.role() != q.qedet.common.model.RoleType.OWNER) {
            throw new ForbiddenException("Only the organization owner can update organization settings");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
