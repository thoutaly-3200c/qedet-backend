package q.qedet.organization.service;

import q.qedet.common.model.LocationType;

public enum OrganizationSiteTemplate {
    SALES_HALL("SALES_HALL", "Sales Hall", LocationType.SALES_HALL, true),
    WAREHOUSE("WAREHOUSE", "Warehouse", LocationType.WAREHOUSE, false);
    private final String code, displayName; private final LocationType type; private final boolean defaultSite;
    OrganizationSiteTemplate(String code, String displayName, LocationType type, boolean defaultSite) { this.code=code; this.displayName=displayName; this.type=type; this.defaultSite=defaultSite; }
    public String code() { return code; } public String displayName() { return displayName; } public LocationType type() { return type; } public boolean defaultSite() { return defaultSite; }
}
