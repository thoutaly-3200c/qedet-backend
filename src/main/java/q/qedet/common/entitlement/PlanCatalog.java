package q.qedet.common.entitlement;



import q.qedet.common.model.PlanDefinition;
import q.qedet.common.model.PlanFeature;
import q.qedet.common.model.PlanQuota;
import q.qedet.common.model.SubscriptionPlan;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class PlanCatalog {

    private static final Map<SubscriptionPlan, PlanDefinition> PLANS = new EnumMap<>(SubscriptionPlan.class);

    static {
        PLANS.put(SubscriptionPlan.FREE, new PlanDefinition(
                SubscriptionPlan.FREE,
                EnumSet.of(
                        PlanFeature.PRODUCTS,
                        PlanFeature.INVENTORY,
                        PlanFeature.SALES_POS,
                        PlanFeature.CUSTOMERS,
                        PlanFeature.BASIC_FINANCE
                ),
                Map.of(
                        PlanQuota.MAX_ACTIVE_EMPLOYEES, 3,
                        PlanQuota.MAX_PURCHASE_DOCUMENTS_PER_MONTH, 25,
                        PlanQuota.MAX_WAREHOUSES, 1,
                        PlanQuota.MAX_QR_CODES_PER_MONTH, 100,
                        PlanQuota.MAX_BRANCHES, 1,
                        PlanQuota.MAX_ACTIVE_INTEGRATIONS, 0
                )
        ));

        PLANS.put(SubscriptionPlan.PRO, new PlanDefinition(
                SubscriptionPlan.PRO,
                EnumSet.of(
                        PlanFeature.PRODUCTS,
                        PlanFeature.INVENTORY,
                        PlanFeature.SALES_POS,
                        PlanFeature.CUSTOMERS,
                        PlanFeature.BASIC_FINANCE,
                        PlanFeature.EMPLOYEES,
                        PlanFeature.PROCUREMENT,
                        PlanFeature.WAREHOUSE,
                        PlanFeature.QR_CODES,
                        PlanFeature.CUSTOM_ROLES_AND_PERMISSIONS,
                        PlanFeature.ADVANCED_PERMISSIONS,
                        PlanFeature.DELIVERY_LOGISTICS,
                        PlanFeature.ADVANCED_ANALYTICS,
                        PlanFeature.MULTIPLE_BRANCHES,
                        PlanFeature.API_INTEGRATIONS
                ),
                Map.of(
                        PlanQuota.MAX_BRANCHES, 3,
                        PlanQuota.MAX_ACTIVE_INTEGRATIONS, 5
                )
        ));

        PLANS.put(SubscriptionPlan.ENTERPRISE, new PlanDefinition(
                SubscriptionPlan.ENTERPRISE,
                EnumSet.allOf(PlanFeature.class),
                Map.of()
        ));
    }

    private PlanCatalog() {
    }

    public static PlanDefinition definition(SubscriptionPlan plan) {
        return PLANS.get(plan);
    }

    public static Set<PlanFeature> features(SubscriptionPlan plan) {
        PlanDefinition definition = definition(plan);
        return definition == null ? Set.of() : definition.features();
    }

    public static Integer quota(SubscriptionPlan plan, PlanQuota quota) {
        PlanDefinition definition = definition(plan);
        return definition == null ? null : definition.quotas().get(quota);
    }
}
