package q.qedet.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.auth.entity.AuthSession;


import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    Optional<AuthSession> findByAccessTokenHash(String accessTokenHash);
    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

    @Modifying
    @Query("update AuthSession s set s.revokedAt = :revokedAt where s.user.id = :userId and s.revokedAt is null")
    int revokeAllActiveByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
