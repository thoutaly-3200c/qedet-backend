package q.qedet.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import q.qedet.employee.entity.RolePermission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
    Optional<RolePermission> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    boolean existsByRoleIdAndPermissionId(
            UUID roleId,
            UUID permissionId
    );

    @Query("""
            select distinct rolePermission.permission.code
            from RolePermission rolePermission
            where rolePermission.role.id in :roleIds
            """)
    List<String> findPermissionCodesByRoleIds(@Param("roleIds") Collection<UUID> roleIds);
}
