package q.qedet.sales.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.finance.entity.*;
import q.qedet.finance.model.*;
import q.qedet.finance.repository.*;
import q.qedet.organization.entity.*;
import q.qedet.organization.repository.*;
import q.qedet.organization.service.OrganizationConfigurationService;
import q.qedet.product.entity.*;
import q.qedet.product.repository.*;
import q.qedet.sales.dto.SalesDtos;
import q.qedet.sales.entity.*;
import q.qedet.sales.repository.*;
import q.qedet.warehouse.entity.*;
import q.qedet.warehouse.repository.*;

import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

@Service
public class PosCheckoutService {
    private final ActorContext actor;
    private final CashierService cashier;
    private final OrganizationRepository organizations;
    private final OrganizationConfigurationRepository configurations;
    private final OrganizationConfigurationService numbering;
    private final EmployeeRepository employees;
    private final ProductRepository products;
    private final OrganizationProductRepository organizationProducts;
    private final SalesPriceRepository prices;
    private final SalesPackageRepository packages;
    private final InventoryBalanceRepository balances;
    private final StockMovementRepository movements;
    private final PosQuoteRepository quotes;
    private final PosSaleRepository sales;
    private final CashBoxRepository cashBoxes;
    private final CashTransactionRepository cashTransactions;
    private final SecureRandom random = new SecureRandom();

    public PosCheckoutService(ActorContext actor, CashierService cashier, OrganizationRepository organizations,
                              OrganizationConfigurationRepository configurations,
                              OrganizationConfigurationService numbering,
                              EmployeeRepository employees, ProductRepository products,
                              OrganizationProductRepository organizationProducts, SalesPriceRepository prices,
                              SalesPackageRepository packages,
                              InventoryBalanceRepository balances, StockMovementRepository movements,
                              PosQuoteRepository quotes, PosSaleRepository sales, CashBoxRepository cashBoxes,
                              CashTransactionRepository cashTransactions) {
        this.actor = actor; this.cashier = cashier; this.organizations = organizations;
        this.configurations = configurations; this.numbering = numbering;
        this.employees = employees; this.products = products; this.organizationProducts = organizationProducts;
        this.prices = prices; this.packages = packages;
        this.balances = balances; this.movements = movements; this.quotes = quotes; this.sales = sales;
        this.cashBoxes = cashBoxes; this.cashTransactions = cashTransactions;
    }

    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal zero() { return BigDecimal.ZERO.setScale(2); }
    private static BigDecimal available(InventoryBalance b) { return b.getOnHand().subtract(b.getReserved()); }
    private Organization org() { return organizations.findById(actor.organizationId()).orElseThrow(() -> new NotFoundException("Organization not found")); }
    private Employee employee() { return employees.findByIdAndOrganizationId(actor.employeeId(), actor.organizationId()).orElseThrow(() -> new NotFoundException("Cashier not found")); }
    private OrganizationConfiguration config() { return configurations.findByOrganizationId(actor.organizationId()).orElseThrow(() -> new ConflictException("POS_CONFIG_MISSING", "Organization sales configuration is missing")); }
    private void hall(UUID id) { if (!cashier.assignmentRecord().getSalesHall().getId().equals(id)) throw new ConflictException("POS_HALL_FORBIDDEN", "Sales Hall is not assigned to cashier"); }

    private static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private String newToken() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record Calculated(List<PosQuoteLine> lines, BigDecimal subtotal, BigDecimal taxTotal, BigDecimal total) {}

