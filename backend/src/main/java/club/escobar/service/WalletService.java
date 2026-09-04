package club.escobar.service;

import club.escobar.dto.common.PageResponse;
import club.escobar.dto.wallet.AddMoneyRequest;
import club.escobar.dto.wallet.AdminCreditRequest;
import club.escobar.dto.wallet.WalletReversalRequest;
import club.escobar.dto.wallet.WalletReviewRequest;
import club.escobar.dto.wallet.WalletSummaryResponse;
import club.escobar.dto.wallet.WalletTransactionResponse;
import club.escobar.entity.Payout;
import club.escobar.entity.enums.FundingSource;
import club.escobar.entity.enums.WalletTransactionStatus;
import club.escobar.entity.enums.WalletTransactionType;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface WalletService {

    WalletSummaryResponse getSummary(Long requestingUserId, Long businessId);

    WalletTransactionResponse addMoney(Long requestingUserId, Long businessId, AddMoneyRequest request);

    PageResponse<WalletTransactionResponse> listTransactions(Long requestingUserId, Long businessId,
                                                               WalletTransactionType type, WalletTransactionStatus status,
                                                               Instant from, Instant to, Pageable pageable);

    PageResponse<WalletSummaryResponse> adminListWallets(String search, Pageable pageable);

    WalletSummaryResponse adminGetWallet(Long businessId);

    PageResponse<WalletTransactionResponse> adminListTransactions(Long businessId, WalletTransactionType type,
                                                                    WalletTransactionStatus status, FundingSource fundingSource,
                                                                    Instant from, Instant to, String search, Pageable pageable);

    // Every matching row, unpaged - backs the admin CSV export, same rationale as
    // PayoutService.listForBusinessAll.
    List<WalletTransactionResponse> adminListTransactionsAll(Long businessId, WalletTransactionType type,
                                                               WalletTransactionStatus status, FundingSource fundingSource,
                                                               Instant from, Instant to, String search);

    WalletTransactionResponse adminCredit(Long adminUserId, Long businessId, AdminCreditRequest request);

    WalletTransactionResponse reviewTopUp(Long adminUserId, Long transactionId, WalletReviewRequest request);

    WalletTransactionResponse reverseTransaction(Long adminUserId, Long transactionId, WalletReversalRequest request);

    // Called only by PayoutServiceImpl.markPaid, inside the same @Transactional method, once the
    // payout's own ownership/status checks have already passed. Throws InvalidStateTransitionException
    // (rolling back the whole markPaid transaction, payout stays PAYABLE) if the business's confirmed
    // balance can't cover payout.getAmountInr(); the amount always comes from the payout itself, never
    // a request body.
    void debitForPayout(Payout payout);
}
