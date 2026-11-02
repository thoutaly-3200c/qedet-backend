package q.qedet.common.model;

import java.util.EnumSet;
import java.util.Set;

public enum RoleType {
    EMPLOYEE,
    OWNER,
    GENERAL_MANAGER,
    HR_MANAGER,
    PROCUREMENT_MANAGER,
    PROCUREMENT_EMPLOYEE,
    WAREHOUSE_MANAGER,
    WAREHOUSE_EMPLOYEE,
    SALES_MANAGER,
    CASHIER,
    FINANCE_MANAGER,
    FINANCE_EMPLOYEE,
    DELIVERY_MANAGER,
    DELIVERY_EMPLOYEE,
    IT_MAINTENANCE_MANAGER,
    IT_MAINTENANCE_EMPLOYEE;

    public boolean canAssign(RoleType target) {
        return switch (this) {
            case OWNER -> true;
            case GENERAL_MANAGER -> Set.of(
                    HR_MANAGER,
                    PROCUREMENT_MANAGER,
                    PROCUREMENT_EMPLOYEE,
                    WAREHOUSE_MANAGER,
                    WAREHOUSE_EMPLOYEE,
                    SALES_MANAGER,
                    CASHIER,
                    FINANCE_MANAGER,
                    FINANCE_EMPLOYEE,
                    DELIVERY_MANAGER,
                    DELIVERY_EMPLOYEE,
                    IT_MAINTENANCE_MANAGER,
                    IT_MAINTENANCE_EMPLOYEE
            ).contains(target);
            case HR_MANAGER -> Set.of(
                    HR_MANAGER,
                    PROCUREMENT_EMPLOYEE,
                    WAREHOUSE_EMPLOYEE,
                    CASHIER,
                    FINANCE_EMPLOYEE,
                    DELIVERY_EMPLOYEE,
                    IT_MAINTENANCE_EMPLOYEE
            ).contains(target);
            case PROCUREMENT_MANAGER -> Set.of(PROCUREMENT_EMPLOYEE).contains(target);
            case WAREHOUSE_MANAGER -> Set.of(WAREHOUSE_EMPLOYEE).contains(target);
            case SALES_MANAGER -> Set.of(CASHIER).contains(target);
            case FINANCE_MANAGER -> Set.of(FINANCE_EMPLOYEE).contains(target);
            case DELIVERY_MANAGER -> Set.of(DELIVERY_EMPLOYEE).contains(target);
            case IT_MAINTENANCE_MANAGER -> Set.of(IT_MAINTENANCE_EMPLOYEE).contains(target);
            default -> false;
        };
    }

    public boolean isManager() {
        return EnumSet.of(
                OWNER,
                GENERAL_MANAGER,
                HR_MANAGER,
                PROCUREMENT_MANAGER,
                WAREHOUSE_MANAGER,
                SALES_MANAGER,
                FINANCE_MANAGER,
                DELIVERY_MANAGER,
                IT_MAINTENANCE_MANAGER
        ).contains(this);
    }

    public static RoleType fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        String normalized = value.trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');
        return RoleType.valueOf(normalized);
    }
}
