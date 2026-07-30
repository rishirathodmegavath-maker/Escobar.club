package club.escobar.mapper;

import club.escobar.dto.content.ContentReviewNoteResponse;
import club.escobar.dto.content.ContentResponse;
import club.escobar.entity.Content;
import club.escobar.entity.ContentReviewNote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContentMapper {

    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "creatorDisplayName", source = "creator.creatorProfile.displayName")
    @Mapping(target = "creatorProfilePictureUrl", source = "creator.creatorProfile.profilePictureUrl")
    @Mapping(target = "campaignId", source = "campaign.id")
    @Mapping(target = "campaignTitle", source = "campaign.title")
    @Mapping(target = "businessId", source = "business.id")
    @Mapping(target = "businessCompanyName", source = "business.businessProfile.companyName")
    @Mapping(target = "businessLogoUrl", source = "business.businessProfile.logoUrl")
    ContentResponse toResponse(Content entity);

    @Mapping(target = "authoredByUserId", source = "authoredBy.id")
    ContentReviewNoteResponse toResponse(ContentReviewNote entity);
}
