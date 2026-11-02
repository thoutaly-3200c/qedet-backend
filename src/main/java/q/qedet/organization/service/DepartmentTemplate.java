package q.qedet.organization.service;

public enum DepartmentTemplate {
    GENERAL("General", "GENERAL"),
    SALES("Sales", "SALES"),
    PROCUREMENT("Procurement", "PROCUREMENT"),
    WAREHOUSE("Warehouse", "WAREHOUSE"),
    FINANCE("Finance", "FINANCE"),
    DELIVERY("Delivery", "DELIVERY"),
    HR("Human Resources", "HR");

    private final String name;
    private final String code;

    DepartmentTemplate(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}
