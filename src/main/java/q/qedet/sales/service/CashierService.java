package q.qedet.sales.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.model.LocationStatus;
import q.qedet.common.model.LocationType;
import q.qedet.common.model.Permission;
import q.qedet.common.model.RoleType;
import q.qedet.common.model.EmployeeStatus;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.finance.entity.CashBox;
import q.qedet.finance.model.CashBoxStatus;
import q.qedet.finance.model.CashBoxType;
import q.qedet.finance.repository.CashBoxRepository;
import q.qedet.organization.repository.OrganizationConfigurationRepository;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.product.entity.*;
import q.qedet.product.repository.*;
import q.qedet.product.service.BarcodeNormalizer;
import q.qedet.qr.entity.*;
import q.qedet.qr.repository.QrCodeRepository;
import q.qedet.sales.dto.SalesDtos;
import q.qedet.sales.entity.*;
import q.qedet.sales.repository.*;
import q.qedet.warehouse.entity.ReceivingOperationSerial;
import q.qedet.warehouse.repository.ReceivingOperationSerialRepository;
import java.time.Instant;
import java.util.UUID;

@Service
public class CashierService {
    private final ActorContext actor;
    private final EmployeeRepository employees;
    private final PosAssignmentRepository assignments;
    private final CashBoxRepository cashBoxes;
    private final OrganizationConfigurationRepository configurations;
    private final OrganizationSiteRepository sites;
    private final QrCodeRepository qrCodes;
    private final ProductBarcodeRepository barcodes;
    private final ProductRepository products;
    private final OrganizationProductRepository organizationProducts;
    private final SalesPackageRepository packages;
    private final SalesPriceRepository prices;
    private final ReceivingOperationSerialRepository serials;

    public CashierService(ActorContext actor, EmployeeRepository employees, PosAssignmentRepository assignments,
                          CashBoxRepository cashBoxes, OrganizationConfigurationRepository configurations,
                          OrganizationSiteRepository sites,
                          QrCodeRepository qrCodes, ProductBarcodeRepository barcodes,
                          ProductRepository products, OrganizationProductRepository organizationProducts,
                          SalesPackageRepository packages, SalesPriceRepository prices,
                          ReceivingOperationSerialRepository serials) {
        this.actor = actor; this.employees = employees; this.assignments = assignments;
        this.cashBoxes = cashBoxes; this.configurations = configurations; this.sites = sites; this.qrCodes = qrCodes;
        this.barcodes = barcodes; this.products = products; this.organizationProducts = organizationProducts;
        this.packages = packages; this.prices = prices;
        this.serials = serials;
    }