    private Calculated calculate(UUID hallId, List<SalesDtos.PosQuoteRequestLine> requested,
                                 OrganizationConfiguration configuration, boolean lock) {
        if (requested == null || requested.isEmpty() || requested.size() > 100)
            throw new BadRequestException("Cart must contain 1 to 100 lines");
        BigDecimal rate = configuration.getSalesTaxRate();
        if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0)
            throw new ConflictException("POS_TAX_CONFIG_INVALID", "Sales tax configuration is invalid");
        Set<UUID> packageIds = new HashSet<>();
        List<PosQuoteLine> lines = new ArrayList<>();
        Map<UUID, BigDecimal> quantities = new TreeMap<>();
        BigDecimal subtotal = zero(), taxTotal = zero();
        for (SalesDtos.PosQuoteRequestLine item : requested) {
            if (item == null) throw new BadRequestException("Cart line is required");
            if ((item.packageId() == null) == (item.productId() == null))
                throw new BadRequestException("Line must contain either productId or packageId");
            SalesPackage pack = null;
            UUID productId = item.productId();
            BigDecimal quantity = item.quantity();
            if (item.packageId() != null) {
                if (quantity != null || item.serialNumber() != null || !packageIds.add(item.packageId()))
                    throw new BadRequestException("Package must appear once without quantity or serial");
                pack = lock ? packages.findLockedByIdAndOrganizationId(item.packageId(), actor.organizationId()).orElse(null)
                        : packages.findByIdAndOrganizationId(item.packageId(), actor.organizationId()).orElse(null);
                if (pack == null || !pack.getSalesHallId().equals(hallId) || !"ACTIVE".equals(pack.getStatus()))
                    throw new ConflictException("POS_PACKAGE_UNAVAILABLE", "Sales package is unavailable");
                productId = pack.getProductId(); quantity = pack.getWeight();
            }
            if (quantity == null || quantity.signum() <= 0 || quantity.stripTrailingZeros().scale() > 2
                    || quantity.compareTo(new BigDecimal("999999999999.99")) > 0)
                throw new BadRequestException("Invalid quantity");
            OrganizationProduct op = organizationProducts.findByOrganizationIdAndProductId(actor.organizationId(), productId)
                    .filter(p -> p.getStatus() == OrganizationProductStatus.ACTIVE)
                    .orElseThrow(() -> new NotFoundException("Product not found"));
            Product product = products.findById(productId).filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                    .orElseThrow(() -> new NotFoundException("Product not found"));
            if (pack == null && product.getProductType() != ProductType.WEIGHTED
                    && quantity.stripTrailingZeros().scale() > 0)
                throw new BadRequestException("Product requires a whole-number quantity");
            if (product.getProductType() == ProductType.SERIALIZED) {
                // Receiving records identify the unit, but transfers do not track its current zone.
                // An organization-wide serial match cannot establish that the unit is in this hall.
                throw new ConflictException("POS_SERIAL_LOCATION_UNTRACKED", "Serialized item location is not tracked through hall transfers");
            }
            if (item.serialNumber() != null) throw new BadRequestException("Serial only applies to serialized products");
            BigDecimal unitPrice = pack == null ? prices.findByOrganizationIdAndProductId(actor.organizationId(), productId)
                    .map(SalesPrice::getCurrentPrice).orElseThrow(() -> new ConflictException("POS_PRICE_MISSING", "Selling price is missing"))
                    : pack.getUnitPrice();
            if (unitPrice == null || unitPrice.signum() <= 0) throw new ConflictException("POS_PRICE_INVALID", "Selling price is invalid");
            BigDecimal gross = pack == null ? money(unitPrice.multiply(quantity)) : pack.getTotalPrice();
            BigDecimal tax = configuration.isSalesPricesTaxInclusive()
                    ? money(gross.multiply(rate).divide(new BigDecimal("100").add(rate), 8, RoundingMode.HALF_UP))
                    : money(gross.multiply(rate).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
            BigDecimal net = configuration.isSalesPricesTaxInclusive() ? gross.subtract(tax) : gross;
            PosQuoteLine line = new PosQuoteLine();
            line.setProductId(productId); line.setPackageId(pack == null ? null : pack.getId());
            line.setSerialNumber(null);
            line.setProductName(product.getName()); line.setSku(op.getOrganizationSku()); line.setQuantity(quantity);
            line.setUnitPrice(unitPrice); line.setLineTotal(net); line.setTaxAmount(tax); lines.add(line);
            quantities.merge(productId, quantity, BigDecimal::add);
            subtotal = subtotal.add(net); taxTotal = taxTotal.add(tax);
        }
        for (var entry : quantities.entrySet()) {
            List<InventoryBalance> stock = lock ? balances.findAllForUpdateByOrganizationIdAndProductId(actor.organizationId(), entry.getKey())
                    : balances.findAllByOrganizationIdAndProductId(actor.organizationId(), entry.getKey());
            BigDecimal inHall = stock.stream().filter(b -> b.getZone().getSite().getId().equals(hallId))
                    .map(PosCheckoutService::available).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (inHall.compareTo(entry.getValue()) < 0)
                throw new ConflictException("POS_INSUFFICIENT_STOCK", "Insufficient Sales Hall inventory");
        }
        return new Calculated(lines, subtotal, taxTotal, subtotal.add(taxTotal));
    }

    private static SalesDtos.PosQuoteLine view(PosQuoteLine line) {
        return new SalesDtos.PosQuoteLine(line.getPackageId() == null ? line.getProductId().toString() : line.getPackageId().toString(),
                line.getProductId(), line.getPackageId(), line.getProductName(), line.getSku(), line.getQuantity(),
                line.getUnitPrice(), line.getLineTotal(), line.getTaxAmount(), line.getSerialNumber());
    }

