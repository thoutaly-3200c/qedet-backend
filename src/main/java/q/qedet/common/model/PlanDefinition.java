package q.qedet.common.model;



import java.util.Map;
import java.util.Set;

public record PlanDefinition(
       SubscriptionPlan plan,
        Set<PlanFeature> features,
        Map<PlanQuota, Integer> quotas
) {
}
