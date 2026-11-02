package q.qedet.auth.security;

import org.junit.jupiter.api.Test;
import q.qedet.common.model.Permission;
import q.qedet.common.model.RoleType;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionCatalogDefaultRolesTest {

    private static final Set<Permission> ATTENDANCE = EnumSet.of(
            Permission.ATTENDANCE_VIEW_SELF,
            Permission.ATTENDANCE_CHECK_IN,
            Permission.ATTENDANCE_CHECK_OUT);

    @Test
    void financeRolesHaveTheRequiredApprovalAndExecutionBoundary() {
        assertThat(PermissionCatalog.permissionsFor(RoleType.FINANCE_MANAGER)).isEqualTo(withAttendance(
                Permission.FINANCE_VIEW, Permission.FINANCE_MANAGE, Permission.REVENUE_VIEW, Permission.REVENUE_MANAGE,
                Permission.REVENUE_RECORD, Permission.EXPENSE_VIEW, Permission.EXPENSE_MANAGE, Permission.EXPENSE_CREATE,
                Permission.FINANCE_PAYMENT_REQUEST_VIEW, Permission.FINANCE_PAYMENT_REQUEST_APPROVE, Permission.FINANCE_PAYMENT_EXECUTE,
                Permission.FINANCE_CASH_BOX_VIEW, Permission.FINANCE_CASH_BOX_UPDATE, Permission.FINANCE_CASH_BOX_ASSIGN,
                Permission.FINANCE_CASH_TRANSACTION_VIEW, Permission.FINANCE_CASH_IN, Permission.FINANCE_CASH_OUT,
                Permission.FINANCE_CASH_TRANSFER, Permission.FINANCE_CASH_RECONCILE, Permission.CASH_ACCOUNT_MANAGE,
                Permission.BANK_ACCOUNT_MANAGE, Permission.CASH_TRANSACTION_RECORD, Permission.TAX_MANAGE,
                Permission.FINANCIAL_REPORT_VIEW, Permission.OPERATING_EXPENSE_MANAGE, Permission.OPERATING_EXPENSE_CREATE,
                Permission.EMPLOYEE_PAYMENT_MANAGE, Permission.SUPPLIER_PAYMENT_VIEW, Permission.SUPPLIER_PAYMENT_RECORD,
                Permission.SUPPLIER_PAYMENT_MANAGE, Permission.FINANCE_SUPPLIER_PAYMENT, Permission.FINANCE_PAYROLL_PAYMENT,
                Permission.FINANCE_EMPLOYEE_LOAN_PAYMENT, Permission.FINANCE_EXPENSE_PAYMENT, Permission.PAYROLL_VIEW,
                Permission.PAYROLL_MANAGE));

        Set<Permission> employee = PermissionCatalog.permissionsFor(RoleType.FINANCE_EMPLOYEE);
        assertThat(employee).isEqualTo(withAttendance(
                Permission.FINANCE_VIEW, Permission.REVENUE_VIEW, Permission.REVENUE_RECORD, Permission.EXPENSE_VIEW,
                Permission.EXPENSE_CREATE, Permission.FINANCE_PAYMENT_REQUEST_VIEW, Permission.FINANCE_PAYMENT_EXECUTE,
                Permission.FINANCE_CASH_BOX_VIEW, Permission.FINANCE_CASH_TRANSACTION_VIEW, Permission.FINANCE_CASH_IN,
                Permission.FINANCE_CASH_OUT, Permission.FINANCE_CASH_TRANSFER, Permission.CASH_TRANSACTION_RECORD,
                Permission.SUPPLIER_PAYMENT_VIEW, Permission.SUPPLIER_PAYMENT_RECORD, Permission.FINANCE_SUPPLIER_PAYMENT,
                Permission.FINANCE_PAYROLL_PAYMENT, Permission.FINANCE_EMPLOYEE_LOAN_PAYMENT, Permission.FINANCE_EXPENSE_PAYMENT,
                Permission.PAYROLL_VIEW));
        assertThat(employee).doesNotContain(
                Permission.FINANCE_PAYMENT_REQUEST_APPROVE,
                Permission.FINANCE_CASH_BOX_CREATE);
    }

    @Test
    void procurementAndWarehouseRolesMatchTheirDefaultResponsibilities() {
        assertThat(PermissionCatalog.permissionsFor(RoleType.PROCUREMENT_MANAGER)).isEqualTo(withAttendance(
                Permission.PROCUREMENT_VIEW, Permission.PROCUREMENT_MANAGE, Permission.PURCHASE_REQUEST_VIEW,
                Permission.PURCHASE_REQUEST_CREATE, Permission.PURCHASE_ORDER_VIEW, Permission.PURCHASE_ORDER_CREATE,
                Permission.PURCHASE_ORDER_UPDATE, Permission.PURCHASE_ORDER_APPROVE, Permission.PROCUREMENT_REPORT_VIEW,
                Permission.SHIPMENT_VIEW, Permission.SHIPMENT_MANAGE, Permission.SHIPMENT_UPDATE, Permission.SUPPLIER_VIEW,
                Permission.SUPPLIER_MANAGE));
        assertThat(PermissionCatalog.permissionsFor(RoleType.PROCUREMENT_EMPLOYEE)).isEqualTo(withAttendance(
                Permission.PROCUREMENT_VIEW, Permission.PURCHASE_REQUEST_VIEW, Permission.PURCHASE_REQUEST_CREATE,
                Permission.PURCHASE_ORDER_VIEW, Permission.PURCHASE_ORDER_CREATE, Permission.PURCHASE_ORDER_UPDATE,
                Permission.SHIPMENT_VIEW, Permission.SHIPMENT_UPDATE, Permission.SUPPLIER_VIEW));
        assertThat(PermissionCatalog.permissionsFor(RoleType.PROCUREMENT_EMPLOYEE))
                .doesNotContain(Permission.PROCUREMENT_MANAGE, Permission.PURCHASE_ORDER_APPROVE);

        assertThat(PermissionCatalog.permissionsFor(RoleType.WAREHOUSE_MANAGER)).isEqualTo(withAttendance(
                Permission.LOCATION_VIEW, Permission.PRODUCT_MANAGE, Permission.INVENTORY_VIEW, Permission.INVENTORY_MANAGE, Permission.INVENTORY_COUNT, Permission.STOCK_TRANSFER,
                Permission.REPLENISHMENT, Permission.WAREHOUSE_REPORT_VIEW, Permission.GOODS_RECEIVE, Permission.GOODS_INSPECT,
                Permission.PUT_AWAY, Permission.PICK, Permission.PACK, Permission.RETURN_VIEW, Permission.RETURN_PROCESS));
        assertThat(PermissionCatalog.permissionsFor(RoleType.WAREHOUSE_EMPLOYEE)).isEqualTo(withAttendance(
                Permission.INVENTORY_VIEW, Permission.INVENTORY_COUNT, Permission.GOODS_RECEIVE, Permission.GOODS_INSPECT,
                Permission.PUT_AWAY, Permission.PICK, Permission.PACK, Permission.RETURN_VIEW, Permission.RETURN_PROCESS));
        assertThat(PermissionCatalog.permissionsFor(RoleType.WAREHOUSE_EMPLOYEE))
                .doesNotContain(Permission.INVENTORY_MANAGE, Permission.STOCK_TRANSFER, Permission.REPLENISHMENT);
    }

    private static Set<Permission> withAttendance(Permission... permissions) {
        Set<Permission> result = EnumSet.copyOf(ATTENDANCE);
        result.addAll(EnumSet.of(permissions[0], permissions));
        return result;
    }
}
