package club.escobar.mapper;

import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.entity.Campaign;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CampaignMapper {

    @Mapping(target = "businessId", source = "business.id")
    @Mapping(target = "businessCompanyName", source = "business.businessProfile.companyName")
    @Mapping(target = "businessLogoUrl", source = "business.businessProfile.logoUrl")
    @Mapping(target = "acceptingSubmissions", expression = "java(entity.isOpenForSubmissions())")
    @Mapping(target = "hot", ignore = true)
    CampaignResponse toResponse(Campaign entity);
}
