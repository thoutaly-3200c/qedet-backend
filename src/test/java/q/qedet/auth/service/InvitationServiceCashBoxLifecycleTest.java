package q.qedet.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.entity.Invitation;
import q.qedet.auth.entity.UserAccount;
import q.qedet.auth.repository.InvitationRepository;
import q.qedet.auth.security.ActorContext;
import q.qedet.auth.security.TokenHasher;
import q.qedet.common.model.BusinessType;
import q.qedet.common.model.EmployeeStatus;
import q.qedet.common.model.InvitationStatus;
import q.qedet.common.model.RoleType;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.email.EmailService;
import q.qedet.finance.service.CashBoxProvisioningService;
import q.qedet.hr.common.event.EmployeeLifecycleEventPublisher;
import q.qedet.hr.common.model.EmployeeLifecycleEventType;
import q.qedet.organization.entity.Organization;
import q.qedet.sales.service.PosAssignmentProvisioningService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceCashBoxLifecycleTest {
    @Mock private InvitationRepository invitations;
    @Mock private EmployeeRepository employees;
    @Mock private ActorContext actor;
    @Mock private EmailService email;
    @Mock private EmployeeLifecycleEventPublisher lifecycleEvents;
    @Mock private CashBoxProvisioningService cashBoxProvisioningService;
    @Mock private PosAssignmentProvisioningService posAssignmentProvisioningService;

    @Test
    void acceptingInvitationActivatesEmployeeAndPreservesExistingAccount() {
        Organization organization = new Organization("Invitation Test", BusinessType.GENERAL_TRADE);
        organization.setId(UUID.randomUUID());
        Employee employee = new Employee(organization, "EMP-00001", "New", "Finance",
                "finance@example.com", null, RoleType.FINANCE_EMPLOYEE);
        employee.setId(UUID.randomUUID());
        employee.setStatus(EmployeeStatus.INVITED);
        UserAccount user = new UserAccount("finance@example.com", "hash");
        user.setId(UUID.randomUUID());
        employee.setUser(user);
        Invitation invitation = new Invitation(organization, employee, TokenHasher.sha256("token"),
                user.getEmail(), Instant.now().plusSeconds(600));
        invitation.setStatus(InvitationStatus.PENDING);

        when(invitations.findByTokenHash(TokenHasher.sha256("token"))).thenReturn(Optional.of(invitation));
        when(employees.save(employee)).thenReturn(employee);

        InvitationService service = new InvitationService(invitations, employees, actor, email, lifecycleEvents, cashBoxProvisioningService, posAssignmentProvisioningService);
        service.accept("token");

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(employee.getUser()).isSameAs(user);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(employee.getActivatedAt()).isNotNull();
        assertThat(employee.getJoinedAt()).isNotNull();
        assertThat(invitation.getUsedAt()).isNotNull();
        verify(lifecycleEvents).publish(employee, EmployeeLifecycleEventType.EMPLOYEE_ACTIVATED);
        verifyNoInteractions(cashBoxProvisioningService, posAssignmentProvisioningService);
    }

    @Test
    void acceptingUnknownInvitationRejectsIt() {
        when(invitations.findByTokenHash(TokenHasher.sha256("token"))).thenReturn(Optional.empty());

        InvitationService service = new InvitationService(invitations, employees, actor, email, lifecycleEvents, cashBoxProvisioningService, posAssignmentProvisioningService);

        assertThatThrownBy(() -> service.accept("token"))
                .isInstanceOf(q.qedet.common.exception.NotFoundException.class);
        verifyNoInteractions(employees, lifecycleEvents, cashBoxProvisioningService, posAssignmentProvisioningService);
    }

    @Test
    void acceptingExpiredInvitationMarksItExpiredAndRejectsIt() {
        Organization organization = new Organization("Invitation Test", BusinessType.GENERAL_TRADE);
        Employee employee = new Employee(organization, "EMP-00001", "New", "Finance",
                "finance@example.com", null, RoleType.FINANCE_EMPLOYEE);
        Invitation invitation = new Invitation(organization, employee, TokenHasher.sha256("token"),
                "finance@example.com", Instant.now().minusSeconds(1));

        when(invitations.findByTokenHash(TokenHasher.sha256("token"))).thenReturn(Optional.of(invitation));

        InvitationService service = new InvitationService(invitations, employees, actor, email, lifecycleEvents, cashBoxProvisioningService, posAssignmentProvisioningService);

        assertThatThrownBy(() -> service.accept("token"))
                .isInstanceOf(q.qedet.common.exception.ForbiddenException.class);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        verify(invitations).save(invitation);
    }

    @Test
    void acceptingAlreadyUsedInvitationRejectsIt() {
        Organization organization = new Organization("Invitation Test", BusinessType.GENERAL_TRADE);
        Employee employee = new Employee(organization, "EMP-00001", "New", "Finance",
                "finance@example.com", null, RoleType.FINANCE_EMPLOYEE);
        Invitation invitation = new Invitation(organization, employee, TokenHasher.sha256("token"),
                "finance@example.com", Instant.now().plusSeconds(600));
        invitation.setStatus(InvitationStatus.ACCEPTED);

        when(invitations.findByTokenHash(TokenHasher.sha256("token"))).thenReturn(Optional.of(invitation));

        InvitationService service = new InvitationService(invitations, employees, actor, email, lifecycleEvents, cashBoxProvisioningService, posAssignmentProvisioningService);

        assertThatThrownBy(() -> service.accept("token"))
                .isInstanceOf(q.qedet.common.exception.ForbiddenException.class);
        verifyNoInteractions(employees, lifecycleEvents, cashBoxProvisioningService, posAssignmentProvisioningService);
    }
}

