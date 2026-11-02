package q.qedet.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.finance.dto.FinanceWorkflowDtos.*;
import q.qedet.finance.entity.*;
import q.qedet.finance.model.FinanceWorkflowTypes.*;
import q.qedet.finance.repository.*;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.hr.payroll.repository.PayrollPeriodRepository;
import q.qedet.procurement.repository.PurchaseOrderRepository;
import q.qedet.procurement.repository.SupplierRepository;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;
import java.math.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
public class FinanceWorkflowService {
    private final ActorContext actor; private final OrganizationRepository organizations; private final FinanceRequestRepository requests;
    private final FinanceRequestAttachmentRepository requestAttachments; private final FinanceRequestEventRepository requestEvents; private final FinanceAccountRepository accounts;
    private final FinanceOperationRepository operations; private final FinanceOperationAttachmentRepository operationAttachments; private final FinanceLedgerEntryRepository ledger;
    private final FinanceTransferConfirmationRepository confirmations; private final FinancePayrollPaymentRepository payrollPayments;
    private final EmployeeRepository employees; private final PayrollPeriodRepository payrollPeriods;
    private final FinanceRequestDetailRepository requestDetails; private final SupplierRepository suppliers; private final PurchaseOrderRepository purchaseOrders;

    public FinanceWorkflowService(ActorContext actor, OrganizationRepository organizations, FinanceRequestRepository requests,
                                  FinanceRequestAttachmentRepository requestAttachments, FinanceRequestEventRepository requestEvents, FinanceAccountRepository accounts,
                                  FinanceOperationRepository operations, FinanceOperationAttachmentRepository operationAttachments, FinanceLedgerEntryRepository ledger,
                                  FinanceTransferConfirmationRepository confirmations, FinancePayrollPaymentRepository payrollPayments,
                                  EmployeeRepository employees, PayrollPeriodRepository payrollPeriods, FinanceRequestDetailRepository requestDetails,
                                  SupplierRepository suppliers, PurchaseOrderRepository purchaseOrders) {
        this.actor = actor; this.organizations = organizations; this.requests = requests; this.requestAttachments = requestAttachments;
        this.requestEvents = requestEvents; this.accounts = accounts; this.operations = operations;
        this.operationAttachments = operationAttachments; this.ledger = ledger; this.confirmations = confirmations; this.payrollPayments = payrollPayments;
        this.employees = employees; this.payrollPeriods = payrollPeriods; this.requestDetails = requestDetails;
        this.suppliers = suppliers; this.purchaseOrders = purchaseOrders;
    }

