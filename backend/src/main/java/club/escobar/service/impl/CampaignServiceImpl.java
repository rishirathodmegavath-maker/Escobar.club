package club.escobar.service.impl;

import club.escobar.dto.campaign.CampaignCreateRequest;
import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.campaign.CampaignUpdateRequest;
import club.escobar.dto.common.PageResponse;
import club.escobar.entity.Campaign;
import club.escobar.entity.User;
import club.escobar.entity.enums.CampaignStatus;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.mapper.CampaignMapper;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignServiceImpl.class);

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final CampaignMapper campaignMapper;

    @Override
    @Transactional
    public CampaignResponse create(Long businessUserId, CampaignCreateRequest request) {
        User business = userRepository.findById(businessUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (business.getRole() != UserRole.BUSINESS) {
            throw new ForbiddenActionException("Only businesses can create campaigns");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidStateTransitionException("Campaign end date cannot be before its start date");
        }

        Campaign campaign = campaignRepository.save(Campaign.builder()
                .business(business)
                .title(request.title())
                .description(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .ratePerThousandViewsInr(request.ratePerThousandViewsInr())
                .status(initialStatusFor(request.startDate(), request.endDate()))
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
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidStateTransitionException("Campaign end date cannot be before its start date");
        }

        campaign.setTitle(request.title());
        campaign.setDescription(request.description());
        campaign.setStartDate(request.startDate());
        campaign.setEndDate(request.endDate());
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

        if ("HOT".equalsIgnoreCase(category)) {
            List<Campaign> hotCampaigns = campaignRepository
                    .searchPublicByStatus(normalizedSearch, CampaignStatus.ACTIVE, Pageable.unpaged())
                    .getContent()
                    .stream()
                    .filter(c -> c.isHot(rateThreshold))
                    .toList();
            return PageResponse.of(paginate(hotCampaigns, pageable)
                    .map(c -> withHot(campaignMapper.toResponse(c), true)));
        }

        Page<Campaign> page = "LIVE".equalsIgnoreCase(category)
                ? campaignRepository.searchPublicByStatus(normalizedSearch, CampaignStatus.ACTIVE, pageable)
                : "UPCOMING".equalsIgnoreCase(category)
                        ? campaignRepository.searchPublicByStatus(normalizedSearch, CampaignStatus.STARTING_SOON, pageable)
                        : campaignRepository.searchPublic(normalizedSearch, pageable);

        return PageResponse.of(page.map(c -> withHot(campaignMapper.toResponse(c), c.isHot(rateThreshold))));
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
                response.businessLogoUrl(), response.title(), response.description(), response.startDate(),
                response.endDate(), response.ratePerThousandViewsInr(), response.status(),
                response.acceptingSubmissions(), response.urgent(), hot, response.createdAt(), response.updatedAt());
    }

    // Top-quartile rate among currently-live (ACTIVE) campaigns, used as one of the "Hot" criteria.
    // Null when there are fewer than 2 live campaigns to compare against.
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

    // A newly created campaign is never left in DRAFT: it opens as either "starting soon" (start date
    // still ahead) or immediately ACTIVE (start date already within range), and CLOSED only if the whole
    // window is already in the past.
    private CampaignStatus initialStatusFor(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (today.isAfter(endDate)) {
            return CampaignStatus.CLOSED;
        }
        if (today.isBefore(startDate)) {
            return CampaignStatus.STARTING_SOON;
        }
        return CampaignStatus.ACTIVE;
    }
}
