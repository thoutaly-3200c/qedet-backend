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

import java.time.Instant;

@Entity
@Table(name = "oauth_exchange_codes")
@Getter
@Setter
@NoArgsConstructor
public class OAuthExchangeCode extends TimestampedEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "auth_session_id", nullable = false)
    private AuthSession authSession;

    @Column(nullable = false, unique = true, length = 128)
    private String codeHash;

    /**
     * Short-lived plaintext Qedet access token. Stored only until the
     * single-use exchange code is consumed or expires; never placed in URLs.
     * Cleared after a successful exchange.
     */
    @Column(length = 64)
    private String accessToken;

    /**
     * Short-lived plaintext Qedet refresh token. Same lifetime rules as
     * {@link #accessToken}.
     */
    @Column(length = 128)
    private String refreshToken;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant consumedAt;

    public OAuthExchangeCode(AuthSession authSession,
                             String codeHash,
                             String accessToken,
                             String refreshToken,
                             Instant expiresAt) {
        this.authSession = authSession;
        this.codeHash = codeHash;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }
}