    @Transactional
    public SalesDtos.PosQuoteResponse quote(SalesDtos.PosQuoteRequest request) {
        actor.requirePermission(Permission.SALE_CREATE);
        hall(request.salesHallId());
        OrganizationConfiguration c = config();
        Calculated calculated = calculate(request.salesHallId(), request.lines(), c, false);
        String token = newToken();
        PosQuote quote = new PosQuote();
        quote.setOrganization(org()); quote.setCashier(employee()); quote.setSalesHall(cashier.assignmentRecord().getSalesHall());
        quote.setTokenHash(hash(token)); quote.setExpiresAt(Instant.now().plusSeconds(300)); quote.setCurrency(cashier.currency());
        quote.setTaxRate(c.getSalesTaxRate()); quote.setPricesTaxInclusive(c.isSalesPricesTaxInclusive());
        quote.setSubtotal(calculated.subtotal()); quote.setDiscountTotal(zero()); quote.setTaxTotal(calculated.taxTotal());
        quote.setTotal(calculated.total());
        for (int i = 0; i < calculated.lines().size(); i++) {
            PosQuoteLine line = calculated.lines().get(i);
            line.setSortOrder(i); line.setQuote(quote); quote.getLines().add(line);
        }
        quotes.save(quote);
        return new SalesDtos.PosQuoteResponse(quote.getCurrency(), quote.getLines().stream().map(PosCheckoutService::view).toList(),
                quote.getSubtotal(), quote.getDiscountTotal(), quote.getTaxTotal(), quote.getTotal(), token, quote.getExpiresAt());
    }

    private String requestHash(SalesDtos.PosSaleRequest request) {
        SalesDtos.PosPaymentRequest p = request.payment();
        return hash(request.quoteToken() + "|" + p.method() + "|" + p.cashboxId() + "|" + p.amountReceived().stripTrailingZeros().toPlainString()
                + "|" + Objects.toString(p.reference(), ""));
    }

    private SalesDtos.PosSaleResponse saleView(PosSale sale) {
        PosSalePayment p = sale.getPayment();
        return new SalesDtos.PosSaleResponse(sale.getId(), sale.getTransactionCode(), "COMPLETED", "PAID", sale.getCurrency(),
                sale.getSubtotal(), sale.getDiscountTotal(), sale.getTaxTotal(), sale.getTotal(), p.getAmountReceived(),
                p.getChangeAmount(), sale.getCompletedAt(), "/api/v1/sales/cashier/sales/" + sale.getId() + "/receipt");
    }

