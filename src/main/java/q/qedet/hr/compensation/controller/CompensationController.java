package q.qedet.hr.compensation.controller;

import org.springframework.web.bind.annotation.*;
import q.qedet.hr.compensation.entity.*;
import q.qedet.hr.compensation.service.CompensationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/hr/compensation")
public class CompensationController {
    private final CompensationService service;
    public CompensationController(CompensationService service) { this.service = service; }
    @PostMapping("/employees/{employeeId}/salary")
    public EmployeeCompensation setSalary(@PathVariable UUID employeeId, @RequestParam BigDecimal salary,
                                          @RequestParam BigDecimal overtimeRate,
                                          @RequestParam(required = false) LocalDate effectiveFrom,
                                          @RequestParam(required = false) String reason) {
        return service.setSalary(employeeId, salary, overtimeRate, effectiveFrom, reason);
    }
    @PostMapping("/employees/{employeeId}/allowances")
    public CompensationAllowance addAllowance(@PathVariable UUID employeeId, @RequestParam String name,
                                              @RequestParam BigDecimal amount,
                                              @RequestParam(required = false) LocalDate effectiveFrom) {
        return service.addAllowance(employeeId, name, amount, effectiveFrom);
    }
    @PostMapping("/allowances/{id}/remove") public CompensationAllowance removeAllowance(@PathVariable UUID id) {
        return service.removeAllowance(id);
    }
    @PostMapping("/employees/{employeeId}/benefits")
    public EmployeeBenefit assignBenefit(@PathVariable UUID employeeId, @RequestParam String benefitCode,
                                         @RequestParam(required = false) String description,
                                         @RequestParam(required = false) LocalDate effectiveFrom) {
        return service.assignBenefit(employeeId, benefitCode, description, effectiveFrom);
    }
    @PostMapping("/benefits/{id}/remove") public EmployeeBenefit removeBenefit(@PathVariable UUID id) {
        return service.removeBenefit(id);
    }
}
