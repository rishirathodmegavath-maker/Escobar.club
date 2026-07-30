package club.escobar.service.impl;

import club.escobar.dto.creator.CreatorProfileResponse;
import club.escobar.dto.creator.CreatorProfileUpdateRequest;
import club.escobar.entity.CreatorProfile;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.mapper.CreatorProfileMapper;
import club.escobar.repository.CreatorProfileRepository;
import club.escobar.service.CreatorProfileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CreatorProfileServiceImpl implements CreatorProfileService {

    private static final Logger log = LoggerFactory.getLogger(CreatorProfileServiceImpl.class);

    private final CreatorProfileRepository creatorProfileRepository;
    private final CreatorProfileMapper creatorProfileMapper;

    @Override
    @Transactional(readOnly = true)
    public CreatorProfileResponse getByUserId(Long userId) {
        CreatorProfile profile = findByUserId(userId);
        return creatorProfileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public CreatorProfileResponse updateOwnProfile(Long userId, CreatorProfileUpdateRequest request) {
        CreatorProfile profile = findByUserId(userId);

        profile.setDisplayName(request.displayName());
        profile.setBio(request.bio());
        profile.setProfilePictureUrl(request.profilePictureUrl());
        profile.setNiche(request.niche());
        profile.setOpenToOtherNiches(request.openToOtherNiches());
        profile.setInstagramProfileUrl(request.instagramProfileUrl());
        profile.setFollowerCount(request.followerCount());
        profile.setPortfolioLinks(request.portfolioLinks() == null ? new ArrayList<>() : new ArrayList<>(request.portfolioLinks()));

        CreatorProfile saved = creatorProfileRepository.save(profile);
        log.info("Creator profile updated for user id={}", userId);
        return creatorProfileMapper.toResponse(saved);
    }

    private CreatorProfile findByUserId(Long userId) {
        return creatorProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator profile not found for user id " + userId));
    }
}
