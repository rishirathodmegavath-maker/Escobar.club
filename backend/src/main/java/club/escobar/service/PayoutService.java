package club.escobar.service;

import club.escobar.dto.common.PageResponse;
import club.escobar.dto.payout.PayoutMarkPaidRequest;
import club.escobar.dto.payout.PayoutResponse;
import club.escobar.entity.enums.PayoutStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PayoutService {

    void recalculate(Long contentId);

    PayoutResponse getForContent(Long requestingUserId, Long contentId);

    PageResponse<PayoutResponse> listForBusiness(Long requestingUserId, Long businessId, PayoutStatus status, Pageable pageable);

    // Every matching row, unpaged - backs the CSV export, which must cover the whole filtered set,
    // not one page of it.
    List<PayoutResponse> listForBusinessAll(Long requestingUserId, Long businessId, PayoutStatus status);

    PayoutResponse markPaid(Long businessUserId, Long contentId, PayoutMarkPaidRequest request);
}
