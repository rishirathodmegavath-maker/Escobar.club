package club.escobar.service;

import club.escobar.dto.campaign.CampaignCreateRequest;
import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.campaign.CampaignScheduleChangeResponse;
import club.escobar.dto.campaign.CampaignScheduleUpdateRequest;
import club.escobar.dto.campaign.CampaignUpdateRequest;
import club.escobar.dto.common.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CampaignService {

    CampaignResponse create(Long businessUserId, CampaignCreateRequest request);

    CampaignResponse update(Long businessUserId, Long campaignId, CampaignUpdateRequest request);

    // Focused, dedicated "prepone/postpone" flow with its own safety rules (effective-status gate,
    // future-publishStartAt guard, audit trail) and its own frontend UX - deliberately kept separate
    // from the general update() above rather than retrofitting these rules onto it.
    CampaignResponse updateSchedule(Long businessUserId, Long campaignId, CampaignScheduleUpdateRequest request);

    List<CampaignScheduleChangeResponse> getScheduleHistory(Long requestingUserId, String requestingRole, Long campaignId);

    PageResponse<CampaignResponse> listPublic(String search, String category, Pageable pageable);

    PageResponse<CampaignResponse> listMine(Long businessUserId, Pageable pageable);

    CampaignResponse getById(Long campaignId);
}
