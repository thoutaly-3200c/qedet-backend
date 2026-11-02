package q.qedet.organization.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

@Entity
@Table(name = "organization_configurations")
@Getter
@Setter
@NoArgsConstructor
public class OrganizationConfiguration extends TimestampedEntity {
    @OneToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Column(length = 3) private String currency = "USD";
    @Column(precision = 7, scale = 4) private java.math.BigDecimal salesTaxRate = java.math.BigDecimal.ZERO;
    @Column(nullable = false) private boolean salesPricesTaxInclusive;
    @Column(length = 16) private String language = "en";
    @Column(length = 32) private String dateFormat = "yyyy-MM-dd";
    @Column(length = 16) private String timeFormat = "HH:mm";
    @Enumerated(EnumType.STRING) @Column(length = 12) private java.time.DayOfWeek firstDayOfWeek = java.time.DayOfWeek.MONDAY;

    @Column(nullable = false) private boolean emailEnabled = true;
    @Column(nullable = false) private boolean smsEnabled;
    @Column(nullable = false) private boolean pushNotificationsEnabled = true;
    @Column(length = 160) private String defaultSenderName;
    @Column(length = 254) private String defaultSenderEmail;

    @Column(nullable = false) private Integer sessionTimeoutMinutes = 480;
    @Column(nullable = false) private boolean mfaEnabled;
    @Column(nullable = false) private boolean mfaRequired;
    @Column(nullable = false) private boolean allowMultipleSessions = true;
}
