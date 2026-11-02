package q.qedet.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.employee.entity.Permission;

import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    Optional<Permission> findByCode(String code);

    Optional<Permission> findByCodeIgnoreCase(String code);
}
