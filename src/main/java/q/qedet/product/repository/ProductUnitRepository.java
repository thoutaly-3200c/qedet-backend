package q.qedet.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.product.entity.ProductUnit;

import java.util.Optional;
import java.util.UUID;

public interface ProductUnitRepository extends JpaRepository<ProductUnit, UUID> {

    Optional<ProductUnit> findByNameIgnoreCase(String name);

    Optional<ProductUnit> findBySymbolIgnoreCase(String symbol);
}
