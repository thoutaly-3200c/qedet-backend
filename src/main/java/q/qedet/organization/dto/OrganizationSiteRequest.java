package q.qedet.organization.dto;

import jakarta.validation.constraints.*;
import q.qedet.common.model.LocationType;

public record OrganizationSiteRequest(
        @NotBlank @Size(max = 40) String code, @NotBlank @Size(max = 160) String name,
        LocationType type, @Size(max = 500) String description,
        @Size(max = 240) String addressLine1, @Size(max = 240) String addressLine2,
        @Size(max = 100) String city, @Size(max = 100) String state, @Size(max = 30) String postalCode,
        @Size(max = 100) String country, @Size(max = 40) String phone, @Email @Size(max = 254) String email,
        Boolean isDefault) { }
