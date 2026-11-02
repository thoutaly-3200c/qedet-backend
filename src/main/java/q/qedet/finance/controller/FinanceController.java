package q.qedet.finance.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.common.exception.BadRequestException;
import q.qedet.finance.dto.FinanceDtos.*;
import q.qedet.finance.dto.FinanceWorkflowDtos.*;
import q.qedet.finance.model.FinanceWorkflowTypes.*;
import q.qedet.finance.service.FinanceService;
import q.qedet.finance.service.FinanceWorkflowService;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/finance")
public class FinanceController {
    private final FinanceService service;
    private final FinanceWorkflowService workflow;
    public FinanceController(FinanceService service, FinanceWorkflowService workflow) { this.service = service; this.workflow = workflow; }
    private void organization(UUID id) { service.requireOrganization(id); }

    @GetMapping("/cash-boxes") public List<Map<String, Object>> cashBoxes(@PathVariable UUID organizationId) { organization(organizationId); return service.cashBoxes(); }
    @GetMapping("/cashboxes") public List<Map<String, Object>> cashboxes(@PathVariable UUID organizationId) { organization(organizationId); return service.cashBoxes(); }
    @GetMapping("/cash-boxes/{cashBoxId}") public Map<String, Object> cashBox(@PathVariable UUID organizationId, @PathVariable UUID cashBoxId) { organization(organizationId); return service.cashBox(cashBoxId); }
    @GetMapping("/cashboxes/{cashBoxId}/transactions") public List<Map<String, Object>> cashboxTransactions(@PathVariable UUID organizationId, @PathVariable UUID cashBoxId) { organization(organizationId); return service.transactions(cashBoxId); }
    @GetMapping("/cashboxes/me") public List<Map<String, Object>> myCashboxes(@PathVariable UUID organizationId) { organization(organizationId); return service.cashBoxes(); }
    @PutMapping("/cash-boxes/{cashBoxId}") public Map<String, Object> updateCashBox(@PathVariable UUID organizationId, @PathVariable UUID cashBoxId, @Valid @RequestBody CashBoxUpdateRequest request) { organization(organizationId); return service.updateCashBox(cashBoxId, request); }
    @PostMapping("/cash-boxes/{cashBoxId}/activate") public Map<String, Object> activateCashBox(@PathVariable UUID organizationId, @PathVariable UUID cashBoxId) { organization(organizationId); return service.status(cashBoxId, true); }
    @PostMapping("/cash-boxes/{cashBoxId}/deactivate") public Map<String, Object> deactivateCashBox(@PathVariable UUID organizationId, @PathVariable UUID cashBoxId) { organization(organizationId); return service.status(cashBoxId, false); }
    @GetMapping("/cash-boxes/{cashBoxId}/employees") public List<Map<String, Object>> assignedEmployees(@PathVariable UUID organizationId, @PathVariable UUID cashBoxId) { organization(organizationId); return service.assignedEmployees(cashBoxId); }
    @PostMapping("/cash-boxes/{cashBoxId}/employees/{employeeId}") public void assignEmployee(@PathVariable UUID organizationId, @PathVariable UUID cashBoxId, @PathVariable UUID employeeId) { organization(organizationId); service.assign(cashBoxId, employeeId); }
    @DeleteMapping("/cash-boxes/{cashBoxId}/employees/{employeeId}") public void unassignEmployee(@PathVariable UUID organizationId, @PathVariable UUID cashBoxId, @PathVariable UUID employeeId) { organization(organizationId); service.unassign(cashBoxId, employeeId); }

    @GetMapping("/transactions/{transactionId}") public Map<String, Object> transaction(@PathVariable UUID organizationId, @PathVariable UUID transactionId) { organization(organizationId); return service.transaction(transactionId); }

