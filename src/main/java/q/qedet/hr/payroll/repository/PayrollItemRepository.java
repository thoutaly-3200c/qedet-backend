package q.qedet.hr.payroll.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.payroll.entity.PayrollItem;
import java.util.*;
public interface PayrollItemRepository extends JpaRepository<PayrollItem, UUID> {
    List<PayrollItem> findAllByPayrollPeriodIdAndOrganizationId(UUID periodId, UUID organizationId);
}
