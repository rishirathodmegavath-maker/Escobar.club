package club.escobar.service;

import club.escobar.dto.payout.PayoutResponse;
import club.escobar.entity.Campaign;
import club.escobar.entity.Content;
import club.escobar.entity.ContentMetricsSnapshot;
import club.escobar.entity.Payout;
import club.escobar.entity.User;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.PayoutStatus;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.ForbiddenActionException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        // lenient: only the recalculate_* tests below exercise this fixture - the listForBusinessAll_*
        // tests use a different mock path entirely, and strict stubbing would otherwise flag these as
        // unused stubs for those tests.
        lenient().when(contentRepository.findById(10L)).thenReturn(Optional.of(content));
        lenient().when(contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(10L))
                .thenReturn(Optional.of(ContentMetricsSnapshot.builder().viewCount(9000L).build()));
        lenient().when(payoutRepository.findByContent_Id(10L)).thenReturn(Optional.empty());
        lenient().when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));
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

    @Test
    void listForBusinessAll_returnsEveryMatchingRowUnpaged() {
        Payout payout = Payout.builder().id(50L).content(content).creator(creator).campaign(campaign).business(business)
                .status(PayoutStatus.PAYABLE).amountInr(new BigDecimal("900.00")).build();
        when(payoutRepository.findByBusiness_IdAndStatus(eq(2L), eq(PayoutStatus.PAYABLE), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable pageable = inv.getArgument(2);
                    assertThat(pageable.isUnpaged()).isTrue();
                    return new PageImpl<>(List.of(payout));
                });
        when(payoutMapper.toResponse(payout)).thenReturn(mock(PayoutResponse.class));

        List<PayoutResponse> result = payoutService.listForBusinessAll(2L, 2L, PayoutStatus.PAYABLE);

        assertThat(result).hasSize(1);
    }

    @Test
    void listForBusinessAll_rejectsWhenNotOwnBusiness() {
        assertThatThrownBy(() -> payoutService.listForBusinessAll(999L, 2L, PayoutStatus.PAYABLE))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void listForCreator_returnsOwnPayouts_filteredByStatus() {
        Payout payout = Payout.builder().id(50L).content(content).creator(creator).campaign(campaign).business(business)
                .status(PayoutStatus.PAYABLE).amountInr(new BigDecimal("900.00")).build();
        when(payoutRepository.findByCreator_IdAndStatus(eq(1L), eq(PayoutStatus.PAYABLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payout)));
        when(payoutMapper.toResponse(payout)).thenReturn(mock(PayoutResponse.class));

        var result = payoutService.listForCreator(1L, 1L, PayoutStatus.PAYABLE, Pageable.unpaged());

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void listForCreator_rejectsWhenNotOwnPayouts() {
        assertThatThrownBy(() -> payoutService.listForCreator(999L, 1L, null, Pageable.unpaged()))
                .isInstanceOf(ForbiddenActionException.class);
    }
}
