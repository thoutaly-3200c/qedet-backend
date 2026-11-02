package q.qedet.employee.service;

import org.junit.jupiter.api.Test;
import q.qedet.common.model.Permission;
import q.qedet.employee.repository.PermissionRepository;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PermissionInitializerTest {

    @Test
    void seedsOnlyMissingPermissionDefinitions() {
        PermissionRepository repository = mock(PermissionRepository.class);
        when(repository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.empty());

        new PermissionInitializer(repository).run();

        verify(repository, times(Permission.values().length)).save(any(q.qedet.employee.entity.Permission.class));
    }

    @Test
    void doesNotDuplicateExistingPermissionDefinitions() {
        PermissionRepository repository = mock(PermissionRepository.class);
        when(repository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.of(new q.qedet.employee.entity.Permission()));

        new PermissionInitializer(repository).run();

        verify(repository, never()).save(any(q.qedet.employee.entity.Permission.class));
    }
}
