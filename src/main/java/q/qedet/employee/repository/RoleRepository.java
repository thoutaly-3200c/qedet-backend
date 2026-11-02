package q.qedet.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.employee.entity.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    List<Role> findAllByOrganizationId(UUID organizationId);
    Optional<Role> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<Role> findByOrganizationIdAndCode(UUID organizationId, String code);
    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

}
