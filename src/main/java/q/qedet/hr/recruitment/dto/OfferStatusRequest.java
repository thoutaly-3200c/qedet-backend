package q.qedet.hr.recruitment.dto;

import jakarta.validation.constraints.NotNull;
import q.qedet.hr.recruitment.model.OfferStatus;

public record OfferStatusRequest(@NotNull OfferStatus status) {
}