    @PostMapping("/payment-requests") public Map<String, Object> receivePaymentRequest(@PathVariable UUID organizationId, @Valid @RequestBody PaymentRequestCreate request) { organization(organizationId); return service.receivePaymentRequest(request); }
    @GetMapping("/payment-requests") public List<Map<String, Object>> paymentRequests(@PathVariable UUID organizationId) { organization(organizationId); return service.paymentRequests(); }
    @GetMapping("/payment-requests/{requestId}") public Map<String, Object> paymentRequest(@PathVariable UUID organizationId, @PathVariable UUID requestId) { organization(organizationId); return service.paymentRequest(requestId); }
    @PostMapping("/payment-requests/{requestId}/approve") public Map<String, Object> approvePaymentRequest(@PathVariable UUID organizationId, @PathVariable UUID requestId) { organization(organizationId); return service.approvePaymentRequest(requestId); }
    @PostMapping("/payment-requests/{requestId}/reject") public Map<String, Object> rejectPaymentRequest(@PathVariable UUID organizationId, @PathVariable UUID requestId, @Valid @RequestBody DecisionRequest request) { organization(organizationId); return service.rejectPaymentRequest(requestId, request); }
    @PostMapping("/payment-requests/{requestId}/execute") public Map<String, Object> executePaymentRequest(@PathVariable UUID organizationId, @PathVariable UUID requestId, @Valid @RequestBody ExecutePaymentRequest request) { organization(organizationId); return service.executePaymentRequest(requestId, request); }

    @PostMapping("/cash-transfers") public Map<String, Object> createTransfer(@PathVariable UUID organizationId, @Valid @RequestBody CashTransferRequest request) { organization(organizationId); return service.createTransfer(request); }
    @GetMapping("/cash-transfers") public List<Map<String, Object>> transfers(@PathVariable UUID organizationId) { organization(organizationId); return service.transfers(); }
    @GetMapping("/cash-transfers/{transferId}") public Map<String, Object> transfer(@PathVariable UUID organizationId, @PathVariable UUID transferId) { organization(organizationId); return service.transfer(transferId); }
    @PostMapping("/cash-transfers/{transferId}/approve") public Map<String, Object> approveTransfer(@PathVariable UUID organizationId, @PathVariable UUID transferId) { organization(organizationId); return service.approveTransfer(transferId); }
    @PostMapping("/cash-transfers/{transferId}/execute") public Map<String, Object> executeTransfer(@PathVariable UUID organizationId, @PathVariable UUID transferId) { organization(organizationId); return service.executeTransfer(transferId); }

    @PostMapping("/cash-boxes/{cashBoxId}/reconciliations") public Map<String, Object> reconcile(@PathVariable UUID organizationId, @PathVariable UUID cashBoxId, @Valid @RequestBody ReconciliationRequest request) { organization(organizationId); return service.reconcile(cashBoxId, request); }
    @GetMapping("/cash-boxes/{cashBoxId}/reconciliations") public List<Map<String, Object>> reconciliations(@PathVariable UUID organizationId, @PathVariable UUID cashBoxId) { organization(organizationId); return service.reconciliations(cashBoxId); }

