package q.qedet.organization.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.policy.model.HolidayType;
import q.qedet.organization.entity.Organization;
import java.time.LocalDate;

@Entity @Table(name="hr_organization_holidays", uniqueConstraints=@UniqueConstraint(name="uk_org_holiday_date_name",columnNames={"organization_id","holiday_date","name"}))
@Getter @Setter @NoArgsConstructor
public class OrganizationHoliday extends TimestampedEntity {
 @ManyToOne(optional=false) @JoinColumn(name="organization_id",nullable=false) private Organization organization;
 @Column(nullable=false,length=160) private String name;
 @Column(name="holiday_date",nullable=false) private LocalDate date;
 @Column(nullable=false) private boolean recurring;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private HolidayType type;
 @Column(nullable=false) private boolean active=true;
}
