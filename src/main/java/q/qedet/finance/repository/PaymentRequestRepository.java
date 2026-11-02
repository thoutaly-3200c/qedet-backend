package q.qedet.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.finance.entity.PaymentRequest;
import java.util.*;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID> {
    Optional<PaymentRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<PaymentRequest> findAllByOrganizationIdOrderByRequestedAtDesc(UUID organizationId);
    boolean existsByOrganizationIdAndRequestTypeAndSourceReference(UUID organizationId, q.qedet.finance.model.PaymentRequestType requestType, String sourceReference);
}