    @PostMapping("/requests") public Map<String, Object> submitRequest(@PathVariable UUID organizationId, @Valid @RequestBody CreateRequest request) { workflow.requireOrganization(organizationId); return workflow.submit(request); }
    @PostMapping("/financial-requests") public Map<String, Object> submitFinancialRequest(@PathVariable UUID organizationId, @Valid @RequestBody CreateFinancialRequest request) { workflow.requireOrganization(organizationId); return workflow.submitTyped(request); }
    @GetMapping("/requests") public List<Map<String, Object>> requests(@PathVariable UUID organizationId, @RequestParam(required = false) RequestStatus status, @RequestParam(required = false) String sourceDomain, @RequestParam(required = false) String type, @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, @RequestParam(required = false) String search) { workflow.requireOrganization(organizationId); return workflow.listRequests(status, sourceDomain, type, from, to, search); }
    @GetMapping("/requests/{requestId}") public Map<String, Object> request(@PathVariable UUID organizationId, @PathVariable UUID requestId) { workflow.requireOrganization(organizationId); return workflow.getRequest(requestId); }
    @PostMapping("/requests/{requestId}/approve") public Map<String, Object> approveRequest(@PathVariable UUID organizationId, @PathVariable UUID requestId, @RequestBody(required = false) Review review) { workflow.requireOrganization(organizationId); return workflow.approve(requestId, review); }
    @PostMapping("/financial-requests/{requestId}/approve") public Map<String, Object> approveFinancialRequest(@PathVariable UUID organizationId, @PathVariable UUID requestId, @Valid @RequestBody ApproveRequest request) { workflow.requireOrganization(organizationId); return workflow.approve(requestId, request); }
    @PostMapping("/requests/{requestId}/reject") public Map<String, Object> rejectRequest(@PathVariable UUID organizationId, @PathVariable UUID requestId, @Valid @RequestBody Review review) { workflow.requireOrganization(organizationId); return workflow.reject(requestId, review); }
    @PostMapping("/requests/{requestId}/cancel") public Map<String, Object> cancelRequest(@PathVariable UUID organizationId, @PathVariable UUID requestId, @RequestBody(required = false) Review review) { workflow.requireOrganization(organizationId); return workflow.cancel(requestId, review); }

    @PostMapping("/accounts") public Map<String, Object> createAccount(@PathVariable UUID organizationId, @Valid @RequestBody CreateAccount account) { workflow.requireOrganization(organizationId); return workflow.createAccount(account); }
    @GetMapping("/accounts") public List<Map<String, Object>> accounts(@PathVariable UUID organizationId, @RequestParam(required = false) AccountType type, @RequestParam(required = false) AccountStatus status, @RequestParam(required = false) UUID employeeId) { workflow.requireOrganization(organizationId); return workflow.listAccounts(type, status, employeeId); }
    @GetMapping("/accounts/{accountId}") public Map<String, Object> account(@PathVariable UUID organizationId, @PathVariable UUID accountId) { workflow.requireOrganization(organizationId); return workflow.getAccount(accountId); }
    @PatchMapping("/accounts/{accountId}") public Map<String, Object> updateAccount(@PathVariable UUID organizationId, @PathVariable UUID accountId, @Valid @RequestBody UpdateAccount input) { workflow.requireOrganization(organizationId); return workflow.updateAccount(accountId, input); }
    @GetMapping("/accounts/{accountId}/transactions") public List<Map<String, Object>> accountTransactions(@PathVariable UUID organizationId, @PathVariable UUID accountId) { workflow.requireOrganization(organizationId); return workflow.accountTransactions(accountId); }

    @PostMapping("/operations") public Map<String, Object> operation(@PathVariable UUID organizationId, @Valid @RequestBody CreateOperation input) { workflow.requireOrganization(organizationId); return workflow.execute(input); }
    @GetMapping("/financial-requests/eligible") public List<Map<String, Object>> eligibleRequests(@PathVariable UUID organizationId) { workflow.requireOrganization(organizationId); return workflow.eligibleRequests(); }
    @PostMapping("/operations/{operationId}/evidence/verify") public Map<String, Object> verifyEvidence(@PathVariable UUID organizationId, @PathVariable UUID operationId, @RequestBody(required = false) Review review) { workflow.requireOrganization(organizationId); return workflow.verifyEvidence(operationId, review); }
    @GetMapping("/operations") public List<Map<String, Object>> operations(@PathVariable UUID organizationId, @RequestParam(required = false) OperationStatus status, @RequestParam(required = false) OperationType type) { workflow.requireOrganization(organizationId); return workflow.listOperations(status, type); }
    @GetMapping("/operations/{operationId}") public Map<String, Object> operation(@PathVariable UUID organizationId, @PathVariable UUID operationId) { workflow.requireOrganization(organizationId); return workflow.getOperation(operationId); }
    @GetMapping("/transactions") public List<Map<String, Object>> transactions(@PathVariable UUID organizationId, @RequestParam(required = false) UUID cashBoxId, @RequestParam(required = false) boolean ledger) { organization(organizationId); return ledger ? workflow.transactions() : service.transactions(cashBoxId); }
    @PostMapping("/transfers") public Map<String, Object> transfer(@PathVariable UUID organizationId, @Valid @RequestBody CreateOperation input) { workflow.requireOrganization(organizationId); return workflow.execute(input); }
    @PostMapping("/transfers/verify-qr") public Map<String, Object> verifyTransfer(@PathVariable UUID organizationId, @Valid @RequestBody TransferConfirmation confirmation) { workflow.requireOrganization(organizationId); return workflow.verifyQr(confirmation.qrToken()); }
    @PostMapping("/transfers/{operationId}/confirm") public Map<String, Object> confirmTransfer(@PathVariable UUID organizationId, @PathVariable UUID operationId, @Valid @RequestBody TransferConfirmation confirmation) { workflow.requireOrganization(organizationId); return workflow.confirm(operationId, confirmation); }
    @PostMapping("/transfers/{operationId}/cancel") public Map<String, Object> cancelTransfer(@PathVariable UUID organizationId, @PathVariable UUID operationId) { workflow.requireOrganization(organizationId); return workflow.cancelTransfer(operationId); }

