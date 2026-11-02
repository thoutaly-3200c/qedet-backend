package q.qedet.hr.leave.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.leave.entity.LeaveType;
import java.util.*;
public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {
    Optional<LeaveType> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<LeaveType> findAllByOrganizationId(UUID organizationId);
}
