package q.qedet.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;

import java.time.Instant;

@Entity
@Table(name = "auth_sessions")
@Getter
@Setter
@NoArgsConstructor
public class AuthSession extends TimestampedEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false, unique = true, length = 128)
    private String accessTokenHash;

    @Column(nullable = false, unique = true, length = 128)
    private String refreshTokenHash;

    @Column(nullable = false)
    private Instant accessTokenExpiresAt;

    @Column(nullable = false)
    private Instant refreshTokenExpiresAt;

    @Column
    private Instant revokedAt;

    public AuthSession(UserAccount user, Employee employee, String accessTokenHash, String refreshTokenHash, Instant accessTokenExpiresAt, Instant refreshTokenExpiresAt) {
        this.user = user;
        this.employee = employee;
        this.accessTokenHash = accessTokenHash;
        this.refreshTokenHash = refreshTokenHash;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

}
