package q.qedet.finance.model;

public final class FinanceWorkflowTypes {
    private FinanceWorkflowTypes() {}

    public enum RequestStatus { PENDING, APPROVED, REJECTED, CANCELLED }
    public enum RequestFulfillmentStatus { UNPAID, PARTIALLY_PAID, FULFILLED }
    public enum FinancialRequestType { SUPPLIER_PAYMENT, EMPLOYEE_PAYMENT, OTHER_PAYMENT, CASHIER_SETTLEMENT, CASH_REQUEST, REFUND_IN, REFUND_OUT }
    public enum EvidenceStatus { NOT_REQUIRED, PENDING, VERIFIED, REJECTED }
    public enum AccountType { CASHBOX, BANK }
    public enum AccountStatus { ACTIVE, INACTIVE, CLOSED }
    public enum OperationType { CASH_PAYMENT, CASH_RECEIPT, CASH_TRANSFER }
    public enum OperationStatus { PENDING, POSTED, CANCELLED }
    public enum LedgerDirection { DEBIT, CREDIT }
    public enum ConfirmationStatus { PENDING, VERIFIED, COMPLETED, CANCELLED, EXPIRED }
}