    private Employee cashier() {
        return employees.findByIdAndOrganizationId(actor.employeeId(), actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Cashier employee not found"));
    }

    PosAssignment assignmentRecord() {
        PosAssignment assignment = assignments.findByOrganizationIdAndCashierIdAndActiveTrue(
                actor.organizationId(), actor.employeeId())
                .orElseThrow(() -> new ConflictException("POS_ASSIGNMENT_MISSING", "Cashier has no active POS assignment"));
        if (assignment.getSalesHall().getType() != LocationType.SALES_HALL
                || assignment.getSalesHall().getStatus() != LocationStatus.ACTIVE
                || !assignment.getSalesHall().getOrganization().getId().equals(actor.organizationId()))
            throw new ConflictException("POS_HALL_UNAVAILABLE", "Assigned Sales Hall is unavailable");
        return assignment;
    }

    String currency() {
        String value = configurations.findByOrganizationId(actor.organizationId())
                .orElseThrow(() -> new ConflictException("POS_CURRENCY_MISSING", "Organization currency is not configured"))
                .getCurrency();
        if (value == null || value.isBlank())
            throw new ConflictException("POS_CURRENCY_MISSING", "Organization currency is not configured");
        return value;
    }

    CashBox ownCashBox() {
        return cashBoxes.findAllByOrganizationIdOrderByNameAsc(actor.organizationId()).stream()
                .filter(b -> b.getType() == CashBoxType.EMPLOYEE && b.getOwnerEmployee() != null
                        && b.getOwnerEmployee().getId().equals(actor.employeeId()))
                .findFirst().orElseThrow(() -> new ConflictException("POS_CASHBOX_MISSING", "Cashier cashbox is missing"));
    }

    @Transactional
    public SalesDtos.PosAssignmentResponse assign(UUID cashierId, SalesDtos.PosAssignmentRequest request) {
        actor.requirePermission(Permission.SALES_MANAGE);
        Employee employee = employees.findByIdAndOrganizationId(cashierId, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Cashier not found"));
        if (employee.getRole() != RoleType.CASHIER || employee.getStatus() != EmployeeStatus.ACTIVE)
            throw new ConflictException("POS_INVALID_CASHIER", "Employee must be an active cashier");
        var hall = sites.findByIdAndOrganizationId(request.salesHallId(), actor.organizationId())
                .filter(s -> s.getType() == LocationType.SALES_HALL && s.getStatus() == LocationStatus.ACTIVE)
                .orElseThrow(() -> new ConflictException("POS_HALL_UNAVAILABLE", "Sales Hall is unavailable"));
        PosAssignment assignment = assignments.findByOrganizationIdAndCashierId(actor.organizationId(), cashierId)
                .orElseGet(PosAssignment::new);
        assignment.setOrganization(employee.getOrganization()); assignment.setCashier(employee);
        assignment.setSalesHall(hall); assignment.setCode(request.code().trim()); assignment.setActive(request.active());
        assignment = assignments.save(assignment);
        return new SalesDtos.PosAssignmentResponse(assignment.getId(), assignment.getCode(), hall.getId(), cashierId, assignment.isActive());
    }

    @Transactional(readOnly = true)
    public SalesDtos.CashierAssignment assignment() {
        actor.requirePermission(Permission.SALE_VIEW);
        Employee cashier = cashier();
        PosAssignment pos = assignmentRecord();
        CashBox box = ownCashBox();
        String currency = currency();
        if (!currency.equalsIgnoreCase(box.getCurrency()))
            throw new ConflictException("POS_CASHBOX_CURRENCY_MISMATCH", "Cashbox currency differs from organization currency");
        return new SalesDtos.CashierAssignment(pos.getId(), pos.getCode(), pos.getSalesHall().getId(),
                pos.getSalesHall().getName(), cashier.getId(), cashier.getFirstName() + " " + cashier.getLastName(),
                new SalesDtos.CashierCashbox(box.getId(), box.getName()),
                box.getStatus() == CashBoxStatus.ACTIVE ? "OPEN" : "CLOSED", currency);
    }

    @Transactional(readOnly = true)
    public SalesDtos.CashierScanResponse scan(SalesDtos.CashierScanRequest request) {
        actor.requirePermission(Permission.PRODUCT_SCAN);
        PosAssignment pos = assignmentRecord();
        if (!pos.getSalesHall().getId().equals(request.salesHallId()))
            throw new ConflictException("POS_HALL_FORBIDDEN", "Sales Hall is not assigned to cashier");
        String code = request.code().trim();
        UUID productId;
        SalesPackage salesPackage = null;
        QrCode qr = qrCodes.findByOrganizationIdAndCodeIgnoreCase(actor.organizationId(), code).orElse(null);
        if (qr != null) {
            if (qr.getStatus() != QrCodeStatus.ACTIVE || qr.isExpired(Instant.now()))
                throw new ConflictException("POS_CODE_UNAVAILABLE", "Code is inactive or expired");
            if (qr.getType() == QrCodeType.SALES_PACKAGE) {
                salesPackage = packages.findByIdAndOrganizationId(qr.getReferenceId(), actor.organizationId())
                        .orElseThrow(() -> new NotFoundException("Sales package not found"));
                if (!salesPackage.getSalesHallId().equals(request.salesHallId()) || !"ACTIVE".equals(salesPackage.getStatus()))
                    throw new ConflictException("POS_PACKAGE_UNAVAILABLE", "Sales package is unavailable");
                productId = salesPackage.getProductId();
            } else if (qr.getType() == QrCodeType.INVENTORY_ITEM && qr.getTargetKind() == QrTargetKind.SERIALIZED_ITEM) {
                ReceivingOperationSerial serial = serials.findById(qr.getReferenceId())
                        .orElseThrow(() -> new NotFoundException("Serialized item not found"));
                if (!serial.getReceivingOperationItem().getReceivingOperation().getOrganization().getId()
                        .equals(actor.organizationId()))
                    throw new NotFoundException("Serialized item not found");
                if (!serial.isAccepted())
                    throw new ConflictException("POS_SERIAL_UNRECEIVED", "Serialized item was not accepted during receiving");
                throw new ConflictException("POS_SERIAL_LOCATION_UNTRACKED", "Serialized item location is not tracked through hall transfers");
            } else if (qr.getType() == QrCodeType.PRODUCT && qr.getTargetKind() == QrTargetKind.PRODUCT) {
                productId = qr.getReferenceId();
            } else if (qr.getType() == QrCodeType.PRODUCT && qr.getTargetKind() == QrTargetKind.ORGANIZATION_PRODUCT) {
                OrganizationProduct organizationProduct = organizationProducts.findById(qr.getReferenceId())
                        .orElseThrow(() -> new NotFoundException("Product not found"));
                if (!organizationProduct.getOrganization().getId().equals(actor.organizationId()))
                    throw new NotFoundException("Product not found");
                productId = organizationProduct.getProduct().getId();
            } else {
                throw new ConflictException("POS_UNSUPPORTED_CODE", "Code does not identify a saleable product or package");
            }
        } else {
            ProductBarcode barcode = barcodes.findByBarcode(BarcodeNormalizer.normalize(code))
                    .orElseThrow(() -> new NotFoundException("Product code not found"));
            if (barcode.getOrganization() != null && !barcode.getOrganization().getId().equals(actor.organizationId()))
                throw new NotFoundException("Product code not found");
            if (barcode.getOrganizationProduct() == null && barcode.getProduct() == null)
                throw new ConflictException("POS_INVALID_PRODUCT_ASSOCIATION", "Barcode has no product association");
            productId = barcode.getOrganizationProduct() != null ? barcode.getOrganizationProduct().getProduct().getId()
                    : barcode.getProduct().getId();
        }
        OrganizationProduct organizationProduct = organizationProducts.findByOrganizationIdAndProductId(actor.organizationId(), productId)
                .filter(p -> p.getStatus() == OrganizationProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        Product product = products.findById(productId).filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (product.getProductType() == ProductType.SERIALIZED)
            throw new ConflictException("POS_SERIAL_LOCATION_UNTRACKED", "Serialized item location is not tracked through hall transfers");
        SalesPrice price = salesPackage == null ? prices.findByOrganizationIdAndProductId(actor.organizationId(), productId)
                .orElseThrow(() -> new ConflictException("POS_PRICE_MISSING", "Selling price is missing")) : null;
        return new SalesDtos.CashierScanResponse(productId, organizationProduct.getOrganizationSku(), product.getName(),
                salesPackage == null ? product.getProductType().name() : "SALES_PACKAGE",
                product.getUnit() == null ? null : product.getUnit().getName(),
                salesPackage == null ? price.getCurrentPrice() : salesPackage.getUnitPrice(), currency(), null,
                salesPackage == null ? null : salesPackage.getId(),
                salesPackage == null ? null : salesPackage.getWeight(),
                salesPackage == null ? null : salesPackage.getTotalPrice());
    }
}
