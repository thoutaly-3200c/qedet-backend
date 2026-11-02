package q.qedet.common.entitlement;

import org.springframework.stereotype.Component;

import q.qedet.common.model.PlanFeature;
import q.qedet.organization.entity.Organization;


@Component
public class AccessPolicy {

    private final EntitlementService entitlementService;

    public AccessPolicy(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    public void requireFeature(Organization organization, PlanFeature feature) {
        entitlementService.requireFeature(organization, feature);
    }

    public boolean hasFeature(Organization organization, PlanFeature feature) {
        return entitlementService.hasFeature(organization, feature);
    }
}
