package q.qedet.hr.recruitment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.entity.UserAccount;
import q.qedet.auth.repository.UserAccountRepository;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.common.model.EmployeeStatus;
import q.qedet.common.response.ResponseMapper;
import q.qedet.employee.dto.EmployeeInvitationRequest;
import q.qedet.employee.dto.EmployeeResponse;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.hr.common.event.*;
import q.qedet.hr.recruitment.entity.*;
import q.qedet.hr.recruitment.dto.*;
import q.qedet.hr.recruitment.model.*;
import q.qedet.hr.recruitment.repository.*;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class RecruitmentService {
    private final JobOpeningRepository jobs;
    private final CandidateRepository candidates;
    private final ApplicationRepository applications;
    private final CandidateCvRepository candidateCvs;
    private final InterviewRepository interviews;
    private final OfferRepository offers;
    private final EmployeeRepository employees;
    private final UserAccountRepository users;
    private final OrganizationRepository organizations;
    private final ActorContext actor;
    private final DomainEventPublisher publisher;

    public RecruitmentService(JobOpeningRepository jobs, CandidateRepository candidates, ApplicationRepository applications,
                              CandidateCvRepository candidateCvs, InterviewRepository interviews, OfferRepository offers,
                              EmployeeRepository employees,
                              UserAccountRepository users,
                              OrganizationRepository organizations, ActorContext actor, DomainEventPublisher publisher) {
        this.jobs = jobs; this.candidates = candidates; this.applications = applications;
        this.candidateCvs = candidateCvs; this.interviews = interviews; this.offers = offers;
        this.employees = employees; this.users = users; this.organizations = organizations;
        this.actor = actor; this.publisher = publisher;
    }

    @Transactional
    public JobOpeningResponse createJob(CreateJobRequest request) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Organization organization = organizations.findById(actor.organizationId()).orElseThrow(() -> new NotFoundException("Organization not found"));
        JobOpening job = new JobOpening();
        job.setOrganization(organization);
        job.setTitle(request.title().trim());
        job.setDepartmentId(request.departmentId());
        job.setPositionId(request.positionId());
        job.setLocationId(request.locationId());
        job.setEmploymentType(request.employmentType());
        job.setDescription(request.description());
        job.setRequirements(request.requirements() == null ? new ArrayList<>() : new ArrayList<>(request.requirements()));
        job.setResponsibilities(request.responsibilities() == null ? new ArrayList<>() : new ArrayList<>(request.responsibilities()));
        job.setVacancies(request.vacancies());
        job.setApplicationDeadline(request.applicationDeadline());
        return JobOpeningResponse.from(jobs.save(job));
    }

    @Transactional(readOnly = true)
    public List<JobOpeningResponse> listJobs() {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        return jobs.findAllWithRequirementsByOrganizationId(actor.organizationId())
                .stream()
                .map(JobOpeningResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobOpeningResponse getJob(UUID id) {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        return JobOpeningResponse.from(jobs.findWithRequirementsByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Job opening not found")));
    }

    @Transactional(readOnly = true)
    public JobOpeningResponse getPublicJob(UUID id) {
        JobOpening job = jobs.findWithRequirementsById(id)
                .orElseThrow(() -> new NotFoundException("Job opening not found"));
        if (job.getStatus() != JobOpeningStatus.OPEN) {
            throw new NotFoundException("Job opening not found");
        }
        return JobOpeningResponse.from(job);
    }
    @Transactional
    public JobOpeningResponse publishJob(UUID id) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        JobOpening job = ownedJob(id);
        if (job.getStatus() != JobOpeningStatus.DRAFT) throw new ConflictException("Job opening is not a draft");
        job.setStatus(JobOpeningStatus.OPEN); JobOpening saved = jobs.save(job);
        publish(saved.getOrganization().getId(), null, saved.getId(), RecruitmentEventType.JOB_OPENED);
        return JobOpeningResponse.from(saved);
    }

    @Transactional
    public Application apply(UUID jobId, String firstName, String lastName, String email, String resumeKey) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        JobOpening job = ownedJob(jobId);
        if (job.getStatus() != JobOpeningStatus.OPEN) throw new ConflictException("Job opening is not open");
        Organization organization = job.getOrganization();
        Candidate savedCandidate = resolveCandidate(authenticatedUser(), firstName, lastName, null);
        if (applications.existsByCandidateIdAndJobOpeningId(savedCandidate.getId(), jobId)) {
            throw new ConflictException("You have already applied to this job");
        }
        Application saved = createApplication(job, savedCandidate, resumeKey, null);
        publish(organization.getId(), null, saved.getId(), RecruitmentEventType.CANDIDATE_APPLIED);
        return saved;
    }

    @Transactional
    public ApplicationResponse applyAsCandidate(UUID jobId, EmployeeApplicationRequest request) {
        UserAccount user = authenticatedUser();
        JobOpening job = availableJob(jobId);
        Candidate candidate = resolveCandidate(user, null, null, null);
        if (applications.existsByCandidateIdAndJobOpeningId(candidate.getId(), jobId)) {
            throw new ConflictException("You have already applied to this job");
        }

        Application saved = createApplication(job, candidate, request.resumeKey(), request.notes());
        publish(job.getOrganization().getId(), null, saved.getId(), RecruitmentEventType.CANDIDATE_APPLIED);
        return ApplicationResponse.from(saved);
    }

    @Transactional
    public ApplicationResponse applyAsEmployee(UUID jobId, EmployeeApplicationRequest request) {
        UUID employeeId = actor.employeeId();
        Employee employee = employeeId == null
                ? null
                : employees.findById(employeeId).orElse(null);
        if (employee == null) {
            throw new NotFoundException("Employee not found");
        }
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new ConflictException("Employee is not active");
        }
        UserAccount user = authenticatedUser();

        JobOpening job = availableJob(jobId);
        Candidate candidate = resolveCandidate(user, employee.getFirstName(), employee.getLastName(),
                employee.getPhone());
        if (applications.existsByCandidateIdAndJobOpeningId(candidate.getId(), jobId)) {
            throw new ConflictException("You have already applied to this job");
        }

        Application saved = createApplication(job, candidate, request.resumeKey(), request.notes());
        publish(job.getOrganization().getId(), employee.getId(), saved.getId(), RecruitmentEventType.CANDIDATE_APPLIED);
        return ApplicationResponse.from(saved);
    }

    private UserAccount authenticatedUser() {
        UUID userId = actor.userId();
        if (userId == null) {
            throw new NotFoundException("User not found");
        }
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private JobOpening availableJob(UUID jobId) {
        JobOpening job = jobs.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job opening not found"));
        if (job.getStatus() != JobOpeningStatus.OPEN) {
            throw new ConflictException("Job opening is not available for applications");
        }
        if (job.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new ConflictException("Applications for this job are closed");
        }
        return job;
    }

    private Candidate resolveCandidate(UserAccount user, String firstName, String lastName, String phone) {
        return candidates.findByUserId(user.getId()).orElseGet(() -> {
            Candidate candidate = new Candidate();
            candidate.setUser(user);
            candidate.setFirstName(profileName(firstName, user.getRegistrationName(), true));
            candidate.setLastName(profileName(lastName, user.getRegistrationName(), false));
            candidate.setEmail(user.getEmail().trim().toLowerCase(Locale.ROOT));
            candidate.setPhone(phone == null || phone.isBlank() ? user.getRegistrationPhone() : phone);
            return candidates.save(candidate);
        });
    }

    private String profileName(String value, String registrationName, boolean firstName) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        if (registrationName == null || registrationName.isBlank()) {
            return firstName ? "Candidate" : "Profile";
        }
        String[] parts = registrationName.trim().split("\\s+", 2);
        return firstName || parts.length == 1 ? parts[0] : parts[1];
    }

    private Application createApplication(JobOpening job, Candidate candidate, String resumeKey, String notes) {
        Application application = new Application();
        application.setOrganization(job.getOrganization());
        application.setCandidate(candidate);
        application.setJobOpening(job);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setResumeKey(resumeKey);
        application.setNotes(notes);
        return applications.save(application);
    }

    @Transactional
    public Application shortlist(UUID id) { return transition(id, ApplicationStatus.SHORTLISTED, RecruitmentEventType.CANDIDATE_SHORTLISTED); }
    @Transactional
    public Application reject(UUID id) { return transition(id, ApplicationStatus.REJECTED, RecruitmentEventType.CANDIDATE_REJECTED); }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listApplications(UUID jobId) {
        return listApplications(jobId, null, null);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listApplications(UUID jobId, UUID candidateId, ApplicationStatus status) {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        List<Application> result = jobId == null
                ? applications.findAllByOrganizationIdOrderByCreatedAtDesc(actor.organizationId())
                : applications.findAllByJobOpeningIdAndOrganizationIdOrderByCreatedAtDesc(jobId, actor.organizationId());
        return result.stream()
                .filter(application -> candidateId == null || application.getCandidate().getId().equals(candidateId))
                .filter(application -> status == null || application.getStatus() == status)
                .map(ApplicationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID id) {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        return ApplicationResponse.from(ownedApplication(id));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listInterviewers() {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        return employees.findAllByOrganizationId(actor.organizationId()).stream()
                .filter(employee -> employee.getStatus() == EmployeeStatus.ACTIVE)
                .map(ResponseMapper::employee)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeInvitationRequest employeeInvitation(UUID applicationId) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Application application = ownedApplication(applicationId);
        if (application.getStatus() != ApplicationStatus.OFFER_ACCEPTED) {
            throw new BadRequestException("Application must have an accepted offer before inviting an employee");
        }
        JobOpening job = application.getJobOpening();
        if (job.getPositionId() == null) {
            throw new BadRequestException("Job opening must have a position assignment");
        }
        Candidate candidate = application.getCandidate();
        return new EmployeeInvitationRequest(
                candidate.getEmail(),
                candidate.getFirstName(),
                candidate.getLastName(),
                job.getPositionId(),
                job.getLocationId());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> myApplications() {
        return applications.findAllByCandidateIdOrderByCreatedAtDesc(currentCandidate().getId())
                .stream().map(ApplicationResponse::from).toList();
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(UUID id, ApplicationStatus status) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Application application = ownedApplication(id);
        validateTransition(application.getStatus(), status);
        application.setStatus(status);
        Application saved = applications.save(application);
        publish(saved.getOrganization().getId(), null, saved.getId(), eventForStatus(status));
        return ApplicationResponse.from(saved);
    }

    @Transactional
    public ApplicationResponse withdrawMyApplication(UUID id) {
        Application application = ownApplication(id);
        validateTransition(application.getStatus(), ApplicationStatus.WITHDRAWN);
        application.setStatus(ApplicationStatus.WITHDRAWN);
        Application saved = applications.save(application);
        publish(saved.getOrganization().getId(), null, saved.getId(), RecruitmentEventType.CANDIDATE_WITHDREW);
        return ApplicationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CandidateResponse> listCandidates() {
        return listCandidates(null, null);
    }

    @Transactional(readOnly = true)
    public List<CandidateResponse> listCandidates(String search, Boolean archived) {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        return applications.findAllByOrganizationIdOrderByCreatedAtDesc(actor.organizationId()).stream()
                .map(Application::getCandidate)
                .collect(java.util.stream.Collectors.toMap(Candidate::getId, candidate -> candidate,
                        (first, ignored) -> first, LinkedHashMap::new))
                .values().stream()
                .filter(candidate -> archived == null || candidate.isArchived() == archived)
                .filter(candidate -> candidateMatches(candidate, search))
                .map(CandidateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CandidateResponse getCandidate(UUID candidateId) {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        return CandidateResponse.from(organizationCandidate(candidateId));
    }

    @Transactional(readOnly = true)
    public CandidateResponse myCandidateProfile() {
        return CandidateResponse.from(currentCandidate());
    }

    @Transactional
    public CandidateResponse createMyCandidateProfile(CandidateProfileRequest request) {
        UserAccount user = authenticatedUser();
        if (candidates.findByUserId(user.getId()).isPresent()) {
            throw new ConflictException("Candidate profile already exists");
        }
        Candidate candidate = new Candidate();
        candidate.setUser(user);
        candidate.setFirstName(request.firstName().trim());
        candidate.setLastName(request.lastName().trim());
        candidate.setEmail(user.getEmail().trim().toLowerCase(Locale.ROOT));
        candidate.setPhone(blankToNull(request.phone()));
        return CandidateResponse.from(candidates.save(candidate));
    }

    @Transactional
    public CandidateResponse updateMyCandidateProfile(CandidateProfileRequest request) {
        Candidate candidate = currentCandidate();
        candidate.setFirstName(request.firstName().trim());
        candidate.setLastName(request.lastName().trim());
        candidate.setPhone(blankToNull(request.phone()));
        return CandidateResponse.from(candidates.save(candidate));
    }

    @Transactional
    public CandidateResponse updateCandidate(UUID candidateId, CandidateProfileRequest request) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Candidate candidate = organizationCandidate(candidateId);
        candidate.setFirstName(request.firstName().trim());
        candidate.setLastName(request.lastName().trim());
        candidate.setPhone(blankToNull(request.phone()));
        return CandidateResponse.from(candidates.save(candidate));
    }

    @Transactional
    public CandidateCvResponse saveMyCv(CandidateCvRequest request) {
        Candidate candidate = currentCandidate();
        CandidateCv cv = candidateCvs.findByCandidateId(candidate.getId()).orElseGet(CandidateCv::new);
        cv.setCandidate(candidate);
        cv.setFileName(request.fileName().trim());
        cv.setStorageKey(request.storageKey().trim());
        cv.setContentType(blankToNull(request.contentType()));
        candidate.setResumeKey(cv.getStorageKey());
        candidates.save(candidate);
        return CandidateCvResponse.from(candidateCvs.save(cv));
    }

    @Transactional
    public CandidateCvResponse uploadMyCv(org.springframework.web.multipart.MultipartFile file) {
        return uploadCv(currentCandidate(), file);
    }

    @Transactional
    public CandidateCvResponse uploadCandidateCv(UUID candidateId, org.springframework.web.multipart.MultipartFile file) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        return uploadCv(organizationCandidate(candidateId), file);
    }

    @Transactional(readOnly = true)
    public CandidateCvResponse myCv() {
        return CandidateCvResponse.from(candidateCvs.findByCandidateId(currentCandidate().getId())
                .orElseThrow(() -> new NotFoundException("Candidate CV not found")));
    }

    @Transactional(readOnly = true)
    public CandidateCvResponse candidateCv(UUID candidateId) {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        Candidate candidate = organizationCandidate(candidateId);
        return CandidateCvResponse.from(candidateCvs.findByCandidateId(candidate.getId())
                .orElseThrow(() -> new NotFoundException("Candidate CV not found")));
    }

    @Transactional
    public void deleteMyCv() {
        Candidate candidate = currentCandidate();
        CandidateCv cv = candidateCvs.findByCandidateId(candidate.getId())
                .orElseThrow(() -> new NotFoundException("Candidate CV not found"));
        candidateCvs.delete(cv);
        candidate.setResumeKey(null);
        candidates.save(candidate);
    }

    @Transactional
    public InterviewResponse scheduleInterview(UUID applicationId, InterviewRequest request) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Application application = ownedApplication(applicationId);
        ensureOpenApplication(application);
        validateInterviewSchedule(request);
        Interview interview = new Interview();
        interview.setOrganization(application.getOrganization());
        interview.setApplication(application);
        applyInterviewRequest(interview, request);
        Interview saved = interviews.save(interview);
        if (application.getStatus() != ApplicationStatus.INTERVIEW) {
            application.setStatus(ApplicationStatus.INTERVIEW);
            applications.save(application);
        }
        publish(saved.getOrganization().getId(), null, saved.getId(), RecruitmentEventType.INTERVIEW_SCHEDULED);
        return InterviewResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> listInterviews(UUID applicationId) {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        ownedApplication(applicationId);
        return interviews.findAllByApplicationIdAndOrganizationIdOrderByScheduledAtAsc(applicationId, actor.organizationId())
                .stream().map(InterviewResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InterviewResponse getInterview(UUID id) {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        return InterviewResponse.from(ownedInterview(id));
    }

    @Transactional
    public InterviewResponse updateInterview(UUID id, InterviewRequest request) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Interview interview = ownedInterview(id);
        ensureInterviewScheduled(interview);
        validateInterviewSchedule(request);
        applyInterviewRequest(interview, request);
        return InterviewResponse.from(interviews.save(interview));
    }

    @Transactional
    public InterviewResponse rescheduleInterview(UUID id, InterviewRequest request) {
        return updateInterview(id, request);
    }

    @Transactional
    public InterviewResponse cancelInterview(UUID id) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Interview interview = ownedInterview(id);
        ensureInterviewScheduled(interview);
        interview.setStatus(InterviewStatus.CANCELLED);
        Interview saved = interviews.save(interview);
        publish(saved.getOrganization().getId(), null, saved.getId(), RecruitmentEventType.INTERVIEW_CANCELLED);
        return InterviewResponse.from(saved);
    }

    @Transactional
    public InterviewResponse recordInterviewResult(UUID id, InterviewResultRequest request) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Interview interview = ownedInterview(id);
        ensureInterviewScheduled(interview);
        interview.setResult(request.result());
        interview.setFeedback(blankToNull(request.feedback()));
        interview.setStatus(InterviewStatus.COMPLETED);
        Interview saved = interviews.save(interview);
        Application application = saved.getApplication();
        application.setStatus(request.result() == InterviewResult.PASS
                ? ApplicationStatus.SELECTED : ApplicationStatus.REJECTED);
        applications.save(application);
        publish(saved.getOrganization().getId(), null, saved.getId(), RecruitmentEventType.INTERVIEW_COMPLETED);
        return InterviewResponse.from(saved);
    }

    @Transactional
    public InterviewResponse updateInterviewStatus(UUID id, InterviewStatusRequest request) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Interview interview = ownedInterview(id);
        ensureInterviewScheduled(interview);
        if (request.status() == InterviewStatus.SCHEDULED) {
            throw new BadRequestException("Interview status must be completed or cancelled");
        }
        interview.setStatus(request.status());
        interview.setFeedback(blankToNull(request.feedback()));
        Interview saved = interviews.save(interview);
        publish(saved.getOrganization().getId(), null, saved.getId(),
                request.status() == InterviewStatus.COMPLETED
                        ? RecruitmentEventType.INTERVIEW_COMPLETED : RecruitmentEventType.INTERVIEW_CANCELLED);
        return InterviewResponse.from(saved);
    }

    @Transactional
    public OfferResponse createOffer(UUID applicationId, OfferRequest request) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Application application = ownedApplication(applicationId);
        ensureOpenApplication(application);
        if (offers.existsByApplicationId(applicationId)) {
            throw new ConflictException("Application already has an offer");
        }
        if (request.expiresOn().isBefore(request.startDate())) {
            throw new BadRequestException("Offer expiration must not be before its start date");
        }
        Offer offer = new Offer();
        offer.setOrganization(application.getOrganization());
        offer.setApplication(application);
        offer.setPositionTitle(request.positionTitle().trim());
        offer.setSalary(request.salary());
        offer.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
        offer.setStartDate(request.startDate());
        offer.setExpiresOn(request.expiresOn());
        offer.setTerms(blankToNull(request.terms()));
        Offer saved = offers.save(offer);
        application.setStatus(ApplicationStatus.OFFER_SENT);
        applications.save(application);
        publish(saved.getOrganization().getId(), null, saved.getId(), RecruitmentEventType.OFFER_SENT);
        return OfferResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> listOffers(UUID applicationId) {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        ownedApplication(applicationId);
        return offers.findAllByApplicationIdAndOrganizationIdOrderByCreatedAtDesc(applicationId, actor.organizationId())
                .stream().map(OfferResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OfferResponse getOffer(UUID id) {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        return OfferResponse.from(ownedOffer(id));
    }

    @Transactional(readOnly = true)
    public OfferResponse getMyOffer(UUID id) {
        UUID candidateId = currentCandidate().getId();

        Offer offer = offers.findByIdAndApplicationCandidateId(id, candidateId)
                .orElseThrow(() -> new NotFoundException("Offer not found"));

        return OfferResponse.from(offer);
    }

    @Transactional(readOnly = true)
    public OfferResponse applicationOffer(UUID applicationId) {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        ownedApplication(applicationId);
        return OfferResponse.from(offers.findByApplicationIdAndOrganizationId(applicationId, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Offer not found")));
    }

    @Transactional
    public OfferResponse updateOffer(UUID id, OfferRequest request) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Offer offer = ownedOffer(id);
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new ConflictException("Only pending offers can be updated");
        }
        if (request.expiresOn().isBefore(request.startDate())) {
            throw new BadRequestException("Offer expiration must not be before its start date");
        }
        offer.setPositionTitle(request.positionTitle().trim());
        offer.setSalary(request.salary());
        offer.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
        offer.setStartDate(request.startDate());
        offer.setExpiresOn(request.expiresOn());
        offer.setTerms(blankToNull(request.terms()));
        return OfferResponse.from(offers.save(offer));
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> myOffers() {
        return offers.findAllByApplicationCandidateIdOrderByCreatedAtDesc(currentCandidate().getId())
                .stream().map(OfferResponse::from).toList();
    }

    @Transactional
    public OfferResponse updateOfferStatus(UUID id, OfferStatus status) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Offer offer = ownedOffer(id);
        if (status != OfferStatus.WITHDRAWN || offer.getStatus() != OfferStatus.PENDING) {
            throw new ConflictException("Only pending offers can be withdrawn");
        }
        offer.setStatus(OfferStatus.WITHDRAWN);
        Offer saved = offers.save(offer);
        publish(saved.getOrganization().getId(), null, saved.getId(), RecruitmentEventType.OFFER_WITHDRAWN);
        return OfferResponse.from(saved);
    }

    @Transactional
    public OfferResponse respondToMyOffer(UUID id, OfferStatus status) {
        if (status != OfferStatus.ACCEPTED && status != OfferStatus.DECLINED) {
            throw new BadRequestException("Candidates may only accept or decline an offer");
        }
        Candidate candidate = currentCandidate();
        Offer offer = offers.findById(id).orElseThrow(() -> new NotFoundException("Offer not found"));
        if (!offer.getApplication().getCandidate().getId().equals(candidate.getId())) {
            throw new ForbiddenException("You do not own this offer");
        }
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new ConflictException("Offer is no longer pending");
        }
        if (offer.getExpiresOn().isBefore(LocalDate.now())) {
            throw new ConflictException("Offer has expired");
        }
        offer.setStatus(status);
        Offer saved = offers.save(offer);
        Application application = offer.getApplication();
        application.setStatus(status == OfferStatus.ACCEPTED
                ? ApplicationStatus.OFFER_ACCEPTED : ApplicationStatus.REJECTED);
        applications.save(application);
        publish(saved.getOrganization().getId(), null, saved.getId(),
                status == OfferStatus.ACCEPTED ? RecruitmentEventType.OFFER_ACCEPTED : RecruitmentEventType.OFFER_REJECTED);
        return OfferResponse.from(saved);
    }

    private Application transition(UUID id, ApplicationStatus status, RecruitmentEventType eventType) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Application application = ownedApplication(id);
        validateTransition(application.getStatus(), status);
        application.setStatus(status); Application saved = applications.save(application);
        publish(saved.getOrganization().getId(), null, saved.getId(), eventType);
        return saved;
    }

    private CandidateCvResponse uploadCv(Candidate candidate, org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CV file is required");
        }
        String fileName = blankToNull(file.getOriginalFilename());
        if (fileName == null) {
            throw new BadRequestException("CV file name is required");
        }
        CandidateCv cv = candidateCvs.findByCandidateId(candidate.getId()).orElseGet(CandidateCv::new);
        cv.setCandidate(candidate);
        cv.setFileName(fileName);
        cv.setStorageKey("candidates/" + candidate.getId() + "/cv/" + UUID.randomUUID() + "-" + fileName);
        cv.setContentType(blankToNull(file.getContentType()));
        candidate.setResumeKey(cv.getStorageKey());
        candidates.save(candidate);
        return CandidateCvResponse.from(candidateCvs.save(cv));
    }

    private boolean candidateMatches(Candidate candidate, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String query = search.trim().toLowerCase(Locale.ROOT);
        return candidate.getFirstName().toLowerCase(Locale.ROOT).contains(query)
                || candidate.getLastName().toLowerCase(Locale.ROOT).contains(query)
                || candidate.getEmail().toLowerCase(Locale.ROOT).contains(query);
    }

    private Interview ownedInterview(UUID id) {
        return interviews.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Interview not found"));
    }

    private Offer ownedOffer(UUID id) {
        return offers.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Offer not found"));
    }

    private void ensureInterviewScheduled(Interview interview) {
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new ConflictException("Interview is already closed");
        }
    }

    private void validateInterviewSchedule(InterviewRequest request) {
        if (request.scheduledAt().isBefore(Instant.now())) {
            throw new BadRequestException("Interview must be scheduled in the future");
        }
        if (request.interviewerId() == null) {
            throw new BadRequestException("Interviewer is required");
        }

        System.out.println("INTERVIEWER ID = " + request.interviewerId());
        System.out.println("ACTOR ORG ID   = " + actor.organizationId());
        Employee interviewer = employees.findByIdAndOrganizationId(request.interviewerId(), actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Interviewer not found"));
        if (interviewer.getStatus() != EmployeeStatus.ACTIVE) {
            throw new ConflictException("Interviewer is not active");
        }
    }

    private void applyInterviewRequest(Interview interview, InterviewRequest request) {
        Employee interviewer = employees.findByIdAndOrganizationId(request.interviewerId(), actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Interviewer not found"));
        interview.setInterviewer(interviewer);
        interview.setScheduledAt(request.scheduledAt());
        interview.setDurationMinutes(request.durationMinutes() == null ? 60 : request.durationMinutes());
        interview.setLocation(blankToNull(request.location()));
        interview.setMeetingLink(blankToNull(request.meetingLink()));
        interview.setNotes(blankToNull(request.notes()));
    }

    private Application ownedApplication(UUID id) {
        return applications.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Application not found"));
    }

    private Application ownApplication(UUID id) {
        Application application = applications.findById(id)
                .orElseThrow(() -> new NotFoundException("Application not found"));
        if (!application.getCandidate().getId().equals(currentCandidate().getId())) {
            throw new ForbiddenException("You do not own this application");
        }
        return application;
    }

    private Candidate organizationCandidate(UUID candidateId) {
        Candidate candidate = candidates.findById(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate not found"));
        if (!applications.existsByCandidateIdAndOrganizationId(candidateId, actor.organizationId())) {
            throw new NotFoundException("Candidate not found");
        }
        return candidate;
    }

    private Candidate currentCandidate() {
        UserAccount user = authenticatedUser();
        return candidates.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));
    }

    private void ensureOpenApplication(Application application) {
        if (application.getStatus() == ApplicationStatus.REJECTED
                || application.getStatus() == ApplicationStatus.WITHDRAWN
                || application.getStatus() == ApplicationStatus.HIRED
                || application.getStatus() == ApplicationStatus.OFFER_ACCEPTED) {
            throw new ConflictException("Application is already closed");
        }
    }

    private void validateTransition(ApplicationStatus current, ApplicationStatus target) {
        if (current == target) {
            throw new ConflictException("Application is already in this status");
        }
        boolean allowed = switch (current) {
            case APPLIED, SUBMITTED -> target == ApplicationStatus.SHORTLISTED
                    || target == ApplicationStatus.INTERVIEW
                    || target == ApplicationStatus.SELECTED
                    || target == ApplicationStatus.REJECTED
                    || target == ApplicationStatus.WITHDRAWN;
            case REVIEWING, SHORTLISTED -> target == ApplicationStatus.SELECTED
                    || target == ApplicationStatus.INTERVIEW
                    || target == ApplicationStatus.REJECTED
                    || target == ApplicationStatus.WITHDRAWN;
            case INTERVIEW, INTERVIEWING -> target == ApplicationStatus.SELECTED
                    || target == ApplicationStatus.OFFER_SENT
                    || target == ApplicationStatus.REJECTED
                    || target == ApplicationStatus.WITHDRAWN;
            case SELECTED -> target == ApplicationStatus.INTERVIEW
                    || target == ApplicationStatus.OFFER_SENT
                    || target == ApplicationStatus.REJECTED
                    || target == ApplicationStatus.WITHDRAWN;
            case OFFER_SENT, OFFERED -> target == ApplicationStatus.OFFER_ACCEPTED
                    || target == ApplicationStatus.REJECTED
                    || target == ApplicationStatus.WITHDRAWN;
            case OFFER_ACCEPTED -> target == ApplicationStatus.HIRED;
            case REJECTED, WITHDRAWN, HIRED -> false;
        };
        if (!allowed) {
            throw new ConflictException("Invalid application status transition");
        }
    }

    private RecruitmentEventType eventForStatus(ApplicationStatus status) {
        return switch (status) {
            case SELECTED, SHORTLISTED -> RecruitmentEventType.CANDIDATE_SHORTLISTED;
            case REJECTED -> RecruitmentEventType.CANDIDATE_REJECTED;
            case HIRED -> RecruitmentEventType.CANDIDATE_HIRED;
            case WITHDRAWN -> RecruitmentEventType.CANDIDATE_WITHDREW;
            default -> RecruitmentEventType.APPLICATION_STATUS_CHANGED;
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private JobOpening ownedJob(UUID id) {
        return jobs.findByIdAndOrganizationId(id, actor.organizationId()).orElseThrow(() -> new NotFoundException("Job opening not found"));
    }
    private void publish(UUID organizationId, UUID employeeId, UUID aggregateId, RecruitmentEventType type) {
        publisher.publish(new DomainEvent(type.name(), organizationId, employeeId, aggregateId, Instant.now(), Map.of()));
    }
}
