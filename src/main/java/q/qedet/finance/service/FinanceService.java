package q.qedet.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.common.model.RoleType;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.finance.dto.FinanceDtos.*;
import q.qedet.finance.entity.*;
import q.qedet.finance.model.*;
import q.qedet.finance.repository.*;
import q.qedet.hr.common.event.*;
import q.qedet.organization.entity.*;
import q.qedet.organization.repository.*;
import q.qedet.procurement.entity.PurchaseOrderItem;
import q.qedet.procurement.repository.PurchaseOrderItemRepository;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class FinanceService {
    private final ActorContext actor;
    private final OrganizationRepository organizations;
    private final OrganizationSiteRepository sites;
    private final EmployeeRepository employees;
    private final CashBoxRepository cashBoxes;
    private final CashBoxEmployeeAssignmentRepository assignments;
    private final CashTransactionRepository transactions;
    private final PaymentRequestRepository paymentRequests;
    private final PurchaseOrderItemRepository purchaseOrderItems;
    private final CashTransferRepository transfers;
    private final CashReconciliationRepository reconciliations;
    private final DomainEventPublisher events;

    public FinanceService(ActorContext actor, OrganizationRepository organizations, OrganizationSiteRepository sites,
                          EmployeeRepository employees, CashBoxRepository cashBoxes,
                          CashBoxEmployeeAssignmentRepository assignments, CashTransactionRepository transactions,
                          PaymentRequestRepository paymentRequests, PurchaseOrderItemRepository purchaseOrderItems, CashTransferRepository transfers,
                          CashReconciliationRepository reconciliations, DomainEventPublisher events) {
        this.actor = actor; this.organizations = organizations; this.sites = sites; this.employees = employees;
        this.cashBoxes = cashBoxes; this.assignments = assignments; this.transactions = transactions;
        this.paymentRequests = paymentRequests; this.purchaseOrderItems = purchaseOrderItems; this.transfers = transfers; this.reconciliations = reconciliations;
        this.events = events;
    }

    public void requireOrganization(UUID organizationId) {
        if (!actor.organizationId().equals(organizationId)) throw new NotFoundException("Organization not found");
    }
    private UUID orgId() { return actor.organizationId(); }
    private Organization organization() { return organizations.findById(orgId()).orElseThrow(() -> new NotFoundException("Organization not found")); }
    private Employee employee() { return employees.findByIdAndOrganizationId(actor.employeeId(), orgId()).orElseThrow(() -> new NotFoundException("Employee not found")); }
    private void permission(Permission permission) { actor.requirePermission(permission); }
    private boolean manager() { return actor.permissions().contains(Permission.FINANCE_CASH_BOX_ASSIGN); }
    private BigDecimal money(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) throw new BadRequestException("Amount cannot be negative");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
    private String currency(String value) {
        if (value == null || !value.matches("(?i)[a-z]{3}")) throw new BadRequestException("Currency must be a three-letter code");
        return value.toUpperCase(Locale.ROOT);
    }
    private <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        try { return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new BadRequestException("Invalid " + field); }
    }
    private CashBox ownedCashBox(UUID id) { return cashBoxes.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Cash box not found")); }
    private CashBox lockedCashBox(UUID id) { return cashBoxes.findLockedByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Cash box not found")); }
    private Employee ownedEmployee(UUID id) { return employees.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Employee not found")); }
    private PaymentRequest ownedPaymentRequest(UUID id) { return paymentRequests.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Payment request not found")); }
    private CashTransfer ownedTransfer(UUID id) { return transfers.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Cash transfer not found")); }
    private void active(CashBox cashBox) {
        if (cashBox.getStatus() != CashBoxStatus.ACTIVE) throw new ConflictException("Cash box is not active");
    }
    private void operate(CashBox cashBox) {
        if (!manager() && !assignments.existsByCashBoxIdAndEmployeeId(cashBox.getId(), actor.employeeId()))
            throw new ForbiddenException("Employee is not assigned to this cash box");
    }
    private BigDecimal balance(CashBox cashBox) {
        return transactions.findAllByCashBoxIdAndOrganizationIdOrderByPostedAtDesc(cashBox.getId(), orgId()).stream()
                .map(t -> switch (t.getType()) {
                    case CASH_IN, TRANSFER_IN, ADJUSTMENT_IN -> t.getAmount();
                    case CASH_OUT, TRANSFER_OUT, ADJUSTMENT_OUT -> t.getAmount().negate();
                }).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
    private void requireCurrency(CashBox cashBox, String currency) {
        if (!cashBox.getCurrency().equalsIgnoreCase(currency)) throw new BadRequestException("Cash box currency does not match request currency");
    }
    private void publish(FinanceEventType type, UUID aggregateId, Map<String, String> attributes) {
        events.publish(new DomainEvent(type.name(), orgId(), actor.employeeId(), aggregateId, Instant.now(), attributes));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> cashBoxes() {
        permission(Permission.FINANCE_CASH_BOX_VIEW);
        List<CashBox> result = manager() ? cashBoxes.findAllByOrganizationIdOrderByNameAsc(orgId())
                : assignments.findAllByEmployeeIdAndOrganizationId(actor.employeeId(), orgId()).stream().map(CashBoxEmployeeAssignment::getCashBox).toList();
        return result.stream().map(this::cashBoxView).toList();
    }
    @Transactional(readOnly = true)
    public Map<String, Object> cashBox(UUID id) { permission(Permission.FINANCE_CASH_BOX_VIEW); CashBox box = ownedCashBox(id); operate(box); return cashBoxView(box); }
    @Transactional
    public Map<String, Object> updateCashBox(UUID id, CashBoxUpdateRequest request) {
        permission(Permission.FINANCE_CASH_BOX_UPDATE); CashBox box = ownedCashBox(id);
        if (!box.getName().equalsIgnoreCase(request.name().trim()) && cashBoxes.existsByOrganizationIdAndNameIgnoreCase(orgId(), request.name().trim()))
            throw new ConflictException("Cash box name already exists");
        box.setName(request.name().trim()); box.setSite(site(request.locationId())); return cashBoxView(cashBoxes.save(box));
    }
    @Transactional
    public Map<String, Object> status(UUID id, boolean activate) {
        permission(Permission.FINANCE_CASH_BOX_UPDATE); CashBox box = ownedCashBox(id);
        if (box.getStatus() == CashBoxStatus.CLOSED) throw new ConflictException("Closed cash boxes cannot be reopened");
        box.setStatus(activate ? CashBoxStatus.ACTIVE : CashBoxStatus.INACTIVE); CashBox saved = cashBoxes.save(box);
        publish(activate ? FinanceEventType.CASH_BOX_ACTIVATED : FinanceEventType.CASH_BOX_DEACTIVATED, id, Map.of());
        return cashBoxView(saved);
    }
    @Transactional
    public void assign(UUID cashBoxId, UUID employeeId) {
        permission(Permission.FINANCE_CASH_BOX_ASSIGN); CashBox box = ownedCashBox(cashBoxId); Employee target = ownedEmployee(employeeId);
        if (target.getRole() != RoleType.FINANCE_EMPLOYEE) throw new BadRequestException("Only finance employees can be assigned to cash boxes");
        if (assignments.existsByCashBoxIdAndEmployeeId(cashBoxId, employeeId)) throw new ConflictException("Employee is already assigned to this cash box");
        CashBoxEmployeeAssignment assignment = new CashBoxEmployeeAssignment();
        assignment.setOrganization(organization()); assignment.setCashBox(box); assignment.setEmployee(target); assignments.save(assignment);
    }
    @Transactional
    public void unassign(UUID cashBoxId, UUID employeeId) {
        permission(Permission.FINANCE_CASH_BOX_ASSIGN);
        CashBoxEmployeeAssignment assignment = assignments.findByCashBoxIdAndEmployeeId(cashBoxId, employeeId)
                .orElseThrow(() -> new NotFoundException("Cash box employee assignment not found"));
        if (!assignment.getOrganization().getId().equals(orgId())) throw new NotFoundException("Cash box employee assignment not found");
        assignments.delete(assignment);
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> assignedEmployees(UUID cashBoxId) {
        permission(Permission.FINANCE_CASH_BOX_VIEW); CashBox box = ownedCashBox(cashBoxId); operate(box);
        return assignments.findAllByCashBoxIdAndOrganizationId(cashBoxId, orgId()).stream().map(a -> {
            Employee e = a.getEmployee(); return Map.<String, Object>of("id", e.getId(), "employeeNumber", e.getEmployeeNumber(), "name", e.getFirstName() + " " + e.getLastName());
        }).toList();
    }

    @Transactional
    public Map<String, Object> receivePaymentRequest(PaymentRequestCreate request) {
        permission(Permission.PAYMENT_REQUEST_CREATE);
        PaymentRequestType type = enumValue(PaymentRequestType.class, request.requestType(), "requestType");
        if (request.purchaseOrderItemId() != null) {
            PurchaseOrderItem item = purchaseOrderItems.findById(request.purchaseOrderItemId())
                    .orElseThrow(() -> new NotFoundException("Purchase order item not found"));
            if (!item.getPurchaseOrder().getOrganization().getId().equals(orgId())
                    || !item.getPurchaseOrder().getId().equals(request.purchaseOrderId())
                    || !item.getSupplier().getId().equals(request.supplierId()))
                throw new BadRequestException("Supplier payment request does not match the purchase order item");
            if (!item.getCurrency().equalsIgnoreCase(request.currency()))
                throw new BadRequestException("Supplier payment currency does not match the purchase order item");
        }
        if (paymentRequests.existsByOrganizationIdAndRequestTypeAndSourceReference(orgId(), type, request.sourceReference().trim()))
            throw new ConflictException("A payment request already exists for this source reference");
        PaymentRequest payment = new PaymentRequest();
        payment.setOrganization(organization()); payment.setRequestType(type); payment.setSourceDepartment(request.sourceDepartment().trim());
        payment.setSourceReference(request.sourceReference().trim()); payment.setAmount(money(request.amount())); payment.setCurrency(currency(request.currency()));
        payment.setRequestedBy(employee()); payment.setRequestedAt(Instant.now()); payment.setReason(request.reason().trim());
        payment.setSupportingDocuments(request.supportingDocuments()); payment.setSupplierId(request.supplierId());
        payment.setPurchaseOrderId(request.purchaseOrderId()); payment.setPurchaseOrderItemId(request.purchaseOrderItemId());
        payment.setSupplierInvoice(request.supplierInvoice()); payment.setDueDate(request.dueDate());
        PaymentRequest saved = paymentRequests.save(payment);
        publish(FinanceEventType.PAYMENT_REQUEST_RECEIVED, saved.getId(), Map.of("requestType", type.name(), "sourceReference", saved.getSourceReference()));
        return paymentView(saved);
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> paymentRequests() { permission(Permission.FINANCE_PAYMENT_REQUEST_VIEW); return paymentRequests.findAllByOrganizationIdOrderByRequestedAtDesc(orgId()).stream().map(this::paymentView).toList(); }
    @Transactional(readOnly = true)
    public Map<String, Object> paymentRequest(UUID id) { permission(Permission.FINANCE_PAYMENT_REQUEST_VIEW); return paymentView(ownedPaymentRequest(id)); }
    @Transactional
    public Map<String, Object> approvePaymentRequest(UUID id) {
        permission(Permission.FINANCE_PAYMENT_REQUEST_APPROVE); PaymentRequest request = ownedPaymentRequest(id);
        if (request.getStatus() != PaymentRequestStatus.RECEIVED) throw new ConflictException("Only received payment requests can be approved");
        request.setStatus(PaymentRequestStatus.APPROVED); request.setReviewedBy(employee()); request.setReviewedAt(Instant.now());
        PaymentRequest saved = paymentRequests.save(request); publish(FinanceEventType.PAYMENT_REQUEST_APPROVED, id, Map.of("requestType", saved.getRequestType().name())); return paymentView(saved);
    }
    @Transactional
    public Map<String, Object> rejectPaymentRequest(UUID id, DecisionRequest decision) {
        permission(Permission.FINANCE_PAYMENT_REQUEST_APPROVE); PaymentRequest request = ownedPaymentRequest(id);
        if (request.getStatus() != PaymentRequestStatus.RECEIVED) throw new ConflictException("Only received payment requests can be rejected");
        if (decision == null || decision.reason() == null || decision.reason().isBlank()) throw new BadRequestException("Rejection reason is required");
        request.setStatus(PaymentRequestStatus.REJECTED); request.setReviewedBy(employee()); request.setReviewedAt(Instant.now()); request.setRejectionReason(decision.reason().trim());
        PaymentRequest saved = paymentRequests.save(request); publish(FinanceEventType.PAYMENT_REQUEST_REJECTED, id, Map.of("requestType", saved.getRequestType().name())); return paymentView(saved);
    }
    @Transactional
    public Map<String, Object> executePaymentRequest(UUID id, ExecutePaymentRequest execution) {
        permission(Permission.FINANCE_PAYMENT_EXECUTE); PaymentRequest request = ownedPaymentRequest(id);
        if (request.getStatus() != PaymentRequestStatus.APPROVED) throw new ConflictException("Only approved payment requests can be executed");
        permissionFor(request.getRequestType());
        CashBox box = lockedCashBox(execution.cashBoxId()); active(box); operate(box); requireCurrency(box, request.getCurrency());
        CashTransactionType type = request.getRequestType() == PaymentRequestType.MONEY_IN ? CashTransactionType.CASH_IN : CashTransactionType.CASH_OUT;
        if (type == CashTransactionType.CASH_OUT && balance(box).compareTo(request.getAmount()) < 0) throw new ConflictException("Insufficient cash box balance");
        CashTransaction transaction = post(box, type, request.getAmount(), request.getCurrency(), "PAYMENT_REQUEST", request.getId(),
                execution.reference(), execution.description() == null ? request.getReason() : execution.description(), request.getReviewedBy(), null);
        request.setStatus(PaymentRequestStatus.EXECUTED); request.setCompletedAt(Instant.now()); paymentRequests.save(request);
        publish(type == CashTransactionType.CASH_IN ? FinanceEventType.CASH_IN_RECORDED : FinanceEventType.CASH_OUT_RECORDED, transaction.getId(), Map.of("paymentRequestId", id.toString()));
        publish(FinanceEventType.PAYMENT_EXECUTED, id, Map.of("requestType", request.getRequestType().name()));
        publishPaymentCompletion(request);
        return transactionView(transaction);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> transactions(UUID cashBoxId) {
        permission(Permission.FINANCE_CASH_TRANSACTION_VIEW);
        List<CashTransaction> result;
        if (cashBoxId == null) result = transactions.findAllByOrganizationIdOrderByPostedAtDesc(orgId());
        else { CashBox box = ownedCashBox(cashBoxId); operate(box); result = transactions.findAllByCashBoxIdAndOrganizationIdOrderByPostedAtDesc(cashBoxId, orgId()); }
        return result.stream().filter(t -> manager() || assignments.existsByCashBoxIdAndEmployeeId(t.getCashBox().getId(), actor.employeeId())).map(this::transactionView).toList();
    }
    @Transactional(readOnly = true)
    public Map<String, Object> transaction(UUID id) {
        permission(Permission.FINANCE_CASH_TRANSACTION_VIEW);
        CashTransaction transaction = transactions.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Cash transaction not found"));
        operate(transaction.getCashBox()); return transactionView(transaction);
    }

    @Transactional
    public Map<String, Object> createTransfer(CashTransferRequest request) {
        permission(Permission.FINANCE_CASH_TRANSFER);
        if (request.fromCashBoxId().equals(request.toCashBoxId())) throw new BadRequestException("Transfer cash boxes must differ");
        CashBox from = ownedCashBox(request.fromCashBoxId()); CashBox to = ownedCashBox(request.toCashBoxId()); operate(from); operate(to); requireCurrency(from, currency(request.currency())); requireCurrency(to, currency(request.currency()));
        CashTransfer transfer = new CashTransfer(); transfer.setOrganization(organization()); transfer.setFromCashBox(from); transfer.setToCashBox(to);
        transfer.setAmount(money(request.amount())); transfer.setCurrency(currency(request.currency())); transfer.setReason(request.reason().trim()); transfer.setRequestedBy(employee());
        CashTransfer saved = transfers.save(transfer); publish(FinanceEventType.CASH_TRANSFER_CREATED, saved.getId(), Map.of()); return transferView(saved);
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> transfers() { permission(Permission.FINANCE_CASH_TRANSACTION_VIEW); return transfers.findAllByOrganizationIdOrderByCreatedAtDesc(orgId()).stream().filter(t -> manager() || assignments.existsByCashBoxIdAndEmployeeId(t.getFromCashBox().getId(), actor.employeeId()) || assignments.existsByCashBoxIdAndEmployeeId(t.getToCashBox().getId(), actor.employeeId())).map(this::transferView).toList(); }
    @Transactional(readOnly = true)
    public Map<String, Object> transfer(UUID id) { permission(Permission.FINANCE_CASH_TRANSACTION_VIEW); CashTransfer t = ownedTransfer(id); operate(t.getFromCashBox()); operate(t.getToCashBox()); return transferView(t); }
    @Transactional
    public Map<String, Object> approveTransfer(UUID id) {
        permission(Permission.FINANCE_CASH_TRANSFER); if (!manager()) throw new ForbiddenException("Only finance managers can approve transfers");
        CashTransfer transfer = ownedTransfer(id); if (transfer.getStatus() != CashTransferStatus.PENDING_APPROVAL) throw new ConflictException("Only pending transfers can be approved");
        transfer.setStatus(CashTransferStatus.APPROVED); transfer.setApprovedBy(employee()); return transferView(transfers.save(transfer));
    }
    @Transactional
    public Map<String, Object> executeTransfer(UUID id) {
        permission(Permission.FINANCE_CASH_TRANSFER); CashTransfer transfer = ownedTransfer(id);
        if (transfer.getStatus() != CashTransferStatus.APPROVED) throw new ConflictException("Only approved transfers can be executed");
        UUID first = transfer.getFromCashBox().getId().compareTo(transfer.getToCashBox().getId()) < 0 ? transfer.getFromCashBox().getId() : transfer.getToCashBox().getId();
        UUID second = first.equals(transfer.getFromCashBox().getId()) ? transfer.getToCashBox().getId() : transfer.getFromCashBox().getId();
        CashBox firstLocked = lockedCashBox(first); CashBox secondLocked = lockedCashBox(second);
        CashBox from = firstLocked.getId().equals(transfer.getFromCashBox().getId()) ? firstLocked : secondLocked;
        CashBox to = firstLocked.getId().equals(transfer.getToCashBox().getId()) ? firstLocked : secondLocked;
        active(from); active(to); operate(from); operate(to); requireCurrency(from, transfer.getCurrency()); requireCurrency(to, transfer.getCurrency());
        if (balance(from).compareTo(transfer.getAmount()) < 0) throw new ConflictException("Insufficient source cash box balance");
        post(from, CashTransactionType.TRANSFER_OUT, transfer.getAmount(), transfer.getCurrency(), "CASH_TRANSFER", transfer.getId(), null, transfer.getReason(), transfer.getApprovedBy(), transfer.getId());
        post(to, CashTransactionType.TRANSFER_IN, transfer.getAmount(), transfer.getCurrency(), "CASH_TRANSFER", transfer.getId(), null, transfer.getReason(), transfer.getApprovedBy(), transfer.getId());
        transfer.setStatus(CashTransferStatus.EXECUTED); transfer.setExecutedBy(employee()); transfer.setExecutedAt(Instant.now()); CashTransfer saved = transfers.save(transfer);
        publish(FinanceEventType.CASH_TRANSFER_COMPLETED, id, Map.of()); return transferView(saved);
    }

    @Transactional
    public Map<String, Object> reconcile(UUID cashBoxId, ReconciliationRequest request) {
        permission(Permission.FINANCE_CASH_RECONCILE); if (!manager()) throw new ForbiddenException("Only finance managers can approve cash adjustments");
        CashBox box = lockedCashBox(cashBoxId); active(box); operate(box); BigDecimal system = balance(box); BigDecimal physical = money(request.physicalCount()); BigDecimal difference = physical.subtract(system);
        CashReconciliation reconciliation = new CashReconciliation(); reconciliation.setOrganization(organization()); reconciliation.setCashBox(box);
        reconciliation.setSystemBalance(system); reconciliation.setPhysicalCount(physical); reconciliation.setDifference(difference); reconciliation.setReason(request.reason().trim());
        reconciliation.setSupportingInformation(request.supportingInformation()); reconciliation.setCountedBy(employee()); reconciliation.setApprovedBy(employee()); reconciliation.setCompletedAt(Instant.now());
        CashReconciliation saved = reconciliations.save(reconciliation);
        if (difference.signum() != 0) post(box, difference.signum() > 0 ? CashTransactionType.ADJUSTMENT_IN : CashTransactionType.ADJUSTMENT_OUT, difference.abs(), box.getCurrency(),
                "CASH_RECONCILIATION", saved.getId(), null, request.reason().trim(), employee(), null);
        publish(FinanceEventType.CASH_RECONCILIATION_COMPLETED, saved.getId(), Map.of()); return reconciliationView(saved);
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> reconciliations(UUID cashBoxId) {
        permission(Permission.FINANCE_CASH_BOX_VIEW); CashBox box = ownedCashBox(cashBoxId); operate(box);
        return reconciliations.findAllByCashBoxIdAndOrganizationIdOrderByCreatedAtDesc(cashBoxId, orgId()).stream().map(this::reconciliationView).toList();
    }

    private CashTransaction post(CashBox box, CashTransactionType type, BigDecimal amount, String currency, String sourceType, UUID sourceId,
                                 String reference, String description, Employee approvedBy, UUID transferId) {
        CashTransaction transaction = new CashTransaction(); transaction.setOrganization(organization()); transaction.setCashBox(box); transaction.setType(type);
        transaction.setAmount(money(amount)); transaction.setCurrency(currency); transaction.setSourceType(sourceType); transaction.setSourceId(sourceId);
        transaction.setReference(reference); transaction.setDescription(description); transaction.setCreatedBy(employee()); transaction.setApprovedBy(approvedBy);
        transaction.setPostedAt(Instant.now()); transaction.setCompletedAt(Instant.now()); transaction.setTransferId(transferId);
        BigDecimal current = balance(box); transaction.setBalanceAfter(current.add(switch (type) {
            case CASH_IN, TRANSFER_IN, ADJUSTMENT_IN -> transaction.getAmount();
            case CASH_OUT, TRANSFER_OUT, ADJUSTMENT_OUT -> transaction.getAmount().negate();
        }));
        return transactions.save(transaction);
    }
    private OrganizationSite site(UUID id) {
        return id == null ? null : sites.findByIdAndOrganizationId(id, orgId()).orElseThrow(() -> new NotFoundException("Site not found"));
    }
    private void permissionFor(PaymentRequestType type) {
        switch (type) {
            case SUPPLIER_PAYMENT -> permission(Permission.FINANCE_SUPPLIER_PAYMENT);
            case PAYROLL_PAYMENT -> permission(Permission.FINANCE_PAYROLL_PAYMENT);
            case EMPLOYEE_LOAN_PAYMENT -> permission(Permission.FINANCE_EMPLOYEE_LOAN_PAYMENT);
            case EXPENSE_PAYMENT, OTHER_PAYMENT -> permission(Permission.FINANCE_EXPENSE_PAYMENT);
            case MONEY_IN -> permission(Permission.FINANCE_CASH_IN);
        }
    }
    private void publishPaymentCompletion(PaymentRequest request) {
        FinanceEventType type = switch (request.getRequestType()) {
            case PAYROLL_PAYMENT -> FinanceEventType.PAYROLL_PAYMENT_COMPLETED;
            case EMPLOYEE_LOAN_PAYMENT -> FinanceEventType.EMPLOYEE_LOAN_PAYMENT_COMPLETED;
            case SUPPLIER_PAYMENT -> FinanceEventType.SUPPLIER_PAYMENT_COMPLETED;
            default -> null;
        };
        if (type != null) publish(type, request.getId(), Map.of("sourceReference", request.getSourceReference()));
    }
    private Map<String, Object> cashBoxView(CashBox box) {
        Map<String, Object> view = new LinkedHashMap<>(); view.put("id", box.getId()); view.put("name", box.getName()); view.put("locationId", box.getSite() == null ? null : box.getSite().getId());
        view.put("currency", box.getCurrency()); view.put("status", box.getStatus()); view.put("balance", balance(box)); view.put("createdAt", box.getCreatedAt()); view.put("updatedAt", box.getUpdatedAt()); return view;
    }
    private Map<String, Object> paymentView(PaymentRequest request) {
        Map<String, Object> view = new LinkedHashMap<>(); view.put("id", request.getId()); view.put("requestType", request.getRequestType()); view.put("sourceDepartment", request.getSourceDepartment()); view.put("sourceReference", request.getSourceReference());
        view.put("amount", request.getAmount()); view.put("currency", request.getCurrency()); view.put("requestedBy", request.getRequestedBy().getId()); view.put("requestedAt", request.getRequestedAt()); view.put("reason", request.getReason()); view.put("supportingDocuments", request.getSupportingDocuments());
        view.put("status", request.getStatus()); view.put("reviewedBy", request.getReviewedBy() == null ? null : request.getReviewedBy().getId()); view.put("reviewedAt", request.getReviewedAt()); view.put("rejectionReason", request.getRejectionReason()); view.put("completedAt", request.getCompletedAt());
        view.put("supplierId", request.getSupplierId()); view.put("purchaseOrderId", request.getPurchaseOrderId());
        view.put("purchaseOrderItemId", request.getPurchaseOrderItemId()); view.put("supplierInvoice", request.getSupplierInvoice()); view.put("dueDate", request.getDueDate()); return view;
    }
    private Map<String, Object> transactionView(CashTransaction transaction) {
        Map<String, Object> view = new LinkedHashMap<>(); view.put("id", transaction.getId()); view.put("cashBoxId", transaction.getCashBox().getId()); view.put("type", transaction.getType()); view.put("amount", transaction.getAmount()); view.put("currency", transaction.getCurrency());
        view.put("sourceType", transaction.getSourceType()); view.put("sourceId", transaction.getSourceId()); view.put("reference", transaction.getReference()); view.put("description", transaction.getDescription()); view.put("createdBy", transaction.getCreatedBy().getId());
        view.put("approvedBy", transaction.getApprovedBy() == null ? null : transaction.getApprovedBy().getId()); view.put("postedAt", transaction.getPostedAt()); view.put("completedAt", transaction.getCompletedAt()); view.put("balanceAfter", transaction.getBalanceAfter()); view.put("transferId", transaction.getTransferId()); return view;
    }
    private Map<String, Object> transferView(CashTransfer transfer) {
        Map<String, Object> view = new LinkedHashMap<>(); view.put("id", transfer.getId()); view.put("fromCashBoxId", transfer.getFromCashBox().getId()); view.put("toCashBoxId", transfer.getToCashBox().getId()); view.put("amount", transfer.getAmount());
        view.put("currency", transfer.getCurrency()); view.put("reason", transfer.getReason()); view.put("requestedBy", transfer.getRequestedBy().getId()); view.put("approvedBy", transfer.getApprovedBy() == null ? null : transfer.getApprovedBy().getId());
        view.put("executedBy", transfer.getExecutedBy() == null ? null : transfer.getExecutedBy().getId()); view.put("status", transfer.getStatus()); view.put("createdAt", transfer.getCreatedAt()); view.put("executedAt", transfer.getExecutedAt()); return view;
    }
    private Map<String, Object> reconciliationView(CashReconciliation reconciliation) {
        Map<String, Object> view = new LinkedHashMap<>(); view.put("id", reconciliation.getId()); view.put("cashBoxId", reconciliation.getCashBox().getId()); view.put("systemBalance", reconciliation.getSystemBalance()); view.put("physicalCount", reconciliation.getPhysicalCount());
        view.put("difference", reconciliation.getDifference()); view.put("reason", reconciliation.getReason()); view.put("supportingInformation", reconciliation.getSupportingInformation()); view.put("countedBy", reconciliation.getCountedBy().getId()); view.put("approvedBy", reconciliation.getApprovedBy().getId()); view.put("completedAt", reconciliation.getCompletedAt()); return view;
    }
}
