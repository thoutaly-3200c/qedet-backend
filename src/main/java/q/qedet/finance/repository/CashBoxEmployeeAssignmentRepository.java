package q.qedet.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.finance.entity.*;
import java.util.*;

public interface CashBoxEmployeeAssignmentRepository extends JpaRepository<CashBoxEmployeeAssignment, UUID> {
    boolean existsByCashBoxIdAndEmployeeId(UUID cashBoxId, UUID employeeId);
    Optional<CashBoxEmployeeAssignment> findByCashBoxIdAndEmployeeId(UUID cashBoxId, UUID employeeId);
    List<CashBoxEmployeeAssignment> findAllByCashBoxIdAndOrganizationId(UUID cashBoxId, UUID organizationId);
    List<CashBoxEmployeeAssignment> findAllByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
}
