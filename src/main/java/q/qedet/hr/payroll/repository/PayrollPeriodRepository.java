package q.qedet.hr.payroll.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.payroll.entity.PayrollPeriod;
import java.util.*;
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, UUID> {
    Optional<PayrollPeriod> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
