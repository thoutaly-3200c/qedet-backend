package q.qedet.hr.recruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.hr.recruitment.model.OfferStatus;
import q.qedet.organization.entity.Organization;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hr_offers", uniqueConstraints = @jakarta.persistence.UniqueConstraint(
        name = "uk_hr_offer_application", columnNames = "application_id"))
@Getter
@Setter
@NoArgsConstructor
public class Offer extends TimestampedEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @jakarta.persistence.OneToOne(optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Column(nullable = false, length = 255)
    private String positionTitle;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal salary;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate expiresOn;

    @Column(length = 4000)
    private String terms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferStatus status = OfferStatus.PENDING;
}
