package q.qedet.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import q.qedet.auth.entity.Invitation;
import q.qedet.employee.dto.InviteRequest;

import q.qedet.auth.repository.InvitationRepository;
import q.qedet.auth.security.ActorContext;
import q.qedet.auth.security.TokenHasher;

import q.qedet.common.model.EmployeeStatus;
import q.qedet.common.model.InvitationStatus;
import q.qedet.sales.service.PosAssignmentProvisioningService;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.ForbiddenException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.email.EmailService;
import q.qedet.hr.common.event.EmployeeLifecycleEventPublisher;
import q.qedet.hr.common.model.EmployeeLifecycleEventType;
import q.qedet.common.model.RoleType;
import q.qedet.finance.service.CashBoxProvisioningService;



import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static q.qedet.common.model.Permission.EMPLOYEE_INVITE;

@Slf4j


@Service
public class InvitationService {

    private static final Duration INVITATION_TTL = Duration.ofDays(7);

    private final InvitationRepository invitationRepository;
    private final EmployeeRepository employeeRepository;
    private final ActorContext actorContext;
    private final EmailService emailService;
    private final EmployeeLifecycleEventPublisher lifecycleEvents;
    private final CashBoxProvisioningService cashBoxProvisioningService;
    private final PosAssignmentProvisioningService posAssignmentProvisioningService;

    public InvitationService(InvitationRepository invitationRepository,
                             EmployeeRepository employeeRepository,
                             ActorContext actorContext,
                             EmailService emailService,
                             EmployeeLifecycleEventPublisher lifecycleEvents ,
                             CashBoxProvisioningService cashBoxProvisioningService
                             ,PosAssignmentProvisioningService posAssignmentProvisioningService) {
        this.invitationRepository = invitationRepository;
        this.employeeRepository = employeeRepository;
        this.actorContext = actorContext;
        this.emailService = emailService;
        this.lifecycleEvents = lifecycleEvents;
        this.cashBoxProvisioningService = cashBoxProvisioningService;
        this.posAssignmentProvisioningService  = posAssignmentProvisioningService;
    }

    @Transactional
    public String createInvitation(UUID employeeId, InviteRequest request) {
        actorContext.requirePermission(EMPLOYEE_INVITE);
        Employee employee = employeeRepository.findByIdAndOrganizationId(employeeId, actorContext.organizationId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        if (request != null && request.email() != null && !request.email().isBlank()) {
            employee.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        }
        if (employee.getEmail() == null || employee.getEmail().isBlank()) {
            throw new BadRequestException("Employee email is required for invitation");
        }
        String email = employee.getEmail().trim().toLowerCase(Locale.ROOT);
        employee.setEmail(email);
        Instant now = Instant.now();
        String rawToken = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        Invitation invitation = invitationRepository
                .findByOrganizationIdAndTargetEmailIgnoreCaseAndStatus(
                        employee.getOrganization().getId(), email, InvitationStatus.PENDING)
                .orElseGet(() -> new Invitation(
                        employee.getOrganization(),
                        employee,
                        TokenHasher.sha256(rawToken),
                        email,
                        now.plus(INVITATION_TTL)
                ));
        invitation.setTargetEmail(email);
        invitation.setTokenHash(TokenHasher.sha256(rawToken));
        invitation.setExpiresAt(now.plus(INVITATION_TTL));
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setUsedAt(null);
        employee.setStatus(EmployeeStatus.INVITED);
        employee.setInvitedAt(now);
        invitationRepository.save(invitation);
        employeeRepository.save(employee);
        emailService.sendInvitationEmail(employee.getEmail(), rawToken);

        return rawToken;
    }


    @Transactional(noRollbackFor = ForbiddenException.class)
    public void accept(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Invitation token is required");
        }

        Invitation invitation = invitationRepository
                .findByTokenHash(TokenHasher.sha256(token))
                .orElseThrow(() -> new NotFoundException("Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ForbiddenException("Invitation is no longer valid");
        }

        if (!invitation.getExpiresAt().isAfter(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new ForbiddenException("Invitation expired");
        }

        Employee employee = invitation.getEmployee();
        boolean wasActive = employee.getStatus() == EmployeeStatus.ACTIVE;
        Instant acceptedAt = Instant.now();

        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setActivatedAt(acceptedAt);
        employee.setJoinedAt(
                employee.getJoinedAt() == null
                        ? acceptedAt
                        : employee.getJoinedAt()
        );

        employeeRepository.save(employee);

        // Create a wallet only for cashiers.
        if (employee.getRole() == RoleType.CASHIER) {
            cashBoxProvisioningService.ensureCashierCashBox(employee);
            posAssignmentProvisioningService.ensureCashierPos(employee);
        }

        if (!wasActive) {
            lifecycleEvents.publish(
                    employee,
                    EmployeeLifecycleEventType.EMPLOYEE_ACTIVATED
            );
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setUsedAt(acceptedAt);
        invitationRepository.save(invitation);

        log.info(
                "Invitation accepted: employeeId={}, status={}, invitationStatus={}",
                employee.getId(),
                employee.getStatus(),
                invitation.getStatus()
        );
    }

}
