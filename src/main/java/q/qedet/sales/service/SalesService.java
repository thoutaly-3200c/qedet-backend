package q.qedet.sales.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.*;
import q.qedet.organization.entity.*;
import q.qedet.organization.repository.*;
import q.qedet.product.entity.*;
import q.qedet.product.repository.*;
import q.qedet.qr.entity.QrCodeType;
import q.qedet.qr.service.QrCodeService;
import q.qedet.sales.dto.SalesDtos;
import q.qedet.sales.entity.*;
import q.qedet.sales.repository.*;
import q.qedet.warehouse.entity.*;
import q.qedet.warehouse.repository.*;

import java.math.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesService {
    private final ActorContext actor;
    private final OrganizationRepository organizations;
    private final OrganizationSiteRepository sites;
    private final OrganizationZoneRepository zones;
    private final ProductRepository products;
    private final OrganizationProductRepository orgProducts;
    private final InventoryBalanceRepository balances;
    private final StockMovementRepository movements;
    private final SalesStockRequestRepository requests;
    private final SalesPriceRepository prices;
    private final SalesPriceHistoryRepository histories;
    private final SalesPackageRepository packages;
    private final SalesReturnRepository returns;
    private final QrCodeService qrs;

    public SalesService(ActorContext actor,
                        OrganizationRepository organizations,
                        OrganizationSiteRepository sites,
                        OrganizationZoneRepository zones,
                        ProductRepository products,
                        OrganizationProductRepository orgProducts,
                        InventoryBalanceRepository balances,
                        StockMovementRepository movements,
                        SalesStockRequestRepository requests,
                        SalesPriceRepository prices,
                        SalesPriceHistoryRepository histories,
                        SalesPackageRepository packages,
                        SalesReturnRepository returns,
                        QrCodeService qrs) {
        this.actor = actor;
        this.organizations = organizations;
        this.sites = sites;
        this.zones = zones;
        this.products = products;
        this.orgProducts = orgProducts;
        this.balances = balances;
        this.movements = movements;
        this.requests = requests;
        this.prices = prices;
        this.histories = histories;
        this.packages = packages;
        this.returns = returns;
        this.qrs = qrs;
    }

    private UUID oid() {
        return actor.organizationId();
    }

    private BigDecimal n(BigDecimal x) {
        return x == null ? BigDecimal.ZERO : x;
    }

    private BigDecimal q(BigDecimal x) {
        return x.setScale(2, RoundingMode.HALF_UP);
    }

    private Organization org() {
        return organizations.findById(oid()).orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private void read() {
        actor.requirePermission(Permission.SALES_VIEW);
    }

    private void write() {
        actor.requirePermission(Permission.SALES_MANAGE);
    }

    private OrganizationSite site(UUID id, LocationType t) {
        return sites.findByIdAndOrganizationId(id, oid()).filter(s -> s.getType() == t && s.getStatus() == LocationStatus.ACTIVE).orElseThrow(() -> new ConflictException("SALES_INVALID_SITE", "Site must be active and have the required type"));
    }

    private OrganizationZone zone(UUID id, UUID site) {
        return zones.findByIdAndSiteId(id, site).filter(OrganizationZone::isActive).orElseThrow(() -> new ConflictException("SALES_INVALID_ZONE", "Zone must be active and belong to the site"));
    }

    private Product product(UUID id) {
        if (!orgProducts.existsByOrganizationIdAndProductId(oid(), id))
            throw new NotFoundException("Product not found");
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private String sku(UUID id) {
        return orgProducts.findByOrganizationIdAndProductId(oid(), id).map(OrganizationProduct::getOrganizationSku).orElse("");
    }

    @Transactional
    public Map<String, Object> createRequest(SalesDtos.StockRequestCreate x) {
        write();

        SalesStockRequest r = new SalesStockRequest();
        r.setOrganization(org());

        r.setNotes(x.notes());
        r.setCreatedBy(actor.employeeId());
        for (var y : x.items()) {
            product(y.productId());
            SalesStockRequestItem i = new SalesStockRequestItem();
            i.setRequest(r);
            i.setProductId(y.productId());
            i.setRequestedQuantity(q(y.quantity()));
            r.getItems().add(i);
        }
        return requestView(requests.save(r));
    }

    @Transactional
    public Map<String, Object> submitRequest(UUID id) {
        write();
        SalesStockRequest r = request(id);
        if (!"DRAFT".equals(r.getStatus())) throw new ConflictException("Only draft stock requests can be submitted");
        r.setStatus("SUBMITTED");
        return requestView(r);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRequests() {
        read();
        return requests.findAllByOrganizationIdOrderByCreatedAtDesc(oid()).stream().map(this::requestView).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRequest(UUID id) {
        read();
        return requestView(request(id));
    }

    private SalesStockRequest request(UUID id) {
        return requests.findByIdAndOrganizationId(id, oid()).orElseThrow(() -> new NotFoundException("Sales stock request not found"));
    }

    private Map<String, Object> requestView(SalesStockRequest r) {
        return Map.of("id", r.getId(), "status", r.getStatus(), "notes", Objects.toString(r.getNotes(), ""), "items", r.getItems().stream().map(i -> Map.of("id", i.getId(), "productId", i.getProductId(), "requestedQuantity", i.getRequestedQuantity(), "approvedQuantity", n(i.getApprovedQuantity()), "fulfilledQuantity", n(i.getFulfilledQuantity()), "pendingAcceptanceQuantity", n(i.getPendingAcceptanceQuantity()), "acceptedQuantity", n(i.getAcceptedQuantity()), "outstandingQuantity", i.getRequestedQuantity().subtract(n(i.getFulfilledQuantity())))).toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> availableWarehouseProducts(UUID id) {
        read();
        site(id, LocationType.WAREHOUSE);
        return balances.findAllByOrganizationIdOrderByZoneSiteNameAsc(oid()).stream().filter(b -> b.getZone().getSite().getId().equals(id)).collect(Collectors.groupingBy(InventoryBalance::getProductId, LinkedHashMap::new, Collectors.reducing(BigDecimal.ZERO, b -> n(b.getOnHand()).subtract(n(b.getReserved())), BigDecimal::add))).entrySet().stream().filter(e -> e.getValue().signum() > 0).map(e -> Map.<String, Object>of("productId", e.getKey(), "sku", sku(e.getKey()), "name", product(e.getKey()).getName(), "availableQuantity", e.getValue())).toList();
    }

    @Transactional
    public Map<String, Object> place(SalesDtos.PlaceStock x) {
        write();
        site(x.salesHallId(), LocationType.SALES_HALL);
        OrganizationZone to = zone(x.locationId(), x.salesHallId());
        OrganizationZone from = zones.findBySiteIdAndActiveTrueAndIsDefaultTrue(x.salesHallId()).orElseThrow(() -> new ConflictException("SALES_DEFAULT_ZONE_REQUIRED", "Sales Hall needs a default zone"));
        transfer(x.productId(), from, to, q(x.quantity()), "SALES_HALL_PLACEMENT", x.salesHallId().toString());
        return Map.of("productId", x.productId(), "salesHallId", x.salesHallId(), "zoneId", to.getId(), "quantity", q(x.quantity()));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> inventory(UUID h, String search) {
        read();
        site(h, LocationType.SALES_HALL);
        String needle = search == null ? "" : search.toLowerCase();
        return balances.findAllByOrganizationIdOrderByZoneSiteNameAsc(oid()).stream().filter(b -> b.getZone().getSite().getId().equals(h)).map(b -> {
            Product p = product(b.getProductId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productId", p.getId());
            m.put("sku", sku(p.getId()));
            m.put("name", p.getName());
            m.put("zoneId", b.getZone().getId());
            m.put("quantity", b.getOnHand());
            m.put("availableQuantity", n(b.getOnHand()).subtract(n(b.getReserved())));
            return m;
        }).filter(m -> needle.isBlank() || m.get("name").toString().toLowerCase().contains(needle) || m.get("sku").toString().toLowerCase().contains(needle)).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> halls() {
        read();
        return sites.findAllByOrganizationIdOrderByNameAsc(oid()).stream().filter(x -> x.getType() == LocationType.SALES_HALL).map(x -> Map.<String, Object>of("id", x.getId(), "code", x.getCode(), "name", x.getName(), "type", x.getType().name(), "status", x.getStatus().name())).toList();
    }

    @Transactional
    public Map<String, Object> updatePrice(SalesDtos.PriceUpdate x) {
        write();
        product(x.productId());

        BigDecimal newPrice = q(x.currentPrice());
        if (newPrice.signum() <= 0) {
            throw new ConflictException(
                    "SALES_INVALID_PRICE",
                    "Selling price must be greater than zero"
            );
        }

        SalesPrice p = prices
                .findByOrganizationIdAndProductId(oid(), x.productId())
                .orElseGet(() -> {
                    SalesPrice v = new SalesPrice();
                    v.setOrganization(org());
                    v.setProductId(x.productId());
                    return v;
                });

        BigDecimal previousPrice = p.getCurrentPrice();

        // Avoid creating misleading history entries for unchanged prices.
        if (previousPrice != null && previousPrice.compareTo(newPrice) == 0) {
            return priceView(p);
        }

        p.setCurrentPrice(newPrice);
        p.setUpdatedBy(actor.employeeId());
        p = prices.saveAndFlush(p);

        SalesPriceHistory history = new SalesPriceHistory();
        history.setSalesPriceId(p.getId());
        history.setProductId(p.getProductId());
        history.setPreviousPrice(previousPrice);
        history.setNewPrice(newPrice);
        history.setChangedBy(actor.employeeId());
        history.setReason(x.reason());
        history.setChangedAt(java.time.Instant.now());

        histories.save(history);

        return priceView(p);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPrices() {
        read();

        return prices.findAllByOrganizationId(oid())
                .stream()
                .map(this::priceView)
                .toList();
    }

        @Transactional(readOnly = true)
    public List<Map<String, Object>> priceHistory(UUID id) {
        read();

        // Scope the price lookup to the current organization first.
        SalesPrice p = prices.findByIdAndOrganizationId(id, oid())
                .orElseThrow(() -> new NotFoundException("Sales price not found"));

        return histories
                .findAllBySalesPriceIdOrderByChangedAtDesc(p.getId())
                .stream()
                .map(x -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", x.getId());
                    m.put("productId", x.getProductId());
                    m.put("previousPrice", x.getPreviousPrice());
                    m.put("newPrice", x.getNewPrice());
                    m.put("reason", x.getReason());
                    m.put("changedBy", x.getChangedBy());
                    m.put("changedAt", x.getChangedAt());
                    return m;
                })
                .toList();
    }

    private Map<String, Object> priceView(SalesPrice p) {
        Product product = product(p.getProductId());
        ProductUnit unit = product.getUnit();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("productId", p.getProductId());
        m.put("currentPrice", p.getCurrentPrice());

        m.put("unitId", unit != null ? unit.getId() : null);
        m.put("unit", unit != null ? unit.getName() : null);
        m.put("unitSymbol", unit != null ? unit.getSymbol() : null);

        m.put("updatedBy", p.getUpdatedBy());
        m.put("updatedAt", p.getUpdatedAt());

        return m;
    }

    @Transactional
    public Map<String, Object> createPackage(SalesDtos.PackageCreate x) {
        write();
        site(x.salesHallId(), LocationType.SALES_HALL);
        product(x.productId());
        SalesPackage p = new SalesPackage();
        p.setOrganization(org());
        p.setSalesHallId(x.salesHallId());
        p.setProductId(x.productId());
        p.setWeight(q(x.weight()));
        p.setUnitPrice(q(x.unitPrice()));
        p.setTotalPrice(p.getWeight().multiply(p.getUnitPrice()));
        p.setCreatedBy(actor.employeeId());
        p = packages.save(p);
        p.setQrReference(qrs.createQrCode(QrCodeType.SALES_PACKAGE, oid(), p.getId(), null).publicIdentifier());
        return packageView(packages.save(p));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPackages(UUID h) {
        read();
        return packages.findAllByOrganizationIdAndSalesHallIdOrderByCreatedAtDesc(oid(), h).stream().map(this::packageView).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPackage(UUID id) {
        read();
        return packageView(packages.findByIdAndOrganizationId(id, oid()).orElseThrow(() -> new NotFoundException("Sales package not found")));
    }

    private Map<String, Object> packageView(SalesPackage x) {
        return Map.of("id", x.getId(), "salesHallId", x.getSalesHallId(), "productId", x.getProductId(), "weight", x.getWeight(), "unitPrice", x.getUnitPrice(), "totalPrice", x.getTotalPrice(), "status", x.getStatus());
    }

    @Transactional
    public Map<String, Object> createReturn(SalesDtos.ReturnCreate x) {
        write();
        site(x.salesHallId(), LocationType.SALES_HALL);
        site(x.destinationWarehouseId(), LocationType.WAREHOUSE);
        SalesReturn r = new SalesReturn();
        r.setOrganization(org());
        r.setSalesHallId(x.salesHallId());
        r.setDestinationWarehouseId(x.destinationWarehouseId());
        r.setReason(x.reason());
        r.setNotes(x.notes());
        r.setCreatedBy(actor.employeeId());
        for (var v : x.items()) {
            SalesReturnItem i = new SalesReturnItem();
            i.setSalesReturn(r);
            i.setProductId(v.productId());
            i.setRequestedQuantity(q(v.quantity()));
            r.getItems().add(i);
        }
        return returnView(returns.save(r));
    }

    @Transactional
    public Map<String, Object> submitReturn(UUID id) {
        write();
        SalesReturn r = salesReturn(id);
        if (!"DRAFT".equals(r.getStatus())) throw new ConflictException("Only draft returns can be submitted");
        r.setStatus("SUBMITTED");
        return returnView(r);
    }

    @Transactional
    public Map<String, Object> receiveReturn(UUID id) {
        throw new ConflictException("SALES_RETURN_RECEIPT_REQUIRES_ZONE_WORKFLOW", "Use Warehouse Returns to record physical receipt into a Warehouse zone");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listReturns() {
        read();
        return returns.findAllByOrganizationIdOrderByCreatedAtDesc(oid()).stream().map(this::returnView).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReturn(UUID id) {
        read();
        return returnView(salesReturn(id));
    }

    private SalesReturn salesReturn(UUID id) {
        return returns.findByIdAndOrganizationId(id, oid()).orElseThrow(() -> new NotFoundException("Sales return not found"));
    }

    private Map<String, Object> returnView(SalesReturn x) {
        return Map.of("id", x.getId(), "salesHallId", x.getSalesHallId(), "destinationWarehouseId", x.getDestinationWarehouseId(), "status", x.getStatus(), "reason", Objects.toString(x.getReason(), ""));
    }

    private void transfer(UUID product, OrganizationZone from, OrganizationZone to, BigDecimal quantity, String type, String ref) {
        InventoryBalance a = balances.findForUpdate(oid(), product, from.getId()).orElseThrow(() -> new ConflictException("SALES_INSUFFICIENT_STOCK", "No stock in source zone"));
        if (n(a.getOnHand()).subtract(n(a.getReserved())).compareTo(quantity) < 0)
            throw new ConflictException("SALES_INSUFFICIENT_STOCK", "Insufficient available stock");
        a.setOnHand(n(a.getOnHand()).subtract(quantity));
        balances.save(a);
        InventoryBalance b = balances.findForUpdate(oid(), product, to.getId()).orElseGet(() -> {
            InventoryBalance v = new InventoryBalance();
            v.setOrganization(org());
            v.setProductId(product);
            v.setZone(to);
            return v;
        });
        b.setOnHand(n(b.getOnHand()).add(quantity));
        balances.save(b);
        StockMovement m = new StockMovement();
        m.setOrganization(org());
        m.setProductId(product);
        m.setFromZone(from);
        m.setToZone(to);
        m.setQuantity(quantity);
        m.setMovementType(type);
        m.setReference(ref);
        movements.save(m);
    }
}
