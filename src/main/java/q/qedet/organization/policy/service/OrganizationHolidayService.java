package q.qedet.organization.policy.service;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import q.qedet.auth.security.ActorContext; import q.qedet.common.exception.NotFoundException; import q.qedet.common.model.Permission; import q.qedet.organization.policy.dto.OrganizationHolidayDtos.*; import q.qedet.organization.policy.entity.OrganizationHoliday; import q.qedet.organization.policy.repository.OrganizationHolidayRepository; import q.qedet.organization.entity.Organization; import q.qedet.organization.repository.OrganizationRepository; import java.time.LocalDate; import java.util.*;
@Service public class OrganizationHolidayService {
 private final ActorContext actor; private final OrganizationHolidayRepository holidays; private final OrganizationRepository organizations;
 public OrganizationHolidayService(ActorContext actor,OrganizationHolidayRepository holidays,OrganizationRepository organizations){this.actor=actor;this.holidays=holidays;this.organizations=organizations;}
 @Transactional(readOnly=true) public List<Response> list(){actor.requirePermission(Permission.ATTENDANCE_VIEW);return holidays.findAllByOrganizationId(actor.organizationId()).stream().map(this::out).toList();}
 @Transactional public Response create(Request r){actor.requirePermission(Permission.ATTENDANCE_MANAGE);OrganizationHoliday h=new OrganizationHoliday();h.setOrganization(org());apply(h,r);return out(holidays.save(h));}
 @Transactional public Response update(UUID id,Request r){actor.requirePermission(Permission.ATTENDANCE_MANAGE);OrganizationHoliday h=owned(id);apply(h,r);return out(h);}
 @Transactional public Response archive(UUID id){actor.requirePermission(Permission.ATTENDANCE_MANAGE);OrganizationHoliday h=owned(id);h.setActive(false);return out(h);}
 public boolean isHoliday(UUID organizationId,LocalDate date){return holidays.findAllByOrganizationId(organizationId).stream().anyMatch(h->h.isActive() && (h.getDate().equals(date) || h.isRecurring() && h.getDate().getMonthValue()==date.getMonthValue() && h.getDate().getDayOfMonth()==date.getDayOfMonth()));}
 private void apply(OrganizationHoliday h,Request r){h.setName(r.name().trim());h.setDate(r.date());h.setRecurring(r.recurring());h.setType(r.type());if(r.active()!=null)h.setActive(r.active());}
 private OrganizationHoliday owned(UUID id){return holidays.findByIdAndOrganizationId(id,actor.organizationId()).orElseThrow(()->new NotFoundException("Holiday not found"));}
 private Organization org(){return organizations.findById(actor.organizationId()).orElseThrow(()->new NotFoundException("Organization not found"));}
 private Response out(OrganizationHoliday h){return new Response(h.getId(),h.getName(),h.getDate(),h.isRecurring(),h.getType(),h.isActive(),h.getCreatedAt(),h.getUpdatedAt());}
}
