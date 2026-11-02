package q.qedet.common.model;

public enum LocationType {
    HEADQUARTERS,
    BRANCH,
    STORE,
    OFFICE,
    WAREHOUSE,
    FACTORY,
    DISTRIBUTION_CENTER,
    OTHER,
    SALES_HALL,

    /**
     * Retained only so existing rows can be migrated to DISTRIBUTION_CENTER at application startup.
     */
    @Deprecated
    RECEIVING_AREA
}
