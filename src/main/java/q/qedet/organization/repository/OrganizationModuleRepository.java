package q.qedet.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.organization.entity.OrganizationModule;
import q.qedet.organization.entity.OrganizationModuleType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationModuleRepository extends JpaRepository<OrganizationModule, UUID> {
    List<OrganizationModule> findAllByOrganizationId(UUID organizationId);
    Optional<OrganizationModule> findByOrganizationIdAndModule(UUID organizationId, OrganizationModuleType module);
}
