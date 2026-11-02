package q.qedet.sales.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.model.*;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.finance.entity.CashBox;
import q.qedet.finance.model.*;
import q.qedet.finance.repository.CashBoxRepository;
import q.qedet.organization.entity.*;
import q.qedet.organization.repository.*;
import q.qedet.product.entity.*;
import q.qedet.product.repository.*;
import q.qedet.qr.repository.QrCodeRepository;
import q.qedet.sales.dto.SalesDtos;
import q.qedet.sales.entity.*;
import q.qedet.sales.repository.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CashierServiceTest {
    @Mock ActorContext actor;
    @Mock EmployeeRepository employees;
    @Mock PosAssignmentRepository assignments;
    @Mock CashBoxRepository boxes;
    @Mock OrganizationConfigurationRepository configurations;
    @Mock OrganizationSiteRepository sites;
    @Mock QrCodeRepository qrs;
    @Mock ProductBarcodeRepository barcodes;
    @Mock ProductRepository products;
    @Mock OrganizationProductRepository organizationProducts;
    @Mock SalesPackageRepository packages;
    @Mock SalesPriceRepository prices;
    @InjectMocks CashierService service;

    UUID orgId = UUID.randomUUID(), cashierId = UUID.randomUUID(), hallId = UUID.randomUUID(), productId = UUID.randomUUID();
    Organization org;
    OrganizationSite hall;
    Employee employee;
    CashBox box;

    @BeforeEach void setup() {
        org = new Organization(); org.setId(orgId);
        hall = new OrganizationSite(org, "HALL", "Sales Hall", LocationType.SALES_HALL); hall.setId(hallId);
        employee = new Employee(org, "E1", "Test", "Cashier", null, null, RoleType.CASHIER);
        employee.setId(cashierId); employee.setStatus(EmployeeStatus.ACTIVE);
        PosAssignment assignment = new PosAssignment(); assignment.setId(UUID.randomUUID()); assignment.setCode("POS-1");
        assignment.setOrganization(org); assignment.setSalesHall(hall); assignment.setCashier(employee);
        box = new CashBox(); box.setId(UUID.randomUUID()); box.setOrganization(org); box.setOwnerEmployee(employee);
        box.setType(CashBoxType.EMPLOYEE); box.setStatus(CashBoxStatus.ACTIVE); box.setCurrency("SAR"); box.setName("Cashier box");
        OrganizationConfiguration config = new OrganizationConfiguration(); config.setCurrency("SAR");
        when(actor.organizationId()).thenReturn(orgId); when(actor.employeeId()).thenReturn(cashierId);
        when(employees.findByIdAndOrganizationId(cashierId, orgId)).thenReturn(Optional.of(employee));
        when(assignments.findByOrganizationIdAndCashierIdAndActiveTrue(orgId, cashierId)).thenReturn(Optional.of(assignment));
        when(boxes.findAllByOrganizationIdOrderByNameAsc(orgId)).thenReturn(List.of(box));
        when(configurations.findByOrganizationId(orgId)).thenReturn(Optional.of(config));
    }

    @Test void assignmentUsesPersistedHallEmployeeAndBox() {
        var result = service.assignment();
        assertThat(result.salesHallId()).isEqualTo(hallId);
        assertThat(result.cashierId()).isEqualTo(cashierId);
        assertThat(result.cashbox().id()).isEqualTo(box.getId());
        assertThat(result.currency()).isEqualTo("SAR");
        assertThat(result.status()).isEqualTo("OPEN");
        verify(actor).requirePermission(Permission.SALE_VIEW);
    }

    @Test void assignmentRejectsMissingPosOrCashbox() {
        when(assignments.findByOrganizationIdAndCashierIdAndActiveTrue(orgId, cashierId)).thenReturn(Optional.empty());
        assertThatThrownBy(service::assignment).isInstanceOf(ConflictException.class).hasMessageContaining("no active POS");
        PosAssignment assignment = new PosAssignment(); assignment.setSalesHall(hall);
        when(assignments.findByOrganizationIdAndCashierIdAndActiveTrue(orgId, cashierId)).thenReturn(Optional.of(assignment));
        when(boxes.findAllByOrganizationIdOrderByNameAsc(orgId)).thenReturn(List.of());
        assertThatThrownBy(service::assignment).isInstanceOf(ConflictException.class).hasMessageContaining("cashbox is missing");
    }

    @Test void scanRejectsOtherHall() {
        assertThatThrownBy(() -> service.scan(new SalesDtos.CashierScanRequest("123", UUID.randomUUID())))
                .isInstanceOf(ConflictException.class).hasMessageContaining("not assigned");
    }

    @Test void scanRejectsBarcodeFromOtherOrganization() {
        ProductBarcode barcode = new ProductBarcode(); barcode.setBarcode("123");
        Organization other = new Organization(); other.setId(UUID.randomUUID()); barcode.setOrganization(other);
        when(barcodes.findByBarcode("123")).thenReturn(Optional.of(barcode));
        assertThatThrownBy(() -> service.scan(new SalesDtos.CashierScanRequest("123", hallId)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test void scanRejectsMissingPrice() {
        Product product = new Product(); product.setId(productId); product.setName("Rice");
        OrganizationProduct op = new OrganizationProduct(); op.setOrganization(org); op.setProduct(product);
        ProductBarcode barcode = new ProductBarcode(); barcode.setBarcode("123"); barcode.setProduct(product);
        when(barcodes.findByBarcode("123")).thenReturn(Optional.of(barcode));
        when(products.findById(productId)).thenReturn(Optional.of(product));
        when(organizationProducts.findByOrganizationIdAndProductId(orgId, productId)).thenReturn(Optional.of(op));
        assertThatThrownBy(() -> service.scan(new SalesDtos.CashierScanRequest("123", hallId)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Selling price is missing");
    }

    @Test void managerCanPersistAnExplicitAssignment() {
        when(sites.findByIdAndOrganizationId(hallId, orgId)).thenReturn(Optional.of(hall));
        when(assignments.findByOrganizationIdAndCashierId(orgId, cashierId)).thenReturn(Optional.empty());
        when(assignments.save(any(PosAssignment.class))).thenAnswer(inv -> {
            PosAssignment assignment = inv.getArgument(0); assignment.setId(UUID.randomUUID()); return assignment;
        });
        var result = service.assign(cashierId, new SalesDtos.PosAssignmentRequest("POS-1", hallId, true));
        assertThat(result.cashierId()).isEqualTo(cashierId);
        assertThat(result.salesHallId()).isEqualTo(hallId);
        verify(actor).requirePermission(Permission.SALES_MANAGE);
        verify(assignments).save(any(PosAssignment.class));
    }
}
