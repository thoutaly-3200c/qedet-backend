package q.qedet.organization.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.organization.dto.OrganizationRequest;
import q.qedet.organization.dto.OrganizationResponse;
import q.qedet.organization.service.OrganizationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import q.qedet.auth.security.AuthenticatedPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/organization", "/api/v1/organizations"})
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping({"","/me"})
    public OrganizationResponse getCurrent() {
        return organizationService.get();
    }

    @GetMapping({"/mine", "/all"})
    public List<OrganizationResponse> listForCurrentUser() {
        return organizationService.listForCurrentUser();
    }

    @PostMapping
    public OrganizationResponse create(@Valid @RequestBody OrganizationRequest request,
                                       @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return organizationService.create(request);
    }

    @GetMapping("/{id}")
    public OrganizationResponse get(@PathVariable UUID id) {
        return organizationService.get(id);
    }

    @PutMapping({"","/me"})
    public OrganizationResponse update(@Valid @RequestBody OrganizationRequest request) {
        return organizationService.update(request);
    }

    @PutMapping("/{id}")
    public OrganizationResponse update(@PathVariable UUID id, @Valid @RequestBody OrganizationRequest request) {
        return organizationService.update(id, request);
    }

    @PostMapping({"/activate","/me/activate"})
    public OrganizationResponse activate() {
        return organizationService.activate();
    }

    @PostMapping({"/suspend","/me/suspend"})
    public OrganizationResponse suspend() {
        return organizationService.suspend();
    }
}
