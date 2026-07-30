package club.escobar.dto.content;

import club.escobar.entity.enums.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContentCreateRequest(
        @NotNull Long campaignId,
        @Size(max = 2000) String caption,
        @NotBlank @Size(max = 500) String mediaUrl,
        @NotNull MediaType mediaType
) {
}
