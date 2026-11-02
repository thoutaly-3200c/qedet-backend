package q.qedet.hr.request.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.request.entity.HrRequest;
import java.util.*;
public interface HrRequestRepository extends JpaRepository<HrRequest, UUID> {
    Optional<HrRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
