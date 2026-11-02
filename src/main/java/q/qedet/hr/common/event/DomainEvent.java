package q.qedet.hr.common.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(
        String eventType,
        UUID organizationId,
        UUID employeeId,
        UUID aggregateId,
        Instant occurredAt,
        Map<String, String> attributes) {

    public DomainEvent {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
