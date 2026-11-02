package q.qedet.organization.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.audit.AuditService;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.model.Permission;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.organization.dto.OrganizationConfigurationDtos.*;
import q.qedet.organization.entity.*;
import q.qedet.organization.repository.*;
import java.util.List;
import java.util.UUID;

@Service
public class OrganizationConfigurationService {
    private final ActorContext actor;
    private final OrganizationRepository organizations;
    private final OrganizationConfigurationRepository configurations;
    private final OrganizationModuleRepository modules;
    private final OrganizationNumberSequenceRepository sequences;
    private final AuditService audit;
    private final EmployeeRepository employees;
    public OrganizationConfigurationService(ActorContext actor, OrganizationRepository organizations,
            OrganizationConfigurationRepository configurations, OrganizationModuleRepository modules,
            OrganizationNumberSequenceRepository sequences, AuditService audit, EmployeeRepository employees) {
        this.actor = actor; this.organizations = organizations; this.configurations = configurations;
        this.modules = modules; this.sequences = sequences; this.audit = audit; this.employees = employees;
    }
    @Transactional
    public ConfigurationResponse get() { actor.requirePermission(Permission.ORGANIZATION_CONFIG_VIEW); return response(configuration()); }
    @Transactional(readOnly = true)
    public SalesTaxResponse salesTax() {
        actor.requirePermission(Permission.ORGANIZATION_CONFIG_VIEW);
        OrganizationConfiguration c = configurations.findByOrganizationId(actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Organization configuration not found"));
        return new SalesTaxResponse(c.getSalesTaxRate(), c.isSalesPricesTaxInclusive());
    }
    @Transactional
    public SalesTaxResponse salesTax(SalesTaxRequest request) {
        actor.requirePermission(Permission.ORGANIZATION_CONFIG_EDIT);
        OrganizationConfiguration c = configuration();
        c.setSalesTaxRate(request.ratePercent());
        c.setSalesPricesTaxInclusive(request.pricesTaxInclusive());
        audit("SALES_TAX_UPDATED", c);
        return new SalesTaxResponse(c.getSalesTaxRate(), c.isSalesPricesTaxInclusive());
    }
    @Transactional
    public ConfigurationResponse localization(LocalizationRequest request) {
        actor.requirePermission(Permission.ORGANIZATION_CONFIG_EDIT);
        try {
            java.time.ZoneId.of(request.timezone().trim());
        } catch (java.time.DateTimeException exception) {
            throw new BadRequestException("Invalid timezone");
        }
        OrganizationConfiguration c = configuration();
        c.getOrganization().setCountry(request.country().trim()); c.getOrganization().setTimezone(request.timezone().trim());
        c.setCurrency(request.currency().trim().toUpperCase()); c.setLanguage(request.language().trim());
        c.setDateFormat(request.dateFormat().trim()); c.setTimeFormat(request.timeFormat().trim()); c.setFirstDayOfWeek(request.firstDayOfWeek());
        audit("LOCALIZATION_UPDATED", c); return response(c);
    }
    @Transactional
    public ConfigurationResponse communication(CommunicationRequest r) {
        actor.requirePermission(Permission.ORGANIZATION_CONFIG_EDIT); OrganizationConfiguration c = configuration();
        if (r.emailEnabled()!=null)c.setEmailEnabled(r.emailEnabled()); if(r.smsEnabled()!=null)c.setSmsEnabled(r.smsEnabled());
        if(r.pushNotificationsEnabled()!=null)c.setPushNotificationsEnabled(r.pushNotificationsEnabled());
        if(r.defaultSenderName()!=null)c.setDefaultSenderName(r.defaultSenderName().trim());
        if(r.defaultSenderEmail()!=null)c.setDefaultSenderEmail(r.defaultSenderEmail().trim());
        audit("COMMUNICATION_UPDATED", c); return response(c);
    }
    @Transactional
    public ConfigurationResponse security(SecurityRequest r) {
        actor.requirePermission(Permission.ORGANIZATION_CONFIG_EDIT); OrganizationConfiguration c = configuration();
        if(r.sessionTimeoutMinutes()!=null)c.setSessionTimeoutMinutes(r.sessionTimeoutMinutes());
        if(r.mfaEnabled()!=null)c.setMfaEnabled(r.mfaEnabled()); if(r.mfaRequired()!=null)c.setMfaRequired(r.mfaRequired());
        if(c.isMfaRequired()) c.setMfaEnabled(true); if(r.allowMultipleSessions()!=null)c.setAllowMultipleSessions(r.allowMultipleSessions());
        audit("SECURITY_UPDATED", c); return response(c);
    }
    @Transactional(readOnly = true)
    public List<ModuleRequest> modules() {
        actor.requirePermission(Permission.ORGANIZATION_CONFIG_VIEW); UUID org = actor.organizationId();
        return java.util.Arrays.stream(OrganizationModuleType.values()).map(type -> new ModuleRequest(type,
                modules.findByOrganizationIdAndModule(org, type).map(OrganizationModule::isEnabled).orElse(false))).toList();
    }
    @Transactional
    public List<ModuleRequest> modules(ModulesRequest request) {
        actor.requirePermission(Permission.ORGANIZATION_CONFIG_EDIT); Organization org = organization();
        for (ModuleRequest r : request.modules()) { OrganizationModule m = modules.findByOrganizationIdAndModule(org.getId(), r.module()).orElseGet(OrganizationModule::new);
            if (m.getId()==null) { m.setOrganization(org); m.setModule(r.module()); } m.setEnabled(r.enabled()); modules.save(m); }
        audit.record(org, auditActor(), "MODULES_UPDATED", "OrganizationConfiguration", org.getId().toString(), null, null); return modules();
    }
    @Transactional(readOnly = true)
    public List<NumberingResponse> numbering() { actor.requirePermission(Permission.ORGANIZATION_CONFIG_VIEW); return sequences.findAllByOrganizationId(actor.organizationId()).stream().map(this::numbering).toList(); }
    @Transactional
    public NumberingResponse numbering(SequenceType type, NumberingRequest r) {
        actor.requirePermission(Permission.ORGANIZATION_CONFIG_EDIT); OrganizationNumberSequence s = sequences.findByOrganizationIdAndSequenceType(actor.organizationId(), type).orElseGet(OrganizationNumberSequence::new);
        if(s.getId()==null){s.setOrganization(organization());s.setSequenceType(type);} if(r.prefix()!=null)s.setPrefix(r.prefix());
        if(r.nextNumber()!=null)s.setNextNumber(r.nextNumber()); if(r.numberLength()!=null)s.setNumberLength(r.numberLength());
        s=sequences.save(s); audit.record(s.getOrganization(), auditActor(), "NUMBER_SEQUENCE_UPDATED", "OrganizationNumberSequence", s.getId().toString(), null, null); return numbering(s);
    }
    @Transactional
    public String nextNumber(SequenceType type) {
        Organization lockedOrganization = organizations.findByIdForUpdate(actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        OrganizationNumberSequence s = sequences.findForUpdate(actor.organizationId(), type).orElseGet(() -> {
            OrganizationNumberSequence fresh=new OrganizationNumberSequence(); fresh.setOrganization(lockedOrganization); fresh.setSequenceType(type); return sequences.saveAndFlush(fresh); });
        long value=s.getNextNumber(); s.setNextNumber(Math.incrementExact(value)); sequences.save(s);
        return s.getPrefix()+String.format("%0"+s.getNumberLength()+"d", value);
    }
    private OrganizationConfiguration configuration(){ UUID id=actor.organizationId(); return configurations.findByOrganizationId(id).orElseGet(()->{OrganizationConfiguration c=new OrganizationConfiguration();c.setOrganization(organization());return configurations.save(c);}); }
    private Organization organization(){return organizations.findById(actor.organizationId()).orElseThrow(()->new NotFoundException("Organization not found"));}
    private void audit(String action, OrganizationConfiguration c){audit.record(c.getOrganization(),auditActor(),action,"OrganizationConfiguration",c.getId().toString(),null,null);}
    private q.qedet.employee.entity.Employee auditActor(){return employees.findByIdAndOrganizationId(actor.employeeId(), actor.organizationId()).orElse(null);}
    private ConfigurationResponse response(OrganizationConfiguration c){Organization o=c.getOrganization();return new ConfigurationResponse(o.getCountry(),o.getTimezone(),c.getCurrency(),c.getLanguage(),c.getDateFormat(),c.getTimeFormat(),c.getFirstDayOfWeek(),c.isEmailEnabled(),c.isSmsEnabled(),c.isPushNotificationsEnabled(),c.getDefaultSenderName(),c.getDefaultSenderEmail(),c.getSessionTimeoutMinutes(),c.isMfaEnabled(),c.isMfaRequired(),c.isAllowMultipleSessions());}
    private NumberingResponse numbering(OrganizationNumberSequence s){return new NumberingResponse(s.getId(),s.getSequenceType(),s.getPrefix(),s.getNextNumber(),s.getNumberLength(),s.getCreatedAt(),s.getUpdatedAt());}
}
