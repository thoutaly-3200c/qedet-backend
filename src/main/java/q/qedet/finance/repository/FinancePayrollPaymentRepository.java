package q.qedet.finance.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.finance.entity.FinancePayrollPayment; import java.util.*;
public interface FinancePayrollPaymentRepository extends JpaRepository<FinancePayrollPayment, UUID> { List<FinancePayrollPayment> findAllByOrganizationIdOrderByPaidAtDesc(UUID organizationId); Optional<FinancePayrollPayment> findByOperationId(UUID operationId); }
