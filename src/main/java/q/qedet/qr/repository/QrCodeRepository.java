package q.qedet.qr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.qr.entity.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QrCodeRepository extends JpaRepository<QrCode, UUID> {
    Optional<QrCode> findByCode(String code);
    Optional<QrCode> findByCodeIgnoreCase(String code);
    List<QrCode> findAllByCodeIgnoreCase(String code);
    boolean existsByCode(String code);
    Optional<QrCode> findByCodeAndStatus(String code, QrCodeStatus status);

    Optional<QrCode> findByOrganizationIdAndCode(UUID organizationId, String code);
    Optional<QrCode> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    List<QrCode> findAllByOrganizationIdAndTypeAndTargetKindAndReferenceIdOrderByCreatedAtAsc(
            UUID organizationId, QrCodeType type, QrTargetKind targetKind, UUID referenceId);

    default Optional<QrCode> findByPublicIdentifier(String identifier) {
        return findByCode(identifier);
    }

    default Optional<QrCode> findByOrganizationIdAndPublicIdentifier(UUID organizationId, String identifier) {
        return findByOrganizationIdAndCode(organizationId, identifier);
    }

    default boolean existsByPublicIdentifier(String identifier) {
        return existsByCode(identifier);
    }

    default boolean existsByOrganizationIdAndPublicIdentifier(UUID organizationId, String identifier) {
        return existsByOrganizationIdAndCode(organizationId, identifier);
    }

    default Optional<QrCode> findByPublicIdentifierAndStatus(String identifier, QrCodeStatus status) {
        return findByCodeAndStatus(identifier, status);
    }
}
