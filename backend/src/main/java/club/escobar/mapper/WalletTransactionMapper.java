package club.escobar.mapper;

import club.escobar.dto.wallet.WalletTransactionResponse;
import club.escobar.entity.WalletTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletTransactionMapper {

    @Mapping(target = "businessId", source = "business.id")
    @Mapping(target = "businessName", source = "business.businessProfile.companyName")
    @Mapping(target = "performedByUserId", source = "performedBy.id")
    @Mapping(target = "performedByName", source = "performedBy.email")
    @Mapping(target = "payoutId", source = "payout.id")
    @Mapping(target = "campaignTitle", source = "payout.campaign.title")
    @Mapping(target = "reversedTransactionId", source = "reversedTransaction.id")
    @Mapping(target = "confirmedByName", source = "confirmedBy.email")
    WalletTransactionResponse toResponse(WalletTransaction entity);
}
