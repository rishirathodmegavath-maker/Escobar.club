package club.escobar.service.impl;

import club.escobar.dto.kyc.CreatorKycProfileResponse;
import club.escobar.dto.kyc.CreatorKycReviewDetailResponse;
import club.escobar.dto.kyc.CreatorKycReviewRequest;
import club.escobar.dto.kyc.CreatorKycSubmitRequest;
import club.escobar.entity.CreatorKycProfile;
import club.escobar.entity.User;
import club.escobar.entity.enums.KycStatus;
import club.escobar.exception.ApiException;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.mapper.CreatorKycMapper;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.CreatorKycProfileRepository;
import club.escobar.repository.UserRepository;
import club.escobar.service.CreatorKycService;
import club.escobar.storage.StorageService;
import club.escobar.storage.StoredFileContent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CreatorKycServiceImpl implements CreatorKycService {

    private static final Logger log = LoggerFactory.getLogger(CreatorKycServiceImpl.class);

    private final CreatorKycProfileRepository creatorKycProfileRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final CreatorKycMapper creatorKycMapper;
    private final StorageService storageService;

    @Override
    @Transactional
    public CreatorKycProfileResponse submit(Long creatorUserId, CreatorKycSubmitRequest request) {
        CreatorKycProfile profile = creatorKycProfileRepository.findByCreator_Id(creatorUserId)
                .orElseGet(() -> {
                    User creator = userRepository.findById(creatorUserId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    return CreatorKycProfile.builder().creator(creator).build();
                });

        // A resubmit may omit documentKey to keep the previously-uploaded document unchanged.
        String documentKey = (request.documentKey() != null && !request.documentKey().isBlank())
                ? request.documentKey()
                : profile.getDocumentKey();
        if (documentKey == null || documentKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A KYC document is required");
        }

        profile.setPanNumber(request.panNumber());
        profile.setNameOnPan(request.nameOnPan());
        profile.setDocumentKey(documentKey);
        profile.setStatus(KycStatus.PENDING);
        profile.setReviewedBy(null);
        profile.setReviewNote(null);
        profile.setReviewedAt(null);

        CreatorKycProfile saved = creatorKycProfileRepository.save(profile);
        log.info("Creator id={} submitted KYC", creatorUserId);
        return creatorKycMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorKycProfileResponse getOwn(Long creatorUserId) {
        return creatorKycMapper.toResponse(findByCreatorId(creatorUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorKycReviewDetailResponse getForReview(Long businessUserId, Long creatorUserId) {
        assertReviewable(businessUserId, creatorUserId);
        return creatorKycMapper.toReviewDetailResponse(findByCreatorId(creatorUserId));
    }

    @Override
    @Transactional
    public CreatorKycReviewDetailResponse review(Long businessUserId, Long creatorUserId, CreatorKycReviewRequest request) {
        assertReviewable(businessUserId, creatorUserId);
        CreatorKycProfile saved = applyReview(businessUserId, creatorUserId, request);
        log.info("Business id={} reviewed KYC for creator id={}: {}", businessUserId, creatorUserId, request.status());
        return creatorKycMapper.toReviewDetailResponse(saved);
    }

    @Override
    @Transactional
    public CreatorKycReviewDetailResponse adminReview(Long adminUserId, Long creatorUserId, CreatorKycReviewRequest request) {
        // Platform admins review globally - no business-content relationship required, unlike review().
        CreatorKycProfile saved = applyReview(adminUserId, creatorUserId, request);
        log.info("Admin id={} reviewed KYC for creator id={}: {}", adminUserId, creatorUserId, request.status());
        return creatorKycMapper.toReviewDetailResponse(saved);
    }

    private CreatorKycProfile applyReview(Long reviewerUserId, Long creatorUserId, CreatorKycReviewRequest request) {
        CreatorKycProfile profile = findByCreatorId(creatorUserId);
        if (profile.getStatus() != KycStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Cannot review KYC that is already " + profile.getStatus());
        }
        if (request.status() == KycStatus.PENDING) {
            throw new InvalidStateTransitionException("Cannot set KYC status back to PENDING");
        }

        profile.setStatus(request.status());
        profile.setReviewedBy(userRepository.getReferenceById(reviewerUserId));
        profile.setReviewNote(request.reviewNote());
        profile.setReviewedAt(Instant.now());

        return creatorKycProfileRepository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFileContent getDocument(Long requesterId, String requesterRole, Long creatorId) {
        boolean isSelf = requesterId.equals(creatorId);
        boolean isAdmin = "ADMIN".equals(requesterRole);
        if (!isSelf && !isAdmin) {
            if (!"BUSINESS".equals(requesterRole)) {
                throw new ForbiddenActionException("You do not have access to this KYC document");
            }
            assertReviewable(requesterId, creatorId);
        }
        CreatorKycProfile profile = findByCreatorId(creatorId);
        return storageService.loadPrivate(profile.getDocumentKey());
    }

    private void assertReviewable(Long businessUserId, Long creatorUserId) {
        if (!contentRepository.existsByCreator_IdAndBusiness_Id(creatorUserId, businessUserId)) {
            throw new ForbiddenActionException("You may only review KYC for creators who have submitted content to your business");
        }
    }

    private CreatorKycProfile findByCreatorId(Long creatorUserId) {
        return creatorKycProfileRepository.findByCreator_Id(creatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC profile not found for creator id " + creatorUserId));
    }
}
