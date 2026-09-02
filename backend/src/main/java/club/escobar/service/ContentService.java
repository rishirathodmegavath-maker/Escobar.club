package club.escobar.service;

import club.escobar.dto.common.PageResponse;
import club.escobar.dto.content.ContentBulkReviewRequest;
import club.escobar.dto.content.ContentBulkReviewResponse;
import club.escobar.dto.content.ContentCreateRequest;
import club.escobar.dto.content.ContentPublishRequest;
import club.escobar.dto.content.ContentResponse;
import club.escobar.dto.content.ContentReviewRequest;
import club.escobar.dto.content.ContentUpdateRequest;
import club.escobar.entity.enums.ContentStatus;
import org.springframework.data.domain.Pageable;

public interface ContentService {

    ContentResponse submit(Long creatorUserId, ContentCreateRequest request);

    ContentResponse resubmit(Long creatorUserId, Long contentId, ContentUpdateRequest request);

    ContentResponse review(Long businessUserId, Long contentId, ContentReviewRequest request);

    // Applies the same decision to a batch of content items. Each item succeeds or fails
    // independently (mixed outcomes are expected and reported back, not treated as an all-or-nothing
    // transaction) - see ContentServiceImpl for why.
    ContentBulkReviewResponse reviewBulk(Long businessUserId, ContentBulkReviewRequest request);

    ContentResponse publish(Long creatorUserId, Long contentId, ContentPublishRequest request);

    PageResponse<ContentResponse> listForBusiness(Long requestingUserId, Long businessId,
                                                   ContentStatus status, Pageable pageable);

    PageResponse<ContentResponse> listForCreator(Long creatorUserId, Pageable pageable);

    ContentResponse getById(Long requestingUserId, Long contentId);
}
