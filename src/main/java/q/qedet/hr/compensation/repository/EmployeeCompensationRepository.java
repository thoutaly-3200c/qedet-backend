package q.qedet.hr.compensation.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.compensation.entity.EmployeeCompensation;
import java.util.*;
public interface EmployeeCompensationRepository extends JpaRepository<EmployeeCompensation, UUID> {
    Optional<EmployeeCompensation> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
}
