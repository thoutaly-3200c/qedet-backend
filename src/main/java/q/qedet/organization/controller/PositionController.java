package q.qedet.organization.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.organization.dto.PositionRequest;
import q.qedet.organization.dto.PositionResponse;
import q.qedet.organization.service.PositionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/positions")
public class PositionController {
    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    public List<PositionResponse> list(@PathVariable UUID organizationId,
                                       @RequestParam(required = false) UUID departmentId,
                                       @RequestParam(name = "locationId", required = false) UUID locationId,
                                       @RequestParam(required = false) Boolean active,
                                       @RequestParam(required = false) String search) {
        return positionService.list(organizationId, departmentId, locationId, active, search);
    }

    @PostMapping
    public PositionResponse create(@PathVariable UUID organizationId, @Valid @RequestBody PositionRequest request) {
        return positionService.create(organizationId, request);
    }

    @GetMapping("/{positionId}")
    public PositionResponse get(@PathVariable UUID organizationId, @PathVariable UUID positionId) {
        return positionService.get(organizationId, positionId);
    }

    @PatchMapping("/{positionId}")
    public PositionResponse update(@PathVariable UUID organizationId, @PathVariable UUID positionId,
                                   @Valid @RequestBody PositionRequest request) {
        return positionService.update(organizationId, positionId, request);
    }

    @DeleteMapping("/{positionId}")
    public void delete(@PathVariable UUID organizationId, @PathVariable UUID positionId) {
        positionService.delete(organizationId, positionId);
    }
}
