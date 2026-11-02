package q.qedet.organization.mapper;

import q.qedet.organization.dto.OrganizationResponse;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.Position;
import q.qedet.organization.dto.PositionResponse;

public final class OrganizationMapper {
    private OrganizationMapper() {
    }

    public static OrganizationResponse organization(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getCountry(),
                organization.getTimezone(),
                organization.getBusinessType(),
                organization.getStatus(),
                organization.getSubscriptionPlan(),
                organization.getSubscriptionStatus(),
                organization.getTrialEndsAt(),
                organization.getRenewalAt(),
                organization.getEmail(),
                organization.getPhone(),
                organization.getAddress(),
                organization.getCity(),
                organization.getState(),
                organization.getPostalCode(),
                organization.getWebsite(),
                organization.getLogoUrl(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }

    public static PositionResponse position(Position position) {
        return new PositionResponse(position.getId(), position.getOrganization().getId(),
                position.getCode(), position.getName(), position.getDescription(), position.isActive(),
                new PositionResponse.DepartmentReference(position.getDepartment().getId(), position.getDepartment().getName()),
                position.getDefaultSite() == null ? null : new PositionResponse.SiteReference(
                        position.getDefaultSite().getId(), position.getDefaultSite().getName(),
                        position.getDefaultSite().getCode(), position.getDefaultSite().getType().name()),
                position.getCreatedAt(), position.getUpdatedAt());
    }
}
