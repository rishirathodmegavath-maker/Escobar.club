package club.escobar.service;

import club.escobar.entity.Campaign;
import club.escobar.entity.Content;
import club.escobar.entity.ContentMetricsSnapshot;
import club.escobar.entity.Payout;
import club.escobar.entity.User;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.PayoutStatus;
import club.escobar.entity.enums.UserRole;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    private PayoutServiceImpl payoutService;

    private User creator;
    private User business;
    private Campaign campaign;
    private Content content;

    @BeforeEach
    void setUp() {
        payoutService = new PayoutServiceImpl(payoutRepository, contentRepository,
                contentMetricsSnapshotRepository, creatorKycProfileRepository, payoutMapper);
        creator = User.builder().id(1L).email("creator@test.com").role(UserRole.CREATOR).build();
        business = User.builder().id(2L).email("business@test.com").role(UserRole.BUSINESS).build();
        campaign = Campaign.builder().id(3L).business(business).ratePerThousandViewsInr(new BigDecimal("100.00")).build();
        content = Content.builder().id(10L).creator(creator).campaign(campaign).business(business)
                .status(ContentStatus.PUBLISHED).build();

        when(contentRepository.findById(10L)).thenReturn(Optional.of(content));
        when(contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(10L))
                .thenReturn(Optional.of(ContentMetricsSnapshot.builder().viewCount(9000L).build()));
        when(payoutRepository.findByContent_Id(10L)).thenReturn(Optional.empty());
        when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void recalculate_setsPayable_whenKycVerifiedByAdmin() {
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(true);

        payoutService.recalculate(10L);

        ArgumentCaptor<Payout> captor = ArgumentCaptor.forClass(Payout.class);
        verify(payoutRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PayoutStatus.PAYABLE);
    }

    @Test
    void recalculate_setsPendingKyc_whenKycOnlyBusinessVerifiedNotAdmin() {
        // A business can also mark a creator's KYC VERIFIED (peer review), but that alone
        // must not unlock payment - only an admin's sign-off does.
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(false);

        payoutService.recalculate(10L);

        ArgumentCaptor<Payout> captor = ArgumentCaptor.forClass(Payout.class);
        verify(payoutRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PayoutStatus.PENDING_KYC);
    }

    @Test
    void recalculate_setsPendingKyc_whenNoKycProfileAtAll() {
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(false);

        payoutService.recalculate(10L);

        ArgumentCaptor<Payout> captor = ArgumentCaptor.forClass(Payout.class);
        verify(payoutRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PayoutStatus.PENDING_KYC);
    }
}
