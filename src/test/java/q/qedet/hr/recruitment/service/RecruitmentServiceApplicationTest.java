package q.qedet.hr.recruitment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.security.ActorContext;
import q.qedet.auth.entity.UserAccount;
import q.qedet.auth.repository.UserAccountRepository;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.model.EmployeeStatus;
import q.qedet.employee.dto.EmployeeInvitationRequest;
import q.qedet.hr.common.event.DomainEvent;
import q.qedet.hr.common.event.DomainEventPublisher;
import q.qedet.hr.recruitment.dto.ApplicationResponse;
import q.qedet.hr.recruitment.dto.EmployeeApplicationRequest;
import q.qedet.hr.recruitment.dto.JobOpeningResponse;
import q.qedet.hr.recruitment.entity.Application;
import q.qedet.hr.recruitment.entity.Candidate;
import q.qedet.hr.recruitment.entity.JobOpening;
import q.qedet.hr.recruitment.model.ApplicationStatus;
import q.qedet.hr.recruitment.model.JobOpeningStatus;
import q.qedet.hr.recruitment.model.RecruitmentEventType;
import q.qedet.hr.recruitment.repository.ApplicationRepository;
import q.qedet.hr.recruitment.repository.CandidateRepository;
import q.qedet.hr.recruitment.repository.JobOpeningRepository;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceApplicationTest {
    @Mock JobOpeningRepository jobs;
    @Mock CandidateRepository candidates;
    @Mock ApplicationRepository applications;
    @Mock EmployeeRepository employees;
    @Mock UserAccountRepository users;
    @Mock OrganizationRepository organizations;
    @Mock ActorContext actor;
    @Mock DomainEventPublisher publisher;
    @InjectMocks RecruitmentService service;

    @Test
    void employeeFromAnotherOrganizationCanApplyAndJobOwnsApplication() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Employee employee = employee(employeeId, UUID.randomUUID(), EmployeeStatus.ACTIVE);
        UserAccount user = user(userId);
        JobOpening job = job(UUID.randomUUID(), UUID.randomUUID(), JobOpeningStatus.OPEN, LocalDate.now().plusDays(1));
        when(actor.employeeId()).thenReturn(employeeId);
        when(actor.userId()).thenReturn(userId);
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        Candidate candidate = candidate(user);
        when(candidates.findByUserId(userId)).thenReturn(Optional.of(candidate));
        saveApplication();

        ApplicationResponse response = service.applyAsEmployee(job.getId(), new EmployeeApplicationRequest("resume-key", "Interested"));

        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applications).save(applicationCaptor.capture());
        Application saved = applicationCaptor.getValue();
        assertEquals(candidate, saved.getCandidate());
        assertEquals(job, saved.getJobOpening());
        assertEquals(job.getOrganization(), saved.getOrganization());
        assertEquals("resume-key", saved.getResumeKey());
        assertEquals("Interested", saved.getNotes());
        assertEquals(ApplicationStatus.APPLIED, response.status());
        assertEquals(job.getOrganization().getId(), response.organizationId());

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        assertEquals(RecruitmentEventType.CANDIDATE_APPLIED.name(), eventCaptor.getValue().eventType());
        assertEquals(job.getOrganization().getId(), eventCaptor.getValue().organizationId());
        assertEquals(employeeId, eventCaptor.getValue().employeeId());
    }

    @Test
    void employeeCannotApplyToDraftOrClosedJob() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(actor.employeeId()).thenReturn(employeeId);
        when(actor.userId()).thenReturn(userId);
        Employee employee = employee(employeeId, UUID.randomUUID(), EmployeeStatus.ACTIVE);
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(users.findById(userId)).thenReturn(Optional.of(user(userId)));

        JobOpening draft = job(UUID.randomUUID(), UUID.randomUUID(), JobOpeningStatus.DRAFT, LocalDate.now().plusDays(1));
        when(jobs.findById(draft.getId())).thenReturn(Optional.of(draft));
        assertThrows(ConflictException.class, () -> service.applyAsEmployee(draft.getId(), new EmployeeApplicationRequest(null, null)));

        JobOpening closed = job(UUID.randomUUID(), UUID.randomUUID(), JobOpeningStatus.CLOSED, LocalDate.now().plusDays(1));
        when(jobs.findById(closed.getId())).thenReturn(Optional.of(closed));
        assertThrows(ConflictException.class, () -> service.applyAsEmployee(closed.getId(), new EmployeeApplicationRequest(null, null)));
    }

    @Test
    void missingOrInactiveEmployeeCannotApply() {
        UUID missingEmployeeId = UUID.randomUUID();
        when(actor.employeeId()).thenReturn(missingEmployeeId);
        when(employees.findById(missingEmployeeId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.applyAsEmployee(UUID.randomUUID(), new EmployeeApplicationRequest(null, null)));

        UUID inactiveEmployeeId = UUID.randomUUID();
        when(actor.employeeId()).thenReturn(inactiveEmployeeId);
        when(employees.findById(inactiveEmployeeId)).thenReturn(Optional.of(
                employee(inactiveEmployeeId, UUID.randomUUID(), EmployeeStatus.INACTIVE)));

        assertThrows(ConflictException.class, () ->
                service.applyAsEmployee(UUID.randomUUID(), new EmployeeApplicationRequest(null, null)));

        verifyNoInteractions(jobs, candidates, applications, users, publisher);
    }

    @Test
    void employeeCannotApplyAfterDeadlineOrMoreThanOnce() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(actor.employeeId()).thenReturn(employeeId);
        when(actor.userId()).thenReturn(userId);
        Employee employee = employee(employeeId, UUID.randomUUID(), EmployeeStatus.ACTIVE);
        UserAccount user = user(userId);
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(candidates.findByUserId(userId)).thenReturn(Optional.of(candidate(user)));

        JobOpening expired = job(UUID.randomUUID(), UUID.randomUUID(), JobOpeningStatus.OPEN, LocalDate.now().minusDays(1));
        when(jobs.findById(expired.getId())).thenReturn(Optional.of(expired));
        assertThrows(ConflictException.class, () -> service.applyAsEmployee(expired.getId(), new EmployeeApplicationRequest(null, null)));

        JobOpening applied = job(UUID.randomUUID(), UUID.randomUUID(), JobOpeningStatus.OPEN, LocalDate.now().plusDays(1));
        when(jobs.findById(applied.getId())).thenReturn(Optional.of(applied));
        when(applications.existsByCandidateIdAndJobOpeningId(any(), org.mockito.ArgumentMatchers.eq(applied.getId()))).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.applyAsEmployee(applied.getId(), new EmployeeApplicationRequest(null, null)));
    }

    @Test
    void employeeCanApplyToMultipleDifferentJobs() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(actor.employeeId()).thenReturn(employeeId);
        when(actor.userId()).thenReturn(userId);
        Employee employee = employee(employeeId, UUID.randomUUID(), EmployeeStatus.ACTIVE);
        UserAccount user = user(userId);
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        JobOpening first = job(UUID.randomUUID(), UUID.randomUUID(), JobOpeningStatus.OPEN, LocalDate.now().plusDays(1));
        JobOpening second = job(UUID.randomUUID(), UUID.randomUUID(), JobOpeningStatus.OPEN, LocalDate.now().plusDays(1));
        when(jobs.findById(first.getId())).thenReturn(Optional.of(first));
        when(jobs.findById(second.getId())).thenReturn(Optional.of(second));
        Candidate candidate = candidate(user);
        when(candidates.findByUserId(userId))
                .thenReturn(Optional.empty(), Optional.of(candidate));
        when(candidates.save(any(Candidate.class))).thenReturn(candidate);
        saveApplication();

        service.applyAsEmployee(first.getId(), new EmployeeApplicationRequest(null, null));
        service.applyAsEmployee(second.getId(), new EmployeeApplicationRequest(null, null));

        verify(candidates, times(1)).save(any(Candidate.class));
        verify(applications, times(2)).save(any(Application.class));
        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applications, times(2)).save(applicationCaptor.capture());
        assertEquals(candidate, applicationCaptor.getAllValues().get(0).getCandidate());
        assertEquals(candidate, applicationCaptor.getAllValues().get(1).getCandidate());
    }

    @Test
    void employeeFirstApplicationCreatesReusableCandidateProfile() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Employee employee = employee(employeeId, UUID.randomUUID(), EmployeeStatus.ACTIVE);
        UserAccount user = user(userId);
        JobOpening job = job(UUID.randomUUID(), UUID.randomUUID(), JobOpeningStatus.OPEN, LocalDate.now().plusDays(1));
        when(actor.employeeId()).thenReturn(employeeId);
        when(actor.userId()).thenReturn(userId);
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        when(candidates.findByUserId(userId)).thenReturn(Optional.empty());
        when(candidates.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate candidate = invocation.getArgument(0);
            candidate.setId(UUID.randomUUID());
            return candidate;
        });
        saveApplication();

        service.applyAsEmployee(job.getId(), new EmployeeApplicationRequest("resume-key", "Interested"));

        ArgumentCaptor<Candidate> candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidates).save(candidateCaptor.capture());
        Candidate created = candidateCaptor.getValue();
        assertEquals(user, created.getUser());
        assertEquals(user.getEmail(), created.getEmail());
    }

    @Test
    void externalUserCreatesAndReusesOwnCandidateProfile() {
        UUID userId = UUID.randomUUID();
        UserAccount user = user(userId);
        JobOpening first = job(UUID.randomUUID(), UUID.randomUUID(), JobOpeningStatus.OPEN, LocalDate.now().plusDays(1));
        JobOpening second = job(UUID.randomUUID(), UUID.randomUUID(), JobOpeningStatus.OPEN, LocalDate.now().plusDays(1));
        Candidate candidate = candidate(user);
        when(actor.userId()).thenReturn(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(jobs.findById(first.getId())).thenReturn(Optional.of(first));
        when(jobs.findById(second.getId())).thenReturn(Optional.of(second));
        when(candidates.findByUserId(userId)).thenReturn(Optional.empty(), Optional.of(candidate));
        when(candidates.save(any(Candidate.class))).thenReturn(candidate);
        saveApplication();

        service.applyAsCandidate(first.getId(), new EmployeeApplicationRequest("resume-one", "First"));
        service.applyAsCandidate(second.getId(), new EmployeeApplicationRequest("resume-two", "Second"));

        verify(candidates, times(1)).save(any(Candidate.class));
        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applications, times(2)).save(applicationCaptor.capture());
        assertEquals(candidate, applicationCaptor.getAllValues().get(0).getCandidate());
        assertEquals(candidate, applicationCaptor.getAllValues().get(1).getCandidate());
    }

    @Test
    void hrCandidateApplicationRemainsCandidateCentric() {
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        JobOpening job = job(UUID.randomUUID(), organizationId, JobOpeningStatus.OPEN, LocalDate.now().plusDays(1));
        when(actor.organizationId()).thenReturn(organizationId);
        when(actor.userId()).thenReturn(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(jobs.findByIdAndOrganizationId(job.getId(), organizationId)).thenReturn(Optional.of(job));
        when(candidates.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate candidate = invocation.getArgument(0);
            candidate.setId(UUID.randomUUID());
            return candidate;
        });
        saveApplication();

        Application saved = service.apply(job.getId(), "Ada", "Lovelace", "ada@example.com", "candidate-resume");

        assertEquals("Ada", saved.getCandidate().getFirstName());
        assertEquals(job.getOrganization(), saved.getOrganization());
        assertEquals("candidate-resume", saved.getResumeKey());
    }

    @Test
    void publishReturnsJobOpeningResponseAndTransitionsDraftToOpen() {
        UUID organizationId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        JobOpening job = job(jobId, organizationId, JobOpeningStatus.DRAFT, LocalDate.now().plusDays(1));
        when(actor.organizationId()).thenReturn(organizationId);
        when(jobs.findByIdAndOrganizationId(jobId, organizationId)).thenReturn(Optional.of(job));
        when(jobs.save(job)).thenReturn(job);

        JobOpeningResponse response = service.publishJob(jobId);

        assertEquals(JobOpeningStatus.OPEN, job.getStatus());
        assertEquals(JobOpeningStatus.OPEN, response.status());
    }

    @Test
    void hrCanShortlistAndRejectAnEmployeeApplication() {
        UUID organizationId = UUID.randomUUID();
        JobOpening job = job(UUID.randomUUID(), organizationId, JobOpeningStatus.OPEN, LocalDate.now().plusDays(1));
        Application application = new Application();
        application.setId(UUID.randomUUID());
        application.setOrganization(job.getOrganization());
        application.setJobOpening(job);
        application.setCandidate(candidate(user(UUID.randomUUID())));
        when(actor.organizationId()).thenReturn(organizationId);
        when(applications.findByIdAndOrganizationId(application.getId(), organizationId)).thenReturn(Optional.of(application));
        when(applications.save(application)).thenReturn(application);

        service.shortlist(application.getId());
        assertEquals(ApplicationStatus.SHORTLISTED, application.getStatus());

        service.reject(application.getId());
        assertEquals(ApplicationStatus.REJECTED, application.getStatus());
    }

    @Test
    void employeeInvitationUsesAcceptedApplicationCandidateAndJobAssignments() {
        UUID organizationId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        Candidate candidate = candidate(user(UUID.randomUUID()));
        JobOpening job = job(UUID.randomUUID(), organizationId, JobOpeningStatus.OPEN, LocalDate.now().plusDays(1));
        UUID departmentId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        job.setDepartmentId(departmentId);
        job.setPositionId(positionId);
        job.setLocationId(locationId);
        Application application = new Application();
        application.setStatus(ApplicationStatus.OFFER_ACCEPTED);
        application.setCandidate(candidate);
        application.setJobOpening(job);
        when(actor.organizationId()).thenReturn(organizationId);
        when(applications.findByIdAndOrganizationId(applicationId, organizationId)).thenReturn(Optional.of(application));

        EmployeeInvitationRequest request = service.employeeInvitation(applicationId);

        assertEquals(candidate.getEmail(), request.email());
        assertEquals(candidate.getFirstName(), request.firstName());
        assertEquals(candidate.getLastName(), request.lastName());
        assertEquals(positionId, request.positionId());
        assertEquals(locationId, request.locationId());
    }

    @Test
    void employeeInvitationRejectsUnacceptedOffersAndJobsWithoutAssignments() {
        UUID organizationId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        Application application = new Application();
        application.setStatus(ApplicationStatus.OFFER_SENT);
        application.setJobOpening(job(UUID.randomUUID(), organizationId, JobOpeningStatus.OPEN, LocalDate.now().plusDays(1)));
        when(actor.organizationId()).thenReturn(organizationId);
        when(applications.findByIdAndOrganizationId(applicationId, organizationId)).thenReturn(Optional.of(application));

        assertThrows(BadRequestException.class, () -> service.employeeInvitation(applicationId));

        application.setStatus(ApplicationStatus.OFFER_ACCEPTED);
        assertThrows(BadRequestException.class, () -> service.employeeInvitation(applicationId));
    }

    @Test
    void listInterviewersReturnsOnlyActiveEmployees() {
        UUID organizationId = UUID.randomUUID();
        Employee active = employee(UUID.randomUUID(), organizationId, EmployeeStatus.ACTIVE);
        Employee inactive = employee(UUID.randomUUID(), organizationId, EmployeeStatus.INACTIVE);
        when(actor.organizationId()).thenReturn(organizationId);
        when(employees.findAllByOrganizationId(organizationId)).thenReturn(java.util.List.of(active, inactive));

        assertEquals(java.util.List.of(active.getId()), service.listInterviewers().stream()
                .map(response -> response.id()).toList());
    }

    private void saveApplication() {
        when(applications.save(any(Application.class))).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            application.setId(UUID.randomUUID());
            return application;
        });
    }

    private Employee employee(UUID id, UUID organizationId, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setOrganization(organization(organizationId));
        employee.setStatus(status);
        employee.setFirstName("Ada");
        employee.setLastName("Lovelace");
        employee.setEmail("ada@example.com");
        return employee;
    }

    private UserAccount user(UUID id) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setEmail("ada@example.com");
        user.setRegistrationName("Ada Lovelace");
        user.setRegistrationPhone("+15555550123");
        return user;
    }

    private Candidate candidate(UserAccount user) {
        Candidate candidate = new Candidate();
        candidate.setId(UUID.randomUUID());
        candidate.setUser(user);
        candidate.setFirstName("Ada");
        candidate.setLastName("Lovelace");
        candidate.setEmail(user.getEmail());
        return candidate;
    }

    private JobOpening job(UUID id, UUID organizationId, JobOpeningStatus status, LocalDate deadline) {
        JobOpening job = new JobOpening();
        job.setId(id);
        job.setOrganization(organization(organizationId));
        job.setStatus(status);
        job.setApplicationDeadline(deadline);
        return job;
    }

    private Organization organization(UUID id) {
        Organization organization = new Organization();
        organization.setId(id);
        return organization;
    }
}
