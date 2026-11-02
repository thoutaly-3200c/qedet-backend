package q.qedet.hr.document.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.hr.common.event.*;
import q.qedet.hr.document.dto.DocumentUploadRequest;
import q.qedet.hr.document.entity.EmployeeDocument;
import q.qedet.hr.document.model.*;
import q.qedet.hr.document.repository.EmployeeDocumentRepository;
import java.time.Instant;
import java.util.*;

@Service
public class EmployeeDocumentService {
    private final EmployeeDocumentRepository documents;
    private final EmployeeRepository employees;
    private final ActorContext actor;
    private final DomainEventPublisher publisher;

    public EmployeeDocumentService(EmployeeDocumentRepository documents, EmployeeRepository employees,
                                   ActorContext actor, DomainEventPublisher publisher) {
        this.documents = documents; this.employees = employees; this.actor = actor; this.publisher = publisher;
    }

    @Transactional
    public EmployeeDocument upload(UUID employeeId, DocumentUploadRequest request) {
        actor.requirePermission(Permission.EMPLOYEE_DOCUMENT_MANAGE);
        Employee employee = employees.findByIdAndOrganizationId(employeeId, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        EmployeeDocument document = new EmployeeDocument();
        document.setOrganization(employee.getOrganization()); document.setEmployee(employee);
        document.setDocumentType(request.documentType().trim()); document.setFileName(request.fileName().trim());
        document.setStorageKey(request.storageKey().trim()); document.setExpiresOn(request.expiresOn());
        EmployeeDocument saved = documents.save(document);
        publish(saved, DocumentEventType.DOCUMENT_UPLOADED);
        return saved;
    }

    @Transactional
    public EmployeeDocument verify(UUID id) {
        actor.requirePermission(Permission.EMPLOYEE_DOCUMENT_MANAGE);
        EmployeeDocument document = owned(id);
        document.setStatus(EmployeeDocumentStatus.VERIFIED); document.setVerifiedAt(Instant.now());
        EmployeeDocument saved = documents.save(document);
        publish(saved, DocumentEventType.DOCUMENT_VERIFIED);
        return saved;
    }

    @Transactional
    public EmployeeDocument reject(UUID id, String reason) {
        actor.requirePermission(Permission.EMPLOYEE_DOCUMENT_MANAGE);
        EmployeeDocument document = owned(id);
        document.setStatus(EmployeeDocumentStatus.REJECTED); document.setRejectionReason(reason);
        EmployeeDocument saved = documents.save(document);
        publish(saved, DocumentEventType.DOCUMENT_REJECTED);
        return saved;
    }

    private EmployeeDocument owned(UUID id) {
        return documents.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Employee document not found"));
    }
    private void publish(EmployeeDocument document, DocumentEventType type) {
        publisher.publish(new DomainEvent(type.name(), document.getOrganization().getId(),
                document.getEmployee().getId(), document.getId(), Instant.now(), Map.of()));
    }
}
