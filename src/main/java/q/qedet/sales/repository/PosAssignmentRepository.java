package q.qedet.sales.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.sales.entity.PosAssignment;
import java.util.Optional;
import java.util.UUID;

public interface PosAssignmentRepository extends JpaRepository<PosAssignment, UUID> {
    Optional<PosAssignment> findByOrganizationIdAndCashierIdAndActiveTrue(UUID organizationId, UUID cashierId);
    Optional<PosAssignment> findByOrganizationIdAndCashierId(UUID organizationId, UUID cashierId);
}
