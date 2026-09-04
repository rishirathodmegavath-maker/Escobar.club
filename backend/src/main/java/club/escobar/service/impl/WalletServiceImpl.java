package club.escobar.service.impl;

import club.escobar.dto.common.PageResponse;
import club.escobar.dto.wallet.AddMoneyRequest;
import club.escobar.dto.wallet.AdminCreditRequest;
import club.escobar.dto.wallet.WalletReversalRequest;
import club.escobar.dto.wallet.WalletReviewRequest;
import club.escobar.dto.wallet.WalletSummaryResponse;
import club.escobar.dto.wallet.WalletTransactionResponse;
import club.escobar.entity.BusinessProfile;
import club.escobar.entity.Payout;
import club.escobar.entity.User;
import club.escobar.entity.WalletTransaction;
import club.escobar.entity.enums.FundingSource;
import club.escobar.entity.enums.WalletTransactionStatus;
import club.escobar.entity.enums.WalletTransactionType;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.mapper.WalletTransactionMapper;
import club.escobar.repository.BusinessProfileRepository;
import club.escobar.repository.UserRepository;
import club.escobar.repository.WalletBalanceRow;
import club.escobar.repository.WalletTransactionRepository;
import club.escobar.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    // "Total Added"/"Total Paid" (and their global-dashboard equivalents) only count genuine
    // top-ups and campaign payouts, not REVERSAL corrections - a reversal still affects the raw
    // credit/debit sums that make up the spendable balance, it just isn't reported as a fresh
    // top-up or payout. See WalletBalanceRow.
    private static final Set<FundingSource> MANUAL_CREDIT_SOURCES = EnumSet.of(FundingSource.BUSINESSMAN_MANUAL, FundingSource.ADMIN_MANUAL);
    private static final Set<FundingSource> CAMPAIGN_PAYMENT_SOURCE = EnumSet.of(FundingSource.CAMPAIGN_PAYMENT);

    private final WalletTransactionRepository walletTransactionRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;
    private final WalletTransactionMapper walletTransactionMapper;

    @Override
    @Transactional(readOnly = true)
    public WalletSummaryResponse getSummary(Long requestingUserId, Long businessId) {
        if (!requestingUserId.equals(businessId)) {
            throw new ForbiddenActionException("You may only view your own wallet");
        }
        return buildSummary(businessId);
    }

    @Override
    @Transactional
    public WalletTransactionResponse addMoney(Long requestingUserId, Long businessId, AddMoneyRequest request) {
        if (!requestingUserId.equals(businessId)) {
            throw new ForbiddenActionException("You may only add money to your own wallet");
        }
        User business = userRepository.getReferenceById(businessId);
        WalletTransaction tx = WalletTransaction.builder()
                .business(business)
                .type(WalletTransactionType.CREDIT)
                .status(WalletTransactionStatus.PENDING)
                .fundingSource(FundingSource.BUSINESSMAN_MANUAL)
                .amountInr(request.amountInr())
                .note(request.note())
                .performedBy(business)
                .build();
        WalletTransaction saved = walletTransactionRepository.save(tx);
        log.info("Business id={} recorded a wallet top-up of {} awaiting admin confirmation", businessId, request.amountInr());
        return walletTransactionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WalletTransactionResponse> listTransactions(Long requestingUserId, Long businessId,
                                                                      WalletTransactionType type, WalletTransactionStatus status,
                                                                      Instant from, Instant to, Pageable pageable) {
        if (!requestingUserId.equals(businessId)) {
            throw new ForbiddenActionException("You may only view your own wallet");
        }
        Page<WalletTransaction> page = walletTransactionRepository.searchForBusiness(businessId, type, status, from, to, pageable);
        return PageResponse.of(page.map(walletTransactionMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WalletSummaryResponse> adminListWallets(String search, Pageable pageable) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        Page<BusinessProfile> businesses = businessProfileRepository.searchForAdmin(normalizedSearch, null, pageable);
        List<Long> businessIds = businesses.getContent().stream().map(BusinessProfile::getId).toList();

        Map<Long, WalletBalanceRow> balancesByBusinessId = businessIds.isEmpty()
                ? Map.of()
                : walletTransactionRepository
                        .sumBalancesByBusinessIds(businessIds, WalletTransactionType.CREDIT, WalletTransactionType.DEBIT,
                                WalletTransactionStatus.CONFIRMED, MANUAL_CREDIT_SOURCES, FundingSource.CAMPAIGN_PAYMENT)
                        .stream()
                        .collect(Collectors.toMap(WalletBalanceRow::getBusinessId, Function.identity()));

        Page<WalletSummaryResponse> page = businesses.map(profile -> {
            WalletBalanceRow row = balancesByBusinessId.get(profile.getId());
            BigDecimal creditAll = row != null ? row.getTotalCreditAll() : BigDecimal.ZERO;
            BigDecimal debitAll = row != null ? row.getTotalDebitAll() : BigDecimal.ZERO;
            return new WalletSummaryResponse(
                    profile.getId(),
                    profile.getCompanyName(),
                    creditAll.subtract(debitAll),
                    row != null ? row.getTotalAddedManual() : BigDecimal.ZERO,
                    row != null ? row.getTotalPaidCampaign() : BigDecimal.ZERO,
                    row != null ? row.getLastActivityAt() : null);
        });
        return PageResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletSummaryResponse adminGetWallet(Long businessId) {
        businessProfileRepository.findByUser_Id(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found for user id " + businessId));
        return buildSummary(businessId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WalletTransactionResponse> adminListTransactions(Long businessId, WalletTransactionType type,
                                                                           WalletTransactionStatus status, FundingSource fundingSource,
                                                                           Instant from, Instant to, String search, Pageable pageable) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        Page<WalletTransaction> page = walletTransactionRepository
                .searchForAdmin(businessId, type, status, fundingSource, from, to, normalizedSearch, pageable);
        return PageResponse.of(page.map(walletTransactionMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> adminListTransactionsAll(Long businessId, WalletTransactionType type,
                                                                      WalletTransactionStatus status, FundingSource fundingSource,
                                                                      Instant from, Instant to, String search) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        Page<WalletTransaction> page = walletTransactionRepository
                .searchForAdmin(businessId, type, status, fundingSource, from, to, normalizedSearch, Pageable.unpaged());
        return page.map(walletTransactionMapper::toResponse).getContent();
    }

    @Override
    @Transactional
    public WalletTransactionResponse adminCredit(Long adminUserId, Long businessId, AdminCreditRequest request) {
        businessProfileRepository.findByUser_Id(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found for user id " + businessId));
        User business = userRepository.getReferenceById(businessId);
        User admin = userRepository.getReferenceById(adminUserId);
        // Admin action is inherently trusted - created straight as CONFIRMED, never PENDING.
        WalletTransaction tx = WalletTransaction.builder()
                .business(business)
                .type(WalletTransactionType.CREDIT)
                .status(WalletTransactionStatus.CONFIRMED)
                .fundingSource(FundingSource.ADMIN_MANUAL)
                .amountInr(request.amountInr())
                .note(request.note())
                .performedBy(admin)
                .build();
        WalletTransaction saved = walletTransactionRepository.save(tx);
        log.info("Admin id={} manually credited business id={} with {}", adminUserId, businessId, request.amountInr());
        return walletTransactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WalletTransactionResponse reviewTopUp(Long adminUserId, Long transactionId, WalletReviewRequest request) {
        WalletTransaction tx = findByIdOrThrow(transactionId);
        if (tx.getStatus() != WalletTransactionStatus.PENDING
                || tx.getType() != WalletTransactionType.CREDIT
                || tx.getFundingSource() != FundingSource.BUSINESSMAN_MANUAL) {
            throw new InvalidStateTransitionException(
                    "Only a PENDING businessman-recorded top-up can be reviewed (current status: " + tx.getStatus() + ")");
        }
        if (request.decision() != WalletTransactionStatus.CONFIRMED && request.decision() != WalletTransactionStatus.REJECTED) {
            throw new InvalidStateTransitionException("Review decision must be CONFIRMED or REJECTED");
        }

        tx.setStatus(request.decision());
        tx.setConfirmedAt(Instant.now());
        tx.setConfirmedBy(userRepository.getReferenceById(adminUserId));
        if (StringUtils.hasText(request.note())) {
            tx.setNote(StringUtils.hasText(tx.getNote()) ? tx.getNote() + " | Admin: " + request.note() : request.note());
        }

        WalletTransaction saved = walletTransactionRepository.save(tx);
        log.info("Admin id={} reviewed wallet transaction id={}: {}", adminUserId, transactionId, request.decision());
        return walletTransactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WalletTransactionResponse reverseTransaction(Long adminUserId, Long transactionId, WalletReversalRequest request) {
        WalletTransaction original = findByIdOrThrow(transactionId);
        if (original.getStatus() != WalletTransactionStatus.CONFIRMED) {
            throw new InvalidStateTransitionException(
                    "Only a CONFIRMED transaction can be reversed (current status: " + original.getStatus() + ")");
        }
        // The original is never mutated (not even its status) - "preserve the complete history".
        // Double-reversal is prevented by checking whether a reversal row already points at it,
        // not by changing its status; the original stays CONFIRMED and keeps counting in every sum,
        // exactly netted out by this new offsetting row.
        if (walletTransactionRepository.existsByReversedTransaction_Id(transactionId)) {
            throw new InvalidStateTransitionException("This transaction has already been reversed");
        }

        User admin = userRepository.getReferenceById(adminUserId);
        WalletTransactionType reversalType = original.getType() == WalletTransactionType.CREDIT
                ? WalletTransactionType.DEBIT
                : WalletTransactionType.CREDIT;

        WalletTransaction reversal = WalletTransaction.builder()
                .business(original.getBusiness())
                .type(reversalType)
                .status(WalletTransactionStatus.CONFIRMED)
                .fundingSource(FundingSource.REVERSAL)
                .amountInr(original.getAmountInr())
                .note(request.note())
                .performedBy(admin)
                .reversedTransaction(original)
                .confirmedAt(Instant.now())
                .confirmedBy(admin)
                .build();
        WalletTransaction savedReversal = walletTransactionRepository.save(reversal);

        log.info("Admin id={} reversed wallet transaction id={} via new transaction id={}", adminUserId, transactionId, savedReversal.getId());
        return walletTransactionMapper.toResponse(savedReversal);
    }

    @Override
    @Transactional
    public void debitForPayout(Payout payout) {
        Long businessId = payout.getBusiness().getId();
        BigDecimal creditAll = walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatus(
                businessId, WalletTransactionType.CREDIT, WalletTransactionStatus.CONFIRMED);
        BigDecimal debitAll = walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatus(
                businessId, WalletTransactionType.DEBIT, WalletTransactionStatus.CONFIRMED);
        BigDecimal available = creditAll.subtract(debitAll);

        if (available.compareTo(payout.getAmountInr()) < 0) {
            throw new InvalidStateTransitionException(
                    "Insufficient wallet balance to mark this payout as paid. Available: ₹" + available
                            + ", required: ₹" + payout.getAmountInr());
        }

        WalletTransaction tx = WalletTransaction.builder()
                .business(payout.getBusiness())
                .type(WalletTransactionType.DEBIT)
                .status(WalletTransactionStatus.CONFIRMED)
                .fundingSource(FundingSource.CAMPAIGN_PAYMENT)
                .amountInr(payout.getAmountInr())
                .performedBy(payout.getBusiness())
                .payout(payout)
                .build();
        walletTransactionRepository.save(tx);
        log.info("Debited business id={} wallet by {} for payout of content id={}",
                businessId, payout.getAmountInr(), payout.getContent().getId());
    }

    private WalletTransaction findByIdOrThrow(Long transactionId) {
        return walletTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet transaction not found with id " + transactionId));
    }

    private WalletSummaryResponse buildSummary(Long businessId) {
        String businessName = businessProfileRepository.findByUser_Id(businessId)
                .map(BusinessProfile::getCompanyName)
                .orElse(null);
        BigDecimal creditAll = walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatus(
                businessId, WalletTransactionType.CREDIT, WalletTransactionStatus.CONFIRMED);
        BigDecimal debitAll = walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatus(
                businessId, WalletTransactionType.DEBIT, WalletTransactionStatus.CONFIRMED);
        BigDecimal totalAdded = walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatusAndFundingSourceIn(
                businessId, WalletTransactionType.CREDIT, WalletTransactionStatus.CONFIRMED, MANUAL_CREDIT_SOURCES);
        BigDecimal totalPaid = walletTransactionRepository.sumAmountInrByBusiness_IdAndTypeAndStatusAndFundingSourceIn(
                businessId, WalletTransactionType.DEBIT, WalletTransactionStatus.CONFIRMED, CAMPAIGN_PAYMENT_SOURCE);
        Instant lastActivityAt = walletTransactionRepository.findTopByBusiness_IdOrderByCreatedAtDesc(businessId)
                .map(WalletTransaction::getCreatedAt)
                .orElse(null);
        return new WalletSummaryResponse(businessId, businessName, creditAll.subtract(debitAll), totalAdded, totalPaid, lastActivityAt);
    }
}
