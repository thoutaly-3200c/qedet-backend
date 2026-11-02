package q.qedet.hr.compensation.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.compensation.entity.EmployeeBenefit;
import java.util.*;
public interface EmployeeBenefitRepository extends JpaRepository<EmployeeBenefit, UUID> {
    Optional<EmployeeBenefit> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
