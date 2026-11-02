package q.qedet.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import q.qedet.common.model.BusinessType;

public record OrganizationRequest(
        @NotBlank @Size(max = 160) String name,
        BusinessType businessType,
        @Size(max = 100) String country,
        @Size(max = 100) String timezone,
        @Size(max = 254) String email,
        @Size(max = 40) String phone,
        @Size(max = 240) String address,
        @Size(max = 100) String city,
        @Size(max = 100) String state,
        @Size(max = 30) String postalCode,
        @Size(max = 254) String website,
        @Size(max = 500) String logoUrl
) {
    public OrganizationRequest(String name, BusinessType businessType, String country, String timezone) {
        this(name, businessType, country, timezone, null, null, null, null, null, null, null, null);
    }

    public OrganizationRequest(String name) {
        this(name, null, null, null);
    }
}
