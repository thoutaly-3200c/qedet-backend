package q.qedet.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import q.qedet.auth.entity.OAuthExchangeCode;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OAuthExchangeCodeRepository extends JpaRepository<OAuthExchangeCode, UUID> {
    Optional<OAuthExchangeCode> findByCodeHash(String codeHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OAuthExchangeCode c
               set c.consumedAt = :consumedAt
             where c.codeHash = :codeHash
               and c.consumedAt is null
               and c.expiresAt > :now
            """)
    int consumeIfValid(@Param("codeHash") String codeHash,
                       @Param("now") Instant now,
                       @Param("consumedAt") Instant consumedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OAuthExchangeCode c
               set c.accessToken = null,
                   c.refreshToken = null
             where c.codeHash = :codeHash
               and c.consumedAt is not null
            """)
    int clearStoredTokens(@Param("codeHash") String codeHash);
}
