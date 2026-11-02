package q.qedet.hr.recruitment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.recruitment.entity.Offer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {
    List<Offer> findAllByApplicationIdAndOrganizationIdOrderByCreatedAtDesc(UUID applicationId, UUID organizationId);
    Optional<Offer> findByApplicationIdAndOrganizationId(UUID applicationId, UUID organizationId);
    boolean existsByApplicationId(UUID applicationId);

    Optional<Offer> findByIdAndApplicationCandidateId(
            UUID id,
            UUID candidateId
    );
    List<Offer> findAllByApplicationCandidateIdOrderByCreatedAtDesc(UUID candidateId);
    Optional<Offer> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
