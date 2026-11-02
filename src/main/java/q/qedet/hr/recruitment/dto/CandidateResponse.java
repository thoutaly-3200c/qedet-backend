package q.qedet.hr.recruitment.dto;

import q.qedet.hr.recruitment.entity.Candidate;

import java.time.Instant;
import java.util.UUID;

public record CandidateResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean archived,
        Instant createdAt,
        Instant updatedAt
) {
    public static CandidateResponse from(Candidate candidate) {
        return new CandidateResponse(
                candidate.getId(),
                candidate.getUser().getId(),
                candidate.getFirstName(),
                candidate.getLastName(),
                candidate.getEmail(),
                candidate.getPhone(),
                candidate.isArchived(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt()
        );
    }
}
