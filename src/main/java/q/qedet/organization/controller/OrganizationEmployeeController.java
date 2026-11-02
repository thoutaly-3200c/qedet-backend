package q.qedet.organization.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import q.qedet.employee.dto.EmployeeInvitationRequest;
import q.qedet.employee.dto.EmployeeResponse;
import q.qedet.employee.service.EmployeeService;

@RestController
@RequestMapping("/api/v1/organizations/employees")
public class OrganizationEmployeeController {
    private final EmployeeService employeeService;

    public OrganizationEmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/invitations")
    public EmployeeResponse invite(@Valid @RequestBody EmployeeInvitationRequest request) {
        return employeeService.inviteNew(request);
    }
}
