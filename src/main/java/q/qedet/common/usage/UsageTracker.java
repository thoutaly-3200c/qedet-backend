package q.qedet.common.usage;

import org.springframework.stereotype.Service;

import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;


@Service
public class UsageTracker {

    private final UsageEventRepository usageEventRepository;

    public UsageTracker(UsageEventRepository usageEventRepository) {
        this.usageEventRepository = usageEventRepository;
    }

    public void record(Organization organization, Employee employee, String moduleName, String actionName, String details) {
        usageEventRepository.save(new UsageEvent(organization, employee, moduleName, actionName, details));
    }
}
