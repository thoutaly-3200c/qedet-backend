package q.qedet.organization.policy.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.organization.policy.entity.OrganizationHoliday; import java.time.LocalDate; import java.util.*;
public interface OrganizationHolidayRepository extends JpaRepository<OrganizationHoliday,UUID> {
 List<OrganizationHoliday> findAllByOrganizationId(UUID organizationId);
 Optional<OrganizationHoliday> findByIdAndOrganizationId(UUID id,UUID organizationId);
 List<OrganizationHoliday> findAllByOrganizationIdAndDateAndActiveTrue(UUID organizationId,LocalDate date);
}
