package q.qedet.common.entitlement;

import org.springframework.stereotype.Service;

import q.qedet.common.model.PlanFeature;
import q.qedet.common.model.PlanQuota;
import q.qedet.common.model.SubscriptionPlan;
import q.qedet.common.model.SubscriptionStatus;
import q.qedet.common.exception.ForbiddenException;
import q.qedet.organization.entity.Organization;

import java.util.Set;

@Service
public class EntitlementService {

    public boolean hasFeature(Organization organization, PlanFeature feature) {
        if (organization == null) {
            return false;
        }
        if (organization.getSubscriptionStatus() != SubscriptionStatus.ACTIVE
                && organization.getSubscriptionStatus() != SubscriptionStatus.TRIAL) {
            return false;
        }
        return enabledFeatures(organization.getSubscriptionPlan()).contains(feature);
    }

    public void requireFeature(Organization organization, PlanFeature feature) {
        if (!hasFeature(organization, feature)) {
            throw new ForbiddenException("Feature not available: " + feature);
        }
    }

    public Set<PlanFeature> enabledFeatures(SubscriptionPlan plan) {
        return PlanCatalog.features(plan);
    }

    public Integer quota(SubscriptionPlan plan, PlanQuota quota) {
        return PlanCatalog.quota(plan, quota);
    }
}
