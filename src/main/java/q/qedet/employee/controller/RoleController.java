package q.qedet.employee.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.employee.dto.*;
import q.qedet.employee.service.RoleService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService service;
    public RoleController(RoleService service) { this.service = service; }
    @GetMapping public List<RoleResponse> list() { return service.list(); }
    @PostMapping public RoleResponse create(@Valid @RequestBody RoleRequest request) { return service.create(request); }
    @PutMapping("/{id}") public RoleResponse update(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) { return service.update(id, request); }
    @PostMapping("/{id}/permissions") public RoleResponse addPermission(@PathVariable UUID id, @RequestParam String code) { return service.addPermission(id, code); }
    @DeleteMapping("/{id}/permissions/{permissionId}") public void removePermission(@PathVariable UUID id, @PathVariable UUID permissionId) { service.removePermission(id, permissionId); }
}
