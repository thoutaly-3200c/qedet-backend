package q.qedet.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.product.entity.OrganizationProduct;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationProductRepository extends JpaRepository<OrganizationProduct, UUID> {

    List<OrganizationProduct> findAllByOrganizationId(UUID organizationId);

    Optional<OrganizationProduct> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndProductId(UUID organizationId, UUID productId);

    Optional<OrganizationProduct> findByOrganizationIdAndProductId(UUID organizationId, UUID productId);
}
