package q.qedet.organization.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import q.qedet.auth.security.ActorContext;
import q.qedet.common.model.DepartmentStatus;
import q.qedet.common.response.ResponseMapper;


import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.organization.dto.DepartmentRequest;
import q.qedet.organization.dto.DepartmentResponse;
import q.qedet.organization.entity.Department;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.DepartmentRepository;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.organization.repository.PositionRepository;
import q.qedet.organization.policy.entity.WorkSchedule;
import q.qedet.organization.policy.repository.WorkScheduleRepository;





import java.util.List;
import java.util.UUID;

import static q.qedet.common.model.Permission.DEPARTMENT_MANAGE;
import static q.qedet.common.model.Permission.DEPARTMENT_VIEW;


@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final PositionRepository positionRepository;
    private final q.qedet.employee.repository.EmployeeRepository employeeRepository;
    private final ActorContext actorContext;

    public DepartmentService(DepartmentRepository departmentRepository, OrganizationRepository organizationRepository,
                             WorkScheduleRepository workScheduleRepository, ActorContext actorContext,
                             PositionRepository positionRepository,
                             q.qedet.employee.repository.EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.organizationRepository = organizationRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.actorContext = actorContext;
        this.positionRepository = positionRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() {
        actorContext.requirePermission(DEPARTMENT_VIEW);
        return departmentRepository.findAllByOrganizationId(actorContext.organizationId())
                .stream().map(ResponseMapper::department).toList();
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        actorContext.requirePermission(DEPARTMENT_MANAGE);
        if (departmentRepository.existsByOrganizationIdAndNameIgnoreCase(actorContext.organizationId(), request.name().trim())) {
            throw new ConflictException("Department name already exists");
        }
        departmentRepository.findByOrganizationIdAndCodeIgnoreCase(actorContext.organizationId(), request.code())
                .ifPresent(existing -> {
                    throw new ConflictException("Department code already exists");
                });
        Organization organization = organizationRepository.findById(actorContext.organizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        Department department = new Department();
        department.setOrganization(organization);
        department.setName(request.name().trim());
        department.setCode(request.code().trim().toUpperCase());
        department.setDescription(request.description());
        department.setSchedule(resolveSchedule(request.scheduleId()));
        if (request.parentDepartmentId() != null) {
            department.setParentDepartment(departmentRepository.findByIdAndOrganizationId(
                    request.parentDepartmentId(), actorContext.organizationId())
                    .orElseThrow(() -> new NotFoundException("Parent department not found")));
        }
        department.setStatus(DepartmentStatus.ACTIVE);
        return ResponseMapper.department(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse update(UUID id, DepartmentRequest request) {
        actorContext.requirePermission(DEPARTMENT_MANAGE);
        Department department = getOwnedDepartment(id);
        if (departmentRepository.existsByOrganizationIdAndNameIgnoreCase(actorContext.organizationId(), request.name().trim())
                && !department.getName().equalsIgnoreCase(request.name().trim())) {
            throw new ConflictException("Department name already exists");
        }
        departmentRepository.findByOrganizationIdAndCodeIgnoreCase(actorContext.organizationId(), request.code().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new ConflictException("Department code already exists"); });
        if (id.equals(request.parentDepartmentId())) {
            throw new ConflictException("A department cannot be its own parent");
        }
        department.setName(request.name().trim());
        department.setCode(request.code().trim().toUpperCase());
        department.setDescription(request.description());
        department.setSchedule(resolveSchedule(request.scheduleId()));
        department.setParentDepartment(resolveParent(id, request.parentDepartmentId()));
        return ResponseMapper.department(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse activate(UUID id) {
        actorContext.requirePermission(DEPARTMENT_MANAGE);
        Department department = getOwnedDepartment(id);
        department.setStatus(DepartmentStatus.ACTIVE);
        return ResponseMapper.department(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse deactivate(UUID id) {
        actorContext.requirePermission(DEPARTMENT_MANAGE);
        Department department = getOwnedDepartment(id);
        department.setStatus(DepartmentStatus.INACTIVE);
        return ResponseMapper.department(departmentRepository.save(department));
    }

    @Transactional
    public void delete(UUID id) {
        actorContext.requirePermission(DEPARTMENT_MANAGE);
        Department department = getOwnedDepartment(id);
        UUID organizationId = actorContext.organizationId();
        boolean hasChild = departmentRepository.findAllByOrganizationId(organizationId).stream()
                .anyMatch(candidate -> candidate.getParentDepartment() != null
                        && id.equals(candidate.getParentDepartment().getId()));
        boolean hasPosition = positionRepository.findAllByOrganizationIdOrderByNameAsc(organizationId).stream()
                .anyMatch(position -> id.equals(position.getDepartment().getId()));
        boolean hasEmployee = employeeRepository.findAllByOrganizationId(organizationId).stream()
                .anyMatch(employee -> employee.getDepartment() != null && id.equals(employee.getDepartment().getId()));
        if (hasChild || hasPosition || hasEmployee) {
            throw new ConflictException("Department is in use and cannot be deleted");
        }
        departmentRepository.delete(department);
    }

    private Department getOwnedDepartment(UUID id) {
        return departmentRepository.findByIdAndOrganizationId(id, actorContext.organizationId())
                .orElseThrow(() -> new NotFoundException("Department not found"));
    }

    private WorkSchedule resolveSchedule(UUID scheduleId) {
        if (scheduleId == null) {
            return null;
        }
        return workScheduleRepository.findByIdAndOrganizationId(scheduleId, actorContext.organizationId())
                .orElseThrow(() -> new NotFoundException("Schedule not found"));
    }

    private Department resolveParent(UUID departmentId, UUID parentId) {
        if (parentId == null) return null;
        Department parent = departmentRepository.findByIdAndOrganizationId(parentId, actorContext.organizationId())
                .orElseThrow(() -> new NotFoundException("Parent department not found"));
        // Walk the entire ancestor chain so indirect cycles are rejected too.
        for (Department current = parent; current != null; current = current.getParentDepartment()) {
            if (departmentId.equals(current.getId())) {
                throw new ConflictException("Department parent relationship would create a cycle");
            }
        }
        return parent;
    }
}
