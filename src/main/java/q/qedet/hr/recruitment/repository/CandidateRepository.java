package q.qedet.hr.recruitment.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.recruitment.entity.Candidate;
import java.util.*;
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {
    Optional<Candidate> findByUserId(UUID userId);
}
