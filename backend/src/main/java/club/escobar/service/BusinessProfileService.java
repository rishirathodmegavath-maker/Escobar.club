package club.escobar.service;

import club.escobar.dto.business.BusinessDashboardResponse;
import club.escobar.dto.business.BusinessProfileResponse;
import club.escobar.dto.business.BusinessProfileUpdateRequest;
import club.escobar.dto.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface BusinessProfileService {

    PageResponse<BusinessProfileResponse> search(String search, String industry, Pageable pageable);

    BusinessProfileResponse getById(Long id);

    BusinessProfileResponse getByUserId(Long userId);

    BusinessProfileResponse updateOwnProfile(Long userId, BusinessProfileUpdateRequest request);

    BusinessDashboardResponse dashboard(Long businessUserId);
}
