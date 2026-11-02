package q.qedet.organization.policy.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.organization.policy.dto.PolicyDtos.*;
import q.qedet.organization.policy.service.PolicyService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/organization/policies")
public class PolicyController {
    private final PolicyService service;

    public PolicyController(PolicyService service) {
        this.service = service;
    }

    @GetMapping("/hr/attendance")
    public AttendancePolicyResponse attendance(@RequestParam(required = false) LocalDate effectiveDate) {
        return service.attendance(effectiveDate);
    }

    @RequestMapping(value = "/hr/attendance", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public AttendancePolicyResponse attendance(@Valid @RequestBody AttendancePolicyRequest request) {
        return service.attendance(request);
    }

    @GetMapping("/hr/leave")
    public LeavePolicyResponse leave(@RequestParam(required = false) LocalDate effectiveDate) {
        return service.leave(effectiveDate);
    }

    @RequestMapping(value = "/hr/leave", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public LeavePolicyResponse leave(@Valid @RequestBody LeavePolicyRequest request) {
        return service.leave(request);
    }

    @GetMapping("/payroll")
    public PayrollPolicyResponse payroll(@RequestParam(required = false) LocalDate effectiveDate) {
        return service.payroll(effectiveDate);
    }

    @RequestMapping(value = "/payroll", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public PayrollPolicyResponse payroll(@Valid @RequestBody PayrollPolicyRequest request) {
        return service.payroll(request);
    }
}
