package q.qedet.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import q.qedet.warehouse.entity.InventoryBalance;
import q.qedet.warehouse.dto.WarehouseDtos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, UUID> {
    List<InventoryBalance> findAllByOrganizationIdOrderByZoneSiteNameAsc(UUID organizationId);
    List<InventoryBalance> findAllByOrganizationIdAndProductId(UUID organizationId, UUID productId);
    @Query("""
            select new q.qedet.warehouse.dto.WarehouseDtos$InventoryResponse(
                balance.id, balance.productId, product.name, organizationProduct.organizationSku,
                zone.id, zone.code, zone.name, site.id, site.code, site.name,
                balance.onHand, balance.reserved, balance.onHand - balance.reserved)
            from InventoryBalance balance
            join balance.zone zone
            join zone.site site
            join GlobalProduct product on product.id = balance.productId
            left join GlobalOrganizationProduct organizationProduct
                on organizationProduct.organization.id = balance.organization.id
                and organizationProduct.product.id = balance.productId
            where balance.organization.id = :organizationId
            order by site.name asc, zone.name asc, product.name asc
            """)
    List<WarehouseDtos.InventoryResponse> findInventoryResponsesByOrganizationId(@Param("organizationId") UUID organizationId);
    Optional<InventoryBalance> findByOrganizationIdAndProductIdAndZoneId(UUID organizationId, UUID productId, UUID zoneId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryBalance i where i.organization.id = :organizationId and i.productId = :productId order by i.zone.id")
    List<InventoryBalance> findAllForUpdateByOrganizationIdAndProductId(@Param("organizationId") UUID organizationId,
                                                                          @Param("productId") UUID productId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryBalance i where i.organization.id = :organizationId and i.productId = :productId and i.zone.id = :zoneId")
    Optional<InventoryBalance> findForUpdate(@Param("organizationId") UUID organizationId,
                                             @Param("productId") UUID productId,
                                             @Param("zoneId") UUID zoneId);
}
