package q.qedet.employee.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import q.qedet.employee.dto.InviteRequest;
import q.qedet.employee.dto.EmployeeRequest;
import q.qedet.employee.dto.EmployeeResponse;
import q.qedet.employee.dto.LifecycleRequest;
import q.qedet.employee.dto.TransferRequest;
import q.qedet.employee.dto.TerminationRequest;
import q.qedet.employee.dto.DepartmentAssignmentRequest;
import q.qedet.employee.dto.RoleAssignmentRequest;
import q.qedet.employee.dto.EmployeePositionAssignmentRequest;
import q.qedet.employee.service.EmployeeService;


import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }


    @GetMapping
    public List<EmployeeResponse> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int limit,
                                       @RequestParam(required = false) String search,
                                       @RequestParam(required = false) UUID departmentId,
                                       @RequestParam(required = false) String status) {
        return employeeService.list(page, limit, search, departmentId, status);
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable UUID id) {
        return employeeService.get(id);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable UUID id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @PostMapping("/{id}/invite")
    public Map<String, String> invite(@PathVariable UUID id, @RequestBody(required = false) InviteRequest request) {
        employeeService.invite(id, request);
        return Map.of("message", "Invitation sent successfully");
    }

    @PostMapping("/{id}/activate")
    public EmployeeResponse activate(@PathVariable UUID id, @Valid @RequestBody(required = false) LifecycleRequest request) {
        return employeeService.activate(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public EmployeeResponse deactivate(@PathVariable UUID id, @Valid @RequestBody(required = false) LifecycleRequest request) {
        return employeeService.deactivate(id, request);
    }

    @PostMapping("/{id}/transfer")
    public EmployeeResponse transfer(@PathVariable UUID id, @Valid @RequestBody TransferRequest request) {
        return employeeService.transfer(id, request);
    }

    @PostMapping("/{id}/terminate")
    public EmployeeResponse terminate(@PathVariable UUID id, @Valid @RequestBody(required = false) TerminationRequest request) {
        return employeeService.terminate(id, request);
    }

    @PostMapping("/{id}/departments")
    public void assignDepartment(@PathVariable UUID id, @Valid @RequestBody DepartmentAssignmentRequest request) {
        employeeService.assignDepartment(id, request);
    }

    @PostMapping("/{id}/position-assignment")
    public EmployeeResponse assignPosition(@PathVariable UUID id,
                                           @Valid @RequestBody EmployeePositionAssignmentRequest request) {
        return employeeService.assignPosition(id, request);
    }

    @DeleteMapping("/department-assignments/{assignmentId}")
    public void endDepartment(@PathVariable UUID assignmentId) {
        employeeService.endDepartment(assignmentId);
    }

    @PostMapping("/{id}/roles")
    public void assignRole(@PathVariable UUID id, @Valid @RequestBody RoleAssignmentRequest request) {
        employeeService.assignRole(id, request);
    }

    @DeleteMapping("/role-assignments/{assignmentId}")
    public void endRole(@PathVariable UUID assignmentId) {
        employeeService.endRole(assignmentId);
    }
}
