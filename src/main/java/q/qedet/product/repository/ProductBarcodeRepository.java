package q.qedet.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.product.entity.ProductBarcode;

import java.util.Optional;
import java.util.UUID;

public interface ProductBarcodeRepository extends JpaRepository<ProductBarcode, UUID> {

    Optional<ProductBarcode> findByBarcode(String barcode);

    Optional<ProductBarcode> findByOrganizationIdAndBarcode(UUID organizationId, String barcode);

    boolean existsByBarcode(String barcode);
}
