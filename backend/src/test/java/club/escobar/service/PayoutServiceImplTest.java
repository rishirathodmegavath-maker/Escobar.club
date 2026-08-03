package club.escobar.service;

import club.escobar.dto.payout.PayoutMarkPaidRequest;
import club.escobar.entity.Campaign;
import club.escobar.entity.Content;
import club.escobar.entity.ContentMetricsSnapshot;
import club.escobar.entity.CreatorKycProfile;
import club.escobar.entity.Payout;
import club.escobar.entity.User;
import club.escobar.entity.enums.CampaignStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.MediaType;
import club.escobar.entity.enums.PayoutStatus;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.mapper.PayoutMapper;
import club.escobar.repository.ContentMetricsSnapshotRepository;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.CreatorKycProfileRepository;
import club.escobar.repository.PayoutRepository;
import club.escobar.service.impl.PayoutServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceImplTest {

    @Mock
    private PayoutRepository payoutRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private ContentMetricsSnapshotRepository contentMetricsSnapshotRepository;
    @Mock
    private CreatorKycProfileRepository creatorKycProfileRepository;
    @Mock
    private PayoutMapper payoutMapper;

    private PayoutServiceImpl service;

    private User creator;
    private User business;
    private Campaign campaign;
    private Content publishedContent;

    @BeforeEach
    void setUp() {
        service = new PayoutServiceImpl(payoutRepository, contentRepository, contentMetricsSnapshotRepository,
                creatorKycProfileRepository, payoutMapper);

        creator = User.builder().id(1L).email("creator@test.com").role(club.escobar.entity.enums.UserRole.CREATOR).build();
        business = User.builder().id(2L).email("business@test.com").role(club.escobar.entity.enums.UserRole.BUSINESS).build();
        campaign = Campaign.builder()
                .id(3L).business(business).title("Summer Launch")
                .submissionOpenAt(LocalDate.now().minusDays(10)).submissionDeadline(LocalDate.now().minusDays(2))
                .publishStartAt(LocalDate.now().minusDays(1)).publishEndAt(LocalDate.now().plusDays(30))
                .ratePerThousandViewsInr(new BigDecimal("500.00")).status(CampaignStatus.PUBLISHED)
                .build();
        publishedContent = Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("post.png").mediaType(MediaType.IMAGE).status(ContentStatus.PUBLISHED)
                .postUrl("https://www.instagram.com/p/Cabc123/").version(1).build();
    }

    private ContentMetricsSnapshot snapshotWithViews(Long views) {
        return ContentMetricsSnapshot.builder().content(publishedContent).likeCount(1L).commentCount(0L).viewCount(views).build();
    }

    @Test
    void recalculate_setsBelowThreshold_whenViewsUnder5000() {
        when(contentRepository.findById(20L)).thenReturn(Optional.of(publishedContent));
        when(contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(20L))
                .thenReturn(Optional.of(snapshotWithViews(3200L)));
        when(payoutRepository.findByContent_Id(20L)).thenReturn(Optional.empty());
        when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recalculate(20L);

        ArgumentCaptor<Payout> captor = ArgumentCaptor.forClass(Payout.class);
        verify(payoutRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PayoutStatus.BELOW_THRESHOLD);
        assertThat(captor.getValue().getAmountInr()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void recalculate_setsPendingKyc_whenAboveThresholdButKycNotVerified() {
        when(contentRepository.findById(20L)).thenReturn(Optional.of(publishedContent));
        when(contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(20L))
                .thenReturn(Optional.of(snapshotWithViews(8000L)));
        when(payoutRepository.findByContent_Id(20L)).thenReturn(Optional.empty());
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.empty());
        when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recalculate(20L);

        ArgumentCaptor<Payout> captor = ArgumentCaptor.forClass(Payout.class);
        verify(payoutRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PayoutStatus.PENDING_KYC);
        assertThat(captor.getValue().getAmountInr()).isEqualByComparingTo(new BigDecimal("4000.00"));
    }

    @Test
    void recalculate_setsPayable_whenAboveThresholdAndKycVerified() {
        CreatorKycProfile kyc = CreatorKycProfile.builder().creator(creator).status(KycStatus.VERIFIED).build();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(publishedContent));
        when(contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(20L))
                .thenReturn(Optional.of(snapshotWithViews(8000L)));
        when(payoutRepository.findByContent_Id(20L)).thenReturn(Optional.empty());
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.of(kyc));
        when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recalculate(20L);

        ArgumentCaptor<Payout> captor = ArgumentCaptor.forClass(Payout.class);
        verify(payoutRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PayoutStatus.PAYABLE);
        assertThat(captor.getValue().getAmountInr()).isEqualByComparingTo(new BigDecimal("4000.00"));
    }

    @Test
    void recalculate_appliesRateCorrectly_withHalfUpRounding() {
        when(contentRepository.findById(20L)).thenReturn(Optional.of(publishedContent));
        when(contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(20L))
                .thenReturn(Optional.of(snapshotWithViews(12345L)));
        when(payoutRepository.findByContent_Id(20L)).thenReturn(Optional.empty());
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.empty());
        when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recalculate(20L);

        ArgumentCaptor<Payout> captor = ArgumentCaptor.forClass(Payout.class);
        verify(payoutRepository).save(captor.capture());
        // rate 500.00 * 12345 / 1000 = 6172.50
        assertThat(captor.getValue().getAmountInr()).isEqualByComparingTo(new BigDecimal("6172.50"));
    }

    @Test
    void recalculate_recomputesOnRepeatedSync_withoutDuplicatingRow() {
        Payout existing = Payout.builder().id(99L).content(publishedContent).creator(creator).campaign(campaign).business(business)
                .viewCountUsed(3000L).rateUsed(new BigDecimal("500.00")).amountInr(BigDecimal.ZERO)
                .status(PayoutStatus.BELOW_THRESHOLD).build();

        when(contentRepository.findById(20L)).thenReturn(Optional.of(publishedContent));
        when(contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(20L))
                .thenReturn(Optional.of(snapshotWithViews(9000L)));
        when(payoutRepository.findByContent_Id(20L)).thenReturn(Optional.of(existing));
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.empty());
        when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recalculate(20L);

        ArgumentCaptor<Payout> captor = ArgumentCaptor.forClass(Payout.class);
        verify(payoutRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(99L);
        assertThat(captor.getValue().getViewCountUsed()).isEqualTo(9000L);
        assertThat(captor.getValue().getStatus()).isEqualTo(PayoutStatus.PENDING_KYC);
    }

    @Test
    void markPaid_transitionsPayableToPaid() {
        Payout payout = Payout.builder().id(99L).content(publishedContent).creator(creator).campaign(campaign).business(business)
                .viewCountUsed(9000L).rateUsed(new BigDecimal("500.00")).amountInr(new BigDecimal("4500.00"))
                .status(PayoutStatus.PAYABLE).build();
        when(payoutRepository.findByContent_Id(20L)).thenReturn(Optional.of(payout));
        when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markPaid(2L, 20L, new PayoutMarkPaidRequest("Paid via bank transfer"));

        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.PAID);
        assertThat(payout.getPaidAt()).isNotNull();
        assertThat(payout.getPaidNote()).isEqualTo("Paid via bank transfer");
    }

    @Test
    void markPaid_rejectsFromNonPayableStatus() {
        Payout payout = Payout.builder().id(99L).content(publishedContent).creator(creator).campaign(campaign).business(business)
                .status(PayoutStatus.PENDING_KYC).build();
        when(payoutRepository.findByContent_Id(20L)).thenReturn(Optional.of(payout));

        assertThatThrownBy(() -> service.markPaid(2L, 20L, new PayoutMarkPaidRequest(null)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void markPaid_rejectsWhenNotOwningBusiness() {
        Payout payout = Payout.builder().id(99L).content(publishedContent).creator(creator).campaign(campaign).business(business)
                .status(PayoutStatus.PAYABLE).build();
        when(payoutRepository.findByContent_Id(20L)).thenReturn(Optional.of(payout));

        assertThatThrownBy(() -> service.markPaid(999L, 20L, new PayoutMarkPaidRequest(null)))
                .isInstanceOf(ForbiddenActionException.class);
    }
}
