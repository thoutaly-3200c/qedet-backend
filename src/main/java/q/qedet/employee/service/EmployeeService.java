package q.qedet.employee.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import q.qedet.auth.security.ActorContext;
import q.qedet.auth.service.InvitationService;
import q.qedet.common.audit.AuditService;
import q.qedet.common.model.EmployeeStatus;
import q.qedet.common.model.RoleType;
import q.qedet.common.model.UserStatus;
import q.qedet.common.response.ResponseMapper;
import q.qedet.common.usage.UsageTracker;


import q.qedet.employee.dto.EmployeeRequest;
import q.qedet.employee.dto.EmployeeResponse;
import q.qedet.employee.dto.InviteRequest;
import q.qedet.employee.dto.LifecycleRequest;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.entity.EmployeeDepartment;
import q.qedet.employee.entity.EmployeeRole;
import q.qedet.employee.entity.Role;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.employee.repository.EmployeeDepartmentRepository;
import q.qedet.employee.repository.EmployeeRoleRepository;
import q.qedet.employee.repository.RoleRepository;
import q.qedet.employee.dto.DepartmentAssignmentRequest;
import q.qedet.employee.dto.RoleAssignmentRequest;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.organization.entity.Department;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.DepartmentRepository;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.employee.dto.TerminationRequest;
import q.qedet.employee.dto.TransferRequest;
import q.qedet.employee.dto.EmployeeInvitationRequest;
import q.qedet.employee.dto.EmployeePositionAssignmentRequest;
import q.qedet.auth.entity.Invitation;
import q.qedet.auth.entity.UserAccount;
import q.qedet.auth.repository.InvitationRepository;
import q.qedet.auth.repository.UserAccountRepository;
import q.qedet.common.model.InvitationStatus;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.Position;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.organization.repository.PositionRepository;
import q.qedet.hr.common.event.EmployeeLifecycleEventPublisher;
import q.qedet.hr.common.model.EmployeeLifecycleEventType;


import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static q.qedet.common.model.Permission.*;


