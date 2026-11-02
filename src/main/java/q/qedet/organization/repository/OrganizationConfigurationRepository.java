package q.qedet.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.organization.entity.OrganizationConfiguration;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationConfigurationRepository extends JpaRepository<OrganizationConfiguration, UUID> {
    Optional<OrganizationConfiguration> findByOrganizationId(UUID organizationId);
}
