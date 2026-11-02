package q.qedet.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import q.qedet.product.entity.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("globalProductRepository")
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("select op.product from GlobalOrganizationProduct op where op.organization.id = :organizationId")
    List<Product> findAllByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("select op.product from GlobalOrganizationProduct op where op.product.id = :id and op.organization.id = :organizationId")
    Optional<Product> findByIdAndOrganizationId(@Param("id") UUID id, @Param("organizationId") UUID organizationId);

    @Query("select op.product from GlobalOrganizationProduct op where op.organization.id = :organizationId and op.organizationSku = :sku")
    Optional<Product> findByOrganizationIdAndSku(@Param("organizationId") UUID organizationId, @Param("sku") String sku);

    @Query("""
            select op.product from GlobalOrganizationProduct op
            where op.organization.id = :organizationId and (
                exists (select 1 from GlobalProductBarcode barcode
                        where barcode.product = op.product and barcode.barcode = :barcode)
                or exists (select 1 from GlobalProductBarcode barcode
                           where barcode.organizationProduct = op and barcode.barcode = :barcode)
            )
            """)
    Optional<Product> findByOrganizationIdAndBarcode(@Param("organizationId") UUID organizationId,
                                                      @Param("barcode") String barcode);
}
