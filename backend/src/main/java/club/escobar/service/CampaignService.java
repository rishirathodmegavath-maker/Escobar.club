package club.escobar.service;

import club.escobar.dto.campaign.CampaignCreateRequest;
import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.campaign.CampaignUpdateRequest;
import club.escobar.dto.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CampaignService {

    CampaignResponse create(Long businessUserId, CampaignCreateRequest request);

    CampaignResponse update(Long businessUserId, Long campaignId, CampaignUpdateRequest request);

    PageResponse<CampaignResponse> listPublic(String search, String category, Pageable pageable);

    PageResponse<CampaignResponse> listMine(Long businessUserId, Pageable pageable);

    CampaignResponse getById(Long campaignId);
}