    @Transactional
    public SalesDtos.PosSaleResponse sale(String key, SalesDtos.PosSaleRequest request) {
        actor.requirePermission(Permission.SALE_CREATE);
        actor.requirePermission(Permission.PAYMENT_RECEIVE);
        if (key == null || key.isBlank() || key.length() > 128) throw new BadRequestException("Valid Idempotency-Key is required");
        String fingerprint = requestHash(request);
        // Serializes same-organization checkouts, including duplicate idempotency keys and sequence creation.
        Organization organization = organizations.findByIdForUpdate(actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        PosSale existing = sales.findByOrganizationIdAndCashierIdAndIdempotencyKey(actor.organizationId(), actor.employeeId(), key).orElse(null);
        if (existing != null) {
            if (!existing.getRequestHash().equals(fingerprint)) throw new ConflictException("POS_IDEMPOTENCY_CONFLICT", "Idempotency key was used for a different request");
            return saleView(existing);
        }
        if (!"CASH".equals(request.payment().method()))
            throw new ConflictException("POS_PAYMENT_UNSUPPORTED", "Card and online payments require a confirmed payment integration");
        PosQuote quote = quotes.findByOrganizationIdAndTokenHash(actor.organizationId(), hash(request.quoteToken()))
                .orElseThrow(() -> new ConflictException("POS_QUOTE_INVALID", "Quote is invalid"));
        if (!quote.getCashier().getId().equals(actor.employeeId()) || quote.getExpiresAt().isBefore(Instant.now()) || quote.getSale() != null)
            throw new ConflictException("POS_QUOTE_INVALID", "Quote is expired, used, or belongs to another cashier");
        hall(quote.getSalesHall().getId());
        OrganizationConfiguration c = config();
        if (!quote.getCurrency().equals(cashier.currency()) || c.getSalesTaxRate().compareTo(quote.getTaxRate()) != 0
                || c.isSalesPricesTaxInclusive() != quote.isPricesTaxInclusive())
            throw new ConflictException("POS_QUOTE_STALE", "Sales configuration changed; request a new quote");
        List<SalesDtos.PosQuoteRequestLine> cart = quote.getLines().stream().map(line ->
                new SalesDtos.PosQuoteRequestLine(line.getPackageId() == null ? line.getProductId() : null,
                        line.getPackageId() == null ? line.getQuantity() : null, line.getSerialNumber(), line.getPackageId())).toList();
        Calculated current = calculate(quote.getSalesHall().getId(), cart, c, true);
        if (current.lines().size() != quote.getLines().size() || current.subtotal().compareTo(quote.getSubtotal()) != 0
                || current.taxTotal().compareTo(quote.getTaxTotal()) != 0 || current.total().compareTo(quote.getTotal()) != 0)
            throw new ConflictException("POS_QUOTE_STALE", "Price or total changed; request a new quote");
        for (int i = 0; i < current.lines().size(); i++) {
            PosQuoteLine a = current.lines().get(i), b = quote.getLines().get(i);
            if (!a.getProductId().equals(b.getProductId()) || !Objects.equals(a.getPackageId(), b.getPackageId())
                    || a.getQuantity().compareTo(b.getQuantity()) != 0
                    || a.getUnitPrice().compareTo(b.getUnitPrice()) != 0 || a.getLineTotal().compareTo(b.getLineTotal()) != 0)
                throw new ConflictException("POS_QUOTE_STALE", "Quote contents changed; request a new quote");
        }
        BigDecimal received = money(request.payment().amountReceived());
        if (received.compareTo(quote.getTotal()) < 0) throw new ConflictException("POS_INSUFFICIENT_CASH", "Amount received is below sale total");
        CashBox box = cashBoxes.findLockedByIdAndOrganizationId(request.payment().cashboxId(), actor.organizationId())
                .orElseThrow(() -> new ConflictException("POS_CASHBOX_INVALID", "Cashbox is unavailable"));
        if (box.getType() != CashBoxType.EMPLOYEE || box.getOwnerEmployee() == null
                || !box.getOwnerEmployee().getId().equals(actor.employeeId()) || box.getStatus() != CashBoxStatus.ACTIVE
                || !box.getCurrency().equalsIgnoreCase(quote.getCurrency()))
            throw new ConflictException("POS_CASHBOX_INVALID", "Cashbox ownership, status, or currency is invalid");
        PosSale sale = new PosSale();
        sale.setOrganization(organization); sale.setCashier(quote.getCashier()); sale.setSalesHall(quote.getSalesHall()); sale.setQuote(quote);
        sale.setIdempotencyKey(key); sale.setRequestHash(fingerprint);
        sale.setTransactionCode("SALE-" + numbering.nextNumber(SequenceType.INVOICE)); sale.setCurrency(quote.getCurrency());
        sale.setSubtotal(quote.getSubtotal()); sale.setDiscountTotal(quote.getDiscountTotal()); sale.setTaxTotal(quote.getTaxTotal());
        sale.setTotal(quote.getTotal()); sale.setCompletedAt(Instant.now());
        for (PosQuoteLine line : quote.getLines()) {
            PosSaleLine copy = new PosSaleLine(); copy.setSale(sale); copy.setProductId(line.getProductId()); copy.setPackageId(line.getPackageId());
            copy.setSerialNumber(line.getSerialNumber()); copy.setProductName(line.getProductName()); copy.setSku(line.getSku());
            copy.setQuantity(line.getQuantity()); copy.setUnitPrice(line.getUnitPrice()); copy.setLineTotal(line.getLineTotal());
            copy.setTaxAmount(line.getTaxAmount()); sale.getLines().add(copy);
        }
        PosSalePayment payment = new PosSalePayment(); payment.setSale(sale); payment.setCashBox(box); payment.setMethod("CASH");
        payment.setAmountReceived(received); payment.setChangeAmount(received.subtract(quote.getTotal()));
        payment.setReference(request.payment().reference()); sale.setPayment(payment);
        sale = sales.saveAndFlush(sale);
        Map<UUID, BigDecimal> quantities = new TreeMap<>();
        for (PosQuoteLine line : quote.getLines()) {
            quantities.merge(line.getProductId(), line.getQuantity(), BigDecimal::add);
            if (line.getPackageId() != null) {
                SalesPackage pack = packages.findLockedByIdAndOrganizationId(line.getPackageId(), actor.organizationId()).orElseThrow();
                if (!"ACTIVE".equals(pack.getStatus())) throw new ConflictException("POS_PACKAGE_UNAVAILABLE", "Package is unavailable");
                pack.setStatus("SOLD");
            }
        }
        for (var entry : quantities.entrySet()) {
            BigDecimal remaining = entry.getValue();
            for (InventoryBalance balance : balances.findAllForUpdateByOrganizationIdAndProductId(actor.organizationId(), entry.getKey())) {
                if (!balance.getZone().getSite().getId().equals(quote.getSalesHall().getId()) || remaining.signum() == 0) continue;
                BigDecimal take = available(balance).min(remaining);
                if (take.signum() <= 0) continue;
                balance.setOnHand(balance.getOnHand().subtract(take));
                StockMovement movement = new StockMovement(); movement.setOrganization(organization); movement.setProductId(entry.getKey());
                movement.setFromZone(balance.getZone()); movement.setQuantity(take); movement.setMovementType("POS_SALE");
                movement.setReference(sale.getId().toString()); movements.save(movement);
                remaining = remaining.subtract(take);
            }
            if (remaining.signum() > 0) throw new ConflictException("POS_INSUFFICIENT_STOCK", "Insufficient Sales Hall inventory");
        }
        BigDecimal before = cashTransactions.findAllByCashBoxIdAndOrganizationIdOrderByPostedAtDesc(box.getId(), actor.organizationId())
                .stream().map(t -> switch (t.getType()) {
                    case CASH_IN, TRANSFER_IN, ADJUSTMENT_IN -> t.getAmount();
                    case CASH_OUT, TRANSFER_OUT, ADJUSTMENT_OUT -> t.getAmount().negate();
                }).reduce(zero(), BigDecimal::add);
        postCash(sale, box, CashTransactionType.CASH_IN, received, before, "POS cash received");
        if (payment.getChangeAmount().signum() > 0)
            postCash(sale, box, CashTransactionType.CASH_OUT, payment.getChangeAmount(), before.add(received), "POS change paid");
        quote.setSale(sale);
        return saleView(sale);
    }

    private void postCash(PosSale sale, CashBox box, CashTransactionType type, BigDecimal amount, BigDecimal before, String description) {
        CashTransaction transaction = new CashTransaction(); transaction.setOrganization(sale.getOrganization()); transaction.setCashBox(box);
        transaction.setType(type); transaction.setAmount(amount); transaction.setCurrency(sale.getCurrency());
        transaction.setSourceType("POS_SALE"); transaction.setSourceId(sale.getId()); transaction.setReference(sale.getTransactionCode());
        transaction.setDescription(description); transaction.setCreatedBy(sale.getCashier()); transaction.setPostedAt(sale.getCompletedAt());
        transaction.setCompletedAt(sale.getCompletedAt()); transaction.setBalanceAfter(type == CashTransactionType.CASH_IN ? before.add(amount) : before.subtract(amount));
        cashTransactions.save(transaction);
    }

    @Transactional(readOnly = true)
    public SalesDtos.PosReceiptResponse receipt(UUID saleId) {
        if (!actor.permissions().contains(Permission.SALE_VIEW) && !actor.permissions().contains(Permission.SALES_VIEW))
            throw new ForbiddenException("Missing sales view permission");
        PosSale sale = sales.findByIdAndOrganizationId(saleId, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Sale not found"));
        if (!sale.getCashier().getId().equals(actor.employeeId()) && !actor.permissions().contains(Permission.SALES_VIEW))
            throw new NotFoundException("Sale not found");
        List<SalesDtos.PosQuoteLine> items = sale.getLines().stream().map(line -> new SalesDtos.PosQuoteLine(
                line.getPackageId() == null ? line.getProductId().toString() : line.getPackageId().toString(),
                line.getProductId(), line.getPackageId(), line.getProductName(), line.getSku(), line.getQuantity(),
                line.getUnitPrice(), line.getLineTotal(), line.getTaxAmount(), line.getSerialNumber())).toList();
        return new SalesDtos.PosReceiptResponse(sale.getId(), sale.getTransactionCode(), sale.getCompletedAt(),
                sale.getSalesHall().getId(), sale.getSalesHall().getName(), sale.getCashier().getId(),
                sale.getCashier().getFirstName() + " " + sale.getCashier().getLastName(), sale.getCurrency(), items,
                sale.getSubtotal(), sale.getDiscountTotal(), sale.getTaxTotal(), sale.getTotal(), sale.getPayment().getMethod(),
                sale.getPayment().getAmountReceived(), sale.getPayment().getChangeAmount());
    }
}
