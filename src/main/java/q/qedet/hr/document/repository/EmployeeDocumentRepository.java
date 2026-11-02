package q.qedet.hr.document.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.document.entity.EmployeeDocument;
import java.util.*;
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, UUID> {
    Optional<EmployeeDocument> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<EmployeeDocument> findAllByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
}
