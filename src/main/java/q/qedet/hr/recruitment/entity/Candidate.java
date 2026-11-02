package q.qedet.hr.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.auth.entity.UserAccount;
import q.qedet.common.model.TimestampedEntity;

@Entity
@Table(name = "hr_candidates", uniqueConstraints = @UniqueConstraint(
        name = "uk_hr_candidate_user", columnNames = "user_id"))
@Getter @Setter @NoArgsConstructor
public class Candidate extends TimestampedEntity {
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;
    @Column(nullable = false) private String firstName;
    @Column(nullable = false) private String lastName;
    @Column(nullable = false) private String email;
    @Column private String phone;
    @Column(length = 2000) private String resumeKey;
    @Column(nullable = false) private boolean archived;
}
