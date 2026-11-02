package q.qedet.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.product.entity.ProductBrand;

import java.util.Optional;
import java.util.UUID;

public interface ProductBrandRepository extends JpaRepository<ProductBrand, UUID> {

    Optional<ProductBrand> findByNameIgnoreCase(String name);
}
