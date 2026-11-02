package q.qedet.qr.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.auth.security.AuthenticatedPrincipal;
import q.qedet.common.audit.AuditService;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.hr.common.event.DomainEvent;
import q.qedet.hr.common.event.DomainEventPublisher;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.product.entity.Product;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.product.repository.ProductRepository;
import q.qedet.qr.dto.QrCodeRequest;
import q.qedet.qr.dto.QrCodeResponse;
import q.qedet.qr.dto.QrProductResponse;
import q.qedet.qr.entity.*;
import q.qedet.qr.repository.QrCodeRepository;
import q.qedet.qr.repository.QrCodeScanRepository;
import q.qedet.warehouse.entity.ReceivingOperationSerial;
import java.util.Optional;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class QrCodeService {
    private static final String URL_PREFIX = "https://qedet.net/q/";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    private final QrCodeRepository repository;
    private final QrCodeCreationPersistence creationPersistence;
    private final OrganizationRepository organizations;
    private final ProductRepository products;
    private final OrganizationProductRepository organizationProducts;
    private final ActorContext actor;
    private final AuditService auditService;
    private final QrCodeScanRepository scans;
    private final DomainEventPublisher events;

    public QrCodeService(QrCodeRepository repository, QrCodeCreationPersistence creationPersistence,
                         OrganizationRepository organizations, ProductRepository products,
                         OrganizationProductRepository organizationProducts,
                         ActorContext actor, AuditService auditService,
                         QrCodeScanRepository scans, DomainEventPublisher events) {
        this.repository = repository;
        this.creationPersistence = creationPersistence;
        this.organizations = organizations;
        this.products = products;
        this.organizationProducts = organizationProducts;
        this.actor = actor;
        this.auditService = auditService;
        this.scans = scans;
        this.events = events;
    }

    @Transactional
    public QrCodeResponse create(QrCodeRequest request) {
        UUID organizationId = actor.organizationId();
        actor.requirePermission(permissionFor(request.type()));
        return createQrCode(request.type(), organizationId, request.targetId(),
                request.targetKind(), request.expiresAt());
    }

    @Transactional
    public QrCodeResponse createQrCode(QrCodeType type, UUID organizationId, UUID referenceId, Instant expiresAt) {
        return createQrCode(type, organizationId, referenceId, defaultTargetKind(type), expiresAt);
    }

    /**
     * The supplied identifier is stored verbatim. It is intended for supplier
     * payloads, whereas generated identifiers retain the existing Qedet format.
     */
    @Transactional
    public QrCodeResponse resolveOrCreate(QrCodeType type, UUID organizationId, UUID referenceId,
                                         QrTargetKind targetKind, String supplierIdentifier) {
        return createQrCode(type, organizationId, referenceId, targetKind, null, supplierIdentifier);
    }

    @Transactional
    public QrCodeResponse resolveOrCreateReceivingSerial(UUID organizationId, ReceivingOperationSerial serial) {
        return resolveOrCreate(QrCodeType.INVENTORY_ITEM, organizationId, serial.getId(),
                QrTargetKind.SERIALIZED_ITEM, serial.getSupplierQrIdentifier());
    }

    /**
     * Validates that a supplier identifier may be bound to the given target within
     * the organization. Unknown identifiers are accepted; identifiers already bound
     * to the same target are accepted idempotently; other bindings conflict.
     */
    @Transactional(readOnly = true)
    public void assertAssignable(QrCodeType type, UUID organizationId, UUID referenceId,
                                 QrTargetKind targetKind, String supplierIdentifier) {
        if (supplierIdentifier == null || supplierIdentifier.isBlank()) {
            return;
        }
        String identifier = validateSupplierIdentifier(supplierIdentifier);
        ensureOrganizationAccess(organizationId);
        QrTargetKind resolvedTargetKind = targetKind == null ? defaultTargetKind(type) : targetKind;
        validateTargetKind(type, resolvedTargetKind);

        QrCode existingIdentifier = findInOrganization(organizationId, identifier).orElse(null);
        if (existingIdentifier != null && !matchesTarget(existingIdentifier, type, resolvedTargetKind, referenceId)) {
            throw new ConflictException("QR_IDENTIFIER_ALREADY_ASSIGNED",
                    "QR identifier is already assigned to a different target in this organization");
        }
    }

    /**
     * For new serialized units whose reference id is not known until persist time.
     * Rejects identifiers that already exist in the organization.
     */
    @Transactional(readOnly = true)
    public void assertIdentifierUnassigned(UUID organizationId, String supplierIdentifier) {
        if (supplierIdentifier == null || supplierIdentifier.isBlank()) {
            return;
        }
        String identifier = validateSupplierIdentifier(supplierIdentifier);
        ensureOrganizationAccess(organizationId);
        if (findInOrganization(organizationId, identifier).isPresent()) {
            throw new ConflictException("QR_IDENTIFIER_ALREADY_ASSIGNED",
                    "QR identifier is already assigned to a different target in this organization");
        }
    }

    /**
     * Receiving-specific lookup: resolves a known org-scoped QR without treating an
     * unknown supplier payload as proof that a product exists.
     */
    @Transactional(readOnly = true)
    public QrCodeResponse resolveInOrganization(UUID organizationId, String identifier) {
        ensureOrganizationAccess(organizationId);
        if (identifier == null || identifier.isBlank()) {
            throw new BadRequestException("QR_INVALID_SUPPLIER_IDENTIFIER", "QR identifier is required");
        }
        QrCode qr = findInOrganization(organizationId, identifier.trim())
                .orElseThrow(() -> new NotFoundException("QR_NOT_FOUND", "QR code not found"));
        QrScanResult result = validationResult(qr);
        if (result != null) {
            throw new ForbiddenException("QR_" + result.name(), "QR code is not resolvable");
        }
        if (!qr.getOrganization().getId().equals(organizationId)) {
            throw new ForbiddenException("UNAUTHORIZED_QR_ACCESS");
        }
        return response(qr);
    }
    @Transactional(readOnly = true)
    public boolean existsInOrganization(UUID organizationId, String identifier) {
        ensureOrganizationAccess(organizationId);

        if (identifier == null || identifier.isBlank()) {
            throw new BadRequestException(
                    "QR_INVALID_SUPPLIER_IDENTIFIER",
                    "QR identifier is required"
            );
        }

        return repository.existsByOrganizationIdAndCodeIgnoreCase(
                organizationId,
                identifier.trim()
        );
    }

    private QrCodeResponse createQrCode(QrCodeType type, UUID organizationId, UUID referenceId,
                                       QrTargetKind targetKind, Instant expiresAt) {
        return createQrCode(type, organizationId, referenceId, targetKind, expiresAt, null);
    }

    private QrCodeResponse createQrCode(QrCodeType type, UUID organizationId, UUID referenceId,
                                       QrTargetKind targetKind, Instant expiresAt, String supplierIdentifier) {
        if (type == null || organizationId == null || referenceId == null) {
            throw new BadRequestException("QR_INVALID_REQUEST", "QR type, organization and reference are required");
        }
        QrTargetKind resolvedTargetKind = targetKind == null ? defaultTargetKind(type) : targetKind;
        validateTargetKind(type, resolvedTargetKind);
        ensureOrganizationAccess(organizationId);
        if (type == QrCodeType.ORGANIZATION && !organizations.existsById(referenceId)) {
            throw new NotFoundException("ORGANIZATION_NOT_FOUND", "Organization not found");
        }
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new BadRequestException("QR_INVALID_EXPIRATION", "Expiration must be in the future");
        }
        Organization organization = organizations.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("ORGANIZATION_NOT_FOUND", "Organization not found"));
        boolean hasSupplierIdentifier = supplierIdentifier != null && !supplierIdentifier.isBlank();
        if (!hasSupplierIdentifier) {
            List<QrCode> existingTargetIdentifiers = repository
                    .findAllByOrganizationIdAndTypeAndTargetKindAndReferenceIdOrderByCreatedAtAsc(
                            organizationId, type, resolvedTargetKind, referenceId);
            if (!existingTargetIdentifiers.isEmpty()) {
                return response(existingTargetIdentifiers.get(0));
            }
        }
        String identifier = hasSupplierIdentifier
                ? validateSupplierIdentifier(supplierIdentifier) : generateCode(type, organizationId);
        QrCode existingIdentifier = findInOrganization(organizationId, identifier).orElse(null);
        if (existingIdentifier != null) {
            if (matchesTarget(existingIdentifier, type, resolvedTargetKind, referenceId)) {
                return response(existingIdentifier);
            }
            throw new ConflictException("QR_IDENTIFIER_ALREADY_ASSIGNED",
                    "QR identifier is already assigned to a different target in this organization");
        }
        QrCode qr = new QrCode();
        qr.setPublicIdentifier(identifier);
        qr.setType(type);
        qr.setTargetKind(resolvedTargetKind);
        qr.setTargetId(referenceId);
        qr.setOrganization(organization);
        qr.setExpiresAt(expiresAt);
        qr.setStatus(QrCodeStatus.ACTIVE);
        try {
            QrCode saved = creationPersistence.save(qr);
            publish("QR_CODE_CREATED", saved, null);
            auditService.record(organization, null, "QR_CODE_CREATED", "QrCode",
                    saved.getPublicIdentifier(), null, saved.getType().name());
            return response(saved);
        } catch (DataIntegrityViolationException conflict) {
            QrCode racedIdentifier = findInOrganization(organizationId, identifier).orElse(null);
            if (racedIdentifier != null && matchesTarget(racedIdentifier, type, resolvedTargetKind, referenceId)) {
                return response(racedIdentifier);
            }
            throw new ConflictException("QR_IDENTIFIER_ALREADY_ASSIGNED",
                    "QR identifier is already assigned to a different target in this organization");
        }
    }

    @Transactional
    public QrCodeResponse resolve(String identifier) {
        String code = normalize(identifier);
        QrCode qr = lookupForResolve(code);
        UUID userId = currentUserId();
        publishScanned(code, qr, userId);
        if (qr == null) {
            saveScan(null, userId, QrScanResult.NOT_FOUND);
            publishFailed(code, QrScanResult.NOT_FOUND, userId);
            throw new NotFoundException("QR_NOT_FOUND", "QR code not found");
        }
        QrScanResult result = validationResult(qr);
        if (result != null) {
            saveScan(qr, userId, result);
            publishFailed(qr.getPublicIdentifier(), result, userId);
            throw new ForbiddenException("QR_" + result.name(), "QR code is not resolvable");
        }
        saveScan(qr, userId, QrScanResult.RESOLVED);
        publish("QR_CODE_RESOLVED", qr, userId);
        return response(qr, productProjection(qr));
    }

    @Transactional
    public QrCodeResponse disableQrCode(UUID id) {
        return transition(id, QrCodeStatus.DISABLED, "QR_CODE_DISABLED");
    }

    @Transactional
    public QrCodeResponse reactivateQrCode(UUID id) {
        return transition(id, QrCodeStatus.ACTIVE, "QR_CODE_REACTIVATED");
    }

    @Transactional
    public QrCodeResponse revokeQrCode(UUID id) {
        return transition(id, QrCodeStatus.REVOKED, "QR_CODE_REVOKED");
    }

    @Transactional
    public QrCodeResponse changeStatus(UUID id, QrCodeStatus status) {
        return switch (status) {
            case DISABLED -> disableQrCode(id);
            case ACTIVE -> reactivateQrCode(id);
            case REVOKED -> revokeQrCode(id);
            case EXPIRED -> throw new BadRequestException("QR_INVALID_TRANSITION", "Expiration is determined by expiresAt");
        };
    }

    @Transactional
    public QrCodeResponse regenerate(UUID id) {
        QrCode old = owned(id);
        revokeQrCode(id);
        return createQrCode(old.getType(), old.getOrganization().getId(), old.getTargetId(),
                old.getTargetKind(), old.getExpiresAt());
    }

    @Transactional
    public byte[] image(String identifier) {
        resolve(identifier);
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(URL_PREFIX + normalize(identifier),
                    BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate QR image", exception);
        }
    }
    @Transactional(readOnly = true)
    public Optional<QrCodeResponse> findInOrganizationForReceiving(
            UUID organizationId,
            String identifier
    ) {
        ensureOrganizationAccess(organizationId);

        if (identifier == null || identifier.isBlank()) {
            throw new BadRequestException(
                    "QR_INVALID_SUPPLIER_IDENTIFIER",
                    "QR identifier is required"
            );
        }

        return findInOrganization(organizationId, identifier.trim())
                .map(qr -> {
                    QrScanResult result = validationResult(qr);

                    if (result != null) {
                        throw new ForbiddenException(
                                "QR_" + result.name(),
                                "QR code is not resolvable"
                        );
                    }

                    if (!qr.getOrganization().getId().equals(organizationId)) {
                        throw new ForbiddenException("UNAUTHORIZED_QR_ACCESS");
                    }

                    return response(qr);
                });
    }

    private QrCode lookupForResolve(String code) {
        UUID organizationId = currentOrganizationId();
        if (organizationId != null) {
            return findInOrganization(organizationId, code).orElse(null);
        }
        List<QrCode> matches = repository.findAllByCodeIgnoreCase(code);
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        throw new ConflictException("QR_IDENTIFIER_AMBIGUOUS",
                "QR identifier exists in multiple organizations; authenticate to resolve within your organization");
    }

    private java.util.Optional<QrCode> findInOrganization(UUID organizationId, String identifier) {
        return repository.findByOrganizationIdAndCode(organizationId, identifier)
                .or(() -> repository.findByOrganizationIdAndCodeIgnoreCase(organizationId, identifier));
    }

    private boolean matchesTarget(QrCode qr, QrCodeType type, QrTargetKind targetKind, UUID referenceId) {
        return qr.getType() == type
                && qr.getTargetKind() == targetKind
                && qr.getTargetId().equals(referenceId);
    }

    private QrCodeResponse transition(UUID id, QrCodeStatus target, String eventType) {
        QrCode qr = owned(id);
        actor.requirePermission(permissionFor(qr.getType()));
        QrCodeStatus current = qr.getStatus();
        if (current == QrCodeStatus.REVOKED || (target == QrCodeStatus.ACTIVE && current != QrCodeStatus.DISABLED)
                || (target == QrCodeStatus.DISABLED && current != QrCodeStatus.ACTIVE)
                || (target == QrCodeStatus.REVOKED && current != QrCodeStatus.ACTIVE)
                || target == QrCodeStatus.EXPIRED) {
            throw new ConflictException("QR_INVALID_TRANSITION", "Invalid QR code lifecycle transition");
        }
        qr.setStatus(target);
        publish(eventType, qr, currentUserId());
        auditService.record(qr.getOrganization(), null, eventType, "QrCode",
                qr.getPublicIdentifier(), current.name(), target.name());
        return response(qr);
    }

    private QrCode owned(UUID id) {
        QrCode qr = repository.findById(id).orElseThrow(() -> new NotFoundException("QR_NOT_FOUND"));
        if (!qr.getOrganization().getId().equals(actor.organizationId())) {
            throw new ForbiddenException("UNAUTHORIZED_QR_ACCESS");
        }
        return qr;
    }

    private QrScanResult validationResult(QrCode qr) {
        if (qr.isExpired(Instant.now())) {
            if (qr.getStatus() == QrCodeStatus.ACTIVE) qr.setStatus(QrCodeStatus.EXPIRED);
            return QrScanResult.EXPIRED;
        }
        return switch (qr.getStatus()) {
            case ACTIVE -> null;
            case DISABLED -> QrScanResult.DISABLED;
            case EXPIRED -> QrScanResult.EXPIRED;
            case REVOKED -> QrScanResult.REVOKED;
        };
    }

    private void saveScan(QrCode qr, UUID userId, QrScanResult result) {
        if (scans != null) scans.save(new QrCodeScan(qr, userId, result));
    }

    private void publish(String type, QrCode qr, UUID userId) {
        if (events == null || qr == null) return;
        events.publish(new DomainEvent(type, qr.getOrganization().getId(), userId, qr.getId(), Instant.now(),
                Map.of("code", qr.getPublicIdentifier(), "type", qr.getType().name(),
                        "referenceId", qr.getTargetId().toString())));
    }

    private void publishScanned(String code, QrCode qr, UUID userId) {
        if (qr != null) {
            publish("QR_CODE_SCANNED", qr, userId);
        } else if (events != null) {
            events.publish(new DomainEvent("QR_CODE_SCANNED", null, userId, null, Instant.now(),
                    Map.of("code", code)));
        }
    }

    private void publishFailed(String code, QrScanResult result, UUID userId) {
        if (events != null) {
            events.publish(new DomainEvent("QR_CODE_SCAN_FAILED", null, userId, null, Instant.now(),
                    Map.of("code", code, "result", result.name())));
        }
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal) {
            return principal.userId();
        }
        return null;
    }

    private UUID currentOrganizationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal) {
            return principal.organizationId();
        }
        try {
            return actor.organizationId();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void ensureOrganizationAccess(UUID organizationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal
                && !organizationId.equals(principal.organizationId())) {
            throw new ForbiddenException("UNAUTHORIZED_QR_ACCESS");
        }
    }

    private String generateCode(QrCodeType type, UUID organizationId) {
        String prefix = switch (type) {
            case JOB_OPENING -> "JOB";
            case PRODUCT -> "PRD";
            case LOCATION -> "LOC";
            default -> "QED";
        };
        String code;
        do {
            StringBuilder suffix = new StringBuilder(8);
            for (int i = 0; i < 8; i++) suffix.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            code = prefix + "-" + suffix;
        } while (repository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code));
        return code;
    }

    private String normalize(String identifier) {
        if (identifier == null || identifier.isBlank()) throw new NotFoundException("QR_NOT_FOUND");
        return identifier.trim().toUpperCase(Locale.ROOT);
    }

    private QrTargetKind defaultTargetKind(QrCodeType type) {
        return switch (type) {
            case ORGANIZATION -> QrTargetKind.ORGANIZATION;
            case PRODUCT -> QrTargetKind.PRODUCT;
            default -> null;
        };
    }

    private String validateSupplierIdentifier(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim()) || value.length() > 40) {
            throw new BadRequestException("QR_INVALID_SUPPLIER_IDENTIFIER",
                    "Supplier QR identifiers must be non-blank, untrimmed values of at most 40 characters");
        }
        return value;
    }

    private void validateTargetKind(QrCodeType type, QrTargetKind targetKind) {
        boolean valid = switch (type) {
            case ORGANIZATION -> targetKind == QrTargetKind.ORGANIZATION;
            case PRODUCT -> targetKind == QrTargetKind.PRODUCT || targetKind == QrTargetKind.ORGANIZATION_PRODUCT;
            case INVENTORY_ITEM -> targetKind == QrTargetKind.SERIALIZED_ITEM;
            case LOCATION -> targetKind == QrTargetKind.WAREHOUSE_ZONE || targetKind == QrTargetKind.BIN_LOCATION;
            default -> targetKind == null;
        };
        if (!valid) {
            throw new BadRequestException("QR_INVALID_TARGET_KIND", "QR type and target kind are incompatible");
        }
    }

    private Permission permissionFor(QrCodeType type) {
        return switch (type) {
            case ORGANIZATION -> Permission.ORGANIZATION_UPDATE;
            case PRODUCT -> Permission.PRODUCT_MANAGE;
            case LOCATION, INVENTORY_ITEM -> Permission.INVENTORY_MANAGE;
            case SALES_PACKAGE -> Permission.SALES_MANAGE;
            case EMPLOYEE, JOB_OPENING, INVITATION -> Permission.EMPLOYEE_MANAGE;
        };
    }

    private QrCodeResponse response(QrCode qr) {
        return response(qr, null);
    }

    private QrCodeResponse response(QrCode qr, QrProductResponse product) {
        UUID targetId = qr.getType() == QrCodeType.ORGANIZATION ? null : qr.getTargetId();
        return new QrCodeResponse(qr.getId(), qr.getPublicIdentifier(), qr.getType(), qr.getTargetKind(),
                qr.getOrganization().getId(), targetId, qr.getStatus(),
                qr.getCreatedAt(), qr.getUpdatedAt(), qr.getExpiresAt(), product);
    }

    private QrProductResponse productProjection(QrCode qr) {
        if (qr.getType() != QrCodeType.PRODUCT || qr.getTargetKind() != QrTargetKind.PRODUCT) {
            return null;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            return null;
        }
        actor.requirePermission(Permission.PRODUCT_VIEW);
        Product product = products.findById(qr.getTargetId()).orElse(null);
        if (product == null || !organizationProducts.existsByOrganizationIdAndProductId(
                qr.getOrganization().getId(), product.getId())) {
            return null;
        }
        return new QrProductResponse(product.getId(), null, product.getName(), null,
                product.getUnit() == null ? null : product.getUnit().getSymbol());
    }
}
