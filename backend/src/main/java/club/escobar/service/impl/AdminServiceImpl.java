package club.escobar.service.impl;

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
import club.escobar.entity.BusinessProfile;
import club.escobar.entity.Campaign;
import club.escobar.entity.Content;
import club.escobar.entity.ContentMetricsSnapshot;
import club.escobar.entity.CreatorKycProfile;
import club.escobar.entity.CreatorProfile;
import club.escobar.entity.User;
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.repository.BusinessProfileRepository;
import club.escobar.repository.CampaignRepository;
import club.escobar.repository.ContentMetricsSnapshotRepository;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.CreatorKycProfileRepository;
import club.escobar.repository.CreatorProfileRepository;
import club.escobar.repository.RefreshTokenRepository;
import club.escobar.repository.UserRepository;
import club.escobar.service.AdminService;
import club.escobar.service.CreatorKycService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final CreatorKycProfileRepository creatorKycProfileRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final CampaignRepository campaignRepository;
    private final ContentRepository contentRepository;
    private final ContentMetricsSnapshotRepository contentMetricsSnapshotRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CreatorKycService creatorKycService;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(
                businessProfileRepository.count(),
                userRepository.countByRole(UserRole.CREATOR),
                campaignRepository.count(),
                campaignRepository.countByApprovalStatus(ApprovalStatus.PENDING),
                creatorKycProfileRepository.countByStatus(KycStatus.PENDING)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminCreatorSummaryResponse> listCreators(KycStatus kycStatus, Pageable pageable) {
        Page<User> users = kycStatus == null
                ? userRepository.findByRole(UserRole.CREATOR, pageable)
                : userRepository.findByRoleAndKycStatus(UserRole.CREATOR, kycStatus, pageable);
        return PageResponse.of(users.map(this::toCreatorSummary));
    }

    @Override
    @Transactional
    public CreatorKycReviewDetailResponse reviewCreatorKyc(Long adminUserId, Long creatorUserId, CreatorKycReviewRequest request) {
        return creatorKycService.adminReview(adminUserId, creatorUserId, request);
    }

    @Override
    @Transactional
    public AdminCreatorSummaryResponse setCreatorActive(Long creatorUserId, boolean active) {
        User user = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.CREATOR) {
            throw new ForbiddenActionException("Only creator accounts can be suspended or activated here");
        }
        user.setActive(active);
        User saved = userRepository.save(user);
        if (!active) {
            // Revokes any still-valid refresh tokens; the JWT filter also re-checks `active` on every
            // request, so access is cut off immediately rather than waiting for the token to expire.
            refreshTokenRepository.revokeAllForUser(creatorUserId);
        }
        log.info("Admin set creator id={} active={}", creatorUserId, active);
        return toCreatorSummary(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminBusinessSummaryResponse> listBusinesses(ApprovalStatus status, String search, Pageable pageable) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        Page<AdminBusinessSummaryResponse> page = businessProfileRepository
                .searchForAdmin(normalizedSearch, status, pageable)
                .map(this::toBusinessSummary);
        return PageResponse.of(page);
    }

    @Override
    @Transactional
    public AdminBusinessSummaryResponse reviewBusiness(Long businessUserId, ApprovalDecisionRequest request) {
        BusinessProfile profile = businessProfileRepository.findByUser_Id(businessUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found for user id " + businessUserId));
        profile.setApprovalStatus(request.status());
        BusinessProfile saved = businessProfileRepository.save(profile);
        log.info("Admin reviewed business user id={}: {}", businessUserId, request.status());
        return toBusinessSummary(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminCampaignSummaryResponse> listCampaigns(ApprovalStatus status, String search, Pageable pageable) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        Page<AdminCampaignSummaryResponse> page = campaignRepository
                .searchForAdmin(normalizedSearch, status, pageable)
                .map(this::toCampaignSummary);
        return PageResponse.of(page);
    }

    @Override
    @Transactional
    public AdminCampaignSummaryResponse reviewCampaign(Long campaignId, ApprovalDecisionRequest request) {
        Campaign campaign = findCampaign(campaignId);
        campaign.setApprovalStatus(request.status());
        Campaign saved = campaignRepository.save(campaign);
        log.info("Admin reviewed campaign id={}: {}", campaignId, request.status());
        return toCampaignSummary(saved);
    }

    @Override
    @Transactional
    public AdminCampaignSummaryResponse setCampaignDisplayStatus(Long campaignId, CampaignDisplayStatusUpdateRequest request) {
        Campaign campaign = findCampaign(campaignId);
        campaign.setAdminDisplayStatus(request.status());
        Campaign saved = campaignRepository.save(campaign);
        log.info("Admin set campaign id={} display status={}", campaignId, request.status());
        return toCampaignSummary(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminContentSummaryResponse> listContent(ContentStatus status, Pageable pageable) {
        Page<Content> page = status == null
                ? contentRepository.findAll(pageable)
                : contentRepository.findByStatus(status, pageable);
        return PageResponse.of(page.map(this::toContentSummary));
    }

    private Campaign findCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id " + campaignId));
    }

    private AdminCreatorSummaryResponse toCreatorSummary(User user) {
        String displayName = creatorProfileRepository.findByUser_Id(user.getId())
                .map(CreatorProfile::getDisplayName)
                .orElse(null);
        KycStatus kycStatus = creatorKycProfileRepository.findByCreator_Id(user.getId())
                .map(CreatorKycProfile::getStatus)
                .orElse(null);
        return new AdminCreatorSummaryResponse(user.getId(), user.getEmail(), displayName,
                user.isActive(), kycStatus, user.getCreatedAt());
    }

    private AdminBusinessSummaryResponse toBusinessSummary(BusinessProfile profile) {
        return new AdminBusinessSummaryResponse(profile.getUser().getId(), profile.getUser().getEmail(),
                profile.getCompanyName(), profile.getGstNumber(), profile.getContactPersonName(),
                profile.getApprovalStatus(), profile.getCreatedAt());
    }

    private AdminCampaignSummaryResponse toCampaignSummary(Campaign campaign) {
        String businessCompanyName = campaign.getBusiness().getBusinessProfile() != null
                ? campaign.getBusiness().getBusinessProfile().getCompanyName()
                : null;
        return new AdminCampaignSummaryResponse(campaign.getId(), campaign.getTitle(), businessCompanyName,
                campaign.getEffectiveStatus(), campaign.getApprovalStatus(), campaign.getAdminDisplayStatus(),
                campaign.getPublishStartAt(), campaign.getPublishEndAt(), campaign.getCreatedAt());
    }

    private AdminContentSummaryResponse toContentSummary(Content content) {
        String businessCompanyName = content.getBusiness().getBusinessProfile() != null
                ? content.getBusiness().getBusinessProfile().getCompanyName()
                : null;
        String creatorDisplayName = creatorProfileRepository.findByUser_Id(content.getCreator().getId())
                .map(CreatorProfile::getDisplayName)
                .orElse(null);
        ContentMetricsSnapshot latest = contentMetricsSnapshotRepository
                .findTopByContent_IdOrderByFetchedAtDesc(content.getId())
                .orElse(null);
        return new AdminContentSummaryResponse(
                content.getId(), creatorDisplayName, content.getCreator().getEmail(),
                businessCompanyName, content.getCampaign().getTitle(), content.getStatus(),
                content.getPostUrl(), content.getPublishedAt(),
                latest != null ? latest.getLikeCount() : null,
                latest != null ? latest.getCommentCount() : null,
                latest != null ? latest.getViewCount() : null,
                latest != null ? latest.getFetchedAt() : null);
    }
}
