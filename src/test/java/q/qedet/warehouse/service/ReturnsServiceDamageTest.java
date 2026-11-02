package q.qedet.warehouse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.finance.repository.FinanceRequestRepository;
import q.qedet.organization.entity.*;
import q.qedet.organization.repository.*;
import q.qedet.product.entity.Product;
import q.qedet.product.repository.*;
import q.qedet.qr.service.QrCodeService;
import q.qedet.warehouse.dto.ReturnDtos;
import q.qedet.warehouse.entity.WarehouseReturn;
import q.qedet.warehouse.entity.InventoryBalance;
import q.qedet.warehouse.repository.InventoryBalanceRepository;
import q.qedet.warehouse.repository.StockMovementRepository;
import q.qedet.warehouse.repository.*;

@ExtendWith(MockitoExtension.class)
class ReturnsServiceDamageTest {
    @Mock ActorContext actor; @Mock WarehouseReturnRepository returns; @Mock WarehouseReturnItemRepository items;
    @Mock OrganizationRepository organizations; @Mock ProductRepository products; @Mock OrganizationProductRepository organizationProducts;
    @Mock InventoryBalanceRepository balances; @Mock StockMovementRepository movements; @Mock OrganizationZoneRepository zones;
    @Mock QrCodeService qrCodes; @Mock FinanceRequestRepository financeRequests; @Mock EmployeeRepository employees;

    @Test void movesAndHoldsStockAtADifferentInspectionSiteZone() {
        Fixture f = fixture("10", "0", true);
        ReturnDtos.ReturnResponse response = service().damage(f.request());
        assertThat(response.status()).isEqualTo("PENDING_INSPECTION");
        assertThat(f.source.getOnHand()).isEqualByComparingTo("8");
        assertThat(f.destination.getOnHand()).isEqualByComparingTo("2");
        assertThat(f.destination.getReserved()).isEqualByComparingTo("2");
        verify(movements).save(argThat(m -> "DAMAGE_TO_INSPECTION".equals(m.getMovementType()) && m.getFromZone() == f.sourceZone && m.getToZone() == f.inspectionZone));
    }

    @Test void holdsStockInPlaceWhenSourceAndInspectionSiteZoneMatch() {
        Fixture f = fixture("10", "0", false);
        service().damage(f.request());
        assertThat(f.source.getOnHand()).isEqualByComparingTo("10");
        assertThat(f.source.getReserved()).isEqualByComparingTo("2");
        verifyNoInteractions(movements);
    }

    @Test void rejectsNoEligibleOrInsufficientInventory() {
        Fixture f = fixture("1", "0", true);
        assertThatThrownBy(() -> service().damage(f.request())).isInstanceOf(ConflictException.class)
                .hasMessageContaining("exceeds available inventory");
        Fixture missing = fixture(null, null, true);
        assertThatThrownBy(() -> service().damage(missing.request())).isInstanceOf(ConflictException.class);
    }

    private ReturnsService service() { return new ReturnsService(actor, returns, items, organizations, products, organizationProducts, balances, movements, zones, qrCodes, financeRequests, employees); }

    private Fixture fixture(String onHand, String reserved, boolean differentInspectionZone) {
        UUID orgId=UUID.randomUUID(), productId=UUID.randomUUID(); Organization organization=new Organization(); organization.setId(orgId);
        OrganizationSite site=new OrganizationSite(); site.setOrganization(organization); OrganizationZone sourceZone=new OrganizationZone(site,"SOURCE","Source"); sourceZone.setId(UUID.randomUUID());
        OrganizationZone inspectionZone=differentInspectionZone?new OrganizationZone(site,"INSPECT","Inspect"):sourceZone; if(differentInspectionZone) inspectionZone.setId(UUID.randomUUID());
        InventoryBalance source=new InventoryBalance(); source.setOrganization(organization); source.setProductId(productId); source.setZone(sourceZone); source.setOnHand(onHand==null?BigDecimal.ZERO:new BigDecimal(onHand)); source.setReserved(reserved==null?BigDecimal.ZERO:new BigDecimal(reserved));
        InventoryBalance destination=differentInspectionZone?new InventoryBalance():source; if(differentInspectionZone){ destination.setOrganization(organization); destination.setProductId(productId); destination.setZone(inspectionZone); destination.setOnHand(BigDecimal.ZERO); destination.setReserved(BigDecimal.ZERO); }
        Product product=new Product(); product.setId(productId); when(actor.organizationId()).thenReturn(orgId); when(organizations.findById(orgId)).thenReturn(Optional.of(organization)); when(products.findByIdAndOrganizationId(productId,orgId)).thenReturn(Optional.of(product));
        when(zones.findByIdAndSiteOrganizationIdAndActiveTrue(inspectionZone.getId(),orgId)).thenReturn(Optional.of(inspectionZone)); when(balances.findAllForUpdateByOrganizationIdAndProductId(orgId,productId)).thenReturn(differentInspectionZone?List.of(source,destination):List.of(source)); when(returns.save(any(WarehouseReturn.class))).thenAnswer(i->i.getArgument(0));
        return new Fixture(productId,inspectionZone,sourceZone,source,destination);
    }
    private record Fixture(UUID productId, OrganizationZone inspectionZone, OrganizationZone sourceZone, InventoryBalance source, InventoryBalance destination) {
        ReturnDtos.DamageCreate request() { return new ReturnDtos.DamageCreate("Water damage",inspectionZone.getId(),List.of(new ReturnDtos.ItemInput(productId,null,new BigDecimal("2"),"DAMAGED","Keep dry"))); }
    }
}
