package club.escobar.repository;

import club.escobar.entity.BusinessProfile;
import club.escobar.entity.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {

    Optional<BusinessProfile> findByUser_Id(Long userId);

    boolean existsByGstNumber(String gstNumber);

    boolean existsByGstNumberAndUser_IdNot(String gstNumber, Long userId);

    @Query("""
            select b from BusinessProfile b
            where b.approvalStatus = club.escobar.entity.enums.ApprovalStatus.APPROVED
            and (:search is null or lower(b.companyName) like lower(concat('%', :search, '%')))
            and (:industry is null or lower(b.industry) = lower(:industry))
            """)
    Page<BusinessProfile> search(@Param("search") String search, @Param("industry") String industry, Pageable pageable);

    @Query("""
            select b from BusinessProfile b
            where (:status is null or b.approvalStatus = :status)
            and (:search is null or lower(b.companyName) like lower(concat('%', :search, '%')))
            """)
    Page<BusinessProfile> searchForAdmin(@Param("search") String search, @Param("status") ApprovalStatus status, Pageable pageable);
}
