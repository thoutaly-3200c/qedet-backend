package q.qedet.organization.dto;

import q.qedet.common.model.BusinessType;
import q.qedet.common.model.OrganizationStatus;
import q.qedet.common.model.SubscriptionPlan;
import q.qedet.common.model.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String country,
        String timezone,
        BusinessType businessType,
        OrganizationStatus status,
        SubscriptionPlan subscriptionPlan,
        SubscriptionStatus subscriptionStatus,
        Instant trialEndsAt,
        Instant renewalAt,
        String email,
        String phone,
        String address,
        String city,
        String state,
        String postalCode,
        String website,
        String logoUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
