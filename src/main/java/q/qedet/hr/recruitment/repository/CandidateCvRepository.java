package q.qedet.hr.recruitment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.recruitment.entity.CandidateCv;

import java.util.Optional;
import java.util.UUID;

public interface CandidateCvRepository extends JpaRepository<CandidateCv, UUID> {
    Optional<CandidateCv> findByCandidateId(UUID candidateId);
}
