package club.escobar.mapper;

import club.escobar.dto.kyc.CreatorKycProfileResponse;
import club.escobar.dto.kyc.CreatorKycReviewDetailResponse;
import club.escobar.entity.CreatorKycProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CreatorKycMapper {

    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "panNumberMasked", expression = "java(maskPan(entity.getPanNumber()))")
    @Mapping(target = "hasDocument", expression = "java(hasDocument(entity.getDocumentKey()))")
    CreatorKycProfileResponse toResponse(CreatorKycProfile entity);

    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "hasDocument", expression = "java(hasDocument(entity.getDocumentKey()))")
    CreatorKycReviewDetailResponse toReviewDetailResponse(CreatorKycProfile entity);

    @Named("hasDocument")
    default boolean hasDocument(String documentKey) {
        return documentKey != null && !documentKey.isBlank();
    }

    // @Named so MapStruct only calls this where the expression above explicitly invokes it, rather
    // than auto-selecting it as an implicit String->String converter for every other string property
    // this mapper touches (it previously did, silently mangling any other field that happened to also
    // be exactly 10 characters long - e.g. reviewNote or nameOnPan - wherever it was mapped).
    @Named("maskPan")
    default String maskPan(String panNumber) {
        if (panNumber == null || panNumber.length() != 10) {
            return panNumber;
        }
        return "XXXXXX" + panNumber.substring(6);
    }
}
