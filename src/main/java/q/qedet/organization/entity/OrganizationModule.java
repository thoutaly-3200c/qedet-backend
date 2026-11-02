package q.qedet.organization.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

@Entity
@Table(name = "organization_modules", uniqueConstraints = @UniqueConstraint(name = "uk_org_module", columnNames = {"organization_id", "module"}))
@Getter @Setter @NoArgsConstructor
public class OrganizationModule extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private OrganizationModuleType module;
    @Column(nullable = false) private boolean enabled;
}
