package q.qedet.organization.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.organization.dto.OrganizationConfigurationDtos.*;
import q.qedet.organization.entity.SequenceType;
import q.qedet.organization.service.OrganizationConfigurationService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/organization/configuration")
public class OrganizationConfigurationController {
    private final OrganizationConfigurationService service;
    public OrganizationConfigurationController(OrganizationConfigurationService service){this.service=service;}
    @GetMapping public ConfigurationResponse get(){return service.get();}
    @GetMapping("/sales-tax") public SalesTaxResponse salesTax(){return service.salesTax();}
    @PutMapping("/sales-tax") public SalesTaxResponse salesTax(@Valid @RequestBody SalesTaxRequest r){return service.salesTax(r);}
    @PatchMapping("/localization") public ConfigurationResponse localization(@Valid @RequestBody LocalizationRequest r){return service.localization(r);}
    @PatchMapping("/communication") public ConfigurationResponse communication(@Valid @RequestBody CommunicationRequest r){return service.communication(r);}
    @PatchMapping("/security") public ConfigurationResponse security(@Valid @RequestBody SecurityRequest r){return service.security(r);}
    @GetMapping("/modules") public List<ModuleRequest> modules(){return service.modules();}
    @PatchMapping("/modules") public List<ModuleRequest> modules(@Valid @RequestBody ModulesRequest r){return service.modules(r);}
    @GetMapping("/numbering") public List<NumberingResponse> numbering(){return service.numbering();}
    @PatchMapping("/numbering/{type}") public NumberingResponse numbering(@PathVariable SequenceType type,@Valid @RequestBody NumberingRequest r){return service.numbering(type,r);}
}