@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final ActorContext actorContext;
    private final InvitationService invitationService;
    private final AuditService auditService;
    private final UsageTracker usageTracker;
    private final EmployeeDepartmentRepository employeeDepartments;
    private final EmployeeRoleRepository employeeRoles;
    private final RoleRepository roleRepository;
    private final OrganizationSiteRepository siteRepository;
    private final PositionRepository positionRepository;
    private final InvitationRepository invitationRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeLifecycleEventPublisher lifecycleEvents;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           OrganizationRepository organizationRepository,
                           ActorContext actorContext,
                           InvitationService invitationService,
                           AuditService auditService,
                           UsageTracker usageTracker,
                           EmployeeDepartmentRepository employeeDepartments,
                           EmployeeRoleRepository employeeRoles,
                           RoleRepository roleRepository,
                            OrganizationSiteRepository siteRepository,
                           PositionRepository positionRepository,
                           InvitationRepository invitationRepository,
                           UserAccountRepository userAccountRepository,
                           EmployeeLifecycleEventPublisher lifecycleEvents) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.organizationRepository = organizationRepository;
        this.actorContext = actorContext;
        this.invitationService = invitationService;
        this.auditService = auditService;
        this.usageTracker = usageTracker;
        this.employeeDepartments = employeeDepartments;
        this.employeeRoles = employeeRoles;
        this.roleRepository = roleRepository;
        this.siteRepository = siteRepository;
        this.positionRepository = positionRepository;
        this.invitationRepository = invitationRepository;
        this.userAccountRepository = userAccountRepository;
        this.lifecycleEvents = lifecycleEvents;
    }

    @Transactional
    public EmployeeResponse inviteNew(EmployeeInvitationRequest request) {
        actorContext.requirePermission(EMPLOYEE_INVITE);
        UUID organizationId = actorContext.organizationId();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Position position = positionRepository.findByIdAndOrganizationId(request.positionId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Position not found"));
        if (!position.isActive()) {
            throw new ConflictException("Inactive positions cannot be assigned");
        }
        Department department = position.getDepartment();
        Role role = roleRepository.findByOrganizationIdAndCode(organizationId, position.getCode()).orElse(null);
        OrganizationSite site = resolveAssignedSite(request.locationId(), position, organizationId);
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException(
                        "This email is not registered in Qedet. The user must register before receiving an invitation."));
        Invitation pendingInvitation = invitationRepository
                .findByOrganizationIdAndTargetEmailIgnoreCaseAndStatus(
                        organizationId, email, InvitationStatus.PENDING)
                .orElse(null);
        if (pendingInvitation != null) {
            Employee employee = pendingInvitation.getEmployee();
            employee.setUser(user);
            employee.setPosition(position);
            employee.setDepartment(department);
            employee.setSite(site);
            employeeRepository.save(employee);
            invitationService.createInvitation(employee.getId(), new InviteRequest(email, null, null));
            return ResponseMapper.employee(employee);
        }
        if (employeeRepository.findByUserIdAndOrganizationId(user.getId(), organizationId).isPresent()) {
            throw new ConflictException("User is already an employee of this organization");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        Employee employee = new Employee(organization, generateEmployeeNumber(organizationId),
                request.firstName().trim(), request.lastName().trim(), email, null,
                position.getDefaultRole() == null ? RoleType.EMPLOYEE : position.getDefaultRole());
        employee.setUser(user);
        employee.setPosition(position);
        employee.setDepartment(department);
        employee.setSite(site);
        employee.setStatus(EmployeeStatus.INVITED);
        Employee saved = employeeRepository.save(employee);
        if (role != null) {
            EmployeeRole roleAssignment = new EmployeeRole();
            roleAssignment.setEmployee(saved);
            roleAssignment.setRole(role);
            employeeRoles.save(roleAssignment);
        }
        invitationService.createInvitation(saved.getId(), new InviteRequest(email, null, null));
        return ResponseMapper.employee(saved);
    }

    @Transactional
    public EmployeeResponse assignPosition(UUID employeeId, EmployeePositionAssignmentRequest request) {
        actorContext.requirePermission(EMPLOYEE_UPDATE);
        Employee employee = getAssignableEmployee(employeeId);
        UUID organizationId = actorContext.organizationId();
        Position position = positionRepository.findByIdAndOrganizationId(request.positionId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Position not found"));
        if (!position.isActive()) {
            throw new ConflictException("Inactive positions cannot be assigned");
        }
        OrganizationSite site = resolveAssignedSite(request.locationId(), position, organizationId);
        employee.setPosition(position);
        employee.setDepartment(position.getDepartment());
        employee.setSite(site);
        employeeDepartments.findAllByEmployeeIdAndEndedAtIsNull(employeeId)
                .forEach(value -> value.setEndedAt(Instant.now()));
        EmployeeDepartment assignment = new EmployeeDepartment();
        assignment.setEmployee(employee);
        assignment.setDepartment(position.getDepartment());
        assignment.setPrimaryAssignment(true);
        employeeDepartments.save(assignment);
        return ResponseMapper.employee(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> list(int page, int limit, String search, UUID departmentId, String status) {
        actorContext.requirePermission(EMPLOYEE_VIEW);
        List<EmployeeResponse> results = employeeRepository.findAllByOrganizationId(actorContext.organizationId())
                .stream()
                .filter(employee -> matchesSearch(employee, search))
                .filter(employee -> departmentId == null || (employee.getDepartment() != null && departmentId.equals(employee.getDepartment().getId())))
                .filter(employee -> matchesStatus(employee, status))
                .map(ResponseMapper::employee)
                .toList();
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);
        int fromIndex = Math.min((safePage - 1) * safeLimit, results.size());
        int toIndex = Math.min(fromIndex + safeLimit, results.size());
        return results.subList(fromIndex, toIndex);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(UUID id) {
        actorContext.requirePermission(EMPLOYEE_VIEW);
        return ResponseMapper.employee(getOwnedEmployee(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        actorContext.requirePermission(EMPLOYEE_CREATE);
        RoleType targetRole = parseRole(request.role());
        actorContext.requireRoleCanAssign(targetRole);

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findByIdAndOrganizationId(request.departmentId(), actorContext.organizationId())
                    .orElseThrow(() -> new NotFoundException("Department not found"));
        }

        Organization organization = organizationRepository.findById(actorContext.organizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        Employee employee = new Employee();
        employee.setOrganization(organization);
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setSalary(request.salary());
        employee.setPhone(request.phone());
        employee.setEmail(request.email() == null ? null : request.email().trim().toLowerCase());
        employee.setEmployeeNumber(generateEmployeeNumber(actorContext.organizationId()));
        employee.setDepartment(department);
        employee.setRole(targetRole);
        employee.setStatus(resolveStatus(request.status(), request.isActive(), EmployeeStatus.CREATED));
        employee.setJoinedAt(request.hireDate() == null ? Instant.now() : request.hireDate().atStartOfDay().toInstant(ZoneOffset.UTC));
        Employee saved = employeeRepository.save(employee);
        if (saved.getStatus() == EmployeeStatus.ACTIVE) {
            lifecycleEvents.publish(saved, EmployeeLifecycleEventType.EMPLOYEE_ACTIVATED);
        }
        if (department != null) {
            EmployeeDepartment assignment = new EmployeeDepartment();
            assignment.setEmployee(saved);
            assignment.setDepartment(department);
            assignment.setPrimaryAssignment(true);
            employeeDepartments.save(assignment);
        }
        auditService.record(saved.getOrganization(), null, "EMPLOYEE_CREATED", "Employee", saved.getId().toString(), null, saved.getEmployeeNumber());
        usageTracker.record(saved.getOrganization(), saved, "employee", "create", "employee created");
        return ResponseMapper.employee(saved);
    }

    @Transactional
    public EmployeeResponse update(UUID id, EmployeeRequest request) {
        actorContext.requirePermission(EMPLOYEE_UPDATE);
        Employee employee = getOwnedEmployee(id);
        RoleType previousRole = employee.getRole();
        EmployeeStatus previousStatus = employee.getStatus();
        if (request.role() != null && !request.role().isBlank()) {
            RoleType requestedRole = parseRole(request.role());
            if (requestedRole != previousRole) {
                actorContext.requireRoleCanAssign(requestedRole);
                employee.setRole(requestedRole);
            }
        }
        if (request.firstName() != null && !request.firstName().isBlank()) {
            employee.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            employee.setLastName(request.lastName().trim());
        }
        if (request.fullName() != null && !request.fullName().isBlank()) {
            String[] parts = splitName(request.fullName());
            employee.setFirstName(parts[0]);
            employee.setLastName(parts[1]);
        }
        employee.setSalary(request.salary() != null ? request.salary() : employee.getSalary());
        if (request.phone() != null) {
            employee.setPhone(request.phone());
        }
        if (request.email() != null) {
            employee.setEmail(request.email().trim().toLowerCase());
        }
        if (request.departmentId() != null) {
        Department department = departmentRepository.findByIdAndOrganizationId(request.departmentId(), actorContext.organizationId())
                    .orElseThrow(() -> new NotFoundException("Department not found"));
            employee.setDepartment(department);
        }
        if (request.hireDate() != null) {
            employee.setJoinedAt(request.hireDate().atStartOfDay().toInstant(ZoneOffset.UTC));
        }
        if (request.status() != null || request.isActive() != null) {
            employee.setStatus(resolveStatus(request.status(), request.isActive(), employee.getStatus()));
        }
        Employee saved = employeeRepository.save(employee);
        if (previousStatus != EmployeeStatus.ACTIVE && saved.getStatus() == EmployeeStatus.ACTIVE) {
            lifecycleEvents.publish(saved, EmployeeLifecycleEventType.EMPLOYEE_ACTIVATED);
        }
        auditService.record(saved.getOrganization(), null, "EMPLOYEE_UPDATED", "Employee", saved.getId().toString(), previousRole.name(), saved.getRole().name());
        return ResponseMapper.employee(saved);
    }

    @Transactional
    public String invite(UUID id, InviteRequest request) {
        Employee employee = getOwnedEmployee(id);
        if (request != null && request.email() != null && !request.email().isBlank()) {
            employee.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        }
        if (request != null && request.role() != null && !request.role().isBlank()) {
            RoleType requestedRole = parseRole(request.role());
            actorContext.requireRoleCanAssign(requestedRole);
            employee.setRole(requestedRole);
        }
        employeeRepository.save(employee);
        return invitationService.createInvitation(employee.getId(), request);
    }

    @Transactional
    public EmployeeResponse activate(UUID id, LifecycleRequest request) {
        actorContext.requirePermission(EMPLOYEE_ACTIVATE);
        Employee employee = getOwnedEmployee(id);
        boolean wasActive = employee.getStatus() == EmployeeStatus.ACTIVE;
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setActivatedAt(Instant.now());
        Employee saved = employeeRepository.save(employee);
        if (!wasActive) {
            lifecycleEvents.publish(saved, EmployeeLifecycleEventType.EMPLOYEE_ACTIVATED);
        }
        auditService.record(saved.getOrganization(), null, "EMPLOYEE_ACTIVATED", "Employee", saved.getId().toString(), null, request == null ? null : request.reason());
        return ResponseMapper.employee(saved);
    }

    @Transactional
    public EmployeeResponse deactivate(UUID id, LifecycleRequest request) {
        actorContext.requirePermission(EMPLOYEE_DEACTIVATE);
        Employee employee = getOwnedEmployee(id);
        employee.setStatus(EmployeeStatus.INACTIVE);
        employee.setDeactivatedAt(Instant.now());
        Employee saved = employeeRepository.save(employee);
        auditService.record(saved.getOrganization(), null, "EMPLOYEE_DEACTIVATED", "Employee", saved.getId().toString(), null, request == null ? null : request.reason());
        return ResponseMapper.employee(saved);
    }

    @Transactional
    public EmployeeResponse transfer(UUID id, TransferRequest request) {
        actorContext.requirePermission(EMPLOYEE_TRANSFER);
        Employee employee = getOwnedEmployee(id);
        Department previous = employee.getDepartment();
        Department newDepartment = departmentRepository.findByIdAndOrganizationId(request.departmentId(), actorContext.organizationId())
                .orElseThrow(() -> new NotFoundException("Department not found"));
        employee.setDepartment(newDepartment);
        employeeDepartments.findAllByEmployeeIdAndEndedAtIsNull(id).forEach(value -> value.setEndedAt(Instant.now()));
        EmployeeDepartment assignment = new EmployeeDepartment();
        assignment.setEmployee(employee);
        assignment.setDepartment(newDepartment);
        assignment.setPrimaryAssignment(true);
        employeeDepartments.save(assignment);
        Employee saved = employeeRepository.save(employee);
        auditService.record(saved.getOrganization(), null, "EMPLOYEE_TRANSFERRED", "Employee", saved.getId().toString(),
                previous == null ? null : previous.getId().toString(),
                "department=" + newDepartment.getId() + ", manager=" + request.managerId() + ", effectiveDate=" + request.effectiveDate() + ", reason=" + request.reason());
        return ResponseMapper.employee(saved);
    }

    @Transactional
    public EmployeeResponse terminate(UUID id, TerminationRequest request) {
        actorContext.requirePermission(EMPLOYEE_TERMINATE);
        Employee employee = getOwnedEmployee(id);
        employee.setStatus(EmployeeStatus.TERMINATED);
        employee.setTerminatedAt(request != null && request.effectiveDate() != null
                ? request.effectiveDate().atStartOfDay().toInstant(ZoneOffset.UTC)
                : Instant.now());
        if (employee.getUser() != null) {
            employee.getUser().setStatus(UserStatus.INACTIVE);
        }

        Employee saved = employeeRepository.save(employee);
        auditService.record(saved.getOrganization(), null, "EMPLOYEE_TERMINATED", "Employee", saved.getId().toString(), null,
                request == null ? null : (request.reason() + " | " + request.notes()));
        return ResponseMapper.employee(saved);
    }

    @Transactional
    public void assignDepartment(UUID employeeId, DepartmentAssignmentRequest request) {
        actorContext.requirePermission(EMPLOYEE_UPDATE);
        Employee employee = getAssignableEmployee(employeeId);
        Department department = departmentRepository.findByIdAndOrganizationId(request.departmentId(), actorContext.organizationId())
                .orElseThrow(() -> new NotFoundException("Department not found"));
        if (employeeDepartments.existsByEmployeeIdAndDepartmentIdAndEndedAtIsNull(employeeId, department.getId()))
            throw new ConflictException("Department assignment already exists");
        if (request.primary()) {
            employeeDepartments.findAllByEmployeeIdAndEndedAtIsNull(employeeId).forEach(value -> value.setPrimaryAssignment(false));
        }
        EmployeeDepartment assignment = new EmployeeDepartment();
        assignment.setEmployee(employee); assignment.setDepartment(department); assignment.setPrimaryAssignment(request.primary());
        employeeDepartments.save(assignment);
    }

    @Transactional
    public void endDepartment(UUID assignmentId) {
        actorContext.requirePermission(EMPLOYEE_UPDATE);
        EmployeeDepartment assignment = employeeDepartments.findByIdAndEmployeeOrganizationId(
                assignmentId, actorContext.organizationId()).orElseThrow(() -> new NotFoundException("Department assignment not found"));
        assignment.setEndedAt(Instant.now());
        employeeDepartments.save(assignment);
    }

    @Transactional
    public void assignRole(UUID employeeId, RoleAssignmentRequest request) {
        actorContext.requirePermission(EMPLOYEE_UPDATE);
        Employee employee = getAssignableEmployee(employeeId);
        Role role = roleRepository.findByIdAndOrganizationId(request.roleId(), actorContext.organizationId())
                .orElseThrow(() -> new NotFoundException("Role not found"));
        if (employeeRoles.existsByEmployeeIdAndRoleIdAndEndedAtIsNull(employeeId, role.getId()))
            throw new ConflictException("Role assignment already exists");
        EmployeeRole assignment = new EmployeeRole();
        assignment.setEmployee(employee); assignment.setRole(role);
        employeeRoles.save(assignment);
    }

    @Transactional
    public void endRole(UUID assignmentId) {
        actorContext.requirePermission(EMPLOYEE_UPDATE);
        EmployeeRole assignment = employeeRoles.findByIdAndEmployeeOrganizationId(
                assignmentId, actorContext.organizationId()).orElseThrow(() -> new NotFoundException("Role assignment not found"));
        assignment.setEndedAt(Instant.now());
        employeeRoles.save(assignment);
    }

    private Employee getAssignableEmployee(UUID id) {
        Employee employee = getOwnedEmployee(id);
        if (employee.getStatus() == EmployeeStatus.TERMINATED || employee.getStatus() == EmployeeStatus.INACTIVE)
            throw new ConflictException("Inactive employees cannot receive assignments");
        return employee;
    }

    private OrganizationSite resolveAssignedSite(UUID requestedSiteId, Position position, UUID organizationId) {
        UUID siteId = requestedSiteId == null
                ? (position.getDefaultSite() == null ? null : position.getDefaultSite().getId())
                : requestedSiteId;
        if (siteId == null) {
            return null;
        }
        return siteRepository.findByIdAndOrganizationId(siteId, organizationId)
                .orElseThrow(() -> new NotFoundException("Site not found"));
    }

    private Employee getOwnedEmployee(UUID id) {
        return employeeRepository.findByIdAndOrganizationId(id, actorContext.organizationId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    private boolean matchesSearch(Employee employee, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String term = search.trim().toLowerCase();
        String fullName = ((employee.getFirstName() == null ? "" : employee.getFirstName()) + " " + (employee.getLastName() == null ? "" : employee.getLastName())).toLowerCase();
        return fullName.contains(term)
                || (employee.getEmail() != null && employee.getEmail().toLowerCase().contains(term))
                || (employee.getEmployeeNumber() != null && employee.getEmployeeNumber().toLowerCase().contains(term));
    }

    private boolean matchesStatus(Employee employee, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "ACTIVE" -> employee.getStatus() == EmployeeStatus.ACTIVE;
            case "INACTIVE" -> employee.getStatus() == EmployeeStatus.INACTIVE;
            case "SUSPENDED" -> employee.getStatus() == EmployeeStatus.SUSPENDED;
            case "TERMINATED" -> employee.getStatus() == EmployeeStatus.TERMINATED;
            default -> true;
        };
    }

    private RoleType parseRole(String role) {
        try {
            return RoleType.fromApiValue(role);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role: " + role);
        }
    }

    private EmployeeStatus resolveStatus(String status, Boolean isActive, EmployeeStatus fallback) {
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase();
            return switch (normalized) {
                case "ACTIVE" -> EmployeeStatus.ACTIVE;
                case "INACTIVE" -> EmployeeStatus.INACTIVE;
                case "SUSPENDED" -> EmployeeStatus.SUSPENDED;
                case "TERMINATED" -> EmployeeStatus.TERMINATED;
                default -> fallback;
            };
        }
        if (isActive != null) {
            return isActive ? EmployeeStatus.ACTIVE : EmployeeStatus.INACTIVE;
        }
        return fallback;
    }

    private String[] splitName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        if (trimmed.isBlank()) {
            return new String[]{"", ""};
        }
        String[] parts = trimmed.split("\\s+", 2);
        if (parts.length == 1) {
            return new String[]{parts[0], ""};
        }
        return new String[]{parts[0], parts[1]};
    }

    private String generateEmployeeNumber(UUID organizationId) {
        long next = employeeRepository.countByOrganizationId(organizationId) + 1;
        return "EMP-" + String.format("%05d", next);
    }
}
