package club.escobar.service;

import club.escobar.dto.wallet.AddMoneyRequest;
import club.escobar.dto.wallet.AdminCreditRequest;
import club.escobar.dto.wallet.WalletReversalRequest;
import club.escobar.dto.wallet.WalletReviewRequest;
import club.escobar.dto.wallet.WalletSummaryResponse;
import club.escobar.dto.wallet.WalletTransactionResponse;
import club.escobar.entity.BusinessProfile;
import club.escobar.entity.Content;
import club.escobar.entity.Payout;
import club.escobar.entity.User;
import club.escobar.entity.WalletTransaction;
import club.escobar.entity.enums.FundingSource;
import club.escobar.entity.enums.UserRole;
import club.escobar.entity.enums.WalletTransactionStatus;
import club.escobar.entity.enums.WalletTransactionType;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.mapper.WalletTransactionMapper;
import club.escobar.repository.BusinessProfileRepository;
import club.escobar.repository.UserRepository;
import club.escobar.repository.WalletTransactionRepository;
import club.escobar.service.impl.WalletServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private BusinessProfileRepository businessProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletTransactionMapper walletTransactionMapper;

    private WalletServiceImpl walletService;

    private User business;
    private User admin;

    @BeforeEach
    void setUp() {
        walletService = new WalletServiceImpl(walletTransactionRepository, businessProfileRepository, userRepository, walletTransactionMapper);
        business = User.builder().id(2L).email("business@test.com").role(UserRole.BUSINESS).build();
        admin = User.builder().id(99L).email("admin@test.com").role(UserRole.ADMIN).build();

        lenient().when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(walletTransactionMapper.toResponse(any(WalletTransaction.class))).thenReturn(mock(WalletTransactionResponse.class));
    }

    @Test
    void getSummary_rejectsWhenNotOwnBusiness() {
        assertThatThrownBy(() -> walletService.getSummary(999L, 2L))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void getSummary_computesAvailableBalanceAsConfirmedCreditMinusDebit() {
        when(businessProfileRepository.findByUser_Id(2L))
                .thenReturn(Optional.of(BusinessProfile.builder().id(2L).companyName("ABC Traders").build()));
        when(walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatus(2L, WalletTransactionType.CREDIT, WalletTransactionStatus.CONFIRMED))
                .thenReturn(new BigDecimal("1000.00"));
        when(walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatus(2L, WalletTransactionType.DEBIT, WalletTransactionStatus.CONFIRMED))
                .thenReturn(new BigDecimal("300.00"));
        when(walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatusAndFundingSourceIn(eq(2L), eq(WalletTransactionType.CREDIT), eq(WalletTransactionStatus.CONFIRMED), anyCollection()))
                .thenReturn(new BigDecimal("1000.00"));
        when(walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatusAndFundingSourceIn(eq(2L), eq(WalletTransactionType.DEBIT), eq(WalletTransactionStatus.CONFIRMED), anyCollection()))
                .thenReturn(new BigDecimal("300.00"));
        when(walletTransactionRepository.findTopByBusiness_IdOrderByCreatedAtDesc(2L)).thenReturn(Optional.empty());

        WalletSummaryResponse summary = walletService.getSummary(2L, 2L);

        assertThat(summary.availableBalanceInr()).isEqualByComparingTo("700.00");
        assertThat(summary.businessName()).isEqualTo("ABC Traders");
    }

    @Test
    void addMoney_rejectsWhenNotOwnBusiness() {
        assertThatThrownBy(() -> walletService.addMoney(999L, 2L, new AddMoneyRequest(new BigDecimal("500.00"), null)))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void addMoney_createsPendingBusinessmanManualCredit() {
        when(userRepository.getReferenceById(2L)).thenReturn(business);

        walletService.addMoney(2L, 2L, new AddMoneyRequest(new BigDecimal("500.00"), "Campaign funding"));

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());
        WalletTransaction saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(WalletTransactionStatus.PENDING);
        assertThat(saved.getType()).isEqualTo(WalletTransactionType.CREDIT);
        assertThat(saved.getFundingSource()).isEqualTo(FundingSource.BUSINESSMAN_MANUAL);
        assertThat(saved.getAmountInr()).isEqualByComparingTo("500.00");
    }

    @Test
    void reviewTopUp_confirm_marksConfirmedAndStampsReviewer() {
        WalletTransaction pending = WalletTransaction.builder().id(100L).business(business)
                .type(WalletTransactionType.CREDIT).status(WalletTransactionStatus.PENDING)
                .fundingSource(FundingSource.BUSINESSMAN_MANUAL).amountInr(new BigDecimal("500.00")).performedBy(business).build();
        when(walletTransactionRepository.findById(100L)).thenReturn(Optional.of(pending));
        when(userRepository.getReferenceById(99L)).thenReturn(admin);

        walletService.reviewTopUp(99L, 100L, new WalletReviewRequest(WalletTransactionStatus.CONFIRMED, null));

        assertThat(pending.getStatus()).isEqualTo(WalletTransactionStatus.CONFIRMED);
        assertThat(pending.getConfirmedAt()).isNotNull();
        assertThat(pending.getConfirmedBy()).isEqualTo(admin);
    }

    @Test
    void reviewTopUp_reject_leavesBalanceUnaffected() {
        WalletTransaction pending = WalletTransaction.builder().id(100L).business(business)
                .type(WalletTransactionType.CREDIT).status(WalletTransactionStatus.PENDING)
                .fundingSource(FundingSource.BUSINESSMAN_MANUAL).amountInr(new BigDecimal("500.00")).performedBy(business).build();
        when(walletTransactionRepository.findById(100L)).thenReturn(Optional.of(pending));
        when(userRepository.getReferenceById(99L)).thenReturn(admin);

        walletService.reviewTopUp(99L, 100L, new WalletReviewRequest(WalletTransactionStatus.REJECTED, "Funds never received"));

        assertThat(pending.getStatus()).isEqualTo(WalletTransactionStatus.REJECTED);
    }

    @Test
    void reviewTopUp_rejectsWhenNotPending() {
        WalletTransaction alreadyConfirmed = WalletTransaction.builder().id(100L).business(business)
                .type(WalletTransactionType.CREDIT).status(WalletTransactionStatus.CONFIRMED)
                .fundingSource(FundingSource.BUSINESSMAN_MANUAL).amountInr(new BigDecimal("500.00")).performedBy(business).build();
        when(walletTransactionRepository.findById(100L)).thenReturn(Optional.of(alreadyConfirmed));

        assertThatThrownBy(() -> walletService.reviewTopUp(99L, 100L, new WalletReviewRequest(WalletTransactionStatus.CONFIRMED, null)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void reverseTransaction_createsOffsettingRow_leavingOriginalUntouched() {
        WalletTransaction original = WalletTransaction.builder().id(100L).business(business)
                .type(WalletTransactionType.CREDIT).status(WalletTransactionStatus.CONFIRMED)
                .fundingSource(FundingSource.BUSINESSMAN_MANUAL).amountInr(new BigDecimal("500.00")).performedBy(business).build();
        when(walletTransactionRepository.findById(100L)).thenReturn(Optional.of(original));
        when(walletTransactionRepository.existsByReversedTransaction_Id(100L)).thenReturn(false);
        when(userRepository.getReferenceById(99L)).thenReturn(admin);

        walletService.reverseTransaction(99L, 100L, new WalletReversalRequest("Entered twice by mistake"));

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository, times(1)).save(captor.capture());
        WalletTransaction reversal = captor.getValue();

        assertThat(reversal.getType()).isEqualTo(WalletTransactionType.DEBIT);
        assertThat(reversal.getFundingSource()).isEqualTo(FundingSource.REVERSAL);
        assertThat(reversal.getAmountInr()).isEqualByComparingTo("500.00");
        assertThat(reversal.getReversedTransaction()).isEqualTo(original);
        // The original is never mutated - its own status/amount stay exactly as they were; the new
        // offsetting row above is what nets the balance back, preserving complete history.
        assertThat(original.getStatus()).isEqualTo(WalletTransactionStatus.CONFIRMED);
    }

    @Test
    void reverseTransaction_rejectsWhenOriginalNotConfirmed() {
        WalletTransaction pending = WalletTransaction.builder().id(100L).business(business)
                .type(WalletTransactionType.CREDIT).status(WalletTransactionStatus.PENDING)
                .fundingSource(FundingSource.BUSINESSMAN_MANUAL).amountInr(new BigDecimal("500.00")).performedBy(business).build();
        when(walletTransactionRepository.findById(100L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> walletService.reverseTransaction(99L, 100L, new WalletReversalRequest("Too early")))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void reverseTransaction_rejectsWhenAlreadyReversed() {
        WalletTransaction original = WalletTransaction.builder().id(100L).business(business)
                .type(WalletTransactionType.CREDIT).status(WalletTransactionStatus.CONFIRMED)
                .fundingSource(FundingSource.BUSINESSMAN_MANUAL).amountInr(new BigDecimal("500.00")).performedBy(business).build();
        when(walletTransactionRepository.findById(100L)).thenReturn(Optional.of(original));
        when(walletTransactionRepository.existsByReversedTransaction_Id(100L)).thenReturn(true);

        assertThatThrownBy(() -> walletService.reverseTransaction(99L, 100L, new WalletReversalRequest("Duplicate reversal")))
                .isInstanceOf(InvalidStateTransitionException.class);
        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    void debitForPayout_throwsAndDoesNotSave_whenBalanceInsufficient() {
        User payoutBusiness = User.builder().id(2L).role(UserRole.BUSINESS).build();
        Content content = Content.builder().id(10L).build();
        Payout payout = Payout.builder().id(50L).content(content).business(payoutBusiness).amountInr(new BigDecimal("500.00")).build();

        when(walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatus(2L, WalletTransactionType.CREDIT, WalletTransactionStatus.CONFIRMED))
                .thenReturn(new BigDecimal("1000.00"));
        when(walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatus(2L, WalletTransactionType.DEBIT, WalletTransactionStatus.CONFIRMED))
                .thenReturn(new BigDecimal("800.00"));

        assertThatThrownBy(() -> walletService.debitForPayout(payout))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    void debitForPayout_createsConfirmedCampaignPaymentDebit_whenBalanceSufficient() {
        User payoutBusiness = User.builder().id(2L).role(UserRole.BUSINESS).build();
        Content content = Content.builder().id(10L).build();
        Payout payout = Payout.builder().id(50L).content(content).business(payoutBusiness).amountInr(new BigDecimal("500.00")).build();

        when(walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatus(2L, WalletTransactionType.CREDIT, WalletTransactionStatus.CONFIRMED))
                .thenReturn(new BigDecimal("1000.00"));
        when(walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatus(2L, WalletTransactionType.DEBIT, WalletTransactionStatus.CONFIRMED))
                .thenReturn(new BigDecimal("200.00"));

        walletService.debitForPayout(payout);

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());
        WalletTransaction saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(WalletTransactionType.DEBIT);
        assertThat(saved.getStatus()).isEqualTo(WalletTransactionStatus.CONFIRMED);
        assertThat(saved.getFundingSource()).isEqualTo(FundingSource.CAMPAIGN_PAYMENT);
        assertThat(saved.getAmountInr()).isEqualByComparingTo("500.00");
        assertThat(saved.getPayout()).isEqualTo(payout);
    }

    @Test
    void adminListWallets_skipsBalanceQuery_whenNoBusinessesMatch() {
        when(businessProfileRepository.searchForAdmin(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = walletService.adminListWallets("nonexistent", Pageable.unpaged());

        assertThat(result.content()).isEmpty();
        verify(walletTransactionRepository, never())
                .sumBalancesByBusinessIds(anyCollection(), any(), any(), any(), anyCollection(), any());
    }

    @Test
    void adminCredit_createsConfirmedAdminManualCreditImmediately() {
        when(businessProfileRepository.findByUser_Id(2L))
                .thenReturn(Optional.of(BusinessProfile.builder().id(2L).companyName("ABC Traders").build()));
        when(userRepository.getReferenceById(2L)).thenReturn(business);
        when(userRepository.getReferenceById(99L)).thenReturn(admin);

        walletService.adminCredit(99L, 2L, new AdminCreditRequest(new BigDecimal("1000.00"), "Correction credit"));

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());
        WalletTransaction saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(WalletTransactionStatus.CONFIRMED);
        assertThat(saved.getFundingSource()).isEqualTo(FundingSource.ADMIN_MANUAL);
        assertThat(saved.getConfirmedAt()).isNull();
    }

    @Test
    void adminCredit_rejectsWhenBusinessNotFound() {
        when(businessProfileRepository.findByUser_Id(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.adminCredit(99L, 2L, new AdminCreditRequest(new BigDecimal("1000.00"), null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
