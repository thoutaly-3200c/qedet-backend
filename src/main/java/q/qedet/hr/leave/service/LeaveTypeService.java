package q.qedet.hr.leave.service;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import q.qedet.auth.security.ActorContext; import q.qedet.common.exception.*; import q.qedet.common.model.Permission; import q.qedet.hr.leave.dto.LeaveTypeDtos.*; import q.qedet.hr.leave.entity.LeaveType; import q.qedet.hr.leave.repository.LeaveTypeRepository; import q.qedet.organization.entity.Organization; import q.qedet.organization.repository.OrganizationRepository; import java.util.*;
@Service public class LeaveTypeService {
 private final ActorContext actor; private final LeaveTypeRepository types; private final OrganizationRepository organizations;
 public LeaveTypeService(ActorContext actor,LeaveTypeRepository types,OrganizationRepository organizations){this.actor=actor;this.types=types;this.organizations=organizations;}
 @Transactional(readOnly=true) public List<Response> list(){actor.requirePermission(Permission.LEAVE_VIEW);return types.findAllByOrganizationId(actor.organizationId()).stream().map(this::out).toList();}
 @Transactional(readOnly=true) public Response get(UUID id){actor.requirePermission(Permission.LEAVE_VIEW);return out(type(id));}
 @Transactional public Response create(Request r){actor.requirePermission(Permission.LEAVE_MANAGE);LeaveType t=new LeaveType();t.setOrganization(org());apply(t,r);return out(types.save(t));}
 @Transactional public Response update(UUID id,Request r){actor.requirePermission(Permission.LEAVE_MANAGE);LeaveType t=type(id);if(!t.isActive())throw new ConflictException("Leave type is archived");apply(t,r);return out(t);}
 @Transactional public Response archive(UUID id){actor.requirePermission(Permission.LEAVE_MANAGE);LeaveType t=type(id);t.setActive(false);return out(t);}
 private LeaveType type(UUID id){return types.findByIdAndOrganizationId(id,actor.organizationId()).orElseThrow(()->new NotFoundException("Leave type not found"));}
 private Organization org(){return organizations.findById(actor.organizationId()).orElseThrow(()->new NotFoundException("Organization not found"));}
 private void apply(LeaveType t,Request r){t.setName(r.name().trim());t.setCode(r.code().trim().toUpperCase());t.setDescription(r.description());t.setPaid(r.paid());t.setRequiresApproval(r.requiresApproval());t.setRequiresAttachment(r.requiresAttachment());t.setMinimumNoticeDays(r.minimumNoticeDays()==null?0:r.minimumNoticeDays());t.setMaximumConsecutiveDays(r.maximumConsecutiveDays());t.setAnnualAllowance(r.annualAllowance());}
 private Response out(LeaveType t){return new Response(t.getId(),t.getName(),t.getCode(),t.getDescription(),t.isPaid(),t.isRequiresApproval(),t.isRequiresAttachment(),t.getMinimumNoticeDays(),t.getMaximumConsecutiveDays(),t.getAnnualAllowance(),t.isActive(),t.getCreatedAt(),t.getUpdatedAt());}
}
