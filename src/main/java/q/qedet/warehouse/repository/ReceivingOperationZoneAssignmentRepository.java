package q.qedet.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import q.qedet.warehouse.entity.ReceivingOperationZoneAssignment;

import java.util.*;

public interface ReceivingOperationZoneAssignmentRepository extends JpaRepository<ReceivingOperationZoneAssignment, UUID> {
    List<ReceivingOperationZoneAssignment> findAllByReceivingOperationItemIdOrderByCreatedAtAsc(UUID id);

    /** Executes immediately so replacement inserts cannot overtake queued entity deletes. */
    @Modifying(flushAutomatically = true)
    @Query("delete from ReceivingOperationZoneAssignment assignment where assignment.receivingOperationItem.id = :operationItemId")
    int deleteAllByReceivingOperationItemIdInBulk(@Param("operationItemId") UUID operationItemId);

    boolean existsByReceivingOperationItemReceivingOperationId(UUID operationId);
}
