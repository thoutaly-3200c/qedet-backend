//package q.qedet;
//
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import q.qedet.common.model.RoleType;
//import q.qedet.employee.entity.Role;
//import q.qedet.employee.entity.RolePermission;
//import q.qedet.employee.repository.PermissionRepository;
//import q.qedet.employee.repository.RolePermissionRepository;
//import q.qedet.employee.repository.RoleRepository;
//
//import java.util.EnumMap;
//import java.util.EnumSet;
//import java.util.Map;
//import java.util.Set;
//
//@Component
//@Order(1)
//@RequiredArgsConstructor
//public class RolePermissionBootstrap implements CommandLineRunner {
//
//    private static final Logger log = LoggerFactory.getLogger(RolePermissionBootstrap.class);
//    private static final Map<RoleType, Set<q.qedet.common.model.Permission>> DEFAULT_PERMISSIONS =
//            new EnumMap<>(RoleType.class);
//
//    static {
//        DEFAULT_PERMISSIONS.put(RoleType.OWNER, EnumSet.allOf(q.qedet.common.model.Permission.class));
//        DEFAULT_PERMISSIONS.put(RoleType.GENERAL_MANAGER, permissions(
//                q.qedet.common.model.Permission.ORGANIZATION_VIEW,
//                q.qedet.common.model.Permission.ORGANIZATION_UPDATE,
//                q.qedet.common.model.Permission.EMPLOYEE_VIEW,
//                q.qedet.common.model.Permission.EMPLOYEE_CREATE,
//                q.qedet.common.model.Permission.EMPLOYEE_UPDATE,
//                q.qedet.common.model.Permission.EMPLOYEE_INVITE,
//                q.qedet.common.model.Permission.EMPLOYEE_ACTIVATE,
//                q.qedet.common.model.Permission.EMPLOYEE_DEACTIVATE,
//                q.qedet.common.model.Permission.ROLE_ASSIGN,
//                q.qedet.common.model.Permission.DEPARTMENT_VIEW,
//                q.qedet.common.model.Permission.LOCATION_VIEW,
//                q.qedet.common.model.Permission.POSITION_VIEW,
//                q.qedet.common.model.Permission.REPORT_VIEW,
//                q.qedet.common.model.Permission.ANALYTICS_VIEW,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_ALL,
//                q.qedet.common.model.Permission.ATTENDANCE_REVIEW
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.HR_MANAGER, permissions(
//                q.qedet.common.model.Permission.EMPLOYEE_VIEW,
//                q.qedet.common.model.Permission.EMPLOYEE_CREATE,
//                q.qedet.common.model.Permission.EMPLOYEE_UPDATE,
//                q.qedet.common.model.Permission.EMPLOYEE_INVITE,
//                q.qedet.common.model.Permission.EMPLOYEE_ACTIVATE,
//                q.qedet.common.model.Permission.EMPLOYEE_DEACTIVATE,
//                q.qedet.common.model.Permission.EMPLOYEE_TRANSFER,
//                q.qedet.common.model.Permission.EMPLOYEE_TERMINATE,
//                q.qedet.common.model.Permission.EMPLOYEE_MANAGE,
//                q.qedet.common.model.Permission.ROLE_ASSIGN,
//                q.qedet.common.model.Permission.DEPARTMENT_VIEW,
//                q.qedet.common.model.Permission.LOCATION_VIEW,
//                q.qedet.common.model.Permission.POSITION_VIEW,
//                q.qedet.common.model.Permission.EMPLOYEE_DOCUMENT_VIEW,
//                q.qedet.common.model.Permission.EMPLOYEE_DOCUMENT_MANAGE,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_ALL,
//                q.qedet.common.model.Permission.ATTENDANCE_REVIEW
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.SALES_MANAGER, permissions(
//                q.qedet.common.model.Permission.SALES_VIEW,
//                q.qedet.common.model.Permission.SALES_MANAGE,
//                q.qedet.common.model.Permission.SALE_CREATE,
//                q.qedet.common.model.Permission.SALE_VIEW,
//                q.qedet.common.model.Permission.SALES_REPORT_VIEW,
//                q.qedet.common.model.Permission.CUSTOMER_VIEW,
//                q.qedet.common.model.Permission.CUSTOMER_MANAGE,
//                q.qedet.common.model.Permission.PRODUCT_VIEW,
//                q.qedet.common.model.Permission.PRODUCT_SCAN,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.CASHIER, permissions(
//                q.qedet.common.model.Permission.SALES_VIEW,
//                q.qedet.common.model.Permission.SALE_CREATE,
//                q.qedet.common.model.Permission.SALE_VIEW,
//                q.qedet.common.model.Permission.PAYMENT_RECEIVE,
//                q.qedet.common.model.Permission.RECEIPT_CREATE,
//                q.qedet.common.model.Permission.CUSTOMER_VIEW,
//                q.qedet.common.model.Permission.PRODUCT_VIEW,
//                q.qedet.common.model.Permission.PRODUCT_SCAN,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.PROCUREMENT_MANAGER, permissions(
//                q.qedet.common.model.Permission.PROCUREMENT_VIEW,
//                q.qedet.common.model.Permission.PROCUREMENT_MANAGE,
//                q.qedet.common.model.Permission.PURCHASE_REQUEST_VIEW,
//                q.qedet.common.model.Permission.PURCHASE_REQUEST_CREATE,
//                q.qedet.common.model.Permission.PURCHASE_ORDER_VIEW,
//                q.qedet.common.model.Permission.PURCHASE_ORDER_CREATE,
//                q.qedet.common.model.Permission.PURCHASE_ORDER_UPDATE,
//                q.qedet.common.model.Permission.PURCHASE_ORDER_APPROVE,
//                q.qedet.common.model.Permission.PROCUREMENT_REPORT_VIEW,
//                q.qedet.common.model.Permission.SUPPLIER_VIEW,
//                q.qedet.common.model.Permission.SUPPLIER_MANAGE,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.PROCUREMENT_EMPLOYEE, permissions(
//                q.qedet.common.model.Permission.PROCUREMENT_VIEW,
//                q.qedet.common.model.Permission.PURCHASE_REQUEST_VIEW,
//                q.qedet.common.model.Permission.PURCHASE_REQUEST_CREATE,
//                q.qedet.common.model.Permission.PURCHASE_ORDER_VIEW,
//                q.qedet.common.model.Permission.SUPPLIER_VIEW,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.WAREHOUSE_MANAGER, permissions(
//                q.qedet.common.model.Permission.INVENTORY_VIEW,
//                q.qedet.common.model.Permission.INVENTORY_MANAGE,
//                q.qedet.common.model.Permission.INVENTORY_COUNT,
//                q.qedet.common.model.Permission.STOCK_TRANSFER,
//                q.qedet.common.model.Permission.REPLENISHMENT,
//                q.qedet.common.model.Permission.WAREHOUSE_REPORT_VIEW,
//                q.qedet.common.model.Permission.GOODS_RECEIVE,
//                q.qedet.common.model.Permission.GOODS_INSPECT,
//                q.qedet.common.model.Permission.PUT_AWAY,
//                q.qedet.common.model.Permission.PICK,
//                q.qedet.common.model.Permission.PACK,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.WAREHOUSE_EMPLOYEE, permissions(
//                q.qedet.common.model.Permission.INVENTORY_VIEW,
//                q.qedet.common.model.Permission.INVENTORY_COUNT,
//                q.qedet.common.model.Permission.GOODS_RECEIVE,
//                q.qedet.common.model.Permission.GOODS_INSPECT,
//                q.qedet.common.model.Permission.PUT_AWAY,
//                q.qedet.common.model.Permission.PICK,
//                q.qedet.common.model.Permission.PACK,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.FINANCE_MANAGER, permissions(
//                q.qedet.common.model.Permission.FINANCE_VIEW,
//                q.qedet.common.model.Permission.FINANCE_MANAGE,
//                q.qedet.common.model.Permission.REVENUE_VIEW,
//                q.qedet.common.model.Permission.REVENUE_MANAGE,
//                q.qedet.common.model.Permission.REVENUE_RECORD,
//                q.qedet.common.model.Permission.EXPENSE_VIEW,
//                q.qedet.common.model.Permission.EXPENSE_MANAGE,
//                q.qedet.common.model.Permission.EXPENSE_CREATE,
//                q.qedet.common.model.Permission.PAYMENT_REQUEST_VIEW,
//                q.qedet.common.model.Permission.PAYMENT_REQUEST_CREATE,
//                q.qedet.common.model.Permission.PAYMENT_REQUEST_APPROVE,
//                q.qedet.common.model.Permission.CASH_ACCOUNT_MANAGE,
//                q.qedet.common.model.Permission.BANK_ACCOUNT_MANAGE,
//                q.qedet.common.model.Permission.CASH_TRANSACTION_RECORD,
//                q.qedet.common.model.Permission.TAX_MANAGE,
//                q.qedet.common.model.Permission.FINANCIAL_REPORT_VIEW,
//                q.qedet.common.model.Permission.EMPLOYEE_PAYMENT_MANAGE,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.FINANCE_EMPLOYEE, permissions(
//                q.qedet.common.model.Permission.FINANCE_VIEW,
//                q.qedet.common.model.Permission.REVENUE_VIEW,
//                q.qedet.common.model.Permission.EXPENSE_VIEW,
//                q.qedet.common.model.Permission.EXPENSE_CREATE,
//                q.qedet.common.model.Permission.PAYMENT_REQUEST_VIEW,
//                q.qedet.common.model.Permission.PAYMENT_REQUEST_CREATE,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.DELIVERY_MANAGER, permissions(
//                q.qedet.common.model.Permission.DELIVERY_VIEW,
//                q.qedet.common.model.Permission.DELIVERY_MANAGE,
//                q.qedet.common.model.Permission.DELIVERY_ASSIGN,
//                q.qedet.common.model.Permission.DELIVERY_TRACK,
//                q.qedet.common.model.Permission.DELIVERY_REPORT_VIEW,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.DELIVERY_EMPLOYEE, permissions(
//                q.qedet.common.model.Permission.DELIVERY_VIEW_ASSIGNED,
//                q.qedet.common.model.Permission.DELIVERY_START,
//                q.qedet.common.model.Permission.DELIVERY_UPDATE,
//                q.qedet.common.model.Permission.DELIVERY_COMPLETE,
//                q.qedet.common.model.Permission.DELIVERY_FAIL,
//                q.qedet.common.model.Permission.DELIVERY_PAYMENT_COLLECT,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.IT_MAINTENANCE_MANAGER, permissions(
//                q.qedet.common.model.Permission.MAINTENANCE_VIEW,
//                q.qedet.common.model.Permission.MAINTENANCE_MANAGE,
//                q.qedet.common.model.Permission.MAINTENANCE_ASSIGN,
//                q.qedet.common.model.Permission.ASSET_VIEW,
//                q.qedet.common.model.Permission.ASSET_MANAGE,
//                q.qedet.common.model.Permission.MAINTENANCE_REPORT_VIEW,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.IT_MAINTENANCE_EMPLOYEE, permissions(
//                q.qedet.common.model.Permission.MAINTENANCE_VIEW_ASSIGNED,
//                q.qedet.common.model.Permission.MAINTENANCE_REQUEST_CREATE,
//                q.qedet.common.model.Permission.MAINTENANCE_UPDATE,
//                q.qedet.common.model.Permission.MAINTENANCE_COMPLETE,
//                q.qedet.common.model.Permission.MAINTENANCE_COST_RECORD,
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//        DEFAULT_PERMISSIONS.put(RoleType.EMPLOYEE, permissions(
//                q.qedet.common.model.Permission.ATTENDANCE_VIEW_SELF,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_IN,
//                q.qedet.common.model.Permission.ATTENDANCE_CHECK_OUT
//        ));
//    }
//
//    private final RoleRepository roleRepository;
//    private final PermissionRepository permissionRepository;
//    private final RolePermissionRepository rolePermissionRepository;
//
//    @Override
//    @Transactional
//    public void run(String... args) {
//        int rolesProcessed = 0;
//        int permissionsCreated = 0;
//        int permissionsExisting = 0;
//
//        log.info("ROLE PERMISSION BOOTSTRAP STARTED");
//        for (Role role : roleRepository.findAll()) {
//            RoleType roleType = roleTypeFor(role);
//            if (roleType == null) {
//                continue;
//            }
//
//            rolesProcessed++;
//            log.info("Role {}:", roleType);
//            for (q.qedet.common.model.Permission defaultPermission : DEFAULT_PERMISSIONS.get(roleType)) {
//                q.qedet.employee.entity.Permission permission = permissionRepository
//                        .findByCode(defaultPermission.name())
//                        .orElseThrow(() -> new IllegalStateException(
//                                "Permission definition is missing for code " + defaultPermission.name()));
//
//                if (rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), permission.getId())) {
//                    permissionsExisting++;
//                    log.info("  skipped: {}", defaultPermission.name());
//                    continue;
//                }
//
//                RolePermission rolePermission = new RolePermission();
//                rolePermission.setRole(role);
//                rolePermission.setPermission(permission);
//                rolePermissionRepository.save(rolePermission);
//                permissionsCreated++;
//                log.info("  created: {}", defaultPermission.name());
//            }
//        }
//        log.info("ROLE PERMISSION BOOTSTRAP FINISHED - roles processed: {}, permissions created: {}, permissions already existing: {}",
//                rolesProcessed, permissionsCreated, permissionsExisting);
//    }
//
//    private RoleType roleTypeFor(Role role) {
//        if (role.getCode() == null || role.getCode().isBlank()) {
//            return null;
//        }
//        try {
//            return RoleType.valueOf(role.getCode().trim().toUpperCase());
//        } catch (IllegalArgumentException exception) {
//            log.debug("Skipping role {} with unsupported code {}", role.getId(), role.getCode());
//            return null;
//        }
//    }
//
//    private static Set<q.qedet.common.model.Permission> permissions(
//            q.qedet.common.model.Permission... permissions) {
//        return EnumSet.copyOf(Set.of(permissions));
//    }
//}
