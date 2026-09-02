package club.escobar.service.impl;

import club.escobar.dto.business.BusinessDashboardResponse;
import club.escobar.dto.business.BusinessProfileResponse;
import club.escobar.dto.business.BusinessProfileUpdateRequest;
import club.escobar.dto.common.PageResponse;
import club.escobar.entity.BusinessProfile;
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.PayoutStatus;
import club.escobar.exception.DuplicateResourceException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.mapper.BusinessProfileMapper;
import club.escobar.repository.BusinessProfileRepository;
import club.escobar.repository.CampaignRepository;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.PayoutRepository;
import club.escobar.service.BusinessProfileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BusinessProfileServiceImpl implements BusinessProfileService {

    private static final Logger log = LoggerFactory.getLogger(BusinessProfileServiceImpl.class);

    private final BusinessProfileRepository businessProfileRepository;
    private final BusinessProfileMapper businessProfileMapper;
    private final CampaignRepository campaignRepository;
    private final ContentRepository contentRepository;
    private final PayoutRepository payoutRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BusinessProfileResponse> search(String search, String industry, Pageable pageable) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        String normalizedIndustry = StringUtils.hasText(industry) ? industry.trim() : null;

        Page<BusinessProfileResponse> page = businessProfileRepository
                .search(normalizedSearch, normalizedIndustry, pageable)
                .map(businessProfileMapper::toResponse);

        return PageResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessProfileResponse getById(Long id) {
        BusinessProfile profile = findById(id);
        if (profile.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResourceNotFoundException("Business profile not found with id " + id);
        }
        return businessProfileMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessProfileResponse getByUserId(Long userId) {
        BusinessProfile profile = businessProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found for user id " + userId));
        return businessProfileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public BusinessProfileResponse updateOwnProfile(Long userId, BusinessProfileUpdateRequest request) {
        BusinessProfile profile = businessProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found for user id " + userId));

        if (!request.gstNumber().equals(profile.getGstNumber())
                && businessProfileRepository.existsByGstNumberAndUser_IdNot(request.gstNumber(), userId)) {
            throw new DuplicateResourceException("This GST number is already registered to another business");
        }

        profile.setCompanyName(request.companyName());
        profile.setGstNumber(request.gstNumber());
        profile.setContactPersonName(request.contactPersonName());
        profile.setMobileNumber(request.mobileNumber());
        profile.setIndustry(request.industry());
        profile.setDescription(request.description());
        profile.setLogoUrl(request.logoUrl());
        profile.setWebsite(request.website());

        BusinessProfile saved = businessProfileRepository.save(profile);
        log.info("Business profile updated for user id={}", userId);
        return businessProfileMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessDashboardResponse dashboard(Long businessUserId) {
        BusinessProfile profile = businessProfileRepository.findByUser_Id(businessUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found for user id " + businessUserId));

        return new BusinessDashboardResponse(
                profile.getApprovalStatus(),
                campaignRepository.countByBusiness_Id(businessUserId),
                campaignRepository.countLiveByBusinessId(businessUserId),
                campaignRepository.countByBusiness_IdAndApprovalStatus(businessUserId, ApprovalStatus.PENDING),
                contentRepository.countByBusiness_IdAndStatus(businessUserId, ContentStatus.SUBMITTED),
                contentRepository.countByBusiness_IdAndStatus(businessUserId, ContentStatus.CHANGES_REQUESTED),
                contentRepository.countByBusiness_IdAndStatus(businessUserId, ContentStatus.PENDING_LINK_REVIEW),
                contentRepository.countByBusiness_IdAndStatus(businessUserId, ContentStatus.PUBLISHED),
                payoutRepository.countByBusiness_IdAndStatus(businessUserId, PayoutStatus.PAYABLE),
                payoutRepository.sumAmountInrByBusiness_IdAndStatus(businessUserId, PayoutStatus.PAYABLE),
                payoutRepository.countByBusiness_IdAndStatus(businessUserId, PayoutStatus.PENDING_KYC),
                payoutRepository.sumAmountInrByBusiness_IdAndStatus(businessUserId, PayoutStatus.PAID)
        );
    }

    private BusinessProfile findById(Long id) {
        return businessProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found with id " + id));
    }
}
