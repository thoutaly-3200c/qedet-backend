package q.qedet.sales.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.sales.entity.SalesReturn;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesReturnRepository extends JpaRepository<SalesReturn, UUID> {
    List<SalesReturn> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<SalesReturn> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
