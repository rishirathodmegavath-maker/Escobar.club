package club.escobar.service;

import club.escobar.dto.creator.CreatorDashboardResponse;
import club.escobar.dto.creator.CreatorProfileResponse;
import club.escobar.dto.creator.CreatorProfileUpdateRequest;

public interface CreatorProfileService {

    CreatorProfileResponse getByUserId(Long userId);

    CreatorProfileResponse updateOwnProfile(Long userId, CreatorProfileUpdateRequest request);

    CreatorDashboardResponse dashboard(Long creatorUserId);
}
