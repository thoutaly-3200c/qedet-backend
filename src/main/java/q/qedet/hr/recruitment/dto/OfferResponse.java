package q.qedet.hr.recruitment.dto;

import q.qedet.hr.recruitment.entity.Offer;
import q.qedet.hr.recruitment.model.OfferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OfferResponse(
        UUID id,
        UUID applicationId,
        UUID organizationId,
        String positionTitle,
        BigDecimal salary,
        String currency,
        LocalDate startDate,
        LocalDate expiresOn,
        String terms,
        OfferStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static OfferResponse from(Offer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getApplication().getId(),
                offer.getOrganization().getId(),
                offer.getPositionTitle(),
                offer.getSalary(),
                offer.getCurrency(),
                offer.getStartDate(),
                offer.getExpiresOn(),
                offer.getTerms(),
                offer.getStatus(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }
}
