package q.qedet.organization.dto;

import q.qedet.common.model.*;
import java.time.Instant;
import java.util.UUID;

public record OrganizationSiteResponse(UUID id, UUID organizationId, String code, String name, LocationType type,
        String description, String addressLine1, String addressLine2, String city, String state, String postalCode,
        String country, String phone, String email, boolean isDefault, LocationStatus status, Instant createdAt, Instant updatedAt) { }