    @PostMapping("/payroll-payments") public Map<String, Object> payrollPayment(@PathVariable UUID organizationId, @Valid @RequestBody PayrollPayment payment) { workflow.requireOrganization(organizationId); return workflow.recordPayroll(payment); }
    @GetMapping("/payroll-payments") public List<Map<String, Object>> payrollPayments(@PathVariable UUID organizationId, @RequestParam(required = false) UUID payrollId, @RequestParam(required = false) UUID employeeId) { workflow.requireOrganization(organizationId); return workflow.payroll(payrollId, employeeId); }
    @GetMapping("/payroll-payments/{payrollId}") public List<Map<String, Object>> payrollPayment(@PathVariable UUID organizationId, @PathVariable UUID payrollId) { workflow.requireOrganization(organizationId); return workflow.payroll(payrollId, null); }
    @GetMapping("/payroll-payments/{payrollId}/payments") public List<Map<String, Object>> payrollPaymentHistory(@PathVariable UUID organizationId, @PathVariable UUID payrollId) { workflow.requireOrganization(organizationId); return workflow.payroll(payrollId, null); }
    @PostMapping("/payroll-payments/{payrollId}/payments") public Map<String, Object> payrollPaymentForPeriod(@PathVariable UUID organizationId, @PathVariable UUID payrollId, @Valid @RequestBody PayrollPayment payment) {
        workflow.requireOrganization(organizationId);
        if (!payrollId.equals(payment.payrollId())) throw new BadRequestException("Payroll ID must match the path");
        return workflow.recordPayroll(payment);
    }
    @GetMapping("/dashboard/summary") public Map<String, Object> dashboard(@PathVariable UUID organizationId, @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) { workflow.requireOrganization(organizationId); return workflow.dashboard(from, to); }
    @GetMapping("/reports/profit-and-loss") public Map<String, Object> profitAndLoss(@PathVariable UUID organizationId, @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) { workflow.requireOrganization(organizationId); return workflow.profitAndLoss(from, to); }
    @GetMapping("/reports/cash-flow") public Map<String, Object> cashFlow(@PathVariable UUID organizationId, @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) { workflow.requireOrganization(organizationId); return workflow.dashboard(from, to); }
    @GetMapping("/reports/balance-sheet") public Map<String, Object> balanceSheet(@PathVariable UUID organizationId) { workflow.requireOrganization(organizationId); return workflow.balanceSheet(); }
    @GetMapping("/accounting/summary") public Map<String, Object> accountingSummary(@PathVariable UUID organizationId) { workflow.requireOrganization(organizationId); return workflow.balanceSheet(); }
    @GetMapping("/accounting/entries") public List<Map<String, Object>> accountingEntries(@PathVariable UUID organizationId) { workflow.requireOrganization(organizationId); return workflow.accountingEntries(); }
}
