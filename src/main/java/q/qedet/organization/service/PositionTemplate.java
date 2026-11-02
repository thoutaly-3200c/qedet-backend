package q.qedet.organization.service;

import q.qedet.common.model.RoleType;

public enum PositionTemplate {
    OWNER("Owner", "OWNER", DepartmentTemplate.GENERAL, OrganizationSiteTemplate.SALES_HALL, RoleType.OWNER),
    GENERAL_MANAGER("General Manager", "GENERAL_MANAGER", DepartmentTemplate.GENERAL, OrganizationSiteTemplate.SALES_HALL, RoleType.GENERAL_MANAGER),
    SALES_MANAGER("Sales Manager", "SALES_MANAGER", DepartmentTemplate.SALES, OrganizationSiteTemplate.SALES_HALL, RoleType.SALES_MANAGER),
    SALES_EMPLOYEE("Sales Employee", "SALES_EMPLOYEE", DepartmentTemplate.SALES, OrganizationSiteTemplate.SALES_HALL, RoleType.CASHIER),
    CASHIER("Cashier", "CASHIER", DepartmentTemplate.SALES, OrganizationSiteTemplate.SALES_HALL, RoleType.CASHIER),
    PROCUREMENT_MANAGER("Procurement Manager", "PROCUREMENT_MANAGER", DepartmentTemplate.PROCUREMENT, OrganizationSiteTemplate.SALES_HALL, RoleType.PROCUREMENT_MANAGER),
    PROCUREMENT_EMPLOYEE("Procurement Employee", "PROCUREMENT_EMPLOYEE", DepartmentTemplate.PROCUREMENT, OrganizationSiteTemplate.SALES_HALL, RoleType.PROCUREMENT_EMPLOYEE),
    WAREHOUSE_MANAGER("Warehouse Manager", "WAREHOUSE_MANAGER", DepartmentTemplate.WAREHOUSE, OrganizationSiteTemplate.WAREHOUSE, RoleType.WAREHOUSE_MANAGER),
    WAREHOUSE_EMPLOYEE("Warehouse Employee", "WAREHOUSE_EMPLOYEE", DepartmentTemplate.WAREHOUSE, OrganizationSiteTemplate.WAREHOUSE, RoleType.WAREHOUSE_EMPLOYEE),
    FINANCE_MANAGER("Finance Manager", "FINANCE_MANAGER", DepartmentTemplate.FINANCE, OrganizationSiteTemplate.SALES_HALL, RoleType.FINANCE_MANAGER),
    FINANCE_EMPLOYEE("Finance Employee", "FINANCE_EMPLOYEE", DepartmentTemplate.FINANCE, OrganizationSiteTemplate.SALES_HALL, RoleType.FINANCE_EMPLOYEE),
    HR_MANAGER("HR Manager", "HR_MANAGER", DepartmentTemplate.HR, OrganizationSiteTemplate.SALES_HALL, RoleType.HR_MANAGER),
    HR_EMPLOYEE("HR Employee", "HR_EMPLOYEE", DepartmentTemplate.HR, OrganizationSiteTemplate.SALES_HALL, RoleType.HR_MANAGER);

    private final String name;
    private final String code;
    private final DepartmentTemplate department;
    private final OrganizationSiteTemplate site;
    private final RoleType defaultRole;

    PositionTemplate(String name, String code, DepartmentTemplate department,
                     OrganizationSiteTemplate site, RoleType defaultRole) {
        this.name = name;
        this.code = code;
        this.department = department;
        this.site = site;
        this.defaultRole = defaultRole;
    }

    public String getName() { return name; }
    public String getCode() { return code; }
    public DepartmentTemplate getDepartment() { return department; }
    public OrganizationSiteTemplate getSite() { return site; }
    public RoleType getDefaultRole() { return defaultRole; }
}
