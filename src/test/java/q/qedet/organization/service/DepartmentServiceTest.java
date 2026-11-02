package q.qedet.organization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.NotFoundException;
import q.qedet.organization.dto.DepartmentRequest;
import q.qedet.organization.dto.DepartmentResponse;
import q.qedet.organization.entity.Department;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.policy.entity.WorkSchedule;
import q.qedet.organization.policy.repository.WorkScheduleRepository;
import q.qedet.organization.repository.DepartmentRepository;
import q.qedet.organization.repository.OrganizationRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock private DepartmentRepository departments;
    @Mock private OrganizationRepository organizations;
    @Mock private WorkScheduleRepository schedules;
    @Mock private ActorContext actor;
    @InjectMocks private DepartmentService service;

    private UUID organizationId;
    private Organization organization;
    private WorkSchedule schedule;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        organization = new Organization();
        organization.setId(organizationId);
        schedule = new WorkSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setOrganization(organization);
        schedule.setName("Weekdays");
        when(actor.organizationId()).thenReturn(organizationId);
        when(departments.existsByOrganizationIdAndNameIgnoreCase(organizationId, "Operations")).thenReturn(false);
        when(departments.findByOrganizationIdAndCodeIgnoreCase(organizationId, "ops")).thenReturn(Optional.empty());
        when(organizations.findById(organizationId)).thenReturn(Optional.of(organization));
    }

    @Test
    void createsDepartmentWithOrganizationOwnedSchedule() {
        when(schedules.findByIdAndOrganizationId(schedule.getId(), organizationId)).thenReturn(Optional.of(schedule));
        when(departments.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentResponse response = service.create(
                new DepartmentRequest("Operations", null, "ops", null, schedule.getId()));

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        org.mockito.Mockito.verify(departments).save(captor.capture());
        assertThat(captor.getValue().getSchedule()).isEqualTo(schedule);
        assertThat(response.scheduleId()).isEqualTo(schedule.getId());
        assertThat(response.scheduleName()).isEqualTo("Weekdays");
    }

    @Test
    void rejectsScheduleFromAnotherOrganization() {
        UUID otherOrganizationScheduleId = UUID.randomUUID();
        when(schedules.findByIdAndOrganizationId(otherOrganizationScheduleId, organizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                new DepartmentRequest("Operations", null, "ops", null, otherOrganizationScheduleId)))
                .isInstanceOf(NotFoundException.class);
    }
}
