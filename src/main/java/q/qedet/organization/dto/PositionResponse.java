package q.qedet.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record PositionResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String description,
        boolean active,
        DepartmentReference department,
        SiteReference defaultSite,
        Instant createdAt,
        Instant updatedAt
) {
    /** Frontend API name for the OrganizationSite relationship. */
    @com.fasterxml.jackson.annotation.JsonProperty("defaultLocation")
    public SiteReference defaultLocation() { return defaultSite; }

    @com.fasterxml.jackson.annotation.JsonProperty("status")
    public String status() { return active ? "ACTIVE" : "INACTIVE"; }
    public record DepartmentReference(UUID id, String name) {}
    public record SiteReference(UUID id, String name, String code, String type) {}
}
