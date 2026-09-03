package club.escobar.service;

import club.escobar.dto.creator.CreatorDashboardResponse;
import club.escobar.entity.CreatorKycProfile;
import club.escobar.entity.CreatorProfile;
import club.escobar.entity.User;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.UserRole;
import club.escobar.mapper.CreatorProfileMapper;
import club.escobar.repository.CampaignRepository;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.CreatorKycProfileRepository;
import club.escobar.repository.CreatorProfileRepository;
import club.escobar.repository.MetricsRollupRow;
import club.escobar.repository.PayoutRepository;
import club.escobar.service.impl.CreatorProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatorProfileServiceImplTest {

    @Mock
    private CreatorProfileRepository creatorProfileRepository;
    @Mock
    private CreatorProfileMapper creatorProfileMapper;
    @Mock
    private CreatorKycProfileRepository creatorKycProfileRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private PayoutRepository payoutRepository;
    @Mock
    private CampaignService campaignService;
    @Mock
    private MetricsRollupRow zeroMetrics;

    @InjectMocks
    private CreatorProfileServiceImpl creatorProfileService;

    private User creator;

    @BeforeEach
    void setUp() {
        creator = User.builder().id(1L).email("creator@test.com").role(UserRole.CREATOR).build();

        when(contentRepository.countByCreator_IdAndStatus(eq(1L), any())).thenReturn(0L);
        when(payoutRepository.sumAmountInrByCreator_IdAndStatus(eq(1L), any())).thenReturn(BigDecimal.ZERO);
        when(payoutRepository.sumAmountInrByCreator_IdAndStatusAndPaidAtAfter(eq(1L), any(), any(Instant.class)))
                .thenReturn(BigDecimal.ZERO);
        when(zeroMetrics.getViews()).thenReturn(0L);
        when(zeroMetrics.getLikes()).thenReturn(0L);
        when(zeroMetrics.getComments()).thenReturn(0L);
        when(zeroMetrics.getPublishedCount()).thenReturn(0L);
        when(contentRepository.sumMetricsByCreatorAndPublishedAtAfter(eq(1L), any(Instant.class))).thenReturn(zeroMetrics);
        when(campaignService.listRecommendedForCreator(1L, 5)).thenReturn(List.of());
        when(campaignRepository.findDistinctCampaignsByCreatorId(1L)).thenReturn(List.of());
        when(contentRepository.findTopContentByCreator(eq(1L), any(Pageable.class))).thenReturn(List.of());
        when(contentRepository.findByCreator_Id(eq(1L), any(Pageable.class))).thenReturn(Page.empty());
        when(payoutRepository.findByCreator_Id(eq(1L), any(Pageable.class))).thenReturn(Page.empty());
    }

    @Test
    void dashboard_emptyProfile_reportsZeroCompletionAndAllSixItemsMissing() {
        CreatorProfile profile = CreatorProfile.builder().id(1L).user(creator).displayName("New Creator")
                .bio(null).profilePictureUrl(null).niche(null).instagramProfileUrl("").portfolioLinks(List.of()).build();
        when(creatorProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.empty());

        CreatorDashboardResponse result = creatorProfileService.dashboard(1L);

        assertThat(result.profileCompletionPercent()).isZero();
        assertThat(result.profileCompletionMissing()).containsExactlyInAnyOrder(
                "Profile photo", "Bio", "Niche", "Instagram link", "Portfolio link", "KYC verification");
        assertThat(result.kycStatus()).isNull();
        // Not-verified-KYC and incomplete-profile items only - no submissions exist yet.
        assertThat(result.needsAttention()).hasSize(2);
        assertThat(result.activeCampaignsCount()).isZero();
        assertThat(result.recommendedCampaigns()).isEmpty();
        assertThat(result.topContent()).isEmpty();
        assertThat(result.earnings().paidInr()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void dashboard_fullyCompleteVerifiedProfile_reports100PercentAndNoProfileOrKycAttentionItems() {
        CreatorProfile profile = CreatorProfile.builder().id(1L).user(creator).displayName("Established Creator")
                .bio("I make content").profilePictureUrl("https://cdn/pic.jpg").niche("Fashion")
                .instagramProfileUrl("https://instagram.com/creator").portfolioLinks(List.of("https://example.com/portfolio"))
                .build();
        when(creatorProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));
        CreatorKycProfile kyc = CreatorKycProfile.builder().id(1L).creator(creator).status(KycStatus.VERIFIED).build();
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.of(kyc));

        CreatorDashboardResponse result = creatorProfileService.dashboard(1L);

        assertThat(result.profileCompletionPercent()).isEqualTo(100);
        assertThat(result.profileCompletionMissing()).isEmpty();
        assertThat(result.kycStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(result.needsAttention()).isEmpty();
    }
}
