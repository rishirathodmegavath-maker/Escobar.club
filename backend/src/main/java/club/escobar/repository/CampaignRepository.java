package club.escobar.repository;

import club.escobar.entity.Campaign;
import club.escobar.entity.enums.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Page<Campaign> findByBusiness_Id(Long businessId, Pageable pageable);

    @Query("""
            select c from Campaign c
            where c.status <> club.escobar.entity.enums.CampaignStatus.DRAFT
            and c.status <> club.escobar.entity.enums.CampaignStatus.CLOSED
            and (:search is null or lower(c.title) like lower(concat('%', :search, '%')))
            """)
    Page<Campaign> searchPublic(@Param("search") String search, Pageable pageable);

    @Query("""
            select c from Campaign c
            where c.status = :status
            and (:search is null or lower(c.title) like lower(concat('%', :search, '%')))
            """)
    Page<Campaign> searchPublicByStatus(@Param("search") String search, @Param("status") CampaignStatus status, Pageable pageable);

    @Query("select c.ratePerThousandViewsInr from Campaign c where c.status = club.escobar.entity.enums.CampaignStatus.ACTIVE")
    List<BigDecimal> findActiveRates();
}
