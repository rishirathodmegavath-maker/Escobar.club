package club.escobar.service.impl;

import club.escobar.dto.business.BusinessDashboardResponse;
import club.escobar.dto.business.BusinessProfileResponse;
import club.escobar.dto.business.BusinessProfileUpdateRequest;
import club.escobar.dto.business.CampaignPreview;
import club.escobar.dto.business.CreatorActivityItem;
import club.escobar.dto.common.NeedsAttentionItem;
import club.escobar.dto.common.PageResponse;
import club.escobar.dto.common.PerformanceSummary;
import club.escobar.dto.common.PerformanceWindow;
import club.escobar.dto.common.TopContentItem;
import club.escobar.entity.BusinessProfile;
import club.escobar.entity.Campaign;
import club.escobar.entity.Content;
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.CampaignStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.PayoutStatus;
import club.escobar.exception.DuplicateResourceException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.mapper.BusinessProfileMapper;
import club.escobar.repository.BusinessCampaignPreviewRow;
import club.escobar.repository.BusinessProfileRepository;
import club.escobar.repository.CampaignCommittedRow;
import club.escobar.repository.CampaignRepository;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.MetricsRollupRow;
import club.escobar.repository.PayoutRepository;
import club.escobar.repository.TopContentRow;
import club.escobar.service.BusinessProfileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessProfileServiceImpl implements BusinessProfileService {

    private static final Logger log = LoggerFactory.getLogger(BusinessProfileServiceImpl.class);
    // Mirrors CampaignServiceImpl.COMMITTED_PAYOUT_STATUSES - everything except BELOW_THRESHOLD,
    // which always carries a zero amount anyway.
    private static final Set<PayoutStatus> COMMITTED_PAYOUT_STATUSES =
            EnumSet.of(PayoutStatus.PENDING_KYC, PayoutStatus.PAYABLE, PayoutStatus.PAID);
    private static final BigDecimal BUDGET_CAP_WARNING_THRESHOLD = new BigDecimal("0.8");

    private final BusinessProfileRepository businessProfileRepository;
    private final BusinessProfileMapper businessProfileMapper;
    private final CampaignRepository campaignRepository;
    private final ContentRepository contentRepository;
    private final PayoutRepository payoutRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BusinessProfileResponse> search(String search, String industry, Pageable pageable) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        String normalizedIndustry = StringUtils.hasText(industry) ? industry.trim() : null;

        Page<BusinessProfileResponse> page = businessProfileRepository
                .search(normalizedSearch, normalizedIndustry, pageable)
                .map(businessProfileMapper::toResponse);

        return PageResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessProfileResponse getById(Long id) {
        BusinessProfile profile = findById(id);
        if (profile.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResourceNotFoundException("Business profile not found with id " + id);
        }
        return businessProfileMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessProfileResponse getByUserId(Long userId) {
        BusinessProfile profile = businessProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found for user id " + userId));
        return businessProfileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public BusinessProfileResponse updateOwnProfile(Long userId, BusinessProfileUpdateRequest request) {
        BusinessProfile profile = businessProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found for user id " + userId));

        if (!request.gstNumber().equals(profile.getGstNumber())
                && businessProfileRepository.existsByGstNumberAndUser_IdNot(request.gstNumber(), userId)) {
            throw new DuplicateResourceException("This GST number is already registered to another business");
        }

        profile.setCompanyName(request.companyName());
        profile.setGstNumber(request.gstNumber());
        profile.setContactPersonName(request.contactPersonName());
        profile.setMobileNumber(request.mobileNumber());
        profile.setIndustry(request.industry());
        profile.setDescription(request.description());
        profile.setLogoUrl(request.logoUrl());
        profile.setWebsite(request.website());

        BusinessProfile saved = businessProfileRepository.save(profile);
        log.info("Business profile updated for user id={}", userId);
        return businessProfileMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessDashboardResponse dashboard(Long businessUserId) {
        BusinessProfile profile = businessProfileRepository.findByUser_Id(businessUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found for user id " + businessUserId));

        long contentAwaitingReview = contentRepository.countByBusiness_IdAndStatus(businessUserId, ContentStatus.SUBMITTED);
        long contentChangesRequested = contentRepository.countByBusiness_IdAndStatus(businessUserId, ContentStatus.CHANGES_REQUESTED);
        long contentPendingLinkReview = contentRepository.countByBusiness_IdAndStatus(businessUserId, ContentStatus.PENDING_LINK_REVIEW);
        long publishedContentCount = contentRepository.countByBusiness_IdAndStatus(businessUserId, ContentStatus.PUBLISHED);
        long approvedContentCount = contentRepository.countByBusiness_IdAndStatus(businessUserId, ContentStatus.APPROVED);
        long rejectedContentCount = contentRepository.countByBusiness_IdAndStatus(businessUserId, ContentStatus.REJECTED);

        BigDecimal totalBudgetInr = campaignRepository.sumMaxBudgetInrByBusinessId(businessUserId);
        BigDecimal totalCommittedInr = payoutRepository.sumAmountInrByBusiness_IdAndStatusIn(businessUserId, COMMITTED_PAYOUT_STATUSES);
        BigDecimal totalRemainingInr = totalBudgetInr.subtract(totalCommittedInr).max(BigDecimal.ZERO);

        MetricsRollupRow allTime = contentRepository.sumMetricsByBusinessAndPublishedAtAfter(businessUserId, Instant.EPOCH);
        long totalViews = allTime.getViews();
        long totalEngagement = allTime.getLikes() + allTime.getComments();

        List<Campaign> campaigns = campaignRepository
                .findByBusiness_Id(businessUserId, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt")))
                .getContent();

        List<NeedsAttentionItem> needsAttention = buildNeedsAttention(businessUserId, contentAwaitingReview, campaigns);
        List<CampaignPreview> campaignsPreview = buildCampaignsPreview(businessUserId, campaigns);
        List<CreatorActivityItem> creatorActivity = buildCreatorActivity(businessUserId);

        List<TopContentItem> topContent = contentRepository.findTopContentByBusiness(businessUserId, PageRequest.of(0, 3)).stream()
                .map(BusinessProfileServiceImpl::toTopContentItem)
                .toList();

        Instant now = Instant.now();
        PerformanceSummary performance = new PerformanceSummary(
                toWindow(contentRepository.sumMetricsByBusinessAndPublishedAtAfter(businessUserId, now.minus(7, ChronoUnit.DAYS))),
                toWindow(contentRepository.sumMetricsByBusinessAndPublishedAtAfter(businessUserId, now.minus(30, ChronoUnit.DAYS))),
                toWindow(contentRepository.sumMetricsByBusinessAndPublishedAtAfter(businessUserId, now.minus(90, ChronoUnit.DAYS))),
                toWindow(allTime)
        );

        return new BusinessDashboardResponse(
                profile.getApprovalStatus(),
                campaignRepository.countByBusiness_Id(businessUserId),
                campaignRepository.countLiveByBusinessId(businessUserId),
                campaignRepository.countByBusiness_IdAndApprovalStatus(businessUserId, ApprovalStatus.PENDING),
                contentAwaitingReview,
                contentChangesRequested,
                contentPendingLinkReview,
                publishedContentCount,
                payoutRepository.countByBusiness_IdAndStatus(businessUserId, PayoutStatus.PAYABLE),
                payoutRepository.sumAmountInrByBusiness_IdAndStatus(businessUserId, PayoutStatus.PAYABLE),
                payoutRepository.countByBusiness_IdAndStatus(businessUserId, PayoutStatus.PENDING_KYC),
                payoutRepository.sumAmountInrByBusiness_IdAndStatus(businessUserId, PayoutStatus.PAID),
                campaignRepository.countNearBudgetCapByBusinessId(businessUserId, COMMITTED_PAYOUT_STATUSES, BUDGET_CAP_WARNING_THRESHOLD),
                approvedContentCount,
                rejectedContentCount,
                contentRepository.countDistinctCreatorsByBusinessId(businessUserId),
                totalViews,
                totalEngagement,
                totalBudgetInr,
                totalCommittedInr,
                totalRemainingInr,
                needsAttention,
                campaignsPreview,
                creatorActivity,
                topContent,
                performance
        );
    }

    private List<NeedsAttentionItem> buildNeedsAttention(Long businessUserId, long contentAwaitingReview, List<Campaign> recentCampaigns) {
        List<NeedsAttentionItem> items = new ArrayList<>();

        if (contentAwaitingReview > 0) {
            items.add(new NeedsAttentionItem(
                    contentAwaitingReview + (contentAwaitingReview == 1 ? " submission is" : " submissions are") + " awaiting your review.",
                    "Review submissions", "/business/content"));
        }

        List<Campaign> nearCap = campaignRepository.findNearBudgetCapByBusinessId(
                businessUserId, COMMITTED_PAYOUT_STATUSES, BUDGET_CAP_WARNING_THRESHOLD, PageRequest.of(0, 3));
        for (Campaign c : nearCap) {
            items.add(new NeedsAttentionItem(
                    "\"" + c.getTitle() + "\" is nearing its budget cap.",
                    "View campaign", "/business/campaigns"));
        }

        LocalDate soon = LocalDate.now().plusDays(3);
        recentCampaigns.stream()
                .filter(c -> c.getEffectiveStatus() == CampaignStatus.UPCOMING
                        && c.isOpenForSubmissions()
                        && !c.getSubmissionDeadline().isAfter(soon))
                .forEach(c -> items.add(new NeedsAttentionItem(
                        "\"" + c.getTitle() + "\" submission window closes soon.",
                        "View campaign", "/business/campaigns")));

        return items;
    }

    private List<CampaignPreview> buildCampaignsPreview(Long businessUserId, List<Campaign> campaigns) {
        if (campaigns.isEmpty()) {
            return List.of();
        }
        Map<Long, BusinessCampaignPreviewRow> previewAgg = contentRepository
                .findCampaignPreviewAggregatesByBusinessId(businessUserId).stream()
                .collect(Collectors.toMap(BusinessCampaignPreviewRow::getCampaignId, row -> row));
        Map<Long, BigDecimal> committedMap = payoutRepository
                .sumCommittedByBusinessGroupedByCampaign(businessUserId, COMMITTED_PAYOUT_STATUSES).stream()
                .collect(Collectors.toMap(CampaignCommittedRow::getCampaignId, CampaignCommittedRow::getCommitted));

        return campaigns.stream()
                .map(c -> {
                    BusinessCampaignPreviewRow agg = previewAgg.get(c.getId());
                    return new CampaignPreview(
                            c.getId(), c.getTitle(), c.getEffectiveStatus().name(),
                            agg != null ? agg.getCreatorsCount() : 0L,
                            agg != null ? agg.getContentSubmittedCount() : 0L,
                            agg != null ? agg.getContentPublishedCount() : 0L,
                            agg != null ? agg.getViews() : 0L,
                            c.getMaxBudgetInr(),
                            committedMap.getOrDefault(c.getId(), BigDecimal.ZERO),
                            c.getSubmissionDeadline());
                })
                .toList();
    }

    private List<CreatorActivityItem> buildCreatorActivity(Long businessUserId) {
        List<Content> recent = contentRepository
                .findByBusiness_Id(businessUserId, PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "updatedAt")))
                .getContent();
        return recent.stream()
                .map(c -> {
                    String creatorName = c.getCreator().getCreatorProfile().getDisplayName();
                    String campaignTitle = c.getCampaign().getTitle();
                    String message = creatorName + " " + creatorActivityVerb(c.getStatus()) + " on \"" + campaignTitle + "\".";
                    return new CreatorActivityItem(creatorName, campaignTitle, message, c.getUpdatedAt());
                })
                .toList();
    }

    private static String creatorActivityVerb(ContentStatus status) {
        return switch (status) {
            case DRAFT -> "started a draft";
            case SUBMITTED -> "submitted content for review";
            case CHANGES_REQUESTED -> "was asked for changes";
            case APPROVED -> "had content approved";
            case REJECTED -> "had content rejected";
            case PENDING_LINK_REVIEW -> "submitted a live post link for review";
            case PUBLISHED -> "had content published";
        };
    }

    private static PerformanceWindow toWindow(MetricsRollupRow row) {
        long views = row.getViews();
        long likes = row.getLikes();
        long comments = row.getComments();
        double engagementRate = views == 0 ? 0.0 : Math.round((likes + comments) * 1000.0 / views) / 10.0;
        return new PerformanceWindow(views, likes, comments, row.getPublishedCount(), engagementRate);
    }

    private static TopContentItem toTopContentItem(TopContentRow row) {
        long views = row.getViews();
        long likes = row.getLikes();
        long comments = row.getComments();
        double engagementRate = views == 0 ? 0.0 : Math.round((likes + comments) * 1000.0 / views) / 10.0;
        return new TopContentItem(row.getContentId(), row.getCreatorDisplayName(), row.getCampaignTitle(),
                row.getMediaType(), views, likes, comments, engagementRate, row.getPostUrl());
    }

    private BusinessProfile findById(Long id) {
        return businessProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found with id " + id));
    }
}
