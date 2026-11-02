package q.qedet.employee.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import q.qedet.common.model.Permission;
import q.qedet.employee.entity.*;
import q.qedet.employee.repository.PermissionRepository;

@Component
@Order(0)
public class PermissionInitializer implements CommandLineRunner {
    private final PermissionRepository repository;
    public PermissionInitializer(PermissionRepository repository) { this.repository = repository; }
    @Override public void run(String... args) {
        for (Permission value : Permission.values()) {
            if (repository.findByCodeIgnoreCase(value.name()).isEmpty()) {
                q.qedet.employee.entity.Permission permission = new q.qedet.employee.entity.Permission();
                permission.setCode(value.name()); permission.setName(value.name());
                permission.setModule(value.name().split("_", 2)[0]);
                repository.save(permission);
            }
        }
    }
}
