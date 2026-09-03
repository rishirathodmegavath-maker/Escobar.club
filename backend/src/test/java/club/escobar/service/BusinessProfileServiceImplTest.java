package club.escobar.service;

import club.escobar.entity.BusinessProfile;
import club.escobar.entity.User;
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.PayoutStatus;
import club.escobar.entity.enums.UserRole;
import club.escobar.dto.business.BusinessDashboardResponse;
import club.escobar.mapper.BusinessProfileMapper;
import club.escobar.repository.BusinessProfileRepository;
import club.escobar.repository.CampaignRepository;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.MetricsRollupRow;
import club.escobar.repository.PayoutRepository;
import club.escobar.service.impl.BusinessProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessProfileServiceImplTest {

    @Mock
    private BusinessProfileRepository businessProfileRepository;
    @Mock
    private BusinessProfileMapper businessProfileMapper;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private PayoutRepository payoutRepository;
    @Mock
    private MetricsRollupRow emptyMetrics;

    private BusinessProfileServiceImpl businessProfileService;

    private User business;
    private BusinessProfile profile;

    @BeforeEach
    void setUp() {
        businessProfileService = new BusinessProfileServiceImpl(
                businessProfileRepository, businessProfileMapper, campaignRepository, contentRepository, payoutRepository);
        business = User.builder().id(2L).email("business@test.com").role(UserRole.BUSINESS).build();
        profile = BusinessProfile.builder().id(2L).user(business).companyName("Acme")
                .approvalStatus(ApprovalStatus.APPROVED).build();
    }

    @Test
    void dashboard_assemblesCountsFromRepositories() {
        when(businessProfileRepository.findByUser_Id(2L)).thenReturn(Optional.of(profile));
        when(campaignRepository.countByBusiness_Id(2L)).thenReturn(5L);
        when(campaignRepository.countLiveByBusinessId(2L)).thenReturn(2L);
        when(campaignRepository.countByBusiness_IdAndApprovalStatus(2L, ApprovalStatus.PENDING)).thenReturn(1L);
        when(contentRepository.countByBusiness_IdAndStatus(2L, ContentStatus.SUBMITTED)).thenReturn(3L);
        when(contentRepository.countByBusiness_IdAndStatus(2L, ContentStatus.CHANGES_REQUESTED)).thenReturn(1L);
        when(contentRepository.countByBusiness_IdAndStatus(2L, ContentStatus.PENDING_LINK_REVIEW)).thenReturn(1L);
        when(contentRepository.countByBusiness_IdAndStatus(2L, ContentStatus.PUBLISHED)).thenReturn(4L);
        when(contentRepository.countByBusiness_IdAndStatus(2L, ContentStatus.APPROVED)).thenReturn(2L);
        when(contentRepository.countByBusiness_IdAndStatus(2L, ContentStatus.REJECTED)).thenReturn(1L);
        when(payoutRepository.countByBusiness_IdAndStatus(2L, PayoutStatus.PAYABLE)).thenReturn(2L);
        when(payoutRepository.sumAmountInrByBusiness_IdAndStatus(2L, PayoutStatus.PAYABLE)).thenReturn(new BigDecimal("2500.00"));
        when(payoutRepository.countByBusiness_IdAndStatus(2L, PayoutStatus.PENDING_KYC)).thenReturn(1L);
        when(payoutRepository.sumAmountInrByBusiness_IdAndStatus(2L, PayoutStatus.PAID)).thenReturn(new BigDecimal("10000.00"));
        when(campaignRepository.countNearBudgetCapByBusinessId(eq(2L), any(), any())).thenReturn(1L);

        when(campaignRepository.sumMaxBudgetInrByBusinessId(2L)).thenReturn(new BigDecimal("50000.00"));
        when(payoutRepository.sumAmountInrByBusiness_IdAndStatusIn(eq(2L), any())).thenReturn(new BigDecimal("12000.00"));
        when(contentRepository.countDistinctCreatorsByBusinessId(2L)).thenReturn(3L);

        when(emptyMetrics.getViews()).thenReturn(0L);
        when(emptyMetrics.getLikes()).thenReturn(0L);
        when(emptyMetrics.getComments()).thenReturn(0L);
        when(emptyMetrics.getPublishedCount()).thenReturn(0L);
        when(contentRepository.sumMetricsByBusinessAndPublishedAtAfter(eq(2L), any(Instant.class))).thenReturn(emptyMetrics);

        when(campaignRepository.findByBusiness_Id(eq(2L), any(Pageable.class))).thenReturn(Page.empty());
        when(campaignRepository.findNearBudgetCapByBusinessId(eq(2L), any(), any(), any())).thenReturn(List.of());
        when(contentRepository.findByBusiness_Id(eq(2L), any(Pageable.class))).thenReturn(Page.empty());
        when(contentRepository.findTopContentByBusiness(eq(2L), any(Pageable.class))).thenReturn(List.of());

        BusinessDashboardResponse result = businessProfileService.dashboard(2L);

        assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(result.totalCampaigns()).isEqualTo(5L);
        assertThat(result.liveCampaigns()).isEqualTo(2L);
        assertThat(result.campaignsPendingApproval()).isEqualTo(1L);
        assertThat(result.contentAwaitingReview()).isEqualTo(3L);
        assertThat(result.contentChangesRequested()).isEqualTo(1L);
        assertThat(result.contentPendingLinkReview()).isEqualTo(1L);
        assertThat(result.publishedContentCount()).isEqualTo(4L);
        assertThat(result.payoutsPayableCount()).isEqualTo(2L);
        assertThat(result.payoutsPayableAmountInr()).isEqualByComparingTo("2500.00");
        assertThat(result.payoutsPendingKycCount()).isEqualTo(1L);
        assertThat(result.totalPaidOutInr()).isEqualByComparingTo("10000.00");
        assertThat(result.campaignsNearBudgetCap()).isEqualTo(1L);
        assertThat(result.approvedContentCount()).isEqualTo(2L);
        assertThat(result.rejectedContentCount()).isEqualTo(1L);
        assertThat(result.participatingCreatorsCount()).isEqualTo(3L);
        assertThat(result.totalViews()).isZero();
        assertThat(result.totalEngagement()).isZero();
        assertThat(result.totalBudgetInr()).isEqualByComparingTo("50000.00");
        assertThat(result.totalCommittedInr()).isEqualByComparingTo("12000.00");
        assertThat(result.totalRemainingInr()).isEqualByComparingTo("38000.00");
        // 3 submissions awaiting review is the only real signal with everything else stubbed empty.
        assertThat(result.needsAttention()).hasSize(1);
        assertThat(result.campaignsPreview()).isEmpty();
        assertThat(result.creatorActivity()).isEmpty();
        assertThat(result.topContent()).isEmpty();
        assertThat(result.performance().allTime().views()).isZero();
    }
}
