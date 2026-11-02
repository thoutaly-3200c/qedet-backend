package q.qedet.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.common.model.RegistrationStatus;
import q.qedet.common.model.UserStatus;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserAccount extends TimestampedEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus registrationStatus = RegistrationStatus.PENDING_EMAIL_VERIFICATION;

    private String registrationName;
    private String registrationPhone;
    private String registrationCompanyName;
    private String registrationCountry;
    private String registrationTimezone;

    /**
     * Stable Google OpenID subject ({@code sub}). Unique when present.
     * Used to resolve the same Qedet account across Google logins.
     */
    @Column(unique = true, length = 128)
    private String googleSubject;

    /** Optional Google profile picture URL from verified claims. */
    @Column(length = 512)
    private String googlePictureUrl;

    public UserAccount(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }
}
