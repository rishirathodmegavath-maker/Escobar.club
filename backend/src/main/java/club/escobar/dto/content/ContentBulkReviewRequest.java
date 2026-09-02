package club.escobar.dto.content;

import club.escobar.entity.enums.ContentStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ContentBulkReviewRequest(
        @NotEmpty @Size(max = 50, message = "At most 50 items can be reviewed at once") List<Long> contentIds,
        @NotNull ContentStatus decision,
        @Size(max = 2000) String note
) {
}
