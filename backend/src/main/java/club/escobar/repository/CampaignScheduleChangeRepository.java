package club.escobar.repository;

import club.escobar.entity.CampaignScheduleChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignScheduleChangeRepository extends JpaRepository<CampaignScheduleChange, Long> {

    List<CampaignScheduleChange> findByCampaign_IdOrderByChangedAtDesc(Long campaignId);
}
