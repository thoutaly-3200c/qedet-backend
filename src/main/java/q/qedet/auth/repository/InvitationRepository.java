package q.qedet.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import q.qedet.auth.entity.Invitation;
import q.qedet.common.model.InvitationStatus;

import jakarta.persistence.LockModeType;
import java.util.UUID;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    Optional<Invitation> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Invitation> findByOrganizationIdAndTargetEmailIgnoreCaseAndStatus(
            UUID organizationId, String targetEmail, InvitationStatus status);
}
