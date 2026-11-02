package q.qedet.sales.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import q.qedet.sales.entity.PosQuote;
import java.util.Optional;
import java.util.UUID;

public interface PosQuoteRepository extends JpaRepository<PosQuote, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PosQuote> findByOrganizationIdAndTokenHash(UUID organizationId, String tokenHash);
}
