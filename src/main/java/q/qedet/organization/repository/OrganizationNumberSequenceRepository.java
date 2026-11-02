package q.qedet.organization.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import q.qedet.organization.entity.OrganizationNumberSequence;
import q.qedet.organization.entity.SequenceType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationNumberSequenceRepository extends JpaRepository<OrganizationNumberSequence, UUID> {
    List<OrganizationNumberSequence> findAllByOrganizationId(UUID organizationId);
    Optional<OrganizationNumberSequence> findByOrganizationIdAndSequenceType(UUID organizationId, SequenceType type);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from OrganizationNumberSequence s where s.organization.id = :organizationId and s.sequenceType = :type")
    Optional<OrganizationNumberSequence> findForUpdate(@Param("organizationId") UUID organizationId, @Param("type") SequenceType type);
}
