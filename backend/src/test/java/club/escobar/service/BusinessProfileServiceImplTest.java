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
import club.escobar.repository.PayoutRepository;
import club.escobar.service.impl.BusinessProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
        when(payoutRepository.countByBusiness_IdAndStatus(2L, PayoutStatus.PAYABLE)).thenReturn(2L);
        when(payoutRepository.sumAmountInrByBusiness_IdAndStatus(2L, PayoutStatus.PAYABLE)).thenReturn(new BigDecimal("2500.00"));
        when(payoutRepository.countByBusiness_IdAndStatus(2L, PayoutStatus.PENDING_KYC)).thenReturn(1L);
        when(payoutRepository.sumAmountInrByBusiness_IdAndStatus(2L, PayoutStatus.PAID)).thenReturn(new BigDecimal("10000.00"));
        when(campaignRepository.countNearBudgetCapByBusinessId(eq(2L), any(), any())).thenReturn(1L);

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
    }
}