    public void requireOrganization(UUID organizationId) { if (!actor.organizationId().equals(organizationId)) throw new NotFoundException("Organization not found"); }
    private UUID orgId() { return actor.organizationId(); }
    private Organization organization() { return organizations.findById(orgId()).orElseThrow(() -> new NotFoundException("Organization not found")); }
    private void view() { actor.requirePermission(Permission.FINANCE_VIEW); }
    private void manage() { actor.requirePermission(Permission.FINANCE_MANAGE); }
    private BigDecimal amount(BigDecimal value) { if (value == null || value.signum() <= 0) throw new BadRequestException("Amount must be positive"); return value.setScale(2, RoundingMode.HALF_UP); }
    private String currency(String value) { if (value == null || !value.matches("(?i)[a-z]{3}")) throw new BadRequestException("Currency must be a three-letter code"); return value.toUpperCase(Locale.ROOT); }
    private <T extends Enum<T>> T enumValue(Class<T> type, String value, String field) { try { return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT)); } catch (Exception ex) { throw new BadRequestException("Invalid " + field); } }
    private FinanceRequest request(UUID id) { return requests.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Finance request not found")); }
    private FinanceRequest lockedRequest(UUID id) { return requests.findLockedByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Finance request not found")); }
    private FinanceOperation operation(UUID id) { return operations.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Finance operation not found")); }
    private FinanceOperation lockedOperation(UUID id) { return operations.findLockedByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Finance operation not found")); }
    private FinanceAccount account(UUID id) { return accounts.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Finance account not found")); }
    private FinanceAccount lockedAccount(UUID id) { return accounts.findLockedByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Finance account not found")); }

    @Transactional
    public Map<String, Object> submit(CreateRequest input) {
        actor.requirePermission(Permission.PAYMENT_REQUEST_CREATE);
        FinanceRequest entity = new FinanceRequest(); entity.setOrganization(organization()); entity.setSourceDomain(input.sourceDomain().trim());
        entity.setSourceRecordId(input.sourceRecordId().trim()); entity.setRequestType(input.type().trim().toUpperCase(Locale.ROOT));
        entity.setAmount(amount(input.amount())); entity.setCurrency(currency(input.currency())); entity.setReason(input.reason().trim());
        entity.setNotes(input.notes()); entity.setSupportingReference(input.supportingReference()); entity.setDueDate(input.dueDate()); entity.setRequesterId(actor.employeeId()); entity.setRequestNumber(nextRequestNumber());
        FinanceRequest saved = requests.save(entity); saveRequestAttachments(saved, input.attachments()); event(saved, null, RequestStatus.PENDING, "SUBMITTED", input.notes());
        return requestView(saved, true);
    }
    @Transactional
    public Map<String, Object> submitSupplierReturnRefund(UUID operationId, BigDecimal amount, String currency, String reference, String notes, String supportingReference) {
        actor.requirePermission(Permission.PROCUREMENT_MANAGE);
        FinanceRequest entity = new FinanceRequest();
        entity.setOrganization(organization()); entity.setSourceDomain("SUPPLIER_RETURN"); entity.setSourceRecordId(operationId.toString());
        entity.setRequestType(FinancialRequestType.REFUND_IN.name()); entity.setAmount(amount(amount)); entity.setCurrency(currency(currency));
        entity.setReason("Supplier return " + reference); entity.setNotes(notes); entity.setSupportingReference(supportingReference);
        entity.setRequesterId(actor.employeeId()); entity.setRequestNumber(nextRequestNumber());
        FinanceRequest saved = requests.save(entity); event(saved, null, RequestStatus.PENDING, "SUBMITTED", notes);
        return requestView(saved, true);
    }
    @Transactional
    public Map<String, Object> submitTyped(CreateFinancialRequest input) {
        actor.requirePermission(Permission.PAYMENT_REQUEST_CREATE);
        FinancialRequestType type = enumValue(FinancialRequestType.class, input.type(), "financial request type");
        validateTypedRequest(type, input.details(), input.requestedAmount(), currency(input.currency()));
        FinanceRequest request = new FinanceRequest();
        request.setOrganization(organization()); request.setRequestNumber(nextRequestNumber()); request.setRequestType(type.name());
        request.setSourceDomain(input.sourceType() == null || input.sourceType().isBlank() ? "MANUAL" : input.sourceType().trim());
        request.setSourceRecordId(input.sourceId() == null || input.sourceId().isBlank() ? request.getRequestNumber() : input.sourceId().trim());
        request.setAmount(amount(input.requestedAmount())); request.setCurrency(currency(input.currency()));
        request.setReason(input.reason().trim()); request.setNotes(input.notes()); request.setDueDate(input.dueDate()); request.setRequesterId(actor.employeeId());
        FinanceRequest saved = requests.save(request);
        saveDetails(saved, input.details()); saveRequestAttachments(saved, input.attachments());
        event(saved, null, RequestStatus.PENDING, "SUBMITTED", input.notes());
        return requestView(saved, true);
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRequests(RequestStatus status, String sourceDomain, String type, LocalDate from, LocalDate to, String search) {
        view();
        return requests.findAllByOrganizationIdOrderByCreatedAtDesc(orgId()).stream().filter(r -> status == null || r.getStatus() == status)
                .filter(r -> sourceDomain == null || r.getSourceDomain().equalsIgnoreCase(sourceDomain))
                .filter(r -> type == null || r.getRequestType().equalsIgnoreCase(type))
                .filter(r -> from == null || !r.getCreatedAt().isBefore(from.atStartOfDay(ZoneOffset.UTC).toInstant()))
                .filter(r -> to == null || !r.getCreatedAt().isAfter(to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()))
                .filter(r -> search == null || search.isBlank() || (r.getReason() + " " + r.getSourceRecordId()).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                .map(r -> requestView(r, false)).toList();
    }
    @Transactional(readOnly = true) public Map<String, Object> getRequest(UUID id) { view(); return requestView(request(id), true); }
    @Transactional public Map<String, Object> approve(UUID id, Review review) {
        return approve(id, new ApproveRequest(request(id).getAmount(), review == null ? null : review.notes()));
    }
    @Transactional public Map<String, Object> approve(UUID id, ApproveRequest input) {
        manage(); FinanceRequest r = lockedRequest(id);
        if (r.getRequesterId().equals(actor.employeeId())) throw new ForbiddenException("Requesters cannot approve their own financial requests");
        BigDecimal approved = amount(input.approvedAmount());
        if (approved.compareTo(r.getAmount()) > 0) throw new BadRequestException("Approved amount cannot exceed requested amount");
        r.setApprovedAmount(approved);
        transition(r, RequestStatus.APPROVED, "APPROVED", input.notes());
        return requestView(r, true);
    }
    @Transactional public Map<String, Object> reject(UUID id, Review review) {
        manage(); if (review == null || review.notes() == null || review.notes().isBlank()) throw new BadRequestException("Rejection reason is required");
        FinanceRequest r = request(id); transition(r, RequestStatus.REJECTED, "REJECTED", review.notes()); r.setRejectionReason(review.notes().trim()); requests.save(r); return requestView(r, true);
    }
    @Transactional public Map<String, Object> cancel(UUID id, Review review) {
        FinanceRequest r = lockedRequest(id); if (!r.getRequesterId().equals(actor.employeeId())) manage();
        if (r.getStatus() != RequestStatus.PENDING && r.getStatus() != RequestStatus.APPROVED) throw new ConflictException("Request cannot be cancelled");
        if (completedAmount(r).signum() > 0) throw new ConflictException("A request with completed transactions cannot be cancelled");
        operations.findAllByFinanceRequestIdAndOrganizationIdAndStatus(r.getId(), orgId(), OperationStatus.PENDING).forEach(operation -> {
            operation.setStatus(OperationStatus.CANCELLED); operations.save(operation);
            confirmations.findByOperationIdAndOrganizationId(operation.getId(), orgId()).ifPresent(confirmation -> {
                confirmation.setStatus(ConfirmationStatus.CANCELLED); confirmations.save(confirmation);
            });
        });
        RequestStatus old = r.getStatus(); r.setStatus(RequestStatus.CANCELLED); r.setCancelledBy(actor.employeeId()); r.setCancelledAt(Instant.now()); r.setCancellationReason(review == null ? null : review.notes()); requests.save(r); event(r, old, RequestStatus.CANCELLED, "CANCELLED", review == null ? null : review.notes()); return requestView(r, true);
    }

    @Transactional public Map<String, Object> createAccount(CreateAccount input) {
        manage(); FinanceAccount a = new FinanceAccount(); a.setOrganization(organization()); a.setType(enumValue(AccountType.class, input.type(), "account type"));
        a.setName(input.name().trim()); a.setLocationId(input.locationId()); a.setCurrency(currency(input.currency())); a.setAssignedEmployeeId(input.assignedEmployeeId()); return accountView(accounts.save(a));
    }
    @Transactional(readOnly = true) public List<Map<String, Object>> listAccounts(AccountType type, AccountStatus status, UUID employeeId) {
        view(); return accounts.findAllByOrganizationIdOrderByNameAsc(orgId()).stream().filter(a -> type == null || a.getType() == type).filter(a -> status == null || a.getStatus() == status)
                .filter(a -> employeeId == null || employeeId.equals(a.getAssignedEmployeeId())).map(this::accountView).toList();
    }
    @Transactional(readOnly = true) public Map<String, Object> getAccount(UUID id) { view(); return accountView(account(id)); }
    @Transactional public Map<String, Object> updateAccount(UUID id, UpdateAccount input) {
        manage(); FinanceAccount account = account(id);
        if (input.name() != null && !input.name().isBlank()) account.setName(input.name().trim());
        if (input.status() != null && !input.status().isBlank()) account.setStatus(enumValue(AccountStatus.class, input.status(), "account status"));
        if (input.assignedEmployeeId() != null) account.setAssignedEmployeeId(input.assignedEmployeeId());
        if (input.locationId() != null) account.setLocationId(input.locationId());
        return accountView(accounts.save(account));
    }
    @Transactional(readOnly = true) public List<Map<String, Object>> accountTransactions(UUID id) { view(); account(id); return ledger.findAllByAccountIdAndOrganizationIdOrderByPostedAtDesc(id, orgId()).stream().map(this::entryView).toList(); }

    @Transactional public Map<String, Object> execute(CreateOperation input) {
        manage(); Optional<FinanceOperation> existing = operations.findByOrganizationIdAndIdempotencyKey(orgId(), input.idempotencyKey());
        if (existing.isPresent()) return operationView(existing.get(), true);
        OperationType type = enumValue(OperationType.class, input.type(), "operation type);");
        validateAccounts(type, input.sourceAccountId(), input.destinationAccountId(), currency(input.currency()), amount(input.amount()));
        FinanceOperation op = new FinanceOperation(); op.setOrganization(organization()); op.setType(type); op.setSourceAccountId(input.sourceAccountId()); op.setDestinationAccountId(input.destinationAccountId());
        op.setAmount(amount(input.amount())); op.setCurrency(currency(input.currency())); op.setReference(input.reference()); op.setDescription(input.description()); op.setNotes(input.notes()); op.setFinanceRequestId(input.requestId()); op.setCreatedBy(actor.employeeId()); op.setIdempotencyKey(input.idempotencyKey().trim());
        linkApprovedRequest(op); setEvidenceRequirement(op); FinanceOperation saved = operations.save(op); saveOperationAttachments(saved, input.attachments());
        if (type == OperationType.CASH_TRANSFER) return initiateTransfer(saved);
        if (saved.getEvidenceStatus() == EvidenceStatus.NOT_REQUIRED) post(saved);
        return operationView(saved, true);
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> eligibleRequests() {
        view();
        return requests.findAllByOrganizationIdOrderByCreatedAtDesc(orgId()).stream()
                .filter(r -> r.getStatus() == RequestStatus.APPROVED && remainingAmount(r).signum() > 0)
                .map(r -> requestView(r, true)).toList();
    }
    @Transactional
    public Map<String, Object> verifyEvidence(UUID operationId, Review review) {
        manage();
        FinanceOperation operation = operation(operationId);
        if (operation.getEvidenceStatus() != EvidenceStatus.PENDING) throw new ConflictException("Operation does not have pending evidence");
        if (operationAttachments.findAllByOperationIdOrderByCreatedAtAsc(operationId).isEmpty()) throw new BadRequestException("Supporting evidence is required");
        operation.setEvidenceStatus(EvidenceStatus.VERIFIED); operation.setEvidenceVerifiedBy(actor.employeeId()); operation.setEvidenceVerifiedAt(Instant.now());
        operations.save(operation); post(operation);
        return operationView(operation, true);
    }
    @Transactional(readOnly = true) public List<Map<String, Object>> listOperations(OperationStatus status, OperationType type) {
        view(); return operations.findAllByOrganizationIdOrderByCreatedAtDesc(orgId()).stream().filter(o -> status == null || o.getStatus() == status).filter(o -> type == null || o.getType() == type).map(o -> operationView(o, false)).toList();
    }
    @Transactional(readOnly = true) public Map<String, Object> getOperation(UUID id) { view(); return operationView(operation(id), true); }
    @Transactional public Map<String, Object> verifyQr(String token) {
        actor.requirePermission(Permission.FINANCE_CASH_TRANSFER); FinanceTransferConfirmation c = confirmations.findByQrTokenAndOrganizationId(token, orgId()).orElseThrow(() -> new NotFoundException("Transfer token not found"));
        if (c.getStatus() != ConfirmationStatus.PENDING || c.getExpiresAt().isBefore(Instant.now())) { if (c.getStatus() == ConfirmationStatus.PENDING) { c.setStatus(ConfirmationStatus.EXPIRED); confirmations.save(c); } throw new ConflictException("Transfer token is not valid"); }
        if (c.getReceiverId() == null || c.getReceiverId().equals(actor.employeeId())) throw new ForbiddenException("The receiver cannot scan their own transfer session");
        c.setSenderId(actor.employeeId()); c.setStatus(ConfirmationStatus.VERIFIED); c.setVerifiedAt(Instant.now()); confirmations.save(c);
        FinanceOperation operation = operation(c.getOperationId());
        return Map.of("operationId", c.getOperationId(), "status", c.getStatus(), "expiresAt", c.getExpiresAt(), "amount", operation.getAmount(), "currency", operation.getCurrency(), "requestId", operation.getFinanceRequestId());
    }
    @Transactional public Map<String, Object> confirm(UUID operationId, TransferConfirmation input) {
        actor.requirePermission(Permission.FINANCE_CASH_TRANSFER); FinanceOperation op = operation(operationId); FinanceTransferConfirmation c = confirmations.findByOperationIdAndOrganizationId(operationId, orgId()).orElseThrow(() -> new NotFoundException("Transfer confirmation not found"));
        if (!c.getQrToken().equals(input.qrToken()) || c.getStatus() != ConfirmationStatus.VERIFIED) throw new ConflictException("Transfer must be verified before confirmation");
        if (c.getExpiresAt().isBefore(Instant.now())) { c.setStatus(ConfirmationStatus.EXPIRED); confirmations.save(c); throw new ConflictException("Transfer token has expired"); }
        if (!c.getSenderId().equals(actor.employeeId())) throw new ForbiddenException("Only the verified sender can confirm this transfer");
        post(op); c.setStatus(ConfirmationStatus.COMPLETED); c.setConfirmedAt(Instant.now()); confirmations.save(c); return operationView(op, true);
    }
    @Transactional public Map<String, Object> cancelTransfer(UUID operationId) {
        manage(); FinanceOperation op = operation(operationId); if (op.getType() != OperationType.CASH_TRANSFER || op.getStatus() != OperationStatus.PENDING) throw new ConflictException("Transfer cannot be cancelled");
        op.setStatus(OperationStatus.CANCELLED); operations.save(op); confirmations.findByOperationIdAndOrganizationId(operationId, orgId()).ifPresent(c -> { c.setStatus(ConfirmationStatus.CANCELLED); confirmations.save(c); }); return operationView(op, true);
    }
    @Transactional(readOnly = true) public List<Map<String, Object>> transactions() { return listOperations(OperationStatus.POSTED, null); }
    @Transactional(readOnly = true) public Map<String, Object> dashboard(Instant from, Instant to) {
        view();
        List<FinanceOperation> posted = postedInPeriod(from, to);
        BigDecimal receipts = total(posted, OperationType.CASH_RECEIPT);
        BigDecimal payments = total(posted, OperationType.CASH_PAYMENT);
        BigDecimal cashBalance = accounts.findAllByOrganizationIdOrderByNameAsc(orgId()).stream().map(a -> balance(a.getId())).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from); result.put("to", to); result.put("receipts", receipts); result.put("payments", payments);
        result.put("netCashFlow", receipts.subtract(payments)); result.put("cashBalance", cashBalance);
        result.put("postedOperations", posted.size()); result.put("pendingRequests", requests.findAllByOrganizationIdOrderByCreatedAtDesc(orgId()).stream().filter(r -> r.getStatus() == RequestStatus.PENDING).count());
        return result;
    }
    @Transactional(readOnly = true) public Map<String, Object> profitAndLoss(Instant from, Instant to) {
        actor.requirePermission(Permission.FINANCIAL_REPORT_VIEW);
        List<FinanceOperation> posted = postedInPeriod(from, to);
        BigDecimal revenue = total(posted, OperationType.CASH_RECEIPT);
        BigDecimal expenses = total(posted, OperationType.CASH_PAYMENT);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from); result.put("to", to); result.put("revenue", revenue); result.put("expenses", expenses); result.put("netIncome", revenue.subtract(expenses));
        return result;
    }
    @Transactional(readOnly = true) public Map<String, Object> balanceSheet() {
        actor.requirePermission(Permission.FINANCIAL_REPORT_VIEW);
        List<Map<String, Object>> accountBalances = accounts.findAllByOrganizationIdOrderByNameAsc(orgId()).stream().map(this::accountView).toList();
        BigDecimal assets = accountBalances.stream().map(a -> (BigDecimal) a.get("balance")).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assets", assets); result.put("liabilities", BigDecimal.ZERO.setScale(2)); result.put("equity", assets); result.put("accounts", accountBalances);
        return result;
    }
    @Transactional(readOnly = true) public List<Map<String, Object>> accountingEntries() {
        view();
        return operations.findAllByOrganizationIdOrderByCreatedAtDesc(orgId()).stream().filter(o -> o.getStatus() == OperationStatus.POSTED).flatMap(o -> ledger.findAllByOperationIdOrderByCreatedAtAsc(o.getId()).stream()).map(this::entryView).toList();
    }
    private List<FinanceOperation> postedInPeriod(Instant from, Instant to) {
        return operations.findAllByOrganizationIdOrderByCreatedAtDesc(orgId()).stream().filter(o -> o.getStatus() == OperationStatus.POSTED)
                .filter(o -> from == null || !o.getExecutedAt().isBefore(from)).filter(o -> to == null || !o.getExecutedAt().isAfter(to)).toList();
    }
    private BigDecimal total(List<FinanceOperation> operations, OperationType type) {
        return operations.stream().filter(o -> o.getType() == type).map(FinanceOperation::getAmount).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    @Transactional public Map<String, Object> recordPayroll(PayrollPayment input) {
        actor.requirePermission(Permission.FINANCE_PAYROLL_PAYMENT);
        payrollPeriods.findByIdAndOrganizationId(input.payrollId(), orgId()).orElseThrow(() -> new NotFoundException("Payroll period not found"));
        employees.findByIdAndOrganizationId(input.employeeId(), orgId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        CreateOperation operationInput = new CreateOperation("CASH_PAYMENT", input.sourceAccountId(), null, input.amount(), input.currency(), "PAYROLL:" + input.payrollId(), "Payroll payment", null, null, input.idempotencyKey(), List.of());
        FinanceOperation operation = operationEntityForPayroll(operationInput);
        Optional<FinancePayrollPayment> existing = payrollPayments.findByOperationId(operation.getId());
        if (existing.isPresent()) return payrollView(existing.get());
        post(operation);
        FinancePayrollPayment payment = new FinancePayrollPayment(); payment.setOrganization(organization()); payment.setPayrollId(input.payrollId()); payment.setEmployeeId(input.employeeId()); payment.setAmount(operation.getAmount()); payment.setCurrency(operation.getCurrency()); payment.setSourceAccountId(input.sourceAccountId()); payment.setOperationId(operation.getId()); payment.setReceiverVerificationStatus("PENDING"); payment.setFinanceActorId(actor.employeeId()); payment.setPaidAt(Instant.now());
        return payrollView(payrollPayments.save(payment));
    }
    @Transactional(readOnly = true) public List<Map<String, Object>> payroll(UUID payrollId, UUID employeeId) { view(); return payrollPayments.findAllByOrganizationIdOrderByPaidAtDesc(orgId()).stream().filter(p -> payrollId == null || payrollId.equals(p.getPayrollId())).filter(p -> employeeId == null || employeeId.equals(p.getEmployeeId())).map(this::payrollView).toList(); }

    private FinanceOperation operationEntityForPayroll(CreateOperation input) {
        Optional<FinanceOperation> existing = operations.findByOrganizationIdAndIdempotencyKey(orgId(), input.idempotencyKey()); if (existing.isPresent()) return existing.get();
        validateAccounts(OperationType.CASH_PAYMENT, input.sourceAccountId(), null, currency(input.currency()), amount(input.amount()));
        FinanceOperation o = new FinanceOperation(); o.setOrganization(organization()); o.setType(OperationType.CASH_PAYMENT); o.setSourceAccountId(input.sourceAccountId()); o.setAmount(amount(input.amount())); o.setCurrency(currency(input.currency())); o.setReference(input.reference()); o.setDescription(input.description()); o.setCreatedBy(actor.employeeId()); o.setIdempotencyKey(input.idempotencyKey()); return operations.save(o);
    }
    private void validateAccounts(OperationType type, UUID source, UUID destination, String currency, BigDecimal value) {
        if (type == OperationType.CASH_RECEIPT) { if (destination == null || source != null) throw new BadRequestException("Cash receipt requires only a destination account"); }
        else if (type == OperationType.CASH_PAYMENT) { if (source == null || destination != null) throw new BadRequestException("Cash payment requires only a source account"); }
        else if (source == null || destination == null || source.equals(destination)) throw new BadRequestException("Transfer requires distinct source and destination accounts");
        if (source != null) validateAccount(account(source), currency, value, true); if (destination != null) validateAccount(account(destination), currency, value, false);
    }
    private void validateAccount(FinanceAccount account, String currency, BigDecimal value, boolean debit) {
        if (account.getStatus() != AccountStatus.ACTIVE) throw new ConflictException("Account is not active"); if (!account.getCurrency().equals(currency)) throw new BadRequestException("Account currency does not match operation currency");
        if (debit && balance(account.getId()).compareTo(value) < 0) throw new ConflictException("Insufficient available funds");
    }
    private Map<String, Object> initiateTransfer(FinanceOperation op) {
        FinanceTransferConfirmation c = new FinanceTransferConfirmation(); c.setOrganization(organization()); c.setOperationId(op.getId()); c.setReceiverId(actor.employeeId()); c.setQrToken(secureToken()); c.setQrReference(UUID.randomUUID().toString()); c.setExpiresAt(Instant.now().plus(Duration.ofHours(24))); confirmations.save(c);
        Map<String, Object> result = operationView(op, true); result.put("transfer", Map.of("status", c.getStatus(), "qrToken", c.getQrToken(), "expiresAt", c.getExpiresAt())); return result;
    }
    private void post(FinanceOperation op) {
        if (op.getStatus() == OperationStatus.POSTED) return; if (op.getStatus() != OperationStatus.PENDING) throw new ConflictException("Operation cannot be posted");
        FinanceAccount source = op.getSourceAccountId() == null ? null : lockedAccount(op.getSourceAccountId()); FinanceAccount destination = op.getDestinationAccountId() == null ? null : lockedAccount(op.getDestinationAccountId());
        if (source != null) validateAccount(source, op.getCurrency(), op.getAmount(), true); if (destination != null) validateAccount(destination, op.getCurrency(), op.getAmount(), false);
        Instant now = Instant.now(); if (source != null) entry(op, source.getId(), LedgerDirection.CREDIT, now); if (destination != null) entry(op, destination.getId(), LedgerDirection.DEBIT, now);
        op.setStatus(OperationStatus.POSTED); op.setExecutedAt(now); op.setConfirmedAt(now); operations.save(op);
        if (op.getFinanceRequestId() != null) {
            FinanceRequest r = lockedRequest(op.getFinanceRequestId());
            if (r.getStatus() != RequestStatus.APPROVED) throw new ConflictException("Linked request is not approved");
            validateRefundLimit(r, op);
            updateFulfillment(r, op.getId());
        }
    }
    private void entry(FinanceOperation operation, UUID accountId, LedgerDirection direction, Instant at) { FinanceLedgerEntry e = new FinanceLedgerEntry(); e.setOrganization(organization()); e.setOperationId(operation.getId()); e.setAccountId(accountId); e.setDirection(direction); e.setAmount(operation.getAmount()); e.setCurrency(operation.getCurrency()); e.setReference(operation.getReference()); e.setPostedAt(at); ledger.save(e); }
    private void linkApprovedRequest(FinanceOperation op) {
        if (op.getFinanceRequestId() == null) return;
        FinanceRequest r = lockedRequest(op.getFinanceRequestId());
        if (r.getStatus() != RequestStatus.APPROVED) throw new ConflictException("Linked request is not approved");
        if (!r.getCurrency().equals(op.getCurrency())) throw new BadRequestException("Operation currency must match the approved request");
        if (op.getAmount().compareTo(remainingAmount(r)) > 0) throw new BadRequestException("Operation exceeds the approved request remaining amount");
        FinancialRequestType type = enumValue(FinancialRequestType.class, r.getRequestType(), "financial request type");
        if (type == FinancialRequestType.REFUND_IN && op.getType() != OperationType.CASH_RECEIPT) throw new BadRequestException("Refund-in requests require a receipt operation");
        if ((type == FinancialRequestType.SUPPLIER_PAYMENT || type == FinancialRequestType.OTHER_PAYMENT || type == FinancialRequestType.REFUND_OUT) && op.getType() != OperationType.CASH_PAYMENT) throw new BadRequestException("This request requires a payment operation");
        if ((type == FinancialRequestType.EMPLOYEE_PAYMENT || type == FinancialRequestType.CASH_REQUEST || type == FinancialRequestType.CASHIER_SETTLEMENT) && op.getType() != OperationType.CASH_TRANSFER) throw new BadRequestException("This request requires a physical transfer operation");
        if (type == FinancialRequestType.CASHIER_SETTLEMENT && op.getAmount().compareTo(remainingAmount(r)) != 0) throw new BadRequestException("Cashier settlement must transfer the full remaining approved amount");
    }
    private BigDecimal completedAmount(FinanceRequest request) {
        return operations.findAllByFinanceRequestIdAndOrganizationIdAndStatus(request.getId(), orgId(), OperationStatus.POSTED).stream()
                .map(FinanceOperation::getAmount).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
    private BigDecimal remainingAmount(FinanceRequest request) {
        BigDecimal approved = request.getApprovedAmount() == null ? request.getAmount() : request.getApprovedAmount();
        return approved.subtract(completedAmount(request)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
    private void updateFulfillment(FinanceRequest request, UUID operationId) {
        BigDecimal completed = completedAmount(request);
        BigDecimal approved = request.getApprovedAmount() == null ? request.getAmount() : request.getApprovedAmount();
        if (completed.compareTo(approved) > 0) throw new ConflictException("Operation exceeds the approved request remaining amount");
        request.setFulfillmentStatus(completed.compareTo(approved) == 0 ? RequestFulfillmentStatus.FULFILLED : RequestFulfillmentStatus.PARTIALLY_PAID);
        request.setResultingOperationId(operationId);
        requests.save(request);
    }
    private void setEvidenceRequirement(FinanceOperation operation) {
        if (operation.getFinanceRequestId() == null) return;
        FinanceRequest request = lockedRequest(operation.getFinanceRequestId());
        FinancialRequestType type = enumValue(FinancialRequestType.class, request.getRequestType(), "financial request type");
        if (type == FinancialRequestType.SUPPLIER_PAYMENT || type == FinancialRequestType.OTHER_PAYMENT) operation.setEvidenceStatus(EvidenceStatus.PENDING);
    }
    private void validateTypedRequest(FinancialRequestType type, RequestDetails details, BigDecimal requestedAmount, String requestCurrency) {
        if (details == null) throw new BadRequestException("Request details are required");
        switch (type) {
            case SUPPLIER_PAYMENT -> {
                if (details.supplierId() == null || details.purchaseOrderId() == null || details.invoiceReference() == null || details.invoiceReference().isBlank()) throw new BadRequestException("Supplier, purchase order, and invoice reference are required");
                suppliers.findByIdAndOrganizationId(details.supplierId(), orgId()).orElseThrow(() -> new NotFoundException("Supplier not found"));
                purchaseOrders.findByIdAndOrganizationId(details.purchaseOrderId(), orgId()).orElseThrow(() -> new NotFoundException("Purchase order not found"));
            }
            case EMPLOYEE_PAYMENT -> {
                if (details.employeeId() == null || details.purpose() == null || details.purpose().isBlank()) throw new BadRequestException("Employee and payment purpose are required");
                employees.findByIdAndOrganizationId(details.employeeId(), orgId()).orElseThrow(() -> new NotFoundException("Employee not found"));
                if (details.payrollId() != null) payrollPeriods.findByIdAndOrganizationId(details.payrollId(), orgId()).orElseThrow(() -> new NotFoundException("Payroll period not found"));
            }
            case OTHER_PAYMENT -> {
                if (details.payeeName() == null || details.payeeName().isBlank() || details.expenseCategory() == null || details.expenseCategory().isBlank()) throw new BadRequestException("Payee name and expense category are required");
            }
            case CASHIER_SETTLEMENT -> {
                if (details.employeeId() == null || details.cashBoxId() == null || details.expectedAmount() == null || details.countedAmount() == null || details.settlementDate() == null) throw new BadRequestException("Cashier, cashbox, expected and counted amounts, and settlement date are required");
                employees.findByIdAndOrganizationId(details.employeeId(), orgId()).orElseThrow(() -> new NotFoundException("Cashier not found"));
                if (details.expectedAmount().compareTo(requestedAmount) != 0) throw new BadRequestException("Settlement request amount must equal the expected settlement amount");
            }
            case CASH_REQUEST -> {
                if (details.employeeId() == null || details.purpose() == null || details.purpose().isBlank()) throw new BadRequestException("Requester and business purpose are required");
                employees.findByIdAndOrganizationId(details.employeeId(), orgId()).orElseThrow(() -> new NotFoundException("Requester not found"));
            }
            case REFUND_IN, REFUND_OUT -> {
                if (details.originalOperationId() == null || details.purpose() == null || details.purpose().isBlank()) throw new BadRequestException("Original transaction and refund reason are required");
                FinanceOperation original = operation(details.originalOperationId());
                if (original.getStatus() != OperationStatus.POSTED || !original.getCurrency().equals(requestCurrency)) throw new BadRequestException("Original transaction is not eligible for this refund");
                BigDecimal refunded = requestDetails.findAllByOriginalOperationId(original.getId()).stream()
                        .map(FinanceRequestDetail::getRequest).filter(r -> r.getStatus() != RequestStatus.CANCELLED)
                        .map(this::completedAmount).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
                if (refunded.add(amount(requestedAmount)).compareTo(original.getAmount()) > 0) throw new ConflictException("Refund exceeds the original transaction amount");
            }
        }
    }
    private void validateRefundLimit(FinanceRequest request, FinanceOperation operation) {
            Optional<FinanceRequestDetail> detail = requestDetails.findByRequestId(request.getId());
            if (detail.isEmpty() || detail.get().getOriginalOperationId() == null) return;
            FinancialRequestType type = enumValue(FinancialRequestType.class, request.getRequestType(), "financial request type");
            if (type != FinancialRequestType.REFUND_IN && type != FinancialRequestType.REFUND_OUT) return;
            FinanceOperation original = lockedOperation(detail.get().getOriginalOperationId());
            BigDecimal refunded = requestDetails.findAllByOriginalOperationId(original.getId()).stream()
                    .map(FinanceRequestDetail::getRequest).map(this::completedAmount)
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            if (refunded.compareTo(original.getAmount()) > 0) throw new ConflictException("Refund exceeds the original transaction amount");
    }
    private void saveDetails(FinanceRequest request, RequestDetails input) {
        FinanceRequestDetail detail = new FinanceRequestDetail();
        detail.setRequest(request); detail.setSupplierId(input.supplierId()); detail.setPurchaseOrderId(input.purchaseOrderId());
        detail.setEmployeeId(input.employeeId()); detail.setPayrollId(input.payrollId()); detail.setDepartmentId(input.departmentId());
        detail.setCashBoxId(input.cashBoxId()); detail.setRecipientEmployeeId(input.recipientEmployeeId()); detail.setOriginalOperationId(input.originalOperationId());
        detail.setPayeeName(input.payeeName()); detail.setPurpose(input.purpose()); detail.setInvoiceReference(input.invoiceReference());
        detail.setExpenseCategory(input.expenseCategory()); detail.setVarianceReason(input.varianceReason()); detail.setExpectedAmount(input.expectedAmount());
        detail.setCountedAmount(input.countedAmount()); detail.setSettlementDate(input.settlementDate());
        requestDetails.save(detail);
    }
    private String nextRequestNumber() { return "FR-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT); }
    private void transition(FinanceRequest r, RequestStatus target, String action, String notes) { if (r.getStatus() != RequestStatus.PENDING) throw new ConflictException("Only pending requests can be reviewed"); RequestStatus old = r.getStatus(); r.setStatus(target); r.setReviewerId(actor.employeeId()); r.setReviewedAt(Instant.now()); requests.save(r); event(r, old, target, action, notes); }
    private void event(FinanceRequest r, RequestStatus old, RequestStatus status, String action, String notes) { FinanceRequestEvent e = new FinanceRequestEvent(); e.setRequest(r); e.setPriorStatus(old); e.setNewStatus(status); e.setAction(action); e.setActorId(actor.employeeId()); e.setNotes(notes); e.setOccurredAt(Instant.now()); requestEvents.save(e); }
    private void saveRequestAttachments(FinanceRequest request, List<Attachment> values) { if (values == null) return; for (Attachment a : values) { FinanceRequestAttachment e = new FinanceRequestAttachment(); e.setRequest(request); e.setStorageReference(a.storageReference()); e.setFilename(a.filename()); e.setContentType(a.contentType()); e.setUploadedBy(actor.employeeId()); requestAttachments.save(e); } }
    private void saveOperationAttachments(FinanceOperation operation, List<Attachment> values) { if (values == null) return; for (Attachment a : values) { FinanceOperationAttachment e = new FinanceOperationAttachment(); e.setOperation(operation); e.setStorageReference(a.storageReference()); e.setFilename(a.filename()); e.setContentType(a.contentType()); e.setUploadedBy(actor.employeeId()); operationAttachments.save(e); } }
    private BigDecimal balance(UUID accountId) { return ledger.findAllByAccountIdAndOrganizationIdOrderByPostedAtDesc(accountId, orgId()).stream().map(e -> e.getDirection() == LedgerDirection.DEBIT ? e.getAmount() : e.getAmount().negate()).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add); }
    private String secureToken() { byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private Map<String, Object> requestView(FinanceRequest r, boolean details) { Map<String,Object> m = new LinkedHashMap<>(); m.put("id",r.getId()); m.put("requestNumber",r.getRequestNumber()); m.put("sourceDomain",r.getSourceDomain()); m.put("sourceRecordId",r.getSourceRecordId()); m.put("type",r.getRequestType()); m.put("requestedAmount",r.getAmount()); m.put("approvedAmount",r.getApprovedAmount()); m.put("amount",r.getAmount()); m.put("currency",r.getCurrency()); m.put("reason",r.getReason()); m.put("notes",r.getNotes()); m.put("approvalStatus",r.getStatus()); m.put("status",r.getStatus()); m.put("fulfillmentStatus",r.getFulfillmentStatus()); m.put("previouslyCompletedAmount",completedAmount(r)); m.put("completedAmount",completedAmount(r)); m.put("remainingApprovedAmount",remainingAmount(r)); m.put("remainingAmount",remainingAmount(r)); m.put("requesterId",r.getRequesterId()); m.put("reviewerId",r.getReviewerId()); m.put("reviewedAt",r.getReviewedAt()); m.put("rejectionReason",r.getRejectionReason()); m.put("dueDate",r.getDueDate()); m.put("resultingOperationId",r.getResultingOperationId()); m.put("createdAt",r.getCreatedAt()); if(details){m.put("typeDetails",requestDetails.findByRequestId(r.getId()).map(this::detailView).orElse(null));m.put("attachments",requestAttachments.findAllByRequestIdOrderByCreatedAtAsc(r.getId()));m.put("approvalHistory",requestEvents.findAllByRequestIdOrderByOccurredAtAsc(r.getId()));m.put("operations",operations.findAllByFinanceRequestIdAndOrganizationIdAndStatus(r.getId(), orgId(), OperationStatus.POSTED).stream().map(o -> operationView(o, false)).toList());} return m; }
    private Map<String, Object> detailView(FinanceRequestDetail d) { Map<String,Object> result = new LinkedHashMap<>(); result.put("supplierId", d.getSupplierId()); result.put("purchaseOrderId", d.getPurchaseOrderId()); result.put("employeeId", d.getEmployeeId()); result.put("payrollId", d.getPayrollId()); result.put("departmentId", d.getDepartmentId()); result.put("cashBoxId", d.getCashBoxId()); result.put("recipientEmployeeId", d.getRecipientEmployeeId()); result.put("originalOperationId", d.getOriginalOperationId()); result.put("payeeName", d.getPayeeName()); result.put("purpose", d.getPurpose()); result.put("invoiceReference", d.getInvoiceReference()); result.put("expenseCategory", d.getExpenseCategory()); result.put("varianceReason", d.getVarianceReason()); result.put("expectedAmount", d.getExpectedAmount()); result.put("countedAmount", d.getCountedAmount()); result.put("settlementDate", d.getSettlementDate()); return result; }
    private Map<String, Object> accountView(FinanceAccount a) { Map<String,Object> m = new LinkedHashMap<>(); m.put("id",a.getId()); m.put("type",a.getType()); m.put("name",a.getName()); m.put("locationId",a.getLocationId()); m.put("currency",a.getCurrency()); m.put("status",a.getStatus()); m.put("assignedEmployeeId",a.getAssignedEmployeeId()); m.put("balance",balance(a.getId())); m.put("createdAt",a.getCreatedAt()); return m; }
    private Map<String, Object> operationView(FinanceOperation o, boolean details) { Map<String,Object> m=new LinkedHashMap<>();m.put("id",o.getId());m.put("type",o.getType());m.put("sourceAccountId",o.getSourceAccountId());m.put("destinationAccountId",o.getDestinationAccountId());m.put("amount",o.getAmount());m.put("currency",o.getCurrency());m.put("reference",o.getReference());m.put("externalReference",o.getReference());m.put("description",o.getDescription());m.put("documentDescription",o.getDescription());m.put("notes",o.getNotes());m.put("createdBy",o.getCreatedBy());m.put("status",o.getStatus());m.put("evidenceStatus",o.getEvidenceStatus());m.put("evidenceVerifiedBy",o.getEvidenceVerifiedBy());m.put("evidenceVerifiedAt",o.getEvidenceVerifiedAt());m.put("requestId",o.getFinanceRequestId());m.put("executedAt",o.getExecutedAt());m.put("createdAt",o.getCreatedAt());if(details){m.put("ledgerEntries",ledger.findAllByOperationIdOrderByCreatedAtAsc(o.getId()));m.put("documents",operationAttachments.findAllByOperationIdOrderByCreatedAtAsc(o.getId()));}return m; }
    private Map<String, Object> entryView(FinanceLedgerEntry e) { return Map.of("id",e.getId(),"operationId",e.getOperationId(),"direction",e.getDirection(),"amount",e.getAmount(),"currency",e.getCurrency(),"reference",e.getReference(),"postedAt",e.getPostedAt()); }
    private Map<String, Object> payrollView(FinancePayrollPayment p) { return Map.of("id",p.getId(),"payrollId",p.getPayrollId(),"employeeId",p.getEmployeeId(),"amount",p.getAmount(),"currency",p.getCurrency(),"sourceAccountId",p.getSourceAccountId(),"operationId",p.getOperationId(),"verificationStatus",p.getReceiverVerificationStatus(),"paidAt",p.getPaidAt()); }
}
