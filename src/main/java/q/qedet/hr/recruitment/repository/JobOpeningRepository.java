package q.qedet.hr.recruitment.repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.recruitment.entity.JobOpening;
import java.util.*;
public interface JobOpeningRepository extends JpaRepository<JobOpening, UUID> {
    Optional<JobOpening> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<JobOpening> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    boolean existsByLocationId(UUID locationId);

    @Query("""
            select distinct j
            from JobOpening j
            left join fetch j.requirements
            where j.organization.id = :organizationId
            order by j.createdAt desc
            """)
    List<JobOpening> findAllWithRequirementsByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("""
            select distinct j
            from JobOpening j
            left join fetch j.requirements
            where j.id = :id
              and j.organization.id = :organizationId
            """)
    Optional<JobOpening> findWithRequirementsByIdAndOrganizationId(
            @Param("id") UUID id,
            @Param("organizationId") UUID organizationId);

    @Query("""
            select distinct j
            from JobOpening j
            left join fetch j.requirements
            where j.id = :id
            """)
    Optional<JobOpening> findWithRequirementsById(@Param("id") UUID id);
}
