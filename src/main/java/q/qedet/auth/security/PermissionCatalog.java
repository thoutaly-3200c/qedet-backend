package q.qedet.auth.security;



import q.qedet.common.model.Permission;
import q.qedet.common.model.RoleType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class PermissionCatalog {

    private static final Map<RoleType, Set<Permission>> ROLE_PERMISSIONS = new EnumMap<>(RoleType.class);

    static {
        ROLE_PERMISSIONS.put(RoleType.OWNER, EnumSet.allOf(Permission.class));

        ROLE_PERMISSIONS.put(RoleType.GENERAL_MANAGER, EnumSet.of(
                Permission.ORGANIZATION_VIEW,
                Permission.ORGANIZATION_CONFIG_VIEW,
                Permission.ORGANIZATION_CONFIG_EDIT,
                Permission.EMPLOYEE_VIEW,
                Permission.EMPLOYEE_CREATE,
                Permission.EMPLOYEE_UPDATE,
                Permission.EMPLOYEE_INVITE,
                Permission.EMPLOYEE_ACTIVATE,
                Permission.EMPLOYEE_DEACTIVATE,
                Permission.DEPARTMENT_VIEW,
                Permission.DEPARTMENT_MANAGE,
                Permission.LOCATION_VIEW,
                Permission.LOCATION_MANAGE,
                Permission.POSITION_VIEW,
                Permission.POSITION_CREATE,
                Permission.POSITION_UPDATE,
                Permission.POSITION_DELETE,
                Permission.PRODUCT_VIEW,
                Permission.PRODUCT_MANAGE,
                Permission.INVENTORY_VIEW,
                Permission.INVENTORY_MANAGE,
                Permission.PROCUREMENT_VIEW,
                Permission.PROCUREMENT_MANAGE,
                Permission.SALES_VIEW,
                Permission.SALES_MANAGE,
                Permission.CUSTOMER_VIEW,
                Permission.CUSTOMER_MANAGE,
                Permission.SUPPLIER_VIEW,
                Permission.SUPPLIER_MANAGE,
                Permission.DELIVERY_VIEW,
                Permission.DELIVERY_MANAGE,
                Permission.FINANCE_VIEW,
                Permission.REPORT_VIEW,
                Permission.ANALYTICS_VIEW
        ));

        ROLE_PERMISSIONS.put(RoleType.HR_MANAGER, EnumSet.of(
                Permission.EMPLOYEE_CREATE,
                Permission.EMPLOYEE_VIEW,
                Permission.EMPLOYEE_UPDATE,
                Permission.EMPLOYEE_INVITE,
                Permission.EMPLOYEE_ACTIVATE,
                Permission.EMPLOYEE_DEACTIVATE,
                Permission.EMPLOYEE_TRANSFER,
                Permission.EMPLOYEE_TERMINATE,
                Permission.DEPARTMENT_VIEW,
                Permission.POSITION_VIEW,
                Permission.POSITION_CREATE,
                Permission.POSITION_UPDATE,
                Permission.POSITION_DELETE,
                Permission.EMPLOYEE_DOCUMENT_VIEW,
                Permission.EMPLOYEE_DOCUMENT_MANAGE,
                Permission.ATTENDANCE_VIEW,
                Permission.ATTENDANCE_MANAGE,
                Permission.ATTENDANCE_VIEW_SELF, Permission.ATTENDANCE_CHECK_IN, Permission.ATTENDANCE_CHECK_OUT,
                Permission.ATTENDANCE_SCHEDULE_CREATE, Permission.ATTENDANCE_SCHEDULE_UPDATE, Permission.ATTENDANCE_SCHEDULE_ACTIVATE, Permission.ATTENDANCE_SCHEDULE_ARCHIVE, Permission.ATTENDANCE_SCHEDULE_ASSIGN,
                Permission.ATTENDANCE_VIEW_ALL, Permission.ATTENDANCE_REVIEW, Permission.ATTENDANCE_ADJUSTMENT_CREATE, Permission.ATTENDANCE_ADJUSTMENT_APPROVE, Permission.ATTENDANCE_ADJUSTMENT_REJECT,
                Permission.ATTENDANCE_OVERTIME_VIEW, Permission.ATTENDANCE_OVERTIME_APPROVE, Permission.ATTENDANCE_OVERTIME_REJECT, Permission.ATTENDANCE_ABSENCE_VIEW, Permission.ATTENDANCE_ABSENCE_REVIEW, Permission.ATTENDANCE_FINALIZE, Permission.ATTENDANCE_REOPEN
                , Permission.HR_POLICY_VIEW, Permission.HR_POLICY_EDIT
        ));

        ROLE_PERMISSIONS.put(RoleType.PROCUREMENT_MANAGER, EnumSet.of(
                Permission.PROCUREMENT_VIEW,
                Permission.PROCUREMENT_MANAGE,
                Permission.PURCHASE_REQUEST_VIEW,
                Permission.PURCHASE_REQUEST_CREATE,
                Permission.PURCHASE_ORDER_VIEW,
                Permission.PURCHASE_ORDER_CREATE,
                Permission.PURCHASE_ORDER_UPDATE,
                Permission.PURCHASE_ORDER_APPROVE,
                Permission.SUPPLIER_VIEW,
                Permission.SUPPLIER_MANAGE,
                Permission.SHIPMENT_VIEW,
                Permission.SHIPMENT_MANAGE,
                Permission.SHIPMENT_UPDATE,
                Permission.PROCUREMENT_REPORT_VIEW,
                Permission.ATTENDANCE_VIEW_SELF,
                Permission.ATTENDANCE_CHECK_IN,
                Permission.ATTENDANCE_CHECK_OUT
        ));

        ROLE_PERMISSIONS.put(RoleType.PROCUREMENT_EMPLOYEE, EnumSet.of(
                Permission.PROCUREMENT_VIEW,
                Permission.PURCHASE_REQUEST_VIEW,
                Permission.PURCHASE_REQUEST_CREATE,
                Permission.PURCHASE_ORDER_VIEW,
                Permission.PURCHASE_ORDER_CREATE,
                Permission.PURCHASE_ORDER_UPDATE,
                Permission.SUPPLIER_VIEW,
                Permission.SHIPMENT_VIEW,
                Permission.SHIPMENT_UPDATE,
                Permission.ATTENDANCE_VIEW_SELF,
                Permission.ATTENDANCE_CHECK_IN,
                Permission.ATTENDANCE_CHECK_OUT
        ));

        ROLE_PERMISSIONS.put(RoleType.WAREHOUSE_MANAGER, EnumSet.of(
                Permission.LOCATION_VIEW,
                Permission.PRODUCT_MANAGE,
                Permission.GOODS_RECEIVE,
                Permission.GOODS_INSPECT,
                Permission.PUT_AWAY,
                Permission.INVENTORY_VIEW,
                Permission.INVENTORY_MANAGE,
                Permission.INVENTORY_COUNT,
                Permission.STOCK_TRANSFER,
                Permission.REPLENISHMENT,
                Permission.PICK,
                Permission.PACK,
                Permission.RETURN_VIEW,
                Permission.RETURN_PROCESS,
                Permission.WAREHOUSE_REPORT_VIEW,
                Permission.ATTENDANCE_VIEW_SELF,
                Permission.ATTENDANCE_CHECK_IN,
                Permission.ATTENDANCE_CHECK_OUT
        ));

        ROLE_PERMISSIONS.put(RoleType.WAREHOUSE_EMPLOYEE, EnumSet.of(
                Permission.GOODS_RECEIVE,
                Permission.GOODS_INSPECT,
                Permission.PUT_AWAY,
                Permission.INVENTORY_VIEW,
                Permission.INVENTORY_COUNT,
                Permission.PICK,
                Permission.PACK,
                Permission.RETURN_VIEW,
                Permission.RETURN_PROCESS,
                Permission.ATTENDANCE_VIEW_SELF,
                Permission.ATTENDANCE_CHECK_IN,
                Permission.ATTENDANCE_CHECK_OUT
        ));

        ROLE_PERMISSIONS.put(RoleType.SALES_MANAGER, EnumSet.of(
                Permission.SALES_VIEW,
                Permission.SALES_MANAGE,
                Permission.PRODUCT_VIEW,
                Permission.PRICE_MANAGE,
                Permission.DISCOUNT_MANAGE,
                Permission.CUSTOMER_VIEW,
                Permission.CUSTOMER_MANAGE,
                Permission.RETURN_VIEW,
                Permission.RETURN_PROCESS,
                Permission.SALES_REPORT_VIEW,
                Permission.EMPLOYEE_VIEW,
                Permission.EMPLOYEE_MANAGE,
                Permission.ATTENDANCE_VIEW_SELF,
                Permission.ATTENDANCE_CHECK_IN,
                Permission.ATTENDANCE_CHECK_OUT
        ));

        ROLE_PERMISSIONS.put(RoleType.CASHIER, EnumSet.of(
                Permission.SALE_CREATE,
                Permission.SALE_VIEW,
                Permission.PRODUCT_VIEW,
                Permission.PRODUCT_SCAN,
                Permission.CUSTOMER_VIEW,
                Permission.PAYMENT_RECEIVE,
                Permission.RECEIPT_CREATE,
                Permission.RETURN_PROCESS,
                Permission.ATTENDANCE_VIEW_SELF,
                Permission.ATTENDANCE_CHECK_IN,
                Permission.ATTENDANCE_CHECK_OUT

        ));

        ROLE_PERMISSIONS.put(RoleType.FINANCE_MANAGER, EnumSet.of(
                Permission.FINANCE_VIEW,
                Permission.FINANCE_MANAGE,
                Permission.REVENUE_VIEW,
                Permission.REVENUE_MANAGE,
                Permission.REVENUE_RECORD,
                Permission.EXPENSE_VIEW,
                Permission.EXPENSE_MANAGE,
                Permission.EXPENSE_CREATE,
                Permission.FINANCE_PAYMENT_REQUEST_VIEW,
                Permission.FINANCE_PAYMENT_REQUEST_APPROVE,
                Permission.FINANCE_PAYMENT_EXECUTE,
                Permission.FINANCE_CASH_BOX_VIEW,
                Permission.FINANCE_CASH_BOX_UPDATE,
                Permission.FINANCE_CASH_BOX_ASSIGN,
                Permission.FINANCE_CASH_TRANSACTION_VIEW,
                Permission.FINANCE_CASH_IN,
                Permission.FINANCE_CASH_OUT,
                Permission.FINANCE_CASH_TRANSFER,
                Permission.FINANCE_CASH_RECONCILE,
                Permission.SUPPLIER_PAYMENT_MANAGE,
                Permission.SUPPLIER_PAYMENT_VIEW,
                Permission.SUPPLIER_PAYMENT_RECORD,
                Permission.EMPLOYEE_PAYMENT_MANAGE,
                Permission.OPERATING_EXPENSE_MANAGE,
                Permission.OPERATING_EXPENSE_CREATE,
                Permission.CASH_ACCOUNT_MANAGE,
                Permission.BANK_ACCOUNT_MANAGE,
                Permission.CASH_TRANSACTION_RECORD,
                Permission.TAX_MANAGE,
                Permission.FINANCIAL_REPORT_VIEW,
                Permission.FINANCE_SUPPLIER_PAYMENT,
                Permission.FINANCE_PAYROLL_PAYMENT,
                Permission.FINANCE_EMPLOYEE_LOAN_PAYMENT,
                Permission.FINANCE_EXPENSE_PAYMENT,
                Permission.PAYROLL_VIEW,
                Permission.PAYROLL_MANAGE,
                Permission.ATTENDANCE_VIEW_SELF,
                Permission.ATTENDANCE_CHECK_IN,
                Permission.ATTENDANCE_CHECK_OUT
        ));

        ROLE_PERMISSIONS.put(RoleType.FINANCE_EMPLOYEE, EnumSet.of(
                Permission.FINANCE_VIEW,
                Permission.REVENUE_VIEW,
                Permission.REVENUE_RECORD,
                Permission.EXPENSE_VIEW,
                Permission.EXPENSE_CREATE,
                Permission.FINANCE_PAYMENT_REQUEST_VIEW,
                Permission.SUPPLIER_PAYMENT_VIEW,
                Permission.SUPPLIER_PAYMENT_RECORD,
                Permission.CASH_TRANSACTION_RECORD,
                Permission.FINANCE_PAYMENT_EXECUTE,
                Permission.FINANCE_CASH_BOX_VIEW,
                Permission.FINANCE_CASH_TRANSACTION_VIEW,
                Permission.FINANCE_CASH_IN,
                Permission.FINANCE_CASH_OUT,
                Permission.FINANCE_CASH_TRANSFER,
                Permission.FINANCE_SUPPLIER_PAYMENT,
                Permission.FINANCE_PAYROLL_PAYMENT,
                Permission.FINANCE_EMPLOYEE_LOAN_PAYMENT,
                Permission.FINANCE_EXPENSE_PAYMENT,
                Permission.PAYROLL_VIEW,
                Permission.ATTENDANCE_VIEW_SELF,
                Permission.ATTENDANCE_CHECK_IN,
                Permission.ATTENDANCE_CHECK_OUT
        ));

        ROLE_PERMISSIONS.put(RoleType.DELIVERY_MANAGER, EnumSet.of(
                Permission.DELIVERY_VIEW,
                Permission.DELIVERY_MANAGE,
                Permission.DELIVERY_ASSIGN,
                Permission.DELIVERY_TRACK,
                Permission.DELIVERY_REPORT_VIEW,
                Permission.EMPLOYEE_VIEW,
                Permission.EMPLOYEE_MANAGE
        ));

        ROLE_PERMISSIONS.put(RoleType.DELIVERY_EMPLOYEE, EnumSet.of(
                Permission.DELIVERY_VIEW_ASSIGNED,
                Permission.DELIVERY_START,
                Permission.DELIVERY_UPDATE,
                Permission.DELIVERY_COMPLETE,
                Permission.DELIVERY_FAIL,
                Permission.DELIVERY_PAYMENT_COLLECT
        ));

        ROLE_PERMISSIONS.put(RoleType.IT_MAINTENANCE_MANAGER, EnumSet.of(
                Permission.MAINTENANCE_VIEW,
                Permission.MAINTENANCE_MANAGE,
                Permission.MAINTENANCE_ASSIGN,
                Permission.ASSET_VIEW,
                Permission.ASSET_MANAGE,
                Permission.EMPLOYEE_VIEW,
                Permission.EMPLOYEE_MANAGE,
                Permission.MAINTENANCE_REPORT_VIEW
        ));

        ROLE_PERMISSIONS.put(RoleType.IT_MAINTENANCE_EMPLOYEE, EnumSet.of(
                Permission.MAINTENANCE_VIEW_ASSIGNED,
                Permission.MAINTENANCE_REQUEST_CREATE,
                Permission.MAINTENANCE_UPDATE,
                Permission.MAINTENANCE_COMPLETE,
                Permission.ASSET_VIEW,
                Permission.MAINTENANCE_COST_RECORD
        ));
    }

    private PermissionCatalog() {
    }

    public static Set<Permission> permissionsFor(RoleType role) {
        return ROLE_PERMISSIONS.getOrDefault(role, Set.of());
    }
}
