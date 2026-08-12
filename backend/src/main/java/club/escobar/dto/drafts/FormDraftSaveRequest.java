package club.escobar.dto.drafts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FormDraftSaveRequest(
        @NotBlank @Size(max = 20000)
        String payload
) {
}
