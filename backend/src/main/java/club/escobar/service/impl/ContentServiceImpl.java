package club.escobar.service.impl;

import club.escobar.dto.common.PageResponse;
import club.escobar.dto.content.ContentCreateRequest;
import club.escobar.dto.content.ContentPublishRequest;
import club.escobar.dto.content.ContentResponse;
import club.escobar.dto.content.ContentReviewRequest;
import club.escobar.dto.content.ContentUpdateRequest;
import club.escobar.entity.Campaign;
import club.escobar.entity.Content;
import club.escobar.entity.ContentReviewNote;
import club.escobar.entity.User;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.mapper.ContentMapper;
import club.escobar.repository.CampaignRepository;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.UserRepository;
import club.escobar.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private static final Logger log = LoggerFactory.getLogger(ContentServiceImpl.class);
    private static final Set<ContentStatus> REVIEWABLE_DECISIONS =
            EnumSet.of(ContentStatus.APPROVED, ContentStatus.REJECTED, ContentStatus.CHANGES_REQUESTED);

    private final ContentRepository contentRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final ContentMapper contentMapper;

    @Override
    @Transactional
    public ContentResponse submit(Long creatorUserId, ContentCreateRequest request) {
        Campaign campaign = campaignRepository.findById(request.campaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id " + request.campaignId()));
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!campaign.isOpenForSubmissions()) {
            throw new InvalidStateTransitionException(campaign.submissionClosedReason());
        }

        Content content = Content.builder()
                .creator(creator)
                .campaign(campaign)
                .business(campaign.getBusiness())
                .caption(request.caption())
                .mediaUrl(request.mediaUrl())
                .mediaType(request.mediaType())
                .status(ContentStatus.SUBMITTED)
                .version(1)
                .submittedAt(Instant.now())
                .build();

        Content saved = contentRepository.save(content);
        log.info("Creator id={} submitted content id={} for campaign id={}", creatorUserId, saved.getId(), campaign.getId());
        return contentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ContentResponse resubmit(Long creatorUserId, Long contentId, ContentUpdateRequest request) {
        Content content = findById(contentId);

        if (!content.getCreator().getId().equals(creatorUserId)) {
            throw new ForbiddenActionException("This content does not belong to you");
        }
        if (content.getStatus() != ContentStatus.CHANGES_REQUESTED) {
            throw new InvalidStateTransitionException(
                    "Content can only be resubmitted when changes have been requested (current status: " + content.getStatus() + ")");
        }

        content.setCaption(request.caption());
        content.setMediaUrl(request.mediaUrl());
        content.setMediaType(request.mediaType());
        content.setVersion(content.getVersion() + 1);
        content.setStatus(ContentStatus.SUBMITTED);
        content.setSubmittedAt(Instant.now());

        Content saved = contentRepository.save(content);
        log.info("Creator id={} resubmitted content id={} as version {}", creatorUserId, contentId, saved.getVersion());
        return contentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ContentResponse review(Long businessUserId, Long contentId, ContentReviewRequest request) {
        Content content = findById(contentId);

        if (!content.getBusiness().getId().equals(businessUserId)) {
            throw new ForbiddenActionException("This content was not submitted to your business");
        }
        if (content.getStatus() != ContentStatus.SUBMITTED) {
            throw new InvalidStateTransitionException(
                    "Cannot review content that is currently " + content.getStatus() + "; it must be SUBMITTED");
        }
        if (!REVIEWABLE_DECISIONS.contains(request.decision())) {
            throw new InvalidStateTransitionException("Decision must be one of " + REVIEWABLE_DECISIONS);
        }

        User business = userRepository.getReferenceById(businessUserId);

        content.setStatus(request.decision());
        content.addReviewNote(ContentReviewNote.builder()
                .authoredBy(business)
                .contentVersion(content.getVersion())
                .decision(request.decision())
                .noteText(request.note())
                .build());

        Content saved = contentRepository.save(content);
        log.info("Business id={} reviewed content id={} (v{}): {}", businessUserId, contentId, content.getVersion(), request.decision());
        return contentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ContentResponse publish(Long creatorUserId, Long contentId, ContentPublishRequest request) {
        Content content = findById(contentId);

        if (!content.getCreator().getId().equals(creatorUserId)) {
            throw new ForbiddenActionException("This content does not belong to you");
        }
        if (content.getStatus() != ContentStatus.APPROVED) {
            throw new InvalidStateTransitionException(
                    "Content can only be published once approved (current status: " + content.getStatus() + ")");
        }

        content.setPostUrl(request.postUrl());
        content.setPublishedAt(Instant.now());
        content.setStatus(ContentStatus.PUBLISHED);

        Content saved = contentRepository.save(content);
        log.info("Creator id={} published content id={} at {}", creatorUserId, contentId, saved.getPostUrl());
        return contentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContentResponse> listForBusiness(Long requestingUserId, Long businessId,
                                                          ContentStatus status, Pageable pageable) {
        if (!requestingUserId.equals(businessId)) {
            throw new ForbiddenActionException("You may only view your own content review queue");
        }

        Page<Content> page = status != null
                ? contentRepository.findByBusiness_IdAndStatus(businessId, status, pageable)
                : contentRepository.findByBusiness_Id(businessId, pageable);

        return PageResponse.of(page.map(contentMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContentResponse> listForCreator(Long creatorUserId, Pageable pageable) {
        Page<ContentResponse> page = contentRepository.findByCreator_Id(creatorUserId, pageable)
                .map(contentMapper::toResponse);
        return PageResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentResponse getById(Long contentId) {
        return contentMapper.toResponse(findById(contentId));
    }

    private Content findById(Long contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id " + contentId));
    }
}
