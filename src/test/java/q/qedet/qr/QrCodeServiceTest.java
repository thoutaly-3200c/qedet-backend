package q.qedet.qr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.audit.AuditService;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.ForbiddenException;
import q.qedet.common.model.Permission;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.product.entity.Product;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.product.repository.ProductRepository;
import q.qedet.qr.dto.QrCodeRequest;
import q.qedet.qr.dto.QrCodeResponse;
import q.qedet.qr.entity.*;
import q.qedet.qr.repository.QrCodeRepository;
import q.qedet.qr.repository.QrCodeScanRepository;
import q.qedet.qr.service.QrCodeService;
import q.qedet.qr.service.QrCodeCreationPersistence;
import q.qedet.hr.common.event.DomainEventPublisher;
import q.qedet.warehouse.entity.ReceivingOperationSerial;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {
    @Mock QrCodeRepository repository;
    @Mock QrCodeCreationPersistence creationPersistence;
    @Mock OrganizationRepository organizations;
    @Mock ProductRepository products;
    @Mock OrganizationProductRepository organizationProducts;
    @Mock ActorContext actor;
    @Mock AuditService auditService;
    @Mock QrCodeScanRepository scans;
    @Mock DomainEventPublisher events;
    @InjectMocks QrCodeService service;

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesProductQrWithAuthorizedProductProjection() {
        UUID organizationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        QrCode qr = productQr(organization(organizationId), productId, "PRODUCT-QR");
        Product product = new Product();
        product.setId(productId);
        product.setName("Widget");
        authenticate(organizationId, Set.of(Permission.PRODUCT_VIEW));
        when(repository.findByOrganizationIdAndCode(organizationId, "PRODUCT-QR")).thenReturn(Optional.of(qr));
        when(products.findById(productId)).thenReturn(Optional.of(product));
        when(organizationProducts.existsByOrganizationIdAndProductId(organizationId, productId)).thenReturn(true);

        QrCodeResponse response = service.resolve("product-qr");

        assertNotNull(response.product());
        assertEquals(productId, response.product().productId());
        assertNull(response.product().sku());
        assertEquals("Widget", response.product().name());
        assertNull(response.product().barcode());
        assertNull(response.product().unit());
        verify(actor).requirePermission(Permission.PRODUCT_VIEW);
        verify(products).findById(productId);
        verify(organizationProducts).existsByOrganizationIdAndProductId(organizationId, productId);
    }

    @Test
    void productQrIsNotResolvedOutsideTheAuthenticatedOrganization() {
        UUID callerOrganizationId = UUID.randomUUID();
        authenticate(callerOrganizationId, Set.of(Permission.PRODUCT_VIEW));
        when(repository.findByOrganizationIdAndCode(callerOrganizationId, "FOREIGN-QR"))
                .thenReturn(Optional.empty());
        when(repository.findByOrganizationIdAndCodeIgnoreCase(callerOrganizationId, "FOREIGN-QR"))
                .thenReturn(Optional.empty());

        assertThrows(q.qedet.common.exception.NotFoundException.class, () -> service.resolve("foreign-qr"));

        verifyNoInteractions(products);
    }

    @Test
    void unknownProductQrReturnsNotFound() {
        UUID organizationId = UUID.randomUUID();
        authenticate(organizationId, Set.of(Permission.PRODUCT_VIEW));
        when(repository.findByOrganizationIdAndCode(organizationId, "UNKNOWN-QR")).thenReturn(Optional.empty());
        when(repository.findByOrganizationIdAndCodeIgnoreCase(organizationId, "UNKNOWN-QR"))
                .thenReturn(Optional.empty());

        assertThrows(q.qedet.common.exception.NotFoundException.class, () -> service.resolve("unknown-qr"));

        verifyNoInteractions(products);
    }

    @Test
    void productQrRequiresProductViewPermissionForProductDetails() {
        UUID organizationId = UUID.randomUUID();
        QrCode qr = productQr(organization(organizationId), UUID.randomUUID(), "PRODUCT-QR");
        authenticate(organizationId, Set.of());
        when(repository.findByOrganizationIdAndCode(organizationId, "PRODUCT-QR")).thenReturn(Optional.of(qr));
        doThrow(new ForbiddenException("Missing permission: PRODUCT_VIEW"))
                .when(actor).requirePermission(Permission.PRODUCT_VIEW);

        assertThrows(ForbiddenException.class, () -> service.resolve("product-qr"));

        verifyNoInteractions(products);
    }

    @Test
    void anonymousProductQrResolutionKeepsMetadataButDoesNotExposeProduct() {
        UUID organizationId = UUID.randomUUID();
        QrCode qr = productQr(organization(organizationId), UUID.randomUUID(), "PUBLIC-QR");
        when(actor.organizationId()).thenThrow(new q.qedet.common.exception.UnauthorizedException("Authentication required"));
        when(repository.findAllByCodeIgnoreCase("PUBLIC-QR")).thenReturn(List.of(qr));

        QrCodeResponse response = service.resolve("public-qr");

        assertEquals("PUBLIC-QR", response.code());
        assertNull(response.product());
        verifyNoInteractions(products);
    }

    @Test
    void createsAndResolvesOrganizationQrWithinOrganization() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = organization(organizationId);
        when(actor.organizationId()).thenReturn(organizationId);
        when(organizations.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizations.existsById(organizationId)).thenReturn(true);
        when(repository.findAllByOrganizationIdAndTypeAndTargetKindAndReferenceIdOrderByCreatedAtAsc(
                eq(organizationId), eq(QrCodeType.ORGANIZATION), eq(QrTargetKind.ORGANIZATION), eq(organizationId)))
                .thenReturn(List.of());
        when(repository.existsByOrganizationIdAndCodeIgnoreCase(eq(organizationId), anyString())).thenReturn(false);
        when(creationPersistence.save(any(QrCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QrCodeResponse created = service.create(new QrCodeRequest(
                QrCodeType.ORGANIZATION, organizationId, null));

        assertTrue(created.publicIdentifier().startsWith("QED-"));
        assertEquals(QrCodeStatus.ACTIVE, created.status());

        when(repository.findByOrganizationIdAndCode(organizationId, created.publicIdentifier()))
                .thenReturn(Optional.of(toEntity(created, organization)));
        when(actor.organizationId()).thenReturn(organizationId);

        QrCodeResponse resolved = service.resolve(created.publicIdentifier().toLowerCase());

        assertEquals(created.publicIdentifier(), resolved.publicIdentifier());
        assertNull(resolved.targetId());
    }

    @Test
    void rejectsRevokedQr() {
        UUID organizationId = UUID.randomUUID();
        QrCode qr = new QrCode();
        qr.setPublicIdentifier("QED-REVOKED");
        qr.setStatus(QrCodeStatus.REVOKED);
        qr.setOrganization(organization(organizationId));
        when(actor.organizationId()).thenReturn(organizationId);
        when(repository.findByOrganizationIdAndCode(organizationId, "QED-REVOKED")).thenReturn(Optional.of(qr));

        assertThrows(RuntimeException.class, () -> service.resolve(" qed-revoked "));
    }

    @Test
    void reusesSupplierQrAlreadyBoundToSameProductInOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Organization organization = organization(organizationId);
        QrCode existing = productQr(organization, productId, "SUPPLIER-QR-001");
        when(organizations.findById(organizationId)).thenReturn(Optional.of(organization));
        when(repository.findByOrganizationIdAndCode(organizationId, "SUPPLIER-QR-001"))
                .thenReturn(Optional.of(existing));

        QrCodeResponse response = service.resolveOrCreate(QrCodeType.PRODUCT, organizationId, productId,
                QrTargetKind.PRODUCT, "SUPPLIER-QR-001");

        assertEquals("SUPPLIER-QR-001", response.publicIdentifier());
        verify(creationPersistence, never()).save(any());
    }

    @Test
    void storesUnknownSupplierQrAsAdditionalIdentifierWithoutReplacingInternalQr() {
        UUID organizationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Organization organization = organization(organizationId);
        QrCode internal = productQr(organization, productId, "PRD-INTERNAL-001");
        when(organizations.findById(organizationId)).thenReturn(Optional.of(organization));
        when(repository.findByOrganizationIdAndCode(organizationId, "SUPPLIER-QR-001"))
                .thenReturn(Optional.empty());
        when(repository.findByOrganizationIdAndCodeIgnoreCase(organizationId, "SUPPLIER-QR-001"))
                .thenReturn(Optional.empty());
        when(creationPersistence.save(any(QrCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QrCodeResponse response = service.resolveOrCreate(QrCodeType.PRODUCT, organizationId, productId,
                QrTargetKind.PRODUCT, "SUPPLIER-QR-001");

        assertEquals("SUPPLIER-QR-001", response.publicIdentifier());
        assertEquals(productId, response.targetId());
        assertEquals("PRD-INTERNAL-001", internal.getPublicIdentifier());
        verify(creationPersistence).save(argThat(qr ->
                qr.getTargetId().equals(productId) && "SUPPLIER-QR-001".equals(qr.getPublicIdentifier())));
    }

    @Test
    void acceptsUnknownSupplierQrWhenTheProductAlreadyHasAnInternalQr() {
        UUID organizationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(repository.findByOrganizationIdAndCode(organizationId, "SUPPLIER-QR-NEW"))
                .thenReturn(Optional.empty());
        when(repository.findByOrganizationIdAndCodeIgnoreCase(organizationId, "SUPPLIER-QR-NEW"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.assertAssignable(QrCodeType.PRODUCT, organizationId, productId,
                QrTargetKind.PRODUCT, "SUPPLIER-QR-NEW"));

        verify(repository, never())
                .findAllByOrganizationIdAndTypeAndTargetKindAndReferenceIdOrderByCreatedAtAsc(any(), any(), any(), any());
    }

    @Test
    void rejectsSupplierQrAssignedToDifferentProductInSameOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID otherProductId = UUID.randomUUID();
        Organization organization = organization(organizationId);
        QrCode existing = productQr(organization, otherProductId, "SUPPLIER-QR-001");
        when(organizations.findById(organizationId)).thenReturn(Optional.of(organization));
        when(repository.findByOrganizationIdAndCode(organizationId, "SUPPLIER-QR-001"))
                .thenReturn(Optional.of(existing));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.resolveOrCreate(QrCodeType.PRODUCT, organizationId, productId,
                        QrTargetKind.PRODUCT, "SUPPLIER-QR-001"));
        assertEquals("QR_IDENTIFIER_ALREADY_ASSIGNED", ex.getCode());
    }

    @Test
    void allowsSameExternalQrValueInTwoOrganizations() {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        Organization organizationB = organization(orgB);
        when(organizations.findById(orgB)).thenReturn(Optional.of(organizationB));
        when(repository.findByOrganizationIdAndCode(orgB, "SHARED-SUPPLIER-QR"))
                .thenReturn(Optional.empty());
        when(repository.findByOrganizationIdAndCodeIgnoreCase(orgB, "SHARED-SUPPLIER-QR"))
                .thenReturn(Optional.empty());
        when(creationPersistence.save(any(QrCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QrCodeResponse created = service.resolveOrCreate(QrCodeType.PRODUCT, orgB, productB,
                QrTargetKind.PRODUCT, "SHARED-SUPPLIER-QR");

        assertEquals("SHARED-SUPPLIER-QR", created.publicIdentifier());
        assertEquals(orgB, created.organizationId());
        verify(repository, never()).findByPublicIdentifier("SHARED-SUPPLIER-QR");
        verify(creationPersistence).save(argThat(qr ->
                qr.getOrganization().getId().equals(orgB) && productB.equals(qr.getTargetId())));
        assertNotEquals(orgA, orgB);
        assertNotEquals(productA, productB);
    }

    @Test
    void assertAssignableRejectsForeignOrganizationAccess() {
        UUID callerOrg = UUID.randomUUID();
        UUID otherOrg = UUID.randomUUID();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new q.qedet.auth.security.AuthenticatedPrincipal(
                                UUID.randomUUID(), UUID.randomUUID(), callerOrg,
                                q.qedet.common.model.RoleType.WAREHOUSE_EMPLOYEE, java.util.Set.of(),
                                "warehouse@example.com"),
                        null));

        try {
            assertThrows(ForbiddenException.class, () ->
                    service.assertAssignable(QrCodeType.PRODUCT, otherOrg, UUID.randomUUID(),
                            QrTargetKind.PRODUCT, "SUPPLIER-QR"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void handlesConcurrentRegistrationByReturningExistingBinding() {
        UUID organizationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Organization organization = organization(organizationId);
        QrCode raced = productQr(organization, productId, "SUPPLIER-QR-RACE");
        when(organizations.findById(organizationId)).thenReturn(Optional.of(organization));
        when(repository.findByOrganizationIdAndCode(organizationId, "SUPPLIER-QR-RACE"))
                .thenReturn(Optional.empty()).thenReturn(Optional.of(raced));
        when(repository.findByOrganizationIdAndCodeIgnoreCase(organizationId, "SUPPLIER-QR-RACE"))
                .thenReturn(Optional.empty());
        when(creationPersistence.save(any(QrCode.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        QrCodeResponse response = service.resolveOrCreate(QrCodeType.PRODUCT, organizationId, productId,
                QrTargetKind.PRODUCT, "SUPPLIER-QR-RACE");

        assertEquals("SUPPLIER-QR-RACE", response.publicIdentifier());
        assertEquals(productId, response.targetId());
    }

    @Test
    void associatesGeneratedSerializedQrWithReceivingSerialIdentity() {
        UUID organizationId = UUID.randomUUID();
        UUID serialId = UUID.randomUUID();
        Organization organization = organization(organizationId);
        ReceivingOperationSerial serial = new ReceivingOperationSerial();
        serial.setId(serialId);
        serial.setSerialNumber("SN-1");
        when(organizations.findById(organizationId)).thenReturn(Optional.of(organization));
        when(repository.findAllByOrganizationIdAndTypeAndTargetKindAndReferenceIdOrderByCreatedAtAsc(
                organizationId, QrCodeType.INVENTORY_ITEM, QrTargetKind.SERIALIZED_ITEM, serialId))
                .thenReturn(List.of());
        when(repository.existsByOrganizationIdAndCodeIgnoreCase(eq(organizationId), anyString())).thenReturn(false);
        when(creationPersistence.save(any(QrCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QrCodeResponse response = service.resolveOrCreateReceivingSerial(organizationId, serial);

        assertTrue(response.publicIdentifier().startsWith("QED-"));
        assertEquals(serialId, response.targetId());
        assertEquals(QrTargetKind.SERIALIZED_ITEM, response.targetKind());
    }

    @Test
    void resolveInOrganizationDoesNotLeakOtherOrganizationBindings() {
        UUID organizationId = UUID.randomUUID();
        when(repository.findByOrganizationIdAndCode(organizationId, "FOREIGN-QR")).thenReturn(Optional.empty());
        when(repository.findByOrganizationIdAndCodeIgnoreCase(organizationId, "FOREIGN-QR")).thenReturn(Optional.empty());

        assertThrows(q.qedet.common.exception.NotFoundException.class,
                () -> service.resolveInOrganization(organizationId, "FOREIGN-QR"));
    }

    @Test
    void ambiguousUnauthenticatedResolveRequiresOrganizationContext() {
        when(actor.organizationId()).thenThrow(new q.qedet.common.exception.UnauthorizedException("Authentication required"));
        when(repository.findAllByCodeIgnoreCase("SHARED")).thenReturn(List.of(
                productQr(organization(UUID.randomUUID()), UUID.randomUUID(), "SHARED"),
                productQr(organization(UUID.randomUUID()), UUID.randomUUID(), "SHARED")));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.resolve("SHARED"));
        assertEquals("QR_IDENTIFIER_AMBIGUOUS", ex.getCode());
    }

    private Organization organization(UUID id) {
        Organization organization = new Organization();
        organization.setId(id);
        return organization;
    }

    private void authenticate(UUID organizationId, Set<Permission> permissions) {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new q.qedet.auth.security.AuthenticatedPrincipal(
                                UUID.randomUUID(), UUID.randomUUID(), organizationId,
                                q.qedet.common.model.RoleType.WAREHOUSE_EMPLOYEE, permissions,
                                "warehouse@example.com"),
                        null));
    }

    private QrCode productQr(Organization organization, UUID productId, String code) {
        QrCode qr = new QrCode();
        qr.setId(UUID.randomUUID());
        qr.setPublicIdentifier(code);
        qr.setType(QrCodeType.PRODUCT);
        qr.setTargetKind(QrTargetKind.PRODUCT);
        qr.setTargetId(productId);
        qr.setOrganization(organization);
        qr.setStatus(QrCodeStatus.ACTIVE);
        return qr;
    }

    private QrCode toEntity(QrCodeResponse response, Organization organization) {
        QrCode qr = new QrCode();
        qr.setPublicIdentifier(response.publicIdentifier());
        qr.setType(response.type());
        qr.setTargetKind(response.targetKind());
        qr.setTargetId(response.targetId() == null ? organization.getId() : response.targetId());
        qr.setOrganization(organization);
        qr.setStatus(response.status());
        return qr;
    }
}
