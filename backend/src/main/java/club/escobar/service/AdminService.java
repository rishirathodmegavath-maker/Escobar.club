package club.escobar.service;

import club.escobar.dto.admin.AdminBusinessSummaryResponse;
import club.escobar.dto.admin.AdminCampaignSummaryResponse;
import club.escobar.dto.admin.AdminContentSummaryResponse;
import club.escobar.dto.admin.AdminCreatorSummaryResponse;
import club.escobar.dto.admin.AdminDashboardResponse;
import club.escobar.dto.admin.ApprovalDecisionRequest;
import club.escobar.dto.admin.CampaignDisplayStatusUpdateRequest;
import club.escobar.dto.common.PageResponse;
import club.escobar.dto.kyc.CreatorKycReviewDetailResponse;
import club.escobar.dto.kyc.CreatorKycReviewRequest;
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.KycStatus;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    AdminDashboardResponse dashboard();

    PageResponse<AdminCreatorSummaryResponse> listCreators(KycStatus kycStatus, Pageable pageable);

    CreatorKycReviewDetailResponse reviewCreatorKyc(Long adminUserId, Long creatorUserId, CreatorKycReviewRequest request);

    AdminCreatorSummaryResponse setCreatorActive(Long creatorUserId, boolean active);

    PageResponse<AdminBusinessSummaryResponse> listBusinesses(ApprovalStatus status, String search, Pageable pageable);

    AdminBusinessSummaryResponse reviewBusiness(Long businessUserId, ApprovalDecisionRequest request);

    PageResponse<AdminCampaignSummaryResponse> listCampaigns(ApprovalStatus status, String search, Pageable pageable);

    AdminCampaignSummaryResponse reviewCampaign(Long campaignId, ApprovalDecisionRequest request);

    AdminCampaignSummaryResponse setCampaignDisplayStatus(Long campaignId, CampaignDisplayStatusUpdateRequest request);

    PageResponse<AdminContentSummaryResponse> listContent(ContentStatus status, Pageable pageable);
}
