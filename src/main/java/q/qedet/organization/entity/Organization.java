package q.qedet.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.*;


import java.time.Instant;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
public class Organization extends TimestampedEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String country;

    @Column
    private String timezone;

    @Column(length = 254)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(length = 240)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 30)
    private String postalCode;

    @Column(length = 254)
    private String website;

    @Column(length = 500)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessType businessType = BusinessType.GENERAL_TRADE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    @Column
    private Instant trialEndsAt;

    @Column
    private Instant renewalAt;

    public Organization(String name, BusinessType businessType) {
        this.name = name;
        this.businessType = businessType == null ? BusinessType.GENERAL_TRADE : businessType;
    }
}
