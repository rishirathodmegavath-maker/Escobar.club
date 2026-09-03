package club.escobar.service.impl;

import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.common.NeedsAttentionItem;
import club.escobar.dto.common.PerformanceSummary;
import club.escobar.dto.common.PerformanceWindow;
import club.escobar.dto.common.TopContentItem;
import club.escobar.dto.creator.ActiveCampaignSummary;
import club.escobar.dto.creator.ContentStatusCounts;
import club.escobar.dto.creator.CreatorDashboardResponse;
import club.escobar.dto.creator.CreatorProfileResponse;
import club.escobar.dto.creator.CreatorProfileUpdateRequest;
import club.escobar.dto.creator.EarningsSummary;
import club.escobar.dto.creator.PayoutPreviewItem;
import club.escobar.dto.creator.RecentActivityItem;
import club.escobar.entity.Campaign;
import club.escobar.entity.Content;
import club.escobar.entity.CreatorKycProfile;
import club.escobar.entity.CreatorProfile;
import club.escobar.entity.Payout;
import club.escobar.entity.enums.CampaignStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.PayoutStatus;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.mapper.CreatorProfileMapper;
import club.escobar.repository.CampaignAggregateRow;
import club.escobar.repository.CampaignRepository;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.CreatorKycProfileRepository;
import club.escobar.repository.CreatorProfileRepository;
import club.escobar.repository.MetricsRollupRow;
import club.escobar.repository.PayoutRepository;
import club.escobar.repository.TopContentRow;
import club.escobar.service.CampaignService;
import club.escobar.service.CreatorProfileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreatorProfileServiceImpl implements CreatorProfileService {

    private static final Logger log = LoggerFactory.getLogger(CreatorProfileServiceImpl.class);
    private static final int PROFILE_CHECKLIST_ITEMS = 6;

    private final CreatorProfileRepository creatorProfileRepository;
    private final CreatorProfileMapper creatorProfileMapper;
    private final CreatorKycProfileRepository creatorKycProfileRepository;
    private final CampaignRepository campaignRepository;
    private final ContentRepository contentRepository;
    private final PayoutRepository payoutRepository;
    private final CampaignService campaignService;

    @Override
    @Transactional(readOnly = true)
    public CreatorProfileResponse getByUserId(Long userId) {
        CreatorProfile profile = findByUserId(userId);
        return creatorProfileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public CreatorProfileResponse updateOwnProfile(Long userId, CreatorProfileUpdateRequest request) {
        CreatorProfile profile = findByUserId(userId);

        profile.setDisplayName(request.displayName());
        profile.setBio(request.bio());
        profile.setProfilePictureUrl(request.profilePictureUrl());
        profile.setNiche(request.niche());
        profile.setOpenToOtherNiches(request.openToOtherNiches());
        profile.setInstagramProfileUrl(request.instagramProfileUrl());
        profile.setFollowerCount(request.followerCount());
        profile.setPortfolioLinks(request.portfolioLinks() == null ? new ArrayList<>() : new ArrayList<>(request.portfolioLinks()));

        CreatorProfile saved = creatorProfileRepository.save(profile);
        log.info("Creator profile updated for user id={}", userId);
        return creatorProfileMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorDashboardResponse dashboard(Long creatorUserId) {
        CreatorProfile profile = findByUserId(creatorUserId);

        KycStatus kycStatus = creatorKycProfileRepository.findByCreator_Id(creatorUserId)
                .map(CreatorKycProfile::getStatus)
                .orElse(null);

        List<String> missing = new ArrayList<>();
        if (isBlank(profile.getProfilePictureUrl())) missing.add("Profile photo");
        if (isBlank(profile.getBio())) missing.add("Bio");
        if (isBlank(profile.getNiche())) missing.add("Niche");
        if (isBlank(profile.getInstagramProfileUrl())) missing.add("Instagram link");
        if (profile.getPortfolioLinks() == null || profile.getPortfolioLinks().isEmpty()) missing.add("Portfolio link");
        if (kycStatus != KycStatus.VERIFIED) missing.add("KYC verification");
        int profileCompletionPercent = (int) Math.round((PROFILE_CHECKLIST_ITEMS - missing.size()) * 100.0 / PROFILE_CHECKLIST_ITEMS);

        ContentStatusCounts submissionStatus = new ContentStatusCounts(
                contentRepository.countByCreator_IdAndStatus(creatorUserId, ContentStatus.SUBMITTED),
                contentRepository.countByCreator_IdAndStatus(creatorUserId, ContentStatus.CHANGES_REQUESTED),
                contentRepository.countByCreator_IdAndStatus(creatorUserId, ContentStatus.APPROVED),
                contentRepository.countByCreator_IdAndStatus(creatorUserId, ContentStatus.PENDING_LINK_REVIEW),
                contentRepository.countByCreator_IdAndStatus(creatorUserId, ContentStatus.PUBLISHED),
                contentRepository.countByCreator_IdAndStatus(creatorUserId, ContentStatus.REJECTED)
        );

        EarningsSummary earnings = new EarningsSummary(
                payoutRepository.sumAmountInrByCreator_IdAndStatus(creatorUserId, PayoutStatus.PENDING_KYC),
                payoutRepository.sumAmountInrByCreator_IdAndStatus(creatorUserId, PayoutStatus.PAYABLE),
                payoutRepository.sumAmountInrByCreator_IdAndStatus(creatorUserId, PayoutStatus.PAID),
                payoutRepository.sumAmountInrByCreator_IdAndStatusAndPaidAtAfter(creatorUserId, PayoutStatus.PAID, startOfCurrentMonth())
        );

        Instant now = Instant.now();
        PerformanceSummary performance = new PerformanceSummary(
                toWindow(contentRepository.sumMetricsByCreatorAndPublishedAtAfter(creatorUserId, now.minus(7, ChronoUnit.DAYS))),
                toWindow(contentRepository.sumMetricsByCreatorAndPublishedAtAfter(creatorUserId, now.minus(30, ChronoUnit.DAYS))),
                toWindow(contentRepository.sumMetricsByCreatorAndPublishedAtAfter(creatorUserId, now.minus(90, ChronoUnit.DAYS))),
                toWindow(contentRepository.sumMetricsByCreatorAndPublishedAtAfter(creatorUserId, Instant.EPOCH))
        );

        List<NeedsAttentionItem> needsAttention = buildNeedsAttention(creatorUserId, kycStatus, profileCompletionPercent, submissionStatus);
        List<CampaignResponse> recommendedCampaigns = campaignService.listRecommendedForCreator(creatorUserId, 5);
        List<ActiveCampaignSummary> activeCampaigns = buildActiveCampaigns(creatorUserId);

        List<TopContentItem> topContent = contentRepository.findTopContentByCreator(creatorUserId, PageRequest.of(0, 3)).stream()
                .map(CreatorProfileServiceImpl::toTopContentItem)
                .toList();

        List<RecentActivityItem> recentActivity = buildRecentActivity(creatorUserId);

        List<PayoutPreviewItem> recentPayouts = payoutRepository
                .findByCreator_Id(creatorUserId, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "calculatedAt")))
                .stream()
                .map(p -> new PayoutPreviewItem(p.getContent().getId(), p.getCampaign().getTitle(), p.getAmountInr(), p.getStatus(), p.getPaidAt()))
                .toList();

        return new CreatorDashboardResponse(
                kycStatus, profileCompletionPercent, missing, activeCampaigns.size(), submissionStatus,
                earnings, performance, needsAttention, recommendedCampaigns, activeCampaigns, topContent,
                recentActivity, recentPayouts);
    }

    private List<ActiveCampaignSummary> buildActiveCampaigns(Long creatorId) {
        List<Campaign> campaigns = campaignRepository.findDistinctCampaignsByCreatorId(creatorId).stream()
                .filter(c -> {
                    CampaignStatus effective = c.getEffectiveStatus();
                    return effective == CampaignStatus.UPCOMING || effective == CampaignStatus.LIVE;
                })
                .toList();
        if (campaigns.isEmpty()) {
            return List.of();
        }
        Map<Long, CampaignAggregateRow> aggregates = contentRepository.sumCampaignAggregatesByCreatorId(creatorId).stream()
                .collect(Collectors.toMap(CampaignAggregateRow::getCampaignId, row -> row));
        return campaigns.stream()
                .map(c -> {
                    CampaignAggregateRow agg = aggregates.get(c.getId());
                    long views = agg != null ? agg.getViews() : 0L;
                    BigDecimal earningsInr = agg != null ? agg.getEarnings() : BigDecimal.ZERO;
                    return new ActiveCampaignSummary(c.getId(), c.getTitle(), c.getEffectiveStatus().name(), views, earningsInr);
                })
                .toList();
    }

    private List<NeedsAttentionItem> buildNeedsAttention(Long creatorId, KycStatus kycStatus, int profileCompletionPercent,
                                                           ContentStatusCounts counts) {
        List<NeedsAttentionItem> items = new ArrayList<>();

        if (counts.changesRequested() > 0) {
            List<Content> changesRequested = contentRepository.findByCreator_IdAndStatus(creatorId, ContentStatus.CHANGES_REQUESTED);
            Content mostRecent = changesRequested.stream().max(Comparator.comparing(Content::getUpdatedAt)).orElse(null);
            String message;
            if (changesRequested.size() == 1 && mostRecent != null) {
                String note = mostRecent.getReviewNotes().isEmpty() ? null
                        : mostRecent.getReviewNotes().get(mostRecent.getReviewNotes().size() - 1).getNoteText();
                message = "Changes requested on \"" + mostRecent.getCampaign().getTitle() + "\""
                        + (isBlank(note) ? "." : ": " + note);
            } else {
                message = changesRequested.size() + " submissions need changes before they can be resubmitted.";
            }
            items.add(new NeedsAttentionItem(message, "Review submissions", "/creator/content"));
        }

        if (counts.rejected() > 0) {
            items.add(new NeedsAttentionItem(
                    counts.rejected() + (counts.rejected() == 1 ? " submission was rejected." : " submissions were rejected."),
                    "View submissions", "/creator/content"));
        }

        if (kycStatus != KycStatus.VERIFIED) {
            String message = kycStatus == null
                    ? "Complete your KYC verification to become eligible for payouts."
                    : kycStatus == KycStatus.REJECTED
                    ? "Your KYC submission was rejected - please resubmit."
                    : "Your KYC verification is still pending review.";
            items.add(new NeedsAttentionItem(message, "Go to KYC", "/creator/kyc"));
        }

        if (profileCompletionPercent < 100) {
            items.add(new NeedsAttentionItem(
                    "Your profile is " + profileCompletionPercent + "% complete.",
                    "Complete profile", "/creator/profile/edit"));
        }

        return items;
    }

    private List<RecentActivityItem> buildRecentActivity(Long creatorId) {
        List<Content> recentContent = contentRepository
                .findByCreator_Id(creatorId, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt")))
                .getContent();
        List<Payout> recentPayoutChanges = payoutRepository
                .findByCreator_Id(creatorId, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "calculatedAt")))
                .getContent();

        List<RecentActivityItem> items = new ArrayList<>();
        for (Content c : recentContent) {
            items.add(new RecentActivityItem(contentActivityMessage(c), c.getUpdatedAt()));
        }
        for (Payout p : recentPayoutChanges) {
            items.add(new RecentActivityItem(payoutActivityMessage(p), p.getCalculatedAt()));
        }
        return items.stream()
                .sorted(Comparator.comparing(RecentActivityItem::occurredAt).reversed())
                .limit(5)
                .toList();
    }

    private static String contentActivityMessage(Content c) {
        String title = c.getCampaign().getTitle();
        return switch (c.getStatus()) {
            case DRAFT -> "You started a draft submission for \"" + title + "\".";
            case SUBMITTED -> "Your submission for \"" + title + "\" was sent for review.";
            case CHANGES_REQUESTED -> "Changes were requested on your \"" + title + "\" submission.";
            case APPROVED -> "Your \"" + title + "\" submission was approved.";
            case REJECTED -> "Your \"" + title + "\" submission was rejected.";
            case PENDING_LINK_REVIEW -> "Your live post link for \"" + title + "\" is awaiting review.";
            case PUBLISHED -> "Your \"" + title + "\" content is now published.";
        };
    }

    private static String payoutActivityMessage(Payout p) {
        String title = p.getCampaign().getTitle();
        return switch (p.getStatus()) {
            case PENDING_KYC -> "A ₹" + p.getAmountInr() + " payout for \"" + title + "\" is waiting on your KYC verification.";
            case PAYABLE -> "A ₹" + p.getAmountInr() + " payout for \"" + title + "\" is now payable.";
            case PAID -> "You were paid ₹" + p.getAmountInr() + " for \"" + title + "\".";
            case BELOW_THRESHOLD -> "\"" + title + "\" hasn't reached the view threshold for a payout yet.";
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

    private static Instant startOfCurrentMonth() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private CreatorProfile findByUserId(Long userId) {
        return creatorProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator profile not found for user id " + userId));
    }
}
