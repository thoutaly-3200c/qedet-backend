package q.qedet.organization.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

@Entity
@Table(name = "organization_number_sequences", uniqueConstraints = @UniqueConstraint(name = "uk_org_sequence_type", columnNames = {"organization_id", "sequence_type"}))
@Getter @Setter @NoArgsConstructor
public class OrganizationNumberSequence extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Enumerated(EnumType.STRING) @Column(name = "sequence_type", nullable = false, length = 32) private SequenceType sequenceType;
    @Column(nullable = false, length = 32) private String prefix = "";
    @Column(nullable = false) private Long nextNumber = 1L;
    @Column(nullable = false) private Integer numberLength = 5;
    @Version private Long version;
}
