package q.qedet.hr.payroll.controller;

import org.springframework.web.bind.annotation.*;
import q.qedet.hr.payroll.entity.*;
import q.qedet.hr.payroll.model.*;
import q.qedet.hr.payroll.service.PayrollService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/hr/payroll")
public class PayrollController {
    private final PayrollService service;
    public PayrollController(PayrollService service) { this.service = service; }

    @PostMapping("/periods")
    public PayrollPeriod createPeriod(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return service.createPeriod(startDate, endDate);
    }
    @PostMapping("/periods/{id}/open") public PayrollPeriod open(@PathVariable UUID id) { return service.open(id); }
    @PostMapping("/periods/{id}/close") public PayrollPeriod close(@PathVariable UUID id) { return service.close(id); }
    @PostMapping("/periods/{id}/finalize") public PayrollPeriod finalizePeriod(@PathVariable UUID id) { return service.finalizePeriod(id); }

    @PostMapping("/items")
    public PayrollItem createItem(@RequestParam UUID periodId, @RequestParam UUID employeeId,
                                  @RequestParam PayrollItemType type, @RequestParam PayrollItemDirection direction,
                                  @RequestParam BigDecimal amount, @RequestParam LocalDate effectiveDate,
                                  @RequestParam(required = false) PayrollItemSourceType sourceType,
                                  @RequestParam(required = false) UUID sourceId,
                                  @RequestParam(required = false) String description) {
        return service.createItem(periodId, employeeId, type, direction, amount, effectiveDate, sourceType, sourceId, description);
    }
    @PostMapping("/items/{id}/adjust")
    public PayrollItem adjust(@PathVariable UUID id, @RequestParam BigDecimal amount,
                              @RequestParam(required = false) String description) {
        return service.adjustItem(id, amount, description);
    }
    @PostMapping("/items/{id}/reverse") public PayrollItem reverse(@PathVariable UUID id) { return service.reverseItem(id); }
}
