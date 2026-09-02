package club.escobar.mapper;

import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.campaign.CampaignScheduleChangeResponse;
import club.escobar.entity.Campaign;
import club.escobar.entity.CampaignScheduleChange;
import club.escobar.entity.enums.CampaignStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.EnumSet;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface CampaignMapper {

    Set<CampaignStatus> RESCHEDULABLE_STATUSES = EnumSet.of(CampaignStatus.DRAFT, CampaignStatus.UPCOMING);

    @Mapping(target = "businessId", source = "business.id")
    @Mapping(target = "businessCompanyName", source = "business.businessProfile.companyName")
    @Mapping(target = "businessLogoUrl", source = "business.businessProfile.logoUrl")
    @Mapping(target = "status", expression = "java(entity.getEffectiveStatus())")
    @Mapping(target = "acceptingSubmissions", expression = "java(entity.isOpenForSubmissions())")
    @Mapping(target = "canChangeSchedule", expression = "java(RESCHEDULABLE_STATUSES.contains(entity.getEffectiveStatus()))")
    @Mapping(target = "hot", ignore = true)
    @Mapping(target = "committedBudgetInr", ignore = true)
    CampaignResponse toResponse(Campaign entity);

    @Mapping(target = "changedByUserId", source = "changedBy.id")
    CampaignScheduleChangeResponse toScheduleChangeResponse(CampaignScheduleChange entity);
}
