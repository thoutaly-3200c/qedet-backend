package q.qedet.hr.recruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

@Entity
@Table(name = "hr_candidate_cvs")
@Getter
@Setter
@NoArgsConstructor
public class CandidateCv extends TimestampedEntity {
    @OneToOne(optional = false)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private Candidate candidate;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 2000)
    private String storageKey;

    @Column(length = 100)
    private String contentType;
}
