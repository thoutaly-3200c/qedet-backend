package q.qedet.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.product.entity.ProductCategory;

import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {

    Optional<ProductCategory> findByNameIgnoreCase(String name);
}
