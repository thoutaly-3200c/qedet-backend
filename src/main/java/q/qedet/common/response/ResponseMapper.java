package q.qedet.common.response;



import q.qedet.auth.security.PermissionCatalog;

import q.qedet.common.model.Permission;
import q.qedet.common.model.RoleType;
import q.qedet.employee.dto.EmployeeResponse;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.dto.DepartmentResponse;
import q.qedet.organization.entity.Department;



import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static EmployeeResponse employee(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getOrganization().getId(),
                employee.getUser() == null ? null : employee.getUser().getId(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName(),
                fullName(employee),
                employee.getEmail(),
                employee.getPhone(),
                employee.getDepartment() == null ? null : employee.getDepartment().getId(),
                employee.getPosition() == null ? null : new EmployeeResponse.PositionReference(
                        employee.getPosition().getId(), employee.getPosition().getCode(), employee.getPosition().getName()),
                employee.getDepartment() == null ? null : new EmployeeResponse.DepartmentReference(
                        employee.getDepartment().getId(), employee.getDepartment().getName()),
                employee.getSite() == null ? null : new EmployeeResponse.LocationReference(
                        employee.getSite().getId(), employee.getSite().getCode(), employee.getSite().getName()),
                employee.getRole(),
                employee.getStatus(),
                hireDate(employee),
                employee.getSalary(),
                employee.getStatus() == q.qedet.common.model.EmployeeStatus.ACTIVE,
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    public static DepartmentResponse department(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getOrganization().getId(),
                department.getName(),
                department.getCode(),
                department.getDescription(),
                department.getParentDepartment() == null ? null : department.getParentDepartment().getId(),
                department.getSchedule() == null ? null : department.getSchedule().getId(),
                department.getSchedule() == null ? null : department.getSchedule().getName(),
                department.getStatus(),
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }

    public static Set<String> permissionNames(RoleType roleType) {
        return PermissionCatalog.permissionsFor(roleType)
                .stream()
                .map(Permission::name)
                .collect(Collectors.toSet());
    }

    private static String fullName(Employee employee) {
        String firstName = employee.getFirstName() == null ? "" : employee.getFirstName();
        String lastName = employee.getLastName() == null ? "" : employee.getLastName();
        if (firstName.isBlank()) {
            return lastName;
        }
        if (lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    private static LocalDate hireDate(Employee employee) {
        return employee.getJoinedAt() == null ? null : employee.getJoinedAt().atZone(ZoneOffset.UTC).toLocalDate();
    }
}
