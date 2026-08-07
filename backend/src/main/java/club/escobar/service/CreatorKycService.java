package club.escobar.service;

import club.escobar.dto.kyc.CreatorKycProfileResponse;
import club.escobar.dto.kyc.CreatorKycReviewDetailResponse;
import club.escobar.dto.kyc.CreatorKycReviewRequest;
import club.escobar.dto.kyc.CreatorKycSubmitRequest;

public interface CreatorKycService {

    CreatorKycProfileResponse submit(Long creatorUserId, CreatorKycSubmitRequest request);

    CreatorKycProfileResponse getOwn(Long creatorUserId);

    CreatorKycReviewDetailResponse getForReview(Long businessUserId, Long creatorUserId);

    CreatorKycReviewDetailResponse review(Long businessUserId, Long creatorUserId, CreatorKycReviewRequest request);

    CreatorKycReviewDetailResponse adminReview(Long adminUserId, Long creatorUserId, CreatorKycReviewRequest request);
}
