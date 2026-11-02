package q.qedet.hr.leave.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.leave.entity.LeaveRequest;
import java.util.*;
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    Optional<LeaveRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
