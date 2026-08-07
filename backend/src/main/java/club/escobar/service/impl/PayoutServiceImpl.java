package club.escobar.service.impl;

import club.escobar.dto.common.PageResponse;
import club.escobar.dto.payout.PayoutMarkPaidRequest;
import club.escobar.dto.payout.PayoutResponse;
import club.escobar.entity.Content;
import club.escobar.entity.Payout;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.PayoutStatus;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.mapper.PayoutMapper;
import club.escobar.repository.ContentMetricsSnapshotRepository;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.CreatorKycProfileRepository;
import club.escobar.repository.PayoutRepository;
import club.escobar.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutServiceImpl.class);
    private static final long ELIGIBILITY_THRESHOLD_VIEWS = 5_000L;

    private final PayoutRepository payoutRepository;
    private final ContentRepository contentRepository;
    private final ContentMetricsSnapshotRepository contentMetricsSnapshotRepository;
    private final CreatorKycProfileRepository creatorKycProfileRepository;
    private final PayoutMapper payoutMapper;

    @Override
    @Transactional
    public void recalculate(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id " + contentId));
        if (content.getStatus() != ContentStatus.PUBLISHED) {
            return;
        }

        long latestViews = contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(contentId)
                .map(s -> s.getViewCount() == null ? 0L : s.getViewCount())
                .orElse(0L);

        Payout payout = payoutRepository.findByContent_Id(contentId)
                .orElseGet(() -> Payout.builder()
                        .content(content)
                        .creator(content.getCreator())
                        .campaign(content.getCampaign())
                        .business(content.getBusiness())
                        .build());

        payout.setViewCountUsed(latestViews);
        payout.setRateUsed(content.getCampaign().getRatePerThousandViewsInr());

        if (latestViews < ELIGIBILITY_THRESHOLD_VIEWS) {
            payout.setAmountInr(BigDecimal.ZERO);
            payout.setStatus(PayoutStatus.BELOW_THRESHOLD);
        } else {
            BigDecimal amount = payout.getRateUsed()
                    .multiply(BigDecimal.valueOf(latestViews))
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            payout.setAmountInr(amount);
            if (payout.getEligibleAt() == null) {
                payout.setEligibleAt(Instant.now());
            }

            // Only an admin-verified KYC unlocks payment - a business's own peer review of a
            // creator's KYC (scoped to their content relationship) is not sufficient on its own.
            boolean kycVerified = creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(
                    content.getCreator().getId(), KycStatus.VERIFIED, UserRole.ADMIN);

            if (payout.getStatus() != PayoutStatus.PAID) {
                payout.setStatus(kycVerified ? PayoutStatus.PAYABLE : PayoutStatus.PENDING_KYC);
            }
        }

        payoutRepository.save(payout);
        log.info("Recalculated payout for content id={}: views={} amount={} status={}",
                contentId, latestViews, payout.getAmountInr(), payout.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutResponse getForContent(Long requestingUserId, Long contentId) {
        Payout payout = findByContentId(contentId);
        if (!payout.getCreator().getId().equals(requestingUserId) && !payout.getBusiness().getId().equals(requestingUserId)) {
            throw new ForbiddenActionException("You do not have access to this content's payout");
        }
        return payoutMapper.toResponse(payout);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PayoutResponse> listForBusiness(Long requestingUserId, Long businessId, PayoutStatus status, Pageable pageable) {
        if (!requestingUserId.equals(businessId)) {
            throw new ForbiddenActionException("You may only view your own payouts");
        }
        Page<Payout> page = status != null
                ? payoutRepository.findByBusiness_IdAndStatus(businessId, status, pageable)
                : payoutRepository.findByBusiness_Id(businessId, pageable);
        return PageResponse.of(page.map(payoutMapper::toResponse));
    }

    @Override
    @Transactional
    public PayoutResponse markPaid(Long businessUserId, Long contentId, PayoutMarkPaidRequest request) {
        Payout payout = findByContentId(contentId);
        if (!payout.getBusiness().getId().equals(businessUserId)) {
            throw new ForbiddenActionException("You may only mark payouts for your own business as paid");
        }
        if (payout.getStatus() != PayoutStatus.PAYABLE) {
            throw new InvalidStateTransitionException(
                    "Cannot mark a payout as paid unless it is currently PAYABLE (current status: " + payout.getStatus() + ")");
        }

        payout.setStatus(PayoutStatus.PAID);
        payout.setPaidAt(Instant.now());
        payout.setPaidNote(request.paidNote());

        Payout saved = payoutRepository.save(payout);
        log.info("Business id={} marked payout for content id={} as paid", businessUserId, contentId);
        return payoutMapper.toResponse(saved);
    }

    private Payout findByContentId(Long contentId) {
        return payoutRepository.findByContent_Id(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found for content id " + contentId));
    }
}
