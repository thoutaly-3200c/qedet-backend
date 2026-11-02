package q.qedet.hr.recruitment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.NotFoundException;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.hr.common.event.DomainEventPublisher;
import q.qedet.hr.recruitment.dto.JobOpeningResponse;
import q.qedet.hr.recruitment.entity.JobOpening;
import q.qedet.hr.recruitment.model.JobOpeningStatus;
import q.qedet.hr.recruitment.repository.ApplicationRepository;
import q.qedet.hr.recruitment.repository.CandidateRepository;
import q.qedet.hr.recruitment.repository.JobOpeningRepository;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentServicePublicJobTest {
    @Mock JobOpeningRepository jobs;
    @Mock CandidateRepository candidates;
    @Mock ApplicationRepository applications;
    @Mock EmployeeRepository employees;
    @Mock OrganizationRepository organizations;
    @Mock ActorContext actor;
    @Mock DomainEventPublisher publisher;
    @InjectMocks RecruitmentService service;

    @Test
    void returnsOpenJobWithoutUsingActorOrganizationOrPermissions() {
        UUID jobId = UUID.randomUUID();
        JobOpening job = job(jobId, JobOpeningStatus.OPEN);
        when(jobs.findWithRequirementsById(jobId)).thenReturn(Optional.of(job));

        JobOpeningResponse response = service.getPublicJob(jobId);

        assertEquals(jobId, response.id());
        assertEquals(JobOpeningStatus.OPEN, response.status());
        verify(jobs).findWithRequirementsById(jobId);
        verifyNoInteractions(actor);
    }

    @Test
    void hidesDraftAndClosedJobs() {
        UUID jobId = UUID.randomUUID();
        when(jobs.findWithRequirementsById(jobId)).thenReturn(Optional.of(job(jobId, JobOpeningStatus.DRAFT)));

        assertThrows(NotFoundException.class, () -> service.getPublicJob(jobId));

        UUID closedJobId = UUID.randomUUID();
        when(jobs.findWithRequirementsById(closedJobId)).thenReturn(Optional.of(job(closedJobId, JobOpeningStatus.CLOSED)));

        assertThrows(NotFoundException.class, () -> service.getPublicJob(closedJobId));
    }

    @Test
    void reportsMissingJobAsNotFound() {
        UUID jobId = UUID.randomUUID();
        when(jobs.findWithRequirementsById(jobId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getPublicJob(jobId));
    }

    private JobOpening job(UUID id, JobOpeningStatus status) {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        JobOpening job = new JobOpening();
        job.setId(id);
        job.setOrganization(organization);
        job.setTitle("Software Engineer");
        job.setStatus(status);
        return job;
    }
}
