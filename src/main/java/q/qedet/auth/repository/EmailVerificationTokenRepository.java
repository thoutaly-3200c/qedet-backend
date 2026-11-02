package q.qedet.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import q.qedet.auth.entity.EmailVerificationToken;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationToken> findByCodeHash(String codeHash);
    void deleteAllByUserId(UUID userId);
    void deleteAllByExpiresAtBefore(Instant instant);
}