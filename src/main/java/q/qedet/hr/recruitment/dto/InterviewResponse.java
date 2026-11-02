package q.qedet.hr.recruitment.dto;

import q.qedet.hr.recruitment.entity.Interview;
import q.qedet.hr.recruitment.model.InterviewResult;
import q.qedet.hr.recruitment.model.InterviewStatus;

import java.time.Instant;
import java.util.UUID;

public record InterviewResponse(
        UUID id,
        UUID applicationId,
        UUID organizationId,
        UUID interviewerId,
        Instant scheduledAt,
        Integer durationMinutes,
        String location,
        String meetingLink,
        String notes,
        String feedback,
        InterviewStatus status,
        InterviewResult result,
        Instant createdAt,
        Instant updatedAt
) {
    public static InterviewResponse from(Interview interview) {
        return new InterviewResponse(
                interview.getId(),
                interview.getApplication().getId(),
                interview.getOrganization().getId(),
                interview.getInterviewer().getId(),
                interview.getScheduledAt(),
                interview.getDurationMinutes(),
                interview.getLocation(),
                interview.getMeetingLink(),
                interview.getNotes(),
                interview.getFeedback(),
                interview.getStatus(),
                interview.getResult(),
                interview.getCreatedAt(),
                interview.getUpdatedAt()
        );
    }
}
