package q.qedet.hr.common.event;

import org.springframework.stereotype.Component;
import q.qedet.employee.entity.Employee;
import q.qedet.hr.common.model.EmployeeLifecycleEventType;

import java.time.Instant;
import java.util.Map;

@Component
public class EmployeeLifecycleEventPublisher {
    private final DomainEventPublisher publisher;
    public EmployeeLifecycleEventPublisher(DomainEventPublisher publisher) { this.publisher = publisher; }

    public void publish(Employee employee, EmployeeLifecycleEventType type) {
        publisher.publish(new DomainEvent(type.name(), employee.getOrganization().getId(), employee.getId(),
                employee.getId(), Instant.now(), Map.of()));
    }
}
