package q.qedet.sales.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.sales.entity.SalesPrice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesPriceRepository extends JpaRepository<SalesPrice, UUID> {

    List<SalesPrice> findAllByOrganizationId(UUID organizationId);

    Optional<SalesPrice> findByOrganizationIdAndProductId(
            UUID organizationId,
            UUID productId
    );

    Optional<SalesPrice> findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
    );
}