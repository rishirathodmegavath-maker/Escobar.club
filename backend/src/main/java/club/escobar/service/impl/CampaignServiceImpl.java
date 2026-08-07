package club.escobar.service.impl;

import club.escobar.dto.campaign.CampaignCreateRequest;
import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.campaign.CampaignUpdateRequest;
import club.escobar.dto.common.PageResponse;
import club.escobar.entity.BusinessProfile;
import club.escobar.entity.Campaign;
import club.escobar.entity.User;
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.CampaignStatus;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.mapper.CampaignMapper;
import club.escobar.repository.BusinessProfileRepository;
import club.escobar.repository.CampaignRepository;
import club.escobar.repository.UserRepository;
import club.escobar.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignServiceImpl.class);
    private static final Set<CampaignStatus> MANUALLY_SELECTABLE_STATUSES =
            EnumSet.of(CampaignStatus.DRAFT, CampaignStatus.PUBLISHED, CampaignStatus.CANCELLED);

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final CampaignMapper campaignMapper;

    @Override
    @Transactional
    public CampaignResponse create(Long businessUserId, CampaignCreateRequest request) {
        User business = userRepository.findById(businessUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (business.getRole() != UserRole.BUSINESS) {
            throw new ForbiddenActionException("Only businesses can create campaigns");
        }
        validateDateOrdering(request.submissionOpenAt(), request.submissionDeadline(),
                request.publishStartAt(), request.publishEndAt());
        BusinessProfile businessProfile = businessProfileRepository.findByUser_Id(businessUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found"));
        if (businessProfile.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ForbiddenActionException("Your business account must be approved by an admin before you can create campaigns");
        }

        // A newly created campaign is always PUBLISHED (i.e. governed by dates): its phase -
        // Upcoming, Live, or Completed - is computed automatically from the dates the business just
        // entered. DRAFT/CANCELLED can only be set afterward, via an explicit edit.
        Campaign campaign = campaignRepository.save(Campaign.builder()
                .business(business)
                .title(request.title())
                .description(request.description())
                .submissionOpenAt(request.submissionOpenAt())
                .submissionDeadline(request.submissionDeadline())
                .publishStartAt(request.publishStartAt())
                .publishEndAt(request.publishEndAt())
                .ratePerThousandViewsInr(request.ratePerThousandViewsInr())
                .status(CampaignStatus.PUBLISHED)
                .urgent(request.urgent())
                .build());

        log.info("Business id={} created campaign id={}", businessUserId, campaign.getId());
        return campaignMapper.toResponse(campaign);
    }

    @Override
    @Transactional
    public CampaignResponse update(Long businessUserId, Long campaignId, CampaignUpdateRequest request) {
        Campaign campaign = findById(campaignId);
        if (!campaign.getBusiness().getId().equals(businessUserId)) {
            throw new ForbiddenActionException("You may only edit your own campaigns");
        }
        validateDateOrdering(request.submissionOpenAt(), request.submissionDeadline(),
                request.publishStartAt(), request.publishEndAt());
        if (!MANUALLY_SELECTABLE_STATUSES.contains(request.status())) {
            throw new InvalidStateTransitionException(
                    "Status must be Draft, Published (automatic), or Cancelled - Upcoming/Live/Completed are computed from dates");
        }

        campaign.setTitle(request.title());
        campaign.setDescription(request.description());
        campaign.setSubmissionOpenAt(request.submissionOpenAt());
        campaign.setSubmissionDeadline(request.submissionDeadline());
        campaign.setPublishStartAt(request.publishStartAt());
        campaign.setPublishEndAt(request.publishEndAt());
        campaign.setRatePerThousandViewsInr(request.ratePerThousandViewsInr());
        campaign.setStatus(request.status());
        campaign.setUrgent(request.urgent());

        Campaign saved = campaignRepository.save(campaign);
        log.info("Business id={} updated campaign id={} (status={})", businessUserId, campaignId, request.status());
        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CampaignResponse> listPublic(String search, String category, Pageable pageable) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        BigDecimal rateThreshold = computeActiveRateThreshold();

        Predicate<Campaign> categoryFilter = categoryFilterFor(category);
        if (categoryFilter != null) {
            List<Campaign> matches = campaignRepository
                    .searchPublicByStatus(normalizedSearch, CampaignStatus.PUBLISHED, Pageable.unpaged())
                    .getContent()
                    .stream()
                    .filter(categoryFilter)
                    .toList();
            boolean forceHot = "HOT".equalsIgnoreCase(category);
            return PageResponse.of(paginate(matches, pageable)
                    .map(c -> withHot(campaignMapper.toResponse(c), forceHot || c.isHot(rateThreshold))));
        }

        Page<Campaign> page = campaignRepository.searchPublic(normalizedSearch, pageable);
        return PageResponse.of(page.map(c -> withHot(campaignMapper.toResponse(c), c.isHot(rateThreshold))));
    }

    // Null for the default/no-category listing, which falls back to the plain searchPublic() page.
    private Predicate<Campaign> categoryFilterFor(String category) {
        if (category == null) {
            return null;
        }
        return switch (category.toUpperCase()) {
            case "HOT" -> c -> c.isHot(computeActiveRateThreshold());
            // Matches the Discover page's Upcoming tab contract exactly: still ahead of publishing,
            // and its submission deadline hasn't already lapsed (a campaign whose window closed before
            // publishing starts falls into neither Upcoming nor Live and simply drops off Discover).
            case "UPCOMING" -> c -> LocalDate.now().isBefore(c.getPublishStartAt())
                    && !LocalDate.now().isAfter(c.getSubmissionDeadline());
            case "LIVE" -> c -> c.getEffectiveStatus() == CampaignStatus.LIVE;
            case "COMPLETED" -> c -> c.getEffectiveStatus() == CampaignStatus.COMPLETED;
            default -> null;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CampaignResponse> listMine(Long businessUserId, Pageable pageable) {
        BigDecimal rateThreshold = computeActiveRateThreshold();
        Page<CampaignResponse> page = campaignRepository.findByBusiness_Id(businessUserId, pageable)
                .map(c -> withHot(campaignMapper.toResponse(c), c.isHot(rateThreshold)));
        return PageResponse.of(page);
    }

    private static CampaignResponse withHot(CampaignResponse response, boolean hot) {
        return new CampaignResponse(response.id(), response.businessId(), response.businessCompanyName(),
                response.businessLogoUrl(), response.title(), response.description(),
                response.submissionOpenAt(), response.submissionDeadline(), response.publishStartAt(),
                response.publishEndAt(), response.ratePerThousandViewsInr(), response.status(),
                response.acceptingSubmissions(), response.urgent(), hot, response.approvalStatus(),
                response.adminDisplayStatus(), response.createdAt(), response.updatedAt());
    }

    // Top-quartile rate among campaigns currently open for creator submissions, used as one of the
    // "Hot" criteria. Null when there are fewer than 2 such campaigns to compare against.
    private BigDecimal computeActiveRateThreshold() {
        List<BigDecimal> rates = campaignRepository.findActiveRates();
        if (rates.size() < 2) {
            return null;
        }
        List<BigDecimal> sorted = rates.stream().sorted(Comparator.naturalOrder()).toList();
        int index = (int) Math.floor(0.75 * (sorted.size() - 1));
        return sorted.get(index);
    }

    private Page<Campaign> paginate(List<Campaign> campaigns, Pageable pageable) {
        int start = Math.min((int) pageable.getOffset(), campaigns.size());
        int end = Math.min(start + pageable.getPageSize(), campaigns.size());
        return new PageImpl<>(campaigns.subList(start, end), pageable, campaigns.size());
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getById(Long campaignId) {
        Campaign campaign = findById(campaignId);
        return withHot(campaignMapper.toResponse(campaign), campaign.isHot(computeActiveRateThreshold()));
    }

    private Campaign findById(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id " + campaignId));
    }

    private void validateDateOrdering(LocalDate submissionOpenAt, LocalDate submissionDeadline,
                                       LocalDate publishStartAt, LocalDate publishEndAt) {
        if (submissionDeadline.isBefore(submissionOpenAt)) {
            throw new InvalidStateTransitionException("Submission deadline cannot be before submissions open");
        }
        if (publishStartAt.isBefore(submissionDeadline)) {
            throw new InvalidStateTransitionException("Publishing cannot start before the submission deadline");
        }
        if (publishEndAt.isBefore(publishStartAt)) {
            throw new InvalidStateTransitionException("Publishing end date cannot be before its start date");
        }
    }
}
