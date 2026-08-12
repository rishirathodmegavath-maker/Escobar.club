package club.escobar.service;

import club.escobar.dto.common.PageResponse;
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

    ContentResponse publish(Long creatorUserId, Long contentId, ContentPublishRequest request);

    PageResponse<ContentResponse> listForBusiness(Long requestingUserId, Long businessId,
                                                   ContentStatus status, Pageable pageable);

    PageResponse<ContentResponse> listForCreator(Long creatorUserId, Pageable pageable);

    ContentResponse getById(Long requestingUserId, Long contentId);
}
