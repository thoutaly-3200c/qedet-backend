package q.qedet.product.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import q.qedet.product.dto.OrganizationProductRequest;
import q.qedet.product.dto.OrganizationProductResponse;
import q.qedet.product.service.OrganizationProductService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationProductController {

    private final OrganizationProductService organizationProductService;

    public OrganizationProductController(OrganizationProductService organizationProductService) {
        this.organizationProductService = organizationProductService;
    }

    @PostMapping("/{organizationId}/products")
    public OrganizationProductResponse create(@PathVariable UUID organizationId,
                                            @Valid @RequestBody OrganizationProductRequest request) {
        return organizationProductService.create(organizationId, request);
    }

    @GetMapping("/{organizationId}/products")
    public List<OrganizationProductResponse> list(@PathVariable UUID organizationId) {
        return organizationProductService.list(organizationId);
    }

    @GetMapping("/{organizationId}/products/{id}")
    public OrganizationProductResponse get(@PathVariable UUID organizationId, @PathVariable UUID id) {
        return organizationProductService.get(organizationId, id);
    }
}
